package com.example.game.engine

import com.example.model.FacilityType
import com.example.model.ItemQuality
import com.example.model.Season
import com.example.model.SpiritElement
import kotlin.random.Random

/** Etap 4: jakość wyrobów w magazynie. Jakość jest częścią klucza przedmiotu („Nazwa · Doskonała”). */
object Quality {

    fun key(base: String, q: ItemQuality): String = base + q.suffix

    fun parse(key: String): Pair<String, ItemQuality> {
        val q = ItemQuality.entries.firstOrNull { it.suffix.isNotEmpty() && key.endsWith(it.suffix) } ?: ItemQuality.NORMAL
        return key.removeSuffix(q.suffix) to q
    }

    fun count(inventory: Map<String, Int>, base: String): Int =
        ItemQuality.entries.sumOf { inventory[key(base, it)] ?: 0 }

    /** Zużywa najlepszą dostępną sztukę; zwraca jej jakość albo null. */
    fun takeBest(inventory: MutableMap<String, Int>, base: String): ItemQuality? {
        val q = ItemQuality.entries.reversed().firstOrNull { (inventory[key(base, it)] ?: 0) > 0 } ?: return null
        val k = key(base, q)
        inventory[k] = (inventory[k] ?: 0) - 1
        return q
    }

    /** Losowanie jakości: 15 / 55 / 22 / 7 / 1 %, [bonus] przesuwa wynik w górę (maks. +40). */
    fun roll(rng: Random, bonus: Int): ItemQuality {
        val r = rng.nextInt(100) + bonus.coerceIn(0, 40)
        return when {
            r < 15 -> ItemQuality.CRACKED
            r < 70 -> ItemQuality.NORMAL
            r < 92 -> ItemQuality.EXCELLENT
            r < 99 -> ItemQuality.LEGENDARY
            else -> ItemQuality.HEAVENLY
        }
    }
}

/** Pory roku: 4 × 30 dni. */
object Seasons {
    const val DAYS = 30

    fun of(day: Int): Season = Season.entries[((day - 1).coerceAtLeast(0) / DAYS) % 4]

    fun dayOfSeason(day: Int): Int = (day - 1).coerceAtLeast(0) % DAYS + 1

    /** Premie pory roku dla ucznia (Ogień latem, Lód zimą). */
    fun discipleModifiers(season: Season, elements: List<SpiritElement>): List<Modifier> = buildList {
        if (season == Season.SUMMER && SpiritElement.FIRE in elements) add(Modifier("Lato (korzeń Ognia)", Stat.QI_GAIN, ModOp.PERCENT, 20.0))
        if (season == Season.WINTER && SpiritElement.ICE in elements) add(Modifier("Zima (korzeń Lodu)", Stat.QI_GAIN, ModOp.PERCENT, 20.0))
    }

    fun fieldYieldFactor(season: Season): Double = when (season) {
        Season.SPRING -> 2.0
        Season.SUMMER -> 1.0
        Season.AUTUMN -> 1.2
        Season.WINTER -> 0.0
    }

    fun sellFactor(season: Season): Double = if (season == Season.AUTUMN) 1.3 else 1.0
}

/** Żyła duchowa pod górą – najsilniejszy mnożnik Qi sekty. */
object Vein {
    const val MAX_LEVEL = 9
    private val QI = listOf(1.0, 1.25, 1.5, 1.85, 2.2, 2.6, 3.0, 3.5, 4.0)
    private val NAMES = listOf("Strumień", "Potok", "Rzeka", "Wartka Rzeka", "Jezioro", "Głębokie Jezioro", "Morze", "Ocean", "Smocza Żyła")

    fun qiMultiplier(level: Int): Double = QI[(level - 1).coerceIn(0, QI.lastIndex)]
    fun name(level: Int): String = NAMES[(level - 1).coerceIn(0, NAMES.lastIndex)]

    data class UpgradeCost(val stones: Int, val mineral: String, val mineralCount: Int, val days: Int)

    fun upgradeCost(currentLevel: Int): UpgradeCost? {
        val next = currentLevel + 1
        if (next > MAX_LEVEL) return null
        val (mineral, count) = when {
            next <= 3 -> "Duchowy Nefryt" to 10 * next
            next <= 6 -> GameContent.FLAME_CRYSTAL to 5 * (next - 2)
            next <= 8 -> "Rdzeń Gwiazdy" to 2
            else -> "Jadeit Chaosu" to 2
        }
        return UpgradeCost(stones = 2_000 * next * next, mineral = mineral, mineralCount = count, days = 3 + next)
    }

    /** Najwyższy stopień zioła, które urośnie na polach przy danym poziomie żyły. */
    fun fieldTier(level: Int): Int = when {
        level >= 7 -> 5
        level >= 5 -> 4
        level >= 3 -> 3
        else -> 2
    }

    /** Dodatkowy slot edyktu od 5. poziomu żyły. */
    fun extraEdictSlots(level: Int): Int = if (level >= 5) 1 else 0

    /** Smocza Żyła łagodzi tribulacje. */
    fun tribulationMitigation(level: Int): Double = if (level >= 9) 0.1 else 0.0

    fun startUpgrade(s: GameState): StepResult {
        if (s.veinUpgradeDoneDay > 0) return fail(s, "Rozbudowa żyły już trwa (do dnia ${s.veinUpgradeDoneDay}).")
        val cost = upgradeCost(s.veinLevel) ?: return fail(s, "Żyła osiągnęła już poziom Smoczej Żyły.")
        if (s.spiritStones < cost.stones) return fail(s, "Rozbudowa żyły kosztuje ${cost.stones} kamieni.")
        val have = s.inventory[cost.mineral] ?: 0
        if (have < cost.mineralCount) return fail(s, "Rozbudowa żyły wymaga ${cost.mineralCount}× ${cost.mineral} (masz $have).")
        return ok(
            s.copy(
                spiritStones = s.spiritStones - cost.stones,
                inventory = s.inventory + (cost.mineral to have - cost.mineralCount),
                veinUpgradeDoneDay = s.currentDay + cost.days
            ),
            "Rozpoczęto pogłębianie żyły duchowej do poziomu ${s.veinLevel + 1} (${cost.days} dni)."
        )
    }

    fun facilityLevel(s: GameState, type: FacilityType): Int = s.facilities.find { it.type == type }?.level ?: 1
}
