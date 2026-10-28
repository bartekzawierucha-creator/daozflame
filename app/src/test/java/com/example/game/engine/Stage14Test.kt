package com.example.game.engine

import com.example.model.FacilityType
import com.example.model.MajorRealm
import com.example.model.TimeOfDay
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Etap 14: sekta zaczyna skromnie – budynki, jajo bestii, Droga Sekty, tempo wczesnej gry. */
class Stage14Test {
    private fun small(seed: Long = 1L) = GameEngine.newGame(seed, startSmall = true).copy(tutorialStep = Tutorial.DONE)
    private fun apply(s: GameState, c: GameCommand) = GameEngine.apply(s, c)
    private fun nextDay(s: GameState): GameState =
        apply(s.copy(timeOfDay = TimeOfDay.FULL_MOON, phaseTick = TimePhase.TICKS_PER_PHASE - 1), GameCommand.Tick).state

    @Test
    fun newSectStartsWithPavilionOnlyAndNoBeasts() {
        val s = small()
        assertTrue(Buildings.has(s, FacilityType.CULTIVATION_PAVILION))
        assertFalse(Buildings.has(s, FacilityType.ALCHEMY_CHAMBER))
        assertFalse(Buildings.has(s, FacilityType.MISSION_HALL))
        assertTrue(s.sectBeasts.isEmpty())
        assertNull(s.activeBeastId)
    }

    @Test
    fun lockedActionsPointToTheBuilding() {
        val s = small()
        val r = apply(s, GameCommand.ClimbTower(s.disciples.first().id))
        assertTrue(r.notice!!.contains("Sala Ekspedycji"))
        assertEquals(s.towerFloor, r.state.towerFloor)
    }

    @Test
    fun buildingTakesStonesMaterialsAndTime() {
        var s = small().copy(spiritStones = 10_000, inventory = small().inventory + ("Duchowy Nefryt" to 20))
        s = apply(s, GameCommand.UpgradeFacility(FacilityType.ALCHEMY_CHAMBER)).state
        val plan = Buildings.PLANS.getValue(FacilityType.ALCHEMY_CHAMBER)
        assertTrue(Buildings.building(s, FacilityType.ALCHEMY_CHAMBER))
        assertEquals(10_000 - plan.stones, s.spiritStones)
        repeat(plan.days) { s = nextDay(s.copy(spiritStones = 10_000)) }
        assertTrue(Buildings.has(s, FacilityType.ALCHEMY_CHAMBER))
    }

    @Test
    fun firstGuardianLeavesAnEggThatHatchesIntoTheFirstBeast() {
        var s = small()
        val (withEgg, msg) = Road.firstEgg(s)
        assertTrue(msg != null)
        s = apply(withEgg, GameCommand.IncubateEgg).state
        repeat(Creatures.HATCH_DAYS) { s = nextDay(s) }
        assertEquals(1, s.sectBeasts.size)
        assertEquals(s.sectBeasts.first().id, s.activeBeastId)
        assertNull(Road.firstEgg(s).second)
    }

    @Test
    fun roadStepsCompleteAndPayOut() {
        var s = small()
        assertEquals(0, s.roadStep)
        val id = s.disciples.first().id
        s = s.copy(disciples = s.disciples.map { if (it.id == id) it.copy(diary = listOf("Dz. 1: Posłuchał rady: medytacja.")) else it })
        val stones = s.spiritStones
        s = apply(s, GameCommand.SeeModule("ADEPTS")).state
        assertEquals(1, s.roadStep)
        assertEquals(stones + Road.STEPS[0].rewardStones, s.spiritStones)
        assertTrue("ADEPTS" in s.seenModules)
    }

    @Test
    fun basicMethodReachesCoreAndFoundationIsNotAMarathon() {
        val s = small()
        val d = s.disciples.first().copy(majorRealm = MajorRealm.FOUNDATION, stage = 4)
        assertTrue(Realms.ceiling(d).ordinal >= MajorRealm.CORE_FORMATION.ordinal)
        // Cała Fundamenty to mniej niż 10 tys. cykli medytacji przy przyroście 60 Qi (ok. 4 h zegara).
        val total = (1..4).sumOf { Realms.required(MajorRealm.FOUNDATION, it) }
        assertTrue("$total", total / 60 < 10_000)
    }
}
