package com.example.preview

import com.example.game.engine.Breath
import com.example.game.engine.CraftV2
import com.example.game.engine.Creatures
import com.example.game.engine.EventScripts
import com.example.game.engine.Expeditions
import com.example.game.engine.FlameSources
import com.example.game.engine.GameCommand
import com.example.game.engine.GameContent
import com.example.game.engine.GameEngine
import com.example.game.engine.GameState
import com.example.game.engine.Journal
import com.example.game.engine.Market
import com.example.game.engine.Secrets
import com.example.game.engine.SecretTools
import com.example.model.AncestorRecord
import com.example.model.BeastEgg
import com.example.model.Catastrophe
import com.example.model.CatastropheType
import com.example.model.Companionship
import com.example.model.DaoArt
import com.example.model.DailyGoal
import com.example.model.FieldPlot
import com.example.model.MajorRealm
import com.example.model.Raid
import com.example.model.SecretState
import com.example.model.SpiritElement
import com.example.model.SpyMission
import com.example.model.SpyType
import com.example.model.TimeOfDay

/**
 * Bogaty, deterministyczny stan gry do zrzutów ekranu: sekta w środku drugiej ery, z każdym systemem
 * w stanie, który ma coś do pokazania (najazd, katastrofa, wyprawa, tajemnice, płomienie, skarby…).
 */
object PreviewWorld {
    const val SEED = 20261027L

