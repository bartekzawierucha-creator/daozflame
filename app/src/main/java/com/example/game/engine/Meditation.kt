package com.example.game.engine

import com.example.model.Disciple
import com.example.model.SpiritElement
import com.example.model.TimeOfDay
import kotlin.random.Random

/** Etap 3: jeden cykl medytacji – Qi, olśnienie, Prawa Dao, wgląd. */
object Meditation {

    const val LAW_RATE = 0.002
    const val INSIGHT_CHANCE_PERMILLE = 5
    const val INSIGHT_LAW_PERCENT = 5.0
    const val EPIPHANY_TICKS = 3

    /** Pory dnia sprzyjające żywiołom korzenia (wgląd ×3). */
    val RESONANCE: Map<TimeOfDay, Set<SpiritElement>> = mapOf(
        TimeOfDay.DAWN to setOf(SpiritElement.WOOD, SpiritElement.WIND),
        TimeOfDay.DAY to setOf(SpiritElement.FIRE, SpiritElement.METAL),
        TimeOfDay.DUSK to setOf(SpiritElement.EARTH, SpiritElement.METAL),
        TimeOfDay.NIGHT to setOf(SpiritElement.WATER, SpiritElement.THUNDER),
        TimeOfDay.FULL_MOON to setOf(SpiritElement.ICE, SpiritElement.WATER, SpiritElement.CHAOS)
    )

    fun meditate(frame: TickFrame, d0: Disciple, rng: Random, efficiency: Double): Disciple {
        val mods = Modifiers.disciple(frame.sectMods, d0)
        val gain = CultivationRules.meditationGain(d0, mods) * efficiency
        var d = CultivationSystem.gainQi(d0, gain)

        // Olśnienie: Qi warte EPIPHANY_TICKS cykli medytacji i punkt Dao.
        if (rng.nextInt(100) < CultivationRules.epiphanyChance(d0, mods)) {
            d = CultivationSystem.gainQi(d, gain * EPIPHANY_TICKS).copy(daoPoints = d.daoPoints + 1)
        }

        if (d.laws.isNotEmpty()) {
            val per = LAW_RATE * (d.comprehension / 50.0) / d.laws.size
            d = d.copy(laws = d.laws.mapValues { (it.value + per).coerceAtMost(100.0) })
        }

        val resonant = d.spiritRoot?.elements?.any { it in (RESONANCE[frame.timeOfDay] ?: emptySet()) } == true
        val insightPermille = INSIGHT_CHANCE_PERMILLE * (if (resonant) 3 else 1)
        if (rng.nextInt(1000) < insightPermille) {
            d = d.copy(
                daoPoints = d.daoPoints + 2,
                laws = d.laws.mapValues { (it.value + INSIGHT_LAW_PERCENT).coerceAtMost(100.0) }
            )
            // Etap 12: wgląd trafia do dziennika ucznia, nie na ekran (było ~300 komunikatów na godzinę).
            d = d.copy(diary = (d.diary + "Dz. ${frame.day}: Wgląd Dao – pojąłem coś, czego nie da się nauczyć (+2 pkt Dao).").takeLast(Autonomy.DIARY_SIZE))
        }
        return d
    }
}
