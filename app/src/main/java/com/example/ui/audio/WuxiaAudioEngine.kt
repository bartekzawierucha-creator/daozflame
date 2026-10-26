package com.example.ui.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin
import kotlin.random.Random

/**
 * Autentyczny syntezator krajobrazu dźwiękowego Wuxia/Xianxia generowany w czasie rzeczywistym.
 * Nie wymaga zewnętrznych plików MP3 — tworzy czyste, mistyczne fale akustyczne:
 * - Dzwon Świątynny / Gong Przełomu Dao
 * - Cięcie Miecza Nieśmiertelnego (Sword Clash)
 * - Wybuch Boskiego Qi (Divine Skill Burst)
 * - Płomień Kotła Alchemicznego (Cauldron Fire)
 * - Uderzenie Niebiańskiej Kary (Heavenly Lightning)
 */
object WuxiaAudioEngine {

    private val audioScope = CoroutineScope(Dispatchers.Default)
    private const val SAMPLE_RATE = 22050

    var isMuted: Boolean = false

    fun playSwordClash() {
        if (isMuted) return
        audioScope.launch {
            try {
                val durationSec = 0.22
                val numSamples = (durationSec * SAMPLE_RATE).toInt()
                val samples = ShortArray(numSamples)

                for (i in 0 until numSamples) {
                    val t = i.toDouble() / SAMPLE_RATE
                    val decay = exp(-t * 22.0)
                    // Sharp metallic harmonic frequencies (1800Hz, 3200Hz, 4800Hz)
                    val tone = 0.5 * sin(2.0 * PI * 1850.0 * t) +
                            0.3 * sin(2.0 * PI * 3400.0 * t) +
                            0.2 * (Random.nextDouble() * 2.0 - 1.0)
                    val s = (tone * decay * 28000).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
                    samples[i] = s.toShort()
                }
                playBuffer(samples)
            } catch (_: Exception) {}
        }
    }

    fun playDivineGong() {
        if (isMuted) return
        audioScope.launch {
            try {
                val durationSec = 1.2
                val numSamples = (durationSec * SAMPLE_RATE).toInt()
                val samples = ShortArray(numSamples)

                for (i in 0 until numSamples) {
                    val t = i.toDouble() / SAMPLE_RATE
                    val decay = exp(-t * 3.5)
                    // Deep bronze temple gong fundamental (196Hz - Sol) with rich shimmer harmonics
                    val tone = 0.6 * sin(2.0 * PI * 196.0 * t) +
                            0.25 * sin(2.0 * PI * 392.0 * t) +
                            0.15 * sin(2.0 * PI * 587.3 * t)
                    val s = (tone * decay * 27000).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
                    samples[i] = s.toShort()
                }
                playBuffer(samples)
            } catch (_: Exception) {}
        }
    }

    fun playThunderStrike() {
        if (isMuted) return
        audioScope.launch {
            try {
                val durationSec = 0.65
                val numSamples = (durationSec * SAMPLE_RATE).toInt()
                val samples = ShortArray(numSamples)

                for (i in 0 until numSamples) {
                    val t = i.toDouble() / SAMPLE_RATE
                    val initialShock = if (t < 0.04) 1.0 else exp(-(t - 0.04) * 8.0)
                    val noise = Random.nextDouble() * 2.0 - 1.0
                    val rumble = sin(2.0 * PI * (65.0 - t * 25.0) * t)
                    val tone = 0.7 * noise + 0.3 * rumble
                    val s = (tone * initialShock * 29000).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
                    samples[i] = s.toShort()
                }
                playBuffer(samples)
            } catch (_: Exception) {}
        }
    }

    fun playCauldronFlame() {
        if (isMuted) return
        audioScope.launch {
            try {
                val durationSec = 0.4
                val numSamples = (durationSec * SAMPLE_RATE).toInt()
                val samples = ShortArray(numSamples)

                for (i in 0 until numSamples) {
                    val t = i.toDouble() / SAMPLE_RATE
                    val env = sin(PI * (t / durationSec))
                    val roar = sin(2.0 * PI * (120.0 + sin(t * 30.0) * 40.0) * t)
                    val hiss = (Random.nextDouble() * 2.0 - 1.0) * 0.4
                    val s = ((roar + hiss) * env * 22000).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
                    samples[i] = s.toShort()
                }
                playBuffer(samples)
            } catch (_: Exception) {}
        }
    }

    fun playUltimateBurst() {
        if (isMuted) return
        audioScope.launch {
            try {
                val durationSec = 0.8
                val numSamples = (durationSec * SAMPLE_RATE).toInt()
                val samples = ShortArray(numSamples)

                for (i in 0 until numSamples) {
                    val t = i.toDouble() / SAMPLE_RATE
                    val sweepFreq = 180.0 + (t / durationSec) * 720.0
                    val env = exp(-t * 2.0)
                    val tone = 0.6 * sin(2.0 * PI * sweepFreq * t) + 0.3 * sin(2.0 * PI * sweepFreq * 2.0 * t)
                    val s = (tone * env * 28000).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
                    samples[i] = s.toShort()
                }
                playBuffer(samples)
            } catch (_: Exception) {}
        }
    }

    private fun playBuffer(samples: ShortArray) {
        val minBuf = AudioTrack.getMinBufferSize(
            SAMPLE_RATE,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        val bufferSize = maxOf(minBuf, samples.size * 2)

        val track = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(SAMPLE_RATE)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setBufferSizeInBytes(bufferSize)
            .setTransferMode(AudioTrack.MODE_STATIC)
            .build()

        track.write(samples, 0, samples.size)
        track.play()

        // Release after finish
        track.setNotificationMarkerPosition(samples.size)
        track.setPlaybackPositionUpdateListener(object : AudioTrack.OnPlaybackPositionUpdateListener {
            override fun onMarkerReached(t: AudioTrack?) {
                t?.stop()
                t?.release()
            }
            override fun onPeriodicNotification(t: AudioTrack?) {}
        })
    }
}
