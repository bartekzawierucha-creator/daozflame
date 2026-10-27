package com.example.game.engine

import com.example.game.content.StoryCatalog
import com.example.model.TimeOfDay
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Etap 7: fabuła i życie sekty. */
class Stage7Test {

    private fun fresh(seed: Long = 1L) = GameEngine.newGame(seed)
    private fun apply(s: GameState, c: GameCommand) = GameEngine.apply(s, c)
    private fun nextDay(s: GameState): GameState =
        apply(s.copy(timeOfDay = TimeOfDay.FULL_MOON, phaseTick = TimePhase.TICKS_PER_PHASE - 1), GameCommand.Tick).state

    /** Gracz-automat: wybiera pierwszą dostępną odpowiedź w każdym zdarzeniu. */
    private fun answer(s: GameState): GameState {
        val e = GameContent.script(s.activeEventId) ?: return s
        val i = e.choices.indexOfFirst { EventScripts.allMet(s, it.conditions) }.coerceAtLeast(0)
        return apply(s, GameCommand.EventChoice(i)).state
    }

    private fun playStory(seed: Long, karma: Int, killChosenOnDay: Int? = null): GameState {
        var s = fresh(seed).let { st -> st.copy(spiritStones = 50_000, sectReputation = 1_000,
            disciples = st.disciples.mapIndexed { i, d -> if (i == 0) d.copy(majorRealm = com.example.model.MajorRealm.CORE_FORMATION) else d }) }
        var day = 0
        while (Story.ending(s) == 0 && day++ < 1_000) {
            // Karma i Złoty Rdzeń jednego ucznia wymuszane – test sprawdza fabułę, nie tempo kultywacji.
            s = nextDay(s.copy(karma = karma, spiritStones = s.spiritStones.coerceAtLeast(5_000),
                disciples = s.disciples.mapIndexed { i, d -> if (i == 0) d.copy(majorRealm = com.example.model.MajorRealm.CORE_FORMATION) else d }))
            repeat(4) { s = answer(apply(s, GameCommand.Tick).state) }
            if (killChosenOnDay == s.currentDay) s.disciple(s.chosenId)?.let { s = Lifecycle.die(s, it, "test") }
        }
        return s
    }

    @Test
    fun storyContentIsValid() {
        assertEquals(emptyList<String>(), EventScripts.validate(GameContent.scripts))
    }

    @Test
    fun everyEndingIsReachable() {
        assertEquals(1, Story.ending(playStory(1L, 80)))
        assertEquals(2, Story.ending(playStory(2L, -80)))
        assertEquals(3, Story.ending(playStory(3L, 0)))
    }

    @Test
    fun storySurvivesDeathOfTheChosen() {
        val s = playStory(4L, 0, killChosenOnDay = 200)
        assertEquals(3, Story.ending(s))
        assertTrue(s.ancestors.any { it.cause == "test" })
        assertNotNull(s.disciple(s.chosenId))
    }

    @Test
    fun dismissedStoryEventComesBackNextDay() {
        val s = EventScripts.activate(fresh(5L), GameContent.script("story_2_2")!!)
        val r = apply(s, GameCommand.DismissEvent).state
        assertNull(r.activeEventId)
        assertTrue(r.scheduledEvents.any { it.eventId == "story_2_2" && it.day == s.currentDay + 1 })
    }

    // --- Cykl życia ---

    @Test
    fun oldSavesGiveCurrentDisciplesYoungAge() {
        val s = fresh(6L)
        val old = s.copy(e7Initialized = false, disciples = s.disciples.map { it.copy(ageYears = 999) })
        assertTrue(GameEngine.normalize(old).disciples.all { it.ageYears in 16..25 })
    }

    @Test
    fun disciplesAgeAndDieOfOldAgeIntoAncestorHall() {
        val s = fresh(7L)
        val d = s.disciples.first()
        val old = s.copy(currentDay = Lifecycle.YEAR_DAYS - 1, disciples = s.disciples.map { if (it.id == d.id) it.copy(ageYears = Lifecycle.lifespan(it) - 1) else it })
        val r = nextDay(old)
        assertNull(r.disciple(d.id))
        assertEquals(d.name, r.ancestors.last().name)
        val ok = r.disciples.first()
        assertEquals(old.disciple(ok.id)!!.ageYears + 1, ok.ageYears)
    }

    @Test
    fun ancestorsOfFoundationGiveSectQi() {
        val s = fresh(8L).copy(ancestors = List(20) { com.example.model.AncestorRecord("x$it", "Fundamenty", com.example.model.MajorRealm.FOUNDATION.ordinal, 1, "starość") })
        assertTrue(Modifiers.sect(s).breakdown(Stat.QI_GAIN).any { it == "Sala Przodków: +15%" })
    }

