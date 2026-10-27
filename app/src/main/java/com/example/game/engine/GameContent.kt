package com.example.game.engine

import com.example.game.WuxiaGenerator
import com.example.model.AlchemyRecipe
import com.example.model.ForgingRecipe
import com.example.model.MultiplayerSectRanking
import com.example.model.RandomEvent
import com.example.model.SkillNode

/**
 * Treść statyczna gry, liczona raz. Generator buduje listy przy każdym wywołaniu,
 * więc silnik i UI korzystają z tej pamięci podręcznej.
 */
object GameContent {
    val skills: List<SkillNode> by lazy { WuxiaGenerator.getAllSkillNodes() }
    val alchemyRecipes: List<AlchemyRecipe> by lazy { WuxiaGenerator.getAlchemyRecipes() }
    val forgingRecipes: List<ForgingRecipe> by lazy { WuxiaGenerator.getForgingRecipes() }
    val events: List<RandomEvent> by lazy { WuxiaGenerator.getRandomEvents() }
    val rankings: List<MultiplayerSectRanking> by lazy { WuxiaGenerator.getMultiplayerRankings() }

    private val skillById by lazy { skills.associateBy { it.id } }
    private val eventById by lazy { events.associateBy { it.id } }
    private val recipeById by lazy { alchemyRecipes.associateBy { it.id } }
    private val recipeByPill by lazy { alchemyRecipes.associateBy { it.pillName } }
    private val forgeById by lazy { forgingRecipes.associateBy { it.id } }

    fun skill(id: String): SkillNode? = skillById[id]
    fun event(id: String?): RandomEvent? = id?.let { eventById[it] }
    fun recipe(id: String): AlchemyRecipe? = recipeById[id]
    fun recipeForPill(pillName: String): AlchemyRecipe? = recipeByPill[pillName]
    fun forging(id: String): ForgingRecipe? = forgeById[id]

    const val BREAKTHROUGH_PILL = "Pigułka Przełomu Qi"
    const val FLAME_CRYSTAL = "Kryształ Geocentrycznego Ognia"
}
