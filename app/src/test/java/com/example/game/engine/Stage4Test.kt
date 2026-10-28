package com.example.game.engine

import com.example.model.ContractType
import com.example.model.FieldPlot
import com.example.model.ItemQuality
import com.example.model.MajorRealm
import com.example.model.Season
import com.example.model.TimeOfDay
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Etap 4: pory roku, żyła, pola, targ, karawany, kontrakty, jakość, receptury, ulepszanie, talizmany, płomienie. */
class Stage4Test {

    private fun fresh(seed: Long = 1L) = GameEngine.newGame(seed)
    private fun apply(s: GameState, c: GameCommand) = GameEngine.apply(s, c)
    private fun dawn(s: GameState, day: Int = s.currentDay + 1): StepResult =
        apply(s.copy(currentDay = day - 1, timeOfDay = TimeOfDay.FULL_MOON, phaseTick = TimePhase.TICKS_PER_PHASE - 1), GameCommand.Tick)

    // --- Pory roku ---

    @Test
    fun seasonsCycleEveryThirtyDays() {
        assertEquals(Season.SPRING, Seasons.of(1))
        assertEquals(Season.SPRING, Seasons.of(30))
        assertEquals(Season.SUMMER, Seasons.of(31))
        assertEquals(Season.WINTER, Seasons.of(120))
        assertEquals(Season.SPRING, Seasons.of(121))
    }

    // --- Żyła ---

    @Test
    fun veinUpgradeTakesTimeAndRaisesQi() {
        val s = fresh(2L).let { it.copy(spiritStones = 50_000, inventory = it.inventory + ("Duchowy Nefryt" to 100)) }
        val started = apply(s, GameCommand.UpgradeVein).state
        val cost = Vein.upgradeCost(1)!!
        assertEquals(s.spiritStones - cost.stones, started.spiritStones)
        assertEquals(s.currentDay + cost.days, started.veinUpgradeDoneDay)
        assertTrue(apply(started, GameCommand.UpgradeVein).notice!!.contains("trwa"))
        val done = dawn(started, day = started.veinUpgradeDoneDay).state
        assertEquals(2, done.veinLevel)
        assertTrue(Modifiers.sect(done).breakdown(Stat.QI_GAIN).any { it.contains("Żyła") })
    }

    @Test
    fun veinUnlocksFieldTiersAndEdictSlot() {
        assertEquals(2, Vein.fieldTier(1))
        assertEquals(5, Vein.fieldTier(9))
        val s = fresh(3L).copy(veinLevel = 5)
        assertEquals(3, EdictCatalog.slots(s))
    }

    // --- Pola ---

    @Test
    fun plantingRespectsSeasonVeinAndSeeds() {
        val s = fresh(4L)
        assertTrue(apply(s.copy(currentDay = 100), GameCommand.PlantHerb(0, "Trawa Wiecznej Wiosny")).notice!!.contains("Zimą"))
        assertTrue(apply(s, GameCommand.PlantHerb(0, "Owoc Drzewa Pantao")).notice!!.contains("żyły"))
        val planted = apply(s, GameCommand.PlantHerb(0, "Trawa Wiecznej Wiosny")).state
        assertEquals("Trawa Wiecznej Wiosny", planted.fields[0].herb)
        assertEquals((s.inventory["Trawa Wiecznej Wiosny"] ?: 0) - 1, planted.inventory["Trawa Wiecznej Wiosny"])
    }

    @Test
    fun harvestYieldsMoreInSpringAndWithNeighbours() {
        val s = fresh(5L)
        val ready = FieldPlot("Trawa Wiecznej Wiosny", 1, 1)
        val oneSpring = s.copy(currentDay = 10, fields = listOf(ready) + List(15) { FieldPlot() })
        val oneSummer = oneSpring.copy(currentDay = 40)
        val spring = apply(oneSpring, GameCommand.HarvestFields).state.inventory["Trawa Wiecznej Wiosny"]!! - (s.inventory["Trawa Wiecznej Wiosny"] ?: 0)
        val summer = apply(oneSummer, GameCommand.HarvestFields).state.inventory["Trawa Wiecznej Wiosny"]!! - (s.inventory["Trawa Wiecznej Wiosny"] ?: 0)
        assertTrue("$spring vs $summer", spring > summer)
        val pair = s.copy(fields = listOf(ready, ready) + List(14) { FieldPlot() })
        assertEquals(0.10, Fields.synergy(pair, 0), 1e-9)
    }

