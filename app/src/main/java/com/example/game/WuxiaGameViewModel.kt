package com.example.game

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.game.engine.ActiveCombat
import com.example.game.engine.DiscipleRules
import com.example.game.engine.SectCommands
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
    val qiBreakdown: List<String> = emptyList(),
    // Etap 2
    val morale: Int = 60,
    val food: Int = 0,
    val promotableIds: Set<String> = emptySet(),
    /** Ile dni zostało do końca egzaminu (0 = zamknięty). */
    val examDaysLeft: Int = 0,
    val nextExamInDays: Int = 0,
    val admissionCost: Int = 0,
    val feastReadyInDays: Int = 0,
    // Etap 3
    /** Uczeń → powód blokady wielkiego przełomu (brak wpisu = można próbować albo nie jest na szczycie). */
    val bottlenecks: Map<String, String> = emptyMap(),
    /** Zwoje metod w magazynie. */
    val scrolls: List<String> = emptyList(),
    val karma: Int = 0,
    val tribulation: TribulationUi? = null,
    // Etap 4
    val economy: EconomyUi = EconomyUi(),
    // Etap 5
    val expedition: ExpeditionRun? = null,
    val lootSlots: Int = 4,
    val hasRing: Boolean = false,
    val towerFloor: Int = 0,
    val towerAttemptsLeft: Int = 3,
    /** Strażnik następnego piętra: HP / atak / obrona. */
    val towerNext: Triple<Int, Int, Int> = Triple(0, 0, 0),
    /** Krainy z aktywnym tropem płomienia (id krainy → nazwa płomienia + pora roku). */
    val flameTrails: Map<String, String> = emptyMap(),
    // Etap 5b
    val raid: Raid? = null,
    val gates: Map<String, String> = emptyMap(),
    val gatePowers: Map<String, Int> = emptyMap(),
    val formationNodes: List<SpiritElement> = emptyList(),
    val formationFactor: Double = 1.0,
    val formationNextCost: String? = null,
    val spies: List<SpyMission> = emptyList(),
    val intel: Map<String, Int> = emptyMap(),
    val chronicle: List<String> = emptyList(),
    // Etap 5c
    val discoveredRegionIds: Set<String> = emptySet(),
    val secretRealms: List<SecretRealmUi> = emptyList(),
    val catastrophe: Catastrophe? = null,
    val imperialFavor: Int = 0,
    val empireFallen: Boolean = false,
    // Etap 6
    val secrets: SecretsUi = SecretsUi(),
    // Etap 7
    val sectLife: SectLifeUi = SectLifeUi(),
    // Etap 8
    val meta: MetaProgress = MetaProgress(),
    val rebirthAvailable: Boolean = false,
    val essenceOnRebirth: Int = 0,
    val achievements: List<AchievementUi> = emptyList(),
    // Etap 9
    val arena: ArenaUi = ArenaUi(),
    // Etap 10: autonomia – uczniowie decydują sami, mistrz doradza
    val autonomy: Boolean = false,
    /** Ogólna szansa (%), że uczeń posłucha rady. */
    val obedience: Map<String, Int> = emptyMap(),
    /** Szansa przyjęcia rady o zajęciu: uczeń → rola → %. */
    val roleAdvice: Map<String, Map<DiscipleRole, Int>> = emptyMap(),
    /** Zajęcie, które uczeń wybrałby sam. */
    val preferredRoles: Map<String, DiscipleRole> = emptyMap(),
    // Etap 11
    val weeklyGoal: com.example.model.SectGoal? = null,
    val weeklyProgress: Long = 0,
    val goalsWon: Int = 0,
    val goalsLost: Int = 0,
    val bonds: List<com.example.model.Bond> = emptyList(),
    val masterFocus: Int = 3,
    val opportunity: com.example.model.Opportunity? = null,
    val steward: com.example.model.StewardPolicy = com.example.model.StewardPolicy(),
    // Etap 12: szanse wielkiego przełomu (bez pigułki, z pigułką) – tribulacja: przeżycie
    val majorOdds: Map<String, Pair<Int, Int>> = emptyMap(),
    val focusNextIn: Int = 0
)

data class ArenaUi(
    val online: Boolean = false,
    val busy: Boolean = false,
    val rating: Int = 1_000,
    val wins: Int = 0,
    val losses: Int = 0,
    val opponents: List<com.example.game.engine.DaoNet.ShadowTeam> = emptyList(),
    val ranking: List<com.example.game.net.DaoNetClient.RankingRow> = emptyList(),
    val myShadowId: String? = null,
    val cloudLocal: com.example.game.engine.CloudSave.Summary? = null,
    val cloudRemote: com.example.game.engine.CloudSave.Summary? = null,
    val cloudSuggestion: com.example.game.engine.CloudSave.Choice? = null,
    val error: String? = null
)

data class AchievementUi(val id: String, val name: String, val description: String, val unlocked: Boolean, val title: Boolean)

