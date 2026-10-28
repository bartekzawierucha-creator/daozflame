package com.example.game.engine

import com.example.model.CultivationPath
import com.example.model.CultivationRealm
import com.example.model.DaoLaw
import com.example.model.Disciple
import com.example.model.FoundationQuality
import com.example.model.MajorRealm
import com.example.model.MethodGrade
import com.example.model.SectRank
import com.example.model.TimeOfDay
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Etap 3: królestwa i etapy, wąskie gardła, tribulacja, ścieżki, metody, Prawa, krew, odosobnienie. */
class Stage3Test {

    private fun fresh(seed: Long = 1L) = GameEngine.newGame(seed)
    private fun apply(s: GameState, c: GameCommand) = GameEngine.apply(s, c)

    private fun at(d: Disciple, major: MajorRealm, stage: Int, full: Boolean = false): Disciple {
        val x = d.copy(majorRealm = major, stage = stage, stageQi = 0.0)
        return Realms.sync(if (full) x.copy(stageQi = Realms.required(x)) else x)
    }

    private fun only(s: GameState, d: Disciple) = s.copy(disciples = listOf(d))

    // --- Model i migracja ---

    @Test
    fun legacyMappingIsMonotonicAndRoundTrips() {
        var last = -1
        MajorRealm.entries.forEach { m ->
            (1..m.stages).forEach { st ->
                val legacy = Realms.toLegacy(m, st)
                assertTrue("$m/$st", legacy.ordinal >= last)
                last = legacy.ordinal
            }
        }
        CultivationRealm.entries.forEach { r ->
            val (m, st) = Realms.fromLegacy(r)
            assertEquals(r, Realms.toLegacy(m, st))
        }
    }

    @Test
    fun oldSaveKeepsRealmAndProgress() {
        val base = fresh(2L)
        val old = base.copy(
            e3Initialized = false,
            disciples = base.disciples.map { it.copy(majorRealm = null, stage = 1, stageQi = 0.0, realmProgress = 40, methodId = null) }
        )
        val migrated = GameEngine.normalize(old)
        assertTrue(migrated.e3Initialized)
        migrated.disciples.zip(old.disciples).forEach { (n, o) ->
            assertEquals(o.realm, n.realm)
            assertEquals(40, n.realmProgress)
            assertNotNull(n.majorRealm)
            assertTrue("Metoda sięga królestwa", Realms.ceiling(n).ordinal >= n.majorRealm!!.ordinal)
        }
        assertEquals(migrated, GameEngine.normalize(migrated))
    }

    @Test
    fun stageCostsGrowBySixtyPercent() {
        val a = Realms.required(MajorRealm.FOUNDATION, 1)
        assertEquals(a * 1.6, Realms.required(MajorRealm.FOUNDATION, 2), 1e-6)
        assertTrue(Realms.required(MajorRealm.CORE_FORMATION, 1) > Realms.required(MajorRealm.FOUNDATION, 4))
    }

    // --- Etapy ---

    @Test
    fun minorBreakthroughIsAutomaticAndKeepsRealm() {
        val s = fresh(3L)
        val d = at(s.disciples.first(), MajorRealm.QI_CONDENSATION, 3, full = true)
        val after = apply(only(s, d).copy(autoBreakthroughEnabled = false), GameCommand.Tick).state.disciples.single()
        assertEquals(4, after.stage)
        assertEquals(MajorRealm.QI_CONDENSATION, after.majorRealm)
        assertTrue(after.attack > d.attack)
    }

    @Test
    fun meditationNeverExceedsStageRequirement() {
        var s = fresh(4L).copy(autoBreakthroughEnabled = false, autonomy = false, spiritStones = 1_000_000)
        val d = at(s.disciples.first(), MajorRealm.QI_CONDENSATION, 9)
        s = only(s, d)
        repeat(3000) { s = apply(s, GameCommand.Tick).state }
        val x = s.disciples.single()
        assertTrue(x.stageQi <= Realms.required(x) + 1e-6)
        assertEquals(MajorRealm.QI_CONDENSATION, x.majorRealm)
    }

    // --- Wąskie gardła ---

    @Test
    fun methodCeilingBlocksMajorBreakthrough() {
        val s = fresh(5L)
        // Etap 14: podstawowa metoda sięga Złotego Rdzenia – gardło pojawia się przed Narodzinami Duszy.
        val d = at(s.disciples.first(), MajorRealm.CORE_FORMATION, 4, full = true).copy(methodId = Realms.BASIC_METHOD, path = CultivationPath.ORTHODOX)
        assertTrue(CultivationSystem.bottleneck(s, d)!!.contains("Wąskie gardło metody"))
    }

