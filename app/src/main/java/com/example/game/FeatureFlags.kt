package com.example.game

/**
 * Flagi funkcji. Każdy nowy system trafia do `main` wyłączony i włączamy go dopiero,
 * gdy spełnia definicję ukończenia (testy, zapis, symulator, test na telefonie).
 */
object FeatureFlags {
    /** Panel narzędzi deweloperskich – tylko w buildzie debug. */
    val debugPanel: Boolean get() = com.example.BuildConfig.DEBUG

    // Etap 1
    const val OFFLINE_PROGRESS = false
    const val EDICTS_V2 = false
    const val TUTORIAL = false
}