data class SectLifeUi(
    val karmaPath: String = "",
    val storyAct: Int = 0,
    val storyEnding: Int = 0,
    val chosenId: String? = null,
    val patronId: String? = null,
    val templeLevel: Int = 0,
    val templeNextCost: Int = 0,
    val treasury: List<Artifact> = emptyList(),
    val ancestors: List<AncestorRecord> = emptyList(),
    val chapters: List<String> = emptyList(),
    val fengShuiTotal: Double = 0.0,
    /** Budynek → numer miejsca (0..5). */
    val buildingSlots: Map<FacilityType, Int> = emptyMap(),
    val dungeonDepth: Int = 0,
    val dungeonAttemptsLeft: Int = 0,
    val caveLevel: Int = 0,
    val eggs: List<BeastEgg> = emptyList(),
    val mounts: Map<String, String> = emptyMap(),
    val flameSpirits: Set<String> = emptySet(),
    val companions: List<Companionship> = emptyList(),
    /** Uczeń → długość życia w latach. */
    val lifespans: Map<String, Int> = emptyMap(),
    val starsReadToday: Boolean = false
)

data class SecretsUi(
    val journal: List<com.example.game.engine.JournalView.Entry> = emptyList(),
    val discovered: Int = 0,
    val total: Int = 0,
    /** Punkty-zatory, które sekta już zna. */
    val knownBlockages: Set<Int> = emptySet(),
    /** Gwiazdozbiory znane na pewno: nazwa → punkty. */
    val knownConstellations: Map<String, List<Int>> = emptyMap(),
    val contemplationObjects: List<String> = emptyList(),
    /** Metoda → liczba fragmentów zwoju. */
    val fragments: Map<String, Int> = emptyMap(),
    val mirrors: Int = 0,
    val researcherId: String? = null,
    val breaths: List<String> = com.example.game.engine.RulesGen.BREATHS,
    val places: List<String> = com.example.game.engine.RulesGen.PLACES
)

data class SecretRealmUi(
    val id: String,
    val name: String,
    val description: String,
    val maxRealm: String,
    val open: Boolean,
    /** Dni do otwarcia (0 = otwarte). */
    val inDays: Int,
    val visited: Boolean,
    val loot: String
)

data class MarketRow(val item: String, val have: Int, val sellPrice: Int, val buyPrice: Int?)
data class FlameRow(val id: String, val name: String, val rank: String, val position: Int, val temperature: Int, val refinement: Int, val where: String, val bearerRealm: String)
data class TalismanRow(val name: String, val have: Int, val cost: String, val description: String)

data class EconomyUi(
    val enabled: Boolean = false,
    val season: Season = Season.SPRING,
    val seasonDay: Int = 1,
    val veinLevel: Int = 1,
    val veinName: String = "",
    val veinQi: Double = 1.0,
    val veinUpgradeDoneDay: Int = 0,
    val veinNextCost: String? = null,
    val fieldTier: Int = 2,
    val fields: List<FieldPlot> = emptyList(),
    val fieldSize: Int = 4,
    val seedHerbs: List<String> = emptyList(),
    val sellable: List<MarketRow> = emptyList(),
    val buyable: List<MarketRow> = emptyList(),
    val caravans: List<Caravan> = emptyList(),
    val contracts: List<Contract> = emptyList(),
    val blackOpen: Boolean = false,
    val blackOffers: List<com.example.game.engine.Market.BlackOffer> = emptyList(),
    val knownRecipeIds: Set<String> = emptySet(),
    val flames: List<FlameRow> = emptyList(),
    val talismans: List<TalismanRow> = emptyList(),
    /** Ulepszanie: indeks w magazynie → (szansa %, koszt). */
    val upgrades: List<Triple<Int, Int, String>> = emptyList()
)

