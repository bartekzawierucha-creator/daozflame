package com.example.game

/**
 * Flagi funkcji (czysty Kotlin – czyta je też silnik). Każdy nowy system trafia do `main`
 * wyłączony i włączamy go dopiero, gdy spełnia definicję ukończenia
 * (testy, zapis, symulator, test na telefonie).
 */
object FeatureFlags {
    // Etap 1
    const val EDICTS_V2 = true
    const val OFFLINE_PROGRESS = true
    const val TUTORIAL = true

    // Etap 2
    const val DISCIPLES_V2 = true
}
