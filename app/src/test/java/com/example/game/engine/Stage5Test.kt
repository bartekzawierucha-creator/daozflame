package com.example.game.engine

import com.example.model.CondType
import com.example.model.Condition
import com.example.model.Effect
import com.example.model.EffectType
import com.example.model.ScriptChoice
import com.example.model.ScriptEvent
import com.example.model.TimeOfDay
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Etap 5: silnik skryptów zdarzeń. */
class Stage5Test {

    private fun fresh(seed: Long = 1L) = GameEngine.newGame(seed)
    private fun apply(s: GameState, c: GameCommand) = GameEngine.apply(s, c)
    private fun nextDay(s: GameState): GameState =
        apply(s.copy(timeOfDay = TimeOfDay.FULL_MOON, phaseTick = TimePhase.TICKS_PER_PHASE - 1, activeEventId = null), GameCommand.Tick).state

    @Test
    fun contentPassesValidator() {
        assertEquals(emptyList<String>(), EventScripts.validate(GameContent.scripts))
    }

    @Test
    fun allLegacyEventsWereMigrated() {
        assertTrue(GameContent.scripts.count { it.chain == null } >= 50)
        val legacy = GameContent.event("ev_1")!!
        assertEquals(350, legacy.choices[0].stoneChange)
    }

    @Test
    fun validatorFindsMissingTargetsAndEndlessLoops() {
        val loopA = ScriptEvent("a", choices = listOf(ScriptChoice(effects = listOf(Effect(EffectType.SCHEDULE, "b", 1)))))
        val loopB = ScriptEvent("b", random = false, choices = listOf(ScriptChoice(effects = listOf(Effect(EffectType.SCHEDULE, "a", 1)))))
        val broken = ScriptEvent("c", choices = listOf(ScriptChoice(effects = listOf(Effect(EffectType.SCHEDULE, "zzz", 1)))))
        val orphan = ScriptEvent("d", random = false, choices = listOf(ScriptChoice()))
        val errors = EventScripts.validate(listOf(loopA, loopB, broken, orphan))
        assertTrue(errors.any { "zzz" in it })
        assertTrue(errors.any { "a należy do pętli" in it })
        assertTrue(errors.any { "d nie wypada" in it })
    }

    @Test
    fun conditionsAreEvaluated() {
        val s = fresh().copy(currentDay = 10, sectReputation = 100, flags = mapOf("x" to 2), timeOfDay = TimeOfDay.FULL_MOON)
        assertTrue(EventScripts.met(s, Condition(CondType.MIN_DAY, value = 10)))
        assertTrue(!EventScripts.met(s, Condition(CondType.MIN_REPUTATION, value = 101)))
        assertTrue(EventScripts.met(s, Condition(CondType.FLAG_AT_LEAST, "x", 2)))
        assertTrue(EventScripts.met(s, Condition(CondType.FLAG_BELOW, "y", 1)))
        assertTrue(EventScripts.met(s, Condition(CondType.TIME_OF_DAY, "NIGHT")))
        assertTrue(EventScripts.met(s, Condition(CondType.SEASON, "SPRING")))
    }

    @Test
    fun chainSchedulesNextLinkAndItArrivesOnTime() {
        val s = fresh(2L).copy(currentDay = 5, spiritStones = 1000)
        val started = EventScripts.activate(s, GameContent.script("chain_hermit_1")!!)
        val helped = apply(started, GameCommand.EventChoice(0)).state
        assertEquals(1, helped.flags["hermit_helped"])
        assertEquals(listOf(com.example.model.ScheduledEvent(11, "chain_hermit_2")), helped.scheduledEvents)
        var st = helped
        while (st.currentDay < 11) st = nextDay(st)
        val after = apply(st, GameCommand.Tick).state
        assertEquals("chain_hermit_2", after.activeEventId)
        assertTrue(after.scheduledEvents.isEmpty())
    }

    @Test
    fun onceEventsNeverRepeat() {
        val s = fresh(3L).copy(currentDay = 30)
        val e = GameContent.script("chain_flame_1")!!
        assertTrue(EventScripts.eligible(s, e))
        val seen = EventScripts.activate(s, e)
        assertTrue(!EventScripts.eligible(seen, e))
    }

    @Test
    fun chainLinksNeverFireRandomly() {
        var s = fresh(4L).copy(currentDay = 40, sectReputation = 5_000)
        val seen = mutableSetOf<String>()
        repeat(3_000) {
            s = apply(s, GameCommand.Tick).state
            s.activeEventId?.let { seen += it }
            s = s.copy(activeEventId = null, scheduledEvents = emptyList())
        }
        val links = GameContent.scripts.filter { !it.random }.map { it.id }
        assertTrue(seen.intersect(links.toSet()).isEmpty())
        assertTrue(seen.size > 20)
    }

