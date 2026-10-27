package com.example.game.engine

import kotlin.random.Random

/**
 * Deterministyczny generator losowości (SplitMix64).
 *
 * Dziedziczy po [Random], więc działa ze wszystkimi funkcjami biblioteki standardowej
 * (`list.random(rng)`, `shuffled(rng)`, `nextInt(a, b)`).
 *
 * W logice gry NIGDY nie używamy globalnego `Random` – zawsze strumienia z [GameRng.stream].
 * Strumień zależy od ziarna świata, licznika kroków i nazwy systemu, więc zmiana losowań
 * w jednym systemie (np. walce) nie przesuwa losowań w innym (np. zdarzeniach).
 */
class GameRng(seed: Long) : Random() {

    private var state: Long = seed

    fun nextRaw(): Long {
        state += GOLDEN_GAMMA
        return mix(state)
    }

    override fun nextBits(bitCount: Int): Int {
        if (bitCount <= 0) return 0
        return (nextRaw() ushr (64 - bitCount)).toInt()
    }

    override fun nextLong(): Long = nextRaw()

    /** Zwraca true z prawdopodobieństwem [percent]/100. */
    fun percent(percent: Int): Boolean = nextInt(100) < percent

    /** Deterministyczny identyfikator (zamiast UUID). */
    fun nextId(prefix: String): String = prefix + "_" + java.lang.Long.toHexString(nextRaw())

    companion object {
        private const val GOLDEN_GAMMA = -7046029254386353131L // 0x9E3779B97F4A7C15
        private const val MIX_1 = -4658895280553007687L        // 0xBF58476D1CE4E5B9
        private const val MIX_2 = -7723592293110705685L        // 0x94D049BB133111EB

        fun mix(value: Long): Long {
            var z = value
            z = (z xor (z ushr 30)) * MIX_1
            z = (z xor (z ushr 27)) * MIX_2
            return z xor (z ushr 31)
        }

        /** Stabilny hash nazwy strumienia (String.hashCode jest zdefiniowany w specyfikacji JVM). */
        private fun streamHash(name: String): Long = mix(name.hashCode().toLong() * GOLDEN_GAMMA)

        /** Strumień dla danego kroku silnika i systemu. */
        fun stream(worldSeed: Long, step: Long, name: String): GameRng =
            GameRng(mix(worldSeed xor mix(step + GOLDEN_GAMMA)) xor streamHash(name))
    }
}
