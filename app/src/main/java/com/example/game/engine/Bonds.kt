package com.example.game.engine

import com.example.game.engine.DiscipleRules.has
import com.example.model.Bond
import com.example.model.BondKind
import com.example.model.Disciple

/**
 * Etap 11: więzi między uczniami. Wspólna praca, charakter i przeżycia zbliżają albo dzielą.
 * Przyjaciele chętniej pracują razem, rywale napędzają się nawzajem (przełom jednego dodaje Qi drugiemu)
 * i czasem ścierają się na dziedzińcu, bracia przysięgli walczą razem lepiej.
 */
object Bonds {
    const val EVERY_DAYS = 3
    const val MIN_STORED = 5
    const val RIVAL_SPUR_PERCENT = 15.0
    const val FRIEND_ROLE_PULL = 8
    const val SPAR_CHANCE = 4
    const val GRIEF_LOYALTY = 10

    fun of(s: GameState, a: String, b: String): Bond? = s.bonds.find { it.has(a) && it.has(b) && a != b }
    fun value(s: GameState, a: String, b: String): Int = of(s, a, b)?.value ?: 0
    fun of(s: GameState, id: String): List<Bond> = s.bonds.filter { it.has(id) }.sortedByDescending { kotlin.math.abs(it.value) }
    fun rivalsOf(s: GameState, id: String): List<String> = s.bonds.filter { it.has(id) && it.value <= -40 }.map { it.other(id) }
    fun friendsOf(s: GameState, id: String): List<String> = s.bonds.filter { it.has(id) && it.value >= 40 }.map { it.other(id) }

    /** Zmiana więzi; przekroczenie progu (przyjaźń, rywalizacja…) trafia do kroniki. */
    fun shift(s: GameState, a: String, b: String, delta: Int): GameState {
        if (a == b || delta == 0 || s.disciple(a) == null || s.disciple(b) == null) return s
        val old = of(s, a, b)
        val v = ((old?.value ?: 0) + delta).coerceIn(-100, 100)
        val rest = s.bonds.filter { it !== old }
        val next = if (kotlin.math.abs(v) < MIN_STORED) rest else rest + Bond(old?.a ?: a, old?.b ?: b, v, old?.sinceDay ?: s.currentDay)
        var st = s.copy(bonds = next)
        val k0 = BondKind.of(old?.value ?: 0)
        val k1 = BondKind.of(v)
        if (k0 != k1 && k1 != BondKind.NEUTRAL) {
            val na = Autonomy.short(s.disciple(a)!!)
            val nb = Autonomy.short(s.disciple(b)!!)
            st = WorldAI.note(st, "${k1.glyph} $na i $nb: ${k1.polishName.lowercase()}.")
        }
        return st
    }

    /** Naturalny dryf więzi między parą uczniów (deterministyczny + szum). */
    fun drift(a: Disciple, b: Disciple, roll: Int): Int {
        var v = 0
        if (a.role == b.role) v += 2
        if (a.martialSchool == b.martialSchool) v += 1
        if (a.mentorId == b.id || b.mentorId == a.id) v += 2
        if (a.has("charismatic") || b.has("charismatic")) v += 1
        if (a.has("humble") && b.has("humble")) v += 1
        if (a.has("hotheaded") && b.has("hotheaded")) v -= 2
        if (a.has("ambitious") && b.has("ambitious")) v -= 2
        if (a.has("jealous") || b.has("jealous")) v -= 1
        if (a.has("traitor") || b.has("traitor")) v -= 1
        return v + roll
    }

    /** Co [EVERY_DAYS] dni: więzi dryfują, rywale czasem się ścierają. */
    fun daily(s0: GameState, ctx: EngineContext): Pair<GameState, List<String>> {
        if (s0.currentDay % EVERY_DAYS != 0 || s0.disciples.size < 2) return s0 to emptyList()
        val rng = ctx.rng("bonds")
        var s = s0
        val notes = mutableListOf<String>()
        val ds = s0.disciples
        for (i in ds.indices) for (j in i + 1 until ds.size) {
            val delta = drift(ds[i], ds[j], rng.nextInt(5) - 2)
            s = shift(s, ds[i].id, ds[j].id, delta)
        }
        // Spięcia rywali: sparing na dziedzińcu – rany albo pojednanie.
        s.bonds.filter { it.value <= -40 }.forEach { b ->
            if (rng.nextInt(100) >= SPAR_CHANCE) return@forEach
            val x = s.disciple(b.a) ?: return@forEach
            val y = s.disciple(b.b) ?: return@forEach
            if (rng.nextInt(100) < 30) {
                s = shift(s, x.id, y.id, 30)
                notes += "🍵 ${Autonomy.short(x)} i ${Autonomy.short(y)} wyjaśniają spór przy herbacie."
            } else {
                val loser = if (x.combatPower >= y.combatPower) y else x
                s = s.copy(disciples = s.disciples.map { if (it.id == loser.id) it.copy(currentHp = (it.currentHp - it.maxHp / 5).coerceAtLeast(1)) else it })
                s = shift(s, x.id, y.id, -5)
                notes += "⚡ ${Autonomy.short(x)} i ${Autonomy.short(y)} ścierają się na dziedzińcu. ${Autonomy.short(loser)} schodzi poobijan${if (loser.gender == "Kobieta") "a" else "y"}."
            }
        }
        return s to notes
    }

