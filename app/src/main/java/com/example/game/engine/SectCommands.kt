package com.example.game.engine

import com.example.game.WuxiaGenerator
import com.example.model.*
import kotlin.random.Random

/** Uczniowie, kultywacja, patriarcha, polityka i bestie sekty. */
object SectCommands {

    const val RECRUIT_COST = 120
    const val REFRESH_COST = 60

    fun recruitPool(rng: Random): List<Disciple> = listOf(
        CultivationRealm.QI_CONDENSATION_1,
        CultivationRealm.QI_CONDENSATION_1,
        CultivationRealm.QI_CONDENSATION_5,
        CultivationRealm.QI_CONDENSATION_5,
        CultivationRealm.QI_CONDENSATION_9,
        CultivationRealm.FOUNDATION_EARLY
    ).map { WuxiaGenerator.generateDisciple(rng, it) }

    private fun GameState.withDisciple(id: String, f: (Disciple) -> Disciple) =
        copy(disciples = disciples.map { if (it.id == id) f(it) else it })

    fun recruit(s: GameState, id: String): StepResult {
        val recruit = s.availableRecruits.find { it.id == id } ?: return ok(s)
        if (s.spiritStones < RECRUIT_COST) return fail(s, "Zbyt mało Kamieni Ducha na ceremonię inicjacji ($RECRUIT_COST wymagane)!")
        return ok(
            s.copy(
                spiritStones = s.spiritStones - RECRUIT_COST,
                sectReputation = s.sectReputation + 10,
                disciples = s.disciples + recruit,
                availableRecruits = s.availableRecruits.filter { it.id != id }
            ),
            "Przyjęto nowego adepta: ${recruit.name} do Sekty!"
        )
    }

    fun refreshRecruits(s: GameState, ctx: EngineContext): StepResult {
        if (s.spiritStones < REFRESH_COST) return fail(s, "Potrzeba $REFRESH_COST Kamieni Ducha na zwołanie próby rekrutacyjnej!")
        return ok(
            s.copy(spiritStones = s.spiritStones - REFRESH_COST, availableRecruits = recruitPool(ctx.rng("recruits"))),
            "Przybyli nowi kandydaci (6 adeptów) szukający mistrza Dao!"
        )
    }

    fun recruitAll(s: GameState): StepResult {
        val recruits = s.availableRecruits
        if (recruits.isEmpty()) return fail(s, "Brak kandydatów w Sali Rekrutacji!")
        val total = recruits.size * RECRUIT_COST
        if (s.spiritStones < total) return fail(s, "Zbyt mało Kamieni Ducha na masową rekrutację ($total wymagane)!")
        return ok(
            s.copy(
                spiritStones = s.spiritStones - total,
                sectReputation = s.sectReputation + recruits.size * 10,
                disciples = s.disciples + recruits,
                availableRecruits = emptyList()
            ),
            "Przyjęto jednocześnie ${recruits.size} nowych adeptów do Sekty!"
        )
    }

    fun assignRole(s: GameState, id: String, role: DiscipleRole): StepResult =
        ok(s.withDisciple(id) { it.copy(role = role) }, "Zmieniono zadanie ucznia na: ${role.polishName}")

    fun toggleAutoBreakthrough(s: GameState, enabled: Boolean?): StepResult {
        val v = enabled ?: !s.autoBreakthroughEnabled
        return ok(
            s.copy(autoBreakthroughEnabled = v),
            if (v) "⚡ Włączono automatyczny przełom kultywacji adeptów!" else "⏸️ Wyłączono automatyczny przełom."
        )
    }

    fun toggleAutoPills(s: GameState, enabled: Boolean?): StepResult {
        val v = enabled ?: !s.autoUsePillsForBreakthrough
        return ok(
            s.copy(autoUsePillsForBreakthrough = v),
            if (v) "💊 Auto-przełom będzie zużywać Pigułki Przełomu Qi (+35% szansy)." else "Auto-przełom nie będzie zużywać pigułek."
        )
    }

