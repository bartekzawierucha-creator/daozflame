package com.example.model

/**
 * Etap 5, krok 1: silnik skryptów zdarzeń.
 * Zamknięty zestaw warunków i efektów (żadnego dowolnego kodu w treści) – kształt danych gotowy do zapisu w JSON.
 */
enum class CondType {
    MIN_DAY, MAX_DAY, TIME_OF_DAY, SEASON,
    FLAG_AT_LEAST, FLAG_BELOW,
    MIN_REPUTATION, MIN_KARMA, MAX_KARMA, MIN_STONES,
    HAS_ITEM, REGION_EXPLORED
}

data class Condition(val type: CondType = CondType.MIN_DAY, val key: String = "", val value: Int = 0)

enum class EffectType {
    STONES, REPUTATION, KARMA, MORALE, ITEM,
    SET_FLAG, ADD_FLAG,
    /** Zaplanuj zdarzenie [key] za [value] dni. */
    SCHEDULE,
    /** Zmień relacje z sektą rywala [key] o [value]. */
    RIVAL_RELATION
}

data class Effect(val type: EffectType = EffectType.STONES, val key: String = "", val value: Int = 0)

data class ScriptChoice(
    val text: String = "",
    val outcome: String = "",
    /** Wybór dostępny tylko, gdy warunki są spełnione. */
    val conditions: List<Condition> = emptyList(),
    val effects: List<Effect> = emptyList()
)

data class ScriptEvent(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val conditions: List<Condition> = emptyList(),
    val choices: List<ScriptChoice> = emptyList(),
    /** true = może wypaść losowo; false = tylko z zaplanowania (ogniwo łańcucha). */
    val random: Boolean = true,
    /** Zdarzenie wypada najwyżej raz w świecie. */
    val once: Boolean = false,
    /** Nazwa łańcucha (np. „npc_hermit”), do podglądu i walidacji. */
    val chain: String? = null
)

/** Zdarzenie zaplanowane na konkretny dzień. */
data class ScheduledEvent(val day: Int = 0, val eventId: String = "")
