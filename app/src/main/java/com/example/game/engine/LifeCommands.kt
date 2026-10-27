package com.example.game.engine

import com.example.game.engine.DiscipleRules.has

/** Etap 2: decyzje patriarchy wobec uczniów i zaopatrzenia sekty. */
object LifeCommands {

    private fun GameState.update(id: String, f: (com.example.model.Disciple) -> com.example.model.Disciple) =
        copy(disciples = disciples.map { if (it.id == id) f(it) else it })

    /** Awans o jedną rangę. Zazdrośni uczniowie niższej rangi tracą lojalność. */
    fun promote(s: GameState, id: String): StepResult {
        val d = s.disciple(id) ?: return ok(s)
        val next = DiscipleRules.nextRank(d) ?: return fail(s, "${d.name} ma już najwyższą rangę.")
        if (d.realm.ordinal < next.requiredRealm.ordinal) {
            return fail(s, "${next.polishName} wymaga królestwa ${next.requiredRealm.polishName}.")
        }
        val limit = DiscipleRules.rankLimit(s, next)
        if (s.disciples.count { it.rank == next } >= limit) {
            return fail(s, "Brak miejsc dla rangi ${next.polishName} (limit $limit).")
        }
        val state = s.copy(disciples = s.disciples.map {
            when {
                it.id == id -> it.copy(rank = next, loyalty = (it.loyalty + 15).coerceAtMost(100), promotionDueSinceDay = null)
                it.has("jealous") && it.rank.ordinal <= next.ordinal -> it.copy(loyalty = (it.loyalty - 10).coerceAtLeast(0))
                else -> it
            }
        })
        return ok(state, "${d.name} awansuje na: ${next.polishName}! Pensja: ${next.salaryPerDay} kamieni dziennie.")
    }

    /** Sala Kar: surowa kara podnosi morale reszty, ale ukarany traci zaufanie. */
    fun punish(s: GameState, id: String): StepResult {
        val d = s.disciple(id) ?: return ok(s)
        return ok(
            s.update(id) { it.copy(loyalty = (it.loyalty - 50).coerceAtLeast(if (it.has("loyal")) 60 else 0)) }
                .copy(morale = (s.morale + 10).coerceAtMost(100)),
            "${d.name} został ukarany w Sali Kar. Dyscyplina wzrosła (+10 morale)."
        )
    }

    /** Ułaskawienie: uczeń wdzięczny, reszta widzi pobłażliwość. */
    fun pardon(s: GameState, id: String): StepResult {
        val d = s.disciple(id) ?: return ok(s)
        return ok(
            s.update(id) { it.copy(loyalty = (it.loyalty + 10).coerceAtMost(100)) }
                .copy(morale = (s.morale - 5).coerceAtLeast(0)),
            "Ułaskawiono ${d.name} (+10 lojalności, −5 morale)."
        )
    }

    /** Kamień Testu Korzenia: ujawnia ukryte cechy kandydata. */
    fun testCandidate(s: GameState, id: String): StepResult {
        val c = s.availableRecruits.find { it.id == id } ?: return ok(s)
        if (c.hiddenTraitIds.isEmpty()) return fail(s, "Ten kandydat nie ma już przed tobą tajemnic.")
        if (s.spiritStones < DiscipleRules.ROOT_TEST_COST) return fail(s, "Test Korzenia kosztuje ${DiscipleRules.ROOT_TEST_COST} kamieni.")
        val revealed = c.copy(traitIds = c.traitIds + c.hiddenTraitIds, hiddenTraitIds = emptyList())
        val names = c.hiddenTraitIds.mapNotNull { DiscipleRules.trait(it)?.name }.joinToString()
        return ok(
            s.copy(
                spiritStones = s.spiritStones - DiscipleRules.ROOT_TEST_COST,
                availableRecruits = s.availableRecruits.map { if (it.id == id) revealed else it }
            ),
            "Test Korzenia: ${c.name} – ukryta cecha: $names."
        )
    }

    fun buyFood(s: GameState, rations: Int): StepResult {
        if (rations <= 0) return ok(s)
        val cost = rations * DiscipleRules.FOOD_PRICE
        if (s.spiritStones < cost) return fail(s, "Zakup $rations racji kosztuje $cost kamieni.")
        return ok(
            s.copy(
                spiritStones = s.spiritStones - cost,
                inventory = s.inventory + (DiscipleRules.FOOD to (s.inventory[DiscipleRules.FOOD] ?: 0) + rations)
            ),
            "Kupiono $rations racji ryżu duchowego za $cost kamieni."
        )
    }

    /** Uczta: 3 racje na ucznia, +30 morale, raz na 10 dni. */
    fun holdFeast(s: GameState): StepResult {
        if (s.currentDay < s.nextFeastDay) return fail(s, "Następna uczta możliwa za ${s.nextFeastDay - s.currentDay} dni.")
        val need = s.disciples.size * 3
        val have = s.inventory[DiscipleRules.FOOD] ?: 0
        if (have < need) return fail(s, "Uczta wymaga $need racji ryżu (masz $have).")
        return ok(
            s.copy(
                inventory = s.inventory + (DiscipleRules.FOOD to have - need),
                morale = (s.morale + 30).coerceAtMost(100),
                nextFeastDay = s.currentDay + DiscipleRules.FEAST_COOLDOWN_DAYS
            ),
            "Wielka uczta w Sali Głównej! Morale sekty +30."
        )
    }
}
