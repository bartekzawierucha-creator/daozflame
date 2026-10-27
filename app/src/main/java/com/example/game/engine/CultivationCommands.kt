package com.example.game.engine

import com.example.model.*

/** Etap 3: decyzje kultywacyjne gracza. */
object CultivationCommands {

    const val MAX_LAWS = 2
    const val SECLUSION_MIN_DAYS = 5
    const val SECLUSION_MAX_DAYS = 60

    private fun GameState.update(id: String, f: (Disciple) -> Disciple) =
        copy(disciples = disciples.map { if (it.id == id) f(it) else it })

    /** Wybór ścieżki – od Fundamentów. Zmiana ścieżki kosztuje 1 etap postępu i Pigułkę Ciszy Umysłu. */
    fun choosePath(s: GameState, id: String, path: CultivationPath): StepResult {
        val d = s.disciple(id) ?: return ok(s)
        val major = d.majorRealm ?: return ok(s)
        if (major.ordinal < MajorRealm.FOUNDATION.ordinal) return fail(s, "Ścieżkę wybiera się przy wejściu w Fundamenty.")
        if (d.path == path) return ok(s)
        if (path == CultivationPath.HEART && s.karma < 0) return fail(s, "Ścieżka Serca wymaga karmy sekty ≥ 0.")
        var inventory = s.inventory
        if (d.path != null) {
            val pill = "Pigułka Ciszy Umysłu"
            val n = s.inventory[pill] ?: 0
            if (n < 1) return fail(s, "Zmiana ścieżki wymaga Pigułki Ciszy Umysłu.")
            inventory = inventory + (pill to n - 1)
        }
        val method = Realms.method(d.methodId)
        val newMethod = if (method.path == null || method.path != path) Realms.methodOf(path, MethodGrade.YELLOW) else method
        val updated = Realms.sync(
            d.copy(
                path = path,
                methodId = if (method.grade.ordinal > newMethod.grade.ordinal && method.path == path) method.id else newMethod.id,
                stageQi = if (d.path != null) 0.0 else d.stageQi,
                maxHp = if (path == CultivationPath.BODY && d.path != CultivationPath.BODY) d.maxHp * 2 else d.maxHp,
                currentHp = if (path == CultivationPath.BODY && d.path != CultivationPath.BODY) d.currentHp * 2 else d.currentHp
            )
        )
        return ok(s.update(id) { updated }.copy(inventory = inventory), "${d.name} obiera Ścieżkę ${path.polishName}.")
    }

    /** Nauka metody ze zwoju (zwój znika). Metoda musi pasować do ścieżki ucznia. */
    fun learnMethod(s: GameState, id: String, scrollItem: String): StepResult {
        val d = s.disciple(id) ?: return ok(s)
        val method = Realms.methodForScroll(scrollItem) ?: return fail(s, "To nie jest zwój metody.")
        if ((s.inventory[scrollItem] ?: 0) < 1) return fail(s, "Brak zwoju w magazynie.")
        if (method.path != null && method.path != d.path) return fail(s, "${method.name} należy do Ścieżki ${method.path.polishName}.")
        if (method.grade.ordinal <= Realms.method(d.methodId).grade.ordinal) return fail(s, "${d.name} zna już równie dobrą metodę.")
        return ok(
            s.update(id) { it.copy(methodId = method.id) }.copy(inventory = s.inventory + (scrollItem to (s.inventory[scrollItem] ?: 0) - 1)),
            "${d.name} zgłębia ${method.name} (stopień ${method.grade.polishName})."
        )
    }

    /** Wybór Prawa Dao do zgłębiania – od Narodzin Duszy, maks. 2. */
    fun chooseLaw(s: GameState, id: String, law: DaoLaw): StepResult {
        val d = s.disciple(id) ?: return ok(s)
        if ((d.majorRealm?.ordinal ?: 0) < MajorRealm.NASCENT_SOUL.ordinal) return fail(s, "Prawa Dao zgłębia się od Narodzin Duszy.")
        if (law.polishName in d.laws) return ok(s)
        if (d.laws.size >= MAX_LAWS) return fail(s, "Uczeń zgłębia już $MAX_LAWS Prawa.")
        return ok(s.update(id) { it.copy(laws = it.laws + (law.polishName to 0.0)) }, "${d.name} zaczyna zgłębiać ${law.polishName}.")
    }