data class TribulationUi(
    val id: Long,
    val discipleName: String,
    val strikes: Int,
    val damage: List<Int>,
    val maxHp: Int,
    val survived: Boolean,
    val perfect: Boolean,
    val catastrophe: Boolean
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
    // Etap 9: Sieć Dao (stan interfejsu, nie trafia do zapisu).
    private var arena: ArenaUi = ArenaUi()
    /** Treść ostatniej awarii (panel debug). */
    var lastCrash: String? = com.example.CrashLog.consume(application.filesDir).also { if (it != null) notification = "Poprzednia sesja zakończyła się błędem – raport w panelu debug." }
        private set

    private val store = SaveStore(File(application.filesDir, "saves"))
    private var game: GameState = loadOrNewGame()

    private var selectedDiscipleId: String? = game.disciples.firstOrNull()?.id
    private var speed: Int = 1
    private var ping: Int = 14
    private var combatFx = CombatFx()
    private var offlineReport: OfflineProgress.Report? = null
    private var celebration: CelebrationUi? = null
    private var tribulation: TribulationUi? = null

    // Sieć Dao: zadeklarowane przed _uiState, bo buildUi() czyta net.enabled już w konstruktorze.
    private val prefs by lazy { getApplication<Application>().getSharedPreferences("daonet", android.content.Context.MODE_PRIVATE) }
    private val net by lazy { com.example.game.net.DaoNetClient(
        com.example.BuildConfig.DAONET_URL, com.example.BuildConfig.DAONET_ANON_KEY,
        object : com.example.game.net.DaoNetClient.TokenStore {
            override var accessToken: String?
                get() = prefs.getString("token", null)
                set(v) { prefs.edit().putString("token", v).apply() }
            override var userId: String?
                get() = prefs.getString("user", null)
                set(v) { prefs.edit().putString("user", v).apply() }
        }
    ) }
    private val cloudPayload by lazy { mutableMapOf<String, String>() }

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
            return GameEngine.normalize(state).copy(lastActiveAtMs = now)
        }
        if (loaded?.error != null) notification = "Nie udało się wczytać zapisu – rozpoczęto nową grę."
        return GameEngine.newGame(worldSeed = now, nowMs = now)
    }

    private fun startAutosave() {
        viewModelScope.launch {
            while (isActive) {
                delay(AUTOSAVE_MS)
                // Etap 8: czas realny tylko dla celów dziennych (silnik odrzuca cofnięty zegar).
                dispatch(GameCommand.RealTime(System.currentTimeMillis()))
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
        dispatch(GameCommand.RealTime(System.currentTimeMillis()))
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
        // Żelazna Wola: jeden zapis, bez wczytywania kopii (save.bak zostaje tylko na wypadek błędu technicznego).
        check(!game.meta.ironWill) { "Żelazna Wola" }
        game = store.import(text, System.currentTimeMillis())
        notification = "Wczytano zapis."
        _uiState.value = buildUi()
    }.isSuccess

    /** Jedyna droga zmiany stanu gry. */
    private fun dispatch(command: GameCommand) {
        // Etap 10: polecenia dotyczące ucznia są radami – uczeń może odmówić.
        val cmd = if (game.autonomy && com.example.game.engine.Autonomy.subject(command) != null) GameCommand.Advise(command) else command
        val result = GameEngine.apply(game, cmd)
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
            // Wynik rady pokazuje już komunikat (słucha / odmawia).
            is GameEvent.AdviceOutcome -> Unit
            is GameEvent.Tribulation -> tribulation = event.result.let { r ->
                TribulationUi(System.nanoTime(), game.disciple(event.discipleId)?.name ?: "Uczeń", r.strikes, r.damagePerStrike, r.maxHp, r.survived, r.perfect, r.catastrophe)
            }
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
    fun breakthroughAllReadyDisciples() {
        // Etap 10: wielkie przełomy to decyzja ucznia – mistrz doradza każdemu gotowemu z osobna.
        if (!game.autonomy) return dispatch(GameCommand.BreakthroughAll)
        val ready = game.disciples.filter { com.example.game.engine.CultivationSystem.atMajorGate(game, it) }
        if (ready.isEmpty()) return dispatch(GameCommand.BreakthroughAll)
        ready.forEach { dispatch(GameCommand.AttemptBreakthrough(it.id, game.autoUsePillsForBreakthrough)) }
    }
    /** Rada „wstrzymaj się z ryzykiem”. */
    fun adviseHold(discipleId: String) = dispatch(GameCommand.Hold(discipleId))
    // Etap 11
    fun equip(discipleId: String, vaultIndex: Int) = dispatch(GameCommand.Equip(discipleId, vaultIndex))
    fun unequip(discipleId: String, slot: com.example.model.EquipSlot) = dispatch(GameCommand.Unequip(discipleId, slot))
    fun meditateWith(discipleId: String) = dispatch(GameCommand.MeditateWith(discipleId))
    fun praise(discipleId: String) = dispatch(GameCommand.Praise(discipleId))
    fun makePromise(discipleId: String, reward: String) = dispatch(GameCommand.MakePromise(discipleId, reward))
    fun acceptOpportunity(discipleId: String?) = dispatch(GameCommand.AcceptOpportunity(discipleId))
    fun declineOpportunity() = dispatch(GameCommand.DeclineOpportunity)
    fun setSteward(policy: com.example.model.StewardPolicy) = dispatch(GameCommand.SetSteward(policy))
    fun attemptBreakthrough(discipleId: String, usePill: Boolean) = dispatch(GameCommand.AttemptBreakthrough(discipleId, usePill))
    fun unlockSkill(discipleId: String, skillId: String) = dispatch(GameCommand.UnlockSkill(discipleId, skillId))
    fun usePill(discipleId: String, pillName: String) = dispatch(GameCommand.UsePill(discipleId, pillName))

    // --- Etap 2: życie sekty ---

    fun promote(discipleId: String) = dispatch(GameCommand.Promote(discipleId))
    fun punish(discipleId: String) = dispatch(GameCommand.Punish(discipleId))
    fun pardon(discipleId: String) = dispatch(GameCommand.Pardon(discipleId))
    fun testCandidate(candidateId: String) = dispatch(GameCommand.TestCandidate(candidateId))
    fun buyFood(rations: Int) = dispatch(GameCommand.BuyFood(rations))
    fun holdFeast() = dispatch(GameCommand.HoldFeast)

    // --- Etap 3: kultywacja ---

    fun choosePath(discipleId: String, path: CultivationPath) = dispatch(GameCommand.ChoosePath(discipleId, path))
    fun learnMethod(discipleId: String, scroll: String) = dispatch(GameCommand.LearnMethod(discipleId, scroll))
    fun chooseLaw(discipleId: String, law: DaoLaw) = dispatch(GameCommand.ChooseLaw(discipleId, law))
    fun seclude(discipleId: String, days: Int) = dispatch(GameCommand.Seclude(discipleId, days))
    fun endSeclusion(discipleId: String) = dispatch(GameCommand.EndSeclusion(discipleId))
    fun awakenBloodline(discipleId: String) = dispatch(GameCommand.AwakenBloodline(discipleId))
    fun ascend(discipleId: String) = dispatch(GameCommand.Ascend(discipleId))

    fun dismissTribulation() {
        tribulation = null
        _uiState.value = buildUi()
    }

    // --- Rzemiosło ---

    fun brewPill(recipe: AlchemyRecipe) = dispatch(GameCommand.BrewPill(recipe.id))
    fun forgeArtifact(recipe: ForgingRecipe) = dispatch(GameCommand.ForgeArtifact(recipe.id))
    fun equipArtifact(discipleId: String, artifact: Artifact) = dispatch(GameCommand.EquipArtifact(discipleId, artifact))
    fun selectGeocentricFlame(flame: GeocentricFlame) = dispatch(GameCommand.SelectFlame(flame.id))
    fun refineFlame(flameId: String) = dispatch(GameCommand.RefineFlame(flameId))
    fun upgradeFacility(type: FacilityType) = dispatch(GameCommand.UpgradeFacility(type))

    // --- Etap 5a: wyprawy, Wieża Prób ---
    fun beginExpedition(regionId: String, discipleId: String, auto: Boolean, companionId: String? = null) = dispatch(GameCommand.BeginExpedition(regionId, discipleId, auto, companionId))
    fun chooseNode(index: Int) = dispatch(GameCommand.ChooseNode(index))
    fun enterLair() {
        combatFx = CombatFx()
        dispatch(GameCommand.EnterLair)
        if (game.combat != null) startCombatLoop()
    }
    fun retreatExpedition() = dispatch(GameCommand.RetreatExpedition)
    fun craftRing() = dispatch(GameCommand.CraftRing)
    fun climbTower(discipleId: String) = dispatch(GameCommand.ClimbTower(discipleId))

    // --- Etap 5b: żywe sekty ---
    fun guardGate(gate: String, discipleId: String?) = dispatch(GameCommand.GuardGate(gate, discipleId))
    fun placeFormationNode(element: SpiritElement) = dispatch(GameCommand.PlaceFormationNode(element))
    fun sendSpy(discipleId: String, sectId: String, type: SpyType) = dispatch(GameCommand.SendSpy(discipleId, sectId, type))
    fun sectBattle(sectId: String, discipleIds: List<String>) = dispatch(GameCommand.SectBattle(sectId, discipleIds))
    fun honorDuel(sectId: String, discipleId: String, toDeath: Boolean, confirmed: Boolean) =
        dispatch(GameCommand.HonorDuel(sectId, discipleId, toDeath, confirmed))

    // --- Etap 5c: świat ---
    fun scoutRegion(regionId: String, discipleId: String) = dispatch(GameCommand.ScoutRegion(regionId, discipleId))
    fun enterSecretRealm(realmId: String, discipleId: String) = dispatch(GameCommand.EnterSecretRealm(realmId, discipleId))

    // --- Etap 6: tajemnice ---
    fun openMeridian(discipleId: String, point: Int) = dispatch(GameCommand.OpenMeridian(discipleId, point))
    fun setBreath(discipleId: String, breath: Int, place: Int) = dispatch(GameCommand.SetBreath(discipleId, breath, place))
    fun decipherScroll(methodId: String) = dispatch(GameCommand.DecipherScroll(methodId))
    fun contemplate(discipleId: String, obj: String) = dispatch(GameCommand.Contemplate(discipleId, obj))
    fun setResearcher(discipleId: String?) = dispatch(GameCommand.SetResearcher(discipleId))
    fun useMirror(discipleId: String) = dispatch(GameCommand.UseMirror(discipleId))

    // --- Etap 7: fabuła i życie sekty ---
    fun assignMentor(studentId: String, mentorId: String?) = dispatch(GameCommand.AssignMentor(studentId, mentorId))
    fun bindCompanions(a: String, b: String) = dispatch(GameCommand.BindCompanions(a, b))
    fun choosePatron(id: String) = dispatch(GameCommand.ChoosePatron(id))
    fun upgradeTemple() = dispatch(GameCommand.UpgradeTemple)
    fun placeTreasure(vaultIndex: Int) = dispatch(GameCommand.PlaceTreasure(vaultIndex))
    fun removeTreasure(index: Int) = dispatch(GameCommand.RemoveTreasure(index))
    fun swapBuildings(a: FacilityType, b: FacilityType) = dispatch(GameCommand.SwapBuildings(a, b))
    fun delveDungeon(discipleId: String) = dispatch(GameCommand.DelveDungeon(discipleId))
    fun copyScroll(scroll: String) = dispatch(GameCommand.CopyScroll(scroll))
    fun upgradeCave() = dispatch(GameCommand.UpgradeCave)
    fun incubateEgg() = dispatch(GameCommand.IncubateEgg)
    fun mountBeast(beastId: String, discipleId: String?) = dispatch(GameCommand.MountBeast(beastId, discipleId))
    fun fuseFlames(a: String, b: String) = dispatch(GameCommand.FuseFlames(a, b, confirmed = true, confirmedTwice = true))
    fun devourFlame(eater: String, food: String) = dispatch(GameCommand.DevourFlame(eater, food))
    fun assignArt(discipleId: String, art: DaoArt?) = dispatch(GameCommand.AssignArt(discipleId, art))
    fun cure(discipleId: String) = dispatch(GameCommand.Cure(discipleId))
    fun cleanse(discipleId: String) = dispatch(GameCommand.Cleanse(discipleId))
    fun readStars() = dispatch(GameCommand.ReadStars)

    // --- Etap 9: Sieć Dao i chmura ---

    private fun season() = com.example.game.engine.DaoNet.season(System.currentTimeMillis() / com.example.game.engine.DailyGoals.DAY_MS)

    private fun arenaUpdate(f: (ArenaUi) -> ArenaUi) {
        arena = f(arena)
        _uiState.value = buildUi()
    }

    /** Odśwież arenę: online – wyślij swój cień i pobierz rywali; offline – cienie sekt-rywali. */
    fun refreshArena() {
        if (!net.enabled) {
            arenaUpdate { it.copy(opponents = com.example.game.engine.DaoNet.offlineOpponents(game), error = null) }
            return
        }
        arenaUpdate { it.copy(busy = true, error = null) }
        viewModelScope.launch(Dispatchers.IO) {
            val r = runCatching {
                val id = net.uploadShadow(com.example.game.engine.DaoNet.shadow(game), season())
                Triple(id, net.opponents(season(), game.pvpRating), net.ranking(season()))
            }
            launch(Dispatchers.Main) {
                r.onSuccess { (id, opp, rank) -> arenaUpdate { it.copy(busy = false, myShadowId = id, opponents = opp.ifEmpty { com.example.game.engine.DaoNet.offlineOpponents(game) }, ranking = rank) } }
                    .onFailure { e -> arenaUpdate { it.copy(busy = false, error = e.message, opponents = com.example.game.engine.DaoNet.offlineOpponents(game)) } }
            }
        }
    }

    fun fightShadow(opponent: com.example.game.engine.DaoNet.ShadowTeam) {
        val seed = com.example.game.engine.GameRng.stream(game.worldSeed, game.step, "pvp").nextLong()
        dispatch(GameCommand.PvpDuel(opponent, seed))
        val me = arena.myShadowId
        if (net.enabled && me != null && !opponent.id.startsWith("rival_")) {
            viewModelScope.launch(Dispatchers.IO) { runCatching { net.verifyDuel(me, opponent.id, seed) } }
        }
    }

    fun uploadCloud() {
        if (!net.enabled) return arenaUpdate { it.copy(error = "Zapis w chmurze wymaga połączenia z Siecią Dao.") }
        val text = store.exportText() ?: run { saveNow(); store.exportText() } ?: return
        val sum = com.example.game.engine.CloudSave.summary(game, System.currentTimeMillis())
        viewModelScope.launch(Dispatchers.IO) {
            val r = runCatching { net.uploadSave(text, sum.day, sum.era, sum.topRealm) }
            launch(Dispatchers.Main) { arenaUpdate { it.copy(error = r.exceptionOrNull()?.message, cloudLocal = sum) }; if (r.isSuccess) { notification = "Zapis wysłany do chmury."; _uiState.value = buildUi() } }
        }
    }

    /** Pobierz zapis z chmury i pokaż porównanie – decyzję podejmuje gracz. */
    fun checkCloud() {
        if (!net.enabled) return arenaUpdate { it.copy(error = "Zapis w chmurze wymaga połączenia z Siecią Dao.") }
        viewModelScope.launch(Dispatchers.IO) {
            val r = runCatching { net.downloadSave() }
            launch(Dispatchers.Main) {
                val row = r.getOrNull()
                if (row == null) { arenaUpdate { it.copy(error = r.exceptionOrNull()?.message ?: "W chmurze nie ma zapisu.") }; return@launch }
                cloudPayload["remote"] = row.payload
                val local = com.example.game.engine.CloudSave.summary(game, System.currentTimeMillis())
                val remote = com.example.game.engine.CloudSave.Summary(row.game_day, row.era, row.top_realm, 0, 0L)
                arenaUpdate { it.copy(cloudLocal = local, cloudRemote = remote, cloudSuggestion = com.example.game.engine.CloudSave.suggest(local, remote)) }
            }
        }
    }

    fun useCloudSave() {
        val text = cloudPayload["remote"] ?: return
        if (importSave(text)) arenaUpdate { it.copy(cloudRemote = null, cloudSuggestion = null) }
        else arenaUpdate { it.copy(error = if (game.meta.ironWill) "Żelazna Wola: nie można wczytać innej kopii." else "Zapis z chmury jest uszkodzony.") }
    }

    fun keepLocalSave() = arenaUpdate { it.copy(cloudRemote = null, cloudSuggestion = null) }

    // --- Etap 8: meta ---
    fun rebirth(difficulty: Difficulty?, ironWill: Boolean?) = dispatch(GameCommand.Rebirth(confirmed = true, difficulty = difficulty, ironWill = ironWill))
    fun buyHeavenNode(id: String) = dispatch(GameCommand.BuyHeavenNode(id))
    fun chooseEra(id: String) = dispatch(GameCommand.ChooseEra(id))
    fun succession(discipleId: String) = dispatch(GameCommand.Succession(discipleId, confirmed = true))
    fun setTitle(id: String?) = dispatch(GameCommand.SetTitle(id))
    fun offerToAncestors() = dispatch(GameCommand.OfferToAncestors)

    // --- Etap 4: gospodarka ---
    fun upgradeVein() = dispatch(GameCommand.UpgradeVein)
    fun plantHerb(index: Int, herb: String) = dispatch(GameCommand.PlantHerb(index, herb))
    fun harvestFields() = dispatch(GameCommand.HarvestFields)
    fun expandFields() = dispatch(GameCommand.ExpandFields)
    fun sell(item: String, quantity: Int) = dispatch(GameCommand.Sell(item, quantity))
    fun buy(item: String, quantity: Int) = dispatch(GameCommand.Buy(item, quantity))
    fun sendCaravan(item: String, quantity: Int, guardId: String?) = dispatch(GameCommand.SendCaravan(item, quantity, guardId))
    fun buyBlackMarket(index: Int) = dispatch(GameCommand.BuyBlackMarket(index))
    fun fulfillContract(id: String) = dispatch(GameCommand.FulfillContract(id))
    fun experiment(main: String, second: String?) = dispatch(GameCommand.Experiment(main, second))
    fun upgradeArtifact(vaultIndex: Int) = dispatch(GameCommand.UpgradeArtifact(vaultIndex))
    fun craftTalisman(name: String) = dispatch(GameCommand.CraftTalisman(name))
    fun healWithTalisman(discipleId: String) = dispatch(GameCommand.HealWithTalisman(discipleId))
    fun combatTalisman(name: String) = dispatch(GameCommand.CombatTalisman(name))
    fun flameToForge(flameId: String) = dispatch(GameCommand.FlameToForge(flameId))
    fun flameToCauldron(flameId: String) = dispatch(GameCommand.SelectFlame(flameId))
    fun flameToDisciple(flameId: String, discipleId: String) = dispatch(GameCommand.FlameToDisciple(flameId, discipleId))

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

    /** Podgląd ekranów (testy zrzutów): podmienia stan gry i zatrzymuje pętlę. */
    @androidx.annotation.VisibleForTesting
    fun previewState(state: GameState, selectedDisciple: String? = null) {
        gameLoopJob?.cancel()
        game = state
        selectedDiscipleId = selectedDisciple ?: selectedDiscipleId
        notification = null
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
        activeRandomEvent = GameContent.event(game.activeEventId, game),
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
        qiBreakdown = Modifiers.sect(game).breakdown(Stat.QI_GAIN),
        morale = game.morale,
        food = game.inventory[DiscipleRules.FOOD] ?: 0,
        promotableIds = game.disciples.filter { DiscipleRules.canPromote(game, it) }.map { it.id }.toSet(),
        examDaysLeft = (game.examOpenUntilDay - game.currentDay + 1).coerceAtLeast(0),
        nextExamInDays = DiscipleRules.EXAM_EVERY_DAYS - game.currentDay % DiscipleRules.EXAM_EVERY_DAYS,
        admissionCost = SectCommands.admissionCost(game),
        feastReadyInDays = (game.nextFeastDay - game.currentDay).coerceAtLeast(0),
        bottlenecks = if (!game.e3Initialized) emptyMap() else game.disciples
            .filter { com.example.game.engine.Realms.atPeak(it) }
            .mapNotNull { d -> com.example.game.engine.CultivationSystem.bottleneck(game, d)?.let { d.id to it } }
            .toMap(),
        scrolls = game.inventory.filter { it.value > 0 && it.key.startsWith("Zwój: ") }.keys.sorted(),
        karma = game.karma,
        tribulation = tribulation,
        economy = economyUi(game),
        expedition = game.expedition,
        lootSlots = com.example.game.engine.Expeditions.slots(game),
        hasRing = (game.inventory[com.example.game.engine.Expeditions.RING] ?: 0) > 0,
        towerFloor = game.towerFloor,
        towerAttemptsLeft = com.example.game.engine.Tower.attemptsLeft(game),
        towerNext = com.example.game.engine.Tower.guardian(game.towerFloor + 1).let { Triple(it.hp, it.attack, it.defense) },
        flameTrails = com.example.game.engine.FlameSources.SOURCES
            .filter { (id, _) -> (game.flags[com.example.game.engine.FlameSources.trailFlag(id)] ?: 0) > 0 }
            .map { (id, src) -> src.regionId to ((com.example.game.engine.FlameSources.allFlames[id]?.name ?: id) + (src.season?.let { " · ${it.polishName}" } ?: "")) }
            .toMap(),
        raid = game.raid,
        gates = game.gates,
        gatePowers = com.example.game.engine.Raids.GATES.associateWith { com.example.game.engine.Raids.gatePower(game, it) },
        formationNodes = game.formationNodes,
        formationFactor = com.example.game.engine.Formation.defenseFactor(game),
        formationNextCost = game.formationNodes.size.takeIf { it < com.example.game.engine.Formation.MAX_NODES }
            ?.let { com.example.game.engine.Formation.cost(it) }?.let { (st, m) -> "$st kamieni · ${m}× ${com.example.game.engine.Formation.MINERAL}" },
        spies = game.spies,
        intel = game.intel,
        chronicle = game.chronicle,
        discoveredRegionIds = game.discoveredRegionIds,
        secretRealms = com.example.game.engine.SecretRealms.REALMS.map { r ->
            val opening = com.example.game.engine.SecretRealms.openingDay(r, game.currentDay)
            val next = generateSequence(game.currentDay) { it + 1 }.take(41)
                .firstOrNull { com.example.game.engine.SecretRealms.openingDay(r, it) == it } ?: game.currentDay
            SecretRealmUi(r.id, r.name, r.description, r.maxRealm.polishName, opening != null,
                if (opening != null) 0 else next - game.currentDay, opening != null && game.realmVisits[r.id] == opening,
                r.loot.joinToString { "${it.second}× ${it.first}" })
        },
        catastrophe = game.catastrophe,
        imperialFavor = com.example.game.engine.Empire.favor(game),
        empireFallen = (game.flags[com.example.game.engine.Empire.FALLEN] ?: 0) > 0,
        secrets = secretsUi(game),
        sectLife = SectLifeUi(
            karmaPath = com.example.game.engine.Identity.karmaPath(game),
            storyAct = com.example.game.engine.Story.act(game),
            storyEnding = com.example.game.engine.Story.ending(game),
            chosenId = game.chosenId,
            patronId = game.patronId,
            templeLevel = game.templeLevel,
            templeNextCost = com.example.game.engine.Identity.templeCost(game.templeLevel),
            treasury = game.treasury,
            ancestors = game.ancestors,
            chapters = game.chronicleChapters,
            fengShuiTotal = com.example.game.engine.FengShui.total(game),
            buildingSlots = FacilityType.entries.associateWith { com.example.game.engine.FengShui.slotOf(game, it) },
            dungeonDepth = game.dungeonDepth,
            dungeonAttemptsLeft = com.example.game.engine.Dungeon.attemptsLeft(game),
            caveLevel = game.caveLevel,
            eggs = game.eggs,
            mounts = game.mounts,
            flameSpirits = game.flameSpirits,
            companions = game.companions,
            lifespans = game.disciples.associate { it.id to com.example.game.engine.Lifecycle.lifespan(it) },
            starsReadToday = game.starsReadDay == game.currentDay
        ),
        meta = game.meta,
        autonomy = game.autonomy,
        obedience = game.disciples.associate { it.id to com.example.game.engine.Autonomy.chance(game, it) },
        roleAdvice = game.disciples.associate { d -> d.id to DiscipleRole.entries.associateWith { com.example.game.engine.Autonomy.chance(game, d, GameCommand.AssignRole(d.id, it)) } },
        preferredRoles = game.disciples.associate { it.id to com.example.game.engine.Autonomy.preferredRole(game, it) },
        weeklyGoal = game.weeklyGoal,
        weeklyProgress = game.weeklyGoal?.let { com.example.game.engine.SectGoals.progress(game, it) } ?: 0,
        goalsWon = game.goalsWon,
        goalsLost = game.goalsLost,
        bonds = game.bonds,
        masterFocus = com.example.game.engine.Mentor.focusLeft(game),
        opportunity = game.opportunity,
        steward = game.steward,
        majorOdds = game.disciples.filter { com.example.game.engine.Realms.atPeak(it) && com.example.game.engine.CultivationSystem.ready(it) }.mapNotNull { d ->
            val trib = com.example.game.engine.CultivationSystem.tribulationOdds(game, d, false)
            if (trib != null) d.id to (trib to (com.example.game.engine.CultivationSystem.tribulationOdds(game, d, true) ?: trib))
            else com.example.game.engine.CultivationSystem.majorOdds(game, d, false)?.let { d.id to (it to (com.example.game.engine.CultivationSystem.majorOdds(game, d, true) ?: it)) }
        }.toMap(),
        focusNextIn = com.example.game.engine.Mentor.nextFocusIn(game),
        rebirthAvailable = com.example.game.engine.Rebirth.available(game),
        essenceOnRebirth = com.example.game.engine.Rebirth.essence(game),
        arena = arena.copy(rating = game.pvpRating, wins = game.pvpWins, losses = game.pvpLosses, online = net.enabled),
        achievements = com.example.game.engine.Achievements.ALL.map { a ->
            val got = a.id in game.meta.achievements
            AchievementUi(a.id, if (a.secret && !got) "???" else a.name, if (a.secret && !got) "???" else a.description, got, a.title)
        }
    )

    private fun secretsUi(g: GameState): SecretsUi {
        val r = g.rules ?: return SecretsUi()
        val J = com.example.game.engine.Journal
        val S = com.example.game.engine.Secrets
        return SecretsUi(
            journal = com.example.game.engine.JournalView.entries(g),
            discovered = J.discovered(g),
            total = S.allIds(r).size,
            knownBlockages = r.blockages.filter { J.certain(g, S.blockage(it)) }.toSet(),
            knownConstellations = r.constellations.filter { J.certain(g, S.constellation(it.id)) }.associate { it.name to it.points },
            contemplationObjects = r.lawObjects.keys.filter { com.example.game.engine.Contemplation.available(g, it) }.sorted(),
            fragments = com.example.game.engine.Realms.methods.mapNotNull { m ->
                (g.inventory[com.example.game.engine.Breath.fragmentItem(m.id)] ?: 0).takeIf { it > 0 }?.let { m.id to it }
            }.toMap(),
            mirrors = g.inventory[com.example.game.engine.SecretTools.MIRROR] ?: 0,
            researcherId = g.researcherId
        )
    }

    private fun economyUi(g: GameState): EconomyUi {
        if (!g.e4Initialized) return EconomyUi()
        val M = com.example.game.engine.Market
        val V = com.example.game.engine.Vein
        val catalog = com.example.game.content.ResourceCatalog
        val tier = V.fieldTier(g.veinLevel)
        val cost = V.upgradeCost(g.veinLevel)
        val buyList = (catalog.herbs + catalog.minerals).map { it.name }.filter { M.buyable(it) } + M.SPIRIT_PAPER
        return EconomyUi(
            enabled = true,
            season = com.example.game.engine.Seasons.of(g.currentDay),
            seasonDay = com.example.game.engine.Seasons.dayOfSeason(g.currentDay),
            veinLevel = g.veinLevel,
            veinName = V.name(g.veinLevel),
            veinQi = V.qiMultiplier(g.veinLevel),
            veinUpgradeDoneDay = g.veinUpgradeDoneDay,
            veinNextCost = cost?.let { "${it.stones} kamieni · ${it.mineralCount}× ${it.mineral} · ${it.days} dni" },
            fieldTier = tier,
            fields = g.fields,
            fieldSize = com.example.game.engine.Fields.size(g),
            seedHerbs = catalog.herbs.filter { it.tier <= tier && (g.inventory[it.name] ?: 0) > 0 }.map { it.name },
            sellable = g.inventory.filter { it.value > 0 && M.basePrice(it.key) != null }.keys.sorted()
                .map { MarketRow(it, g.inventory[it] ?: 0, M.sellPrice(g, it), if (M.buyable(it)) M.buyPrice(g, it) else null) },
            buyable = buyList.map { MarketRow(it, g.inventory[it] ?: 0, M.sellPrice(g, it), M.buyPrice(g, it)) },
            caravans = g.caravans,
            contracts = g.contracts.filter { !it.done },
            blackOpen = M.blackMarketOpen(g),
            blackOffers = M.blackOffers(g),
            knownRecipeIds = g.knownRecipeIds,
            flames = g.flames.map { f ->
                val info = com.example.game.engine.Flames.info(f.id)
                FlameRow(f.id, f.name, info.rank.polishName, info.position, com.example.game.engine.Creatures.flameTemperature(g, f),
                    f.refinementLevel, com.example.game.engine.Flames.where(g, f.id), info.rank.bearerRealm.polishName)
            }.sortedBy { it.position },
            talismans = com.example.game.engine.Talismans.recipes.map {
                TalismanRow(it.name, g.inventory[it.name] ?: 0, "${it.paper}× papier · ${it.ingredientCount}× ${it.ingredient} · ${it.stones} kam.", it.description)
            },
            upgrades = g.vaultArtifacts.mapIndexed { i, a ->
                Triple(i, com.example.game.engine.CraftV2.upgradeChance(a.plus), "${100 * (a.plus + 1) * (a.plus + 1)} kam.")
            }
        )
    }
}
