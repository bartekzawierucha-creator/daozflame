package com.example.game.engine

import com.example.model.CombatLogEntry
import com.example.model.ConstitutionType
import com.example.model.EnvironmentalHazard

/**
 * Walka z bossem krainy rozstrzygana krok po kroku. UI wywołuje [GameCommand.CombatStep]
 * co ~0,7 s i animuje zdarzenia [GameEvent.CombatHit]; ręczne akcje gracza naprawdę
 * zmieniają przebieg walki (wcześniej były tylko wizualne).
 */
object CombatSystem {

    const val CRIT_CHANCE = 28
    const val CRIT_MULTIPLIER = 1.9
    const val PILL_HEAL = 85
    const val ULTIMATE_QI = 30

    fun hazardPenalty(hazard: EnvironmentalHazard, constitution: ConstitutionType): Int = when {
        hazard == EnvironmentalHazard.TOXIC_MIASMA && constitution != ConstitutionType.MYRIAD_POISON_IMMUNE -> 15
        hazard == EnvironmentalHazard.EXTREME_FROST && constitution != ConstitutionType.PURE_YANG -> 15
        hazard == EnvironmentalHazard.SCORCHING_MAGMA && constitution != ConstitutionType.CELESTIAL_FROST -> 15
        else -> 0
    }

    fun start(s: GameState, regionId: String, discipleId: String): StepResult {
        val region = s.regions.find { it.id == regionId } ?: return ok(s)
        val d = s.disciple(discipleId) ?: return ok(s)
        val beast = region.bossBeast ?: return ok(s)
        val penalty = hazardPenalty(region.hazard, d.constitution)
        val raw = d.attack + (s.activeFlame?.combatDamageBonus ?: 0) - penalty
        val attack = (raw * Modifiers.sect(s).factor(Stat.COMBAT_DAMAGE)).toInt().coerceAtLeast(10)
        val combat = ActiveCombat(
            regionId = region.id,
            discipleId = d.id,
            discipleName = d.name,
            discipleSchool = d.martialSchool,
            effectiveAttack = attack,
            enemy = beast,
            discipleHp = d.currentHp,
            discipleMaxHp = d.maxHp,
            enemyHp = beast.hp,
            enemyMaxHp = beast.maxHp,
            logs = listOf(
                CombatLogEntry(1, "Wkroczono do regionu: ${region.name} (${region.biome.emoji})"),
                if (penalty > 0) CombatLogEntry(1, "⚠️ Środowisko (${region.hazard.polishName}) osłabia adepta o $penalty pkt!")
                else CombatLogEntry(1, "Konstytucja adepta harmonizuje z otoczeniem!"),
                CombatLogEntry(1, "Rozpoczęto starcie z mitycznym stworem: ${beast.name}!")
            )
        )
        return ok(s.copy(combat = combat))
    }

    /** Pół rundy: na zmianę cios ucznia i kontra bestii. */
    fun step(s: GameState, ctx: EngineContext): StepResult {
        val c = s.combat ?: return ok(s)
        if (c.finished) return ok(s)
        val beast = c.enemy ?: return finish(s, c, false)

        if (c.discipleTurn) {
            if (c.round > ActiveCombat.MAX_ROUNDS) return finish(s, c, false)
            val round = c.round + 1
            val crit = ctx.rng("combat").nextInt(100) < CRIT_CHANCE
            val dmg = ((c.effectiveAttack - beast.defense * 0.4) * (if (crit) CRIT_MULTIPLIER else 1.0)).toInt().coerceAtLeast(20)
            val enemyHp = (c.enemyHp - dmg).coerceAtLeast(0)
            val text = if (crit) "💥 ${c.discipleName} wyprowadza krytyczne cięcie techniki: $dmg obrażeń!"
            else "⚔️ ${c.discipleName} atakuje witalne punkty ${beast.name}: $dmg obrażeń."
            val next = c.copy(
                round = round,
                enemyHp = enemyHp,
                discipleQi = (c.discipleQi + 15).coerceAtMost(100),
                discipleTurn = false,
                logs = c.logs + CombatLogEntry(round, text, isCrit = crit)
            )
            val hit = GameEvent.CombatHit(if (crit) GameEvent.HitKind.CRIT else GameEvent.HitKind.SLASH, dmg, onDisciple = false)
            return if (enemyHp <= 0) finish(s, next, true, hit) else ok(s.copy(combat = next), null, hit)
        }

        val dmg = (beast.attack - (s.disciple(c.discipleId)?.defense ?: 0) * 0.35).toInt().coerceAtLeast(12)
        val discipleHp = (c.discipleHp - dmg).coerceAtLeast(0)
        val next = c.copy(
            discipleHp = discipleHp,
            discipleTurn = true,
            logs = c.logs + CombatLogEntry(c.round, "🔥 ${beast.name} wyprowadza kontratak: $dmg obrażeń adepta!")
        )
        val hit = GameEvent.CombatHit(GameEvent.HitKind.BEAST_ATTACK, dmg, onDisciple = true)
        return if (discipleHp <= 0) finish(s, next, false, hit) else ok(s.copy(combat = next), null, hit)
    }

