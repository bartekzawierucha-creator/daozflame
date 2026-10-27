package com.example.game.engine

import com.example.model.Disciple
import com.example.model.DiplomaticStatus
import com.example.model.SectPolicy

/** Statystyki, na które działają premie. Nowy system = nowe wartości tutaj, nie nowe ify w kodzie. */
enum class Stat(val label: String) {
    QI_GAIN("Przyrost Qi"),
    BREAKTHROUGH_CHANCE("Szansa przełomu"),
    EPIPHANY_CHANCE("Szansa olśnienia"),
    STONE_INCOME("Dochód kamieni"),
    GATHER_YIELD("Zbiory ziół i rud"),
    ALCHEMY_COST("Koszt alchemii"),
    FORGE_COST("Koszt kuźni"),
    COMBAT_DAMAGE("Obrażenia w walce"),
    RECRUIT_POOL("Kandydaci do sekty"),
    REPUTATION_GAIN("Zysk reputacji")
}

enum class ModOp {
    /** Dodaje stałą wartość do bazy (np. +25 pkt szansy). */
    FLAT,
    /** Procent – wszystkie procenty jednej statystyki sumują się, potem mnożą bazę. */
    PERCENT,
    /** Osobny mnożnik (np. talent ×1,5). */
    MULTIPLY
}

data class Modifier(val source: String, val stat: Stat, val op: ModOp, val value: Double)

/**
 * Jedyne miejsce liczenia premii. Kolejność: (baza + FLAT) × (1 + ΣPERCENT/100) × ΠMULTIPLY.
 * Ta sama funkcja liczy wartość w logice i rozbicie w tooltipie, więc nie mogą się rozjechać.
 */
data class ModifierStack(
    val modifiers: List<Modifier>,
    val day: Int = 0,
    /** Etap 6: stan, z którego liczy się premie z tajemnic (reguły świata, Dziennik, pora dnia). */
    val state: GameState? = null
) {

    fun of(stat: Stat): List<Modifier> = modifiers.filter { it.stat == stat }

    fun apply(stat: Stat, base: Double): Double {
        val mods = of(stat)
        val flat = mods.filter { it.op == ModOp.FLAT }.sumOf { it.value }
        val percent = mods.filter { it.op == ModOp.PERCENT }.sumOf { it.value }
        val mult = mods.filter { it.op == ModOp.MULTIPLY }.fold(1.0) { acc, m -> acc * m.value }
        return (base + flat) * (1.0 + percent / 100.0) * mult
    }

    fun percent(stat: Stat): Double = of(stat).filter { it.op == ModOp.PERCENT }.sumOf { it.value }
    fun flat(stat: Stat): Double = of(stat).filter { it.op == ModOp.FLAT }.sumOf { it.value }

    /** Mnożnik procentowy (1 + Σ%/100) – dla kosztów i plonów. */
    fun factor(stat: Stat): Double = (1.0 + percent(stat) / 100.0) *
        of(stat).filter { it.op == ModOp.MULTIPLY }.fold(1.0) { acc, m -> acc * m.value }

    operator fun plus(other: List<Modifier>) = ModifierStack(modifiers + other, day, state)

    /** Tekst do tooltipa „skąd to?”. */
    fun breakdown(stat: Stat): List<String> = of(stat).map { m ->
        val v = when (m.op) {
            ModOp.FLAT -> "%+.0f".format(java.util.Locale.ROOT, m.value)
            ModOp.PERCENT -> "%+.0f%%".format(java.util.Locale.ROOT, m.value)
            ModOp.MULTIPLY -> "×%.2f".format(java.util.Locale.ROOT, m.value)
        }
        "${m.source}: $v"
    }
}

object Modifiers {

    /** Premie całej sekty: edykty, polityka, bestia, sojusze. */
    fun sect(s: GameState): ModifierStack {
        val list = mutableListOf<Modifier>()
        list += EdictCatalog.activeModifiers(s)

        val policy = when (s.currentPolicy) {
            SectPolicy.RIGHTEOUS_DAO -> 25
            SectPolicy.ISOLATIONIST -> 35
            SectPolicy.EXPANSIONIST -> 10
            SectPolicy.DEMONIC_RAIDERS -> 15
            SectPolicy.MERCHANT_CONSORTIUM -> 5
        }
        list += Modifier("Doktryna: ${s.currentPolicy.polishName}", Stat.QI_GAIN, ModOp.PERCENT, policy.toDouble())

        s.activeBeast?.let { b ->
            list += Modifier("Bestia: ${b.name}", Stat.QI_GAIN, ModOp.PERCENT, (b.tier * 12 + b.intimacy / 4).toDouble())
        }
        val alliances = s.rivalSects.filter { it.status == DiplomaticStatus.ALLIANCE }.sumOf { it.cultivationBuffBonusPercent }
        if (alliances != 0) list += Modifier("Sojusze sekt", Stat.QI_GAIN, ModOp.PERCENT, alliances.toDouble())
        list += DiscipleRules.sectModifiers(s)
        if (s.e5Initialized && Catastrophes.qiPenaltyPercent(s) != 0.0) {
            list += Modifier("Burza Qi", Stat.QI_GAIN, ModOp.PERCENT, Catastrophes.qiPenaltyPercent(s))
        }
        if (s.e5Initialized && Formation.fiveElements(s)) {
            list += Modifier("Formacja Pięciu Żywiołów", Stat.QI_GAIN, ModOp.PERCENT, Formation.qiBonusPercent(s))
        }
        if (s.e4Initialized && s.veinLevel > 1) {
            list += Modifier("Żyła: ${Vein.name(s.veinLevel)}", Stat.QI_GAIN, ModOp.MULTIPLY, Vein.qiMultiplier(s.veinLevel))
        }
        if (s.ascendedNames.isNotEmpty()) {
            list += Modifier("Dziedzictwo Wniebowstąpienia (${s.ascendedNames.size})", Stat.QI_GAIN, ModOp.PERCENT, 5.0 * s.ascendedNames.size)
        }
        return ModifierStack(list, s.currentDay, s)
    }

    /** Premie sekty + cechy konkretnego ucznia. */
    fun disciple(sect: ModifierStack, d: Disciple): ModifierStack {
        val list = mutableListOf<Modifier>()
        list += Modifier("Talent ${d.talentGrade.name}", Stat.QI_GAIN, ModOp.MULTIPLY, CultivationRules.talentMultiplier(d.talentGrade))
        d.fatedTraits.filter { it.cultivationBonusPercent != 0 }.forEach {
            list += Modifier("Cecha: ${it.name}", Stat.QI_GAIN, ModOp.PERCENT, it.cultivationBonusPercent.toDouble())
        }
        if (d.ancientClan.name.contains("Ye")) list += Modifier("Ród Ye", Stat.QI_GAIN, ModOp.PERCENT, 50.0)
        list += DiscipleRules.discipleModifiers(d)
        list += CultivationModifiers.of(d, sect.day)
        d.spiritRoot?.let { list += Seasons.discipleModifiers(Seasons.of(sect.day), it.elements) }
        sect.state?.let { list += SecretModifiers.of(it, d) }
        return sect + list
    }
}
