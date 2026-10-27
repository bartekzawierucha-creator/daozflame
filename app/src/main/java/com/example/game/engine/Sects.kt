package com.example.game.engine

import com.example.model.DiplomaticStatus
import com.example.model.Disciple
import com.example.model.Raid
import com.example.model.RivalSect
import com.example.model.SpiritElement
import com.example.model.SpyMission
import com.example.model.SpyType

/**
 * Etap 5b: żywe sekty. AI każdej sekty działa raz na 10 dni (przesunięte o indeks),
 * więc koszt symulacji offline rośnie z dniami, a nie z tickami.
 */
object WorldAI {
    const val ACTION_EVERY_DAYS = 10
    const val FIRST_RAID_DAY = 20
    const val MAX_NEMESES = 3
    const val NEMESIS_GRUDGE = 3
    const val MAX_NEMESIS_LEVEL = 5
    /** Żadna sekta nie urośnie ponad tyle średnich siły (koalicja słabszych). */
    const val STRENGTH_CAP = 2.5
    const val DOMINANCE_SHARE = 0.4
    const val CHRONICLE = 30

    fun initialStrength(rank: Int): Int = 600 + (17 - rank.coerceIn(1, 16)) * 120

    /** Migracja do Etapu 5b: siła z rangi sekty. */
    fun normalize(s: GameState): GameState {
        if (s.e5Initialized) return s
        return s.copy(
            e5Initialized = true,
            rivalSects = s.rivalSects.map { if (it.strength > 0) it else it.copy(strength = initialStrength(it.powerRank)) }
        )
    }

    fun hostile(r: RivalSect): Boolean = r.relations <= -30 || r.status == DiplomaticStatus.TOTAL_WAR || r.nemesisLevel > 0

    /** Kronika ma limit wpisów; starsze wpisy kompresuje się do rozdziałów (Etap 7). */
    fun note(s: GameState, text: String): GameState {
        val all = s.chronicle + "Dzień ${s.currentDay}: $text"
        if (all.size <= CHRONICLE) return s.copy(chronicle = all)
        val old = all.take(10)
        val first = old.first().substringAfter("Dzień ").substringBefore(":")
        val last = old.last().substringAfter("Dzień ").substringBefore(":")
        val chapter = "Rozdział ${s.chronicleChapters.size + 1} (dni $first–$last): " + old.joinToString(" ") { it.substringAfter(": ") }.take(400)
        return s.copy(chronicle = all.drop(10), chronicleChapters = (s.chronicleChapters + chapter).takeLast(40))
    }

    /** Raz na dzień gry: przybycie najazdu, powrót szpiegów, decyzje sekt. */
    fun daily(start: GameState, ctx: EngineContext): Pair<GameState, List<String>> {
        if (!start.e5Initialized) return start to emptyList()
        var s = start
        val notes = mutableListOf<String>()
        s.raid?.let { r -> if (s.currentDay >= r.arriveDay) Raids.resolve(s, ctx).let { (st, msg) -> s = st; notes += msg } }
        Spies.returning(s, ctx).let { (st, msgs) -> s = st; notes += msgs }
        val avg = s.rivalSects.map { it.strength }.average().takeIf { !it.isNaN() } ?: 1.0
        s.rivalSects.forEachIndexed { i, sect ->
            if (s.currentDay % ACTION_EVERY_DAYS != i % ACTION_EVERY_DAYS) return@forEachIndexed
            val (st, msg) = act(s, s.rivalSects.first { it.id == sect.id }, avg, ctx)
            s = st
            msg?.let { notes += it }
        }
        return s to notes
    }

