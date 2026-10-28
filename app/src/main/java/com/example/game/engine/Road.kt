package com.example.game.engine

import com.example.model.FacilityType

/**
 * Etap 14: sekta zaczyna skromnie i rośnie. Budynki trzeba zbudować (kamienie, materiały, czas), a każdy otwiera
 * nowy dział gry. Bestie przychodzą z jaja znalezionego u strażnika krainy. Droga Sekty prowadzi gracza krok po kroku.
 */
object Buildings {
    data class Plan(val stones: Int, val material: String?, val count: Int, val days: Int, val unlocks: String)

    val PLANS: Map<FacilityType, Plan> = mapOf(
        FacilityType.ALCHEMY_CHAMBER to Plan(500, "Duchowy Nefryt", 8, 6, "Alchemia: pigułki przełomu, leczenia i Qi, pola ziół, płomienie w kotle."),
        FacilityType.MISSION_HALL to Plan(800, null, 0, 8, "Świat: wyprawy po zioła i rudy, bossowie krain, Wieża Prób, tajne królestwa."),
        FacilityType.DIVINE_FORGE to Plan(1_500, "Gwiezdne Żelazo", 12, 12, "Kuźnia: artefakty i ulepszanie ekwipunku uczniów."),
        FacilityType.MARTIAL_ARCHIVES to Plan(1_200, null, 0, 10, "Wieża Technik: +1 punkt Dao przy przełomach, zwoje do przepisania."),
        FacilityType.DIPLOMACY_HALL to Plan(2_000, null, 0, 12, "Klany i Sieć Dao: dyplomacja, sojusze, szpiedzy, wojny, arena.")
    )

    /** Startowe zabudowania nowej sekty: tylko Pawilon Kultywacji. */
    fun starting(all: List<com.example.model.SectFacility>) = all.map { if (it.type == FacilityType.CULTIVATION_PAVILION) it else it.copy(level = 0) }

    fun has(s: GameState, t: FacilityType): Boolean = (s.facilities.find { it.type == t }?.level ?: 1) >= 1
    fun building(s: GameState, t: FacilityType): Boolean = s.facilities.find { it.type == t }?.let { it.level == 0 && it.buildDoneDay > 0 } ?: false

    fun start(s: GameState, t: FacilityType): StepResult {
        val f = s.facilities.find { it.type == t } ?: return ok(s)
        val p = PLANS[t] ?: return ok(s)
        if (f.level > 0) return fail(s, "${t.polishName} już stoi.")
        if (f.buildDoneDay > 0) return fail(s, "${t.polishName} jest w budowie (do dnia ${f.buildDoneDay}).")
        if (s.spiritStones < p.stones) return fail(s, "Budowa wymaga ${p.stones} kamieni.")
        val have = p.material?.let { s.inventory[it] ?: 0 } ?: 0
        if (p.material != null && have < p.count) return fail(s, "Budowa wymaga ${p.count}× ${p.material} (masz $have).")
        val inv = p.material?.let { s.inventory + (it to have - p.count) } ?: s.inventory
        return ok(
            s.copy(spiritStones = s.spiritStones - p.stones, inventory = inv,
                facilities = s.facilities.map { if (it.type == t) it.copy(buildDoneDay = s.currentDay + p.days) else it }),
            "🏗 Rusza budowa: ${t.polishName}. Gotowe za ${p.days} dni."
        )
    }

    fun daily(s: GameState): Pair<GameState, List<String>> {
        val done = s.facilities.filter { it.level == 0 && it.buildDoneDay in 1..s.currentDay }
        if (done.isEmpty()) return s to emptyList()
        val st = s.copy(facilities = s.facilities.map { f -> if (f in done) f.copy(level = 1, buildDoneDay = 0) else f })
        return done.fold(st) { acc, f -> WorldAI.note(acc, "Zbudowano: ${f.type.polishName}.") } to
            done.map { "🏯 Zbudowano: ${it.type.polishName}! ${PLANS[it.type]?.unlocks ?: ""}" }
    }

    /** Budynek, którego wymaga komenda (null = żaden). */
    fun required(c: GameCommand): FacilityType? = when (c) {
        is GameCommand.BrewPill, is GameCommand.Experiment, is GameCommand.PlantHerb, GameCommand.HarvestFields, GameCommand.ExpandFields -> FacilityType.ALCHEMY_CHAMBER
        is GameCommand.ForgeArtifact, is GameCommand.UpgradeArtifact -> FacilityType.DIVINE_FORGE
        is GameCommand.BeginExpedition, is GameCommand.StartExpedition, is GameCommand.ClimbTower, is GameCommand.ScoutRegion,
        is GameCommand.EnterSecretRealm, is GameCommand.DelveDungeon, is GameCommand.AttackWorldBoss -> FacilityType.MISSION_HALL
        is GameCommand.InteractRivalSect, is GameCommand.SendSpy, is GameCommand.SectBattle, is GameCommand.HonorDuel,
        is GameCommand.PvpDuel, is GameCommand.ArenaDuel, is GameCommand.SendCaravan -> FacilityType.DIPLOMACY_HALL
        is GameCommand.Advise -> required(c.command)
        else -> null
    }

    fun gate(s: GameState, c: GameCommand): StepResult? {
        val t = required(c) ?: return null
        if (has(s, t)) return null
        return fail(s, "Najpierw zbuduj: ${t.polishName} (Pałac → Budowa albo dotknij budynku na hubie).")
    }
}

