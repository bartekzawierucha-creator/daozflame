package com.example.game.engine

import com.example.model.Artifact
import com.example.model.BondKind
import com.example.model.EquipSlot
import com.example.model.GoalKind
import com.example.model.MajorRealm
import com.example.model.Opportunity
import com.example.model.OpportunityKind
import com.example.model.StewardPolicy
import com.example.model.TimeOfDay
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Etap 11: ekwipunek, cel tygodnia, mentor, więzi, porażka z sensem, okazje, wyprawy w drużynie, zarządca. */
class Stage11Test {

    private fun fresh(seed: Long = 1L) = GameEngine.newGame(seed).copy(tutorialStep = Tutorial.DONE)
    private fun apply(s: GameState, c: GameCommand) = GameEngine.apply(s, c)
    private fun nextDay(s: GameState): GameState =
        apply(s.copy(timeOfDay = TimeOfDay.FULL_MOON, phaseTick = TimePhase.TICKS_PER_PHASE - 1), GameCommand.Tick).state
    private fun GameState.with(id: String, f: (com.example.model.Disciple) -> com.example.model.Disciple) =
        copy(disciples = disciples.map { if (it.id == id) f(it) else it })

    private val sword = Artifact("a1", "Miecz Gromowego Kryształu", "Niebiański", 95, 15, "")
    private val sword2 = Artifact("a2", "Obsydianowe Sztylety Miazmatu", "Rzadki", 55, 5, "")
    private val robe = Artifact("a3", "Szata Niebiańskiego Pióra Feniksa", "Boski", 30, 95, "")
    private val ring = Artifact("a4", "Pierścień Piekielnego Płomienia", "Boski", 75, 25, "")

    // --- Ekwipunek ---

    @Test
    fun slotsAreRecognizedFromNames() {
        assertEquals(EquipSlot.WEAPON, Gear.slotOf(sword))
        assertEquals(EquipSlot.ARMOR, Gear.slotOf(robe))
        assertEquals(EquipSlot.RING, Gear.slotOf(ring))
        assertEquals(EquipSlot.HEAD, Gear.slotOf(Artifact("h", "Rogaty Hełm Mroźnego Qilina", "Niebiański", 15, 95, "")))
        assertEquals(EquipSlot.HANDS, Gear.slotOf(Artifact("g", "Rękawice Smoczej Kości", "Boski", 120, 60, "")))
        assertEquals(EquipSlot.RELIC, Gear.slotOf(Artifact("b", "Dzwon Tłumienia Duszy Pustki", "Nieśmiertelny", 110, 80, "")))
    }

    @Test
    fun equipMovesItemFromVaultAndSwapsBack() {
        var s = fresh().copy(vaultArtifacts = listOf(sword, sword2))
        val d = s.disciples.first()
        s = apply(s, GameCommand.Equip(d.id, 0)).state
        assertEquals(listOf(sword2), s.vaultArtifacts)
        assertEquals(d.attack + 95, s.disciple(d.id)!!.attack)
        // Druga broń zastępuje pierwszą, pierwsza wraca do skarbca.
        s = apply(s, GameCommand.Equip(d.id, 0)).state
        assertEquals(listOf(sword), s.vaultArtifacts)
        assertEquals(d.attack + 55, s.disciple(d.id)!!.attack)
        s = apply(s, GameCommand.Unequip(d.id, EquipSlot.WEAPON)).state
        assertEquals(d.attack, s.disciple(d.id)!!.attack)
        assertEquals(2, s.vaultArtifacts.size)
    }

    @Test
    fun fullSetGivesBonusAndRemovingItTakesItBack() {
        var s = fresh().copy(vaultArtifacts = listOf(sword, robe, ring))
        val d = s.disciples.first()
        repeat(3) { s = apply(s, GameCommand.Equip(d.id, 0)).state }
        val x = s.disciple(d.id)!!
        assertEquals(Gear.SET_HALF, Gear.setPercent(Gear.items(x).size))
        assertEquals(d.attack + (95 + 30 + 75) * (100 + Gear.SET_HALF) / 100, x.attack)
        assertEquals(d.defense + (15 + 95 + 25) * (100 + Gear.SET_HALF) / 100, x.defense)
        s = apply(s, GameCommand.Unequip(d.id, EquipSlot.ARMOR)).state
        assertEquals(d.attack + 95 + 75, s.disciple(d.id)!!.attack)
    }

