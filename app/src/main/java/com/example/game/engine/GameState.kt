package com.example.game.engine

import com.example.model.*

/**
 * Pełny stan gry – to, co trafia do zapisu. Bez pól interfejsu (toasty, zaznaczenia, prędkość).
 *
 * ZASADA: każde pole MUSI mieć wartość domyślną, inaczej stary zapis się nie wczyta.
 * Treść statyczna (techniki, receptury, zdarzenia) nie jest tu kopiowana – trzymamy tylko id.
 */
data class GameState(
    /** Ziarno świata – losowane raz przy nowej grze. */
    val worldSeed: Long = 0L,
    /** Licznik kroków silnika; razem z ziarnem wyznacza strumienie losowości. */
    val step: Long = 0L,
    /** Licznik ticków pętli gry (co 16 ticków szansa na zdarzenie). */
    val tick: Long = 0L,

    val sectName: String = "Sekta Niebiańskiego Smoka",
    val sectMasterName: String = "Mistrz Chen Tian",
    val spiritStones: Int = 850,
    val sectReputation: Int = 180,
    val sectContribution: Int = 450,
    val currentDay: Int = 1,
    val timeOfDay: TimeOfDay = TimeOfDay.DAY,
    /** Cykle, które minęły w bieżącej porze dnia (0 … TimePhase.TICKS_PER_PHASE − 1). */
    val phaseTick: Int = 0,

    val disciples: List<Disciple> = emptyList(),
    val availableRecruits: List<Disciple> = emptyList(),
    val inventory: Map<String, Int> = emptyMap(),
    val facilities: List<SectFacility> = emptyList(),
    val regions: List<WorldRegion> = emptyList(),
    val rivalSects: List<RivalSect> = emptyList(),
    val territories: List<TerritoryNode> = emptyList(),

    val flames: List<GeocentricFlame> = emptyList(),
    val activeFlameId: String? = null,
    val vaultArtifacts: List<Artifact> = emptyList(),

    val activeEventId: String? = null,
    /** Ostatnio wylosowane zdarzenia – nie powtarzają się, dopóki nie minie 12 innych. */
    val recentEventIds: List<String> = emptyList(),

    val worldBoss: WorldBossRaid = com.example.game.WuxiaGenerator.getInitialWorldBoss(),
    val sectLeader: SectLeaderData = com.example.game.WuxiaGenerator.getInitialSectLeader(),
    val currentPolicy: SectPolicy = SectPolicy.RIGHTEOUS_DAO,
    val sectBeasts: List<SectDivineBeast> = emptyList(),
    val activeBeastId: String? = null,

    /** Edykt → dzień, od którego wolno go znów przełączyć. */
    val edictCooldowns: Map<String, Int> = emptyMap(),

    val autoBreakthroughEnabled: Boolean = true,
    val autoUsePillsForBreakthrough: Boolean = true,

    val combat: ActiveCombat? = null,

    // --- Etap 2: życie sekty ---
    /** false = zapis sprzed Etapu 2; silnik uzupełnia korzenie, cechy i rangi przy wczytaniu. */
    val e2Initialized: Boolean = false,
    /** Morale sekty 0–100. */
    val morale: Int = 60,
    /** Dzień, w którym zabrakło jedzenia (kara do Qi tego dnia). */
    val hungryOnDay: Int = -1,
    /** Egzamin wstępny trwa do tego dnia włącznie (0 = brak egzaminu). */
    val examOpenUntilDay: Int = 0,
    /** Ilu kandydatów przyjęto na bieżącym egzaminie (pierwsi dwaj za darmo). */
    val admittedInExam: Int = 0,
    val nextFeastDay: Int = 0,
    val lastTournamentDay: Int = 0,

    // --- Etap 3: kultywacja ---
    /** false = zapis sprzed Etapu 3; silnik przelicza królestwa na etapy przy wczytaniu. */
    val e3Initialized: Boolean = false,
    /** Karma sekty: −100 (demoniczna) … +100 (prawa). */
    val karma: Int = 0,
    /** Uczniowie, którzy wstąpili do Wyższego Świata (każdy daje stałą premię). */
    val ascendedNames: List<String> = emptyList(),

    // --- Etap 4: gospodarka, rzemiosło, płomienie ---
    val e4Initialized: Boolean = false,
    val veinLevel: Int = 1,
    /** Dzień zakończenia trwającej rozbudowy żyły (0 = brak). */
    val veinUpgradeDoneDay: Int = 0,
    val fields: List<com.example.model.FieldPlot> = emptyList(),
    /** Mnożniki cen targu (przedmiot → mnożnik); brak wpisu = 1,0. */
    val marketMultipliers: Map<String, Double> = emptyMap(),
    val caravans: List<com.example.model.Caravan> = emptyList(),
    val contracts: List<com.example.model.Contract> = emptyList(),
    val contractsRefreshDay: Int = 0,
    /** Znane receptury alchemii (puste = wszystkie, dla starych zapisów przed migracją). */
    val knownRecipeIds: Set<String> = emptySet(),
    /** Płomień w palenisku kuźni (activeFlameId = płomień w kotle alchemii). */
    val forgeFlameId: String? = null,

    /** Krok samouczka; brak pola w starym zapisie = samouczek ukończony. */
    val tutorialStep: Int = Tutorial.DONE,

    /** Znacznik czasu ostatniej aktywności (ms) – do postępu offline (Etap 1). */
    val lastActiveAtMs: Long = 0L
) {
    val activeFlame: GeocentricFlame? get() = flames.find { it.id == activeFlameId }
    val activeBeast: SectDivineBeast? get() = sectBeasts.find { it.id == activeBeastId }
    fun disciple(id: String?): Disciple? = disciples.find { it.id == id }
}

/** Walka w toku. Silnik rozstrzyga ją krok po kroku ([GameCommand.CombatStep]). */
data class ActiveCombat(
    val regionId: String = "",
    val discipleId: String = "",
    val discipleName: String = "",
    val discipleSchool: MartialSchool = MartialSchool.SWORD_IMMORTAL,
    val effectiveAttack: Int = 0,
    val enemy: MythicalBeast? = null,
    val discipleHp: Int = 0,
    val discipleMaxHp: Int = 1,
    val discipleQi: Int = 100,
    val enemyHp: Int = 0,
    val enemyMaxHp: Int = 1,
    val round: Int = 1,
    /** Kto wykonuje następny ruch w [GameCommand.CombatStep]. */
    val discipleTurn: Boolean = true,
    /** Talizman Pieczęci – przeciwnik traci następny ruch. */
    val enemySealed: Boolean = false,
    val logs: List<CombatLogEntry> = emptyList(),
    /** null = trwa, true = zwycięstwo, false = porażka. */
    val outcome: Boolean? = null,
    val rewardText: String = ""
) {
    val finished: Boolean get() = outcome != null

    companion object {
        const val MAX_ROUNDS = 20
    }
}