    private fun act(s: GameState, sect: RivalSect, avg: Double, ctx: EngineContext): Pair<GameState, String?> {
        val rng = ctx.rng("sect-ai:${sect.id}")
        fun update(f: (RivalSect) -> RivalSect) = s.rivalSects.map { if (it.id == sect.id) f(it) else it }

        // Najazd na gracza: tylko wrogowie, nie za wcześnie, jeden naraz.
        if (hostile(sect) && s.currentDay >= FIRST_RAID_DAY && s.raid == null && rng.nextInt(100) < if (sect.nemesisLevel > 0) 60 else 35) {
            val raid = Raid(sect.id, sect.name, s.currentDay + 2, Raids.strength(sect))
            val msg = "⚠ ${sect.name} szykuje najazd! Wojownicy staną u bram za 2 dni. Obsadź bramy."
            return note(s.copy(raid = raid), "${sect.name} wyrusza na sektę gracza.") to msg
        }
        val roll = rng.nextInt(100)
        return when {
            roll < 40 -> {
                val cap = (avg * STRENGTH_CAP).toInt()
                s.copy(rivalSects = update { it.copy(strength = (it.strength * 1.04).toInt().coerceAtMost(cap.coerceAtLeast(it.strength))) }) to null
            }
            roll < 70 -> attackSect(s, sect, rng)
            roll < 90 -> {
                // Dyplomacja: relacje z graczem wracają ku zeru; demoniczne sekty dryfują w stronę wrogości.
                // Same z siebie nie popadają w otwartą wrogość: demoniczne dryfują do −20, reszta do zera.
                val rest = if (sect.alignment.contains("Demoniczna")) -20 else 0
                val drift = when {
                    sect.relations < rest -> 4
                    sect.relations > rest -> -3
                    else -> 0
                }
                s.copy(rivalSects = update { it.copy(relations = (it.relations + drift).coerceIn(-100, 100)) }) to null
            }
            else -> s to null
        }
    }

    private fun attackSect(s: GameState, sect: RivalSect, rng: kotlin.random.Random): Pair<GameState, String?> {
        val owned = s.territories.count { it.controllingSectId == sect.id }
        val total = s.territories.size.coerceAtLeast(1)
        // Dominująca sekta nie atakuje dalej – reszta zawiązuje koalicję.
        if (owned.toDouble() / total >= DOMINANCE_SHARE) return s to null
        val rumored = s.rivalSects.filter { (s.flags["rumor:${it.id}"] ?: 0) >= s.currentDay && it.id != sect.id }
        val target = (rumored.ifEmpty { s.rivalSects.filter { it.id != sect.id } }).randomOrNull(rng) ?: return s to null
        val win = sect.strength * (0.7 + rng.nextDouble() * 0.6) > target.strength
        var st = s.copy(rivalSects = s.rivalSects.map {
            when (it.id) {
                sect.id -> it.copy(strength = (it.strength * if (win) 1.03 else 0.95).toInt())
                target.id -> it.copy(strength = (it.strength * if (win) 0.92 else 0.98).toInt().coerceAtLeast(300))
                else -> it
            }
        })
        if (win) {
            st.territories.firstOrNull { it.controllingSectId == target.id }?.let { t ->
                st = st.copy(territories = st.territories.map {
                    if (it.id == t.id) it.copy(controllingSectId = sect.id, controllingSectName = sect.name) else it
                })
                st = note(st, "${sect.name} odbiera ${t.name} sekcie ${target.name}.")
            } ?: run { st = note(st, "${sect.name} pokonuje ${target.name} w potyczce.") }
        }
        return st to null
    }

    /** Uraza po porażce z rąk gracza; po 3 porażkach sekta staje się nemezis (najwyżej 3 naraz) i wraca silniejsza. */
    fun grudge(s: GameState, sectId: String): Pair<GameState, String?> {
        val sect = s.rivalSects.find { it.id == sectId } ?: return s to null
        val activeNemeses = s.rivalSects.count { it.nemesisLevel > 0 }
        val g = sect.grudge + 1
        val (level, msg) = when {
            sect.nemesisLevel > 0 -> (sect.nemesisLevel + 1).coerceAtMost(MAX_NEMESIS_LEVEL) to "☠ ${sect.leaderName} poprzysięga zemstę – nemezis wróci silniejszy."
            g >= NEMESIS_GRUDGE && activeNemeses < MAX_NEMESES -> 1 to "☠ ${sect.name} ogłasza twoją sektę śmiertelnym wrogiem. Narodził się nemezis."
            else -> 0 to null
        }
        val st = s.copy(rivalSects = s.rivalSects.map {
            if (it.id == sectId) it.copy(grudge = g, nemesisLevel = level, relations = (it.relations - 10).coerceAtLeast(-100)) else it
        })
        return (msg?.let { note(st, it) } ?: st) to msg
    }
}

/** Najazdy: ostrzeżenie 2 dni wcześniej, 3 rundy – po jednej na każdą bramę. */
object Raids {
    val GATES = listOf("Brama Wschodnia", "Brama Zachodnia", "Brama Górska")
    const val WALL = 150