    @Test
    fun fieldsExpandToSixBySix() {
        val s = fresh(6L).copy(spiritStones = 10_000)
        val bigger = apply(s, GameCommand.ExpandFields).state
        assertEquals(36, bigger.fields.size)
        assertTrue(apply(bigger, GameCommand.ExpandFields).notice!!.contains("całe"))
    }

    // --- Targ ---

    @Test
    fun sellingPushesPriceDownAndBuyingCostsMore() {
        val s = fresh(7L).let { it.copy(inventory = it.inventory + ("Trawa Wiecznej Wiosny" to 100)) }
        val price = Market.sellPrice(s, "Trawa Wiecznej Wiosny")
        val sold = apply(s, GameCommand.Sell("Trawa Wiecznej Wiosny", 50)).state
        assertTrue(Market.sellPrice(sold, "Trawa Wiecznej Wiosny") < price)
        assertTrue(Market.buyPrice(s, "Trawa Wiecznej Wiosny") > price)
        // Brak arbitrażu: kupno i natychmiastowa sprzedaż zawsze tracą.
        val bought = apply(s.copy(spiritStones = 10_000), GameCommand.Buy("Trawa Wiecznej Wiosny", 10)).state
        val resold = apply(bought, GameCommand.Sell("Trawa Wiecznej Wiosny", 10)).state
        assertTrue(resold.spiritStones < 10_000)
    }

    @Test
    fun autumnRaisesSellPrices() {
        val s = fresh(8L)
        assertTrue(Market.sellPrice(s.copy(currentDay = 70), "Duchowy Nefryt") > Market.sellPrice(s.copy(currentDay = 10), "Duchowy Nefryt"))
    }

    @Test
    fun onlySimpleGoodsAreBuyable() {
        assertTrue(Market.buyable("Trawa Wiecznej Wiosny"))
        assertTrue(Market.buyable(Market.SPIRIT_PAPER))
        assertFalse(Market.buyable("Owoc Drzewa Pantao"))
        assertFalse(Market.buyable("Boska Smocza Łuska"))
    }

    @Test
    fun pricesRecoverOverTime() {
        val s = fresh(9L).copy(marketMultipliers = mapOf("Duchowy Nefryt" to 0.6))
        var st = s
        repeat(60) { st = dawn(st).state }
        assertTrue(Market.multiplier(st, "Duchowy Nefryt") > 0.7)
    }

    @Test
    fun caravanReturnsWithProfitOrIsRobbed() {
        val s = fresh(10L).let { it.copy(inventory = it.inventory + ("Duchowy Nefryt" to 40)) }
        val sent = apply(s, GameCommand.SendCaravan("Duchowy Nefryt", 20, null)).state
        assertEquals(1, sent.caravans.size)
        val c = sent.caravans.single()
        assertTrue(c.unitPrice >= Market.sellPrice(s, "Duchowy Nefryt"))
        val back = dawn(sent, day = c.returnDay)
        assertTrue(back.state.caravans.isEmpty())
        assertNotNull(back.notice)
    }

    @Test
    fun blackMarketOnlyAtNight() {
        val s = fresh(11L).copy(spiritStones = 50_000)
        assertTrue(apply(s.copy(timeOfDay = TimeOfDay.DAY), GameCommand.BuyBlackMarket(0)).notice!!.contains("nocą"))
        val r = apply(s.copy(timeOfDay = TimeOfDay.NIGHT), GameCommand.BuyBlackMarket(1)).state
        assertTrue(r.spiritStones < s.spiritStones)
    }

    // --- Kontrakty ---

    @Test
    fun contractsAppearAndDeliveriesPay() {
        val s = dawn(fresh(12L)).state
        assertTrue(s.contracts.size >= Contracts.BOARD_SIZE)
        val deliver = s.contracts.first { it.type == ContractType.DELIVER }
        val stocked = s.copy(inventory = s.inventory + (deliver.item to deliver.quantity))
        val r = apply(stocked, GameCommand.FulfillContract(deliver.id)).state
        assertEquals(stocked.spiritStones + deliver.rewardStones, r.spiritStones)
        assertTrue(r.contracts.first { it.id == deliver.id }.done)
    }

