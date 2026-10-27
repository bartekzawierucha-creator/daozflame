package com.example.game.engine

import com.example.model.DiscipleRole
import com.example.model.FacilityType
import com.example.model.TimeOfDay
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Etap 1: ModifierStack, edykty, postęp offline, samouczek. */
class Stage1Test {

    private fun apply(s: GameState, c: GameCommand) = GameEngine.apply(s, c)
    private fun fresh(seed: Long = 1L) = GameEngine.newGame(seed)

    // --- ModifierStack ---

    @Test
    fun modifierOrderIsFlatThenPercentThenMultiply() {
        val stack = ModifierStack(
            listOf(
                Modifier("a", Stat.QI_GAIN, ModOp.FLAT, 10.0),
                Modifier("b", Stat.QI_GAIN, ModOp.PERCENT, 50.0),
                Modifier("c", Stat.QI_GAIN, ModOp.PERCENT, -25.0),
                Modifier("d", Stat.QI_GAIN, ModOp.MULTIPLY, 2.0),
                Modifier("e", Stat.STONE_INCOME, ModOp.PERCENT, 99.0)
            )
        )
        assertEquals((20.0 + 10.0) * 1.25 * 2.0, stack.apply(Stat.QI_GAIN, 20.0), 1e-9)
        assertEquals(4, stack.breakdown(Stat.QI_GAIN).size)
        assertEquals(1.0, ModifierStack(emptyList()).factor(Stat.ALCHEMY_COST), 1e-9)
    }

    @Test
    fun meditationGainMatchesFormerFormula() {
        // Bez systemów Etapu 2 (korzeń, ranga, cechy, morale) wzór musi być identyczny z dawnym.
        val base = fresh(2L)
        val s = base.copy(
            e2Initialized = false,
            disciples = base.disciples.map { it.copy(spiritRoot = null, traitIds = emptyList(), hiddenTraitIds = emptyList(), rank = com.example.model.SectRank.OUTER) }
        )
        val sect = Modifiers.sect(s)
        s.disciples.forEach { d ->
            val shared = sect.percent(Stat.QI_GAIN)
            val traits = d.fatedTraits.sumOf { it.cultivationBonusPercent }
            val clan = if (d.ancientClan.name.contains("Ye")) 50 else 0
            val expected = ((10 + d.comprehension / 8) * CultivationRules.talentMultiplier(d.talentGrade) * (1.0 + (shared + traits + clan) / 100.0)).toInt()
            val actual = CultivationRules.meditationGain(d, Modifiers.disciple(sect, d))
            assertTrue("$expected vs $actual", kotlin.math.abs(expected - actual) <= 1)
        }
    }

    // --- Edykty ---

    @Test
    fun newGameHasAllTenEdictsAndTwoActive() {
        val s = fresh()
        assertEquals(10, s.sectLeader.activeDecrees.size)
        assertEquals(2, s.sectLeader.activeDecrees.count { it.isActive })
    }

    @Test
    fun edictSlotsAreLimited() {
        val s = fresh()
        val inactive = s.sectLeader.activeDecrees.first { !it.isActive }
        val r = apply(s, GameCommand.ToggleDecree(inactive.id))
        assertTrue(r.notice!!.contains("najwyżej"))
        assertEquals(s.sectLeader.activeDecrees, r.state.sectLeader.activeDecrees)

        val withPalace = s.copy(facilities = s.facilities.map { if (it.type == FacilityType.DIPLOMACY_HALL) it.copy(level = 5) else it })
        assertEquals(3, EdictCatalog.slots(withPalace))
        val r2 = apply(withPalace, GameCommand.ToggleDecree(inactive.id))
        assertTrue(r2.state.sectLeader.activeDecrees.first { it.id == inactive.id }.isActive)
    }

    @Test
    fun edictHasThreeDayCooldown() {
        val s = fresh()
        val active = s.sectLeader.activeDecrees.first { it.isActive }
        val off = apply(s, GameCommand.ToggleDecree(active.id)).state
        val back = apply(off, GameCommand.ToggleDecree(active.id))
        assertTrue(back.notice!!.contains("dopiero za"))
        val later = off.copy(currentDay = off.currentDay + EdictCatalog.COOLDOWN_DAYS)
        assertTrue(apply(later, GameCommand.ToggleDecree(active.id)).state.sectLeader.activeDecrees.first { it.id == active.id }.isActive)
    }

