package com.example.game

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.game.engine.ActiveCombat
import com.example.game.engine.GameCommand
import com.example.game.engine.GameContent
import com.example.game.engine.GameEngine
import com.example.game.engine.GameEvent
import com.example.game.engine.GameState
import com.example.model.*
import com.example.ui.audio.WuxiaAudioEngine
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/** Kształt danych dla interfejsu. Budowany z [GameState] – nigdy nie zmieniany bezpośrednio. */
data class WuxiaUiState(
    val sectName: String = "Sekta Niebiańskiego Smoka",
    val sectMasterName: String = "Mistrz Chen Tian",
    val spiritStones: Int = 850,
    val sectReputation: Int = 180,
    val sectContribution: Int = 450,
    val currentDay: Int = 1,
    val timeOfDay: TimeOfDay = TimeOfDay.DAY,
    val disciples: List<Disciple> = emptyList(),
    val availableRecruits: List<Disciple> = emptyList(),
    val inventory: Map<String, Int> = emptyMap(),
    val facilities: List<SectFacility> = emptyList(),
    val regions: List<WorldRegion> = emptyList(),
    val rivalSects: List<RivalSect> = emptyList(),
    val skills: List<SkillNode> = emptyList(),
    val flames: List<GeocentricFlame> = emptyList(),
    val activeFlame: GeocentricFlame? = null,
    val vaultArtifacts: List<Artifact> = emptyList(),
    val activeRandomEvent: RandomEvent? = null,
    val combatState: CombatState = CombatState(),
    val multiplayerRankings: List<MultiplayerSectRanking> = emptyList(),
    val worldBoss: WorldBossRaid = WuxiaGenerator.getInitialWorldBoss(),
    val serverPingMs: Int = 14,
    val gameSpeedMultiplier: Int = 1,
    val autoBreakthroughEnabled: Boolean = true,
    val autoUsePillsForBreakthrough: Boolean = true,
    val notificationMessage: String? = null,
    val currentSelectedDiscipleId: String? = null,
    val sectLeader: SectLeaderData = WuxiaGenerator.getInitialSectLeader(),
    val currentPolicy: SectPolicy = SectPolicy.RIGHTEOUS_DAO,
    val sectBeasts: List<SectDivineBeast> = emptyList(),
    val activeBeast: SectDivineBeast? = null,
    val territories: List<TerritoryNode> = emptyList()
)

/**
 * Cienka warstwa między silnikiem a Compose: pętla czasu, zamiana zdarzeń na komunikaty,
 * dźwięki i animacje walki. Cała logika gry jest w [GameEngine].
 */
class WuxiaGameViewModel(application: Application) : AndroidViewModel(application) {

    private var game: GameState = GameEngine.newGame(worldSeed = System.currentTimeMillis(), nowMs = System.currentTimeMillis())

    // Stan wyłącznie interfejsu (nie trafia do zapisu).
    private var notification: String? = null
    private var selectedDiscipleId: String? = game.disciples.firstOrNull()?.id
    private var speed: Int = 1
    private var ping: Int = 14
    private var combatFx = CombatFx()

    private val _uiState = MutableStateFlow(buildUi())
    val uiState: StateFlow<WuxiaUiState> = _uiState.asStateFlow()

    private var gameLoopJob: Job? = null
    private var combatJob: Job? = null

    init {
        startGameLoop()
    }

    /** Jedyna droga zmiany stanu gry. */
    private fun dispatch(command: GameCommand) {
        val result = GameEngine.apply(game, command)
        game = result.state
        result.events.forEach(::onEvent)
        _uiState.value = buildUi()
    }

    private fun onEvent(event: GameEvent) {
        when (event) {
            is GameEvent.Notice -> notification = event.message
            is GameEvent.CombatHit -> onCombatHit(event)
            is GameEvent.CombatEnded -> if (event.victory) WuxiaAudioEngine.playDivineGong()
            is GameEvent.Breakthrough -> Unit
        }
    }

    private fun startGameLoop() {
        gameLoopJob?.cancel()
        gameLoopJob = viewModelScope.launch {
            while (isActive) {
                delay((1600 / speed).toLong())
                ping = 12 + kotlin.random.Random.nextInt(5) // kosmetyka sieci, nie logika gry
                dispatch(GameCommand.Tick)
            }
        }
    }

