package com.example.game.engine

import com.example.model.DiscipleRole
import com.example.model.MajorRealm
import com.example.model.TimeOfDay
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Etap 10: uczniowie żyją sami, mistrz doradza. */
class Stage10Test {

    private fun fresh(seed: Long = 1L) = GameEngine.newGame(seed).copy(tutorialStep = Tutorial.DONE)
    private fun apply(s: GameState, c: GameCommand) = GameEngine.apply(s, c)
    private fun nextDay(s: GameState): GameState =
        apply(s.copy(timeOfDay = TimeOfDay.FULL_MOON, phaseTick = TimePhase.TICKS_PER_PHASE - 1), GameCommand.Tick).state
    private fun GameState.with(id: String, f: (com.example.model.Disciple) -> com.example.model.Disciple) =
        copy(disciples = disciples.map { if (it.id == id) f(it) else it })

    @Test
    fun newGameHasAutonomyAndTrust() {
        val s = fresh()
        assertTrue(s.autonomy)
        assertTrue(s.disciples.all { it.trust in 1..100 })
    }

    @Test
    fun characterAndBondDecideObedience() {
        val s = fresh()
        val d = s.disciples.first()
        val devoted = d.copy(loyalty = 100, trust = 100, traitIds = listOf("loyal"), hiddenTraitIds = emptyList())
        val rebel = d.copy(loyalty = 0, trust = 0, traitIds = listOf("hotheaded"), hiddenTraitIds = listOf("coward"))
        val tower = GameCommand.ClimbTower(d.id)
        assertEquals(95, Autonomy.chance(s, devoted, tower))
        assertEquals(5, Autonomy.chance(s, rebel, tower))
        // Tchórz chętnie słucha rady, by się wstrzymać – i niechętnie, by ryzykować.
        val coward = d.copy(traitIds = emptyList(), hiddenTraitIds = listOf("coward"))
        assertTrue(Autonomy.chance(s, coward, GameCommand.Hold(d.id)) > Autonomy.chance(s, coward, tower))
    }

    @Test
    fun rebelRefusesAndStateStaysAsItWas() {
        var s = fresh()
        val id = s.disciples.first().id
        s = s.with(id) { it.copy(loyalty = 0, trust = 0, traitIds = listOf("hotheaded"), hiddenTraitIds = listOf("coward")) }
        var refused = 0
        repeat(10) {
            val r = apply(s, GameCommand.Advise(GameCommand.ClimbTower(id)))
            if (r.events.any { it is GameEvent.AdviceOutcome && !it.accepted }) {
                refused++
                assertEquals(s.towerFloor, r.state.towerFloor)
                assertTrue(r.notice!!.contains("odmawia"))
            }
            s = r.state.copy(towerAttempts = 0)
        }
        assertTrue("odmów: $refused", refused >= 7)
    }

    @Test
    fun naggingLowersObedience() {
        val s = fresh()
        val id = s.disciples.first().id
        val before = Autonomy.chance(s, s.disciple(id)!!)
        val after = apply(s, GameCommand.Advise(GameCommand.AssignRole(id, DiscipleRole.MEDITATING))).state
        assertTrue(Autonomy.chance(after, after.disciple(id)!!) < before)
    }

    @Test
    fun novicesInTutorialAlwaysListen() {
        var s = GameEngine.newGame(1L)
        assertTrue(s.tutorialStep != Tutorial.DONE)
        val id = s.disciples.first().id
        s = s.with(id) { it.copy(loyalty = 0, trust = 0, traitIds = listOf("lazy"), hiddenTraitIds = emptyList()) }
        val r = apply(s, GameCommand.Advise(GameCommand.AssignRole(id, DiscipleRole.MINING)))
        assertEquals(DiscipleRole.MINING, r.state.disciple(id)!!.role)
    }

