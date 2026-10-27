package com.example.game.save

import com.example.game.engine.GameState
import java.io.File

/**
 * Plikowy magazyn zapisu. Zapis jest atomowy: najpierw `save.tmp`, potem zamiana nazwy.
 * Poprzedni dobry zapis zostaje jako `save.bak` i jest wczytywany, gdy główny jest uszkodzony.
 */
class SaveStore(private val dir: File) {

    val mainFile: File get() = File(dir, "save.json")
    val backupFile: File get() = File(dir, "save.bak")
    private val tmpFile: File get() = File(dir, "save.tmp")

    data class LoadResult(val state: GameState?, val fromBackup: Boolean, val error: String?)

    @Synchronized
    fun save(state: GameState, nowMs: Long) {
        dir.mkdirs()
        tmpFile.writeText(SaveCodec.encode(state, nowMs), Charsets.UTF_8)
        if (mainFile.exists()) {
            backupFile.delete()
            if (!mainFile.renameTo(backupFile)) mainFile.copyTo(backupFile, overwrite = true)
        }
        if (!tmpFile.renameTo(mainFile)) {
            tmpFile.copyTo(mainFile, overwrite = true)
            tmpFile.delete()
        }
    }

    @Synchronized
    fun load(): LoadResult {
        var firstError: String? = null
        for ((file, isBackup) in listOf(mainFile to false, backupFile to true)) {
            if (!file.exists()) continue
            try {
                return LoadResult(SaveCodec.decode(file.readText(Charsets.UTF_8)), isBackup, firstError)
            } catch (e: Exception) {
                if (firstError == null) firstError = e.message
            }
        }
        return LoadResult(null, false, firstError)
    }

    fun exportText(): String? = mainFile.takeIf { it.exists() }?.readText(Charsets.UTF_8)

    /** Import zapisu (np. z panelu debug). Zwraca stan albo rzuca wyjątek, gdy tekst jest uszkodzony. */
    fun import(text: String, nowMs: Long): GameState {
        val state = SaveCodec.decode(text)
        save(state, nowMs)
        return state
    }

    fun delete() {
        listOf(mainFile, backupFile, tmpFile).forEach { it.delete() }
    }
}