    @Test
    fun deadDisciplesGearReturnsToVault() {
        var s = fresh().copy(vaultArtifacts = listOf(robe), e7Initialized = true)
        val d = s.disciples.first()
        s = apply(s, GameCommand.Equip(d.id, 0)).state
        s = Lifecycle.die(s, s.disciple(d.id)!!, "test")
        assertEquals(listOf(robe), s.vaultArtifacts)
    }

    @Test
    fun legacyArmorInWeaponSlotIsMigrated() {
        val s = fresh()
        val id = s.disciples.first().id
        val old = s.with(id) { it.copy(equippedArtifact = robe) }
        val n = GameEngine.normalize(old)
        assertNull(n.disciple(id)!!.equippedArtifact)
        assertEquals(robe, n.disciple(id)!!.armor)
    }

    // --- Cel tygodnia ---

    @Test
    fun weeklyGoalIsAnnouncedAndRewarded() {
        var s = fresh().copy(currentDay = SectGoals.FIRST_DAY)
        s = nextDay(s)
        val g = s.weeklyGoal
        assertNotNull(g)
        assertEquals(s.currentDay + SectGoals.DURATION, g!!.deadline)
        // Spełnij cel ręcznie: podbij licznik.
        s = when (g.kind) {
            GoalKind.BREAKTHROUGHS, GoalKind.WINS, GoalKind.CONTRACTS -> {
                val key = mapOf(GoalKind.BREAKTHROUGHS to "breakthroughs", GoalKind.WINS to "wins", GoalKind.CONTRACTS to "contracts").getValue(g.kind)
                s.copy(meta = s.meta.copy(stats = s.meta.stats + (key to g.base + g.target)))
            }
            GoalKind.TOWER -> s.copy(towerFloor = (g.base + g.target).toInt())
            GoalKind.REPUTATION -> s.copy(sectReputation = (g.base + g.target).toInt())
            GoalKind.STONES -> s.copy(spiritStones = (g.base + g.target).toInt())
            GoalKind.TOURNAMENT -> s.copy(disciples = s.disciples.map { it.copy(combatPower = it.combatPower * 2) })
        }
        val stones = s.spiritStones
        s = nextDay(s)
        assertEquals(1, s.goalsWon)
        assertNull(s.weeklyGoal)
        assertTrue(s.spiritStones >= stones + g.rewardStones - 2000)
    }

    @Test
    fun missedWeeklyGoalCostsMorale() {
        var s = fresh().copy(currentDay = SectGoals.FIRST_DAY)
        s = nextDay(s)
        val g = s.weeklyGoal!!
        s = s.copy(currentDay = g.deadline, weeklyGoal = g.copy(target = 1_000_000_000), morale = 60)
        s = nextDay(s)
        assertEquals(1, s.goalsLost)
        assertTrue(s.morale < 60)
    }

    // --- Mentor ---

    @Test
    fun masterFocusLimitsPersonalActions() {
        var s = fresh()
        val ids = s.disciples.map { it.id }
        repeat(Mentor.MAX_FOCUS) { i -> s = apply(s, GameCommand.MeditateWith(ids[i])).state }
        assertEquals(0, Mentor.focusLeft(s))
        val r = apply(s, GameCommand.MeditateWith(ids[Mentor.MAX_FOCUS]))
        assertTrue(r.notice!!.contains("wyczerpany"))
        // Skupienie wraca po punkcie co FOCUS_REGEN_DAYS dni.
        repeat(Mentor.FOCUS_REGEN_DAYS) { s = nextDay(s) }
        assertEquals(1, Mentor.focusLeft(s))
        repeat(Mentor.FOCUS_REGEN_DAYS * 3) { s = nextDay(s) }
        assertEquals(Mentor.MAX_FOCUS, Mentor.focusLeft(s))
    }

