package com.example.game.engine

import com.example.model.*
import kotlin.random.Random

/**
 * Etap 3: postęp etapów i królestw.
 * - mały przełom (etap → etap) dzieje się sam przy 100%, także offline,
 * - wielki przełom (królestwo → królestwo) wymaga spełnienia wąskiego gardła,
 *   a od Złotego Rdzenia – przetrwania tribulacji. Zawsze wymaga obecności gracza
 *   (automat może przejść tylko wielkie przełomy bez tribulacji, gdy gracz jest w grze).
 */
object CultivationSystem {

    const val FOUNDATION_PILL_LESS_CHANCE = 20
    const val TRIBULATION_PILL = "Pigułka Dziewięciu Gromów Tribulacji"
    const val CHAOS_KEY = "Serce Chaosu"
    const val DEVIATION_TOXICITY = 60

    data class Advance(
        val disciple: Disciple?,
        val message: String?,
        val reputation: Int = 0,
        /** true = wielki przełom udany. */
        val majorSuccess: Boolean = false,
        val attempted: Boolean = false,
        val died: Boolean = false,
        val tribulation: TribulationResult? = null
    )

    data class TribulationResult(
        val strikes: Int,
        val damagePerStrike: List<Int>,
        val hpLeft: Int,
        val maxHp: Int,
        val survived: Boolean,
        val perfect: Boolean,
        val catastrophe: Boolean
    )

    /** Dodaje Qi do bieżącego etapu (bez przekraczania wymagania). */
    fun gainQi(d: Disciple, amount: Double): Disciple {
        if (d.majorRealm == null || amount <= 0) return d
        val req = Realms.required(d)
        return Realms.sync(d.copy(stageQi = (d.stageQi + amount).coerceAtMost(req)))
    }

    /** Dodaje procent wymagania bieżącego etapu. */
    fun gainPercent(d: Disciple, percent: Double): Disciple = gainQi(d, Realms.required(d) * percent / 100.0)

    /** Minimalny stopień pigułki, który działa w pełni na danym królestwie. */
    fun pillTierFor(major: MajorRealm): Int = when (major) {
        MajorRealm.BODY_TEMPERING, MajorRealm.QI_CONDENSATION -> 1
        MajorRealm.FOUNDATION -> 2
        MajorRealm.CORE_FORMATION -> 3
        MajorRealm.NASCENT_SOUL, MajorRealm.SOUL_TRANSFORMATION -> 4
        MajorRealm.VOID_TRIBULATION, MajorRealm.MAHAYANA -> 5
        MajorRealm.HEAVENLY_IMMORTAL, MajorRealm.CHAOS_GOLDEN_IMMORTAL -> 6
    }

    /** Skuteczność pigułki: pełna od wymaganego stopnia, każdy stopień niżej ×0,25. */
    fun pillEfficiency(pillTier: Int, major: MajorRealm): Double {
        val gap = (pillTierFor(major) - pillTier).coerceAtLeast(0)
        return Math.pow(0.25, gap.toDouble())
    }

    /** Qi warte [ticks] cykli medytacji ucznia (nagrody, które nie mogą skalować się z wymaganiem etapu). */
    fun ticksWorth(s: GameState, d: Disciple, ticks: Int): Double =
        CultivationRules.meditationGain(d, Modifiers.disciple(Modifiers.sect(s), d)).toDouble() * ticks

    fun ready(d: Disciple): Boolean = d.majorRealm != null && d.stageQi >= Realms.required(d)

    private fun grow(d: Disciple, f: Double): Disciple {
        val hp = (d.maxHp * f).toInt()
        return d.copy(maxHp = hp, currentHp = hp, attack = (d.attack * f).toInt(), defense = (d.defense * f).toInt())
    }

