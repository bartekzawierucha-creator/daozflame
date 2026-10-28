package com.example.game.engine

/** Etap 11: dobowe systemy (blizny, obietnice, więzi, cel tygodnia, okazje, zarządca) i migracja. */
object Stage11 {
    fun normalize(s: GameState): GameState {
        if (s.disciples.none { Gear.misplaced(it) }) return s
        return s.copy(disciples = s.disciples.map { Gear.normalize(it) })
    }

    fun daily(s0: GameState, ctx: EngineContext): Pair<GameState, List<String>> {
        val notes = mutableListOf<String>()
        var s = s0
        fun run(step: (GameState) -> Pair<GameState, List<String>>) { val (x, n) = step(s); s = x; notes += n }
        run { Legacy.scars(it) }
        run { Mentor.daily(it) }
        run { Bonds.daily(it, ctx) }
        run { SectGoals.daily(it, ctx) }
        run { Opportunities.daily(it, ctx) }
        run { Steward.daily(it) }
        // Etap 13
        run { Auction.daily(it, ctx) }
        run { SelfStudy.daily(it) }
        run { Elders.daily(it) }
        // Etap 14
        run { Buildings.daily(it) }
        return s to notes
    }
}
