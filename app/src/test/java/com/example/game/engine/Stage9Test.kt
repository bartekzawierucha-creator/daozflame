package com.example.game.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** Etap 9: Sieć Dao – cienie, walidacja, deterministyczne pojedynki, ranking, chmura. */
class Stage9Test {

    private fun fresh(seed: Long = 1L) = GameEngine.newGame(seed)

    @Test
    fun shadowOfLegitSectIsValid() {
        var s = fresh(1L)
        repeat(300) { s = GameEngine.apply(s, GameCommand.Tick).state.copy(activeEventId = null) }
        val t = DaoNet.shadow(s)
        assertEquals(minOf(DaoNet.TEAM_SIZE, s.disciples.size), t.fighters.size)
        assertTrue(DaoNet.valid(t))
    }

    @Test
    fun impossibleStatsAreRejected() {
        val t = DaoNet.shadow(fresh(2L))
        assertFalse(DaoNet.valid(t.copy(fighters = t.fighters.map { it.copy(attack = Int.MAX_VALUE) })))
        assertFalse(DaoNet.valid(t.copy(fighters = t.fighters.map { it.copy(realm = 0, attack = 50_000) })))
        assertFalse(DaoNet.valid(t.copy(fighters = emptyList())))
        val r = GameEngine.apply(fresh(3L), GameCommand.PvpDuel(t.copy(fighters = t.fighters.map { it.copy(hp = -5) }), 1L))
        assertTrue(r.notice!!.contains("niemożliwe"))
    }

    @Test
    fun duelsAreDeterministicFromSeed() {
        val a = DaoNet.shadow(fresh(4L))
        val b = DaoNet.offlineOpponents(fresh(5L)).first()
        assertEquals(DaoNet.resolve(a, b, 42L), DaoNet.resolve(a, b, 42L))
        val outcomes = (1L..50L).map { DaoNet.resolve(a, b, it).bouts.map { x -> x.rounds } }.toSet()
        assertTrue(outcomes.size > 1)
    }

    /** Złoty zestaw wspólny z Edge Function (supabase/functions/_shared/duel.parity.test.ts). */
    @Test
    fun goldenDuelsMatch() {
        val file = listOf("src/test/resources/pvp/golden_duels.json", "app/src/test/resources/pvp/golden_duels.json").map { File(it) }.first { it.exists() }
        val text = file.readText()
        val cases = Regex("""\{"a":(\{.*?\]\}),"b":(\{.*?\]\}),"seed":"(-?\d+)","winnerA":(true|false),"bouts":\[(.*?)\]\}""").findAll(text).toList()
        assertEquals(40, cases.size)
        val f = Regex(""""realm":(\d+),"stage":(\d+),"attack":(\d+),"defense":(\d+),"hp":(\d+)""")
        fun team(js: String) = DaoNet.ShadowTeam(fighters = f.findAll(js).map { m ->
            val (r, st, at, de, hp) = m.destructured
            DaoNet.ShadowFighter("f", r.toInt(), st.toInt(), at.toInt(), de.toInt(), hp.toInt())
        }.toList())
        cases.forEach { m ->
            val (ja, jb, seed, winner, bouts) = m.destructured
            val d = DaoNet.resolve(team(ja), team(jb), seed.toLong())
            assertEquals(winner.toBoolean(), d.winnerA)
            val rounds = Regex(""""rounds":(\d+)""").findAll(bouts).map { it.groupValues[1].toInt() }.toList()
            assertEquals(rounds, d.bouts.map { it.rounds })
        }
    }

    @Test
    fun eloMovesTowardsResult() {
        assertEquals(500, DaoNet.expectedPermille(1000, 1000))
        assertTrue(DaoNet.newRating(1000, 1000, true) > 1000)
        assertTrue(DaoNet.newRating(1000, 1000, false) < 1000)
        assertTrue(DaoNet.newRating(1000, 1400, true) - 1000 > DaoNet.newRating(1000, 600, true) - 1000)
    }

    @Test
    fun bracketPicksAChampion() {
        val s = fresh(6L)
        val teams = DaoNet.offlineOpponents(s) + DaoNet.shadow(s) + DaoNet.offlineOpponents(fresh(7L)).take(1)
        val path = DaoNet.bracket(teams, 99L)
        assertEquals(4 + 2 + 1, path.size)
        assertEquals(path, DaoNet.bracket(teams, 99L))
    }

    @Test
    fun pvpResultUpdatesRatingAndCountsAsCommand() {
        val s = fresh(8L)
        val opp = DaoNet.offlineOpponents(s).first()
        val r = GameEngine.apply(s, GameCommand.PvpDuel(opp, 7L)).state
        assertTrue(r.pvpRating != s.pvpRating)
        assertEquals(1, r.pvpWins + r.pvpLosses)
    }

    @Test
    fun offlineOpponentsAreValid() {
        assertTrue(DaoNet.offlineOpponents(fresh(9L)).all { DaoNet.valid(it) })
    }

    @Test
    fun cloudConflictSuggestsFurtherSave() {
        val a = CloudSave.Summary(100, 1, "x", 5, 0)
        assertEquals(CloudSave.Choice.REMOTE, CloudSave.suggest(a, a.copy(day = 120)))
        assertEquals(CloudSave.Choice.REMOTE, CloudSave.suggest(a, a.copy(era = 2, day = 3)))
        assertEquals(CloudSave.Choice.LOCAL, CloudSave.suggest(a, a.copy(day = 50)))
        assertEquals(CloudSave.Choice.SAME, CloudSave.suggest(a, a))
    }
}