    @Test
    fun huntContractCompletesOnBossVictory() {
        val s = dawn(fresh(13L)).state
        val hunt = s.contracts.first { it.type == ContractType.HUNT }
        val started = apply(s, GameCommand.StartExpedition(hunt.regionId!!, s.disciples.first().id)).state
        val nearlyWon = started.copy(combat = started.combat!!.copy(enemyHp = 1))
        val r = apply(nearlyWon, GameCommand.CombatManualAttack).state
        assertTrue(r.contracts.first { it.id == hunt.id }.done)
    }

    // --- Jakość, receptury, ulepszanie ---

    @Test
    fun qualityRollsFollowDistributionAndBonusHelps() {
        val rng = GameRng(1L)
        val n = 20_000
        val base = (1..n).map { Quality.roll(rng, 0) }.groupingBy { it }.eachCount()
        assertEquals(0.15, (base[ItemQuality.CRACKED] ?: 0).toDouble() / n, 0.01)
        assertEquals(0.55, (base[ItemQuality.NORMAL] ?: 0).toDouble() / n, 0.015)
        val boosted = (1..n).map { Quality.roll(rng, 20) }.count { it.ordinal >= ItemQuality.EXCELLENT.ordinal }
        assertTrue(boosted > (base[ItemQuality.EXCELLENT] ?: 0) + (base[ItemQuality.LEGENDARY] ?: 0))
    }

    @Test
    fun qualityKeysParseAndBestIsUsedFirst() {
        val k = Quality.key("Pigułka Przełomu Qi", ItemQuality.LEGENDARY)
        assertEquals("Pigułka Przełomu Qi" to ItemQuality.LEGENDARY, Quality.parse(k))
        val inv = mutableMapOf("Pigułka Przełomu Qi" to 2, k to 1)
        assertEquals(ItemQuality.LEGENDARY, Quality.takeBest(inv, "Pigułka Przełomu Qi"))
        assertEquals(ItemQuality.NORMAL, Quality.takeBest(inv, "Pigułka Przełomu Qi"))
        assertEquals(1, Quality.count(inv, "Pigułka Przełomu Qi"))
    }

    @Test
    fun newGameKnowsOnlyBasicRecipes() {
        val s = fresh(14L)
        assertTrue(s.knownRecipeIds.isNotEmpty())
        val unknown = GameContent.alchemyRecipes.first { it.id !in s.knownRecipeIds }
        assertTrue(apply(s, GameCommand.BrewPill(unknown.id)).notice!!.contains("nieznana"))
    }

    @Test
    fun experimentCanDiscoverRecipes() {
        val s = fresh(15L).copy(spiritStones = 99_999)
        val target = GameContent.alchemyRecipes.first { it.id !in s.knownRecipeIds }
        var st = s.copy(inventory = s.inventory + (target.requiredHerbName to 50) + (target.secondaryHerbName?.let { mapOf(it to 50) } ?: emptyMap()))
        var tries = 0
        while (target.id !in st.knownRecipeIds && tries++ < 30) {
            st = apply(st, GameCommand.Experiment(target.requiredHerbName, target.secondaryHerbName)).state
        }
        assertTrue(target.id in st.knownRecipeIds)
    }

    @Test
    fun highTierPillsNeedHotFlame() {
        val s = fresh(16L).copy(spiritStones = 99_999, knownRecipeIds = emptySet())
        val t5 = GameContent.alchemyRecipes.first { it.tier == 5 }
        assertTrue(apply(s, GameCommand.BrewPill(t5.id)).notice!!.contains("temperaturze"))
    }

    @Test
    fun artifactUpgradeChancesAndCracking() {
        assertEquals(100, CraftV2.upgradeChance(0))
        assertEquals(100, CraftV2.upgradeChance(2))
        assertEquals(90, CraftV2.upgradeChance(3))
        assertEquals(30, CraftV2.upgradeChance(9))
        val art = GameContent.forgingRecipes.first().artifactResult
        val s = fresh(17L).let { it.copy(spiritStones = 1_000_000, vaultArtifacts = listOf(art), inventory = it.inventory + (GameContent.forgingRecipes.first().requiredMineralName to 999)) }
        val up = apply(s, GameCommand.UpgradeArtifact(0)).state.vaultArtifacts.single()
        assertEquals(1, up.plus)
        assertTrue(up.attackBonus >= art.attackBonus)
    }