    @Test
    fun coreNeedsPathAndTierTwoTechnique() {
        val s = fresh(6L)
        val black = Realms.methodOf(CultivationPath.ORTHODOX, MethodGrade.BLACK).id
        val noPath = at(s.disciples.first(), MajorRealm.FOUNDATION, 4, full = true).copy(methodId = black, path = null, unlockedSkillIds = emptySet())
        assertTrue(CultivationSystem.bottleneck(s, noPath)!!.contains("ścieżkę"))
        val noSkill = noPath.copy(path = CultivationPath.ORTHODOX)
        assertTrue(CultivationSystem.bottleneck(s, noSkill)!!.contains("techniki"))
        val tier2 = GameContent.skills.first { it.tier >= 2 }.id
        assertNull(CultivationSystem.bottleneck(s, noSkill.copy(unlockedSkillIds = setOf(tier2))))
    }

    @Test
    fun nascentNeedsLowToxicityAndNoHeartDemon() {
        val s = fresh(7L)
        val mystic = Realms.methodOf(CultivationPath.ORTHODOX, MethodGrade.MYSTIC).id
        val d = at(s.disciples.first(), MajorRealm.CORE_FORMATION, 4, full = true).copy(methodId = mystic, path = CultivationPath.ORTHODOX)
        assertTrue(CultivationSystem.bottleneck(s, d.copy(toxicity = 40))!!.contains("Toksyczność"))
        assertTrue(CultivationSystem.bottleneck(s, d.copy(heartDemon = true))!!.contains("demon", ignoreCase = true))
        assertNull(CultivationSystem.bottleneck(s, d))
    }

    @Test
    fun soulTransformationNeedsDaoLaw() {
        val s = fresh(8L)
        val mystic = Realms.methodOf(CultivationPath.ORTHODOX, MethodGrade.MYSTIC).id
        val d = at(s.disciples.first(), MajorRealm.NASCENT_SOUL, 4, full = true).copy(methodId = mystic, path = CultivationPath.ORTHODOX)
        assertNotNull(CultivationSystem.bottleneck(s, d))
        assertNull(CultivationSystem.bottleneck(s, d.copy(laws = mapOf(DaoLaw.SWORD.polishName to 10.0))))
    }

    // --- Wielki przełom i tribulacja ---

    @Test
    fun tribulationRealmsNeedPlayerAction() {
        val s = fresh(9L)
        val black = Realms.methodOf(CultivationPath.ORTHODOX, MethodGrade.BLACK).id
        val tier2 = GameContent.skills.first { it.tier >= 2 }.id
        val d = at(s.disciples.first(), MajorRealm.FOUNDATION, 4, full = true)
            .copy(methodId = black, path = CultivationPath.ORTHODOX, unlockedSkillIds = setOf(tier2))
        val auto = apply(only(s, d), GameCommand.Tick).state.disciples.single()
        assertEquals("Automat nie przechodzi tribulacji", MajorRealm.FOUNDATION, auto.majorRealm)

        val r = apply(only(s, d), GameCommand.AttemptBreakthrough(d.id, usePill = false))
        assertTrue(r.events.any { it is GameEvent.Tribulation })
        val x = r.state.disciples.singleOrNull()
        assertTrue(x == null || x.majorRealm == MajorRealm.CORE_FORMATION || x.injuredUntilDay > s.currentDay)
        if (x?.majorRealm == MajorRealm.CORE_FORMATION) assertTrue(x.coreGrade in 1..9)
    }

    @Test
    fun tribulationMitigationHelps() {
        val d = at(fresh(10L).disciples.first(), MajorRealm.FOUNDATION, 4).copy(defense = 10, maxHp = 1000, currentHp = 1000)
        val plain = CultivationSystem.tribulation(d, MajorRealm.CORE_FORMATION, mutableMapOf(), GameRng(1L))
        val withPill = CultivationSystem.tribulation(d, MajorRealm.CORE_FORMATION, mutableMapOf(CultivationSystem.TRIBULATION_PILL to 1), GameRng(1L))
        assertTrue(withPill.damagePerStrike.sum() < plain.damagePerStrike.sum())
        val body = CultivationSystem.tribulation(d.copy(path = CultivationPath.BODY), MajorRealm.CORE_FORMATION, mutableMapOf(), GameRng(1L))
        assertTrue(body.damagePerStrike.sum() < plain.damagePerStrike.sum())
    }

    @Test
    fun foundationWithoutPillIsHard() {
        var successes = 0
        repeat(400) { i ->
            val s = fresh(11L).copy(inventory = emptyMap(), step = i.toLong())
            val d = at(s.disciples.first(), MajorRealm.QI_CONDENSATION, 9, full = true)
            val r = apply(only(s, d), GameCommand.AttemptBreakthrough(d.id, usePill = true))
            if (r.state.disciples.single().majorRealm == MajorRealm.FOUNDATION) successes++
        }
        assertTrue("Bez pigułki: $successes/400", successes in 50..110)
    }

