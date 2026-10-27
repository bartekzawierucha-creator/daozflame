package com.example.model

/**
 * Etap 6: ukryta głębia. Reguły świata losuje się RAZ przy nowej grze (z ziarna świata) i zapisuje w stanie –
 * nigdy nie generuje ponownie przy wczytaniu, bo zmiana treści przesunęłaby odpowiedzi graczy.
 */
data class WorldRules(
    val version: Int = 1,
    /** Gwiazdozbiory meridianów: punkty siatki 6×6 (0..35). */
    val constellations: List<Constellation> = emptyList(),
    /** Zatory: punkty, których otwarcie kosztuje trzy razy więcej. */
    val blockages: Set<Int> = emptySet(),
    /** Metoda → optymalny oddech, miejsce i pora. */
    val breath: Map<String, BreathOptimum> = emptyMap(),
    /** Metoda → żywioł (decyduje, które miejsca są dla niej zakazane). */
    val methodElements: Map<String, SpiritElement> = emptyMap(),
    /** Obiekt kontemplacji → Prawo i siła rezonansu (×100). */
    val lawObjects: Map<String, LawResonance> = emptyMap(),
    /** Prawa pochodne: para Praw → nazwa. */
    val derivedLaws: List<DerivedLaw> = emptyList(),
    /** Zioło → atrybuty alchemiczne (nazwa → siła 1..5). */
    val herbAttributes: Map<String, Map<String, Int>> = emptyMap(),
    /** Formy walki: sekwencje 3 ruchów gracza. */
    val forms: List<CombatForm> = emptyList(),
    /** Wskazówki do każdej tajemnicy (co najmniej 2 z różnych źródeł). */
    val hints: List<Hint> = emptyList(),
    /** v2 (Etap 7): żywioł feng shui każdego z 6 stałych miejsc budynków. */
    val fengShui: List<SpiritElement> = emptyList()
)

data class Constellation(val id: String = "", val name: String = "", val points: List<Int> = emptyList(), val stat: String = "QI_GAIN", val percent: Int = 5)

data class BreathOptimum(val breath: Int = 0, val place: Int = 0, val time: TimeOfDay = TimeOfDay.DAWN)

data class LawResonance(val law: String = "", val affinity: Int = 100)

data class DerivedLaw(val first: String = "", val second: String = "", val name: String = "")

data class CombatForm(val id: String = "", val name: String = "", val sequence: String = "", val bonusPercent: Int = 30)

enum class HintSource(val polishName: String) {
    DREAM("Sen"), SCROLL("Zwój"), NPC("Opowieść"), DESCRIPTION("Opis"), RESEARCH("Badania")
}

data class Hint(val secretId: String = "", val source: HintSource = HintSource.DREAM, val text: String = "")

/** Poziom wiedzy w Dzienniku. Liczby pokazuje się dopiero przy [CERTAIN]. */
enum class KnowledgeLevel(val polishName: String) {
    GUESS("Przypuszczenie"), LIKELY("Prawdopodobne"), CERTAIN("Pewne")
}

data class Knowledge(
    val level: KnowledgeLevel = KnowledgeLevel.GUESS,
    val observations: Int = 0,
    val notes: List<String> = emptyList()
)

/** Ukryte stany kultywacji – każdy ma koniec, żaden nie blokuje ucznia na zawsze. */
enum class SecretState(val polishName: String, val description: String) {
    HALF_STEP("Półkrok", "Zawieszony między królestwami: +10% obrażeń, Qi ×0,8. Mija po przełomie albo po 30 dniach."),
    FALSE_IMMORTAL("Fałszywy Nieśmiertelny", "Królestwo zbudowane na pigułkach: Qi ×1,2, ale przełomy trudniejsze. Mija po oczyszczeniu toksyn (toksyczność 0) albo po 60 dniach."),
    CLEAR_HEART("Czyste Serce", "Umysł bez skazy: olśnienia ×2 i odporność na demona serca. Mija, gdy karma spadnie poniżej 30, albo po 45 dniach.")
}
