package com.example.game.engine

import com.example.game.engine.DiscipleRules.has

/**
 * Etap 2 – dzienne życie sekty (raz na dobę, o świcie):
 * pensje → jedzenie → lojalność i jej skutki → morale → egzamin wstępny → turniej.
 */
object SectLifePhase : TickPhase {

    override fun run(frame: TickFrame) {
        val s = frame.before
        if (!s.e2Initialized || frame.day == s.currentDay) return
        val day = frame.day
        val rng = frame.ctx.rng("sect-life")
        var morale = s.morale
        val notices = mutableListOf<String>()

        // 1. Pensje
        val salaries = frame.disciples.sumOf { DiscipleRules.salary(it) }
        val canPay = s.spiritStones - frame.upkeepPaid >= salaries
        if (canPay) frame.upkeepPaid += salaries else notices += "Skarbiec nie wystarcza na pensje – uczniowie tracą zaufanie."

        // 2. Jedzenie
        val ration = frame.disciples.size
        val food = (frame.inventory[DiscipleRules.FOOD] ?: 0) + DiscipleRules.dailyFoodProduction(ration)
        var hungry = false
        if (food >= ration) {
            frame.inventory[DiscipleRules.FOOD] = food - ration
        } else {
            frame.inventory[DiscipleRules.FOOD] = 0
            hungry = true
            morale -= DiscipleRules.HUNGER_MORALE
            notices += "Zabrakło ryżu duchowego – głód w sekcie (−${DiscipleRules.HUNGER_MORALE} morale, −20% Qi dziś)."
        }

        // 3. Lojalność i jej skutki
        val deserters = mutableListOf<String>()
        frame.disciples = frame.disciples.mapNotNull { d0 ->
            var d = d0
            var loyalty = d.loyalty
            if (!canPay) loyalty -= 5
            if (hungry) loyalty -= 1
            if (morale < 30) loyalty -= 1
            if (d.has("hotheaded") && day % 5 == 0) loyalty -= 1

            // Ambitny bez awansu
            val due = DiscipleRules.canPromote(s, d)
            val dueSince = if (due) d.promotionDueSinceDay ?: day else null
            if (d.has("ambitious") && dueSince != null && day - dueSince >= 10) loyalty -= 1

            // Dobrze opłacana, syta sekta z niezłym morale odbudowuje zaufanie (do 70).
            if (canPay && !hungry && morale >= 40 && loyalty < 70) loyalty += 1
            if (d.has("loyal")) loyalty = loyalty.coerceAtLeast(60)
            loyalty = loyalty.coerceIn(0, 100)
            d = d.copy(loyalty = loyalty, promotionDueSinceDay = dueSince)

            when {
                loyalty < 15 && !d.has("loyal") && rng.nextInt(100) < 10 -> {
                    deserters += d.name
                    d.equippedArtifact?.let { frame.returnedArtifacts += it }
                    return@mapNotNull null
                }
                d.has("greedy") && loyalty < 30 && rng.nextInt(100) < 10 -> {
                    val stealable = frame.inventory.filter { it.value > 0 && it.key != DiscipleRules.FOOD }.keys.sorted()
                    stealable.randomOrNull(rng)?.let { item ->
                        val lost = ((frame.inventory[item] ?: 0) / 10).coerceAtLeast(1)
                        frame.inventory[item] = (frame.inventory[item] ?: 0) - lost
                        notices += "Z magazynu zniknęło $lost× $item. Podejrzenia padają na ${d.name}."
                    }
                }
                "traitor" in d.hiddenTraitIds && loyalty < 40 && rng.nextInt(100) < 5 -> {
                    frame.reputationGained -= 50
                    morale -= 10
                    notices += "Zdrada! ${d.name} sprzedał sekrety sekty rywalom (−50 reputacji)."
                    d = d.copy(traitIds = d.traitIds + "traitor", hiddenTraitIds = d.hiddenTraitIds - "traitor")
                }
            }
            d
        }
        if (deserters.isNotEmpty()) {
            morale -= 5 * deserters.size
            notices += "Sektę opuścili: ${deserters.joinToString()}."
        }

        // 4. Morale dąży do 50; charyzmatyczni podnoszą je codziennie.
        morale += when {
            morale > 50 -> -1
            morale < 50 -> 1
            else -> 0
        }
        morale += frame.disciples.count { it.has("charismatic") }.coerceAtMost(3)
        frame.morale = morale.coerceIn(0, 100)
        if (hungry) frame.hungryOnDay = day

        // 5. Egzamin wstępny: otwarcie co 30 dni, zamknięcie po 5.
        if (s.examOpenUntilDay in 1 until day && frame.recruits.isNotEmpty()) {
            frame.recruits = emptyList()
            notices += "Kandydaci z egzaminu wstępnego rozeszli się."
        }
        if (day % DiscipleRules.EXAM_EVERY_DAYS == 0) {
            val extra = frame.sectMods.flat(Stat.RECRUIT_POOL).toInt()
            frame.recruits = SectCommands.recruitPool(frame.ctx.rng("exam"), extra)
                .map { GameEngine.prepareCandidate(it, frame.ctx.rng("exam-${it.id}"), s) }
            frame.examOpenUntilDay = day + DiscipleRules.EXAM_OPEN_DAYS - 1
            frame.admittedInExam = 0
            notices += "Egzamin wstępny! Przed bramą czeka ${frame.recruits.size} kandydatów (${DiscipleRules.EXAM_OPEN_DAYS} dni)."
        }

        // 6. Turniej wewnętrzny co 60 dni.
        if (day % DiscipleRules.TOURNAMENT_EVERY_DAYS == 0 && frame.disciples.size >= 2) {
            Tournament.run(frame, rng)?.let { notices += it }
        }

        notices.lastOrNull()?.let { frame.notice = frame.notice ?: it }
    }
}