    @Test
    fun lockedChoiceIsRefusedAndMarked() {
        val s = EventScripts.activate(fresh(5L).copy(sectReputation = 0), GameContent.script("chain_hermit_3")!!)
        assertTrue(GameContent.event(s.activeEventId, s)!!.choices[2].text.contains("niedostępne"))
        val r = apply(s, GameCommand.EventChoice(2))
        assertEquals("chain_hermit_3", r.state.activeEventId)
    }

    @Test
    fun effectsChangeRivalRelationsAndKarma() {
        val s = EventScripts.activate(fresh(6L), GameContent.script("chain_hermit_3")!!)
        val before = s.rivalSects.first { it.id == "sect_shadow" }.relations
        val r = apply(s, GameCommand.EventChoice(1)).state
        assertEquals((before + 20).coerceIn(-100, 100), r.rivalSects.first { it.id == "sect_shadow" }.relations)
        assertEquals(s.karma - 15, r.karma)
        assertNull(r.activeEventId)
    }

    @Test
    fun brokenLinkIsDroppedWhenConditionsFail() {
        val s = fresh(7L).copy(scheduledEvents = listOf(com.example.model.ScheduledEvent(1, "nie_istnieje")))
        val r = apply(s, GameCommand.Tick).state
        assertTrue(r.scheduledEvents.isEmpty())
    }

    // --- 5a: wyprawy ---

    @Test
    fun expeditionMapIsDeterministicAndHasThreeToFiveLayers() {
        val s = fresh(10L)
        val r = s.regions.first()
        val a = Expeditions.generate(s, r)
        assertEquals(a, Expeditions.generate(s, r))
        assertTrue(a.size in 3..5)
        assertTrue(a.all { it.size == 2 })
        val sizes = (1L..40L).map { Expeditions.generate(fresh(it), r).size }.toSet()
        assertTrue(sizes.size >= 2)
    }

    @Test
    fun manualExpeditionWalksLayersThenBossPaysLoot() {
        var s = fresh(11L)
        val d = s.disciples.maxBy { it.attack }
        val region = s.regions.first()
        s = apply(s, GameCommand.BeginExpedition(region.id, d.id)).state
        val run = s.expedition!!
        repeat(run.layers.size) {
            if (s.expedition != null) {
                val layer = s.expedition!!.layers[s.expedition!!.layer]
                val safe = layer.indexOfFirst { it.type != com.example.model.ExpNodeType.FIGHT && it.type != com.example.model.ExpNodeType.TRAP }.coerceAtLeast(0)
                s = apply(s, GameCommand.ChooseNode(safe)).state
            }
        }
        val loot = s.expedition!!.loot
        s = apply(s, GameCommand.EnterLair).state
        assertTrue(s.expedition!!.atBoss)
        s = s.copy(combat = s.combat!!.copy(enemyHp = 1))
        s = apply(s, GameCommand.CombatManualAttack).state
        assertNull(s.expedition)
        loot.filterKeys { !it.startsWith("@") }.forEach { (k, v) -> assertTrue((s.inventory[k] ?: 0) >= v) }
    }

    @Test
    fun autoModeGivesLessLootThanManual() {
        assertEquals(0.8, Expeditions.AUTO_LOOT, 1e-9)
        val s = fresh(12L).let { it.copy(disciples = it.disciples.map { d -> d.copy(attack = 5_000, defense = 2_000, currentHp = d.maxHp) }) }
        val d = s.disciples.first()
        val r = apply(s, GameCommand.BeginExpedition(s.regions.first().id, d.id, auto = true))
        assertNull(r.state.expedition)
        assertTrue(r.state.spiritStones > s.spiritStones)
        assertTrue(r.notice!!.contains("auto"))
    }

    @Test
    fun retreatKeepsHalf() {
        val s = fresh(13L)
        val d = s.disciples.first()
        val started = apply(s, GameCommand.BeginExpedition(s.regions.first().id, d.id)).state
        val withLoot = started.copy(expedition = started.expedition!!.copy(loot = mapOf("Duchowy Nefryt" to 10), stones = 100))
        val r = apply(withLoot, GameCommand.RetreatExpedition).state
        assertEquals((s.inventory["Duchowy Nefryt"] ?: 0) + 5, r.inventory["Duchowy Nefryt"])
        assertEquals(s.spiritStones + 50, r.spiritStones)
    }

