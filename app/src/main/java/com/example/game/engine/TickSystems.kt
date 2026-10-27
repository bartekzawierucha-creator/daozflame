package com.example.game.engine

import com.example.game.content.ResourceCatalog
import com.example.model.*

/**
 * Dane robocze jednego cyklu pętli gry. Fazy czytają stan z początku cyklu ([before])
 * i budują nowy w polach roboczych – dokładnie tak jak dawna pętla w ViewModelu.
 */
class TickFrame(val before: GameState, val ctx: EngineContext) {
    /** Premie sekty liczone raz na cykl. */
    val sectMods: ModifierStack = Modifiers.sect(before)
    var tick: Long = before.tick + 1
    var timeOfDay: TimeOfDay = before.timeOfDay
    var phaseTick: Int = before.phaseTick
    var day: Int = before.currentDay
    var stonesGained: Int = 0
    var reputationGained: Int = 0
    val inventory: MutableMap<String, Int> = before.inventory.toMutableMap()
    var disciples: List<Disciple> = before.disciples
    var activeEventId: String? = before.activeEventId
    var recentEventIds: List<String> = before.recentEventIds
    var notice: String? = null
    val events = mutableListOf<GameEvent>()

    /** Plon po premiach zbiorów. */
    fun gather(amount: Int): Int = (amount * sectMods.factor(Stat.GATHER_YIELD)).toInt().coerceAtLeast(0)

    fun add(item: String, amount: Int) {
        inventory[item] = (inventory[item] ?: 0) + amount
    }

    /** Koszty pobierane w tym cyklu (utrzymanie edyktów, pensje) – nie podlegają premiom dochodu. */
    var upkeepPaid: Int = 0
    var decrees = before.sectLeader.activeDecrees
    var morale: Int = before.morale
    var hungryOnDay: Int = before.hungryOnDay
    var recruits = before.availableRecruits
    var examOpenUntilDay: Int = before.examOpenUntilDay
    var admittedInExam: Int = before.admittedInExam
    var lastTournamentDay: Int = before.lastTournamentDay
    val returnedArtifacts = mutableListOf<com.example.model.Artifact>()

