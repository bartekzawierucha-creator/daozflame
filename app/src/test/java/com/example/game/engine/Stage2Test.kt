package com.example.game.engine

import com.example.model.CultivationRealm
import com.example.model.FacilityType
import com.example.model.RootGrade
import com.example.model.SectRank
import com.example.model.TimeOfDay
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Etap 2: korzenie, cechy, rangi, lojalność, morale, jedzenie, egzaminy, turnieje. */
class Stage2Test {

    private fun fresh(seed: Long = 1L) = GameEngine.newGame(seed)
    private fun apply(s: GameState, c: GameCommand) = GameEngine.apply(s, c)

    /** Ustawia stan tuż przed świtem nowego dnia [day] i wykonuje jeden cykl. */
    private fun dawn(s: GameState, day: Int = s.currentDay + 1): StepResult =
        apply(s.copy(currentDay = day - 1, timeOfDay = TimeOfDay.FULL_MOON, phaseTick = TimePhase.TICKS_PER_PHASE - 1), GameCommand.Tick)

    // --- Korzenie ---

    @Test
    fun rootDistributionMatchesTable() {
        val rng = GameRng(99L)
        val n = 100_000
        val counts = (1..n).map { DiscipleRules.rollRoot(rng).grade }.groupingBy { it }.eachCount()
        RootGrade.entries.forEach { g ->
            val expected = g.permille / 1000.0
            val actual = (counts[g] ?: 0).toDouble() / n
            assertTrue("${g.name}: $actual vs $expected", kotlin.math.abs(actual - expected) < 0.01)
        }
    }

    @Test
    fun rootElementsMatchGrade() {
        val rng = GameRng(5L)
        repeat(2000) {
            val r = DiscipleRules.rollRoot(rng)
            val n = r.elements.size
            when (r.grade) {
                RootGrade.MIXED -> assertTrue(n in 4..5)
                RootGrade.TRIPLE -> assertEquals(3, n)
                RootGrade.DUAL -> assertEquals(2, n)
                else -> assertEquals(1, n)
            }
        }
    }

    // --- Migracja ---

    @Test
    fun oldSaveIsMigratedDeterministicallyAndOnce() {
        val old = fresh(3L).let { s ->
            s.copy(
                e2Initialized = false,
                disciples = s.disciples.map { it.copy(spiritRoot = null, traitIds = emptyList(), hiddenTraitIds = emptyList(), rank = SectRank.OUTER) }
            )
        }
        val a = GameEngine.normalize(old)
        val b = GameEngine.normalize(old)
        assertEquals(a, b)
        assertTrue(a.e2Initialized)
        assertTrue(a.disciples.all { it.spiritRoot != null && it.traitIds.isNotEmpty() })
        assertEquals(a, GameEngine.normalize(a))
        assertEquals(DiscipleRules.STARTING_FOOD, a.inventory[DiscipleRules.FOOD])
        // Rangi z królestw, z zachowaniem limitów.
        assertTrue(a.disciples.count { it.rank == SectRank.DIRECT } <= DiscipleRules.DIRECT_LIMIT)
        a.disciples.forEach { assertTrue(it.realm.ordinal >= it.rank.requiredRealm.ordinal) }
        // Obecni członkowie mają jawne cechy (poza ukrytym Zdrajcą).
        assertTrue(a.disciples.all { d -> d.hiddenTraitIds.all { it == "traitor" } })
    }

    // --- Rangi ---

    @Test
    fun promotionRequiresRealmAndFreeSlot() {
        val s = fresh(4L)
        val low = s.disciples.first().copy(id = "x", realm = CultivationRealm.QI_CONDENSATION_1, rank = SectRank.OUTER)
        val st = s.copy(disciples = s.disciples + low)
        assertTrue(apply(st, GameCommand.Promote("x")).notice!!.contains("wymaga królestwa"))

        val ready = low.copy(realm = CultivationRealm.FOUNDATION_EARLY)
        val promoted = apply(st.copy(disciples = s.disciples + ready), GameCommand.Promote("x")).state.disciple("x")!!
        assertEquals(SectRank.INNER, promoted.rank)
        assertEquals((ready.loyalty + 15).coerceAtMost(100), promoted.loyalty)

        val directs = (1..3).map { s.disciples.first().copy(id = "d$it", realm = CultivationRealm.CORE_FORMATION, rank = SectRank.DIRECT) }
        val candidate = s.disciples.first().copy(id = "c", realm = CultivationRealm.CORE_FORMATION, rank = SectRank.INNER)
        val full = s.copy(disciples = directs + candidate)
        assertTrue(apply(full, GameCommand.Promote("c")).notice!!.contains("limit"))
    }