    fun strength(sect: RivalSect): Int = (sect.strength * 0.5 * (1 + 0.25 * sect.nemesisLevel)).toInt()

    fun guardPower(d: Disciple): Int = d.attack * 8 + d.defense * 4

    fun gatePower(s: GameState, gate: String): Int {
        val d = s.gates[gate]?.let { s.disciple(it) }
        return ((WALL + (d?.let { guardPower(it) } ?: 0)) * Formation.defenseFactor(s)).toInt()
    }

    fun assign(s: GameState, gate: String, discipleId: String?): StepResult {
        if (gate !in GATES) return ok(s)
        val gates = s.gates.filterValues { it != discipleId }.let { if (discipleId == null) it - gate else it + (gate to discipleId) }
        val name = s.disciple(discipleId)?.name
        return ok(s.copy(gates = gates), if (name != null) "$name strzeże: $gate." else "$gate bez straży.")
    }

    fun resolve(s: GameState, ctx: EngineContext): Pair<GameState, String> {
        val raid = s.raid ?: return s to ""
        val rng = ctx.rng("raid")
        val wins = GATES.count { gate -> gatePower(s, gate) >= raid.strength / 3 * (0.85 + rng.nextDouble() * 0.3) }
        val repelled = wins >= 2
        var st = s.copy(raid = null)
        val msg: String
        if (repelled) {
            val loot = raid.strength / 4
            st = st.copy(
                spiritStones = st.spiritStones + loot,
                sectReputation = st.sectReputation + 20,
                morale = (st.morale + 5).coerceAtMost(100),
                rivalSects = st.rivalSects.map { if (it.id == raid.sectId) it.copy(strength = (it.strength * 0.9).toInt()) else it }
            )
            val (withGrudge, nemesis) = WorldAI.grudge(st, raid.sectId)
            st = withGrudge
            msg = "🛡 Najazd ${raid.sectName} odparty ($wins/3 bram)! Łupy: $loot kamieni." + (nemesis?.let { " $it" } ?: "")
        } else {
            val nemesis = st.rivalSects.find { it.id == raid.sectId }?.nemesisLevel ?: 0
            val lost = minOf(st.spiritStones / 10, 3_000 + 500 * nemesis)
            st = st.copy(
                spiritStones = st.spiritStones - lost,
                sectReputation = st.sectReputation - 15,
                morale = (st.morale - 10).coerceAtLeast(0),
                // Udany najazd gasi gniew rywala; siła rośnie, ale nie ponad limit świata.
                rivalSects = st.rivalSects.map {
                    if (it.id == raid.sectId) it.copy(
                        strength = minOf((it.strength * 1.05).toInt(), (st.rivalSects.map { r -> r.strength }.average() * WorldAI.STRENGTH_CAP).toInt()).coerceAtLeast(it.strength),
                        relations = (it.relations + 15).coerceAtMost(100)
                    ) else it
                }
            )
            msg = "🔥 ${raid.sectName} przełamuje bramy ($wins/3 utrzymane). Zrabowano $lost kamieni." +
                Identity.raidLoss(st, ctx).let { (after, loss) -> st = after; loss?.let { " $it" } ?: "" }
        }
        return WorldAI.note(st, msg) to msg
    }
}

/** Wielka Formacja: 7 węzłów żywiołów; komplet pięciu podstawowych i pełne 7 węzłów dają premie zestawu. */
object Formation {
    const val MAX_NODES = 7
    const val MINERAL = "Duchowy Nefryt"

    fun cost(n: Int): Pair<Int, Int> = 800 * (n + 1) to 5 * (n + 1)

    fun fiveElements(s: GameState): Boolean = SpiritElement.BASIC.all { it in s.formationNodes }

    fun defenseFactor(s: GameState): Double =
        1 + 0.08 * s.formationNodes.size + (if (fiveElements(s)) 0.2 else 0.0) + (if (s.formationNodes.size >= MAX_NODES) 0.1 else 0.0)

    fun qiBonusPercent(s: GameState): Double = if (fiveElements(s)) 5.0 else 0.0

