package com.example.game.engine

import com.example.game.content.ResourceCatalog
import com.example.model.*

/**
 * Dane robocze jednego cyklu pętli gry. Fazy czytają stan z początku cyklu ([before])
 * i budują nowy w polach roboczych – dokładnie tak jak dawna pętla w ViewModelu.
 */
class TickFrame(val before: GameState, val ctx: EngineContext) {
    var tick: Long = before.tick + 1
    var timeOfDay: TimeOfDay = before.timeOfDay
    var day: Int = before.currentDay
    var stonesGained: Int = 0
    var reputationGained: Int = 0
    val inventory: MutableMap<String, Int> = before.inventory.toMutableMap()
    var disciples: List<Disciple> = before.disciples
    var activeEventId: String? = before.activeEventId
    var recentEventIds: List<String> = before.recentEventIds
    var notice: String? = null
    val events = mutableListOf<GameEvent>()

    fun add(item: String, amount: Int) {
        inventory[item] = (inventory[item] ?: 0) + amount
    }

    fun toResult(): StepResult {
        val state = before.copy(
            tick = tick,
            currentDay = day,
            timeOfDay = timeOfDay,
            spiritStones = before.spiritStones + stonesGained,
            sectReputation = before.sectReputation + reputationGained,
            inventory = inventory.toMap(),
            disciples = disciples,
            activeEventId = activeEventId,
            recentEventIds = recentEventIds
        )
        notice?.let { events += GameEvent.Notice(it) }
        return StepResult(state, events.toList())
    }
}

/** Faza cyklu gry. Kolejność faz jest jawna i stała – patrz [GameEngine.TICK_PHASES]. */
fun interface TickPhase {
    fun run(frame: TickFrame)
}

/** 1. Pora dnia i dzień. */
object TimePhase : TickPhase {
    override fun run(frame: TickFrame) {
        val rng = frame.ctx.rng("time")
        frame.timeOfDay = when (frame.before.timeOfDay) {
            TimeOfDay.DAWN -> TimeOfDay.DAY
            TimeOfDay.DAY -> TimeOfDay.DUSK
            TimeOfDay.DUSK -> TimeOfDay.NIGHT
            TimeOfDay.NIGHT -> if (rng.nextInt(10) < 3) TimeOfDay.FULL_MOON else TimeOfDay.DAWN
            TimeOfDay.FULL_MOON -> TimeOfDay.DAWN
        }
        if (frame.timeOfDay == TimeOfDay.DAWN) frame.day = frame.before.currentDay + 1
    }
}

/** 2. Dochód: podstawa za uczniów i plony terytoriów. */
object IncomePhase : TickPhase {
    override fun run(frame: TickFrame) {
        val s = frame.before
        frame.stonesGained += 2 * s.disciples.size
        s.territories.filter { it.controllingSectId == "player" }.forEach { terr ->
            when {
                terr.resourceType.contains("Kamienie") -> frame.stonesGained += terr.yieldAmountPerCycle
                terr.resourceType.contains("Zioła") -> frame.add("Trawa Wiecznej Wiosny", terr.yieldAmountPerCycle)
                terr.resourceType.contains("Minerały") -> frame.add("Gwiezdne Żelazo", terr.yieldAmountPerCycle)
                terr.resourceType.contains("Płomienia") -> frame.add(GameContent.FLAME_CRYSTAL, terr.yieldAmountPerCycle)
                else -> frame.add(terr.resourceType, terr.yieldAmountPerCycle)
            }
        }
    }
}

/** 3. Praca uczniów wg roli: medytacja, zbieranie ziół, kopalnia, trening, dyplomacja. */
object DiscipleWorkPhase : TickPhase {
    override fun run(frame: TickFrame) {
        val s = frame.before
        val rng = frame.ctx.rng("work")
        val exploredRegionIds = s.regions.filter { it.explorationProgress > 0 }.map { it.id }.toSet()
        val sharedBonus = CultivationRules.sectBonusPercent(s)
        val decreeBonus = CultivationRules.decreeBonusPercent(s)

        frame.disciples = frame.disciples.map { disciple ->
            when (disciple.role) {
                DiscipleRole.MEDITATING -> {
                    val gain = CultivationRules.meditationGain(disciple, decreeBonus + sharedBonus)
                    var bonusDao = 0
                    var epiphanyGain = 0
                    if (rng.nextInt(100) < (disciple.karmicLuck / 25).coerceIn(3, 20)) {
                        bonusDao = 1
                        epiphanyGain = 12
                    }
                    disciple.copy(
                        realmProgress = (disciple.realmProgress + gain + epiphanyGain).coerceAtMost(100),
                        daoPoints = disciple.daoPoints + bonusDao
                    )
                }
                DiscipleRole.HERB_GATHERING -> {
                    if (rng.nextBoolean()) {
                        ResourceCatalog.pickWeighted(ResourceCatalog.gatherable(ResourceCatalog.herbs, exploredRegionIds), rng)
                            ?.let { frame.add(it.name, 1) }
                    }
                    disciple
                }
                DiscipleRole.MINING -> {
                    if (rng.nextBoolean()) {
                        ResourceCatalog.pickWeighted(ResourceCatalog.gatherable(ResourceCatalog.minerals, exploredRegionIds), rng)
                            ?.let { frame.add(it.name, 1) }
                    }
                    disciple
                }
                DiscipleRole.COMBAT_TRAINING ->
                    if (rng.nextInt(100) < 40) disciple.copy(daoPoints = disciple.daoPoints + 1) else disciple
                DiscipleRole.DIPLOMACY -> {
                    frame.stonesGained += 5
                    disciple
                }
                DiscipleRole.SECT_GUARD -> disciple
            }
        }
    }
}

/** 4. Zdarzenie losowe: co 16 ticków 65% szansy, jeśli żadne nie jest aktywne. */
object RandomEventPhase : TickPhase {
    const val EVERY_TICKS = 16
    const val CHANCE_PERCENT = 65
    const val MEMORY = 12

    override fun run(frame: TickFrame) {
        if (frame.activeEventId != null || frame.tick % EVERY_TICKS != 0L) return
        val rng = frame.ctx.rng("events")
        if (rng.nextInt(100) >= CHANCE_PERCENT) return
        val eligible = GameContent.events.filter {
            it.isEligible(frame.day, frame.before.sectReputation, frame.timeOfDay) && it.id !in frame.recentEventIds
        }
        val picked = eligible.randomOrNull(rng) ?: return
        frame.activeEventId = picked.id
        frame.recentEventIds = (frame.recentEventIds + picked.id).takeLast(MEMORY)
    }
}

/** 5. Automatyczny przełom uczniów ze 100% Qi. */
object AutoBreakthroughPhase : TickPhase {
    override fun run(frame: TickFrame) {
        val s = frame.before
        if (!s.autoBreakthroughEnabled) return
        val rng = frame.ctx.rng("breakthrough")
        frame.disciples = frame.disciples.map { disciple ->
            if (disciple.realmProgress < 100) return@map disciple
            val r = CultivationRules.breakthrough(disciple, s.autoUsePillsForBreakthrough, frame.inventory, frame.timeOfDay, rng)
            if (frame.notice == null) frame.notice = r.message
            frame.reputationGained += r.reputation
            frame.events += GameEvent.Breakthrough(disciple.id, r.success)
            r.disciple
        }
    }
}