    @Test
    fun jealousDisciplesResentPromotions() {
        val s = fresh(5L)
        val jealous = s.disciples.first().copy(id = "j", traitIds = listOf("jealous"), hiddenTraitIds = emptyList(), rank = SectRank.OUTER, loyalty = 70)
        val star = s.disciples.first().copy(id = "s", realm = CultivationRealm.FOUNDATION_EARLY, rank = SectRank.OUTER)
        val r = apply(s.copy(disciples = listOf(jealous, star)), GameCommand.Promote("s")).state
        assertEquals(60, r.disciple("j")!!.loyalty)
    }

    @Test
    fun techniqueTierIsGatedByRank() {
        val s = fresh(6L)
        val d = s.disciples.first().copy(rank = SectRank.OUTER, daoPoints = 99)
        val skill = GameContent.skills.first { it.martialSchool == d.martialSchool && it.tier == 2 }
        val r = apply(s.copy(disciples = listOf(d)), GameCommand.UnlockSkill(d.id, skill.id))
        assertTrue(r.notice!!.contains("wymaga rangi"))
    }

    @Test
    fun skillCostIsHigherWithoutRootAffinity() {
        val d = fresh(7L).disciples.first()
        val noAffinity = d.copy(martialSchool = com.example.model.MartialSchool.SWORD_IMMORTAL,
            spiritRoot = com.example.model.SpiritRoot(RootGrade.SINGLE, listOf(com.example.model.SpiritElement.FIRE)))
        assertEquals(4, DiscipleRules.skillCost(noAffinity, 3))
        val affinity = noAffinity.copy(spiritRoot = com.example.model.SpiritRoot(RootGrade.SINGLE, listOf(com.example.model.SpiritElement.METAL)))
        assertEquals(3, DiscipleRules.skillCost(affinity, 3))
    }

    // --- Dzień sekty: pensje, jedzenie, lojalność, morale ---

    @Test
    fun salariesAndFoodAreConsumedAtDawn() {
        val base = fresh(8L)
        val s = base.copy(
            morale = 50,
            territories = emptyList(),
            disciples = base.disciples.map { it.copy(role = com.example.model.DiscipleRole.SECT_GUARD) }
        )
        val n = s.disciples.size
        val salaries = s.disciples.sumOf { DiscipleRules.salary(it) }
        val upkeep = EdictCatalog.dailyUpkeep(s)
        val r = dawn(s).state
        assertEquals((s.inventory[DiscipleRules.FOOD] ?: 0) + DiscipleRules.dailyFoodProduction(n) - n, r.inventory[DiscipleRules.FOOD])
        assertEquals(s.spiritStones + 2 * n - upkeep - salaries - Upkeep.facilities(s), r.spiritStones)
    }

    @Test
    fun hungerHurtsMoraleAndQi() {
        val s = fresh(9L).let { it.copy(inventory = it.inventory + (DiscipleRules.FOOD to 0), morale = 60) }
        val r = dawn(s).state
        assertTrue("Morale ${r.morale}", r.morale <= 60 - DiscipleRules.HUNGER_MORALE + 4)
        assertEquals(r.currentDay, r.hungryOnDay)
        assertTrue(Modifiers.sect(r).breakdown(Stat.QI_GAIN).any { it.contains("Głód") })
    }

    @Test
    fun unpaidSalariesLowerLoyalty() {
        val s = fresh(10L).copy(spiritStones = 0)
        val before = s.disciples.associate { it.id to it.loyalty }
        val r = dawn(s).state
        r.disciples.forEach { d ->
            val floor = if ("loyal" in d.traitIds) 60 else 0
            assertTrue(d.loyalty <= maxOf(before[d.id]!! - 5, floor) || d.loyalty == floor)
        }
        assertNotNull(dawn(s).notice)
    }

    @Test
    fun disloyalDisciplesEventuallyDesert() {
        var s = fresh(11L)
        s = s.copy(disciples = s.disciples.map { it.copy(loyalty = 0, traitIds = listOf("lazy"), hiddenTraitIds = emptyList()) })
        val start = s.disciples.size
        repeat(80) { s = dawn(s).state }
        assertTrue("Zostało ${s.disciples.size} z $start", s.disciples.size < start)
    }

    @Test
    fun moraleDriftsTowardFifty() {
        val s = fresh(12L).copy(morale = 90, disciples = fresh(12L).disciples.map { it.copy(traitIds = listOf("lazy")) })
        assertEquals(88, dawn(s).state.morale)
    }

