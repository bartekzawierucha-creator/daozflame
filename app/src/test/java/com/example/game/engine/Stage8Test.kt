package com.example.game.engine

import com.example.model.Difficulty
import com.example.model.KnowledgeLevel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Etap 8: meta – odrodzenie, Drzewo Niebios, Księga Dziedzictwa, osiągnięcia, cele dzienne. */
class Stage8Test {

    private fun fresh(seed: Long = 1L) = GameEngine.newGame(seed)
    private fun apply(s: GameState, c: GameCommand) = GameEngine.apply(s, c)

    private fun ready(seed: Long = 1L) = fresh(seed).copy(currentDay = Rebirth.MIN_DAY, spiritStones = 4_000_000, sectReputation = 20_000)

    @Test
    fun rebirthNeedsConditionAndConfirmation() {
        assertTrue(apply(fresh(), GameCommand.Rebirth(true)).notice!!.contains("Odrodzenie możliwe"))
        assertTrue(apply(ready(), GameCommand.Rebirth(false)).notice!!.contains("Potwierdź"))
    }

    @Test
    fun essenceIsSqrtOfPowerOverHundred() {
        val s = ready()
        assertEquals((kotlin.math.sqrt(Rebirth.power(s).toDouble()) / 100).toInt(), Rebirth.essence(s))
    }

    /** Jawna lista: zostaje tylko [GameState.meta]; reszta to świeża gra z nowym ziarnem (poza polami pochodnymi). */
    @Test
    fun rebirthResetsEveryFieldExceptMeta() {
        val s = ready(2L).let { it.copy(meta = it.meta.copy(achievements = setOf("wins_1"), stats = mapOf("wins" to 5L))) }
        val r = apply(s, GameCommand.Rebirth(true)).state
        val expected = GameEngine.newGame(Rebirth.newSeed(s), s.lastActiveAtMs)
        val KEEPS = setOf("meta")
        val DERIVED = setOf("step", "journal", "sectMasterName")
        GameState::class.java.declaredFields.filter { !java.lang.reflect.Modifier.isStatic(it.modifiers) }.forEach { f ->
            f.isAccessible = true
            if (f.name in KEEPS || f.name in DERIVED) return@forEach
            assertEquals("pole ${f.name}", f.get(expected), f.get(r))
        }
        assertEquals(2, r.meta.era)
        assertEquals(setOf("wins_1"), r.meta.achievements - (r.meta.achievements - setOf("wins_1")))
        assertTrue(r.meta.essence >= Rebirth.essence(s))
        assertEquals(5L, r.meta.stats["wins"])
    }

    @Test
    fun legacyBookCarriesUniversalLawsNotAnswers() {
        var s = ready(3L)
        s = Journal.reveal(s, Secrets.CYCLE_EXCESS)
        val breath = Secrets.breath(s.rules!!.breath.keys.first())
        s = Journal.reveal(s, breath)
        val r = apply(s, GameCommand.Rebirth(true)).state
        assertTrue(Journal.certain(r, Secrets.CYCLE_EXCESS))
        assertNull(Journal.level(r, breath))
        assertTrue((r.meta.codex["Oddech"] ?: 0) >= 1)
        assertTrue(r.rules != s.rules)
    }

    @Test
    fun heavenTreeIsSequentialAndGivesModifiers() {
        val s = fresh(4L).copy(meta = com.example.model.MetaProgress(essence = 10))
        assertTrue(apply(s, GameCommand.BuyHeavenNode("qi_2")).notice!!.contains("poprzedni"))
        var st = apply(s, GameCommand.BuyHeavenNode("qi_1")).state
        st = apply(st, GameCommand.BuyHeavenNode("qi_2")).state
        assertEquals(7, st.meta.essence)
        assertTrue(Modifiers.sect(st).breakdown(Stat.QI_GAIN).any { it.startsWith("Drzewo Niebios: Qi 2") })
        assertEquals(30, HeavenTree.NODES.size)
    }

    @Test
    fun eraModifierOncePerEra() {
        var s = apply(fresh(5L), GameCommand.ChooseEra("storm")).state
        assertTrue(apply(s, GameCommand.ChooseEra("gold")).notice!!.contains("już"))
        assertTrue(Rebirth.raidFactor(s) > 1.0)
        s = apply(s.copy(currentDay = Rebirth.MIN_DAY), GameCommand.Rebirth(true)).state
        assertNull(s.meta.eraModifier)
    }

    @Test
    fun successionPicksHeirAndPassedOverMayLeave() {
        val s = fresh(6L)
        val weakest = s.disciples.minBy { it.combatPower }
        assertTrue(apply(s, GameCommand.Succession(weakest.id, true)).notice!!.contains("pierwszym roku"))
        val year = s.copy(currentDay = 200, disciples = s.disciples.map { it.copy(loyalty = 10) })
        assertTrue(apply(year, GameCommand.Succession(weakest.id, false)).notice!!.contains("Potwierdź"))
        val r = apply(year, GameCommand.Succession(weakest.id, true)).state
        assertEquals(weakest.name.substringBefore(" ("), r.meta.heirName)
        assertTrue(r.disciples.size < s.disciples.size)
        val next = apply(r.copy(currentDay = Rebirth.MIN_DAY), GameCommand.Rebirth(true)).state
        assertTrue(next.sectMasterName.contains(r.meta.heirName!!))
    }

