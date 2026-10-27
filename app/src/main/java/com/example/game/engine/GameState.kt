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