    @Test
    fun foundationQualityIsRecorded() {
        val s = fresh(12L)
        val d = at(s.disciples.first(), MajorRealm.QI_CONDENSATION, 9, full = true)
        var st = only(s, d)
        var tries = 0
        while (st.disciples.single().majorRealm != MajorRealm.FOUNDATION && tries++ < 50) {
            st = apply(st.copy(disciples = listOf(d)), GameCommand.AttemptBreakthrough(d.id, usePill = true)).state
        }
        assertNotNull(st.disciples.single().foundationQuality)
    }

    @Test
    fun highToxicityCanCauseDeviation() {
        var deviations = 0
        repeat(300) { i ->
            val s = fresh(13L).copy(step = i.toLong())
            val d = at(s.disciples.first(), MajorRealm.QI_CONDENSATION, 9, full = true).copy(toxicity = 90)
            val r = apply(only(s, d), GameCommand.AttemptBreakthrough(d.id, usePill = true))
            if (r.notice?.contains("Odchylenie") == true) deviations++
        }
        assertTrue("Odchylenia: $deviations/300", deviations in 35..90)
    }

    // --- Ścieżki, metody, Prawa ---

    @Test
    fun pathChoiceRequiresFoundationAndGivesYellowMethod() {
        val s = fresh(14L)
        val low = at(s.disciples.first(), MajorRealm.QI_CONDENSATION, 5)
        assertTrue(apply(only(s, low), GameCommand.ChoosePath(low.id, CultivationPath.SWORD)).notice!!.contains("Fundamenty"))
        val found = at(s.disciples.first(), MajorRealm.FOUNDATION, 1).copy(methodId = Realms.BASIC_METHOD, path = null)
        val r = apply(only(s, found), GameCommand.ChoosePath(found.id, CultivationPath.SWORD)).state.disciples.single()
        assertEquals(CultivationPath.SWORD, r.path)
        assertEquals(Realms.methodOf(CultivationPath.SWORD, MethodGrade.YELLOW).id, r.methodId)
    }

    @Test
    fun bodyPathDoublesHp() {
        val s = fresh(15L)
        val d = at(s.disciples.first(), MajorRealm.FOUNDATION, 1).copy(path = null)
        val r = apply(only(s, d), GameCommand.ChoosePath(d.id, CultivationPath.BODY)).state.disciples.single()
        assertEquals(d.maxHp * 2, r.maxHp)
    }

    @Test
    fun scrollTeachesMatchingPathOnly() {
        val s = fresh(16L)
        val black = Realms.methodOf(CultivationPath.SWORD, MethodGrade.BLACK)
        val d = at(s.disciples.first(), MajorRealm.FOUNDATION, 1).copy(path = CultivationPath.ORTHODOX, methodId = Realms.methodOf(CultivationPath.ORTHODOX, MethodGrade.YELLOW).id)
        val st = only(s, d).copy(inventory = s.inventory + (black.scrollItem to 1))
        assertTrue(apply(st, GameCommand.LearnMethod(d.id, black.scrollItem)).notice!!.contains("należy do"))
        val sword = only(s, d.copy(path = CultivationPath.SWORD)).copy(inventory = s.inventory + (black.scrollItem to 1))
        val learned = apply(sword, GameCommand.LearnMethod(d.id, black.scrollItem)).state
        assertEquals(black.id, learned.disciples.single().methodId)
        assertEquals(0, learned.inventory[black.scrollItem])
    }

    @Test
    fun lawsGrowDuringMeditationAndBoostCombat() {
        var s = fresh(17L).copy(autoBreakthroughEnabled = false)
        val d = at(s.disciples.first(), MajorRealm.NASCENT_SOUL, 1).copy(laws = mapOf(DaoLaw.FIRE.polishName to 0.0), role = com.example.model.DiscipleRole.MEDITATING)
        s = only(s, d)
        repeat(500) { s = apply(s, GameCommand.Tick).state }
        val law = s.disciples.single().laws.values.single()
        assertTrue("Prawo: $law", law > 0.5)
        val mods = Modifiers.disciple(Modifiers.sect(s), s.disciples.single())
        assertTrue(mods.percent(Stat.COMBAT_DAMAGE) > 0)
    }