    @Test
    fun sixtyAchievementsUnlockedOnlyByEngineEvents() {
        assertEquals(60, Achievements.ALL.size)
        assertEquals(Achievements.ALL.size, Achievements.ALL.map { it.id }.toSet().size)
        val rich = fresh(7L).copy(spiritStones = 20_000_000)
        // Komenda bez wyzwalacza DAY nie sprawdza progów kamieni.
        val noTrigger = apply(rich, GameCommand.ToggleAutoPills(false)).state
        assertTrue("stones_10000" !in noTrigger.meta.achievements)
        val day = apply(rich.copy(timeOfDay = com.example.model.TimeOfDay.FULL_MOON, phaseTick = TimePhase.TICKS_PER_PHASE - 1), GameCommand.Tick).state
        assertTrue("stones_10000000" in day.meta.achievements)
        assertTrue(Achievements.ALL.filter { it.secret }.all { it.description == "???" })
    }

    @Test
    fun titlesRequireAchievement() {
        val s = fresh(8L)
        assertTrue(apply(s, GameCommand.SetTitle("ending_1")).notice!!.contains("zdobyć"))
        val r = apply(s.copy(meta = s.meta.copy(achievements = setOf("ending_1"))), GameCommand.SetTitle("ending_1")).state
        assertEquals("Pieczęć Światła", r.meta.title)
    }

    @Test
    fun dailyGoalsResistClockRollbackAndStreakPauses() {
        val day = DailyGoals.DAY_MS
        var s = apply(fresh(9L), GameCommand.RealTime(10 * day + 1000)).state
        assertEquals(3, s.meta.goals.size)
        val goals = s.meta.goals
        val back = apply(s, GameCommand.RealTime(5 * day)).state
        assertEquals(s.meta, back.meta)
        // Wykonaj cele „na siłę” i przejdź przez dzień z pominięciem.
        s = s.copy(meta = s.meta.copy(goals = goals.map { it.copy(progress = it.target - 1) }))
        val winGoal = s.meta.goals.first()
        s = DailyGoals.progress(GameCommand.Tick, s, s.copy(meta = s.meta.copy(goals = s.meta.goals.map { it.copy(progress = it.target) }.dropLast(0))), emptyList())
        s = s.copy(meta = s.meta.copy(goals = s.meta.goals.map { it.copy(progress = it.target) }, streak = 1))
        val skipped = apply(s, GameCommand.RealTime(13 * day)).state
        assertEquals(1, skipped.meta.streak)
        assertTrue(skipped.meta.goals.none { it.done })
        assertTrue(winGoal.kind in DailyGoals.KINDS)
    }

    @Test
    fun completingAllGoalsAdvancesStreakWithReward() {
        var s = apply(fresh(10L).copy(spiritStones = 0), GameCommand.RealTime(DailyGoals.DAY_MS * 3)).state
        s = s.copy(meta = s.meta.copy(goals = listOf(com.example.model.DailyGoal("harvest", 1, 0), com.example.model.DailyGoal("win", 1, 1), com.example.model.DailyGoal("tower", 1, 1))))
        val plot = com.example.model.FieldPlot("Trawa Wiecznej Wiosny", 1, 1)
        s = s.copy(currentDay = 10, fields = listOf(plot) + s.fields.drop(1))
        val r = apply(s, GameCommand.HarvestFields).state
        assertEquals(1, r.meta.streak)
        assertEquals(300, r.spiritStones)
    }

    @Test
    fun offeringOncePerRealDay() {
        var s = apply(fresh(11L).copy(spiritStones = 1_000), GameCommand.RealTime(DailyGoals.DAY_MS * 2)).state
        s = apply(s, GameCommand.OfferToAncestors).state
        assertTrue(apply(s, GameCommand.OfferToAncestors).notice!!.contains("Dziś"))
    }

    @Test
    fun difficultyAndIronWillAreKeptAcrossEras() {
        val s = ready(12L)
        val r = apply(s, GameCommand.Rebirth(true, Difficulty.HARD, true)).state
        assertEquals(Difficulty.HARD, r.meta.difficulty)
        assertTrue(r.meta.ironWill)
        assertTrue(Modifiers.sect(r).breakdown(Stat.STONE_INCOME).any { it.startsWith(Difficulty.HARD.polishName) })
        assertTrue(Rebirth.raidFactor(r) > 1.0)
    }

    @Test
    fun countersComeFromEvents() {
        var s = fresh(13L)
        s = apply(s, GameCommand.StartExpedition(s.regions.first().id, s.disciples.first().id)).state
        s = s.copy(combat = s.combat!!.copy(enemyHp = 1))
        s = apply(s, GameCommand.CombatManualAttack).state
        assertEquals(1L, s.meta.stats["wins"])
        assertTrue("wins_1" in s.meta.achievements)
        assertEquals(KnowledgeLevel.GUESS, KnowledgeLevel.GUESS)
    }
}