    fun manualAttack(s: GameState, ctx: EngineContext): StepResult {
        val c = s.combat?.takeIf { !it.finished } ?: return ok(s)
        val dmg = ctx.rng("combat").nextInt(35, 65)
        val next = c.copy(
            enemyHp = (c.enemyHp - dmg).coerceAtLeast(0),
            logs = c.logs + CombatLogEntry(c.logs.size + 1, "⚡ Ręczny cios miecza: zadano $dmg obrażeń!")
        )
        val hit = GameEvent.CombatHit(GameEvent.HitKind.SLASH, dmg, onDisciple = false)
        return if (next.enemyHp <= 0) finish(s, next, true, hit) else ok(s.copy(combat = next), null, hit)
    }

    fun usePill(s: GameState): StepResult {
        val c = s.combat?.takeIf { !it.finished } ?: return ok(s)
        val next = c.copy(
            discipleHp = (c.discipleHp + PILL_HEAL).coerceAtMost(c.discipleMaxHp),
            logs = c.logs + CombatLogEntry(c.logs.size + 1, "💊 Użyto esencji leczącej: przywrócono $PILL_HEAL HP!", isHeal = true)
        )
        return ok(s.copy(combat = next), null, GameEvent.CombatHit(GameEvent.HitKind.HEAL, PILL_HEAL, onDisciple = true))
    }

    fun ultimate(s: GameState, ctx: EngineContext): StepResult {
        val c = s.combat?.takeIf { !it.finished } ?: return ok(s)
        if (c.discipleQi < ULTIMATE_QI) return fail(s, "Zbyt mało Qi na technikę ostateczną!")
        val dmg = ctx.rng("combat").nextInt(120, 190)
        val next = c.copy(
            enemyHp = (c.enemyHp - dmg).coerceAtLeast(0),
            discipleQi = (c.discipleQi - ULTIMATE_QI).coerceAtLeast(0),
            logs = c.logs + CombatLogEntry(c.logs.size + 1, "💥 UWOLNIONO BOSKĄ TECHNIKĘ DAO: Potężne $dmg obrażeń!", isCrit = true)
        )
        val hit = GameEvent.CombatHit(GameEvent.HitKind.ULTIMATE, dmg, onDisciple = false)
        return if (next.enemyHp <= 0) finish(s, next, true, hit) else ok(s.copy(combat = next), null, hit)
    }

    /** Koniec walki: łupy, eksploracja krainy, HP ucznia, reputacja. */
    private fun finish(s: GameState, c: ActiveCombat, victory: Boolean, vararg extra: GameEvent): StepResult {
        val beast = c.enemy
        val region = s.regions.find { it.id == c.regionId }
        val reward = if (victory && beast != null) {
            "🏆 Zwycięstwo! Pokonano ${beast.name}! Zdobyto: ${beast.dropItemName} oraz +${beast.spiritStoneReward} Kamieni Ducha!"
        } else "☠️ Porażka! Adept musiał salwować się ucieczką z pola bitwy."

        val inv = s.inventory.toMutableMap()
        if (victory && beast != null && region != null) {
            inv[region.mineralResource] = (inv[region.mineralResource] ?: 0) + 3
            inv[region.herbResource] = (inv[region.herbResource] ?: 0) + 2
            inv[beast.dropItemName] = (inv[beast.dropItemName] ?: 0) + 1
        }
        val state = s.copy(
            inventory = inv.toMap(),
            regions = s.regions.map {
                if (it.id == c.regionId) it.copy(explorationProgress = (it.explorationProgress + 25).coerceAtMost(100)) else it
            },
            disciples = s.disciples.map { if (it.id == c.discipleId) it.copy(currentHp = c.discipleHp.coerceAtLeast(10)) else it },
            spiritStones = s.spiritStones + if (victory) beast?.spiritStoneReward ?: 0 else 0,
            sectReputation = s.sectReputation + if (victory) (35 * Modifiers.sect(s).factor(Stat.REPUTATION_GAIN)).toInt() else -5,
            combat = c.copy(outcome = victory, rewardText = reward, logs = c.logs + CombatLogEntry(c.round + 1, reward))
        )
        return StepResult(state, extra.toList() + GameEvent.CombatEnded(victory))
    }
}
