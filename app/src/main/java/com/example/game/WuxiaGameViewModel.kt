package com.example.game

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.model.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.random.Random
import com.example.data.room.DiscipleCultivationRepository
import com.example.ui.audio.WuxiaAudioEngine

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

class WuxiaGameViewModel(
    private val cultivationRepository: DiscipleCultivationRepository? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(WuxiaUiState())
    val uiState: StateFlow<WuxiaUiState> = _uiState.asStateFlow()

    private var gameLoopJob: Job? = null
    private var combatJob: Job? = null

    init {
        initGameWorld()
        startGameLoop()
    }

    private fun initGameWorld() {
        val initialDisciples = listOf(
            WuxiaGenerator.generateDisciple(CultivationRealm.FOUNDATION_EARLY, BloodlineType.GOLDEN_DRAGON).copy(
                name = "Li Long (Starszy Uczeń)",
                martialSchool = MartialSchool.SWORD_IMMORTAL
            ),
            WuxiaGenerator.generateDisciple(CultivationRealm.QI_CONDENSATION_9, BloodlineType.ANCIENT_PHOENIX).copy(
                name = "Yue Qing (Adepta Ognia)",
                martialSchool = MartialSchool.ELEMENTAL_TAOIST
            ),
            WuxiaGenerator.generateDisciple(CultivationRealm.QI_CONDENSATION_5, BloodlineType.TITANIC_ASURA).copy(
                name = "Wu Zhen (Pięściarz Asury)",
                martialSchool = MartialSchool.BODY_ASURA
            ),
            WuxiaGenerator.generateDisciple(CultivationRealm.QI_CONDENSATION_5, BloodlineType.HEAVENLY_NETHER_VIPER).copy(
                name = "Ye Mo (Cień Otchłani)",
                martialSchool = MartialSchool.SHADOW_VIPER
            ),
            WuxiaGenerator.generateDisciple(CultivationRealm.FOUNDATION_LATE, BloodlineType.IMMORTAL_LOTUS_HEART).copy(
                name = "Lin Xue (Szermierka Lotosu)",
                martialSchool = MartialSchool.SWORD_IMMORTAL
            ),
            WuxiaGenerator.generateDisciple(CultivationRealm.FOUNDATION_EARLY, BloodlineType.PURPLE_LIGHTNING_QILIN).copy(
                name = "Bai Chen (Władca Pioruna)",
                martialSchool = MartialSchool.ELEMENTAL_TAOIST
            )
        )

        val initialRecruits = listOf(
            WuxiaGenerator.generateDisciple(CultivationRealm.QI_CONDENSATION_1),
            WuxiaGenerator.generateDisciple(CultivationRealm.QI_CONDENSATION_1),
            WuxiaGenerator.generateDisciple(CultivationRealm.QI_CONDENSATION_5),
            WuxiaGenerator.generateDisciple(CultivationRealm.QI_CONDENSATION_5),
            WuxiaGenerator.generateDisciple(CultivationRealm.QI_CONDENSATION_9),
            WuxiaGenerator.generateDisciple(CultivationRealm.FOUNDATION_EARLY)
        )

        val startingInventory = mapOf(
            "Trawa Wiecznej Wiosny" to 35,
            "Śnieżny Lotos Oświecenia" to 25,
            "Fioletowy Korzeń Skorpiona" to 20,
            "Płomienny Żeń-szeń Smoka" to 18,
            "Kwiat Niebiańskiego Zrozumienia" to 15,
            "Duchowy Nefryt" to 30,
            "Gwiezdne Żelazo" to 24,
            "Kryształ Geocentrycznego Ognia" to 18,
            "Starożytna Ruda Pustki" to 12,
            "Pigułka Przełomu Qi" to 10
        )

        val flamesList = WuxiaGenerator.getGeocentricFlames()

        _uiState.value = WuxiaUiState(
            spiritStones = 1200,
            disciples = initialDisciples,
            availableRecruits = initialRecruits,
            inventory = startingInventory,
            facilities = WuxiaGenerator.getInitialFacilities(),
            regions = WuxiaGenerator.getInitialRegions(),
            rivalSects = WuxiaGenerator.getInitialRivalSects(),
            skills = WuxiaGenerator.getAllSkillNodes(),
            flames = flamesList,
            activeFlame = flamesList.first(),
            multiplayerRankings = WuxiaGenerator.getMultiplayerRankings(),
            currentSelectedDiscipleId = initialDisciples.firstOrNull()?.id,
            sectBeasts = WuxiaGenerator.getInitialSectBeasts(),
            activeBeast = WuxiaGenerator.getInitialSectBeasts().firstOrNull(),
            territories = WuxiaGenerator.getInitialTerritories()
        )

        cultivationRepository?.let { repo ->
            viewModelScope.launch {
                repo.syncFromDisciples(initialDisciples)
            }
        }
    }

    private fun startGameLoop() {
        gameLoopJob?.cancel()
        gameLoopJob = viewModelScope.launch {
            var cycleTick = 0
            while (isActive) {
                val speed = _uiState.value.gameSpeedMultiplier
                delay((1600 / speed).toLong())
                cycleTick++

                _uiState.update { state ->
                    // 1. Advance Time of Day
                    val nextTime = when (state.timeOfDay) {
                        TimeOfDay.DAWN -> TimeOfDay.DAY
                        TimeOfDay.DAY -> TimeOfDay.DUSK
                        TimeOfDay.DUSK -> TimeOfDay.NIGHT
                        TimeOfDay.NIGHT -> if (Random.nextInt(10) < 3) TimeOfDay.FULL_MOON else TimeOfDay.DAWN
                        TimeOfDay.FULL_MOON -> TimeOfDay.DAWN
                    }
                    val newDay = if (nextTime == TimeOfDay.DAWN) state.currentDay + 1 else state.currentDay

                    // 2. Disciples tick: Gain Qi & produce resources based on roles
                    var gatheredStones = 2 * state.disciples.size
                    val newInventory = state.inventory.toMutableMap()

                    // Dochód z kontrolowanych terytoriów sekty
                    state.territories.filter { it.controllingSectId == "player" }.forEach { terr ->
                        when {
                            terr.resourceType.contains("Kamienie") -> gatheredStones += terr.yieldAmountPerCycle
                            terr.resourceType.contains("Zioła") -> {
                                newInventory["Trawa Wiecznej Wiosny"] = (newInventory["Trawa Wiecznej Wiosny"] ?: 0) + terr.yieldAmountPerCycle
                            }
                            terr.resourceType.contains("Minerały") -> {
                                newInventory["Gwiezdne Żelazo"] = (newInventory["Gwiezdne Żelazo"] ?: 0) + terr.yieldAmountPerCycle
                            }
                            terr.resourceType.contains("Płomienia") -> {
                                newInventory["Kryształ Geocentrycznego Ognia"] = (newInventory["Kryształ Geocentrycznego Ognia"] ?: 0) + terr.yieldAmountPerCycle
                            }
                            else -> {
                                newInventory[terr.resourceType] = (newInventory[terr.resourceType] ?: 0) + terr.yieldAmountPerCycle
                            }
                        }
                    }

                    // Bonusy kultywacyjne z Systemu Bestii, Sojuszy Sekt i Polityki
                    val beastBonusPercent = state.activeBeast?.let { beast ->
                        (beast.tier * 12) + (beast.intimacy / 4)
                    } ?: 0

                    val allianceBonusPercent = state.rivalSects
                        .filter { it.status == DiplomaticStatus.ALLIANCE }
                        .sumOf { it.cultivationBuffBonusPercent }

                    val policyBonusPercent = when (state.currentPolicy) {
                        SectPolicy.RIGHTEOUS_DAO -> 25
                        SectPolicy.ISOLATIONIST -> 35
                        SectPolicy.EXPANSIONIST -> 10
                        SectPolicy.DEMONIC_RAIDERS -> 15
                        SectPolicy.MERCHANT_CONSORTIUM -> 5
                    }

                    val updatedDisciples = state.disciples.map { disciple ->
                        when (disciple.role) {
                            DiscipleRole.MEDITATING -> {
                                // 1. Podstawowa absorpcja Qi ze zrozumienia Dantian
                                val baseGain = (10 + (disciple.comprehension / 8))

                                // 2. Mnożnik Rangi Talentu (SSS: 1.8x, SS: 1.5x, S: 1.25x, A: 1.1x, B: 1.0x)
                                val talentMultiplier = when (disciple.talentGrade) {
                                    TalentGrade.SSS -> 1.8
                                    TalentGrade.SS -> 1.5
                                    TalentGrade.S -> 1.25
                                    TalentGrade.A -> 1.1
                                    TalentGrade.B -> 1.0
                                }

                                // 3. Bonusy Aktywnych Dekretów Sekty Patriarchy
                                val decreeBonusPercent = if (state.sectLeader.activeDecrees.any { it.id == "dec_1" && it.isActive }) 35 else 0

                                // 4. Cechy Przeznaczenia (Fated Traits)
                                val traitBonusPercent = disciple.fatedTraits.sumOf { it.cultivationBonusPercent }

                                // 5. Wpływ Dziedzictwa Starożytnego Rodu
                                val clanBonusPercent = if (disciple.ancientClan.name.contains("Ye")) 50 else 0

                                val totalMultiplier = talentMultiplier * (1.0 + (decreeBonusPercent + traitBonusPercent + clanBonusPercent + beastBonusPercent + allianceBonusPercent + policyBonusPercent) / 100.0)
                                val gain = (baseGain * totalMultiplier).toInt().coerceAtLeast(1)

                                // 6. Szansa na Błysk Oświecenia Dao (Epiphany) na podstawie Karmy
                                var bonusDao = 0
                                var epiphanyGain = 0
                                if (Random.nextInt(100) < (disciple.karmicLuck / 25).coerceIn(3, 20)) {
                                    bonusDao = 1
                                    epiphanyGain = 12
                                }

                                val newProgress = (disciple.realmProgress + gain + epiphanyGain).coerceAtMost(100)

                                // Trwały zapis w bazie Room
                                cultivationRepository?.let { repo ->
                                    viewModelScope.launch {
                                        repo.updateQiProgress(disciple.id, newProgress, disciple.realm.maxQi)
                                    }
                                }

                                disciple.copy(
                                    realmProgress = newProgress,
                                    daoPoints = disciple.daoPoints + bonusDao
                                )
                            }
                            DiscipleRole.HERB_GATHERING -> {
                                if (Random.nextBoolean()) {
                                    val herb = listOf("Trawa Wiecznej Wiosny", "Śnieżny Lotos Oświecenia", "Fioletowy Korzeń Skorpiona").random()
                                    newInventory[herb] = (newInventory[herb] ?: 0) + 1
                                }
                                disciple
                            }
                            DiscipleRole.MINING -> {
                                if (Random.nextBoolean()) {
                                    val mineral = listOf("Duchowy Nefryt", "Gwiezdne Żelazo").random()
                                    newInventory[mineral] = (newInventory[mineral] ?: 0) + 1
                                }
                                disciple
                            }
                            DiscipleRole.COMBAT_TRAINING -> {
                                if (Random.nextInt(100) < 40) {
                                    disciple.copy(daoPoints = disciple.daoPoints + 1)
                                } else disciple
                            }
                            DiscipleRole.DIPLOMACY -> {
                                gatheredStones += 5
                                disciple
                            }
                            DiscipleRole.SECT_GUARD -> disciple
                        }
                    }

                    // 3. Auto-Breakthrough for ready disciples
                    var autoNotification: String? = null
                    var reputationGained = 0
                    val finalDisciples = if (state.autoBreakthroughEnabled) {
                        updatedDisciples.map { disciple ->
                            if (disciple.realmProgress >= 100) {
                                val (upgraded, msg, rep) = executeSingleBreakthrough(
                                    disciple = disciple,
                                    usePill = state.autoUsePillsForBreakthrough,
                                    inventory = newInventory,
                                    timeOfDay = nextTime
                                )
                                if (msg != null && autoNotification == null) {
                                    autoNotification = msg
                                }
                                reputationGained += rep
                                upgraded
                            } else {
                                disciple
                            }
                        }
                    } else {
                        updatedDisciples
                    }

                    // 4. Random Event Trigger (chance every 16 ticks if none active)
                    var currentEvent = state.activeRandomEvent
                    if (currentEvent == null && cycleTick % 16 == 0 && Random.nextInt(100) < 65) {
                        currentEvent = WuxiaGenerator.getRandomEvents().random()
                    }

                    // 5. Subtle ping jitter for low-latency multiplayer realism
                    val newPing = (12 + Random.nextInt(5))

                    state.copy(
                        currentDay = newDay,
                        timeOfDay = nextTime,
                        spiritStones = state.spiritStones + gatheredStones,
                        sectReputation = state.sectReputation + reputationGained,
                        inventory = newInventory,
                        disciples = finalDisciples,
                        activeRandomEvent = currentEvent,
                        serverPingMs = newPing,
                        notificationMessage = autoNotification ?: state.notificationMessage
                    )
                }
            }
        }
    }

    fun setGameSpeed(speed: Int) {
        _uiState.update { it.copy(gameSpeedMultiplier = speed) }
    }

    fun selectDisciple(id: String) {
        _uiState.update { it.copy(currentSelectedDiscipleId = id) }
    }

    fun recruitDisciple(discipleId: String) {
        val state = _uiState.value
        val recruit = state.availableRecruits.find { it.id == discipleId } ?: return
        val cost = 120

        if (state.spiritStones < cost) {
            showNotification("Zbyt mało Kamieni Ducha na ceremonię inicjacji ($cost wymagane)!")
            return
        }

        _uiState.update { current ->
            current.copy(
                spiritStones = current.spiritStones - cost,
                sectReputation = current.sectReputation + 10,
                disciples = current.disciples + recruit,
                availableRecruits = current.availableRecruits.filter { it.id != discipleId },
                notificationMessage = "Przyjęto nowego adepta: ${recruit.name} do Sekty!"
            )
        }

        cultivationRepository?.let { repo ->
            viewModelScope.launch {
                repo.syncFromDisciples(listOf(recruit))
            }
        }
    }

    fun refreshRecruits() {
        val cost = 60
        if (_uiState.value.spiritStones < cost) {
            showNotification("Potrzeba $cost Kamieni Ducha na zwołanie próby rekrutacyjnej!")
            return
        }
        _uiState.update { current ->
            current.copy(
                spiritStones = current.spiritStones - cost,
                availableRecruits = listOf(
                    WuxiaGenerator.generateDisciple(CultivationRealm.QI_CONDENSATION_1),
                    WuxiaGenerator.generateDisciple(CultivationRealm.QI_CONDENSATION_1),
                    WuxiaGenerator.generateDisciple(CultivationRealm.QI_CONDENSATION_5),
                    WuxiaGenerator.generateDisciple(CultivationRealm.QI_CONDENSATION_5),
                    WuxiaGenerator.generateDisciple(CultivationRealm.QI_CONDENSATION_9),
                    WuxiaGenerator.generateDisciple(CultivationRealm.FOUNDATION_EARLY)
                ),
                notificationMessage = "Przybyli nowi kandydaci (6 adeptów) szukający mistrza Dao!"
            )
        }
    }

    fun recruitAllAvailable() {
        val state = _uiState.value
        val recruits = state.availableRecruits
        if (recruits.isEmpty()) {
            showNotification("Brak kandydatów w Sali Rekrutacji!")
            return
        }
        val costPerUnit = 120
        val totalCost = recruits.size * costPerUnit
        if (state.spiritStones < totalCost) {
            showNotification("Zbyt mało Kamieni Ducha na masową rekrutację ($totalCost wymagane)!")
            return
        }

        _uiState.update { current ->
            current.copy(
                spiritStones = current.spiritStones - totalCost,
                sectReputation = current.sectReputation + (recruits.size * 10),
                disciples = current.disciples + recruits,
                availableRecruits = emptyList(),
                notificationMessage = "Przyjęto jednocześnie ${recruits.size} nowych adeptów do Sekty!"
            )
        }

        cultivationRepository?.let { repo ->
            viewModelScope.launch {
                repo.syncFromDisciples(recruits)
            }
        }
    }

    fun assignRole(discipleId: String, newRole: DiscipleRole) {
        _uiState.update { current ->
            val updated = current.disciples.map {
                if (it.id == discipleId) it.copy(role = newRole) else it
            }
            current.copy(
                disciples = updated,
                notificationMessage = "Zmieniono zadanie ucznia na: ${newRole.polishName}"
            )
        }
    }

    fun toggleAutoBreakthrough(enabled: Boolean? = null) {
        _uiState.update { current ->
            val newValue = enabled ?: !current.autoBreakthroughEnabled
            current.copy(
                autoBreakthroughEnabled = newValue,
                notificationMessage = if (newValue) "⚡ Włączono automatyczny przełom kultywacji adeptów!" else "⏸️ Wyłączono automatyczny przełom."
            )
        }
    }

    fun toggleAutoUsePills(enabled: Boolean? = null) {
        _uiState.update { current ->
            val newValue = enabled ?: !current.autoUsePillsForBreakthrough
            current.copy(
                autoUsePillsForBreakthrough = newValue,
                notificationMessage = if (newValue) "💊 Auto-przełom będzie zużywać Pigułki Przełomu Qi (+35% szansy)." else "Auto-przełom nie będzie zużywać pigułek."
            )
        }
    }

    fun breakthroughAllReadyDisciples() {
        val state = _uiState.value
        val ready = state.disciples.filter { it.realmProgress >= 100 }
        if (ready.isEmpty()) {
            showNotification("Żaden adept nie zgromadził jeszcze 100% Qi.")
            return
        }

        val newInventory = state.inventory.toMutableMap()
        var totalRep = 0
        val messages = mutableListOf<String>()

        val updatedDisciples = state.disciples.map { disciple ->
            if (disciple.realmProgress >= 100) {
                val (upgraded, msg, rep) = executeSingleBreakthrough(
                    disciple = disciple,
                    usePill = state.autoUsePillsForBreakthrough,
                    inventory = newInventory,
                    timeOfDay = state.timeOfDay
                )
                if (msg != null) messages.add(msg)
                totalRep += rep
                upgraded
            } else disciple
        }

        _uiState.update { current ->
            current.copy(
                disciples = updatedDisciples,
                inventory = newInventory,
                sectReputation = current.sectReputation + totalRep,
                notificationMessage = "🌟 Dokonano masowego przełomu dla ${ready.size} adeptów!"
            )
        }
    }

    private fun executeSingleBreakthrough(
        disciple: Disciple,
        usePill: Boolean,
        inventory: MutableMap<String, Int>,
        timeOfDay: TimeOfDay
    ): Triple<Disciple, String?, Int> {
        val pillName = "Pigułka Przełomu Qi"
        var pillBonus = 0
        if (usePill) {
            val pillCount = inventory[pillName] ?: 0
            if (pillCount > 0) {
                inventory[pillName] = pillCount - 1
                pillBonus = 35
            }
        }

        var chance = 40 + (disciple.karmicLuck / 5) + pillBonus
        if (timeOfDay == TimeOfDay.FULL_MOON) {
            chance += 45
        }
        if (disciple.bloodline == BloodlineType.PURE_MORTAL) {
            chance += 15
        }

        val success = Random.nextInt(100) < chance
        val nextRealm = getNextRealm(disciple.realm)

        return if (success && nextRealm != null) {
            val updated = disciple.copy(
                realm = nextRealm,
                realmProgress = 0,
                maxHp = (disciple.maxHp * 1.45).toInt(),
                currentHp = (disciple.maxHp * 1.45).toInt(),
                attack = (disciple.attack * 1.4).toInt(),
                defense = (disciple.defense * 1.35).toInt(),
                daoPoints = disciple.daoPoints + 2
            )
            Triple(updated, "🌟 ${disciple.name} osiągnął poziom: ${nextRealm.polishName}!", 25)
        } else {
            val updated = disciple.copy(
                realmProgress = 40,
                currentHp = (disciple.currentHp * 0.7).toInt().coerceAtLeast(10)
            )
            Triple(updated, "Przełom ${disciple.name} napotkał tribulację Qi (zredukowano do 40%).", 0)
        }
    }

    fun attemptBreakthrough(discipleId: String, usePill: Boolean) {
        val state = _uiState.value
        val disciple = state.disciples.find { it.id == discipleId } ?: return

        if (disciple.realmProgress < 100) {
            showNotification("Uczeń musi zgromadzić 100% Qi w swoim meridianie przed próbą przełomu!")
            return
        }

        val newInventory = state.inventory.toMutableMap()
        val (updatedDisciple, msg, rep) = executeSingleBreakthrough(
            disciple = disciple,
            usePill = usePill,
            inventory = newInventory,
            timeOfDay = state.timeOfDay
        )

        _uiState.update { current ->
            current.copy(
                disciples = current.disciples.map { if (it.id == discipleId) updatedDisciple else it },
                inventory = newInventory,
                sectReputation = current.sectReputation + rep,
                notificationMessage = msg
            )
        }
    }

    private fun getNextRealm(current: CultivationRealm): CultivationRealm? {
        val all = CultivationRealm.values()
        val index = all.indexOf(current)
        return if (index < all.size - 1) all[index + 1] else null
    }

    fun unlockSkill(discipleId: String, skillId: String) {
        val state = _uiState.value
        val disciple = state.disciples.find { it.id == discipleId } ?: return
        val skill = state.skills.find { it.id == skillId } ?: return

        if (disciple.unlockedSkillIds.contains(skillId)) {
            showNotification("Ta technika walki została już opanowana!")
            return
        }

        if (disciple.daoPoints < skill.costDaoPoints) {
            showNotification("Zbyt mało punktów Dao (${skill.costDaoPoints} wymagane)!")
            return
        }

        if (disciple.martialSchool != skill.martialSchool) {
            showNotification("Uczeń należy do innej szkoły walki!")
            return
        }

        val updatedSkills = disciple.unlockedSkillIds + skillId
        val newHp = disciple.maxHp + skill.hpBonus
        val newAtk = disciple.attack + skill.attackBonus
        val newDef = disciple.defense + skill.defenseBonus
        val newCp = (newHp / 2) + (newAtk * 14) + (newDef * 9) + (disciple.comprehension * 6) + (disciple.karmicLuck * 5)

        val updatedDisciple = disciple.copy(
            daoPoints = disciple.daoPoints - skill.costDaoPoints,
            unlockedSkillIds = updatedSkills,
            attack = newAtk,
            defense = newDef,
            maxHp = newHp,
            currentHp = disciple.currentHp + skill.hpBonus,
            combatPower = newCp
        )

        _uiState.update { current ->
            current.copy(
                disciples = current.disciples.map { if (it.id == discipleId) updatedDisciple else it },
                notificationMessage = "Uczeń ${disciple.name} opanował technikę: ${skill.name}!"
            )
        }
    }

    fun brewPill(recipe: AlchemyRecipe) {
        val state = _uiState.value
        val chamber = state.facilities.find { it.type == FacilityType.ALCHEMY_CHAMBER }
        val chamberLevel = chamber?.level ?: 1

        if (chamberLevel < recipe.requiredAlchemyChamberLevel) {
            showNotification("Zbyt niski poziom Sali Alchemii! Wymagany: ${recipe.requiredAlchemyChamberLevel} (Obecny: $chamberLevel). Rozbuduj Salę Alchemii w Kampusie!")
            return
        }

        val currentHerbCount = state.inventory[recipe.requiredHerbName] ?: 0
        if (currentHerbCount < recipe.requiredHerbCount) {
            showNotification("Brak składnika głównego: ${recipe.requiredHerbName} (${recipe.requiredHerbCount} wymagane)!")
            return
        }

        if (recipe.secondaryHerbName != null && recipe.secondaryHerbCount > 0) {
            val currentSecCount = state.inventory[recipe.secondaryHerbName] ?: 0
            if (currentSecCount < recipe.secondaryHerbCount) {
                showNotification("Brak składnika pomocniczego: ${recipe.secondaryHerbName} (${recipe.secondaryHerbCount} wymagane)!")
                return
            }
        }

        if (state.spiritStones < recipe.requiredSpiritStones) {
            showNotification("Brak Kamieni Ducha (${recipe.requiredSpiritStones} wymagane)!")
            return
        }

        val newInventory = state.inventory.toMutableMap()
        newInventory[recipe.requiredHerbName] = currentHerbCount - recipe.requiredHerbCount
        if (recipe.secondaryHerbName != null && recipe.secondaryHerbCount > 0) {
            val curSec = newInventory[recipe.secondaryHerbName] ?: 0
            newInventory[recipe.secondaryHerbName] = (curSec - recipe.secondaryHerbCount).coerceAtLeast(0)
        }
        newInventory[recipe.pillName] = (newInventory[recipe.pillName] ?: 0) + 1

        val flameBonus = state.activeFlame?.bonusCraftingRate ?: 10
        val repGain = recipe.tier * 6

        _uiState.update { current ->
            current.copy(
                spiritStones = current.spiritStones - recipe.requiredSpiritStones,
                inventory = newInventory,
                sectReputation = current.sectReputation + repGain,
                notificationMessage = "Uwarzono [${recipe.tierName}]: ${recipe.pillName}! Płomień: ${state.activeFlame?.name ?: "Zwykły"} (+${flameBonus}% Sukcesu)"
            )
        }
    }

    fun forgeArtifact(recipe: ForgingRecipe) {
        val state = _uiState.value
        val currentMineralCount = state.inventory[recipe.requiredMineralName] ?: 0

        if (currentMineralCount < recipe.requiredMineralCount) {
            showNotification("Brak minerału: ${recipe.requiredMineralName} (${recipe.requiredMineralCount} wymagane)!")
            return
        }

        if (state.spiritStones < recipe.requiredSpiritStones) {
            showNotification("Brak Kamieni Ducha (${recipe.requiredSpiritStones} wymagane)!")
            return
        }

        val newInventory = state.inventory.toMutableMap()
        newInventory[recipe.requiredMineralName] = currentMineralCount - recipe.requiredMineralCount

        _uiState.update { current ->
            current.copy(
                spiritStones = current.spiritStones - recipe.requiredSpiritStones,
                inventory = newInventory,
                vaultArtifacts = current.vaultArtifacts + recipe.artifactResult,
                sectReputation = current.sectReputation + 15,
                notificationMessage = "Wykuto boski artefakt: ${recipe.artifactResult.name} w Boskiej Kuźni!"
            )
        }
    }

    fun equipArtifact(discipleId: String, artifact: Artifact) {
        _uiState.update { current ->
            val updated = current.disciples.map { d ->
                if (d.id == discipleId) {
                    val prevAtk = d.equippedArtifact?.attackBonus ?: 0
                    val prevDef = d.equippedArtifact?.defenseBonus ?: 0
                    d.copy(
                        equippedArtifact = artifact,
                        attack = d.attack - prevAtk + artifact.attackBonus,
                        defense = d.defense - prevDef + artifact.defenseBonus
                    )
                } else d
            }
            current.copy(
                disciples = updated,
                notificationMessage = "Wyposażono artefakt: ${artifact.name}!"
            )
        }
    }

    fun selectGeocentricFlame(flame: GeocentricFlame) {
        _uiState.update { current ->
            current.copy(
                activeFlame = flame,
                notificationMessage = "Wybrano Płomień Geocentryczny: ${flame.name}!"
            )
        }
    }

    fun upgradeFacility(type: FacilityType) {
        val state = _uiState.value
        val facility = state.facilities.find { it.type == type } ?: return

        if (state.spiritStones < facility.upgradeCostStones) {
            showNotification("Zbyt mało Kamieni Ducha na rozbudowę (${facility.upgradeCostStones} wymagane)!")
            return
        }

        val updatedFacilities = state.facilities.map {
            if (it.type == type) {
                it.copy(
                    level = it.level + 1,
                    upgradeCostStones = (it.upgradeCostStones * 1.8).toInt(),
                    effectDescription = "${it.effectDescription} (Poziom ${it.level + 1})"
                )
            } else it
        }

        _uiState.update { current ->
            current.copy(
                spiritStones = current.spiritStones - facility.upgradeCostStones,
                facilities = updatedFacilities,
                sectReputation = current.sectReputation + 20,
                notificationMessage = "Ukończono rozbudowę obiektu: ${facility.type.polishName} (Poz. ${facility.level + 1})!"
            )
        }
    }

    fun startExpedition(regionId: String, discipleId: String) {
        val state = _uiState.value
        val region = state.regions.find { it.id == regionId } ?: return
        val disciple = state.disciples.find { it.id == discipleId } ?: return
        val beast = region.bossBeast ?: return

        // Check environmental hazard resistance
        var hazardPenalty = 0
        if (region.hazard == EnvironmentalHazard.TOXIC_MIASMA && disciple.constitution != ConstitutionType.MYRIAD_POISON_IMMUNE) {
            hazardPenalty += 15
        } else if (region.hazard == EnvironmentalHazard.EXTREME_FROST && disciple.constitution != ConstitutionType.PURE_YANG) {
            hazardPenalty += 15
        } else if (region.hazard == EnvironmentalHazard.SCORCHING_MAGMA && disciple.constitution != ConstitutionType.CELESTIAL_FROST) {
            hazardPenalty += 15
        }

        val flameBonus = state.activeFlame?.combatDamageBonus ?: 0
        val effectiveDiscipleAttack = (disciple.attack + flameBonus - hazardPenalty).coerceAtLeast(10)

        // Initialize combat
        _uiState.update { current ->
            current.copy(
                combatState = CombatState(
                    active = true,
                    discipleName = disciple.name,
                    discipleSchool = disciple.martialSchool,
                    discipleHp = disciple.currentHp,
                    discipleMaxHp = disciple.maxHp,
                    enemyName = "${beast.title} - ${beast.name}",
                    enemyHp = beast.hp,
                    enemyMaxHp = beast.maxHp,
                    logs = listOf(
                        CombatLogEntry(1, "Wkroczono do regionu: ${region.name} (${region.biome.emoji})"),
                        if (hazardPenalty > 0) CombatLogEntry(1, "⚠️ Środowisko (${region.hazard.polishName}) osłabia adepta o $hazardPenalty pkt!") else CombatLogEntry(1, "Konstytucja adepta harmonizuje z otoczeniem!"),
                        CombatLogEntry(1, "Rozpoczęto starcie z mitycznym stworem: ${beast.name}!")
                    )
                )
            )
        }

        runCombatSequence(region, disciple, effectiveDiscipleAttack, beast)
    }

    private fun runCombatSequence(
        region: WorldRegion,
        disciple: Disciple,
        effectiveAttack: Int,
        beast: MythicalBeast
    ) {
        combatJob?.cancel()
        combatJob = viewModelScope.launch {
            var round = 1
            var dHp = disciple.currentHp
            var eHp = beast.hp
            var dQi = 100

            while (dHp > 0 && eHp > 0 && round <= 20) {
                delay(750)
                round++

                // Disciple attacks
                val isCrit = Random.nextInt(100) < 28
                val critMultiplier = if (isCrit) 1.9 else 1.0
                val rawDamage = ((effectiveAttack - beast.defense * 0.4) * critMultiplier).toInt().coerceAtLeast(20)
                eHp = (eHp - rawDamage).coerceAtLeast(0)
                dQi = (dQi + 15).coerceAtMost(100)

                if (isCrit) {
                    WuxiaAudioEngine.playThunderStrike()
                } else {
                    WuxiaAudioEngine.playSwordClash()
                }

                val attackText = if (isCrit) {
                    "💥 ${disciple.name} wyprowadza krytyczne cięcie techniki: $rawDamage obrażeń!"
                } else {
                    "⚔️ ${disciple.name} atakuje witalne punkty ${beast.name}: $rawDamage obrażeń."
                }

                val dmgText = if (isCrit) "-$rawDamage CRIT!" else "-$rawDamage"
                val newDamage = FloatingDamage(
                    id = System.currentTimeMillis() + round,
                    text = dmgText,
                    isCrit = isCrit,
                    isDisciple = false
                )

                _uiState.update { state ->
                    val logs = state.combatState.logs + CombatLogEntry(round, attackText, isCrit = isCrit)
                    state.copy(
                        combatState = state.combatState.copy(
                            enemyHp = eHp,
                            discipleQi = dQi,
                            logs = logs,
                            currentEffect = if (isCrit) "CRIT" else "SLASH",
                            effectCounter = state.combatState.effectCounter + 1,
                            floatingDamages = (state.combatState.floatingDamages + newDamage).takeLast(6)
                        )
                    )
                }

                if (eHp <= 0) break

                delay(650)

                // Beast retaliates
                val beastDamage = (beast.attack - disciple.defense * 0.35).toInt().coerceAtLeast(12)
                dHp = (dHp - beastDamage).coerceAtLeast(0)

                val enemyText = "🔥 ${beast.name} wyprowadza kontratak: $beastDamage obrażeń adepta!"
                val beastDmgItem = FloatingDamage(
                    id = System.currentTimeMillis() + round * 100L,
                    text = "-$beastDamage",
                    isCrit = false,
                    isDisciple = true
                )

                _uiState.update { state ->
                    val logs = state.combatState.logs + CombatLogEntry(round, enemyText)
                    state.copy(
                        combatState = state.combatState.copy(
                            discipleHp = dHp,
                            logs = logs,
                            currentEffect = "BEAST_ATTACK",
                            effectCounter = state.combatState.effectCounter + 1,
                            floatingDamages = (state.combatState.floatingDamages + beastDmgItem).takeLast(6)
                        )
                    )
                }
            }

            val isVictory = eHp <= 0
            if (isVictory) {
                WuxiaAudioEngine.playDivineGong()
            }
            val rewardMsg = if (isVictory) {
                "🏆 Zwycięstwo! Pokonano ${beast.name}! Zdobyto: ${beast.dropItemName} oraz +${beast.spiritStoneReward} Kamieni Ducha!"
            } else {
                "☠️ Porażka! Adept musiał salwować się ucieczką z pola bitwy."
            }

            _uiState.update { state ->
                val newInv = state.inventory.toMutableMap()
                if (isVictory) {
                    newInv[region.mineralResource] = (newInv[region.mineralResource] ?: 0) + 3
                    newInv[region.herbResource] = (newInv[region.herbResource] ?: 0) + 2
                    newInv[beast.dropItemName] = (newInv[beast.dropItemName] ?: 0) + 1
                }

                val updatedRegions = state.regions.map {
                    if (it.id == region.id) it.copy(explorationProgress = (it.explorationProgress + 25).coerceAtMost(100)) else it
                }

                val updatedDisciples = state.disciples.map {
                    if (it.id == disciple.id) it.copy(currentHp = dHp.coerceAtLeast(10)) else it
                }

                state.copy(
                    disciples = updatedDisciples,
                    regions = updatedRegions,
                    inventory = newInv,
                    spiritStones = state.spiritStones + if (isVictory) beast.spiritStoneReward else 0,
                    sectReputation = state.sectReputation + if (isVictory) 35 else -5,
                    combatState = state.combatState.copy(
                        isVictory = isVictory,
                        rewardText = rewardMsg,
                        logs = state.combatState.logs + CombatLogEntry(round + 1, rewardMsg)
                    )
                )
            }
        }
    }

    fun combatManualAttack() {
        val state = _uiState.value
        if (!state.combatState.active || state.combatState.isVictory != null) return

        WuxiaAudioEngine.playSwordClash()
        val dmg = Random.nextInt(35, 65)
        val newHp = (state.combatState.enemyHp - dmg).coerceAtLeast(0)
        val isVict = if (newHp == 0) true else null

        val floatDmg = FloatingDamage(
            id = System.currentTimeMillis(),
            text = "-$dmg",
            isCrit = false,
            isDisciple = false
        )

        if (isVict == true) WuxiaAudioEngine.playDivineGong()

        _uiState.update { current ->
            current.copy(
                combatState = current.combatState.copy(
                    enemyHp = newHp,
                    isVictory = isVict ?: current.combatState.isVictory,
                    currentEffect = "SLASH",
                    effectCounter = current.combatState.effectCounter + 1,
                    floatingDamages = (current.combatState.floatingDamages + floatDmg).takeLast(6),
                    logs = current.combatState.logs + CombatLogEntry(
                        round = current.combatState.logs.size + 1,
                        text = "⚡ Ręczny cios miecza: zadano $dmg obrażeń!"
                    )
                )
            )
        }
    }

    fun combatUsePill() {
        val state = _uiState.value
        if (!state.combatState.active || state.combatState.isVictory != null) return

        val heal = 85
        val newHp = (state.combatState.discipleHp + heal).coerceAtMost(state.combatState.discipleMaxHp)

        val floatHeal = FloatingDamage(
            id = System.currentTimeMillis(),
            text = "+$heal HP",
            isCrit = false,
            isHeal = true,
            isDisciple = true
        )

        _uiState.update { current ->
            current.copy(
                combatState = current.combatState.copy(
                    discipleHp = newHp,
                    currentEffect = "HEAL",
                    effectCounter = current.combatState.effectCounter + 1,
                    floatingDamages = (current.combatState.floatingDamages + floatHeal).takeLast(6),
                    logs = current.combatState.logs + CombatLogEntry(
                        round = current.combatState.logs.size + 1,
                        text = "💊 Użyto esencji leczącej: przywrócono $heal HP!",
                        isHeal = true
                    )
                )
            )
        }
    }

    fun combatUltimate() {
        val state = _uiState.value
        if (!state.combatState.active || state.combatState.isVictory != null) return
        if (state.combatState.discipleQi < 30) {
            showNotification("Zbyt mało Qi na technikę ostateczną!")
            return
        }

        WuxiaAudioEngine.playUltimateBurst()
        val dmg = Random.nextInt(120, 190)
        val newHp = (state.combatState.enemyHp - dmg).coerceAtLeast(0)
        val isVict = if (newHp == 0) true else null
        val newQi = (state.combatState.discipleQi - 30).coerceAtLeast(0)

        val floatDmg = FloatingDamage(
            id = System.currentTimeMillis(),
            text = "-$dmg BOSKIE DAO!",
            isCrit = true,
            isDisciple = false
        )

        if (isVict == true) WuxiaAudioEngine.playDivineGong()

        _uiState.update { current ->
            current.copy(
                combatState = current.combatState.copy(
                    enemyHp = newHp,
                    discipleQi = newQi,
                    isVictory = isVict ?: current.combatState.isVictory,
                    currentEffect = "ULTIMATE",
                    effectCounter = current.combatState.effectCounter + 1,
                    floatingDamages = (current.combatState.floatingDamages + floatDmg).takeLast(6),
                    logs = current.combatState.logs + CombatLogEntry(
                        round = current.combatState.logs.size + 1,
                        text = "💥 UWOLNIONO BOSKĄ TECHNIKĘ DAO: Potężne $dmg obrażeń!",
                        isCrit = true
                    )
                )
            )
        }
    }

    fun closeCombat() {
        combatJob?.cancel()
        _uiState.update { it.copy(combatState = CombatState(active = false)) }
    }

    fun interactRivalSect(sectId: String, actionType: String) {
        val state = _uiState.value
        val sect = state.rivalSects.find { it.id == sectId } ?: return

        when (actionType) {
            "GIFT" -> {
                val cost = 100
                if (state.spiritStones < cost) {
                    showNotification("Brak Kamieni Ducha na poselstwo darów ($cost wymagane)!")
                    return
                }
                val newRel = (sect.relations + 20).coerceAtMost(100)
                val newStatus = if (newRel >= 60) DiplomaticStatus.ALLIANCE else if (newRel >= 20) DiplomaticStatus.FRIENDLY else DiplomaticStatus.NEUTRAL
                _uiState.update { current ->
                    current.copy(
                        spiritStones = current.spiritStones - cost,
                        rivalSects = current.rivalSects.map { if (it.id == sectId) it.copy(relations = newRel, status = newStatus) else it },
                        notificationMessage = "Wysłano dary do sekty ${sect.name}. Relacje wzrosły do $newRel!"
                    )
                }
            }
            "TRADE" -> {
                val trade = sect.activeTrade
                if (trade == null) {
                    showNotification("Sekta nie oferuje obecnie wolnych karawan handlowych.")
                    return
                }
                if (state.spiritStones < trade.costStones) {
                    showNotification("Brak ${trade.costStones} Kamieni Ducha na zakup towarów karawany!")
                    return
                }
                val newInv = state.inventory.toMutableMap()
                newInv[trade.rewardItem] = (newInv[trade.rewardItem] ?: 0) + 3
                _uiState.update { current ->
                    current.copy(
                        spiritStones = current.spiritStones - trade.costStones,
                        inventory = newInv,
                        notificationMessage = "Pomyślnie zrealizowano handel z ${sect.name}: odebrano 3x ${trade.rewardItem}!"
                    )
                }
            }
            "ALLIANCE" -> {
                if (sect.relations < 40) {
                    showNotification("Relacje muszą wynosić co najmniej 40 punktów, aby zaproponować sojusz!")
                    return
                }
                _uiState.update { current ->
                    current.copy(
                        rivalSects = current.rivalSects.map { if (it.id == sectId) it.copy(status = DiplomaticStatus.ALLIANCE) else it },
                        sectReputation = current.sectReputation + 30,
                        notificationMessage = "Podpisano Wieczysty Sojusz z ${sect.name}!"
                    )
                }
            }
            "DECLARE_WAR" -> {
                _uiState.update { current ->
                    current.copy(
                        rivalSects = current.rivalSects.map { if (it.id == sectId) it.copy(status = DiplomaticStatus.TOTAL_WAR, relations = -90) else it },
                        notificationMessage = "Wypowiedziano Wojnę Totalną sekcie ${sect.name}! Ich wojownicy będą atakować twoje kopalnie!"
                    )
                }
            }
        }
    }

    fun handleRandomEventChoice(choiceIndex: Int) {
        val event = _uiState.value.activeRandomEvent ?: return
        val choice = event.choices.getOrNull(choiceIndex) ?: return

        val newInventory = _uiState.value.inventory.toMutableMap()
        choice.rewardItem?.let { item ->
            newInventory[item] = (newInventory[item] ?: 0) + 2
        }

        _uiState.update { current ->
            current.copy(
                activeRandomEvent = null,
                spiritStones = (current.spiritStones + choice.stoneChange).coerceAtLeast(0),
                sectReputation = current.sectReputation + choice.repChange,
                inventory = newInventory,
                notificationMessage = choice.outcomeDescription
            )
        }
    }

    fun dismissRandomEvent() {
        _uiState.update { it.copy(activeRandomEvent = null) }
    }

    fun attackWorldBoss(discipleId: String) {
        val disciple = _uiState.value.disciples.find { it.id == discipleId } ?: return
        val damage = (disciple.attack * 12L) + Random.nextLong(2000, 5000)

        _uiState.update { current ->
            val boss = current.worldBoss
            val newHp = (boss.currentHp - damage).coerceAtLeast(0)
            val stonesGained = (damage / 25).toInt()
            current.copy(
                worldBoss = boss.copy(
                    currentHp = newHp,
                    playerContributionDamage = boss.playerContributionDamage + damage
                ),
                spiritStones = current.spiritStones + stonesGained,
                notificationMessage = "⚔️ Twój czempion ${disciple.name} zadał $damage obrażeń Władcy Próżni! Zdobyto $stonesGained Kamieni Ducha!"
            )
        }
    }

    fun fightArenaDuel(discipleId: String, rivalRanking: MultiplayerSectRanking) {
        val disciple = _uiState.value.disciples.find { it.id == discipleId } ?: return
        val playerWin = disciple.attack > 60 || Random.nextBoolean()

        _uiState.update { current ->
            if (playerWin) {
                current.copy(
                    spiritStones = current.spiritStones + 200,
                    sectReputation = current.sectReputation + 35,
                    notificationMessage = "🏆 Zwycięstwo w Niebiańskiej Arenie! ${disciple.name} pokonał czempiona sekty ${rivalRanking.sectName}! (+200 Kamieni, +35 Reputacji)"
                )
            } else {
                current.copy(
                    notificationMessage = "Pojedynek w Arenie zakończony remisem ze wskazaniem na ${rivalRanking.sectName}."
                )
            }
        }
    }

    fun toggleSectDecree(decreeId: String) {
        _uiState.update { state ->
            val updated = state.sectLeader.activeDecrees.map {
                if (it.id == decreeId) it.copy(isActive = !it.isActive) else it
            }
            val decreeToggled = updated.find { it.id == decreeId }
            val statusText = if (decreeToggled?.isActive == true) "Aktywowano" else "Cofnięto"
            state.copy(
                sectLeader = state.sectLeader.copy(activeDecrees = updated),
                notificationMessage = "$statusText edykt: ${decreeToggled?.title ?: decreeId}"
            )
        }
    }

    fun upgradePatriarchPrestige() {
        val cost = _uiState.value.sectLeader.prestigeLevel * 250
        if (_uiState.value.spiritStones >= cost) {
            _uiState.update { state ->
                val newLeader = state.sectLeader.copy(
                    prestigeLevel = state.sectLeader.prestigeLevel + 1,
                    leadershipPoints = state.sectLeader.leadershipPoints + 25,
                    divineAuthority = state.sectLeader.divineAuthority + 60
                )
                state.copy(
                    spiritStones = state.spiritStones - cost,
                    sectReputation = state.sectReputation + 45,
                    sectLeader = newLeader,
                    notificationMessage = "Awansowano Prestiż Patriarchy do poziomu ${newLeader.prestigeLevel}!"
                )
            }
        } else {
            showNotification("Brak Kamieni Ducha na podniesienie Prestiżu Patriarchy! Potrzeba: $cost")
        }
    }

    fun changeSectPolicy(policy: SectPolicy) {
        _uiState.update { current ->
            current.copy(
                currentPolicy = policy,
                notificationMessage = "📜 Zmieniono doktrynę sekty na: ${policy.polishName}! (${policy.buffDescription})"
            )
        }
    }

    fun setActiveBeast(beastId: String) {
        _uiState.update { current ->
            val beast = current.sectBeasts.find { it.id == beastId } ?: return@update current
            current.copy(
                activeBeast = beast,
                notificationMessage = "🐉 Wybrano Świętą Bestię Sekty: ${beast.name} (${beast.title})!"
            )
        }
    }

    fun feedSectBeast(beastId: String, herbName: String) {
        val state = _uiState.value
        val beast = state.sectBeasts.find { it.id == beastId } ?: return
        val herbCount = state.inventory[herbName] ?: 0
        if (herbCount < 1) {
            showNotification("Brak składnika: $herbName w magazynie sekty!")
            return
        }

        val newInventory = state.inventory.toMutableMap()
        newInventory[herbName] = herbCount - 1

        val isFavorite = herbName.equals(beast.favoriteHerb, ignoreCase = true)
        val intimacyGain = if (isFavorite) 15 else 8
        val energyGain = if (isFavorite) 300 else 150

        val updatedBeasts = state.sectBeasts.map { b ->
            if (b.id == beastId) {
                b.copy(
                    intimacy = (b.intimacy + intimacyGain).coerceAtMost(100),
                    currentEnergy = (b.currentEnergy + energyGain).coerceAtMost(b.maxEnergy)
                )
            } else b
        }
        val updatedActiveBeast = updatedBeasts.find { it.id == state.activeBeast?.id }

        _uiState.update { current ->
            current.copy(
                inventory = newInventory,
                sectBeasts = updatedBeasts,
                activeBeast = updatedActiveBeast,
                notificationMessage = "Nakarmino ${beast.name} ziółkiem $herbName! Intymność +$intimacyGain% ${if (isFavorite) "(Ulubiony Przysmak! ✨)" else ""}"
            )
        }
    }

    fun evolveSectBeast(beastId: String) {
        val state = _uiState.value
        val beast = state.sectBeasts.find { it.id == beastId } ?: return
        val costStones = beast.tier * 300
        if (beast.intimacy < 60) {
            showNotification("Wymagana intymność co najmniej 60%, aby obudzić ewolucję Bestii! (Obecnie: ${beast.intimacy}%)")
            return
        }
        if (state.spiritStones < costStones) {
            showNotification("Zbyt mało Kamieni Ducha na rytuał ewolucji Bestii! Wymagane: $costStones")
            return
        }

        val updatedBeasts = state.sectBeasts.map { b ->
            if (b.id == beastId) {
                b.copy(
                    tier = b.tier + 1,
                    maxEnergy = b.maxEnergy + 500,
                    currentEnergy = b.maxEnergy + 500,
                    intimacy = 20,
                    passiveSectBuff = "+${(b.tier + 1) * 20}% Wzmocnienie Potęgi Sekty i Błogosławieństwo Dao"
                )
            } else b
        }
        val updatedActiveBeast = updatedBeasts.find { it.id == state.activeBeast?.id }

        _uiState.update { current ->
            current.copy(
                spiritStones = current.spiritStones - costStones,
                sectBeasts = updatedBeasts,
                activeBeast = updatedActiveBeast,
                sectReputation = current.sectReputation + 50,
                notificationMessage = "✨ Rytuał Ewolucji udany! ${beast.name} osiągnął Rząd ${beast.tier + 1} Boskiego Strażnika!"
            )
        }
    }

    fun refineFlame(flameId: String) {
        val state = _uiState.value
        val flame = state.flames.find { it.id == flameId } ?: return
        val costStones = flame.refinementLevel * 150
        val costCrystals = (flame.refinementLevel / 2) + 1
        val crystalsInInv = state.inventory["Kryształ Geocentrycznego Ognia"] ?: 0

        if (state.spiritStones < costStones) {
            showNotification("Brak Kamieni Ducha na uszlachetnienie płomienia ($costStones wymagane)!")
            return
        }
        if (crystalsInInv < costCrystals) {
            showNotification("Brak Kryształów Geocentrycznego Ognia ($costCrystals wymagane)!")
            return
        }

        val newInventory = state.inventory.toMutableMap()
        newInventory["Kryształ Geocentrycznego Ognia"] = crystalsInInv - costCrystals

        val updatedFlames = state.flames.map { f ->
            if (f.id == flameId) {
                f.copy(
                    refinementLevel = f.refinementLevel + 1,
                    temperatureCelsius = f.temperatureCelsius + 350,
                    bonusCraftingRate = f.bonusCraftingRate + 8,
                    combatDamageBonus = f.combatDamageBonus + 15
                )
            } else f
        }
        val updatedActiveFlame = updatedFlames.find { it.id == state.activeFlame?.id }

        _uiState.update { current ->
            current.copy(
                spiritStones = current.spiritStones - costStones,
                inventory = newInventory,
                flames = updatedFlames,
                activeFlame = updatedActiveFlame,
                notificationMessage = "🔥 Uszlachetniono ${flame.name}! Poziom Uszlachetnienia: ${flame.refinementLevel + 1}, Temperatura: ${flame.temperatureCelsius + 350}°C!"
            )
        }
    }

    fun captureTerritory(territoryId: String, discipleId: String) {
        val state = _uiState.value
        val territory = state.territories.find { it.id == territoryId } ?: return
        val disciple = state.disciples.find { it.id == discipleId } ?: return

        if (territory.controllingSectId == "player") {
            showNotification("To terytorium jest już pod jurysdykcją twojej sekty!")
            return
        }

        val combatPower = disciple.attack * 8 + (disciple.comprehension * 3)
        val success = combatPower >= territory.garrisonDefensePower || Random.nextInt(100) < 60

        if (success) {
            val updatedTerritories = state.territories.map { t ->
                if (t.id == territoryId) {
                    t.copy(
                        controllingSectId = "player",
                        controllingSectName = "Sekta Niebiańskiego Smoka",
                        assignedDiscipleId = disciple.id,
                        garrisonDefensePower = combatPower
                    )
                } else t
            }
            val rewardStones = 200
            _uiState.update { current ->
                current.copy(
                    territories = updatedTerritories,
                    spiritStones = current.spiritStones + rewardStones,
                    sectReputation = current.sectReputation + 40,
                    notificationMessage = "🚩 Sukces ekspansji! Czempion ${disciple.name} podbił węzeł ${territory.name}! Węzeł generuje teraz ${territory.yieldAmountPerCycle}x ${territory.resourceType} co cykl!"
                )
            }
        } else {
            _uiState.update { current ->
                current.copy(
                    notificationMessage = "Garnizon ${territory.controllingSectName} odparł atak na ${territory.name}. Zwiększ moc swoich adeptów!"
                )
            }
        }
    }

    fun assignGarrison(territoryId: String, discipleId: String?) {
        val state = _uiState.value
        val territory = state.territories.find { it.id == territoryId } ?: return
        val disciple = state.disciples.find { it.id == discipleId }

        val updatedTerritories = state.territories.map { t ->
            if (t.id == territoryId) {
                t.copy(
                    assignedDiscipleId = discipleId,
                    garrisonDefensePower = disciple?.let { it.attack * 8 + it.comprehension * 3 } ?: 200
                )
            } else t
        }

        _uiState.update { current ->
            current.copy(
                territories = updatedTerritories,
                notificationMessage = if (disciple != null) "Obsadzono posterunek ${territory.name} adeptem ${disciple.name}!" else "Wycofano garnizon z ${territory.name}."
            )
        }
    }

    private fun showNotification(msg: String) {
        _uiState.update { it.copy(notificationMessage = msg) }
    }

    fun clearNotification() {
        _uiState.update { it.copy(notificationMessage = null) }
    }
}
