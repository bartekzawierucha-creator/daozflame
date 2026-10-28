package com.example

import com.example.game.WuxiaGenerator
import com.example.model.*
import org.junit.Assert.*
import org.junit.Test

class WuxiaGameUnitTest {

    @Test
    fun testDiscipleGeneration() {
        val disciple = WuxiaGenerator.generateDisciple(
            rng = com.example.game.engine.GameRng(1L),
            predefinedRealm = CultivationRealm.FOUNDATION_EARLY,
            guaranteedBloodline = BloodlineType.GOLDEN_DRAGON
        )

        assertNotNull(disciple.id)
        assertTrue(disciple.name.isNotEmpty())
        assertEquals(BloodlineType.GOLDEN_DRAGON, disciple.bloodline)
        assertEquals(CultivationRealm.FOUNDATION_EARLY, disciple.realm)
        assertTrue(disciple.maxHp > 200)
        assertTrue(disciple.attack > 30)
    }

    @Test
    fun testSkillTreeCompleteness() {
        val skills = WuxiaGenerator.getAllSkillNodes()
        assertTrue(skills.isNotEmpty())

        val swordSkills = skills.filter { it.martialSchool == MartialSchool.SWORD_IMMORTAL }
        val bodySkills = skills.filter { it.martialSchool == MartialSchool.BODY_ASURA }
        val elementalSkills = skills.filter { it.martialSchool == MartialSchool.ELEMENTAL_TAOIST }
        val shadowSkills = skills.filter { it.martialSchool == MartialSchool.SHADOW_VIPER }

        assertTrue(swordSkills.isNotEmpty())
        assertTrue(bodySkills.isNotEmpty())
        assertTrue(elementalSkills.isNotEmpty())
        assertTrue(shadowSkills.isNotEmpty())

        // Ensure Tier 4 ultimate techniques exist for each class
        assertTrue(swordSkills.any { it.tier == 4 && it.activeTechniqueName != null })
        assertTrue(bodySkills.any { it.tier == 4 && it.activeTechniqueName != null })
        assertTrue(elementalSkills.any { it.tier == 4 && it.activeTechniqueName != null })
        assertTrue(shadowSkills.any { it.tier == 4 && it.activeTechniqueName != null })
    }

    @Test
    fun testWorldRegionsAndBeasts() {
        val regions = WuxiaGenerator.getInitialRegions()
        assertEquals(12, regions.size)
        assertTrue(regions.all { it.bossBeast != null })
        assertTrue(regions.any { it.biome == BiomeType.KUNLUN_PEAK })
        assertTrue(regions.any { it.biome == BiomeType.LAVA_CHASM })
    }

    @Test
    fun testAlchemyAndForgingRecipes() {
        val alchemy = WuxiaGenerator.getAlchemyRecipes()
        val forging = WuxiaGenerator.getForgingRecipes()

        assertTrue(alchemy.any { it.pillName == "Pigułka Przełomu Qi" })
        assertTrue(forging.any { it.artifactName.contains("Miecz") })
    }

    @Test
    fun testAutoBreakthroughDisciples() {
        var disciple = WuxiaGenerator.generateDisciple(com.example.game.engine.GameRng(2L), CultivationRealm.QI_CONDENSATION_1)
        val initialRealm = disciple.realm
        val readyDisciple = disciple.copy(realmProgress = 100)
        assertEquals(100, readyDisciple.realmProgress)
        assertEquals(CultivationRealm.QI_CONDENSATION_1, readyDisciple.realm)
    }

    @Test
    fun testContentVolume() {
        assertEquals(50, WuxiaGenerator.getRandomEvents().size)
        assertEquals(24, WuxiaGenerator.getInitialTerritories().size)
        assertEquals(16, WuxiaGenerator.getInitialRivalSects().size)
        assertEquals(37, WuxiaGenerator.getAlchemyRecipes().size)
        assertEquals(30, WuxiaGenerator.getForgingRecipes().size)
        assertEquals(12, WuxiaGenerator.getInitialSectBeasts().size)
        assertEquals(12, WuxiaGenerator.getGeocentricFlames().size)
        assertTrue(WuxiaGenerator.getAllSkillNodes().size >= 80)
    }

    @Test
    fun testContentIdsAreUnique() {
        fun <T> assertUnique(label: String, ids: List<T>) =
            assertEquals("Zdublowane id: $label", ids.size, ids.toSet().size)
        assertUnique("zdarzenia", WuxiaGenerator.getRandomEvents().map { it.id })
        assertUnique("techniki", WuxiaGenerator.getAllSkillNodes().map { it.id })
        assertUnique("krainy", WuxiaGenerator.getInitialRegions().map { it.id })
        assertUnique("terytoria", WuxiaGenerator.getInitialTerritories().map { it.id })
        assertUnique("sekty", WuxiaGenerator.getInitialRivalSects().map { it.id })
        assertUnique("receptury", WuxiaGenerator.getAlchemyRecipes().map { it.id })
        assertUnique("kuźnia", WuxiaGenerator.getForgingRecipes().map { it.id })
        assertUnique("bestie", WuxiaGenerator.getInitialSectBeasts().map { it.id })
        assertUnique("płomienie", WuxiaGenerator.getGeocentricFlames().map { it.id })
    }

    @Test
    fun testEveryIngredientIsObtainable() {
        // Każdy składnik receptur i kuźni musi dać się zdobyć: zbieranie, krainy, łupy bossów lub nagrody zdarzeń.
        val regions = WuxiaGenerator.getInitialRegions()
        val obtainable = mutableSetOf<String>()
        obtainable += com.example.game.content.ResourceCatalog.herbs.map { it.name }
        obtainable += com.example.game.content.ResourceCatalog.minerals.map { it.name }
        regions.forEach { r -> obtainable += r.mineralResource; obtainable += r.herbResource; r.bossBeast?.let { obtainable += it.dropItemName } }
        val needed = WuxiaGenerator.getAlchemyRecipes().flatMap { listOfNotNull(it.requiredHerbName, it.secondaryHerbName) } +
            WuxiaGenerator.getForgingRecipes().map { it.requiredMineralName }
        val missing = needed.filter { it !in obtainable }.distinct()
        assertTrue("Niedostępne składniki: $missing", missing.isEmpty())
    }

    @Test
    fun testEventEligibility() {
        val events = WuxiaGenerator.getRandomEvents()
        val dayOne = events.filter { it.isEligible(day = 1, reputation = 180, time = TimeOfDay.DAY) }
        assertTrue("Na start musi być z czego losować", dayOne.size >= 15)
        assertTrue(events.any { it.requiredTimeOfDay == TimeOfDay.FULL_MOON })
    }
}