    @Test
    fun meditationBuildsTrustOncePerDay() {
        var s = fresh()
        val id = s.disciples.first().id
        val t = s.disciple(id)!!.trust
        s = apply(s, GameCommand.MeditateWith(id)).state
        assertEquals((t + Mentor.MEDITATION_TRUST).coerceAtMost(100), s.disciple(id)!!.trust)
        val again = apply(s, GameCommand.MeditateWith(id))
        assertEquals(s.disciple(id)!!.trust, again.state.disciple(id)!!.trust)
    }

    @Test
    fun praiseStirsJealousy() {
        var s = fresh()
        val (a, b) = s.disciples.take(2).map { it.id }
        s = s.with(b) { it.copy(traitIds = listOf("jealous"), loyalty = 50) }
        s = apply(s, GameCommand.Praise(a)).state
        assertEquals(50 - Mentor.JEALOUSY, s.disciple(b)!!.loyalty)
        assertTrue(Bonds.value(s, a, b) < 0)
    }

    @Test
    fun keptPromiseDeliversRewardAndBuildsTrust() {
        var s = fresh().copy(vaultArtifacts = listOf(sword))
        val id = s.disciples.first().id
        s = apply(s, GameCommand.MakePromise(id, "art:0")).state
        val p = s.disciple(id)!!.promise!!
        val t = s.disciple(id)!!.trust
        s = s.with(id) { it.copy(majorRealm = MajorRealm.entries[p.targetMajor], stage = 1, stageQi = 0.0) }
        s = nextDay(s)
        val d = s.disciple(id)!!
        assertNull(d.promise)
        assertEquals(sword, d.equippedArtifact)
        assertTrue(d.trust >= t + Mentor.PROMISE_KEPT_TRUST - 5)
    }

    @Test
    fun brokenPromiseShattersTrust() {
        var s = fresh().copy(vaultArtifacts = listOf(sword))
        val id = s.disciples.first().id
        s = apply(s, GameCommand.MakePromise(id, "art:0")).state
        val p = s.disciple(id)!!.promise!!
        s = s.copy(vaultArtifacts = emptyList()).with(id) { it.copy(majorRealm = MajorRealm.entries[p.targetMajor], stage = 1, stageQi = 0.0, trust = 60) }
        s = nextDay(s)
        assertTrue(s.disciple(id)!!.trust <= 60 - Mentor.PROMISE_BROKEN_TRUST + 10)
    }

    // --- Więzi ---

    @Test
    fun bondThresholdsAndRivalSpur() {
        var s = fresh()
        val (a, b) = s.disciples.take(2).map { it.id }
        s = Bonds.shift(s, a, b, -50)
        assertEquals(BondKind.RIVAL, Bonds.of(s, a, b)!!.kind)
        assertTrue(s.chronicle.last().contains("rywale"))
        val qi = s.disciple(b)!!.stageQi
        s = Bonds.afterEvents(s, listOf(GameEvent.Breakthrough(a, true)))
        assertTrue(s.disciple(b)!!.stageQi > qi)
    }

    @Test
    fun workingTogetherBuildsFriendship() {
        var s = fresh()
        val (a, b) = s.disciples.take(2).map { it.id }
        s = s.with(a) { it.copy(role = com.example.model.DiscipleRole.MINING, traitIds = listOf("humble"), hiddenTraitIds = emptyList()) }
            .with(b) { it.copy(role = com.example.model.DiscipleRole.MINING, traitIds = listOf("humble"), hiddenTraitIds = emptyList(), martialSchool = s.disciple(a)!!.martialSchool) }
        val together = Bonds.drift(s.disciple(a)!!, s.disciple(b)!!, 0)
        val apart = Bonds.drift(s.disciple(a)!!, s.disciple(b)!!.copy(role = com.example.model.DiscipleRole.DIPLOMACY, martialSchool = com.example.model.MartialSchool.entries.first { it != s.disciple(a)!!.martialSchool }), 0)
        assertTrue("$together vs $apart", together >= apart + 2)
    }