    @Test
    fun edictEffectsReachTheGame() {
        val base = fresh(3L)
        val decrees = base.sectLeader.activeDecrees.map { it.copy(isActive = it.id == "ed_trade") }
        val trade = base.copy(sectLeader = base.sectLeader.copy(activeDecrees = decrees), morale = 50)
        assertEquals(25.0, Modifiers.sect(trade).percent(Stat.STONE_INCOME), 1e-9)

        val guards = trade.disciples.map { it.copy(role = DiscipleRole.SECT_GUARD) }
        val s = trade.copy(disciples = guards, territories = emptyList(), timeOfDay = TimeOfDay.DAY, phaseTick = 0)
        val after = apply(s, GameCommand.Tick).state
        assertEquals(s.spiritStones + (2 * guards.size * 1.25).toInt(), after.spiritStones)
    }

    @Test
    fun upkeepIsPaidAtDawnOrEdictsAreSuspended() {
        val s = fresh(4L).copy(timeOfDay = TimeOfDay.FULL_MOON, phaseTick = TimePhase.TICKS_PER_PHASE - 1) // następny cykl = świt
        val upkeep = EdictCatalog.dailyUpkeep(s)
        assertTrue(upkeep > 0)
        val paid = apply(s, GameCommand.Tick).state
        assertEquals(s.currentDay + 1, paid.currentDay)
        assertTrue(paid.sectLeader.activeDecrees.any { it.isActive })

        val broke = apply(s.copy(spiritStones = 0), GameCommand.Tick)
        assertFalse(broke.state.sectLeader.activeDecrees.any { it.isActive && (EdictCatalog.get(it.id)?.upkeepStones ?: 0) > 0 })
        assertTrue(broke.notice!!.contains("Skarbiec pusty"))
    }

    // --- Postęp offline ---

    @Test
    fun offlineProgressRespectsLimitsAndStopsBeforeBreakthrough() {
        val s = fresh(5L).copy(disciples = fresh(5L).disciples.map { it.copy(realmProgress = 0) })
        val r = apply(s, GameCommand.OfflineCatchUp(30L * 3_600_000)) // 30 h
        val report = r.events.filterIsInstance<GameEvent.OfflineSummary>().single().report
        assertTrue(report.capped)
        assertEquals(OfflineProgress.MAX_HOURS * 3_600_000L, report.countedMs)
        assertTrue(r.state.disciples.all { it.realmProgress <= 99 })
        assertEquals(s.disciples.map { it.realm }, r.state.disciples.map { it.realm })
        assertEquals(null, r.state.activeEventId)
        assertEquals((s.spiritStones + report.stones - report.upkeep).coerceAtLeast(0), r.state.spiritStones)
        assertTrue(report.stones > 0)
        assertTrue(r.state.currentDay > s.currentDay)
    }

    @Test
    fun offlineIgnoresShortOrNegativeTime() {
        val s = fresh(6L)
        assertEquals(s.copy(step = s.step + 1), apply(s, GameCommand.OfflineCatchUp(-5_000_000)).state)
        assertEquals(s.copy(step = s.step + 1), apply(s, GameCommand.OfflineCatchUp(10_000)).state)
    }

    @Test
    fun offlineIsDeterministic() {
        val s = fresh(7L)
        assertEquals(apply(s, GameCommand.OfflineCatchUp(8 * 3_600_000L)).state, apply(s, GameCommand.OfflineCatchUp(8 * 3_600_000L)).state)
    }

    // --- Samouczek ---

    @Test
    fun tutorialStartsForNewGameAndIsDoneForOldSaves() {
        assertEquals(0, fresh().tutorialStep)
        assertEquals(Tutorial.DONE, GameState().tutorialStep)
    }

    @Test
    fun tutorialAdvancesOnlyOnSuccessfulGoal() {
        var s = fresh(8L)
        s = apply(s, GameCommand.TutorialNext).state
        s = apply(s, GameCommand.TutorialNext).state
        assertEquals(GameCommand.AttemptBreakthrough::class, Tutorial.current(s)!!.goal)

        val notReady = s.disciples.first()
        val failed = apply(s.copy(disciples = listOf(notReady.copy(realmProgress = 10))), GameCommand.AttemptBreakthrough(notReady.id, false))
        assertEquals(2, failed.state.tutorialStep)

        val ready = s.copy(disciples = listOf(notReady.copy(realmProgress = 100)))
        assertEquals(3, apply(ready, GameCommand.AttemptBreakthrough(notReady.id, false)).state.tutorialStep)
    }

    @Test
    fun tutorialCanBeSkipped() {
        assertEquals(Tutorial.DONE, apply(fresh(), GameCommand.TutorialSkip).state.tutorialStep)
    }
}