    fun place(s: GameState, element: SpiritElement): StepResult {
        val n = s.formationNodes.size
        if (n >= MAX_NODES) return fail(s, "Wielka Formacja ma już wszystkie $MAX_NODES węzłów.")
        val (stones, mineral) = cost(n)
        if (s.spiritStones < stones) return fail(s, "Węzeł formacji kosztuje $stones kamieni.")
        val have = s.inventory[MINERAL] ?: 0
        if (have < mineral) return fail(s, "Węzeł wymaga ${mineral}× $MINERAL (masz $have).")
        val next = s.copy(spiritStones = s.spiritStones - stones, inventory = s.inventory + (MINERAL to have - mineral), formationNodes = s.formationNodes + element)
        val set = if (!fiveElements(s) && fiveElements(next)) " Zestaw Pięciu Żywiołów aktywny!" else ""
        return ok(next, "Wbito węzeł formacji: ${element.polishName} (${n + 1}/$MAX_NODES).$set")
    }
}

/** Wywiad: 4 typy misji, powrót po 3 dniach, ryzyko wykrycia. */
object Spies {
    const val DAYS = 3

    fun busy(s: GameState, discipleId: String) = s.spies.any { it.discipleId == discipleId }

    fun chance(d: Disciple, sect: RivalSect): Int = (40 + d.comprehension / 2 - sect.strength / 100).coerceIn(10, 90)

    fun send(s: GameState, ctx: EngineContext, discipleId: String, sectId: String, type: SpyType): StepResult {
        val d = s.disciple(discipleId) ?: return ok(s)
        val sect = s.rivalSects.find { it.id == sectId } ?: return ok(s)
        if (busy(s, discipleId)) return fail(s, "${d.name} jest już na misji.")
        val mission = SpyMission(ctx.rng("spy").nextId("spy"), discipleId, sectId, type, s.currentDay + DAYS)
        return ok(s.copy(spies = s.spies + mission), "${d.name} wyrusza: ${type.polishName} u ${sect.name} (szansa ${chance(d, sect)}%).")
    }

    fun returning(s: GameState, ctx: EngineContext): Pair<GameState, List<String>> {
        val due = s.spies.filter { it.returnDay <= s.currentDay }
        if (due.isEmpty()) return s to emptyList()
        var st = s.copy(spies = s.spies - due.toSet())
        val notes = mutableListOf<String>()
        for (m in due) {
            val d = st.disciple(m.discipleId) ?: continue
            val sect = st.rivalSects.find { it.id == m.sectId } ?: continue
            val rng = ctx.rng("spy-return:${m.id}")
            if (rng.nextInt(100) >= chance(d, sect)) {
                st = st.copy(
                    rivalSects = st.rivalSects.map { if (it.id == sect.id) it.copy(relations = (it.relations - 20).coerceAtLeast(-100)) else it },
                    disciples = st.disciples.map { if (it.id == d.id) it.copy(currentHp = (it.maxHp / 10).coerceAtLeast(10)) else it }
                )
                notes += "🕵 ${d.name} wykryty w ${sect.name}! Wraca ranny, relacje −20."
                continue
            }
            st = when (m.type) {
                SpyType.SCOUT -> st.copy(intel = st.intel + (sect.id to st.currentDay)).also { notes += "🕵 Zwiad: ${sect.name} ma siłę ${sect.strength}" + (if (WorldAI.hostile(sect)) ", wrogie zamiary." else ".") }
                SpyType.STEAL -> (sect.strength / 5).let { loot -> notes += "🕵 ${d.name} wynosi $loot kamieni ze skarbca ${sect.name}."; st.copy(spiritStones = st.spiritStones + loot) }
                SpyType.SABOTAGE -> st.copy(rivalSects = st.rivalSects.map { if (it.id == sect.id) it.copy(strength = (it.strength * 0.9).toInt()) else it })
                    .also { notes += "🕵 Sabotaż w ${sect.name}: siła −10%." }
                SpyType.RUMOR -> st.copy(flags = st.flags + ("rumor:${sect.id}" to st.currentDay + 30))
                    .also { notes += "🕵 Plotki krążą: sąsiedzi ${sect.name} szykują się do walki." }
            }
        }
        return st to notes
    }
}

/** Wojny sekt: bitwy masowe (tylko w stanie wojny) i honorowe pojedynki (na śmierć – zawsze z potwierdzeniem). */
object Wars {
    const val MAX_ARMY = 5

    fun declare(s: GameState, sectId: String): StepResult = WorldCommands.interactRivalSect(s, sectId, "DECLARE_WAR")

