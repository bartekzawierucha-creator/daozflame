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
        if (s.discoveredRegionIds.isNotEmpty() && regionId !in s.discoveredRegionIds) return fail(s, "Kraina kryje się we mgle – najpierw wyślij zwiad.")
        val penalty = hazardPenalty(region.hazard, d.constitution)
        // Etap 4: premię z płomienia ma tylko jego nosiciel; wcześniej działał płomień z kotła.
        val flameBonus = if (s.e4Initialized) s.flames.find { it.id == d.flameId }?.combatDamageBonus ?: 0 else s.activeFlame?.combatDamageBonus ?: 0
        val raw = d.attack + flameBonus - penalty
        // Etap 5a: żywioł krainy kontra korzeń ucznia (+30% dla przewagi).
        val element = Elements.of(region.biome)
        val cycles = if (s.rules != null) Cycles.apply(s, d, element, beast.defense) else Cycles.Effect(1.0, 1.0, emptyList())
        val elemAtk = (if (s.e2Initialized) Elements.attackFactor(d, element) else 1.0) * cycles.attack
        val elemDef = (if (s.e2Initialized) Elements.defenseFactor(d, element) else 1.0) * cycles.defense
        val attack = (raw * elemAtk * Modifiers.disciple(Modifiers.sect(s), d).factor(Stat.COMBAT_DAMAGE)).toInt().coerceAtLeast(10)
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
            enemyAttackFactor = elemDef * Catastrophes.beastFactor(s),
            logs = listOfNotNull(
                CombatLogEntry(1, "Wkroczono do regionu: ${region.name} (${region.biome.emoji})"),
                if (penalty > 0) CombatLogEntry(1, "⚠️ Środowisko (${region.hazard.polishName}) osłabia adepta o $penalty pkt!")
                else CombatLogEntry(1, "Konstytucja adepta harmonizuje z otoczeniem!"),
                CombatLogEntry(1, "Rozpoczęto starcie z mitycznym stworem: ${beast.name}!"),
                if (elemAtk > 1.0) CombatLogEntry(1, "${element.glyph} Żywioł ucznia przełamuje ${element.polishName} bestii: +30% obrażeń!") else null,
                if (elemDef > 1.0) CombatLogEntry(1, "${element.glyph} ${element.polishName} bestii tłumi korzeń ucznia: bestia rani mocniej!") else null
            ) + cycles.observed.map { (id, text) -> CombatLogEntry(1, "☯ " + Cycles.label(s, id, text)) }
        )
        var observed = s
        cycles.observed.forEach { (id, text) -> observed = Journal.observe(observed, id, text) }
        return ok(observed.copy(combat = combat))
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

        if (c.enemySealed) {
            return ok(s.copy(combat = c.copy(enemySealed = false, discipleTurn = true,
                logs = c.logs + CombatLogEntry(c.round, "📜 Pieczęć wiąże ${beast.name} – traci ruch!"))))
        }
        val dmg = (beast.attack * c.enemyAttackFactor - (s.disciple(c.discipleId)?.defense ?: 0) * 0.35).toInt().coerceAtLeast(12)
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

    /** Etap 6: dopisuje ruch gracza i sprawdza, czy ostatnie trzy ruchy tworzą formę. */
    fun withForm(result: StepResult, move: Char): StepResult {
        val s = result.state
        val c = s.combat ?: return result
        if (c.finished) return result
        val history = (c.moves + move).takeLast(6)
        val form = Forms.detect(s, history, c.formsUsed) ?: return result.copy(state = s.copy(combat = c.copy(moves = history)))
        val dmg = (c.effectiveAttack * form.bonusPercent / 100.0).toInt().coerceAtLeast(10)
        val next = c.copy(
            moves = history,
            formsUsed = c.formsUsed + form.id,
            enemyHp = (c.enemyHp - dmg).coerceAtLeast(0),
            logs = c.logs + CombatLogEntry(c.logs.size + 1, "🌀 ${form.name}! Ruchy splatają się w formę: $dmg obrażeń.", isCrit = true)
        )
        val known = Journal.reveal(s, Secrets.form(form.id), "Sekwencja: ${form.sequence}")
        val hit = GameEvent.CombatHit(GameEvent.HitKind.ULTIMATE, dmg, onDisciple = false)
        if (next.enemyHp <= 0) {
            val fin = finish(known, next, true, hit)
            return fin.copy(events = result.events + fin.events)
        }
        return StepResult(known.copy(combat = next), result.events + hit)
    }

    /** Etap 4: talizman użyty w walce (z magazynu). */
    fun talisman(s: GameState, name: String): StepResult {
        val c = s.combat?.takeIf { !it.finished } ?: return ok(s)
        val have = s.inventory[name] ?: 0
        if (have < 1) return fail(s, "Brak: $name.")
        val st = s.copy(inventory = s.inventory + (name to have - 1))
        return when (name) {
            "Talizman Ognistej Kuli" -> {
                val next = c.copy(enemyHp = (c.enemyHp - 250).coerceAtLeast(0), logs = c.logs + CombatLogEntry(c.logs.size + 1, "🔥 Talizman Ognistej Kuli: 250 obrażeń!", isCrit = true))
                val hit = GameEvent.CombatHit(GameEvent.HitKind.ULTIMATE, 250, onDisciple = false)
                if (next.enemyHp <= 0) finish(st, next, true, hit) else ok(st.copy(combat = next), null, hit)
            }
            "Talizman Złotej Tarczy" -> ok(
                st.copy(combat = c.copy(discipleHp = (c.discipleHp + 300).coerceAtMost(c.discipleMaxHp),
                    logs = c.logs + CombatLogEntry(c.logs.size + 1, "🛡️ Złota Tarcza: +300 HP", isHeal = true))),
                null, GameEvent.CombatHit(GameEvent.HitKind.HEAL, 300, onDisciple = true)
            )
            "Talizman Pieczęci" -> ok(st.copy(combat = c.copy(enemySealed = true,
                logs = c.logs + CombatLogEntry(c.logs.size + 1, "📜 Pieczęć gotowa – bestia straci następny ruch."))))
            "Talizman Ucieczki" -> {
                val fled = c.copy(outcome = false, rewardText = "Uczeń uciekł dzięki talizmanowi – bez utraty reputacji.",
                    logs = c.logs + CombatLogEntry(c.logs.size + 1, "💨 Talizman Ucieczki – odwrót bez hańby."))
                StepResult(st.copy(combat = fled, disciples = st.disciples.map { if (it.id == c.discipleId) it.copy(currentHp = c.discipleHp.coerceAtLeast(10)) else it }),
                    listOf(GameEvent.CombatEnded(false)))
            }
            else -> fail(s, "$name nie działa w walce.")
        }
    }

    /** Szansa (%) na zwój metody z bossa krainy i jego stopień wg numeru krainy. */
    const val SCROLL_DROP_CHANCE = 25
    /** Qi za zwycięstwo na ścieżkach walki – wyrażone w cyklach medytacji ucznia. */
    const val SWORD_VICTORY_TICKS = 150
    const val DEMONIC_VICTORY_TICKS = 200

    fun scrollGrade(regionIndex: Int): com.example.model.MethodGrade = when {
        regionIndex < 2 -> com.example.model.MethodGrade.BLACK
        regionIndex < 6 -> com.example.model.MethodGrade.MYSTIC
        else -> com.example.model.MethodGrade.HEAVENLY
    }

    /** Zwój z bossa krainy (także na wyprawie Auto): 25%, zwykle ścieżki zwycięzcy (75%). */
    fun rollScroll(s: GameState, discipleId: String, regionIndex: Int, rng: kotlin.random.Random): String? {
        if (!s.e3Initialized || rng.nextInt(100) >= SCROLL_DROP_CHANCE) return null
        val own = s.disciple(discipleId)?.path
        val path = if (own != null && rng.nextInt(100) < 75) own else com.example.model.CultivationPath.entries.random(rng)
        return Realms.methodOf(path, scrollGrade(regionIndex)).scrollItem
    }

    /** Ścieżka, której sekta naprawdę potrzebuje: losowo spośród ścieżek uczniów (częściej tych przy suficie metody). */
    fun wantedPath(s: GameState, rng: kotlin.random.Random): com.example.model.CultivationPath {
        val stuck = s.disciples.filter { d -> d.path != null && d.majorRealm?.next?.let { it.ordinal > Realms.ceiling(d).ordinal } == true }.mapNotNull { it.path }
        val all = s.disciples.mapNotNull { it.path }
        val pool = stuck.ifEmpty { all }
        return if (pool.isNotEmpty() && rng.nextInt(100) < 70) pool.random(rng) else com.example.model.CultivationPath.entries.random(rng)
    }

    /** Koniec walki: łupy, eksploracja krainy, HP ucznia, reputacja. */
    private fun finish(s: GameState, c: ActiveCombat, victory: Boolean, vararg extra: GameEvent): StepResult {
        val beast = c.enemy
        val region = s.regions.find { it.id == c.regionId }
        val reward = if (victory && beast != null) {
            "🏆 Zwycięstwo! Pokonano ${beast.name}! Zdobyto: ${beast.dropItemName} oraz +${beast.spiritStoneReward} Kamieni Ducha!"
        } else "☠️ Porażka! Adept musiał salwować się ucieczką z pola bitwy."

        val inv = s.inventory.toMutableMap()
        var scroll: String? = null
        if (victory && beast != null && region != null) {
            inv[region.mineralResource] = (inv[region.mineralResource] ?: 0) + 3
            inv[region.herbResource] = (inv[region.herbResource] ?: 0) + 2
            inv[beast.dropItemName] = (inv[beast.dropItemName] ?: 0) + 1
            if (s.e3Initialized) {
                val rng = GameRng.stream(s.worldSeed, s.step, "scroll-drop")
                rollScroll(s, c.discipleId, s.regions.indexOfFirst { it.id == region.id }, rng)?.let { item ->
                    inv[item] = (inv[item] ?: 0) + 1
                    scroll = item
                }
            }
        }
        // Ścieżki Miecza i Demoniczna czerpią Qi z walki; demoniczna walka obniża karmę.
        val fighter = s.disciple(c.discipleId)
        val pathTicks = when {
            !s.e3Initialized || fighter == null -> 0
            fighter.path == com.example.model.CultivationPath.SWORD && victory -> SWORD_VICTORY_TICKS
            fighter.path == com.example.model.CultivationPath.DEMONIC && victory -> DEMONIC_VICTORY_TICKS
            else -> 0
        }
        val pathQi = if (fighter != null && pathTicks > 0) CultivationSystem.ticksWorth(s, fighter, pathTicks) else 0.0
        val karmaDelta = if (s.e3Initialized && fighter?.path == com.example.model.CultivationPath.DEMONIC) -2 else 0
        val state = s.copy(
            inventory = inv.toMap(),
            regions = s.regions.map {
                if (it.id == c.regionId) it.copy(explorationProgress = (it.explorationProgress + 25).coerceAtMost(100)) else it
            },
            disciples = s.disciples.map {
                if (it.id == c.discipleId) CultivationSystem.gainQi(it.copy(currentHp = c.discipleHp.coerceAtLeast(10)), pathQi) else it
            },
            karma = (s.karma + karmaDelta).coerceIn(-100, 100),
            spiritStones = s.spiritStones + if (victory) beast?.spiritStoneReward ?: 0 else 0,
            sectReputation = s.sectReputation + if (victory) (35 * Modifiers.sect(s).factor(Stat.REPUTATION_GAIN)).toInt() else -5,
            combat = c.copy(
                outcome = victory,
                rewardText = reward + (scroll?.let { " Znaleziono: $it!" } ?: ""),
                logs = c.logs + CombatLogEntry(c.round + 1, reward + (scroll?.let { " Znaleziono: $it!" } ?: ""))
            ),
            morale = if (s.e2Initialized) (s.morale + if (victory) 2 else -3).coerceIn(0, 100) else s.morale
        )
        var result = state
        val notes = mutableListOf<String>()
        // Etap 14: pierwszy strażnik zostawia jajo bestii.
        if (victory && region != null) Road.firstEgg(result).let { (st, msg) -> result = st; msg?.let { notes += it } }
        // Etap 13: bossowie zostawiają artefakty.
        if (victory && region != null) {
            val rng = GameRng.stream(s.worldSeed, s.step, "boss-loot")
            if (rng.nextInt(100) < Loot.BOSS_ARTIFACT_CHANCE) {
                val a = Loot.artifactFor(s.regions.indexOfFirst { it.id == region.id }, rng)
                result = result.copy(vaultArtifacts = result.vaultArtifacts + a)
                notes += "⚔ Z legowiska: ${a.name} (${a.quality.polishName}) trafia do skarbca."
            }
        }
        // Etap 5a: koniec wyprawy – łupy z węzłów (porażka: połowa).
        val run = s.expedition
        if (run != null && run.atBoss && run.regionId == c.regionId) {
            result = Expeditions.payout(result, run, if (victory) 1.0 else Expeditions.DEFEAT_LOOT).copy(expedition = null)
            notes += if (victory) "Wyprawa zakończona – łupy trafiły do skarbca." else "Odwrót z legowiska – zabrano połowę łupów."
        }
        if (victory && (region?.explorationProgress ?: 0) < 100 && result.regions.find { it.id == c.regionId }?.explorationProgress == 100) {
            val (withTrail, msg) = FlameSources.onRegionExplored(result, c.regionId)
            result = withTrail
            msg?.let { notes += it }
        }
        if (victory && s.e4Initialized) {
            val (withContracts, msg) = Contracts.onBossDefeated(result, c.regionId)
            result = withContracts
            msg?.let { notes += it }
        }
        return StepResult(result, extra.toList() + notes.map { GameEvent.Notice(it) } + GameEvent.CombatEnded(victory))
    }
}