    fun breakthroughAll(s: GameState, ctx: EngineContext): StepResult {
        val ready = s.disciples.filter { it.realmProgress >= 100 }
        if (ready.isEmpty()) return fail(s, "Żaden adept nie zgromadził jeszcze 100% Qi.")
        val rng = ctx.rng("breakthrough")
        val inv = s.inventory.toMutableMap()
        var rep = 0
        val events = mutableListOf<GameEvent>()
        val updated = s.disciples.map { d ->
            if (d.realmProgress < 100) return@map d
            val r = CultivationRules.breakthrough(d, s.autoUsePillsForBreakthrough, inv, s.timeOfDay, rng)
            rep += r.reputation
            events += GameEvent.Breakthrough(d.id, r.success)
            r.disciple
        }
        return ok(
            s.copy(disciples = updated, inventory = inv.toMap(), sectReputation = s.sectReputation + rep),
            "🌟 Dokonano masowego przełomu dla ${ready.size} adeptów!",
            *events.toTypedArray()
        )
    }

    fun attemptBreakthrough(s: GameState, ctx: EngineContext, id: String, usePill: Boolean): StepResult {
        val d = s.disciple(id) ?: return ok(s)
        if (d.realmProgress < 100) return fail(s, "Uczeń musi zgromadzić 100% Qi w swoim meridianie przed próbą przełomu!")
        val inv = s.inventory.toMutableMap()
        val r = CultivationRules.breakthrough(d, usePill, inv, s.timeOfDay, ctx.rng("breakthrough"))
        return ok(
            s.withDisciple(id) { r.disciple }.copy(inventory = inv.toMap(), sectReputation = s.sectReputation + r.reputation),
            r.message,
            GameEvent.Breakthrough(id, r.success)
        )
    }

    fun unlockSkill(s: GameState, id: String, skillId: String): StepResult {
        val d = s.disciple(id) ?: return ok(s)
        val skill = GameContent.skill(skillId) ?: return ok(s)
        if (skillId in d.unlockedSkillIds) return fail(s, "Ta technika walki została już opanowana!")
        if (d.daoPoints < skill.costDaoPoints) return fail(s, "Zbyt mało punktów Dao (${skill.costDaoPoints} wymagane)!")
        if (d.martialSchool != skill.martialSchool) return fail(s, "Uczeń należy do innej szkoły walki!")
        val hp = d.maxHp + skill.hpBonus
        val atk = d.attack + skill.attackBonus
        val def = d.defense + skill.defenseBonus
        val updated = d.copy(
            daoPoints = d.daoPoints - skill.costDaoPoints,
            unlockedSkillIds = d.unlockedSkillIds + skillId,
            attack = atk,
            defense = def,
            maxHp = hp,
            currentHp = d.currentHp + skill.hpBonus,
            combatPower = CultivationRules.combatPower(hp, atk, def, d.comprehension, d.karmicLuck)
        )
        return ok(s.withDisciple(id) { updated }, "Uczeń ${d.name} opanował technikę: ${skill.name}!")
    }

