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
        val s = fresh(2L).copy(currentDay = 5, spiritStones = 1000, e7Initialized = false)
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
        repeat(8_000) {
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
        assertEquals((1500 * (1 + Tower.GROWTH_PER_FLOOR)).toInt(), Tower.guardian(2).hp)
        assertEquals((1500 * (1 + Tower.GROWTH_PER_FLOOR * 10)).toInt(), Tower.guardian(11).hp)
        var s = fresh(19L).let { it.copy(disciples = it.disciples.map { d -> d.copy(attack = 9_999, defense = 9_999) }) }
        val id = s.disciples.first().id
        repeat(3) { s = apply(s, GameCommand.ClimbTower(id)).state }
        assertEquals(3, s.towerFloor)
        val r = apply(s, GameCommand.ClimbTower(id))
        assertTrue(r.notice!!.contains("jutra"))
        assertEquals(3, Tower.attemptsLeft(s.copy(currentDay = s.currentDay + 1)))
    }

    // --- 5b: żywe sekty ---

    private fun advanceDays(s0: GameState, days: Int): GameState {
        var s = s0
        repeat(days) { s = nextDay(s) }
        return s
    }

    @Test
    fun sectsGetStrengthFromRankAndOldSavesMigrate() {
        val s = fresh(30L)
        assertTrue(s.rivalSects.all { it.strength > 0 })
        assertEquals(WorldAI.initialStrength(1), s.rivalSects.first { it.powerRank == 1 }.strength)
        val old = s.copy(e5Initialized = false, rivalSects = s.rivalSects.map { it.copy(strength = 0) })
        assertTrue(GameEngine.normalize(old).rivalSects.all { it.strength > 0 })
    }

    @Test
    fun fiveYearsWithoutPlayerNoSectTakesTheWholeMap() {
        var s = fresh(31L)
        val ctx = EngineContext(s.worldSeed, 0)
        repeat(600) { day ->
            s = s.copy(currentDay = s.currentDay + 1, step = s.step + 1, raid = null)
            s = WorldAI.daily(s, EngineContext(s.worldSeed, s.step)).first
        }
        val byOwner = s.territories.groupBy { it.controllingSectId }.mapValues { it.value.size }
        val maxOwned = byOwner.filterKeys { it != "player" }.values.maxOrNull() ?: 0
        assertTrue("$byOwner", maxOwned < s.territories.size)
        val avg = s.rivalSects.map { it.strength }.average()
        assertTrue(s.rivalSects.all { it.strength <= avg * WorldAI.STRENGTH_CAP * 1.1 })
        assertTrue(s.chronicle.isNotEmpty())
    }

    @Test
    fun hostileSectRaidsWithTwoDayWarningAndGuardedGatesHold() {
        val base = fresh(32L).let { st ->
            st.copy(currentDay = 40, rivalSects = st.rivalSects.map { if (it.id == "sect_shadow") it.copy(status = com.example.model.DiplomaticStatus.TOTAL_WAR) else it })
        }
        var s = base
        var tries = 0
        while (s.raid == null && tries++ < 200) s = nextDay(s)
        val raid = s.raid!!
        assertEquals(s.currentDay + WorldAI.RAID_WARNING_DAYS, raid.arriveDay)
        // Silni strażnicy na wszystkich bramach.
        val strong = s.copy(disciples = s.disciples.map { it.copy(attack = 2_000, defense = 1_000) })
        var guarded = strong
        Raids.GATES.zip(strong.disciples).forEach { (g, d) -> guarded = apply(guarded, GameCommand.GuardGate(g, d.id)).state }
        val (won, msg) = Raids.resolve(guarded.copy(currentDay = raid.arriveDay), EngineContext(1, 1))
        assertTrue(msg.contains("odparty"))
        assertNull(won.raid)
        val (lost, msg2) = Raids.resolve(s.copy(gates = emptyMap(), currentDay = raid.arriveDay, spiritStones = 10_000), EngineContext(1, 1))
        assertTrue(msg2.contains("przełamuje"))
        assertEquals(9_000, lost.spiritStones)
    }

    @Test
    fun formationNodesRaiseDefenseAndFiveElementsGiveSetBonus() {
        var s = fresh(33L).copy(spiritStones = 100_000, inventory = mapOf(Formation.MINERAL to 500))
        val f0 = Formation.defenseFactor(s)
        com.example.model.SpiritElement.BASIC.forEach { s = apply(s, GameCommand.PlaceFormationNode(it)).state }
        assertTrue(Formation.fiveElements(s))
        assertEquals(f0 + 0.4 + 0.2, Formation.defenseFactor(s), 1e-9)
        assertTrue(Modifiers.sect(s).breakdown(Stat.QI_GAIN).any { it.contains("Pięciu") })
        repeat(3) { s = apply(s, GameCommand.PlaceFormationNode(com.example.model.SpiritElement.FIRE)).state }
        assertEquals(Formation.MAX_NODES, s.formationNodes.size)
    }

    @Test
    fun spiesReturnAfterThreeDaysWithResults() {
        val s = fresh(34L)
        val d = s.disciples.maxBy { it.comprehension }
        val sent = apply(s, GameCommand.SendSpy(d.id, "sect_drunken", com.example.model.SpyType.SCOUT)).state
        assertTrue(apply(sent, GameCommand.SendSpy(d.id, "sect_drunken", com.example.model.SpyType.STEAL)).notice!!.contains("misji"))
        val back = advanceDays(sent, Spies.DAYS)
        assertTrue(back.spies.isEmpty())
        val drunk = back.rivalSects.first { it.id == "sect_drunken" }
        assertTrue(back.intel.containsKey("sect_drunken") || drunk.relations < s.rivalSects.first { it.id == "sect_drunken" }.relations)
    }

    @Test
    fun threeDefeatsMakeANemesisCappedAtThree() {
        var s = fresh(35L)
        repeat(3) { s = WorldAI.grudge(s, "sect_shadow").first }
        assertEquals(1, s.rivalSects.first { it.id == "sect_shadow" }.nemesisLevel)
        s = WorldAI.grudge(s, "sect_shadow").first
        assertEquals(2, s.rivalSects.first { it.id == "sect_shadow" }.nemesisLevel)
        listOf("sect_dragon", "sect_wudang", "sect_nether").forEach { id -> repeat(3) { s = WorldAI.grudge(s, id).first } }
        assertEquals(3, s.rivalSects.count { it.nemesisLevel > 0 })
        assertTrue(Raids.strength(s.rivalSects.first { it.id == "sect_shadow" }) > (s.rivalSects.first { it.id == "sect_shadow" }.strength * 0.5).toInt())
    }

    @Test
    fun deathDuelNeedsConfirmationAndLoserDies() {
        val s = fresh(36L)
        val weak = s.disciples.minBy { it.attack }
        assertTrue(apply(s, GameCommand.HonorDuel("sect_wudang", weak.id, toDeath = true)).notice!!.contains("potwierdzenia"))
        val weakened = s.copy(disciples = s.disciples.map { if (it.id == weak.id) it.copy(attack = 1, defense = 1) else it })
        val r = apply(weakened, GameCommand.HonorDuel("sect_wudang", weak.id, toDeath = true, confirmed = true)).state
        assertNull(r.disciple(weak.id))
    }

    @Test
    fun battlesNeedWarAndCanTakeTerritory() {
        val s = fresh(37L)
        val ids = s.disciples.map { it.id }
        assertTrue(apply(s, GameCommand.SectBattle("sect_dragon", ids)).notice!!.contains("wojny"))
        val war = s.copy(
            rivalSects = s.rivalSects.map { if (it.id == "sect_dragon") it.copy(status = com.example.model.DiplomaticStatus.TOTAL_WAR) else it },
            disciples = s.disciples.map { it.copy(attack = 5_000, defense = 5_000) }
        )
        val r = apply(war, GameCommand.SectBattle("sect_dragon", ids)).state
        assertTrue(r.territories.count { it.controllingSectId == "player" } > s.territories.count { it.controllingSectId == "player" })
    }

    // --- 5c: świat ---

    @Test
    fun twelveWanderingNpcsWithThreeToFiveLinks() {
        val chains = GameContent.scripts.filter { it.chain?.startsWith("npc_") == true || it.chain in setOf("merchant_loan") }
            .groupBy { it.chain }
        assertEquals(12, chains.size)
        chains.forEach { (name, links) -> assertTrue("$name: ${links.size}", links.size in 3..5) }
    }

    @Test
    fun empireDemandsTaxEveryThirtyDays() {
        var s = fresh(40L).copy(currentDay = 29, spiritStones = 10_000)
        s = nextDay(s)
        assertEquals(30, s.currentDay)
        val tax = apply(s, GameCommand.Tick).state
        assertEquals("emp_tax", tax.activeEventId)
        val paid = apply(tax, GameCommand.EventChoice(0)).state
        assertEquals(tax.spiritStones - tax.spiritStones * 5 / 100, paid.spiritStones)
        assertEquals(5, paid.flags[Empire.FAVOR])
    }

    @Test
    fun civilWarEndsEmpireWhenRebelsWin() {
        var s = EventScripts.activate(fresh(41L), GameContent.script("emp_civil_1")!!)
        s = apply(s, GameCommand.EventChoice(1)).state
        s = EventScripts.activate(s, GameContent.script("emp_civil_3")!!)
        val view = GameContent.event(s.activeEventId, s)!!
        assertTrue(view.choices[0].text.contains("niedostępne"))
        s = apply(s, GameCommand.EventChoice(1)).state
        assertEquals(1, s.flags[Empire.FALLEN])
        val later = Empire.daily(s.copy(currentDay = 60), EngineContext(1, 1)).first
        assertTrue(later.scheduledEvents.none { it.eventId == "emp_tax" })
    }

    @Test
    fun fogHidesFarRegionsUntilScouted() {
        val s = fresh(42L).copy(spiritStones = 10_000)
        assertTrue(s.discoveredRegionIds.size >= Fog.START_DISCOVERED)
        val k = s.regions.indexOfFirst { it.id !in s.discoveredRegionIds }
        val far = s.regions[k + 1]
        assertTrue(apply(s, GameCommand.StartExpedition(far.id, s.disciples.first().id)).notice!!.contains("mgle"))
        assertTrue(apply(s, GameCommand.ScoutRegion(far.id, s.disciples.first().id)).notice!!.contains("bliżej"))
        val strong = s.copy(disciples = s.disciples.map { it.copy(attack = 9_999) })
        val r = apply(strong, GameCommand.ScoutRegion(s.regions[k].id, strong.disciples.first().id)).state
        assertTrue(s.regions[k].id in r.discoveredRegionIds)
        val visited = s.copy(discoveredRegionIds = emptySet(), regions = s.regions.mapIndexed { i, x -> if (i == 7) x.copy(explorationProgress = 25) else x })
        assertTrue(s.regions[7].id in Fog.normalize(visited).discoveredRegionIds)
    }

    @Test
    fun secretRealmsOpenEveryFortyDaysWithRankLimit() {
        val r = SecretRealms.REALMS.first { it.id == "sr_bamboo" }
        val off = r.offset
        assertEquals(off, SecretRealms.openingDay(r, off + SecretRealms.OPEN_DAYS - 1))
        assertNull(SecretRealms.openingDay(r, off + SecretRealms.OPEN_DAYS))
        assertEquals(off + SecretRealms.EVERY, SecretRealms.openingDay(r, off + SecretRealms.EVERY))
        val s = fresh(43L).copy(currentDay = 6)
        val high = s.disciples.first { (it.majorRealm?.ordinal ?: 0) > r.maxRealm.ordinal }
        assertTrue(apply(s, GameCommand.EnterSecretRealm(r.id, high.id)).notice!!.contains("odrzuca"))
        val low = s.disciples.first { it.majorRealm == r.maxRealm }.let { it.copy(attack = 9_999, defense = 9_999) }
        val ss = s.copy(disciples = s.disciples.map { if (it.id == low.id) low else it })
        val inside = apply(ss, GameCommand.EnterSecretRealm(r.id, low.id)).state
        assertEquals((ss.inventory["Trawa Wiecznej Wiosny"] ?: 0) + 10, inside.inventory["Trawa Wiecznej Wiosny"])
        assertTrue(apply(inside, GameCommand.EnterSecretRealm(r.id, low.id)).notice!!.contains("już"))
        assertTrue(apply(s.copy(currentDay = off + SecretRealms.OPEN_DAYS + 1), GameCommand.EnterSecretRealm(r.id, low.id)).notice!!.contains("zamknięte"))
    }

    @Test
    fun catastrophesAreAnnouncedAndBounded() {
        var s = fresh(44L).copy(spiritStones = 1_000_000)
        var announced: com.example.model.Catastrophe? = null
        var day = 50
        while (announced == null && day < 3_000) {
            day += 30
            s = Catastrophes.daily(s.copy(currentDay = day, step = day.toLong()), EngineContext(s.worldSeed, day.toLong())).first
            announced = s.catastrophe
        }
        val c = announced!!
        assertTrue(c.startDay - c.announcedDay >= 3)
        val quake = s.copy(catastrophe = c.copy(type = com.example.model.CatastropheType.EARTHQUAKE), currentDay = c.startDay)
        val after = Catastrophes.daily(quake, EngineContext(1, 1)).first
        assertTrue(quake.spiritStones - after.spiritStones <= Catastrophes.EARTHQUAKE_CAP)
        val storm = s.copy(catastrophe = c.copy(type = com.example.model.CatastropheType.QI_STORM), currentDay = c.startDay)
        assertTrue(Modifiers.sect(storm).breakdown(Stat.QI_GAIN).any { it.contains("Burza") })
        val flood = s.copy(catastrophe = c.copy(type = com.example.model.CatastropheType.FLOOD), currentDay = c.startDay + 1)
        assertTrue(Market.sellPrice(flood, "Trawa Wiecznej Wiosny") > Market.sellPrice(flood.copy(catastrophe = null), "Trawa Wiecznej Wiosny"))
        val ended = Catastrophes.daily(s.copy(currentDay = c.endDay + 1), EngineContext(1, 1)).first
        assertNull(ended.catastrophe)
    }
}
