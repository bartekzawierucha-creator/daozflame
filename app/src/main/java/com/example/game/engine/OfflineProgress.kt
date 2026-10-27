package com.example.game.engine

import com.example.game.content.ResourceCatalog
import com.example.model.DiscipleRole

/**
 * Postęp offline: uproszczony model wartości oczekiwanych zamiast symulacji tick po ticku
 * (12 h = 27 000 ticków – na telefonie to byłoby kilka sekund zamrożenia).
 *
 * Zasady:
 * - limit [MAX_HOURS], wydajność [EFFICIENCY] względem gry na żywo,
 * - postęp Qi zatrzymuje się na 99% – przełom wymaga obecności gracza,
 * - zdarzenia z wyborem nie wypadają offline,
 * - cofnięty zegar telefonu (ujemny czas) = brak postępu.
 */
object OfflineProgress {

    const val MAX_HOURS = 12
    const val EFFICIENCY = 0.6
    const val SECONDS_PER_TICK = 1.6
    const val MIN_ELAPSED_MS = 60_000L
    val TICKS_PER_DAY = TimePhase.TICKS_PER_DAY

    data class Report(
        val elapsedMs: Long,
        val countedMs: Long,
        val ticks: Long,
        val stones: Int,
        val upkeep: Int,
        val days: Int,
        val qiGained: Map<String, Int>,
        val readyForBreakthrough: Int,
        val items: Map<String, Int>
    ) {
        val capped: Boolean get() = countedMs < elapsedMs
    }

    fun apply(s: GameState, elapsedMs: Long, ctx: EngineContext): StepResult {
        if (elapsedMs < MIN_ELAPSED_MS) return ok(s)
        val counted = elapsedMs.coerceAtMost(MAX_HOURS * 3_600_000L)
        val ticks = (counted / 1000.0 / SECONDS_PER_TICK).toLong()
        val effective = ticks * EFFICIENCY
        val sect = Modifiers.sect(s)

        // Kamienie: podstawa, terytoria, dyplomaci – jak w IncomePhase/DiscipleWorkPhase.
        val territoryStones = s.territories
            .filter { it.controllingSectId == "player" && it.resourceType.contains("Kamienie") }
            .sumOf { it.yieldAmountPerCycle }
        val diplomats = s.disciples.count { it.role == DiscipleRole.DIPLOMACY }
        val perTick = (2 * s.disciples.size + territoryStones + 5 * diplomats) * sect.factor(Stat.STONE_INCOME)
        val stones = (perTick * effective).toInt()

        val days = (ticks / TICKS_PER_DAY).toInt()
        val upkeep = EdictCatalog.dailyUpkeep(s) * days

        // Qi: wartość oczekiwana medytacji, z olśnieniem; stop na 99%.
        val qiGained = mutableMapOf<String, Int>()
        val disciples = s.disciples.map { d ->
            if (d.role != DiscipleRole.MEDITATING || d.realmProgress >= 99) return@map d
            val mods = Modifiers.disciple(sect, d)
            val perTickQi = CultivationRules.meditationGain(d, mods) + 12 * CultivationRules.epiphanyChance(d, mods) / 100.0
            val newProgress = (d.realmProgress + perTickQi * effective).toInt().coerceAtMost(99)
            qiGained[d.id] = newProgress - d.realmProgress
            d.copy(realmProgress = newProgress)
        }

        // Zbiory: liczba udanych zbiorów jako wartość oczekiwana, rodzaj losowany wg wag.
        val rng = ctx.rng("offline")
        val items = mutableMapOf<String, Int>()
        val explored = s.regions.filter { it.explorationProgress > 0 }.map { it.id }.toSet()
        val gatherChance = 0.5 * sect.factor(Stat.GATHER_YIELD)
        fun gather(role: DiscipleRole, pool: List<com.example.game.content.ResourceDef>) {
            val workers = s.disciples.count { it.role == role }
            val picks = (workers * gatherChance * effective).toInt().coerceAtMost(MAX_PICKS)
            val available = ResourceCatalog.gatherable(pool, explored)
            repeat(picks) { ResourceCatalog.pickWeighted(available, rng)?.let { items.merge(it.name, 1, Int::plus) } }
        }
        gather(DiscipleRole.HERB_GATHERING, ResourceCatalog.herbs)
        gather(DiscipleRole.MINING, ResourceCatalog.minerals)
        s.territories.filter { it.controllingSectId == "player" }.forEach { t ->
            val amount = (t.yieldAmountPerCycle * effective * sect.factor(Stat.GATHER_YIELD)).toInt()
            val item = when {
                t.resourceType.contains("Kamienie") -> null
                t.resourceType.contains("Zioła") -> "Trawa Wiecznej Wiosny"
                t.resourceType.contains("Minerały") -> "Gwiezdne Żelazo"
                t.resourceType.contains("Płomienia") -> GameContent.FLAME_CRYSTAL
                else -> t.resourceType
            }
            if (item != null && amount > 0) items.merge(item, amount, Int::plus)
        }

        val inventory = s.inventory.toMutableMap()
        items.forEach { (k, v) -> inventory.merge(k, v, Int::plus) }

        val report = Report(
            elapsedMs = elapsedMs,
            countedMs = counted,
            ticks = ticks,
            stones = stones,
            upkeep = upkeep,
            days = days,
            qiGained = qiGained,
            readyForBreakthrough = disciples.count { it.realmProgress >= 99 },
            items = items
        )
        return ok(
            s.copy(
                spiritStones = (s.spiritStones + stones - upkeep).coerceAtLeast(0),
                currentDay = s.currentDay + days,
                disciples = disciples,
                inventory = inventory.toMap()
            ),
            null,
            GameEvent.OfflineSummary(report)
        )
    }

    /** Górny limit losowań rodzaju zbiorów (wydajność); powyżej tej liczby i tak wychodzi rozkład wag. */
    private const val MAX_PICKS = 5_000
}