    @Test
    fun acceptedRoleAdviceIsKeptForCommitDays() {
        var s = fresh()
        val id = s.disciples.first().id
        s = s.with(id) { it.copy(loyalty = 100, trust = 100, traitIds = listOf("loyal"), hiddenTraitIds = emptyList()) }
        // Rada sprzeczna z charakterem (lojalny medytujący) – po przyjęciu trzyma się jej mimo własnych upodobań.
        var r = apply(s, GameCommand.Advise(GameCommand.AssignRole(id, DiscipleRole.MINING)))
        var tries = 0
        while (r.state.disciple(id)!!.role != DiscipleRole.MINING && tries++ < 10) r = apply(r.state.copy(step = r.state.step + 1), GameCommand.Advise(GameCommand.AssignRole(id, DiscipleRole.MINING)))
        s = r.state
        assertEquals(DiscipleRole.MINING, s.disciple(id)!!.role)
        repeat(Autonomy.COMMIT_DAYS - 1) { s = nextDay(s) }
        assertEquals(DiscipleRole.MINING, s.disciple(id)!!.role)
        assertTrue(s.disciple(id)!!.plan!!.contains("rady mistrza"))
    }

    @Test
    fun boldDiscipleBreaksThroughOnHisOwn() {
        var s = fresh().copy(autoUsePillsForBreakthrough = true)
        val id = s.disciples.first().id
        s = s.with(id) { d ->
            val x = d.copy(majorRealm = MajorRealm.QI_CONDENSATION, stage = MajorRealm.QI_CONDENSATION.stages, traitIds = listOf("fearless"),
                hiddenTraitIds = emptyList(), injuredUntilDay = 0, illUntilDay = 0, currentHp = d.maxHp, holdUntilDay = 0)
            x.copy(stageQi = Realms.required(x))
        }
        assertTrue(CultivationSystem.atMajorGate(s, s.disciple(id)!!))
        repeat(3) { s = nextDay(s) }
        val d = s.disciple(id)!!
        assertTrue(d.diary.joinToString(), d.diary.any { "przełomu" in it })
    }

    @Test
    fun holdAdviceKeepsHimFromTheGate() {
        var s = fresh().copy(autoUsePillsForBreakthrough = true)
        val id = s.disciples.first().id
        s = s.with(id) { d ->
            val x = d.copy(majorRealm = MajorRealm.QI_CONDENSATION, stage = MajorRealm.QI_CONDENSATION.stages, traitIds = listOf("fearless"),
                hiddenTraitIds = emptyList(), injuredUntilDay = 0, illUntilDay = 0, currentHp = d.maxHp)
            x.copy(stageQi = Realms.required(x))
        }
        s = apply(s, GameCommand.Hold(id)).state
        repeat(Autonomy.HOLD_DAYS - 1) { s = nextDay(s) }
        val d = s.disciple(id)!!
        assertEquals(MajorRealm.QI_CONDENSATION, d.majorRealm)
        assertFalse(d.diary.any { "przełomu" in it })
    }

    @Test
    fun woundsHealWithRest() {
        var s = fresh()
        val id = s.disciples.first().id
        s = s.with(id) { it.copy(currentHp = 10, role = DiscipleRole.MEDITATING) }
        repeat(5) { s = nextDay(s) }
        val d = s.disciple(id)!!
        assertTrue("${d.currentHp}/${d.maxHp}", d.currentHp >= d.maxHp * 3 / 10)
    }

    @Test
    fun punishmentHurtsTheBond() {
        val s = fresh()
        val id = s.disciples.first().id
        val r = apply(s, GameCommand.Punish(id)).state
        if (r != s) assertTrue(r.disciple(id)!!.trust < s.disciple(id)!!.trust)
    }

    @Test
    fun disciplesPickRolesThemselvesAndKeepADiary() {
        var s = fresh(7L)
        repeat(40) { s = nextDay(s) }
        assertTrue(s.disciples.all { it.plan != null })
        assertTrue(s.disciples.any { it.diary.isNotEmpty() })
    }

    @Test
    fun gatherersGoToTheFieldsWhenThePantryIsEmpty() {
        val s = fresh().let { st -> st.copy(inventory = st.inventory - DiscipleRules.FOOD) }
        val d = s.disciples.first().copy(traitIds = listOf("gatherer"), hiddenTraitIds = listOf("humble"), injuredUntilDay = 0, illUntilDay = 0)
        assertEquals(DiscipleRole.HERB_GATHERING, Autonomy.preferredRole(s, d.copy(currentHp = d.maxHp)))
    }
}
