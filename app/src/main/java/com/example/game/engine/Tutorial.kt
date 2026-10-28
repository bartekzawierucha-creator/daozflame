package com.example.game.engine

import kotlin.reflect.KClass

/**
 * Samouczek prowadzony przez Ducha Założyciela. Krok kończy się przyciskiem „Dalej”
 * albo samodzielnie, gdy gracz wykona wskazaną akcję (komenda, która zmieniła stan).
 * Stare zapisy mają samouczek ukończony ([DONE] jako wartość domyślna w stanie).
 */
object Tutorial {

    const val DONE = 100

    data class Step(
        val title: String,
        val text: String,
        /** Komenda, która kończy krok; null = krok informacyjny („Dalej”). */
        val goal: KClass<out GameCommand>? = null
    )

    /** Etap 15: krótki wstęp – resztę prowadzi Droga Sekty (baner z kompasem na hubie). */
    val steps: List<Step> = listOf(
        Step(
            "Duch Założyciela",
            "Witaj, Patriarcho. Ta góra była niegdyś moją sektą. Pokażę ci, jak ją odbudować."
        ),
        Step(
            "Uczniowie żyją sami",
            "Twoi uczniowie sami wybierają zajęcia i decydują o przełomach. Ty doradzasz. Otwórz Pawilon Adeptów i doradź komuś zajęcie.",
            GameCommand.AssignRole::class
        ),
        Step(
            "Droga Sekty",
            "Sekta zaczyna skromnie: budynki otwierają nowe działy. Baner z kompasem na górze ekranu zawsze podpowie następny krok i nagrodę."
        )
    )

    fun current(s: GameState): Step? = steps.getOrNull(s.tutorialStep)

    /** Po każdej komendzie: jeśli spełniła cel bieżącego kroku i zmieniła stan, przechodzimy dalej. */
    fun afterCommand(before: GameState, command: GameCommand, after: GameState): GameState {
        val step = current(after) ?: return after
        val goal = step.goal ?: return after
        if (!goal.isInstance(command)) return after
        if (after.copy(step = before.step) == before) return after // komenda się nie powiodła
        return after.copy(tutorialStep = next(after.tutorialStep))
    }

    fun next(step: Int): Int = if (step + 1 >= steps.size) DONE else step + 1
}