    fun build(): GameState {
        var s = GameEngine.newGame(SEED, nowMs = 1_000L)
        // Trochę prawdziwej gry: dni, zdarzenia, gospodarka.
        repeat(1_500) {
            s = GameEngine.apply(s, GameCommand.Tick).state
            if (s.activeEventId != null) s = GameEngine.apply(s, GameCommand.EventChoice(0)).state
        }
        val rules = s.rules!!
        val d = s.disciples
        val star = rules.constellations.first()
        val (method, opt) = rules.breath.entries.first()
        s = s.copy(
            currentDay = 412, timeOfDay = TimeOfDay.DUSK,
            spiritStones = 184_250, sectReputation = 6_420, karma = 34, morale = 78,
            inventory = s.inventory + mapOf(
                Market.SPIRIT_PAPER to 24, "Talizman Ognistej Kuli" to 3, "Talizman Pieczęci" to 1,
                Creatures.EGG to 1, SecretTools.MIRROR to 2, Breath.fragmentItem(method) to 3,
                GameContent.FLAME_CRYSTAL to 40, "Duchowy Nefryt" to 120, "Gwiezdne Żelazo" to 60,
                "Esencja: Duchowy" to 2, Expeditions.RING to 1
            ),
            flames = com.example.game.WuxiaGenerator.getGeocentricFlames().take(5).mapIndexed { i, f -> f.copy(refinementLevel = 3 + i) },
            flameSpirits = setOf("flame_5"),
            veinLevel = 4,
            fields = List(16) { i ->
                when {
                    i < 4 -> FieldPlot("Trawa Wiecznej Wiosny", 400, 410)
                    i < 7 -> FieldPlot("Śnieżny Lotos Oświecenia", 405, 418)
                    else -> FieldPlot()
                }
            },
            raid = Raid("sect_shadow", "Sekta Cienia", 414, 1_350),
            gates = mapOf("Brama Wschodnia" to d[0].id, "Brama Górska" to d[1].id),
            formationNodes = listOf(SpiritElement.FIRE, SpiritElement.WATER, SpiritElement.WOOD),
            spies = listOf(SpyMission("spy_1", d[3].id, "sect_shadow", SpyType.SCOUT, 414)),
            intel = mapOf("sect_dragon" to 400),
            catastrophe = Catastrophe(CatastropheType.QI_STORM, 405, 410, 419),
            towerFloor = 17, dungeonDepth = 6, caveLevel = 1,
            patronId = "p_moon", templeLevel = 2,
            treasury = listOf(GameContent.forgingRecipes.first().artifactResult),
            vaultArtifacts = s.vaultArtifacts + GameContent.forgingRecipes.drop(1).take(2).map { it.artifactResult.copy(plus = 6) },
            ancestors = listOf(
                AncestorRecord("Starszy Hu Wen", "Złoty Rdzeń", MajorRealm.CORE_FORMATION.ordinal, 288, "starość"),
                AncestorRecord("Mei Lan", "Fundamenty", MajorRealm.FOUNDATION.ordinal, 351, "tribulacja")
            ),
            companions = listOf(Companionship(d[1].id, d[4].id, 290, 1)),
            eggs = listOf(BeastEgg("egg_1", 418, s.sectBeasts.first().id)),
            mounts = mapOf(s.sectBeasts.first().id to d[0].id),
            chosenId = d[0].id,
            pvpRating = 1_148, pvpWins = 9, pvpLosses = 4,
            disciples = d.mapIndexed { i, x ->
                when (i) {
                    0 -> x.copy(majorRealm = MajorRealm.CORE_FORMATION, stage = 3, openedMeridians = (star.points + listOf(14, 15, 20, 21)).toSet(),
                        methodId = method, breath = opt.breath, meditationPlace = opt.place, art = DaoArt.GO, flameId = "flame_5", ageYears = 41)
                    1 -> x.copy(illUntilDay = 416, art = DaoArt.GUQIN, mentorId = d[0].id)
                    2 -> x.copy(secretState = SecretState.HALF_STEP, secretStateUntil = 430, cursed = true)
                    3 -> x.copy(ageYears = 116)
                    else -> x
                }
            },
            meta = s.meta.copy(
                era = 2, essence = 9, heavenTree = mapOf("qi_1" to 1, "qi_2" to 1, "qi_3" to 1, "bogactwo_1" to 1),
                achievements = setOf("wins_1", "wins_10", "realm_FOUNDATION", "realm_CORE_FORMATION", "tower_10", "stones_10000", "rep_1000", "constellation", "forms_1"),
                title = null, eraModifier = "storm", streak = 4, lastRealMs = 86_400_000L * 20_000, goalsDay = 20_000,
                goals = listOf(DailyGoal("win", 2, 2), DailyGoal("brew", 3, 1), DailyGoal("harvest", 1, 0)),
                codex = mapOf("Gwiazdozbiory" to 3, "Oddech" to 2), legacy = setOf(Secrets.CYCLE_EXCESS)
            ),
            flags = s.flags + mapOf("story_act" to 2, "story_seal" to 2, "imperial_favor" to 15)
        )
        s = s.copy(chronicle = listOf(
            "Dzień 401: Smocza Sekta odbiera Kopalnię Nefrytu Sekcie Wudang.",
            "Dzień 405: Obserwatorium zapowiada: Burza Qi.",
            "Dzień 409: Sekta Cienia wyrusza na sektę gracza.",
            "Dzień 410: Rozpoczyna się akt 2 opowieści o Demonie pod Górą."
        ), chronicleChapters = listOf("Rozdział 1 (dni 3–180): Sekta rośnie, pierwsze wyprawy, przybycie pustelnika."))
        // Dziennik: pewne, prawdopodobne i przypuszczenia z różnych kategorii.
        s = Journal.reveal(s, Secrets.constellation(star.id), "Rozbłysł u ucznia")
        s = Journal.reveal(s, Secrets.breath(method), "Zwój odszyfrowany")
        s = Journal.reveal(s, Secrets.CYCLE_EXCESS, "Księga Dziedzictwa")
        rules.forms.take(2).forEach { s = Journal.reveal(s, Secrets.form(it.id), "Sekwencja ${it.sequence}") }
        rules.lawObjects.keys.take(4).forEach { o -> repeat(3) { s = Journal.observe(s, Secrets.law(o), "Rezonans przy kontemplacji") } }
        rules.herbAttributes.entries.take(3).forEach { (h, a) -> s = Journal.observe(s, Secrets.herb(h, a.keys.first()), "Eksperyment z $h") }
        s = Journal.observe(s, Secrets.state(SecretState.HALF_STEP), "Zawieszony po trzech porażkach")
        // Trop płomienia i wyprawa w toku.
        s = FlameSources.onRegionExplored(s, "reg_2").first
        val region = s.regions.first()
        s = s.copy(expedition = com.example.model.ExpeditionRun(region.id, d[0].id, Expeditions.generate(s, region), layer = 2, hp = d[0].maxHp * 7 / 10,
            loot = mapOf(region.herbResource to 4, region.mineralResource to 3), stones = 260,
            log = listOf("${d[0].name} wyrusza do krainy ${region.name}.", "🌿 Polana ziół.", "⚔ Pokonano strażników krainy (4 rund).")))
        s = s.copy(discoveredRegionIds = s.discoveredRegionIds + s.regions.take(6).map { it.id }, realmVisits = emptyMap())
        // Aktywne zdarzenie fabuły do ekranu Omenu.
        s = EventScripts.activate(s, GameContent.script("story_2_2")!!)
        return s
    }

    /** Ta sama sekta bez aktywnego zdarzenia (do ekranów, na których okno zdarzenia by przeszkadzało). */
    fun calm(): GameState = build().copy(activeEventId = null)
}