    // --- Egzamin i rekrutacja ---

    @Test
    fun examOpensEveryThirtyDaysAndCloses() {
        val s = fresh(13L).copy(availableRecruits = emptyList(), examOpenUntilDay = 0)
        val opened = dawn(s, day = 30).state
        assertTrue(opened.availableRecruits.size >= 6)
        assertEquals(30 + DiscipleRules.EXAM_OPEN_DAYS - 1, opened.examOpenUntilDay)
        assertTrue(opened.availableRecruits.all { it.spiritRoot != null && it.hiddenTraitIds.isNotEmpty() })
        val closed = dawn(opened, day = 30 + DiscipleRules.EXAM_OPEN_DAYS).state
        assertTrue(closed.availableRecruits.isEmpty())
    }

    @Test
    fun firstTwoAdmissionsAreFreeThenCostRises() {
        var s = fresh(14L).copy(spiritStones = 5000)
        val ids = s.availableRecruits.map { it.id }
        val costs = ids.take(4).map { id ->
            val before = s.spiritStones
            s = apply(s, GameCommand.Recruit(id)).state
            before - s.spiritStones
        }
        assertEquals(listOf(0, 0, 600, 900), costs)
    }

    @Test
    fun recruitmentOnlyDuringExam() {
        val s = fresh(15L).copy(examOpenUntilDay = 0)
        val r = apply(s, GameCommand.Recruit(s.availableRecruits.first().id))
        assertTrue(r.notice!!.contains("zamknięty"))
        assertTrue(apply(s, GameCommand.RefreshRecruits).notice!!.contains("egzamin"))
    }

    @Test
    fun rootTestRevealsHiddenTraits() {
        val s = fresh(16L)
        val c = s.availableRecruits.first()
        val r = apply(s, GameCommand.TestCandidate(c.id))
        val tested = r.state.availableRecruits.first { it.id == c.id }
        assertTrue(tested.hiddenTraitIds.isEmpty())
        assertEquals(s.spiritStones - DiscipleRules.ROOT_TEST_COST, r.state.spiritStones)
    }

    // --- Decyzje patriarchy ---

    @Test
    fun punishAndPardonTradeLoyaltyForMorale() {
        val s = fresh(17L).copy(morale = 50)
        val d = s.disciples.first().copy(traitIds = listOf("lazy"), loyalty = 70)
        val st = s.copy(disciples = listOf(d))
        val punished = apply(st, GameCommand.Punish(d.id)).state
        assertEquals(20, punished.disciple(d.id)!!.loyalty)
        assertEquals(60, punished.morale)
        val pardoned = apply(st, GameCommand.Pardon(d.id)).state
        assertEquals(80, pardoned.disciple(d.id)!!.loyalty)
        assertEquals(45, pardoned.morale)
    }

    @Test
    fun feastNeedsFoodAndHasCooldown() {
        val s = fresh(18L).copy(morale = 40)
        val fed = apply(s, GameCommand.HoldFeast).state
        assertEquals(70, fed.morale)
        assertTrue(apply(fed, GameCommand.HoldFeast).notice!!.contains("za"))
        val noFood = s.copy(inventory = s.inventory + (DiscipleRules.FOOD to 0))
        assertTrue(apply(noFood, GameCommand.HoldFeast).notice!!.contains("wymaga"))
    }

    @Test
    fun tournamentRunsEverySixtyDays() {
        val s = fresh(19L).copy(morale = 50)
        val r = dawn(s, day = 60)
        assertTrue(r.state.sectReputation > s.sectReputation)
        assertTrue(r.state.morale > 50)
    }

    @Test
    fun capacityLimitsAdmissions() {
        val s = fresh(21L)
        val full = s.copy(disciples = List(DiscipleRules.capacity(s)) { i -> s.disciples.first().copy(id = "f$i") })
        assertTrue(apply(full, GameCommand.Recruit(s.availableRecruits.first().id)).notice!!.contains("mieści"))
    }

    @Test
    fun cultivationPavilionLevelSetsElderSlots() {
        val s = fresh(20L)
        val lvl3 = s.copy(facilities = s.facilities.map { if (it.type == FacilityType.CULTIVATION_PAVILION) it.copy(level = 3) else it })
        assertEquals(3, DiscipleRules.rankLimit(lvl3, SectRank.ELDER))
        assertFalse(DiscipleRules.canPromote(s, s.disciples.first().copy(realm = CultivationRealm.QI_CONDENSATION_1, rank = SectRank.OUTER)))
    }
}