    @Test
    fun atMostTwoLawsFromNascentSoul() {
        val s = fresh(18L)
        val low = at(s.disciples.first(), MajorRealm.CORE_FORMATION, 1)
        assertTrue(apply(only(s, low), GameCommand.ChooseLaw(low.id, DaoLaw.FIRE)).notice!!.contains("Narodzin"))
        val d = at(s.disciples.first(), MajorRealm.NASCENT_SOUL, 1)
        var st = only(s, d)
        st = apply(st, GameCommand.ChooseLaw(d.id, DaoLaw.FIRE)).state
        st = apply(st, GameCommand.ChooseLaw(d.id, DaoLaw.WATER)).state
        assertTrue(apply(st, GameCommand.ChooseLaw(d.id, DaoLaw.TIME)).notice!!.contains("już"))
    }

    // --- Pigułki, odosobnienie, krew, Wniebowstąpienie ---

    @Test
    fun lowTierPillsWeakenAtHighRealms() {
        assertEquals(1.0, CultivationSystem.pillEfficiency(1, MajorRealm.QI_CONDENSATION), 1e-9)
        assertEquals(0.25, CultivationSystem.pillEfficiency(1, MajorRealm.FOUNDATION), 1e-9)
        assertEquals(1.0, CultivationSystem.pillEfficiency(6, MajorRealm.FOUNDATION), 1e-9)
    }

    @Test
    fun seclusionTriplesQiAndInterruptingHurts() {
        val s = fresh(19L).copy(autoBreakthroughEnabled = false)
        val d = at(s.disciples.first(), MajorRealm.FOUNDATION, 1)
        val secluded = apply(only(s, d), GameCommand.Seclude(d.id, 10)).state
        val x = secluded.disciples.single()
        assertEquals(s.currentDay + 10, x.secludedUntilDay)
        val normalGain = CultivationRules.meditationGain(d, Modifiers.disciple(Modifiers.sect(secluded), d.copy(secludedUntilDay = 0)))
        val boosted = CultivationRules.meditationGain(x, Modifiers.disciple(Modifiers.sect(secluded), x))
        assertTrue("$boosted vs $normalGain", boosted >= normalGain * 2.9)
        val broken = apply(secluded, GameCommand.EndSeclusion(d.id)).state.disciples.single()
        assertTrue(broken.injuredUntilDay > s.currentDay)
    }

    @Test
    fun bloodlineAwakeningNeedsRealmAndCatalyst() {
        val s = fresh(20L)
        val d = at(s.disciples.first(), MajorRealm.QI_CONDENSATION, 9)
        assertTrue(apply(only(s, d), GameCommand.AwakenBloodline(d.id)).notice!!.contains("wymaga"))
        val f = at(s.disciples.first(), MajorRealm.FOUNDATION, 1)
        val cat = Realms.bloodlineCatalyst(f.bloodline)
        val r = apply(only(s, f).copy(inventory = s.inventory + (cat to 1)), GameCommand.AwakenBloodline(f.id)).state
        assertEquals(1, r.disciples.single().bloodlineAwakening)
    }

    @Test
    fun ascensionRemovesDiscipleAndGrantsLegacy() {
        val s = fresh(21L)
        val d = at(s.disciples.first(), MajorRealm.MAHAYANA, 4, full = true).copy(maxHp = 1_000_000, currentHp = 1_000_000, defense = 100_000)
        val r = apply(only(s, d), GameCommand.Ascend(d.id)).state
        assertTrue(r.disciples.isEmpty())
        assertEquals(listOf(d.name), r.ascendedNames)
        assertTrue(Modifiers.sect(r).breakdown(Stat.QI_GAIN).any { it.contains("Wniebowstąpienia") })
    }

    @Test
    fun offlineAdvancesStagesButNotRealms() {
        val s = fresh(22L)
        val d = at(s.disciples.first(), MajorRealm.QI_CONDENSATION, 1).copy(role = com.example.model.DiscipleRole.MEDITATING)
        val r = apply(only(s, d), GameCommand.OfflineCatchUp(12 * 3_600_000L)).state.disciples.single()
        assertEquals(MajorRealm.QI_CONDENSATION, r.majorRealm)
        assertEquals(9, r.stage)
        assertTrue(r.realmProgress <= 99)
    }

    @Test
    fun ranksStillFollowLegacyRealm() {
        val d = at(fresh(23L).disciples.first(), MajorRealm.CORE_FORMATION, 2)
        assertEquals(CultivationRealm.CORE_FORMATION, d.realm)
        assertEquals(SectRank.DIRECT, DiscipleRules.naturalRank(d.realm))
        assertFalse(d.realm == CultivationRealm.FOUNDATION_LATE)
        assertTrue(FoundationQuality.HEAVENLY.qiMultiplier > 1)
        assertEquals(TimeOfDay.DAY, fresh(23L).timeOfDay)
    }
}
