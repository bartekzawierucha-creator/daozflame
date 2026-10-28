package com.example.game.engine

import com.example.game.engine.DiscipleRules.has
import com.example.model.Disciple
import com.example.model.MajorRealm
import com.example.model.MasterPromise

/**
 * Etap 11: narzędzia mentora. Mistrz ma ograniczone skupienie ([MAX_FOCUS] działań na dzień gry), więc musi wybrać,
 * komu poświęci czas: wspólna medytacja, pochwała przy całej sekcie albo obietnica nagrody.
 * Pochwała budzi zazdrość rywali, a złamana obietnica niszczy zaufanie.
 */
object Mentor {
    const val MAX_FOCUS = 3
    /** Etap 12: jeden punkt skupienia wraca co [FOCUS_REGEN_DAYS] dni gry (~4 min). */
    const val FOCUS_REGEN_DAYS = 5
    /** Z tym samym uczniem medytuje się najwyżej raz na tyle dni. */
    const val MEDITATION_COOLDOWN = 15
    const val MEDITATION_TICKS = 90
    const val MEDITATION_TRUST = 6
    const val PRAISE_TRUST = 4
    const val PRAISE_LOYALTY = 6
    const val JEALOUSY = 4
    const val PROMISE_TRUST = 3
    const val PROMISE_KEPT_TRUST = 15
    const val PROMISE_KEPT_LOYALTY = 10
    const val PROMISE_BROKEN_TRUST = 25
    const val PROMISE_BROKEN_LOYALTY = 15
    const val PROMISE_DAYS = 1800

    fun focusLeft(s: GameState): Int =
        (s.masterFocus + ((s.currentDay - s.focusDay).coerceAtLeast(0) / FOCUS_REGEN_DAYS)).coerceAtMost(MAX_FOCUS)

    /** Za ile dni wróci następny punkt (0 = pełne skupienie). */
    fun nextFocusIn(s: GameState): Int = if (focusLeft(s) >= MAX_FOCUS) 0 else FOCUS_REGEN_DAYS - (s.currentDay - s.focusDay).coerceAtLeast(0) % FOCUS_REGEN_DAYS

    private fun spend(s: GameState): GameState {
        val left = focusLeft(s)
        val elapsed = (s.currentDay - s.focusDay).coerceAtLeast(0)
        // Pełne skupienie: odliczanie startuje od dziś; inaczej zachowuje rozpoczęty cykl.
        val anchor = if (left >= MAX_FOCUS) s.currentDay else s.focusDay + elapsed / FOCUS_REGEN_DAYS * FOCUS_REGEN_DAYS
        return s.copy(focusDay = anchor, masterFocus = left - 1)
    }

    private fun GameState.update(id: String, f: (Disciple) -> Disciple) = copy(disciples = disciples.map { if (it.id == id) f(it) else it })
    private fun Disciple.v(m: String, f: String) = if (gender == "Kobieta") f else m
    private fun Disciple.note(day: Int, text: String) = copy(diary = (diary + "Dz. $day: $text").takeLast(Autonomy.DIARY_SIZE))

    private fun tired(s: GameState) = fail(s, "Mistrz jest wyczerpany – skupienie wróci za ${nextFocusIn(s)} dni.")

    /** Wspólna medytacja: Qi i zaufanie. Raz dziennie z tym samym uczniem. */
    fun meditate(s: GameState, id: String): StepResult {
        val d = s.disciple(id) ?: return ok(s)
        if (focusLeft(s) <= 0) return tired(s)
        if (d.meditatedWithMasterDay >= 0 && s.currentDay - d.meditatedWithMasterDay < MEDITATION_COOLDOWN) return fail(s, "${Autonomy.short(d)} medytował${d.v("", "a")} niedawno z mistrzem.")
        if (d.secludedUntilDay >= s.currentDay) return fail(s, "${Autonomy.short(d)} trwa w odosobnieniu.")
        val day = s.currentDay
        val st = spend(s).update(id) {
            val qi = if (s.e3Initialized) CultivationSystem.gainQi(it, CultivationSystem.ticksWorth(s, it, MEDITATION_TICKS))
            else it.copy(realmProgress = (it.realmProgress + 8).coerceAtMost(100))
            qi.copy(trust = (it.trust + MEDITATION_TRUST).coerceAtMost(100), meditatedWithMasterDay = day)
                .note(day, "Medytował${d.v("", "a")} z mistrzem. Qi płynęło spokojnie.")
        }
        return ok(st, "🧘 Mistrz medytuje z ${Autonomy.short(d)}. Qi krąży między nimi (+zaufanie).")
    }

    /** Pochwała przy całej sekcie: lojalność i zaufanie, ale zazdrośni i rywale to zapamiętają. */
    fun praise(s: GameState, id: String): StepResult {
        val d = s.disciple(id) ?: return ok(s)
        if (focusLeft(s) <= 0) return tired(s)
        val day = s.currentDay
        val rivals = Bonds.rivalsOf(s, id).toSet()
        val envious = s.disciples.filter { it.id != id && (it.has("jealous") || it.id in rivals) }
        var st = spend(s).update(id) {
            it.copy(trust = (it.trust + PRAISE_TRUST).coerceAtMost(100), loyalty = (it.loyalty + PRAISE_LOYALTY).coerceAtMost(100))
                .note(day, "Mistrz pochwalił mnie przy całej sekcie.")
        }
        envious.forEach { e ->
            st = st.update(e.id) { it.copy(loyalty = (it.loyalty - JEALOUSY).coerceAtLeast(0)).note(day, "Mistrz chwali ${Autonomy.short(d)}… a mnie?") }
            st = Bonds.shift(st, e.id, id, -8)
        }
        st = st.copy(morale = (st.morale + 1).coerceAtMost(100))
        val tail = if (envious.isEmpty()) "" else " Zazdrość: ${envious.joinToString { Autonomy.short(it) }}."
        return ok(st, "🏮 Mistrz chwali ${Autonomy.short(d)} przy całej sekcie.$tail")
    }