    fun seclude(s: GameState, id: String, days: Int): StepResult {
        val d = s.disciple(id) ?: return ok(s)
        if (d.secludedUntilDay >= s.currentDay) return fail(s, "${d.name} już przebywa w odosobnieniu.")
        val n = days.coerceIn(SECLUSION_MIN_DAYS, SECLUSION_MAX_DAYS)
        return ok(
            s.update(id) { it.copy(secludedUntilDay = s.currentDay + n) },
            "${d.name} zamyka się w jaskini odosobnienia na $n dni (Qi ×3)."
        )
    }

    /** Przerwanie odosobnienia przed czasem grozi odchyleniem Qi. */
    fun endSeclusion(s: GameState, id: String): StepResult {
        val d = s.disciple(id) ?: return ok(s)
        if (d.secludedUntilDay < s.currentDay) return ok(s)
        return ok(
            s.update(id) { it.copy(secludedUntilDay = 0, injuredUntilDay = s.currentDay + 20) },
            "Przerwano odosobnienie ${d.name} – odchylenie Qi (rany na 20 dni)."
        )
    }

    fun awakenBloodline(s: GameState, id: String): StepResult {
        val d = s.disciple(id) ?: return ok(s)
        val level = d.bloodlineAwakening + 1
        val need = Realms.awakeningRealm(level) ?: return fail(s, "Krew ${d.name} jest już w pełni przebudzona.")
        if ((d.majorRealm?.ordinal ?: 0) < need.ordinal) return fail(s, "Przebudzenie $level. stopnia wymaga ${need.polishName}.")
        val catalyst = Realms.bloodlineCatalyst(d.bloodline)
        val have = s.inventory[catalyst] ?: 0
        if (have < level) return fail(s, "Przebudzenie wymaga $level× $catalyst (masz $have).")
        return ok(
            s.update(id) { it.copy(bloodlineAwakening = level) }.copy(inventory = s.inventory + (catalyst to have - level)),
            "${d.bloodline.polishName} w ${d.name} budzi się na $level. stopień!"
        )
    }

    /** Wniebowstąpienie: uczeń na szczycie Mahajany opuszcza sektę, zostawiając stałą premię. */
    fun ascend(s: GameState, id: String, ctx: EngineContext): StepResult {
        val d = s.disciple(id) ?: return ok(s)
        if (d.majorRealm != MajorRealm.MAHAYANA || !Realms.atPeak(d) || !CultivationSystem.ready(d)) {
            return fail(s, "Wniebowstąpić może tylko uczeń na szczycie Mahajany z pełnym Qi.")
        }
        val inv = s.inventory.toMutableMap()
        val trial = CultivationSystem.tribulation(d, MajorRealm.HEAVENLY_IMMORTAL, inv, ctx.rng("ascension"))
        if (!trial.survived) {
            return ok(
                s.update(id) { Realms.sync(it.copy(stageQi = Realms.required(it) * 0.6, injuredUntilDay = s.currentDay + 30)) }.copy(inventory = inv),
                "Próba Dziewięciu Niebios odrzuciła ${d.name}. Rany na 30 dni."
            )
        }
        return ok(
            s.copy(
                disciples = s.disciples.filter { it.id != id },
                ascendedNames = s.ascendedNames + d.name,
                sectReputation = s.sectReputation + 100,
                inventory = inv,
                vaultArtifacts = s.vaultArtifacts + listOfNotNull(d.equippedArtifact)
            ),
            "✨ ${d.name} wstępuje do Wyższego Świata! Sekta zyskuje Dziedzictwo Wniebowstąpienia (+5% Qi na zawsze)."
        )
    }
}