    /** Zażycie pigułki: postęp Qi, leczenie i punkty Dao wg receptury. */
    fun usePill(s: GameState, id: String, pillName: String): StepResult {
        val d = s.disciple(id) ?: return ok(s)
        val count = s.inventory[pillName] ?: 0
        if (count < 1) return fail(s, "Brak pigułki: $pillName w magazynie!")
        val recipe = GameContent.recipeForPill(pillName) ?: return fail(s, "$pillName nie jest pigułką do zażycia.")
        val progress = (d.realmProgress + recipe.breakthroughBonusPercent).coerceAtMost(100)
        val hp = (d.currentHp + recipe.hpRestore).coerceAtMost(d.maxHp)
        val effects = buildList {
            if (recipe.breakthroughBonusPercent > 0) add("Qi ${d.realmProgress}% → $progress%")
            if (recipe.hpRestore > 0) add("HP ${d.currentHp} → $hp")
            if (recipe.daoPointsBonus > 0) add("+${recipe.daoPointsBonus} pkt Dao")
        }.joinToString(", ")
        return ok(
            s.withDisciple(id) { it.copy(realmProgress = progress, currentHp = hp, daoPoints = it.daoPoints + recipe.daoPointsBonus) }
                .copy(inventory = s.inventory + (pillName to count - 1)),
            "💊 ${d.name.substringBefore(" (")} zażywa: $pillName. $effects"
        )
    }

    fun toggleDecree(s: GameState, decreeId: String): StepResult {
        val updated = s.sectLeader.activeDecrees.map { if (it.id == decreeId) it.copy(isActive = !it.isActive) else it }
        val toggled = updated.find { it.id == decreeId }
        val status = if (toggled?.isActive == true) "Aktywowano" else "Cofnięto"
        return ok(s.copy(sectLeader = s.sectLeader.copy(activeDecrees = updated)), "$status edykt: ${toggled?.title ?: decreeId}")
    }

    fun upgradePrestige(s: GameState): StepResult {
        val cost = s.sectLeader.prestigeLevel * 250
        if (s.spiritStones < cost) return fail(s, "Brak Kamieni Ducha na podniesienie Prestiżu Patriarchy! Potrzeba: $cost")
        val leader = s.sectLeader.copy(
            prestigeLevel = s.sectLeader.prestigeLevel + 1,
            leadershipPoints = s.sectLeader.leadershipPoints + 25,
            divineAuthority = s.sectLeader.divineAuthority + 60
        )
        return ok(
            s.copy(spiritStones = s.spiritStones - cost, sectReputation = s.sectReputation + 45, sectLeader = leader),
            "Awansowano Prestiż Patriarchy do poziomu ${leader.prestigeLevel}!"
        )
    }

    fun changePolicy(s: GameState, policy: SectPolicy): StepResult =
        ok(s.copy(currentPolicy = policy), "📜 Zmieniono doktrynę sekty na: ${policy.polishName}! (${policy.buffDescription})")

    fun setActiveBeast(s: GameState, beastId: String): StepResult {
        val beast = s.sectBeasts.find { it.id == beastId } ?: return ok(s)
        return ok(s.copy(activeBeastId = beastId), "🐉 Wybrano Świętą Bestię Sekty: ${beast.name} (${beast.title})!")
    }

    fun feedBeast(s: GameState, beastId: String, herb: String): StepResult {
        val beast = s.sectBeasts.find { it.id == beastId } ?: return ok(s)
        val count = s.inventory[herb] ?: 0
        if (count < 1) return fail(s, "Brak składnika: $herb w magazynie sekty!")
        val favorite = herb.equals(beast.favoriteHerb, ignoreCase = true)
        val intimacy = if (favorite) 15 else 8
        val energy = if (favorite) 300 else 150
        val beasts = s.sectBeasts.map {
            if (it.id == beastId) it.copy(
                intimacy = (it.intimacy + intimacy).coerceAtMost(100),
                currentEnergy = (it.currentEnergy + energy).coerceAtMost(it.maxEnergy)
            ) else it
        }
        return ok(
            s.copy(inventory = s.inventory + (herb to count - 1), sectBeasts = beasts),
            "Nakarmino ${beast.name} ziółkiem $herb! Intymność +$intimacy% ${if (favorite) "(Ulubiony Przysmak! ✨)" else ""}"
        )
    }

    fun evolveBeast(s: GameState, beastId: String): StepResult {
        val beast = s.sectBeasts.find { it.id == beastId } ?: return ok(s)
        val cost = beast.tier * 300
        if (beast.intimacy < 60) return fail(s, "Wymagana intymność co najmniej 60%, aby obudzić ewolucję Bestii! (Obecnie: ${beast.intimacy}%)")
        if (s.spiritStones < cost) return fail(s, "Zbyt mało Kamieni Ducha na rytuał ewolucji Bestii! Wymagane: $cost")
        val beasts = s.sectBeasts.map {
            if (it.id == beastId) it.copy(
                tier = it.tier + 1,
                maxEnergy = it.maxEnergy + 500,
                currentEnergy = it.maxEnergy + 500,
                intimacy = 20,
                passiveSectBuff = "+${(it.tier + 1) * 20}% Wzmocnienie Potęgi Sekty i Błogosławieństwo Dao"
            ) else it
        }
        return ok(
            s.copy(spiritStones = s.spiritStones - cost, sectBeasts = beasts, sectReputation = s.sectReputation + 50),
            "✨ Rytuał Ewolucji udany! ${beast.name} osiągnął Rząd ${beast.tier + 1} Boskiego Strażnika!"
        )
    }
}
