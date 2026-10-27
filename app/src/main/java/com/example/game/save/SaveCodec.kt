package com.example.game.save

import com.example.game.engine.GameState
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import java.util.zip.CRC32

/**
 * Zamiana stanu gry na tekst i z powrotem.
 *
 * Format pliku: koperta JSON `{version, savedAtMs, crc, payload}`, gdzie `payload` to stan gry
 * jako tekst JSON, a `crc` to suma kontrolna payloadu. Uszkodzony plik = wyjątek, a magazyn
 * wczytuje kopię zapasową.
 *
 * ZASADY ZMIAN:
 * - nowe pole w stanie = wartość domyślna (Moshi uzupełni brakujące pola z domyślnych),
 * - zmiana nazwy/kształtu pola albo stałej enuma = podbicie [SAVE_VERSION] + migracja w [migrations]
 *   + złoty zapis poprzedniej wersji w testach.
 */
object SaveCodec {

    const val SAVE_VERSION = 1

    /**
     * Migracje: klucz = wersja ŹRÓDŁOWA. Każda dostaje surowy stan (mapa JSON) i podnosi go o jedną wersję.
     * Przykład na przyszłość:  `1 to { s -> s["nowePole"] = s.remove("starePole") }`
     */
    val migrations: Map<Int, (MutableMap<String, Any?>) -> Unit> = emptyMap()

    data class SaveEnvelope(
        val version: Int = SAVE_VERSION,
        val savedAtMs: Long = 0L,
        val crc: Long = 0L,
        val payload: String = ""
    )

    class CorruptSaveException(message: String) : Exception(message)

    private val moshi: Moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
    private val stateAdapter: JsonAdapter<GameState> = moshi.adapter(GameState::class.java)
    private val envelopeAdapter: JsonAdapter<SaveEnvelope> = moshi.adapter(SaveEnvelope::class.java)
    private val mapAdapter: JsonAdapter<Map<String, Any?>> =
        moshi.adapter(Types.newParameterizedType(Map::class.java, String::class.java, Any::class.java))

    fun crc(text: String): Long = CRC32().apply { update(text.toByteArray(Charsets.UTF_8)) }.value

    fun encode(state: GameState, savedAtMs: Long): String {
        val payload = stateAdapter.toJson(state)
        return envelopeAdapter.toJson(SaveEnvelope(SAVE_VERSION, savedAtMs, crc(payload), payload))
    }

    fun decode(text: String): GameState {
        val envelope = try {
            envelopeAdapter.fromJson(text)
        } catch (e: Exception) {
            throw CorruptSaveException("Nieczytelna koperta zapisu: ${e.message}")
        } ?: throw CorruptSaveException("Pusta koperta zapisu")

        if (crc(envelope.payload) != envelope.crc) throw CorruptSaveException("Zła suma kontrolna zapisu")
        if (envelope.version > SAVE_VERSION) {
            throw CorruptSaveException("Zapis z nowszej wersji gry (${envelope.version} > $SAVE_VERSION)")
        }

        val payload = if (envelope.version == SAVE_VERSION) envelope.payload else migrate(envelope.payload, envelope.version)
        return try {
            stateAdapter.fromJson(payload)
        } catch (e: Exception) {
            throw CorruptSaveException("Nieczytelny stan gry: ${e.message}")
        } ?: throw CorruptSaveException("Pusty stan gry")
    }

    private fun migrate(payload: String, fromVersion: Int): String {
        val raw = mapAdapter.fromJson(payload)?.toMutableMap() ?: throw CorruptSaveException("Pusty stan do migracji")
        var v = fromVersion
        while (v < SAVE_VERSION) {
            val step = migrations[v] ?: throw CorruptSaveException("Brak migracji z wersji $v")
            step(raw)
            v++
        }
        return mapAdapter.toJson(raw)
    }
}