    // --- Talizmany ---

    @Test
    fun talismansAreCraftedAndWorkInCombat() {
        val s = fresh(18L).let { it.copy(spiritStones = 5_000, inventory = it.inventory + (Market.SPIRIT_PAPER to 10)) }
        val crafted = apply(s, GameCommand.CraftTalisman("Talizman Ognistej Kuli")).state
        assertEquals(1, crafted.inventory["Talizman Ognistej Kuli"])
        val inFight = apply(crafted, GameCommand.StartExpedition(crafted.regions.first().id, crafted.disciples.first().id)).state
        val hp = inFight.combat!!.enemyHp
        val after = apply(inFight, GameCommand.CombatTalisman("Talizman Ognistej Kuli")).state
        assertEquals((hp - 250).coerceAtLeast(0), after.combat!!.enemyHp)
        assertEquals(0, after.inventory["Talizman Ognistej Kuli"])
    }

    @Test
    fun sealMakesBeastLoseATurn() {
        val s = fresh(19L).let { it.copy(inventory = it.inventory + ("Talizman Pieczęci" to 1)) }
        var st = apply(s, GameCommand.StartExpedition(s.regions.first().id, s.disciples.first().id)).state
        st = apply(st, GameCommand.CombatTalisman("Talizman Pieczęci")).state
        st = apply(st, GameCommand.CombatStep).state // cios ucznia
        val hp = st.combat!!.discipleHp
        st = apply(st, GameCommand.CombatStep).state // bestia związana
        assertEquals(hp, st.combat!!.discipleHp)
    }

    // --- Płomienie ---

    @Test
    fun newGameStartsWithOneFlameOldSaveKeepsAll() {
        assertEquals(listOf(Flames.STARTING_FLAME), fresh(20L).flames.map { it.id })
        val old = fresh(20L).copy(e4Initialized = false, flames = com.example.game.WuxiaGenerator.getGeocentricFlames(), knownRecipeIds = emptySet())
        val migrated = GameEngine.normalize(old)
        assertEquals(12, migrated.flames.size)
        assertEquals(GameContent.alchemyRecipes.size, migrated.knownRecipeIds.size)
    }

    @Test
    fun flameBearerNeedsRealmAndGetsCombatBonus() {
        val s = fresh(21L).copy(flames = com.example.game.WuxiaGenerator.getGeocentricFlames())
        val low = s.disciples.first { it.majorRealm == MajorRealm.QI_CONDENSATION }
        assertTrue(apply(s, GameCommand.FlameToDisciple("flame_9", low.id)).notice!!.contains("wymaga"))
        val r = apply(s, GameCommand.FlameToDisciple("flame_1", low.id)).state
        assertEquals("flame_1", r.disciple(low.id)!!.flameId)
        assertNull(r.activeFlameId)
        val withFlame = apply(r, GameCommand.StartExpedition(r.regions.first().id, low.id)).state.combat!!.effectiveAttack
        val without = apply(s.copy(activeFlameId = null), GameCommand.StartExpedition(s.regions.first().id, low.id)).state.combat!!.effectiveAttack
        assertTrue("$withFlame vs $without", withFlame > without)
    }

    @Test
    fun refinementIsCappedAtNine() {
        val s = fresh(22L).let { st -> st.copy(spiritStones = 999_999, flames = st.flames.map { it.copy(refinementLevel = 9) }, inventory = st.inventory + (GameContent.FLAME_CRYSTAL to 99)) }
        assertTrue(apply(s, GameCommand.RefineFlame(Flames.STARTING_FLAME)).notice!!.contains("najwyższy"))
        assertEquals(1_800 + 8 * 350, Flames.temperature(s.flames.single()))
    }

    @Test
    fun betterBreakthroughPillsGiveBiggerBonus() {
        val inv = mutableMapOf(Quality.key(GameContent.BREAKTHROUGH_PILL, ItemQuality.HEAVENLY) to 1)
        assertEquals(ItemQuality.HEAVENLY, Quality.takeBest(inv, GameContent.BREAKTHROUGH_PILL))
        assertEquals(105, (35 * ItemQuality.HEAVENLY.effect).toInt())
    }
}
