package com.example.game

import java.io.File

/**
 * Etap 9: lokalny rejestr awarii (do czasu podłączenia Firebase Crashlytics – wymaga google-services.json).
 * Ostatnia awaria trafia do pliku; po ponownym uruchomieniu gra o niej informuje, a panel debug pokazuje treść.
 */
object CrashLog {
    private const val FILE = "crash.txt"

    fun install(dir: File) {
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, e ->
            runCatching {
                File(dir, FILE).writeText("czas: ${System.currentTimeMillis()}\nwątek: ${thread.name}\n${e.stackTraceToString()}")
            }
            previous?.uncaughtException(thread, e)
        }
    }

    /** Treść ostatniej awarii (jednorazowo – plik jest usuwany po odczycie). */
    fun consume(dir: File): String? {
        val f = File(dir, FILE)
        if (!f.exists()) return null
        return runCatching { f.readText() }.getOrNull().also { f.delete() }
    }
}
