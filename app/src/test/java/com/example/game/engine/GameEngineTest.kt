package com.example.game.engine

import com.example.model.CultivationRealm
import com.example.model.DiscipleRole
import com.example.model.FacilityType
import com.example.model.TimeOfDay
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class GameEngineTest {

    private fun run(state: GameState, vararg commands: GameCommand): GameState =
        commands.fold(state) { s, c -> GameEngine.apply(s, c).state }

    private fun ticks(state: GameState, n: Int): GameState =
        (1..n).fold(state) { s, _ -> GameEngine.apply(s, GameCommand.Tick).state }

    // --- Losowość ---

    @Test
    fun rngIsDeterministic() {
        val a = GameRng(42L)
        val b = GameRng(42L)
        repeat(1000) { assertEquals(a.nextLong(), b.nextLong()) }
    }

    @Test
    fun rngStreamsAreIndependent() {
        val combat = GameRng.stream(7L, 3L, "combat").nextLong()
        val events = GameRng.stream(7L, 3L, "events").nextLong()
        val nextStep = GameRng.stream(7L, 4L, "combat").nextLong()
        assertNotEquals(combat, events)
        assertNotEquals(combat, nextStep)
        assertEquals(combat, GameRng.stream(7L, 3L, "combat").nextLong())
    }

    @Test
    fun rngPercentIsRoughlyFair() {
        val rng = GameRng(9L)
        val hits = (1..100_000).count { rng.percent(30) }
        assertTrue("Wynik: $hits", hits in 29_000..31_000)
    }

    // --- Determinizm silnika ---

    @Test
    fun newGameIsDeterministicForSeed() {
        assertEquals(GameEngine.newGame(123L), GameEngine.newGame(123L))
        assertNotEquals(GameEngine.newGame(123L).disciples, GameEngine.newGame(124L).disciples)
    }

    @Test
    fun sameSeedAndCommandsGiveSameState() {
        val a = ticks(GameEngine.newGame(5L), 400)
        val b = ticks(GameEngine.newGame(5L), 400)
        assertEquals(a, b)
    }

    @Test
    fun everyCommandAdvancesStepCounter() {
        val s = GameEngine.newGame(1L)
        assertEquals(s.step + 1, GameEngine.apply(s, GameCommand.Tick).state.step)
        assertEquals(s.step + 1, GameEngine.apply(s, GameCommand.DismissEvent).state.step)
    }

    // --- Cykl gry ---

    @Test
    fun timeAdvancesAndDaysPass() {
        val s = ticks(GameEngine.newGame(2L), 40)
        assertTrue(s.currentDay >= 9)
        assertEquals(40L, s.tick)
    }

    @Test
    fun baseIncomeIsTwoStonesPerDisciple() {
        val s = GameEngine.newGame(3L).copy(territories = emptyList(), disciples = GameEngine.newGame(3L).disciples.map { it.copy(role = DiscipleRole.SECT_GUARD) })
        val after = GameEngine.apply(s, GameCommand.Tick).state
        assertEquals(s.spiritStones + 2 * s.disciples.size, after.spiritStones)
    }

    @Test
    fun meditationNeverExceedsHundredPercent() {
        val s = ticks(GameEngine.newGame(4L).copy(autoBreakthroughEnabled = false), 200)
        assertTrue(s.disciples.all { it.realmProgress in 0..100 })
        assertTrue(s.disciples.any { it.realmProgress == 100 })
    }

    @Test
    fun randomEventsOnlyOnEverySixteenthTickAndNotRepeated() {
        var s = GameEngine.newGame(6L)
        val seen = mutableListOf<String>()
        repeat(2000) {
            val r = GameEngine.apply(s, GameCommand.Tick).state
            if (r.activeEventId != null && s.activeEventId == null) {
                assertEquals(0L, r.tick % RandomEventPhase.EVERY_TICKS)
                seen += r.activeEventId!!
            }
            s = GameEngine.apply(r, GameCommand.DismissEvent).state
        }
        assertTrue("Wylosowano ${seen.size}", seen.size > 50)
        seen.windowed(RandomEventPhase.MEMORY + 1).forEach { w ->
            assertEquals("Powtórka w oknie $w", w.size, w.toSet().size)
        }
    }

    @Test
    fun autoBreakthroughHappensForReadyDisciples() {
        var s = GameEngine.newGame(8L)
        val start = s.disciples.associate { it.id to it.realm }
        s = ticks(s, 600)
        assertTrue(s.disciples.any { it.realm.ordinal > (start[it.id]?.ordinal ?: 0) })
    }

    // --- Komendy ---

    @Test
    fun recruitCostsStonesAndRespectsBalance() {
        val s = GameEngine.newGame(10L)
        val id = s.availableRecruits.first().id
        val after = run(s, GameCommand.Recruit(id))
        assertEquals(s.spiritStones - SectCommands.RECRUIT_COST, after.spiritStones)
        assertTrue(after.disciples.any { it.id == id })
        assertFalse(after.availableRecruits.any { it.id == id })

        val poor = s.copy(spiritStones = 10)
        val r = GameEngine.apply(poor, GameCommand.Recruit(id))
        assertEquals(10, r.state.spiritStones)
        assertNotNull(r.notice)
    }

    @Test
    fun brewingConsumesIngredientsAndProducesPill() {
        val s = GameEngine.newGame(11L)
        val recipe = GameContent.alchemyRecipes.first { it.requiredAlchemyChamberLevel <= 1 && it.secondaryHerbName == null }
        val rich = s.copy(spiritStones = 99_999, inventory = s.inventory + (recipe.requiredHerbName to 50))
        val after = run(rich, GameCommand.BrewPill(recipe.id))
        assertEquals(50 - recipe.requiredHerbCount, after.inventory[recipe.requiredHerbName])
        assertEquals((rich.inventory[recipe.pillName] ?: 0) + 1, after.inventory[recipe.pillName])
        assertEquals(rich.spiritStones - recipe.requiredSpiritStones, after.spiritStones)
    }

    @Test
    fun brewingRequiresChamberLevel() {
        val s = GameEngine.newGame(12L).copy(spiritStones = 99_999)
        val recipe = GameContent.alchemyRecipes.first { it.requiredAlchemyChamberLevel >= 6 }
        val r = GameEngine.apply(s.copy(inventory = s.inventory + (recipe.requiredHerbName to 99)), GameCommand.BrewPill(recipe.id))
        assertTrue(r.notice!!.contains("Zbyt niski poziom"))
        assertEquals(s.spiritStones, r.state.spiritStones)
    }

    @Test
    fun breakthroughConsumesPillAndFollowsChanceRules() {
        val s = GameEngine.newGame(13L)
        val d = s.disciples.first().copy(realmProgress = 100)
        val ready = s.copy(disciples = listOf(d) + s.disciples.drop(1))
        val r = GameEngine.apply(ready, GameCommand.AttemptBreakthrough(d.id, usePill = true))
        assertEquals((ready.inventory[GameContent.BREAKTHROUGH_PILL] ?: 0) - 1, r.state.inventory[GameContent.BREAKTHROUGH_PILL])
        val after = r.state.disciple(d.id)!!
        assertTrue(after.realmProgress == 0 || after.realmProgress == 40)
        assertEquals(
            40 + d.karmicLuck / 5 + 35 + 45,
            CultivationRules.breakthroughChance(d.copy(bloodline = com.example.model.BloodlineType.GOLDEN_DRAGON), 35, TimeOfDay.FULL_MOON)
        )
    }

    @Test
    fun breakthroughRequiresFullQi() {
        val s = GameEngine.newGame(14L)
        val d = s.disciples.first().copy(realmProgress = 50)
        val r = GameEngine.apply(s.copy(disciples = listOf(d)), GameCommand.AttemptBreakthrough(d.id, usePill = false))
        assertEquals(d, r.state.disciple(d.id))
    }

    @Test
    fun facilityUpgradeRaisesLevelAndCost() {
        val s = GameEngine.newGame(15L).copy(spiritStones = 99_999)
        val f = s.facilities.first { it.type == FacilityType.ALCHEMY_CHAMBER }
        val after = run(s, GameCommand.UpgradeFacility(FacilityType.ALCHEMY_CHAMBER))
        val g = after.facilities.first { it.type == FacilityType.ALCHEMY_CHAMBER }
        assertEquals(f.level + 1, g.level)
        assertEquals((f.upgradeCostStones * 1.8).toInt(), g.upgradeCostStones)
    }

    // --- Walka ---

    @Test
    fun combatEndsWithinRoundLimitAndAppliesRewardsOnce() {
        val s = GameEngine.newGame(16L)
        val region = s.regions.first()
        val strongest = s.disciples.maxBy { it.attack }
        var state = run(s, GameCommand.StartExpedition(region.id, strongest.id))
        assertNotNull(state.combat)
        var steps = 0
        while (state.combat?.finished == false && steps < 200) {
            state = GameEngine.apply(state, GameCommand.CombatStep).state
            steps++
        }
        val c = state.combat!!
        assertTrue(c.finished)
        assertTrue(steps <= ActiveCombat.MAX_ROUNDS * 2 + 2)
        val again = GameEngine.apply(state, GameCommand.CombatStep).state
        assertEquals("Po końcu walki nic się nie zmienia", state.spiritStones, again.spiritStones)
        if (c.outcome == true) {
            assertEquals(25, state.regions.first { it.id == region.id }.explorationProgress - region.explorationProgress)
            assertEquals(1, (state.inventory[region.bossBeast!!.dropItemName] ?: 0) - (s.inventory[region.bossBeast!!.dropItemName] ?: 0))
        }
        assertNull(run(state, GameCommand.CloseCombat).combat)
    }

    @Test
    fun ultimateNeedsQiAndManualVictoryCounts() {
        val s = GameEngine.newGame(17L)
        val started = run(s, GameCommand.StartExpedition(s.regions.first().id, s.disciples.first().id))
        val noQi = started.copy(combat = started.combat!!.copy(discipleQi = 10))
        assertEquals("Zbyt mało Qi na technikę ostateczną!", GameEngine.apply(noQi, GameCommand.CombatUltimate).notice)

        val almostDead = started.copy(combat = started.combat!!.copy(enemyHp = 1))
        val won = GameEngine.apply(almostDead, GameCommand.CombatManualAttack)
        assertEquals(true, won.state.combat!!.outcome)
        assertTrue(won.events.any { it is GameEvent.CombatEnded && it.victory })
    }

    // --- Pilnowanie zasad architektury ---

    @Test
    fun gameLogicDoesNotUseGlobalRandomOrClock() {
        val roots = listOf(File("src/main/java/com/example/game"), File("app/src/main/java/com/example/game"))
        val root = roots.firstOrNull { it.isDirectory } ?: return
        val forbidden = Regex("""\bRandom\.(next|Default)|\.random\(\)|\.shuffled\(\)|currentTimeMillis|nanoTime|UUID\.random""")
        val offenders = root.walkTopDown()
            .filter { it.isFile && it.extension == "kt" && it.name != "WuxiaGameViewModel.kt" }
            .flatMap { f -> f.readLines().mapIndexedNotNull { i, line -> if (forbidden.containsMatchIn(line)) "${f.name}:${i + 1}: ${line.trim()}" else null } }
            .toList()
        assertTrue("Globalna losowość lub zegar w logice gry:\n" + offenders.joinToString("\n"), offenders.isEmpty())
    }

    @Test
    fun startingRealmsMatchPreviousGame() {
        val realms = GameEngine.newGame(18L).disciples.map { it.realm }
        assertEquals(
            listOf(
                CultivationRealm.FOUNDATION_EARLY, CultivationRealm.QI_CONDENSATION_9, CultivationRealm.QI_CONDENSATION_5,
                CultivationRealm.QI_CONDENSATION_5, CultivationRealm.FOUNDATION_LATE, CultivationRealm.FOUNDATION_EARLY
            ),
            realms
        )
    }
}
