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

    val steps: List<Step> = listOf(
        Step(
            "Duch Założyciela",
            "Witaj, Patriarcho. Ta góra była niegdyś moją sektą. Pokażę ci, jak ją odbudować."
        ),
        Step(
            "Uczniowie",
            "Twoi uczniowie medytują i gromadzą Qi. Otwórz Salę Adeptów, by poznać ich talenty i zadania."
        ),
        Step(
            "Przełom",
            "Gdy pasek Qi ucznia dojdzie do 100%, może przejść przełom. Spróbuj przełomu w Sali Adeptów.",
            GameCommand.AttemptBreakthrough::class
        ),
        Step(
            "Alchemia",
            "Pigułki zwiększają szansę przełomu. W Sali Alchemii uwarz dowolną pigułkę.",
            GameCommand.BrewPill::class
        ),
        Step(
            "Edykty",
            "Edykty to prawa sekty: premia za koszt. W Pałacu aktywuj albo zmień edykt.",
            GameCommand.ToggleDecree::class
        ),
        Step(
            "Świat",
            "Krainy kryją zioła, rudy i bossów. Wyślij ucznia na wyprawę z mapy Świata.",
            GameCommand.StartExpedition::class
        ),
        Step(
            "Kuźnia",
            "Łupy z bossów i rudy przetopisz w Kuźni na artefakty. Wykuj pierwszy artefakt.",
            GameCommand.ForgeArtifact::class
        ),
        Step(
            "Rekrutacja",
            "Sekta rośnie dzięki nowym uczniom. Przyjmij kandydata w Pałacu.",
            GameCommand.Recruit::class
        ),
        Step(
            "Omen",
            "Świat przynosi zdarzenia. Gdy pojawi się Omen, Twój wybór ma konsekwencje."
        ),
        Step(
            "Droga przed tobą",
            "Reszta należy do ciebie. Wiele tajemnic tej góry odkryjesz tylko sam. Powodzenia, Patriarcho."
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