    @Test
    fun ringRaisesLootSlots() {
        val s = fresh(14L)
        assertEquals(Expeditions.SLOTS, Expeditions.slots(s))
        val rich = s.copy(spiritStones = 5_000, inventory = s.inventory + (Expeditions.RING_MINERAL to 5))
        val r = apply(rich, GameCommand.CraftRing).state
        assertEquals(Expeditions.RING_SLOTS, Expeditions.slots(r))
    }

    // --- 5a: żywioły ---

    @Test
    fun elementCyclesGiveThirtyPercent() {
        assertTrue(Elements.overcomes(com.example.model.SpiritElement.WATER, com.example.model.SpiritElement.FIRE))
        assertTrue(!Elements.overcomes(com.example.model.SpiritElement.FIRE, com.example.model.SpiritElement.WATER))
        val d = fresh().disciples.first().let { it.copy(spiritRoot = it.spiritRoot!!.copy(elements = listOf(com.example.model.SpiritElement.WATER))) }
        assertEquals(1.3, Elements.attackFactor(d, com.example.model.SpiritElement.FIRE), 1e-9)
        assertEquals(1.3, Elements.defenseFactor(d, com.example.model.SpiritElement.EARTH), 1e-9)
        assertEquals(1.0, Elements.attackFactor(d, com.example.model.SpiritElement.CHAOS), 1e-9)
    }

    @Test
    fun combatUsesElementAdvantage() {
        val s = fresh(15L)
        val lava = s.regions.first { it.biome == com.example.model.BiomeType.LAVA_CHASM }
        fun withRoot(e: com.example.model.SpiritElement) = s.copy(disciples = s.disciples.map {
            it.copy(spiritRoot = it.spiritRoot!!.copy(elements = listOf(e)))
        })
        val id = s.disciples.first().id
        val water = apply(withRoot(com.example.model.SpiritElement.WATER), GameCommand.StartExpedition(lava.id, id)).state.combat!!
        val wood = apply(withRoot(com.example.model.SpiritElement.WOOD), GameCommand.StartExpedition(lava.id, id)).state.combat!!
        assertTrue(water.effectiveAttack > wood.effectiveAttack)
    }

    // --- 5a: płomienie ---

    @Test
    fun everyFlameHasAReachableSource() {
        val s = fresh(16L)
        val regions = s.regions.map { it.id }.toSet()
        FlameSources.allFlames.keys.forEach { id ->
            val src = FlameSources.SOURCES[id]
            assertTrue("$id bez źródła", src != null)
            assertTrue("$id: brak krainy ${src!!.regionId}", src.regionId in regions)
        }
    }

    @Test
    fun exploringRegionRevealsTrailAndSourceAppearsInSeason() {
        val s = fresh(17L)
        val (withTrail, msg) = FlameSources.onRegionExplored(s, "reg_2")
        assertTrue(msg!!.contains("Trop"))
        assertEquals(1, withTrail.flags[FlameSources.trailFlag("flame_2")])
        assertNull(FlameSources.available(withTrail.copy(currentDay = 10), "reg_2"))
        assertEquals("flame_2", FlameSources.available(withTrail.copy(currentDay = 100), "reg_2"))
        val map = Expeditions.generate(withTrail.copy(currentDay = 100), withTrail.regions.first { it.id == "reg_2" })
        assertTrue(map.last().any { it.flameId == "flame_2" })
    }

    @Test
    fun tamedFlameJoinsSect() {
        val s = fresh(18L).copy(flags = mapOf(FlameSources.trailFlag("flame_10") to 1))
        val run = com.example.model.ExpeditionRun("reg_1", s.disciples.first().id, loot = mapOf("@flame:flame_10" to 1), hp = 50)
        val r = Expeditions.payout(s, run, 0.5)
        assertTrue(r.flames.any { it.id == "flame_10" })
        assertNull(r.flags[FlameSources.trailFlag("flame_10")])
    }

    // --- 5a: Wieża Prób ---

    @Test
    fun towerGrowsEightPercentPerFloorAndLimitsAttempts() {
        assertEquals((400 * 1.08).toInt(), Tower.guardian(2).hp)
        var s = fresh(19L).let { it.copy(disciples = it.disciples.map { d -> d.copy(attack = 9_999, defense = 9_999) }) }
        val id = s.disciples.first().id
        repeat(3) { s = apply(s, GameCommand.ClimbTower(id)).state }
        assertEquals(3, s.towerFloor)
        val r = apply(s, GameCommand.ClimbTower(id))
        assertTrue(r.notice!!.contains("jutra"))
        assertEquals(3, Tower.attemptsLeft(s.copy(currentDay = s.currentDay + 1)))
    }
}