    @Test
    fun mentorGivesQiAndHasStudentLimit() {
        val s = fresh(9L)
        val sorted = s.disciples.sortedByDescending { (it.majorRealm?.ordinal ?: 0) * 10 + it.stage }
        val mentor = sorted.first()
        val students = sorted.drop(2).filter { (it.majorRealm?.ordinal ?: 0) < (mentor.majorRealm?.ordinal ?: 0) }
        var st = s
        students.take(2).forEach { st = apply(st, GameCommand.AssignMentor(it.id, mentor.id)).state }
        assertTrue(Modifiers.disciple(Modifiers.sect(st), st.disciple(students[0].id)!!).breakdown(Stat.QI_GAIN).any { it.startsWith("Mistrz") })
        if (students.size > 2) assertTrue(apply(st, GameCommand.AssignMentor(students[2].id, mentor.id)).notice!!.contains("ma już"))
        assertTrue(apply(st, GameCommand.AssignMentor(mentor.id, students[0].id)).notice!!.contains("wyżej"))
    }

    @Test
    fun companionsHaveChildrenThatOftenInheritRoots() {
        var inherited = 0
        val n = 60
        (1L..n.toLong()).forEach { seed ->
            val s = fresh(seed).let { st -> st.copy(disciples = st.disciples.map { it.copy(loyalty = 90) }) }
            val (a, b) = s.disciples.take(2)
            var st = apply(s, GameCommand.BindCompanions(a.id, b.id)).state
            st = st.copy(currentDay = st.currentDay + Lifecycle.YEAR_DAYS - 1, availableRecruits = emptyList())
            st = nextDay(st)
            val kid = st.availableRecruits.single()
            if (kid.spiritRoot == a.spiritRoot || kid.spiritRoot == b.spiritRoot) inherited++
        }
        assertTrue("dziedziczenie $inherited/$n", inherited.toDouble() / n in 0.45..0.8)
    }

    // --- Tożsamość sekty ---

    @Test
    fun karmaPathThresholds() {
        val s = fresh(10L)
        assertEquals("Sekta Prawa", Identity.karmaPath(s.copy(karma = 50)))
        assertEquals("Sekta Demoniczna", Identity.karmaPath(s.copy(karma = -50)))
        assertEquals("Sekta Równowagi", Identity.karmaPath(s.copy(karma = 49)))
        assertTrue(Modifiers.sect(s.copy(karma = -60)).breakdown(Stat.COMBAT_DAMAGE).any { it.startsWith("Sekta Demoniczna") })
    }

    @Test
    fun patronTempleAndCooldown() {
        var s = fresh(11L).copy(spiritStones = 100_000)
        s = apply(s, GameCommand.ChoosePatron("p_war")).state
        assertTrue(apply(s, GameCommand.ChoosePatron("p_moon")).notice!!.contains("dni"))
        val before = Modifiers.sect(s).percent(Stat.COMBAT_DAMAGE)
        s = apply(s, GameCommand.UpgradeTemple).state
        assertTrue(Modifiers.sect(s).percent(Stat.COMBAT_DAMAGE) > before)
        val later = apply(s.copy(currentDay = s.currentDay + Identity.PATRON_COOLDOWN), GameCommand.ChoosePatron("p_moon")).state
        assertEquals("p_moon", later.patronId)
    }

    @Test
    fun treasuresHaveFiveSlotsAndCanBeLostInRaids() {
        val art = GameContent.forgingRecipes.first().artifactResult
        var s = fresh(12L).copy(vaultArtifacts = List(6) { art })
        repeat(5) { s = apply(s, GameCommand.PlaceTreasure(0)).state }
        assertEquals(5, s.treasury.size)
        assertTrue(apply(s, GameCommand.PlaceTreasure(0)).notice!!.contains("5"))
        assertTrue(Modifiers.sect(s).breakdown(Stat.QI_GAIN).any { it.startsWith("Skarby sekty") })
        val lost = (1L..40L).count { seed -> Identity.raidLoss(s, EngineContext(seed, seed)).second != null }
        assertTrue(lost in 4..24)
    }

    @Test
    fun chronicleIsCompressedIntoChapters() {
        var s = fresh(13L)
        repeat(100) { s = WorldAI.note(s.copy(currentDay = it + 1), "wydarzenie $it") }
        assertTrue(s.chronicle.size <= WorldAI.CHRONICLE)
        assertTrue(s.chronicleChapters.isNotEmpty())
        assertTrue(s.chronicleChapters.first().startsWith("Rozdział 1"))
    }
}