    fun battle(s: GameState, ctx: EngineContext, sectId: String, discipleIds: List<String>): StepResult {
        val sect = s.rivalSects.find { it.id == sectId } ?: return ok(s)
        if (sect.status != DiplomaticStatus.TOTAL_WAR) return fail(s, "Bitwa wymaga stanu wojny z ${sect.name}.")
        val army = discipleIds.distinct().take(MAX_ARMY).mapNotNull { s.disciple(it) }.filter { it.currentHp > it.maxHp / 3 }
        if (army.isEmpty()) return fail(s, "Brak zdolnych do walki uczniów.")
        val rng = ctx.rng("battle")
        val power = army.sumOf { Raids.guardPower(it) }
        val enemy = sect.strength * (0.8 + rng.nextDouble() * 0.4)
        val ids = army.map { it.id }.toSet()
        if (power >= enemy) {
            var st = s.copy(
                spiritStones = s.spiritStones + sect.strength / 2,
                sectReputation = s.sectReputation + 50,
                rivalSects = s.rivalSects.map { if (it.id == sectId) it.copy(strength = (it.strength * 0.8).toInt().coerceAtLeast(300)) else it }
            )
            val taken = st.territories.firstOrNull { it.controllingSectId == sectId }
            if (taken != null) st = st.copy(territories = st.territories.map {
                if (it.id == taken.id) it.copy(controllingSectId = "player", controllingSectName = st.sectName) else it
            })
            val (withGrudge, nemesis) = WorldAI.grudge(st, sectId)
            val msg = "⚔ Zwycięstwo nad ${sect.name}! +${sect.strength / 2} kamieni" + (taken?.let { ", zdobyto ${it.name}" } ?: "") + "." + (nemesis?.let { " $it" } ?: "")
            return ok(WorldAI.note(withGrudge, msg), msg)
        }
        val st = s.copy(
            disciples = s.disciples.map { if (it.id in ids) it.copy(currentHp = (it.maxHp / 10).coerceAtLeast(10)) else it },
            morale = (s.morale - 10).coerceAtLeast(0),
            sectReputation = s.sectReputation - 20
        )
        return ok(WorldAI.note(st, "Porażka w bitwie z ${sect.name}."), "☠ Armia ${sect.name} rozbija uczniów sekty. Ranni wracają do domu.")
    }

    fun duel(s: GameState, ctx: EngineContext, sectId: String, discipleId: String, toDeath: Boolean, confirmed: Boolean): StepResult {
        val sect = s.rivalSects.find { it.id == sectId } ?: return ok(s)
        val d = s.disciple(discipleId) ?: return ok(s)
        if (toDeath && !confirmed) return fail(s, "Pojedynek na śmierć wymaga potwierdzenia – przegrany nie wraca.")
        val champ = sect.strength * (1 + 0.2 * sect.nemesisLevel)
        val res = Skirmish.resolve(d.attack, d.defense, d.maxHp, champ.toInt(), (champ / 20).toInt(), (champ / 40).toInt(), ctx.rng("duel"))
        if (res.win) {
            var st = s.copy(
                sectReputation = s.sectReputation + if (toDeath) 100 else 40,
                rivalSects = s.rivalSects.map { if (it.id == sectId) it.copy(strength = (it.strength * if (toDeath) 0.85 else 0.95).toInt()) else it }
            )
            var extra = ""
            if (toDeath) WorldAI.grudge(st, sectId).let { (g, n) -> st = g; extra = n?.let { " $it" } ?: "" }
            val msg = "🏆 ${d.name} wygrywa honorowy pojedynek z czempionem ${sect.name}!$extra"
            return ok(WorldAI.note(st, msg), msg)
        }
        if (toDeath) {
            val st = (if (s.e7Initialized) Lifecycle.die(s, d, "pojedynek z ${sect.name}") else s).copy(
                disciples = s.disciples.filter { it.id != d.id },
                gates = s.gates.filterValues { it != d.id },
                morale = (s.morale - 15).coerceAtLeast(0),
                sectReputation = s.sectReputation - 30
            )
            return ok(WorldAI.note(st, "${d.name} poległ w pojedynku z ${sect.name}."), "☠ ${d.name} poległ w pojedynku na śmierć.")
        }
        return ok(
            s.copy(sectReputation = s.sectReputation - 10, disciples = s.disciples.map { if (it.id == d.id) it.copy(currentHp = (it.maxHp / 4).coerceAtLeast(10)) else it }),
            "${d.name} uznaje wyższość czempiona ${sect.name}."
        )
    }
}
