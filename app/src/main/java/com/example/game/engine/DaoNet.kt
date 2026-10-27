package com.example.game.engine

import com.example.model.Disciple
import com.example.model.MajorRealm

/**
 * Etap 9: Sieć Dao – asynchroniczne PvP z „cieniami” drużyn.
 *
 * Walka liczona lokalnie, ale w arytmetyce całkowitej i z jawnym SplitMix64 ([GameRng.nextRaw]),
 * więc serwer (Edge Function, `supabase/functions/verify-duel`) odtwarza ją bit w bit z tego samego ziarna.
 */
object DaoNet {
    const val TEAM_SIZE = 3
    const val START_RATING = 1_000
    const val K_FACTOR = 32
    const val MAX_ROUNDS = 30
    const val SEASON_DAYS = 28

    data class ShadowFighter(
        val name: String = "",
        val realm: Int = 0,
        val stage: Int = 1,
        val attack: Int = 0,
        val defense: Int = 0,
        val hp: Int = 0
    )

    data class ShadowTeam(
        val id: String = "",
        val sectName: String = "",
        val rating: Int = START_RATING,
        val era: Int = 1,
        val fighters: List<ShadowFighter> = emptyList()
    )

    data class Bout(val a: Int, val b: Int, val winnerA: Boolean, val rounds: Int)
    data class Duel(val winnerA: Boolean, val bouts: List<Bout>, val seed: Long)

    fun fighter(d: Disciple): ShadowFighter =
        ShadowFighter(d.name.substringBefore(" ("), d.majorRealm?.ordinal ?: 0, d.stage, d.attack, d.defense, d.maxHp)

    /** Cień drużyny: trzech najsilniejszych uczniów. */
    fun shadow(s: GameState, id: String = "local"): ShadowTeam = ShadowTeam(
        id = id,
        sectName = s.sectName,
        rating = s.pvpRating,
        era = s.meta.era,
        fighters = s.disciples.sortedByDescending { it.combatPower }.take(TEAM_SIZE).map { fighter(it) }
    )

    // --- Zakresy statystyk (sprawdzane też na serwerze) ---

    /** Górna granica ataku/obrony/HP dla królestwa – hojna, łapie tylko oczywiste oszustwa. */
    fun maxStat(realm: Int): Long {
        var v = 4_000L
        repeat(realm.coerceIn(0, MajorRealm.entries.size)) { v *= 3 }
        return v
    }

    fun valid(t: ShadowTeam): Boolean =
        t.fighters.size in 1..TEAM_SIZE && t.fighters.all { f ->
            f.realm in 0 until MajorRealm.entries.size && f.stage in 1..9 &&
                f.attack in 1..maxStat(f.realm) && f.defense in 0..maxStat(f.realm) && f.hp in 1..maxStat(f.realm) * 10
        }

    // --- Walka (przenośna arytmetyka całkowita) ---

    /** Liczba z [0, n) z jednego słowa SplitMix64. */
    fun roll(rng: GameRng, n: Int): Int = ((rng.nextRaw() ushr 1) % n).toInt()

    fun bout(a: ShadowFighter, b: ShadowFighter, rng: GameRng): Pair<Boolean, Int> {
        var ha = a.hp.toLong()
        var hb = b.hp.toLong()
        var round = 0
        while (ha > 0 && hb > 0 && round < MAX_ROUNDS) {
            round++
            val critA = roll(rng, 100) < CombatSystem.CRIT_CHANCE
            var dmgA = maxOf(20L, a.attack.toLong() - b.defense.toLong() * 2 / 5)
            if (critA) dmgA = dmgA * 19 / 10
            hb -= dmgA
            if (hb <= 0) break
            val critB = roll(rng, 100) < CombatSystem.CRIT_CHANCE
            var dmgB = maxOf(20L, b.attack.toLong() - a.defense.toLong() * 2 / 5)
            if (critB) dmgB = dmgB * 19 / 10
            ha -= dmgB
        }
        // Remis po limicie rund: wygrywa ten, komu zostało procentowo więcej życia.
        val winA = when {
            hb <= 0 -> true
            ha <= 0 -> false
            else -> ha * b.hp >= hb * a.hp
        }
        return winA to round
    }

    /** Trzy pojedynki w kolejności siły; wygrywa drużyna z większą liczbą zwycięstw. */
    fun resolve(a: ShadowTeam, b: ShadowTeam, seed: Long): Duel {
        val rng = GameRng(seed)
        val n = minOf(a.fighters.size, b.fighters.size)
        val bouts = (0 until n).map { i ->
            val (w, r) = bout(a.fighters[i], b.fighters[i], rng)
            Bout(i, i, w, r)
        }
        val winsA = bouts.count { it.winnerA }
        return Duel(winsA * 2 > n || (winsA * 2 == n && a.fighters.size > b.fighters.size), bouts, seed)
    }

