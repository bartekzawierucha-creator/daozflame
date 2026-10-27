package com.example.game.engine

import com.example.model.*
import kotlin.random.Random

/** Reguły kultywacji – wspólne dla pętli gry, komend gracza i symulatora. */
object CultivationRules {

    data class BreakthroughResult(
        val disciple: Disciple,
        val message: String,
        val reputation: Int,
        val success: Boolean
    )

    fun talentMultiplier(grade: TalentGrade): Double = when (grade) {
        TalentGrade.SSS -> 1.8
        TalentGrade.SS -> 1.5
        TalentGrade.S -> 1.25
        TalentGrade.A -> 1.1
        TalentGrade.B -> 1.0
    }

    /** Przyrost postępu Qi za jeden cykl medytacji (bez błysku oświecenia). */
    fun meditationGain(d: Disciple, stack: ModifierStack): Int {
        val base = 10 + d.comprehension / 8
        return stack.apply(Stat.QI_GAIN, base.toDouble()).toInt().coerceAtLeast(1)
    }

    /** Szansa na błysk oświecenia (%) – zależy od karmy, wzmacniają ją edykty. */
    fun epiphanyChance(d: Disciple, stack: ModifierStack): Int =
        ((d.karmicLuck / 25).coerceIn(3, 20) * stack.factor(Stat.EPIPHANY_CHANCE)).toInt().coerceIn(0, 100)

    fun breakthroughChance(d: Disciple, pillBonus: Int, time: TimeOfDay, bonus: Int = 0): Int {
        var chance = 40 + d.karmicLuck / 5 + pillBonus + bonus
        if (time == TimeOfDay.FULL_MOON) chance += 45
        if (d.bloodline == BloodlineType.PURE_MORTAL) chance += 15
        return chance
    }

    fun nextRealm(current: CultivationRealm): CultivationRealm? {
        val all = CultivationRealm.entries
        val i = all.indexOf(current)
        return if (i < all.size - 1) all[i + 1] else null
    }

    /** Próba przełomu. Zużywa pigułkę z [inventory], jeśli [usePill] i jest dostępna. */
    fun breakthrough(
        d: Disciple,
        usePill: Boolean,
        inventory: MutableMap<String, Int>,
        time: TimeOfDay,
        rng: Random,
        chanceBonus: Int = 0
    ): BreakthroughResult {
        var pillBonus = 0
        if (usePill) {
            val count = inventory[GameContent.BREAKTHROUGH_PILL] ?: 0
            if (count > 0) {
                inventory[GameContent.BREAKTHROUGH_PILL] = count - 1
                pillBonus = 35
            }
        }
        val success = rng.nextInt(100) < breakthroughChance(d, pillBonus, time, chanceBonus)
        val next = nextRealm(d.realm)
        return if (success && next != null) {
            val hp = (d.maxHp * 1.45).toInt()
            BreakthroughResult(
                d.copy(
                    realm = next,
                    realmProgress = 0,
                    maxHp = hp,
                    currentHp = hp,
                    attack = (d.attack * 1.4).toInt(),
                    defense = (d.defense * 1.35).toInt(),
                    daoPoints = d.daoPoints + 2
                ),
                "🌟 ${d.name} osiągnął poziom: ${next.polishName}!", 25, true
            )
        } else {
            BreakthroughResult(
                d.copy(realmProgress = 40, currentHp = (d.currentHp * 0.7).toInt().coerceAtLeast(10)),
                "Przełom ${d.name} napotkał tribulację Qi (zredukowano do 40%).", 0, false
            )
        }
    }

    fun combatPower(hp: Int, atk: Int, def: Int, comprehension: Int, luck: Int): Int =
        hp / 2 + atk * 14 + def * 9 + comprehension * 6 + luck * 5
}