    // --- Ustawienia i zaznaczenia (tylko UI) ---

    fun setGameSpeed(newSpeed: Int) {
        speed = newSpeed.coerceAtLeast(1)
        _uiState.value = buildUi()
    }

    fun selectDisciple(id: String) {
        selectedDiscipleId = id
        _uiState.value = buildUi()
    }

    fun clearNotification() {
        notification = null
        _uiState.value = buildUi()
    }

    // --- Uczniowie i kultywacja ---

    fun recruitDisciple(discipleId: String) = dispatch(GameCommand.Recruit(discipleId))
    fun refreshRecruits() = dispatch(GameCommand.RefreshRecruits)
    fun recruitAllAvailable() = dispatch(GameCommand.RecruitAll)
    fun assignRole(discipleId: String, newRole: DiscipleRole) = dispatch(GameCommand.AssignRole(discipleId, newRole))
    fun toggleAutoBreakthrough(enabled: Boolean? = null) = dispatch(GameCommand.ToggleAutoBreakthrough(enabled))
    fun toggleAutoUsePills(enabled: Boolean? = null) = dispatch(GameCommand.ToggleAutoPills(enabled))
    fun breakthroughAllReadyDisciples() = dispatch(GameCommand.BreakthroughAll)
    fun attemptBreakthrough(discipleId: String, usePill: Boolean) = dispatch(GameCommand.AttemptBreakthrough(discipleId, usePill))
    fun unlockSkill(discipleId: String, skillId: String) = dispatch(GameCommand.UnlockSkill(discipleId, skillId))
    fun usePill(discipleId: String, pillName: String) = dispatch(GameCommand.UsePill(discipleId, pillName))

    // --- Rzemiosło ---

    fun brewPill(recipe: AlchemyRecipe) = dispatch(GameCommand.BrewPill(recipe.id))
    fun forgeArtifact(recipe: ForgingRecipe) = dispatch(GameCommand.ForgeArtifact(recipe.id))
    fun equipArtifact(discipleId: String, artifact: Artifact) = dispatch(GameCommand.EquipArtifact(discipleId, artifact))
    fun selectGeocentricFlame(flame: GeocentricFlame) = dispatch(GameCommand.SelectFlame(flame.id))
    fun refineFlame(flameId: String) = dispatch(GameCommand.RefineFlame(flameId))
    fun upgradeFacility(type: FacilityType) = dispatch(GameCommand.UpgradeFacility(type))

    // --- Walka ---

    fun startExpedition(regionId: String, discipleId: String) {
        combatFx = CombatFx()
        dispatch(GameCommand.StartExpedition(regionId, discipleId))
        combatJob?.cancel()
        combatJob = viewModelScope.launch {
            while (isActive) {
                val c = game.combat ?: break
                if (c.finished) break
                delay(if (c.discipleTurn) 750 else 650)
                dispatch(GameCommand.CombatStep)
            }
        }
    }

    fun combatManualAttack() = dispatch(GameCommand.CombatManualAttack)
    fun combatUsePill() = dispatch(GameCommand.CombatUsePill)
    fun combatUltimate() = dispatch(GameCommand.CombatUltimate)

    fun closeCombat() {
        combatJob?.cancel()
        combatFx = CombatFx()
        dispatch(GameCommand.CloseCombat)
    }

    private fun onCombatHit(hit: GameEvent.CombatHit) {
        when (hit.kind) {
            GameEvent.HitKind.SLASH -> WuxiaAudioEngine.playSwordClash()
            GameEvent.HitKind.CRIT -> WuxiaAudioEngine.playThunderStrike()
            GameEvent.HitKind.ULTIMATE -> WuxiaAudioEngine.playUltimateBurst()
            else -> Unit
        }
        val text = when (hit.kind) {
            GameEvent.HitKind.CRIT -> "-${hit.amount} CRIT!"
            GameEvent.HitKind.ULTIMATE -> "-${hit.amount} BOSKIE DAO!"
            GameEvent.HitKind.HEAL -> "+${hit.amount} HP"
            else -> "-${hit.amount}"
        }
        val floating = FloatingDamage(
            id = System.nanoTime(),
            text = text,
            isCrit = hit.kind == GameEvent.HitKind.CRIT || hit.kind == GameEvent.HitKind.ULTIMATE,
            isHeal = hit.kind == GameEvent.HitKind.HEAL,
            isDisciple = hit.onDisciple
        )
        combatFx = combatFx.copy(
            effect = hit.kind.name,
            counter = combatFx.counter + 1,
            floating = (combatFx.floating + floating).takeLast(6)
        )
    }

