package com.example.model

/**
 * Etap 8: postęp ponad erami. Wszystko, co przetrwa odrodzenie sekty, mieszka TUTAJ – reszta stanu
 * zeruje się do nowej gry. Dzięki temu lista „zostaje / zeruje się” jest jawna i testowalna.
 */
data class MetaProgress(
    /** Numer ery (1 = pierwsza gra). */
    val era: Int = 1,
    /** Niewydana Esencja Niebios. */
    val essence: Int = 0,
    /** Drzewo Niebios: id węzła → poziom. */
    val heavenTree: Map<String, Int> = emptyMap(),
    /** Księga Dziedzictwa: uniwersalne tajemnice (prawa świata), które przechodzą do nowej ery jako Pewne. */
    val legacy: Set<String> = emptySet(),
    /** Kodeks: kategoria Dziennika → najwięcej odkryć w jednej erze (licznik bez listy brakujących). */
    val codex: Map<String, Int> = emptyMap(),
    /** Modyfikator bieżącej ery. */
    val eraModifier: String? = null,
    /** Dziedzic: uczeń przeniesiony z poprzedniej ery jako patriarcha-następca. */
    val heirName: String? = null,
    val achievements: Set<String> = emptySet(),
    val title: String? = null,
    /** Liczniki do osiągnięć (zwycięstwa, przełomy…), aktualizowane przez zdarzenia silnika. */
    val stats: Map<String, Long> = emptyMap(),
    val difficulty: Difficulty = Difficulty.NORMAL,
    val ironWill: Boolean = false,
    // Cele dzienne (czas realny – jedyne miejsce, z zabezpieczeniem przed przestawianiem zegara)
    val lastRealMs: Long = 0L,
    val goalsDay: Long = -1L,
    val goals: List<DailyGoal> = emptyList(),
    /** Postęp cyklu 7 dni: wstrzymuje się przy pominiętym dniu, nie zeruje. */
    val streak: Int = 0,
    val offeringDay: Long = -1L
)

enum class Difficulty(val polishName: String, val incomePercent: Double, val raidPercent: Double) {
    EASY("Łagodna droga", 25.0, -25.0),
    NORMAL("Droga kultywatora", 0.0, 0.0),
    HARD("Droga przez ogień", -20.0, 30.0)
}

data class DailyGoal(val kind: String = "", val target: Int = 1, val progress: Int = 0) {
    val done: Boolean get() = progress >= target
}