    /** Premia do wyboru zajęcia: przyjaciele lubią pracować razem. */
    fun rolePull(s: GameState, d: Disciple, role: com.example.model.DiscipleRole): Int =
        friendsOf(s, d.id).count { s.disciple(it)?.role == role } * FRIEND_ROLE_PULL

    /** Po kroku silnika: udany przełom ucznia podsyca jego rywali (+Qi), wspólne zwycięstwa zbliżają. */
    fun afterEvents(s: GameState, events: List<GameEvent>): GameState {
        var st = s
        events.filterIsInstance<GameEvent.Breakthrough>().filter { it.success }.forEach { e ->
            rivalsOf(st, e.discipleId).forEach { rid ->
                st = st.copy(disciples = st.disciples.map { r ->
                    if (r.id != rid) r
                    else if (st.e3Initialized) CultivationSystem.gainPercent(r, RIVAL_SPUR_PERCENT)
                    else r.copy(realmProgress = (r.realmProgress + 10).coerceAtMost(100))
                })
            }
        }
        return st
    }

    /** Żałoba: przyjaciele zmarłego tracą lojalność, rywal czuje pustkę (więź znika). */
    fun grief(s: GameState, dead: Disciple): GameState {
        val friends = friendsOf(s, dead.id).toSet()
        if (friends.isEmpty()) return s
        return s.copy(disciples = s.disciples.map { d ->
            if (d.id !in friends) d
            else d.copy(loyalty = (d.loyalty - GRIEF_LOYALTY).coerceAtLeast(0), plan = "Opłakuje ${Autonomy.short(dead)}.",
                diary = (d.diary + "Dz. ${s.currentDay}: Odszedł${if (dead.gender == "Kobieta") "a" else ""} ${Autonomy.short(dead)}. Pamiętam.").takeLast(Autonomy.DIARY_SIZE))
        })
    }
}

/**
 * Etap 11: porażka z sensem. Zagojona ciężka rana zostawia bliznę, która hartuje (+obrona, +HP).
 * Śmierć zostawia spuściznę: najbliższy uczeń (podopieczny albo przyjaciel) przejmuje część zrozumienia Praw
 * i przysięgę – zyskuje zrozumienie Dao.
 */
object Legacy {
    const val SCAR_DEFENSE = 3
    const val SCAR_HP_PERCENT = 3
    const val MAX_SCARS = 9
    const val LAW_SHARE = 0.3
    const val OATH_COMPREHENSION = 3

    /** Codziennie: rany zagojone do wczoraj zamieniają się w blizny. */
    fun scars(s: GameState): Pair<GameState, List<String>> {
        val notes = mutableListOf<String>()
        val ds = s.disciples.map { d ->
            val healed = d.injuredUntilDay in 1 until s.currentDay && d.scarredForDay != d.injuredUntilDay
            if (!healed) return@map d
            if (d.scars >= MAX_SCARS) return@map d.copy(scarredForDay = d.injuredUntilDay)
            notes += "🩹 ${Autonomy.short(d)} wstaje z łoża ran zahartowan${if (d.gender == "Kobieta") "a" else "y"} – blizna nr ${d.scars + 1} (+$SCAR_DEFENSE obrony)."
            val hp = d.maxHp * SCAR_HP_PERCENT / 100
            d.copy(scars = d.scars + 1, scarredForDay = d.injuredUntilDay, defense = d.defense + SCAR_DEFENSE, maxHp = d.maxHp + hp,
                diary = (d.diary + "Dz. ${s.currentDay}: Co mnie nie zabiło, to mnie zahartowało.").takeLast(Autonomy.DIARY_SIZE))
        }
        return s.copy(disciples = ds) to notes
    }

    /** Spadkobierca spuścizny: podopieczny zmarłego, inaczej najbliższy przyjaciel. */
    fun heir(s: GameState, dead: Disciple): Disciple? =
        s.disciples.filter { it.id != dead.id && it.mentorId == dead.id }.maxByOrNull { it.combatPower }
            ?: Bonds.friendsOf(s, dead.id).mapNotNull { s.disciple(it) }.maxByOrNull { Bonds.value(s, dead.id, it.id) }

    fun onDeath(s0: GameState, dead: Disciple, cause: String): GameState {
        var s = Bonds.grief(s0, dead)
        val h = heir(s, dead) ?: return s
        val laws = (h.laws.keys + dead.laws.keys).associateWith { k ->
            maxOf(h.laws[k] ?: 0.0, minOf(100.0, (h.laws[k] ?: 0.0) + (dead.laws[k] ?: 0.0) * LAW_SHARE))
        }
        s = s.copy(disciples = s.disciples.map { d ->
            if (d.id != h.id) d
            else d.copy(
                laws = laws,
                comprehension = d.comprehension + OATH_COMPREHENSION,
                plan = "Przysięga nad grobem ${Autonomy.short(dead)}.",
                diary = (d.diary + "Dz. ${s.currentDay}: Przejmuję naukę ${Autonomy.short(dead)}. Nie zawiodę.").takeLast(Autonomy.DIARY_SIZE)
            )
        })
        return WorldAI.note(s, "📿 ${Autonomy.short(h)} przejmuje spuściznę ${Autonomy.short(dead)} ($cause).")
    }
}
