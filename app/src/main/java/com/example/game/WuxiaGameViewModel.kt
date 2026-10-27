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
import com.example.game.engine.Modifiers
import com.example.game.engine.OfflineProgress
import com.example.game.engine.Stat
import com.example.game.engine.Tutorial
import com.example.game.save.SaveStore
import com.example.model.*
import com.example.ui.audio.WuxiaAudioEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File

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
    val territories: List<TerritoryNode> = emptyList(),
    /** Podsumowanie „Gdy cię nie było” do pokazania (null = brak). */
    val offlineReport: OfflineProgress.Report? = null,
    val tutorial: TutorialUi? = null,
    /** Ostatnia celebracja (np. przełom) – id rośnie przy każdej nowej. */
    val celebration: CelebrationUi? = null,
    /** Rozbicie przyrostu Qi sekty: „źródło: wartość” (tooltip „skąd to?”). */
    val qiBreakdown: List<String> = emptyList()
)

data class TutorialUi(val index: Int, val total: Int, val title: String, val text: String, val waitingForAction: Boolean)

data class CelebrationUi(val id: Long, val text: String)

/**
 * Cienka warstwa między silnikiem a Compose: pętla czasu, zamiana zdarzeń na komunikaty,
 * dźwięki i animacje walki. Cała logika gry jest w [GameEngine].
 */
class WuxiaGameViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        const val AUTOSAVE_MS = 30_000L
    }

    // Stan wyłącznie interfejsu (nie trafia do zapisu). Zadeklarowany PRZED wczytaniem gry,
    // bo loadOrNewGame() może ustawić komunikat i czas nieobecności.
    private var notification: String? = null
    private var pendingOfflineMs: Long = 0L

    private val store = SaveStore(File(application.filesDir, "saves"))
    private var game: GameState = loadOrNewGame()

    private var selectedDiscipleId: String? = game.disciples.firstOrNull()?.id
    private var speed: Int = 1
    private var ping: Int = 14
    private var combatFx = CombatFx()
    private var offlineReport: OfflineProgress.Report? = null
    private var celebration: CelebrationUi? = null

    private val _uiState = MutableStateFlow(buildUi())
    val uiState: StateFlow<WuxiaUiState> = _uiState.asStateFlow()

    private var gameLoopJob: Job? = null
    private var combatJob: Job? = null

    init {
        if (pendingOfflineMs > 0) dispatch(GameCommand.OfflineCatchUp(pendingOfflineMs))
        startGameLoop()
        startAutosave()
        if (game.combat?.finished == false) startCombatLoop()
    }

    // --- Zapis gry ---

    private fun loadOrNewGame(): GameState {
        val now = System.currentTimeMillis()
        val loaded = runCatching { store.load() }.getOrNull()
        loaded?.state?.let { state ->
            if (loaded.fromBackup) notification = "Główny zapis był uszkodzony – wczytano kopię zapasową."
            pendingOfflineMs = if (FeatureFlags.OFFLINE_PROGRESS && state.lastActiveAtMs > 0) now - state.lastActiveAtMs else 0L
            return state.copy(lastActiveAtMs = now)
        }
        if (loaded?.error != null) notification = "Nie udało się wczytać zapisu – rozpoczęto nową grę."
        return GameEngine.newGame(worldSeed = now, nowMs = now)
    }

    private fun startAutosave() {
        viewModelScope.launch {
            while (isActive) {
                delay(AUTOSAVE_MS)
                saveNow()
            }
        }
    }

    /** Zapis w tle. Stan jest niemutowalny, więc wysyłamy migawkę bez kopiowania. */
    fun saveNow() {
        val snapshot = game.copy(lastActiveAtMs = System.currentTimeMillis())
        viewModelScope.launch(Dispatchers.IO + NonCancellable) {
            runCatching { store.save(snapshot, snapshot.lastActiveAtMs) }
        }
    }

    private var backgroundSinceMs: Long = 0L

    /** Aplikacja schowana: zapis i pauza pętli (oszczędność baterii). */
    fun onBackground() {
        backgroundSinceMs = System.currentTimeMillis()
        gameLoopJob?.cancel()
        saveNow()
    }

    /** Powrót do gry: nadrobienie czasu nieobecności i wznowienie pętli. */
    fun onForeground() {
        val since = backgroundSinceMs
        backgroundSinceMs = 0L
        if (since > 0 && FeatureFlags.OFFLINE_PROGRESS) {
            dispatch(GameCommand.OfflineCatchUp(System.currentTimeMillis() - since))
        }
        if (gameLoopJob?.isActive != true) startGameLoop()
    }

    /** Zapis synchroniczny – przy niszczeniu ViewModelu nie ma już zakresu korutyn. */
    override fun onCleared() {
        runCatching { store.save(game.copy(lastActiveAtMs = System.currentTimeMillis()), System.currentTimeMillis()) }
        super.onCleared()
    }

    /** Nowa gra (z potwierdzeniem w UI). Usuwa zapis. */
    fun startNewGame() {
        combatJob?.cancel()
        store.delete()
        val now = System.currentTimeMillis()
        game = GameEngine.newGame(worldSeed = now, nowMs = now)
        selectedDiscipleId = game.disciples.firstOrNull()?.id
        combatFx = CombatFx()
        notification = "Rozpoczęto nową grę."
        _uiState.value = buildUi()
        saveNow()
    }

    fun exportSave(): String? = store.exportText()

    fun importSave(text: String): Boolean = runCatching {
        game = store.import(text, System.currentTimeMillis())
        notification = "Wczytano zapis."
        _uiState.value = buildUi()
    }.isSuccess

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
            is GameEvent.Breakthrough -> if (event.success) {
                val d = game.disciple(event.discipleId)
                celebration = CelebrationUi(System.nanoTime(), "${d?.name?.substringBefore(" (") ?: "Uczeń"}: ${d?.realm?.polishName ?: "przełom"}!")
            }
            is GameEvent.OfflineSummary -> offlineReport = event.report
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

    fun claimOfflineReport() {
        offlineReport = null
        _uiState.value = buildUi()
    }

    fun tutorialNext() = dispatch(GameCommand.TutorialNext)
    fun tutorialSkip() = dispatch(GameCommand.TutorialSkip)

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
        startCombatLoop()
    }

    private fun startCombatLoop() {
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

    // --- Narzędzia debug (tylko build debug) ---

    fun debug(command: GameCommand) {
        if (com.example.BuildConfig.DEBUG) dispatch(command)
    }

    /** Przewija [count] cykli gry bez rysowania każdego z osobna. */
    fun debugSkipTicks(count: Int) {
        if (!com.example.BuildConfig.DEBUG) return
        repeat(count) { game = GameEngine.apply(game, GameCommand.Tick).state }
        notification = "[debug] przewinięto $count cykli (dzień ${game.currentDay})"
        _uiState.value = buildUi()
    }

    fun debugInfo(): String =
        "ziarno ${game.worldSeed} · krok ${game.step} · tick ${game.tick} · dzień ${game.currentDay}"

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
        territories = game.territories,
        offlineReport = offlineReport,
        tutorial = Tutorial.current(game)?.let { step ->
            TutorialUi(game.tutorialStep, Tutorial.steps.size, step.title, step.text, waitingForAction = step.goal != null)
        },
        celebration = celebration,
        qiBreakdown = Modifiers.sect(game).breakdown(Stat.QI_GAIN)
    )
}
