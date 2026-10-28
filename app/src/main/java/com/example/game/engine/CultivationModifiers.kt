package com.example.game.engine

import com.example.model.CultivationPath
import com.example.model.Disciple

/** Etap 3: premie z metody, fundamentu, rdzenia, duszy, ścieżki, Praw, krwi, odosobnienia i ran. */
object CultivationModifiers {

    /** [today] = bieżący dzień gry (odosobnienie i rany mają datę końca). */
    fun of(d: Disciple, today: Int): List<Modifier> = buildList {
        if (d.majorRealm == null) return@buildList
        val method = Realms.method(d.methodId)
        if (method.grade.qiMultiplier != 1.0) add(Modifier("Metoda: ${method.name}", Stat.QI_GAIN, ModOp.MULTIPLY, method.grade.qiMultiplier))
        d.foundationQuality?.let { if (it.qiMultiplier != 1.0) add(Modifier("Fundament: ${it.polishName}", Stat.QI_GAIN, ModOp.MULTIPLY, it.qiMultiplier)) }
        if (d.coreGrade > 0) add(Modifier("Złoty Rdzeń gradu ${d.coreGrade}", Stat.COMBAT_DAMAGE, ModOp.PERCENT, 5.0 * d.coreGrade))
        d.soulShape?.let { shape -> Realms.SOUL_SHAPES.firstOrNull { it.first == shape }?.let { add(it.second) } }

        when (d.path) {
            CultivationPath.ORTHODOX -> add(Modifier("Ścieżka Ortodoksyjna", Stat.QI_GAIN, ModOp.PERCENT, 15.0))
            CultivationPath.BODY -> add(Modifier("Ścieżka Ciała", Stat.QI_GAIN, ModOp.MULTIPLY, 0.85))
            CultivationPath.SWORD -> add(Modifier("Ścieżka Miecza", Stat.COMBAT_DAMAGE, ModOp.PERCENT, 20.0))
            CultivationPath.PILLS -> add(Modifier("Ścieżka Pigułek", Stat.COMBAT_DAMAGE, ModOp.PERCENT, -15.0))
            CultivationPath.DEMONIC -> Unit
            CultivationPath.HEART -> {
                add(Modifier("Ścieżka Serca", Stat.QI_GAIN, ModOp.MULTIPLY, 0.8))
                add(Modifier("Ścieżka Serca", Stat.EPIPHANY_CHANCE, ModOp.PERCENT, 100.0))
            }
            null -> Unit
        }

        val lawSum = d.laws.values.sum()
        if (lawSum > 0) add(Modifier("Prawa Dao", Stat.COMBAT_DAMAGE, ModOp.PERCENT, lawSum * 0.5))
        val domains = d.laws.values.count { it >= 100.0 }
        if (domains > 0) add(Modifier("Domena ($domains)", Stat.COMBAT_DAMAGE, ModOp.PERCENT, 30.0 * domains))

        if (d.bloodlineAwakening > 0) {
            add(Modifier("Przebudzona krew ${d.bloodlineAwakening}", Stat.QI_GAIN, ModOp.PERCENT, 5.0 * d.bloodlineAwakening))
            add(Modifier("Przebudzona krew ${d.bloodlineAwakening}", Stat.COMBAT_DAMAGE, ModOp.PERCENT, 10.0 * d.bloodlineAwakening))
        }
        if (d.secludedUntilDay >= today && d.secludedUntilDay > 0) add(Modifier("Odosobnienie", Stat.QI_GAIN, ModOp.MULTIPLY, 3.0))
        if (d.injuredUntilDay >= today && d.injuredUntilDay > 0) add(Modifier("Rany", Stat.QI_GAIN, ModOp.MULTIPLY, 0.5))
    }
}