    fun toResult(): StepResult {
        val income = (stonesGained * sectMods.factor(Stat.STONE_INCOME)).toInt()
        val rep = if (reputationGained > 0) (reputationGained * sectMods.factor(Stat.REPUTATION_GAIN)).toInt() else reputationGained
        val state = before.copy(
            tick = tick,
            phaseTick = phaseTick,
            currentDay = day,
            timeOfDay = timeOfDay,
            spiritStones = (before.spiritStones + income - upkeepPaid).coerceAtLeast(0),
            sectReputation = before.sectReputation + rep,
            sectLeader = before.sectLeader.copy(activeDecrees = decrees),
            morale = morale,
            hungryOnDay = hungryOnDay,
            availableRecruits = recruits,
            examOpenUntilDay = examOpenUntilDay,
            admittedInExam = admittedInExam,
            lastTournamentDay = lastTournamentDay,
            vaultArtifacts = before.vaultArtifacts + returnedArtifacts,
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

/**
 * 1. Pora dnia i dzień. Każda pora trwa [TICKS_PER_PHASE] cykli (6 × 1,6 s ≈ 10 s),
 * więc doba to ok. 40 s, a rok gry (120 dni) ok. 80 minut gry.
 */
object TimePhase : TickPhase {
    const val TICKS_PER_PHASE = 6
    /** Średnia długość doby w cyklach (pełnia dokłada noc w 30% dni). */
    const val TICKS_PER_DAY = TICKS_PER_PHASE * 4.3

    override fun run(frame: TickFrame) {
        if (frame.before.phaseTick + 1 < TICKS_PER_PHASE) {
            frame.phaseTick = frame.before.phaseTick + 1
            return
        }
        frame.phaseTick = 0
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

/** 1b. Utrzymanie edyktów – płatne o świcie. Brak kamieni = edykty zawieszone. */
object UpkeepPhase : TickPhase {
    override fun run(frame: TickFrame) {
        if (frame.day == frame.before.currentDay) return
        val upkeep = EdictCatalog.dailyUpkeep(frame.before)
        if (upkeep <= 0) return
        if (frame.before.spiritStones >= upkeep) {
            frame.upkeepPaid += upkeep
        } else {
            frame.decrees = frame.decrees.map { d -> if ((EdictCatalog.get(d.id)?.upkeepStones ?: 0) > 0) d.copy(isActive = false) else d }
            frame.notice = "Skarbiec pusty – edykty z kosztem utrzymania zostały zawieszone."
        }
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
                terr.resourceType.contains("Zioła") -> frame.add("Trawa Wiecznej Wiosny", frame.gather(terr.yieldAmountPerCycle))
                terr.resourceType.contains("Minerały") -> frame.add("Gwiezdne Żelazo", frame.gather(terr.yieldAmountPerCycle))
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
        fun gatherChance(d: com.example.model.Disciple) = 0.5 * Modifiers.disciple(frame.sectMods, d).factor(Stat.GATHER_YIELD)

        val e3 = s.e3Initialized
        frame.disciples = frame.disciples.map { disciple ->
            val secluded = e3 && disciple.secludedUntilDay >= frame.day && disciple.secludedUntilDay > 0
            val bodyTraining = e3 && disciple.path == com.example.model.CultivationPath.BODY && disciple.role == DiscipleRole.COMBAT_TRAINING
            if (secluded || bodyTraining) return@map Meditation.meditate(frame, disciple, rng, if (bodyTraining) 0.8 else 1.0)
            when (disciple.role) {
                DiscipleRole.MEDITATING -> {
                    if (e3) return@map Meditation.meditate(frame, disciple, rng, 1.0)
                    val mods = Modifiers.disciple(frame.sectMods, disciple)
                    val gain = CultivationRules.meditationGain(disciple, mods)
                    var bonusDao = 0
                    var epiphanyGain = 0
                    if (rng.nextInt(100) < CultivationRules.epiphanyChance(disciple, mods)) {
                        bonusDao = 1
                        epiphanyGain = 12
                    }
                    disciple.copy(
                        realmProgress = (disciple.realmProgress + gain + epiphanyGain).coerceAtMost(100),
                        daoPoints = disciple.daoPoints + bonusDao
                    )
                }
                DiscipleRole.HERB_GATHERING -> {
                    if (rng.nextDouble() < gatherChance(disciple)) {
                        ResourceCatalog.pickWeighted(ResourceCatalog.gatherable(ResourceCatalog.herbs, exploredRegionIds), rng)
                            ?.let { frame.add(it.name, 1) }
                        // Pola ryżu duchowego przy zbieraniu ziół – źródło jedzenia sekty.
                        if (frame.before.e2Initialized) frame.add(DiscipleRules.FOOD, 2)
                    }
                    disciple
                }
                DiscipleRole.MINING -> {
                    if (rng.nextDouble() < gatherChance(disciple)) {
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
        if (s.e3Initialized) return runV2(frame)
        if (!s.autoBreakthroughEnabled) return
        val rng = frame.ctx.rng("breakthrough")
        frame.disciples = frame.disciples.map { disciple ->
            if (disciple.realmProgress < 100) return@map disciple
            val r = CultivationRules.breakthrough(
                disciple, s.autoUsePillsForBreakthrough, frame.inventory, frame.timeOfDay, rng,
                frame.sectMods.flat(Stat.BREAKTHROUGH_CHANCE).toInt()
            )
            if (frame.notice == null) frame.notice = r.message
            frame.reputationGained += r.reputation
            frame.events += GameEvent.Breakthrough(disciple.id, r.success)
            r.disciple
        }
    }

    /** Etap 3: małe przełomy zawsze; wielkie bez tribulacji tylko z włączonym automatem. */
    private fun runV2(frame: TickFrame) {
        val s = frame.before
        val rng = frame.ctx.rng("breakthrough")
        val state = s.copy(currentDay = frame.day, timeOfDay = frame.timeOfDay)
        frame.disciples = frame.disciples.map { d ->
            if (!CultivationSystem.ready(d)) return@map d
            val major = d.majorRealm ?: return@map d
            if (d.stage >= major.stages && !s.autoBreakthroughEnabled) return@map d
            val r = CultivationSystem.advance(state, d, s.autoUsePillsForBreakthrough, frame.inventory, rng, player = false)
            frame.reputationGained += r.reputation
            if (r.attempted) {
                if (frame.notice == null) frame.notice = r.message
                frame.events += GameEvent.Breakthrough(d.id, r.majorSuccess)
            }
            r.disciple ?: d
        }
    }
}