    /** Powód, dla którego wielki przełom jest zablokowany (null = można próbować). */
    fun bottleneck(s: GameState, d: Disciple): String? {
        val major = d.majorRealm ?: return "Brak danych kultywacji."
        val next = major.next ?: return "To szczyt drabiny kultywacji."
        if (next.ordinal > Realms.ceiling(d).ordinal) {
            return "Wąskie gardło metody: ${Realms.method(d.methodId).name} nie sięga ${next.polishName}. Potrzebny lepszy zwój."
        }
        return when (next) {
            MajorRealm.CORE_FORMATION -> when {
                d.path == null -> "Najpierw wybierz ścieżkę kultywacji."
                d.unlockedSkillIds.none { (GameContent.skill(it)?.tier ?: 0) >= 2 } -> "Złoty Rdzeń wymaga opanowania techniki co najmniej 2. stopnia."
                else -> null
            }
            MajorRealm.NASCENT_SOUL -> when {
                d.toxicity >= 30 -> "Toksyczność pigułek (${d.toxicity}) musi spaść poniżej 30."
                d.heartDemon -> "Demon serca blokuje rozbicie rdzenia."
                else -> null
            }
            MajorRealm.SOUL_TRANSFORMATION ->
                if ((d.laws.values.maxOrNull() ?: 0.0) < 10.0) "Transformacja wymaga zrozumienia jednego Prawa Dao na 10%." else null
            MajorRealm.VOID_TRIBULATION -> if (d.heartDemon) "Serce Dao ma skazę – demon serca blokuje przełom." else null
            MajorRealm.MAHAYANA -> {
                val v = d.laws.values
                if (v.count { it >= 50.0 } >= 2 || v.any { it >= 100.0 }) null
                else "Mahajana wymaga dwóch Praw na 50% albo jednego na 100%."
            }
            MajorRealm.HEAVENLY_IMMORTAL -> "Do Niebiańskiego Nieśmiertelnego prowadzi tylko Wniebowstąpienie."
            MajorRealm.CHAOS_GOLDEN_IMMORTAL ->
                if ((s.inventory[CHAOS_KEY] ?: 0) < 1) "Potrzebne $CHAOS_KEY." else null
            else -> null
        }
    }

    /**
     * Próba postępu ucznia ze 100% etapu.
     * [player] = akcja gracza (może przejść tribulację); automat robi tylko małe przełomy
     * i wielkie bez tribulacji.
     */
    fun advance(
        s: GameState,
        d: Disciple,
        usePill: Boolean,
        inventory: MutableMap<String, Int>,
        rng: Random,
        player: Boolean
    ): Advance {
        val major = d.majorRealm ?: return Advance(d, null)
        if (!ready(d)) return Advance(d, null)

        // Mały przełom – zawsze udany.
        if (d.stage < major.stages) {
            val next = Realms.sync(grow(d, Realms.MINOR_STAT_GROWTH).copy(stage = d.stage + 1, stageQi = 0.0))
            return Advance(next, null, reputation = 1)
        }

        val target = major.next ?: return Advance(d, null)
        bottleneck(s, d)?.let { return Advance(d, if (player) it else null) }
        if (target.tribulationStrikes > 0 && !player) return Advance(d, null)

        val sect = Modifiers.sect(s)
        val mods = Modifiers.disciple(sect, d)
        var pillBonus = 0
        if (usePill) {
            val n = inventory[GameContent.BREAKTHROUGH_PILL] ?: 0
            if (n > 0) {
                inventory[GameContent.BREAKTHROUGH_PILL] = n - 1
                pillBonus = 35
            }
        }

        // Odchylenie Qi przy wysokiej toksyczności.
        if (d.toxicity > DEVIATION_TOXICITY && rng.nextInt(100) < 20) {
            val mutated = rng.nextInt(100) < (if (d.path == CultivationPath.DEMONIC) 50 else 25) && d.path != CultivationPath.HEART
            val hurt = d.copy(
                stageQi = Realms.required(d) * 0.6,
                injuredUntilDay = s.currentDay + 20,
                heartDemon = d.heartDemon || mutated,
                traitIds = if (mutated) (d.traitIds + "blood_eyes").distinct() else d.traitIds
            )
            return Advance(
                Realms.sync(hurt),
                "Odchylenie Qi! ${d.name} – toksyczność pigułek rozerwała meridiany (rany na 20 dni)" + if (mutated) ", obudził się demon serca." else ".",
                attempted = true
            )
        }

        if (target.tribulationStrikes == 0) {
            val chance = when (target) {
                MajorRealm.FOUNDATION -> if (pillBonus > 0) 70 + d.karmicLuck / 10 else FOUNDATION_PILL_LESS_CHANCE
                else -> CultivationRules.breakthroughChance(d, pillBonus, s.timeOfDay, mods.flat(Stat.BREAKTHROUGH_CHANCE).toInt())
            }
            return if (rng.nextInt(100) < chance) {
                val entered = enter(d, target, pillBonus > 0, perfect = false, rng)
                Advance(entered, "🌟 ${d.name} wkracza w ${target.polishName}!", 25, majorSuccess = true, attempted = true)
            } else {
                Advance(Realms.sync(d.copy(stageQi = Realms.required(d) * 0.4)), "Przełom ${d.name} się nie powiódł (postęp spadł do 40%).", attempted = true)
            }
        }

        val trib = tribulation(d, target, inventory, rng)
        return when {
            trib.catastrophe -> Advance(null, "☠️ Katastrofa! ${d.name} nie przetrwał gniewu niebios.", -10, attempted = true, died = true, tribulation = trib)
            !trib.survived -> Advance(
                Realms.sync(d.copy(stageQi = Realms.required(d) * 0.6, currentHp = (d.maxHp / 10).coerceAtLeast(1), injuredUntilDay = s.currentDay + 15)),
                "⚡ ${d.name} padł pod piorunami tribulacji. Rany na 15 dni, postęp spadł do 60%.", attempted = true, tribulation = trib
            )
            else -> {
                val entered = enter(d, target, pillBonus > 0, trib.perfect, rng)
                Advance(
                    entered,
                    (if (trib.perfect) "⚡ Doskonała tribulacja! " else "⚡ ") + "${d.name} wkracza w ${target.polishName}!",
                    if (trib.perfect) 60 else 40, majorSuccess = true, attempted = true, tribulation = trib
                )
            }
        }
    }