/** Droga Sekty: kolejne kroki z nagrodą. Krok zalicza się sam, gdy warunek jest spełniony. */
object Road {
    data class Step(
        val title: String,
        val why: String,
        /** Gdzie to zrobić – nazwa modułu (enum GameModule w UI) albo "build:<FacilityType>". */
        val target: String,
        val rewardStones: Int,
        val rewardItem: Pair<String, Int>? = null,
        val done: (GameState) -> Boolean
    )

    private fun built(t: FacilityType) = { s: GameState -> Buildings.has(s, t) }

    val STEPS: List<Step> = listOf(
        Step("Doradź uczniowi medytację", "Uczniowie sami wybierają zajęcia – ty doradzasz. Medytacja to Qi i przełomy.", "ADEPTS", 200) { s -> s.disciples.any { it.diary.any { d -> "Posłuchał" in d } } },
        Step("Zbuduj Salę Alchemii", "Pigułki przełomu podnoszą szansę wielkiego przełomu o 25–60 pkt.", "build:ALCHEMY_CHAMBER", 300, "Duchowy Nefryt" to 10, built(FacilityType.ALCHEMY_CHAMBER)),
        Step("Uwarz pigułkę", "Kocioł zamienia zioła w pigułki. Jakość zależy od płomienia i alchemików.", "ALCHEMY", 300) { s -> (s.meta.stats["brews"] ?: 0) > 0 },
        Step("Zbuduj Salę Ekspedycji", "Świat poza bramą: zioła, rudy, strażnicy krain i Wieża Prób.", "build:MISSION_HALL", 400, done = built(FacilityType.MISSION_HALL)),
        Step("Pokonaj strażnika krainy", "Strażnicy strzegą łupów – i jaj bestii.", "WORLD", 500) { s -> (s.meta.stats["wins"] ?: 0) > 0 },
        Step("Wykluj bestię", "Jajo z legowiska wysiaduje się w Gnieździe Bestii (Bestie → Wysiaduj). Bestia daje premię Qi całej sekcie.", "BEASTS", 500) { s -> s.sectBeasts.isNotEmpty() },
        Step("Wybierz ścieżkę ucznia", "Ścieżka (Miecz, Ciało, Pigułki…) zmienia rozwój ucznia. Uczniowie wybierają też sami.", "ADEPTS", 300) { s -> s.disciples.any { it.path != null } },
        Step("Zbuduj Boską Kuźnię", "Artefakty i ich ulepszanie – ekwipunek uczniów.", "build:DIVINE_FORGE", 600, done = built(FacilityType.DIVINE_FORGE)),
        Step("Załóż uczniowi artefakt", "Sześć miejsc ekwipunku; pełny komplet daje premię.", "ADEPTS", 600) { s -> s.disciples.any { Gear.items(it).isNotEmpty() } },
        Step("Osiągnij Złoty Rdzeń", "Pierwsza tribulacja. Pigułka gromów i obrona chronią przed piorunami.", "ADEPTS", 1_500, GameContent.BREAKTHROUGH_PILL to 3) { s -> s.disciples.any { (it.majorRealm?.ordinal ?: 0) >= com.example.model.MajorRealm.CORE_FORMATION.ordinal } },
        Step("Zbuduj Salę Dyplomacji", "Sojusze, handel karawanami, szpiedzy i arena innych graczy.", "build:DIPLOMACY_HALL", 1_000, done = built(FacilityType.DIPLOMACY_HALL)),
        Step("Wygraj cel tygodnia", "Co godzinę świat stawia sekcie wyzwanie – nagroda rośnie z każdym zwycięstwem.", "PALACE", 1_000) { s -> s.goalsWon > 0 },
        Step("Osiągnij Narodziny Duszy", "Potrzebny zwój metody stopnia Czarnego – z bossów albo z Domu Aukcyjnego.", "ADEPTS", 5_000, GameContent.BREAKTHROUGH_PILL to 5) { s -> s.disciples.any { (it.majorRealm?.ordinal ?: 0) >= com.example.model.MajorRealm.NASCENT_SOUL.ordinal } }
    )

    fun current(s: GameState): Step? = STEPS.getOrNull(s.roadStep)

    /** Po każdym kroku silnika: zalicza kolejne spełnione kroki i wypłaca nagrody. */
    fun after(s0: GameState): Pair<GameState, List<String>> {
        if (s0.tutorialStep != Tutorial.DONE) return s0 to emptyList()
        var s = s0
        val notes = mutableListOf<String>()
        var guard = 0
        while (guard++ < STEPS.size) {
            val step = current(s) ?: break
            if (!step.done(s)) break
            val inv = step.rewardItem?.let { (k, n) -> s.inventory + (k to (s.inventory[k] ?: 0) + n) } ?: s.inventory
            s = s.copy(roadStep = s.roadStep + 1, spiritStones = s.spiritStones + step.rewardStones, inventory = inv)
            notes += "🧭 Droga Sekty: ${step.title} ✓ (+${step.rewardStones} kamieni" + (step.rewardItem?.let { ", ${it.second}× ${it.first}" } ?: "") + ")"
        }
        return s to notes
    }

    /** Pierwsze jajo: strażnik krainy zostawia je, gdy sekta nie ma jeszcze bestii ani jaja. */
    fun firstEgg(s: GameState): Pair<GameState, String?> {
        if (s.sectBeasts.isNotEmpty() || s.eggs.isNotEmpty() || (s.inventory[Creatures.EGG] ?: 0) > 0) return s to null
        return s.copy(inventory = s.inventory + (Creatures.EGG to 1)) to "🥚 W legowisku strażnika leży ciepłe jajo bestii! Wysiaduj je w Gnieździe Bestii."
    }

    fun markSeen(s: GameState, module: String): StepResult = ok(s.copy(seenModules = s.seenModules + module))
}
