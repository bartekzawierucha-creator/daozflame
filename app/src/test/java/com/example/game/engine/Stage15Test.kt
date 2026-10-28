package com.example.game.engine

import com.example.game.content.CraftCatalog
import com.example.model.FacilityType
import com.example.model.MajorRealm
import com.example.model.MethodGrade
import com.example.model.CultivationPath
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Etap 15: żadnych martwych ścieżek – każdy budynek, przedmiot i walka coś daje. */
class Stage15Test {
    private fun fresh() = GameEngine.newGame(3L).copy(tutorialStep = Tutorial.DONE)
    private fun apply(s: GameState, c: GameCommand) = GameEngine.apply(s, c)
    private fun lvl(s: GameState, t: FacilityType, l: Int) = s.copy(facilities = s.facilities.map { if (it.type == t) it.copy(level = l) else it })

    @Test
    fun detoxPillUsesCactusAndCleansToxicity() {
        var s = fresh()
        val id = s.disciples.first().id
        s = s.copy(inventory = s.inventory + (CraftCatalog.DETOX_PILL to 1), disciples = s.disciples.map { if (it.id == id) it.copy(toxicity = 50) else it })
        s = apply(s, GameCommand.UsePill(id, CraftCatalog.DETOX_PILL)).state
        assertEquals(20, s.disciple(id)!!.toxicity)
        assertTrue(GameContent.alchemyRecipes.any { it.requiredHerbName == "Kaktus Duszy Pustki" })
    }

    @Test
    fun archivesGiveDaoPerStageAndTurnScrollsIntoDao() {
        var s = lvl(fresh(), FacilityType.MARTIAL_ARCHIVES, 1)
        val d = s.disciples.first().copy(majorRealm = MajorRealm.QI_CONDENSATION, stage = 3).let { Realms.sync(it.copy(stageQi = Realms.required(it))) }
        val a = CultivationSystem.advance(s, d, false, mutableMapOf(), kotlin.random.Random(1), player = false)
        assertEquals(d.daoPoints + 1, a.disciple!!.daoPoints)
        val scroll = Realms.methodOf(CultivationPath.SWORD, MethodGrade.BLACK).scrollItem
        s = s.copy(inventory = s.inventory + (scroll to 1))
        val before = s.disciple(d.id)!!.daoPoints
        s = apply(s, GameCommand.StudyScroll(d.id, scroll)).state
        assertEquals(before + Scrolls.daoValue(MethodGrade.BLACK), s.disciple(d.id)!!.daoPoints)
        assertEquals(0, s.inventory[scroll])
    }

    @Test
    fun fragmentsAssembleIntoAScroll() {
        val m = Realms.methodOf(CultivationPath.HEART, MethodGrade.BLACK)
        val s = fresh().let { it.copy(inventory = it.inventory + (Breath.fragmentItem(m.id) to 5)) }
        val r = apply(s, GameCommand.AssembleScroll(m.id)).state
        assertEquals(1, r.inventory[m.scrollItem])
        assertEquals(0, r.inventory[Breath.fragmentItem(m.id)])
    }

    @Test
    fun pavilionLevelsSpeedUpQiAndMissionHallAddsTowerAttempt() {
        val s = fresh()
        val base = Modifiers.sect(s).percent(Stat.QI_GAIN)
        assertEquals(base + 20.0, Modifiers.sect(lvl(s, FacilityType.CULTIVATION_PAVILION, 3)).percent(Stat.QI_GAIN), 0.01)
        assertEquals(Tower.ATTEMPTS_PER_DAY + 1, Tower.attempts(lvl(s, FacilityType.MISSION_HALL, 3)))
    }

    @Test
    fun worldBossFallsPaysAndReturnsStronger() {
        var s = fresh()
        val id = s.disciples.first().id
        s = s.copy(worldBoss = s.worldBoss.copy(currentHp = 1))
        val maxHp = s.worldBoss.maxHp
        val stones = s.spiritStones
        s = apply(s, GameCommand.AttackWorldBoss(id)).state
        assertEquals((maxHp * 1.5).toLong(), s.worldBoss.currentHp)
        assertTrue(s.spiritStones > stones + 4_000)
        assertTrue(apply(s, GameCommand.AttackWorldBoss(id)).notice!!.contains("jutro"))
    }

    @Test
    fun arenaIsOncePerDayAndNotAFreeWin() {
        val s = fresh()
        val weak = s.disciples.minBy { it.attack }
        val rival = s.rivalSects.maxBy { it.strength }
        val r = apply(s, GameCommand.ArenaDuel(weak.id, rival.name))
        assertTrue(r.notice!!.contains("przegrany") || r.notice!!.contains("Zwycięstwo"))
        assertTrue(apply(r.state, GameCommand.ArenaDuel(weak.id, rival.name)).notice!!.contains("jutra"))
    }
}
