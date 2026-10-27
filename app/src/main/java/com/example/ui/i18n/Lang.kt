package com.example.ui.i18n

import java.util.Locale

/**
 * Etap 9: lokalizacja. Rama interfejsu (moduły, nawigacja, nazwa gry) jest tłumaczona; treść gry (katalogi
 * zdarzeń, fabuła, tajemnice) jest po polsku – plan i stan prac w docs/localization.md, terminy w docs/glossary.md.
 * Nazwy własne (Dao, Qi) zostają bez tłumaczenia.
 */
object Lang {
    val english: Boolean get() = Locale.getDefault().language == "en"

    /** Tekst w języku urządzenia (polski domyślnie). */
    fun t(pl: String, en: String): String = if (english) en else pl
}