    // --- Świat ---

    fun interactRivalSect(sectId: String, actionType: String) = dispatch(GameCommand.InteractRivalSect(sectId, actionType))
    fun captureTerritory(territoryId: String, discipleId: String) = dispatch(GameCommand.CaptureTerritory(territoryId, discipleId))
    fun assignGarrison(territoryId: String, discipleId: String?) = dispatch(GameCommand.AssignGarrison(territoryId, discipleId))
    fun attackWorldBoss(discipleId: String) = dispatch(GameCommand.AttackWorldBoss(discipleId))
    fun fightArenaDuel(discipleId: String, rivalRanking: MultiplayerSectRanking) =
        dispatch(GameCommand.ArenaDuel(discipleId, rivalRanking.sectName))
    fun handleRandomEventChoice(choiceIndex: Int) = dispatch(GameCommand.EventChoice(choiceIndex))
    fun dismissRandomEvent() = dispatch(GameCommand.DismissEvent)

    // --- Patriarcha i sekta ---

    fun toggleSectDecree(decreeId: String) = dispatch(GameCommand.ToggleDecree(decreeId))
    fun upgradePatriarchPrestige() = dispatch(GameCommand.UpgradePrestige)
    fun changeSectPolicy(policy: SectPolicy) = dispatch(GameCommand.ChangePolicy(policy))
    fun setActiveBeast(beastId: String) = dispatch(GameCommand.SetActiveBeast(beastId))
    fun feedSectBeast(beastId: String, herbName: String) = dispatch(GameCommand.FeedBeast(beastId, herbName))
    fun evolveSectBeast(beastId: String) = dispatch(GameCommand.EvolveBeast(beastId))

    // --- Mapowanie stanu gry na interfejs ---

    private data class CombatFx(
        val effect: String = "",
        val counter: Int = 0,
        val floating: List<FloatingDamage> = emptyList()
    )

    private fun combatUi(c: ActiveCombat?): CombatState {
        if (c == null) return CombatState(active = false)
        val enemy = c.enemy
        return CombatState(
            active = true,
            discipleName = c.discipleName,
            discipleSchool = c.discipleSchool,
            discipleHp = c.discipleHp,
            discipleMaxHp = c.discipleMaxHp,
            discipleQi = c.discipleQi,
            enemyName = enemy?.let { "${it.title} - ${it.name}" } ?: "",
            enemyHp = c.enemyHp,
            enemyMaxHp = c.enemyMaxHp,
            logs = c.logs,
            isVictory = c.outcome,
            rewardText = c.rewardText,
            currentEffect = combatFx.effect,
            effectCounter = combatFx.counter,
            floatingDamages = combatFx.floating
        )
    }

    private fun buildUi(): WuxiaUiState = WuxiaUiState(
        sectName = game.sectName,
        sectMasterName = game.sectMasterName,
        spiritStones = game.spiritStones,
        sectReputation = game.sectReputation,
        sectContribution = game.sectContribution,
        currentDay = game.currentDay,
        timeOfDay = game.timeOfDay,
        disciples = game.disciples,
        availableRecruits = game.availableRecruits,
        inventory = game.inventory,
        facilities = game.facilities,
        regions = game.regions,
        rivalSects = game.rivalSects,
        skills = GameContent.skills,
        flames = game.flames,
        activeFlame = game.activeFlame,
        vaultArtifacts = game.vaultArtifacts,
        activeRandomEvent = GameContent.event(game.activeEventId),
        combatState = combatUi(game.combat),
        multiplayerRankings = GameContent.rankings,
        worldBoss = game.worldBoss,
        serverPingMs = ping,
        gameSpeedMultiplier = speed,
        autoBreakthroughEnabled = game.autoBreakthroughEnabled,
        autoUsePillsForBreakthrough = game.autoUsePillsForBreakthrough,
        notificationMessage = notification,
        currentSelectedDiscipleId = selectedDiscipleId,
        sectLeader = game.sectLeader,
        currentPolicy = game.currentPolicy,
        sectBeasts = game.sectBeasts,
        activeBeast = game.activeBeast,
        territories = game.territories
    )
}
