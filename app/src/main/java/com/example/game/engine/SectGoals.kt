package com.example.game.engine

import com.example.model.GoalKind
import com.example.model.SectGoal

/**
 * Etap 11: cel tygodnia sekty. Co tydzień gry świat stawia sekcie jedno wyzwanie z terminem (turniej, zamówienie
 * dworu, próba sił…). Wszystkie systemy dostają kierunek: wygrana daje kamienie, sławę i przedmiot, przegrana
 * kosztuje morale i reputację. Postęp liczy się z liczników silnika od chwili ogłoszenia.
 */
object SectGoals {
    const val DURATION = 7
    const val PAUSE = 1
    const val FIRST_DAY = 3
    const val FAIL_MORALE = 8

    /** Wartość licznika danego rodzaju celu. */
    fun counter(s: GameState, k: GoalKind): Long = when (k) {
        GoalKind.BREAKTHROUGHS -> s.meta.stats["breakthroughs"] ?: 0
        GoalKind.WINS -> s.meta.stats["wins"] ?: 0
        GoalKind.TOWER -> s.towerFloor.toLong()
        GoalKind.CONTRACTS -> s.meta.stats["contracts"] ?: 0
        GoalKind.REPUTATION -> s.sectReputation.toLong()
        GoalKind.STONES -> s.spiritStones.toLong()
        GoalKind.TOURNAMENT -> champions(s)
    }

    /** Siła trzech najlepszych uczniów – miara na turniej. */
    fun champions(s: GameState): Long = s.disciples.map { it.combatPower.toLong() }.sortedDescending().take(3).sum()

    fun progress(s: GameState, g: SectGoal): Long =
        if (g.kind == GoalKind.TOURNAMENT) champions(s) else (counter(s, g.kind) - g.base).coerceAtLeast(0)

    fun done(s: GameState, g: SectGoal) = progress(s, g) >= g.target

    /** Nowy cel: rodzaj losowany bez powtórzenia poprzedniego, cel skalowany do siły sekty. */
    fun generate(s: GameState, ctx: EngineContext, previous: GoalKind?): SectGoal {
        val rng = ctx.rng("sect-goal")
        val kinds = GoalKind.entries.filter { it != previous }
        val k = kinds[rng.nextInt(kinds.size)]
        val n = s.disciples.size.coerceAtLeast(1)
        val tier = (s.goalsWon + 1).coerceAtMost(10)
        val stones = 400 + 150 * tier
        val rep = 20 + 8 * tier
        val day = s.currentDay
        fun g(title: String, desc: String, target: Long, item: String? = null) =
            SectGoal(k, title, desc, target.coerceAtLeast(1), counter(s, k), day, day + DURATION, stones, rep, item)
        return when (k) {
            GoalKind.BREAKTHROUGHS -> (2 + n / 3 + tier / 3).toLong().let {
                g("Wiosenna Próba Dao", "Starszyzna Kontynentu ocenia sekty po przełomach. Dokonaj $it przełomów.", it, GameContent.BREAKTHROUGH_PILL)
            }
            GoalKind.WINS -> (3 + tier).toLong().let {
                g("Łowy na bestie", "Wioski proszą o ochronę przed bestiami. Wygraj $it walk.", it, CraftV2.PROTECTION_TALISMAN)
            }
            GoalKind.TOWER -> (2 + tier / 2).toLong().let {
                g("Wyzwanie Wieży", "Mistrzowie innych sekt patrzą na Wieżę Prób. Zdobądź $it nowe piętra.", it)
            }
            GoalKind.CONTRACTS -> (2 + tier / 3).toLong().let {
                g("Zamówienie Dworu", "Dwór Cesarski szuka zaufanych dostawców. Wypełnij $it kontrakty.", it)
            }
            GoalKind.REPUTATION -> (60L + 25L * tier).let {
                g("Sława sekty", "Bardowie śpiewają o sektach. Zdobądź $it reputacji.", it)
            }
            GoalKind.STONES -> (s.spiritStones / 5 + 800L * tier).let {
                g("Skarbiec na zimę", "Zima idzie ciężka. Odłóż $it kamieni ponad obecny stan.", it)
            }
            GoalKind.TOURNAMENT -> (champions(s) * (108 + 2 * tier) / 100).let {
                g("Turniej Pięciu Sekt", "Za $DURATION dni turniej. Trzech najlepszych musi mieć łącznie $it mocy.", it, GameContent.BREAKTHROUGH_PILL)
            }
        }
    }

    /** Raz na dzień: rozliczenie celu i ogłoszenie nowego. */
    fun daily(s0: GameState, ctx: EngineContext): Pair<GameState, List<String>> {
        if (s0.tutorialStep != Tutorial.DONE || s0.currentDay < FIRST_DAY) return s0 to emptyList()
        var s = s0
        val notes = mutableListOf<String>()
        val g = s.weeklyGoal
        if (g != null) {
            when {
                done(s, g) -> {
                    val inv = g.rewardItem?.let { s.inventory + (it to (s.inventory[it] ?: 0) + 1) } ?: s.inventory
                    s = s.copy(
                        spiritStones = s.spiritStones + g.rewardStones, sectReputation = s.sectReputation + g.rewardReputation, inventory = inv,
                        morale = (s.morale + 5).coerceAtMost(100), weeklyGoal = null, goalsWon = s.goalsWon + 1, nextGoalDay = s.currentDay + PAUSE
                    )
                    s = WorldAI.note(s, "🏆 ${g.title} – sekta sprostała wyzwaniu.")
                    notes += "🏆 ${g.title}: wygrana! +${g.rewardStones} kamieni, +${g.rewardReputation} reputacji" + (g.rewardItem?.let { ", $it" } ?: "") + "."
                }
                s.currentDay > g.deadline -> {
                    s = s.copy(
                        morale = (s.morale - FAIL_MORALE).coerceAtLeast(0), sectReputation = (s.sectReputation - g.rewardReputation / 2).coerceAtLeast(0),
                        weeklyGoal = null, goalsLost = s.goalsLost + 1, nextGoalDay = s.currentDay + PAUSE
                    )
                    s = WorldAI.note(s, "🥀 ${g.title} – sekta nie zdążyła.")
                    notes += "🥀 ${g.title}: porażka. Morale −$FAIL_MORALE, reputacja −${g.rewardReputation / 2}."
                }
            }
        }
        if (s.weeklyGoal == null && s.currentDay >= s.nextGoalDay) {
            val next = generate(s, ctx, g?.kind)
            s = s.copy(weeklyGoal = next)
            notes += "📯 Cel tygodnia: ${next.title}. ${next.description}"
        }
        return s to notes
    }
}