    // --- Ranking ---

    /** Elo w liczbach całkowitych (tabela oczekiwanego wyniku w promilach). */
    fun expectedPermille(ra: Int, rb: Int): Int {
        val diff = (rb - ra).coerceIn(-800, 800)
        return (1000.0 / (1 + Math.pow(10.0, diff / 400.0))).toInt()
    }

    fun newRating(r: Int, opponent: Int, won: Boolean): Int {
        val exp = expectedPermille(r, opponent)
        val score = if (won) 1000 else 0
        return (r + K_FACTOR * (score - exp) / 1000).coerceAtLeast(100)
    }

    fun season(realDay: Long): Long = realDay / SEASON_DAYS

    /** Drabinka turnieju: rozstawienie 1–8, 4–5, 2–7, 3–6 (klasyczne), deterministyczne z ziarna turnieju. */
    fun bracket(teams: List<ShadowTeam>, seed: Long): List<ShadowTeam> {
        val seeded = teams.sortedByDescending { it.rating }.take(8)
        if (seeded.size < 2) return seeded
        var round = listOf(0 to 7, 3 to 4, 1 to 6, 2 to 5).filter { (x, y) -> x < seeded.size && y < seeded.size }.map { (x, y) -> seeded[x] to seeded[y] }
        var r = 0
        val path = mutableListOf<ShadowTeam>()
        while (round.isNotEmpty()) {
            val winners = round.mapIndexed { i, (x, y) -> if (resolve(x, y, seed * 31 + r * 8 + i).winnerA) x else y }
            path += winners
            round = winners.chunked(2).filter { it.size == 2 }.map { it[0] to it[1] }
            r++
        }
        return path
    }

    /** Wynik pojedynku w sieci: zmiana rankingu gracza (komenda, żeby trafiła do zapisu i licznika osiągnięć). */
    fun applyResult(s: GameState, opponent: ShadowTeam, seed: Long): StepResult {
        val me = shadow(s)
        if (!valid(opponent)) return fail(s, "Cień przeciwnika ma niemożliwe statystyki – odrzucono.")
        val duel = resolve(me, opponent, seed)
        val rating = newRating(s.pvpRating, opponent.rating, duel.winnerA)
        val st = s.copy(
            pvpRating = rating,
            pvpWins = s.pvpWins + if (duel.winnerA) 1 else 0,
            pvpLosses = s.pvpLosses + if (duel.winnerA) 0 else 1,
            sectReputation = s.sectReputation + if (duel.winnerA) 15 else 0
        )
        val score = duel.bouts.joinToString(" ") { if (it.winnerA) "✔" else "✘" }
        return ok(st, (if (duel.winnerA) "🏆 Zwycięstwo" else "☠ Porażka") + " z cieniem ${opponent.sectName} ($score). Ranking: ${s.pvpRating} → $rating.")
    }

    /** Lokalni przeciwnicy (bez sieci): cienie sekt-rywali skalowane do siły gracza. */
    fun offlineOpponents(s: GameState): List<ShadowTeam> {
        val me = shadow(s)
        val base = me.fighters.ifEmpty { listOf(ShadowFighter("Uczeń", 1, 1, 100, 50, 1000)) }
        return s.rivalSects.take(6).mapIndexed { i, r ->
            val f = 0.75 + 0.1 * i
            ShadowTeam(
                id = "rival_${r.id}",
                sectName = r.name,
                rating = START_RATING + (r.strength - 1500) / 5,
                fighters = base.mapIndexed { j, b ->
                    ShadowFighter("${r.leaderName.substringAfterLast(" ")} ${j + 1}", b.realm, b.stage, (b.attack * f).toInt().coerceAtLeast(1), (b.defense * f).toInt(), (b.hp * f).toInt().coerceAtLeast(1))
                }
            )
        }
    }
}

/** Zapis w chmurze: konflikt dwóch urządzeń rozstrzyga gracz, widząc porównanie. */
object CloudSave {
    data class Summary(val day: Int, val era: Int, val topRealm: String, val disciples: Int, val savedAtMs: Long)

    fun summary(s: GameState, savedAtMs: Long): Summary = Summary(
        s.currentDay, s.meta.era,
        s.disciples.maxByOrNull { (it.majorRealm?.ordinal ?: 0) * 10 + it.stage }?.let { Realms.label(it) } ?: "—",
        s.disciples.size, savedAtMs
    )

    enum class Choice { LOCAL, REMOTE, SAME }

    /** Podpowiedź: dalej w grze = era, potem dzień. Decyzję i tak podejmuje gracz. */
    fun suggest(local: Summary, remote: Summary): Choice = when {
        local.era == remote.era && local.day == remote.day -> Choice.SAME
        remote.era > local.era || (remote.era == local.era && remote.day > local.day) -> Choice.REMOTE
        else -> Choice.LOCAL
    }
}