    /** Następne wielkie królestwo – cel obietnicy. */
    fun nextMajor(d: Disciple): MajorRealm? = d.majorRealm?.let { m -> MajorRealm.entries.getOrNull(m.ordinal + 1) }

    /**
     * Obietnica: „przebij się do [nextMajor] w [PROMISE_DAYS] dni, a dostaniesz [reward]”.
     * [reward] = klucz przedmiotu w magazynie albo "art:<indeks w skarbcu>".
     */
    fun promise(s: GameState, id: String, reward: String): StepResult {
        val d = s.disciple(id) ?: return ok(s)
        if (focusLeft(s) <= 0) return tired(s)
        if (d.promise != null) return fail(s, "${Autonomy.short(d)} czeka już na spełnienie obietnicy.")
        val target = nextMajor(d) ?: return fail(s, "${Autonomy.short(d)} stoi na szczycie – nie ma czego obiecać.")
        val p = if (reward.startsWith("art:")) {
            val a = s.vaultArtifacts.getOrNull(reward.removePrefix("art:").toIntOrNull() ?: -1) ?: return ok(s)
            MasterPromise(artifactId = a.id, artifactName = a.name, targetMajor = target.ordinal, madeDay = s.currentDay, untilDay = s.currentDay + PROMISE_DAYS)
        } else {
            if ((s.inventory[reward] ?: 0) <= 0) return fail(s, "W magazynie nie ma: $reward.")
            MasterPromise(item = reward, targetMajor = target.ordinal, madeDay = s.currentDay, untilDay = s.currentDay + PROMISE_DAYS)
        }
        val st = spend(s).update(id) {
            it.copy(promise = p, trust = (it.trust + PROMISE_TRUST).coerceAtMost(100))
                .note(s.currentDay, "Mistrz obiecał mi ${p.rewardName}, jeśli osiągnę ${target.polishName} do dnia ${p.untilDay}.")
        }
        return ok(st, "🤝 Obietnica: ${Autonomy.short(d)} dostanie ${p.rewardName} za ${target.polishName} (do dnia ${p.untilDay}). Nie sprzedawaj nagrody!")
    }

    /** Codziennie: spełnione obietnice dostarczają nagrodę, przeterminowane wygasają, niemożliwe do spełnienia – łamią więź. */
    fun daily(s0: GameState): Pair<GameState, List<String>> {
        var s = s0
        val notes = mutableListOf<String>()
        for (d in s0.disciples) {
            val p = d.promise ?: continue
            val reached = (d.majorRealm?.ordinal ?: 0) >= p.targetMajor
            when {
                reached -> {
                    val (paid, ok) = deliver(s, d, p)
                    s = paid
                    s = if (ok) {
                        notes += "🤝 Mistrz dotrzymuje słowa: ${Autonomy.short(d)} otrzymuje ${p.rewardName}."
                        s.update(d.id) {
                            it.copy(promise = null, trust = (it.trust + PROMISE_KEPT_TRUST).coerceAtMost(100), loyalty = (it.loyalty + PROMISE_KEPT_LOYALTY).coerceAtMost(100))
                                .note(s.currentDay, "Mistrz dotrzymał obietnicy: ${p.rewardName}.")
                        }
                    } else {
                        notes += "💔 Obietnica złamana: dla ${Autonomy.short(d)} zabrakło ${p.rewardName}. Zaufanie runęło."
                        s.update(d.id) {
                            it.copy(promise = null, trust = (it.trust - PROMISE_BROKEN_TRUST).coerceAtLeast(0), loyalty = (it.loyalty - PROMISE_BROKEN_LOYALTY).coerceAtLeast(0))
                                .note(s.currentDay, "Mistrz złamał słowo. Nie zapomnę.")
                        }
                    }
                }
                s.currentDay > p.untilDay -> {
                    notes += "⌛ ${Autonomy.short(d)} nie zdążył${d.v("", "a")} – obietnica wygasa."
                    s = s.update(d.id) { it.copy(promise = null).note(s.currentDay, "Nie zdążył${d.v("em", "am")}. Następnym razem.") }
                }
            }
        }
        return s to notes
    }

    /** Przekazanie nagrody: przedmiot z magazynu albo artefakt ze skarbca (od razu założony). */
    private fun deliver(s: GameState, d: Disciple, p: MasterPromise): Pair<GameState, Boolean> {
        p.item?.let { item ->
            val n = s.inventory[item] ?: 0
            if (n <= 0) return s to false
            val used = SectCommands.usePill(s.copy(inventory = s.inventory + (item to n)), d.id, item)
            // Gdy przedmiot nie jest pigułką do zażycia – trafia do ucznia jako dar (znika z magazynu).
            return (if (used.state.inventory[item] != n) used.state else s.copy(inventory = s.inventory + (item to n - 1))) to true
        }
        val i = s.vaultArtifacts.indexOfFirst { it.id == p.artifactId && it.name == p.artifactName }
        if (i < 0) return s to false
        return Gear.equip(s, d.id, i).state to true
    }
}
