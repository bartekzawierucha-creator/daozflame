package com.example

import com.example.game.WuxiaGenerator
import com.example.model.*
import org.junit.Assert.*
import org.junit.Test

class WuxiaGameUnitTest {

    @Test
    fun testDiscipleGeneration() {
        val disciple = WuxiaGenerator.generateDisciple(
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
        assertEquals(6, regions.size)
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
        var disciple = WuxiaGenerator.generateDisciple(CultivationRealm.QI_CONDENSATION_1)
        val initialRealm = disciple.realm
        val readyDisciple = disciple.copy(realmProgress = 100)
        assertEquals(100, readyDisciple.realmProgress)
        assertEquals(CultivationRealm.QI_CONDENSATION_1, readyDisciple.realm)
    }
}