    /** Wejście do nowego królestwa: statystyki i trwała jakość wyniku. */
    private fun enter(d: Disciple, target: MajorRealm, pill: Boolean, perfect: Boolean, rng: Random): Disciple {
        var x = grow(d, Realms.MAJOR_STAT_GROWTH * if (perfect) 1.1 else 1.0)
            .copy(majorRealm = target, stage = 1, stageQi = 0.0, daoPoints = d.daoPoints + 2)
        when (target) {
            MajorRealm.FOUNDATION -> {
                val roll = rng.nextInt(100)
                val q = if (pill) when {
                    roll < 5 -> FoundationQuality.HEAVENLY
                    roll < 30 -> FoundationQuality.PERFECT
                    roll < 90 -> FoundationQuality.NORMAL
                    else -> FoundationQuality.CRACKED
                } else when {
                    roll < 5 -> FoundationQuality.PERFECT
                    roll < 60 -> FoundationQuality.NORMAL
                    else -> FoundationQuality.CRACKED
                }
                x = x.copy(foundationQuality = q)
            }
            MajorRealm.CORE_FORMATION -> {
                val base = 2 + rng.nextInt(4) + (if (perfect) 2 else 0) + when (d.foundationQuality) {
                    FoundationQuality.HEAVENLY -> 2
                    FoundationQuality.PERFECT -> 1
                    FoundationQuality.CRACKED -> -1
                    else -> 0
                }
                val grade = if (rng.nextInt(100) == 0) 9 else base.coerceIn(1, 8)
                x = x.copy(coreGrade = grade)
            }
            MajorRealm.NASCENT_SOUL -> x = x.copy(soulShape = Realms.SOUL_SHAPES.random(rng).first)
            else -> Unit
        }
        return Realms.sync(x)
    }

    /** Tribulacja: kolejne uderzenia pioruna osłabiane przez obronę i przygotowanie. */
    fun tribulation(d: Disciple, target: MajorRealm, inventory: MutableMap<String, Int>, rng: Random): TribulationResult {
        var mitigation = (d.defense / (d.defense + 500.0)).coerceAtMost(0.6)
        if (d.path == CultivationPath.BODY) mitigation += 0.3
        val pills = inventory[TRIBULATION_PILL] ?: 0
        if (pills > 0) {
            inventory[TRIBULATION_PILL] = pills - 1
            mitigation += 0.3
        }
        mitigation = mitigation.coerceAtMost(0.85)
        var hp = d.maxHp
        val hits = mutableListOf<Int>()
        for (i in 0 until target.tribulationStrikes) {
            val variance = 0.85 + rng.nextDouble() * 0.3
            val dmg = (d.maxHp * 0.15 * (1 + i * 0.08) * variance * (1 - mitigation)).toInt()
            hits += dmg
            hp -= dmg
            if (hp <= 0) break
        }
        val survived = hp > 0
        return TribulationResult(
            strikes = target.tribulationStrikes,
            damagePerStrike = hits,
            hpLeft = hp.coerceAtLeast(0),
            maxHp = d.maxHp,
            survived = survived,
            perfect = survived && hp > d.maxHp * 0.7,
            catastrophe = !survived && rng.nextInt(100) < 2
        )
    }
}