/** Turniej wewnętrzny: drabinka do 8 najsilniejszych uczniów. */
object Tournament {
    fun run(frame: TickFrame, rng: kotlin.random.Random): String? {
        var bracket = frame.disciples.sortedByDescending { it.combatPower }.take(8).map { it.id }
        if (bracket.size < 2) return null
        val power = frame.disciples.associate { it.id to it.combatPower.toDouble() }
        val losers = mutableSetOf<String>()
        while (bracket.size > 1) {
            bracket = bracket.chunked(2).map { pair ->
                if (pair.size == 1) return@map pair[0]
                val a = (power[pair[0]] ?: 1.0) * (0.8 + rng.nextDouble() * 0.4)
                val b = (power[pair[1]] ?: 1.0) * (0.8 + rng.nextDouble() * 0.4)
                val (win, lose) = if (a >= b) pair[0] to pair[1] else pair[1] to pair[0]
                losers += lose
                win
            }
        }
        val winnerId = bracket.single()
        var prize: String? = null
        frame.disciples = frame.disciples.map { d ->
            when {
                d.id == winnerId -> {
                    // Nagroda: zwój metody o stopień wyżej na ścieżce zwycięzcy.
                    val path = d.path
                    if (frame.before.e3Initialized && path != null) {
                        val grade = com.example.model.MethodGrade.entries.getOrNull(Realms.method(d.methodId).grade.ordinal + 1)
                        if (grade != null) prize = Realms.methodOf(path, grade).scrollItem
                    }
                    d.copy(loyalty = (d.loyalty + 20).coerceAtMost(100))
                }
                d.id in losers && d.has("hotheaded") -> d.copy(loyalty = (d.loyalty - 5).coerceAtLeast(0))
                else -> d
            }
        }
        frame.reputationGained += 30
        frame.morale = (frame.morale + 15).coerceAtMost(100)
        prize?.let { frame.add(it, 1) }
        val name = frame.disciples.find { it.id == winnerId }?.name ?: "?"
        return "Turniej Uczniów wygrał $name! (+30 reputacji, +15 morale" + (prize?.let { ", nagroda: $it" } ?: "") + ")"
    }
}
