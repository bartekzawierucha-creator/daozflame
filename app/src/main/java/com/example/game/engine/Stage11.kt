package com.example.game.engine

/** Etap 11: dobowe systemy (blizny, obietnice, więzi, cel tygodnia, okazje, zarządca) i migracja. */
object Stage11 {
    fun normalize(s0: GameState): GameState {
        // Etap 15: opisy budynków mówią, co budynek naprawdę robi.
        val s = s0.copy(facilities = s0.facilities.map { f -> FACILITY_TEXT[f.type]?.let { f.copy(effectDescription = it) } ?: f })
        if (s.disciples.none { Gear.misplaced(it) }) return s
        return s.copy(disciples = s.disciples.map { Gear.normalize(it) })
    }

    val FACILITY_TEXT: Map<com.example.model.FacilityType, String> = mapOf(
        com.example.model.FacilityType.CULTIVATION_PAVILION to "Miejsca dla uczniów i limit Starszych; od 2. poziomu +10% Qi sekty za każdy poziom.",
        com.example.model.FacilityType.ALCHEMY_CHAMBER to "Kolejne poziomy otwierają wyższe stopnie receptur i podnoszą jakość pigułek.",
        com.example.model.FacilityType.DIVINE_FORGE to "Poziom podnosi jakość wykuwanych artefaktów.",
        com.example.model.FacilityType.MARTIAL_ARCHIVES to "+1 punkt Dao za każdy etap kultywacji; przy wielkim przełomie tyle punktów, ile poziomów Wieży. Zbędne zwoje zamienia na punkty Dao.",
        com.example.model.FacilityType.MISSION_HALL to "Wyprawy i Wieża Prób; +10% łupów wypraw za poziom ponad 1., od 3. poziomu dodatkowa próba w Wieży i dłuższe ostrzeżenia przed katastrofą.",
        com.example.model.FacilityType.DIPLOMACY_HALL to "Klany, karawany i Sieć Dao; kupcy płacą +5% za poziom ponad 1., od 5. poziomu dodatkowe miejsce na edykt."
    )

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