/** Etap 7b: miejsca, istoty, codzienność. */
class Stage7bTest {
    private fun fresh(seed: Long = 1L) = GameEngine.newGame(seed)
    private fun apply(s: GameState, c: GameCommand) = GameEngine.apply(s, c)
    private fun nextDay(s: GameState): GameState =
        apply(s.copy(timeOfDay = com.example.model.TimeOfDay.FULL_MOON, phaseTick = TimePhase.TICKS_PER_PHASE - 1), GameCommand.Tick).state

    @Test
    fun fengShuiIsAppendedAsRulesV2WithoutTouchingV1() {
        val v1 = RulesGen.generate(5L)
        val s = fresh(5L)
        assertEquals(2, s.rules!!.version)
        assertEquals(6, s.rules!!.fengShui.size)
        assertEquals(v1.breath, s.rules!!.breath)
        assertEquals(v1.constellations, s.rules!!.constellations)
        assertEquals(emptyList<String>(), RulesGen.validate(s.rules!!))
    }

    @Test
    fun swappingBuildingsChangesHarmonyAndHasCooldown() {
        var s = fresh(6L).copy(spiritStones = 100_000)
        val before = FengShui.total(s)
        val types = com.example.model.FacilityType.entries
        var best = s
        for (a in types) for (b in types) if (a < b) {
            val r = apply(s, GameCommand.SwapBuildings(a, b)).state
            if (FengShui.total(r) > FengShui.total(best)) best = r
        }
        assertTrue(FengShui.total(best) >= before)
        s = apply(s, GameCommand.SwapBuildings(types[0], types[1])).state
        assertTrue(apply(s, GameCommand.SwapBuildings(types[2], types[3])).notice!!.contains("dni"))
        assertTrue(s.journal.keys.any { it.startsWith("fengshui:") })
    }

    @Test
    fun dungeonGoesDeeperWithTwoAttemptsPerDay() {
        var s = fresh(7L).let { it.copy(disciples = it.disciples.map { d -> d.copy(attack = 9_999, defense = 9_999) }) }
        val id = s.disciples.first().id
        repeat(2) { s = apply(s, GameCommand.DelveDungeon(id)).state }
        assertEquals(2, s.dungeonDepth)
        assertTrue(apply(s, GameCommand.DelveDungeon(id)).notice!!.contains("jutra"))
    }

    @Test
    fun libraryCopiesOnlyLowerScrollsAndCavesBoostSeclusion() {
        val scroll = Realms.methodOf(com.example.model.CultivationPath.SWORD, com.example.model.MethodGrade.BLACK).scrollItem
        val high = Realms.methodOf(com.example.model.CultivationPath.SWORD, com.example.model.MethodGrade.HEAVENLY).scrollItem
        val s = fresh(8L).copy(spiritStones = 100_000, inventory = mapOf(scroll to 1, high to 1, Market.SPIRIT_PAPER to 10))
        assertEquals(2, apply(s, GameCommand.CopyScroll(scroll)).state.inventory[scroll])
        assertTrue(apply(s, GameCommand.CopyScroll(high)).notice!!.contains("nie da się"))
        val caved = apply(s, GameCommand.UpgradeCave).state
        val d = caved.disciples.first().copy(secludedUntilDay = caved.currentDay + 5)
        val st = caved.copy(disciples = listOf(d) + caved.disciples.drop(1))
        assertTrue(Modifiers.disciple(Modifiers.sect(st), d).breakdown(Stat.QI_GAIN).any { it.startsWith("Jaskinia") })
    }

    @Test
    fun eggsHatchAndBeastsStopAtThirdStage() {
        var s = fresh(9L).let { it.copy(inventory = it.inventory + (Creatures.EGG to 1), spiritStones = 100_000) }
        s = apply(s, GameCommand.IncubateEgg).state
        val n = s.sectBeasts.size
        repeat(Creatures.HATCH_DAYS) { s = nextDay(s) }
        assertEquals(n + 1, s.sectBeasts.size)
        val b = s.sectBeasts.first().copy(tier = 3, intimacy = 100)
        s = s.copy(sectBeasts = listOf(b) + s.sectBeasts.drop(1))
        assertTrue(apply(s, GameCommand.EvolveBeast(b.id)).notice!!.contains("trzeci"))
    }

    @Test
    fun mountGivesCombatBonus() {
        val s = fresh(10L)
        val b = s.sectBeasts.first().copy(intimacy = 80)
        val d = s.disciples.first()
        val r = apply(s.copy(sectBeasts = listOf(b) + s.sectBeasts.drop(1)), GameCommand.MountBeast(b.id, d.id)).state
        assertTrue(Modifiers.disciple(Modifiers.sect(r), r.disciple(d.id)!!).breakdown(Stat.COMBAT_DAMAGE).any { it.startsWith("Wierzchowiec") })
    }

