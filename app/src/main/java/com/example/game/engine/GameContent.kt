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
    /** Wszystkie zdarzenia jako skrypty: dawne (przeniesione) + łańcuchy Etapu 5. */
    val scripts: List<com.example.model.ScriptEvent> by lazy {
        WuxiaGenerator.getRandomEvents().map { EventScripts.fromLegacy(it) } + com.example.game.content.ScriptCatalog.events + com.example.game.content.NpcCatalog.events
    }
    val rankings: List<MultiplayerSectRanking> by lazy { WuxiaGenerator.getMultiplayerRankings() }

    private val skillById by lazy { skills.associateBy { it.id } }
    private val scriptById by lazy { scripts.associateBy { it.id } }
    private val recipeById by lazy { alchemyRecipes.associateBy { it.id } }
    private val recipeByPill by lazy { alchemyRecipes.associateBy { it.pillName } }
    private val forgeById by lazy { forgingRecipes.associateBy { it.id } }

    fun skill(id: String): SkillNode? = skillById[id]
    fun script(id: String?): com.example.model.ScriptEvent? = id?.let { scriptById[it] }
    /** Widok zdarzenia (kształt dawnego [RandomEvent]); ze stanem – zablokowane wybory są oznaczone. */
    fun event(id: String?, state: GameState? = null): RandomEvent? = script(id)?.let { EventScripts.view(it, state) }
    fun recipe(id: String): AlchemyRecipe? = recipeById[id]
    fun recipeForPill(pillName: String): AlchemyRecipe? = recipeByPill[pillName]
    fun forging(id: String): ForgingRecipe? = forgeById[id]

    const val BREAKTHROUGH_PILL = "Pigułka Przełomu Qi"
    const val FLAME_CRYSTAL = "Kryształ Geocentrycznego Ognia"
}
