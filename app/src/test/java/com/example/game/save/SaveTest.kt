package com.example.game.save

import com.example.game.engine.GameCommand
import com.example.game.engine.GameEngine
import com.example.game.engine.GameState
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.file.Files

class SaveTest {

    private fun playedState(): GameState {
        var s = GameEngine.newGame(77L, nowMs = 1_000L)
        repeat(300) { s = GameEngine.apply(s, GameCommand.Tick).state }
        s = GameEngine.apply(s, GameCommand.StartExpedition(s.regions.first().id, s.disciples.first().id)).state
        repeat(4) { s = GameEngine.apply(s, GameCommand.CombatStep).state }
        return s
    }

    /** Etap 7: zapis po 5 latach gry (600 dni) ma < 2 MB i wczytuje się < 1 s. */
    @Test
    fun fiveYearSaveIsSmallAndLoadsFast() {
        var s = GameEngine.newGame(91L, nowMs = 1_000L)
        while (s.currentDay < 600) {
            s = GameEngine.apply(s, GameCommand.Tick).state
            if (s.activeEventId != null) s = GameEngine.apply(s, GameCommand.EventChoice(0)).state
        }
        val text = SaveCodec.encode(s, 0L)
        assertTrue("rozmiar ${text.length / 1024} KB", text.toByteArray().size < 2 * 1024 * 1024)
        val t0 = System.nanoTime()
        val loaded = SaveCodec.decode(text)
        val ms = (System.nanoTime() - t0) / 1_000_000
        assertTrue("wczytanie $ms ms", ms < 1_000)
        assertEquals(s, loaded)
    }

    @Test
    fun roundTripKeepsEveryField() {
        val s = playedState()
        assertEquals(s, SaveCodec.decode(SaveCodec.encode(s, 123L)))
    }

    @Test
    fun loadedGameContinuesIdentically() {
        val s = playedState()
        val loaded = SaveCodec.decode(SaveCodec.encode(s, 0L))
        val a = (1..100).fold(s) { st, _ -> GameEngine.apply(st, GameCommand.Tick).state }
        val b = (1..100).fold(loaded) { st, _ -> GameEngine.apply(st, GameCommand.Tick).state }
        assertEquals(a, b)
    }

    @Test(expected = SaveCodec.CorruptSaveException::class)
    fun tamperedPayloadIsRejected() {
        val text = SaveCodec.encode(playedState(), 0L)
        SaveCodec.decode(text.replace("spiritStones", "spiritStonez"))
    }

    @Test
    fun missingFieldsFallBackToDefaults() {
        // Symuluje stary zapis bez nowszych pól: usuwamy pola z payloadu i liczymy nową sumę.
        val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
        val mapType = Types.newParameterizedType(Map::class.java, String::class.java, Any::class.java)
        val mapAdapter = moshi.adapter<Map<String, Any?>>(mapType)
        val envelope = mapAdapter.fromJson(SaveCodec.encode(playedState(), 0L))!!
        val payload = mapAdapter.fromJson(envelope["payload"] as String)!!.toMutableMap()
        payload.remove("lastActiveAtMs")
        payload.remove("recentEventIds")
        payload.remove("combat")
        val newPayload = mapAdapter.toJson(payload)
        val newEnvelope = envelope.toMutableMap().apply {
            put("payload", newPayload)
            put("crc", SaveCodec.crc(newPayload))
        }
        val loaded = SaveCodec.decode(mapAdapter.toJson(newEnvelope))
        assertEquals(0L, loaded.lastActiveAtMs)
        assertTrue(loaded.recentEventIds.isEmpty())
        assertNull(loaded.combat)
    }

    @Test
    fun storeSavesAtomicallyAndFallsBackToBackup() {
        val dir = Files.createTempDirectory("daoz-save").toFile()
        val store = SaveStore(dir)
        assertNull(store.load().state)

        val first = playedState()
        store.save(first, 1L)
        val second = GameEngine.apply(first, GameCommand.Tick).state
        store.save(second, 2L)
        assertEquals(second, store.load().state)

        store.mainFile.writeText("{ uszkodzony")
        val r = store.load()
        assertEquals(first, r.state)
        assertTrue(r.fromBackup)
        assertNotNull(r.error)
        dir.deleteRecursively()
    }

    /**
     * Złote zapisy: każdy plik z test/resources/saves musi się wczytać w bieżącej wersji.
     * Gdy brakuje zapisu bieżącej wersji, test go generuje do build/golden – trzeba go dodać do repo.
     */
    @Test
    fun goldenSavesStillLoad() {
        val dirs = listOf(File("src/test/resources/saves"), File("app/src/test/resources/saves"))
        val golden = dirs.firstOrNull { it.isDirectory }
        golden?.listFiles { f -> f.extension == "json" }?.forEach { f ->
            val state = SaveCodec.decode(f.readText())
            assertTrue("${f.name}: brak uczniów", state.disciples.isNotEmpty())
        }
        val current = "save_v${SaveCodec.SAVE_VERSION}.json"
        if (golden?.resolve(current)?.exists() != true) {
            val out = File("build/golden").apply { mkdirs() }
            out.resolve(current).writeText(SaveCodec.encode(playedState(), 0L))
        }
    }
}