    @Test
    fun fusionNeedsDoubleConfirmationAndSometimesExplodes() {
        val base = fresh(11L).copy(flames = com.example.game.WuxiaGenerator.getGeocentricFlames().take(4))
        assertTrue(apply(base, GameCommand.FuseFlames("flame_1", "flame_2", true, false)).notice!!.contains("dwukrotnie"))
        var exploded = 0
        var fused = 0
        (1L..60L).forEach { seed ->
            val r = apply(base.copy(worldSeed = seed), GameCommand.FuseFlames("flame_1", "flame_2", true, true)).state
            if (r.flames.none { it.id == "flame_1" || it.id == "flame_2" }) exploded++ else fused++
        }
        assertTrue("wybuchy $exploded/60", exploded in 8..30)
        val ok = (1L..60L).map { apply(base.copy(worldSeed = it), GameCommand.FuseFlames("flame_1", "flame_2", true, true)).state }.first { st -> st.flames.size == 3 }
        val kept = ok.flames.first { it.id == "flame_2" || it.id == "flame_1" }
        assertEquals(500, ok.flameHeat[kept.id])
    }

    @Test
    fun devourAndFlameSpirit() {
        val s = fresh(12L).copy(flames = com.example.game.WuxiaGenerator.getGeocentricFlames().filter { it.id in setOf("flame_3", "flame_10") }.map { it.copy(refinementLevel = 8) })
        assertTrue(apply(s, GameCommand.DevourFlame("flame_10", "flame_3")).notice!!.contains("Słabszy"))
        val r = apply(s, GameCommand.DevourFlame("flame_3", "flame_10")).state
        assertEquals(9, r.flames.single().refinementLevel)
        val (woke, notes) = Creatures.awakenFlameSpirits(r)
        assertTrue("flame_3" in woke.flameSpirits)
        assertTrue(notes.single().contains("duch"))
    }

    @Test
    fun artifactSpiritAwakensAtPlusSeven() {
        val recipe = GameContent.forgingRecipes.first()
        val art = recipe.artifactResult.copy(plus = 6)
        val s = fresh(13L).copy(spiritStones = 10_000_000, vaultArtifacts = listOf(art), inventory = mapOf(recipe.requiredMineralName to 9_999))
        var st = s
        var tries = 0
        while (st.vaultArtifacts.firstOrNull()?.plus == 6 && tries++ < 50) st = apply(st.copy(worldSeed = tries.toLong()), GameCommand.UpgradeArtifact(0)).state
        val up = st.vaultArtifacts.firstOrNull()
        if (up != null && up.plus == 7) assertNotNull(up.spirit)
    }

    @Test
    fun plagueHasSpreadLimit() {
        var s = fresh(14L)
        s = s.copy(disciples = s.disciples.mapIndexed { i, d -> if (i == 0) d.copy(illUntilDay = 10_000) else d })
        repeat(300) { s = DailyLife.daily(s.copy(currentDay = s.currentDay + 1, step = s.step + 1), EngineContext(s.worldSeed, s.step + 1)).first }
        assertTrue(s.disciples.count { DailyLife.sick(s, it) } <= DailyLife.MAX_SICK)
        val sick = s.disciples.first()
        val cured = apply(s.copy(spiritStones = 1_000), GameCommand.Cure(sick.id)).state
        assertTrue(!DailyLife.sick(cured, cured.disciple(sick.id)!!))
    }

    @Test
    fun artsAndCursesAffectModifiers() {
        val s = fresh(15L)
        var st = apply(s, GameCommand.AssignArt(s.disciples.first().id, com.example.model.DaoArt.GO)).state
        val d = st.disciples.first()
        assertTrue(Modifiers.disciple(Modifiers.sect(st), d).flat(Stat.BREAKTHROUGH_CHANCE) >= 3.0)
        st = st.copy(disciples = st.disciples.map { it.copy(cursed = true) }, spiritStones = 5_000)
        assertTrue(Modifiers.disciple(Modifiers.sect(st), st.disciples.first()).breakdown(Stat.QI_GAIN).any { it.startsWith("Klątwa") })
        val clean = apply(st, GameCommand.Cleanse(d.id)).state
        assertTrue(!clean.disciple(d.id)!!.cursed)
    }

    @Test
    fun starsForecastAndOncePerDay() {
        val s = fresh(16L).copy(spiritStones = 1_000, currentDay = 3)
        val r = apply(s, GameCommand.ReadStars)
        assertTrue(r.notice!!.startsWith("🔭"))
        assertTrue(apply(r.state, GameCommand.ReadStars).notice!!.contains("dziś"))
    }
}