    // --- Porażka z sensem ---

    @Test
    fun healedInjuryLeavesHardeningScar() {
        var s = fresh()
        val id = s.disciples.first().id
        s = s.with(id) { it.copy(injuredUntilDay = s.currentDay) }
        val def = s.disciple(id)!!.defense
        s = nextDay(s)
        assertEquals(1, s.disciple(id)!!.scars)
        assertEquals(def + Legacy.SCAR_DEFENSE, s.disciple(id)!!.defense)
        s = nextDay(s)
        assertEquals(1, s.disciple(id)!!.scars)
    }

    @Test
    fun deathPassesLegacyToStudent() {
        var s = fresh().copy(e7Initialized = true)
        val (m, st) = s.disciples.take(2).map { it.id }
        s = s.with(m) { it.copy(laws = mapOf("Miecz" to 80.0)) }.with(st) { it.copy(mentorId = m, laws = emptyMap()) }
        val comp = s.disciple(st)!!.comprehension
        s = Lifecycle.die(s, s.disciple(m)!!, "test")
        val heir = s.disciple(st)!!
        assertEquals(24.0, heir.laws["Miecz"]!!, 0.01)
        assertEquals(comp + Legacy.OATH_COMPREHENSION, heir.comprehension)
    }

    // --- Okazje ---

    @Test
    fun wanderingMasterTeachesForStones() {
        var s = fresh().copy(spiritStones = 5000, opportunity = Opportunity(OpportunityKind.WANDERING_MASTER, 99, cost = 600))
        val d = s.disciples.first()
        s = apply(s, GameCommand.AcceptOpportunity(d.id)).state
        assertNull(s.opportunity)
        assertEquals(4400, s.spiritStones)
        assertEquals(d.comprehension + Opportunities.MASTER_COMPREHENSION, s.disciple(d.id)!!.comprehension)
    }

    @Test
    fun opportunitiesAppearAndExpire() {
        var s = fresh().copy(lastOpportunityDay = -100)
        var seen = false
        repeat(40) { s = nextDay(s); if (s.opportunity != null) seen = true }
        assertTrue(seen)
    }

    // --- Wyprawy w drużynie ---

    @Test
    fun companionAddsSightAndStrength() {
        val s = fresh().copy(discoveredRegionIds = emptySet())
        val (a, b) = s.disciples.take(2).map { it.id }
        val region = s.regions.first()
        val r = apply(s, GameCommand.BeginExpedition(region.id, a, auto = false, companionId = b)).state
        val run = r.expedition!!
        assertEquals(b, run.companionId)
        assertTrue(run.sight >= 2)
        val (atk, _) = Expeditions.team(r, run, r.disciple(a)!!)
        assertTrue(atk > r.disciple(a)!!.attack)
    }

    // --- Zarządca ---

    @Test
    fun stewardSellsSurplusButKeepsPromisedItems() {
        var s = fresh()
        val (a, b) = s.disciples.take(2).map { it.id }
        val herb = s.inventory.keys.first { it.startsWith("Trawa") }
        s = s.copy(inventory = s.inventory + (herb to 100) + (DiscipleRules.FOOD to 0))
        s = apply(s, GameCommand.SetSteward(StewardPolicy(a, keepAbove = 20, autoFood = true))).state
        val stones = s.spiritStones
        repeat(Steward.EVERY_DAYS) { s = nextDay(s) }
        assertTrue((s.inventory[herb] ?: 0) < 60)
        assertTrue((s.inventory[DiscipleRules.FOOD] ?: 0) > 0)
        assertFalse(s.spiritStones == stones)
        // Obiecany przedmiot jest nietykalny.
        s = s.copy(inventory = s.inventory + (herb to 50))
        s = apply(s, GameCommand.MakePromise(b, herb)).state
        repeat(Steward.EVERY_DAYS) { s = nextDay(s) }
        assertTrue((s.inventory[herb] ?: 0) >= 50)
    }
}
