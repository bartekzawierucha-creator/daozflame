package com.example.game.engine

import com.example.model.DailyGoal
import com.example.model.Difficulty
import com.example.model.KnowledgeLevel
import com.example.model.MajorRealm
import com.example.model.MetaProgress
import kotlin.math.sqrt

/** Etap 8: Drzewo Niebios – 30 ulepszeń w 3 gałęziach po 10 poziomów, kupowanych Esencją. */
object HeavenTree {
    data class Node(val id: String, val branch: String, val tier: Int, val stat: Stat, val percent: Double) {
        val cost: Int get() = tier
    }

    val BRANCHES = listOf("Qi" to (Stat.QI_GAIN to 10.0), "Bogactwo" to (Stat.STONE_INCOME to 10.0), "Wojna" to (Stat.COMBAT_DAMAGE to 8.0))

    val NODES: List<Node> = BRANCHES.flatMap { (branch, sp) -> (1..10).map { t -> Node("${branch.lowercase()}_$t", branch, t, sp.first, sp.second) } }

    fun owned(meta: MetaProgress, id: String) = (meta.heavenTree[id] ?: 0) > 0

    fun buy(s: GameState, id: String): StepResult {
        val n = NODES.find { it.id == id } ?: return ok(s)
        if (owned(s.meta, id)) return fail(s, "Ten węzeł już świeci.")
        val prev = NODES.find { it.branch == n.branch && it.tier == n.tier - 1 }
        if (prev != null && !owned(s.meta, prev.id)) return fail(s, "Najpierw rozbudź poprzedni węzeł gałęzi ${n.branch}.")
        if (s.meta.essence < n.cost) return fail(s, "Węzeł kosztuje ${n.cost} Esencji Niebios.")
        var st = s.copy(meta = s.meta.copy(essence = s.meta.essence - n.cost, heavenTree = s.meta.heavenTree + (id to 1)))
        // Węzeł kupiony w pierwszym dniu ery od razu daje swoją premię startową.
        if (s.currentDay <= 1) st = startBonus(st, n.branch, 1)
        return ok(st, "🌳 Drzewo Niebios: ${n.branch} ${n.tier} (+${n.percent.toInt()}% ${n.stat.label}).")
    }

    /** Premie startowe ery: Bogactwo – kamienie, Qi i Wojna – silniejsi uczniowie. */
    fun startBonus(s: GameState, branch: String, tiers: Int): GameState {
        if (tiers <= 0) return s
        return if (branch == "Bogactwo") s.copy(spiritStones = s.spiritStones + Rebirth.START_STONES_PER_TIER * tiers)
        else {
            val f = 1 + Rebirth.START_STATS_PER_TIER * tiers
            s.copy(disciples = s.disciples.map { d -> d.copy(attack = (d.attack * f).toInt(), defense = (d.defense * f).toInt(), maxHp = (d.maxHp * f).toInt(), currentHp = (d.maxHp * f).toInt()) })
        }
    }

    fun modifiers(meta: MetaProgress): List<Modifier> = BRANCHES.mapNotNull { (branch, sp) ->
        val tiers = NODES.count { it.branch == branch && owned(meta, it.id) }
        if (tiers == 0) null else Modifier("Drzewo Niebios: $branch $tiers", sp.first, ModOp.PERCENT, sp.second * tiers)
    }
}

/** Modyfikatory ery – jeden wybór na erę. */
object EraModifiers {
    data class Era(val id: String, val name: String, val description: String, val mods: List<Modifier>, val raidPercent: Double = 0.0)

    val ALL = listOf(
        Era("storm", "Era Burz", "+20% Qi, ale najazdy są o 30% silniejsze.", listOf(Modifier("Era Burz", Stat.QI_GAIN, ModOp.PERCENT, 20.0)), 30.0),
        Era("peace", "Era Pokoju", "Najazdy słabsze o 30%, ale −10% obrażeń.", listOf(Modifier("Era Pokoju", Stat.COMBAT_DAMAGE, ModOp.PERCENT, -10.0)), -30.0),
        Era("gold", "Era Złota", "+30% dochodu kamieni, −10% Qi.", listOf(Modifier("Era Złota", Stat.STONE_INCOME, ModOp.PERCENT, 30.0), Modifier("Era Złota", Stat.QI_GAIN, ModOp.PERCENT, -10.0))),
        Era("insight", "Era Olśnień", "+50% szansy olśnienia, −15% dochodu.", listOf(Modifier("Era Olśnień", Stat.EPIPHANY_CHANCE, ModOp.PERCENT, 50.0), Modifier("Era Olśnień", Stat.STONE_INCOME, ModOp.PERCENT, -15.0))),
        Era("war", "Era Wojny", "+20% obrażeń i reputacji, najazdy +15%.", listOf(Modifier("Era Wojny", Stat.COMBAT_DAMAGE, ModOp.PERCENT, 20.0), Modifier("Era Wojny", Stat.REPUTATION_GAIN, ModOp.PERCENT, 20.0)), 15.0)
    )

    fun of(meta: MetaProgress): Era? = ALL.find { it.id == meta.eraModifier }

    fun choose(s: GameState, id: String): StepResult {
        if (s.meta.eraModifier != null) return fail(s, "Ta era ma już swój znak.")
        val e = ALL.find { it.id == id } ?: return ok(s)
        return ok(s.copy(meta = s.meta.copy(eraModifier = id)), "Niebo nadaje erze znak: ${e.name}.")
    }
}

/** Odrodzenie sekty: Esencja = √(siła)/100, nowa gra z nowym ziarnem, przetrwa tylko [MetaProgress]. */
object Rebirth {
    const val MIN_DAY = 1_200
    const val SUCCESSION_DAY = 120
    const val START_STONES_PER_TIER = 10_000
    const val START_STATS_PER_TIER = 0.06

    fun power(s: GameState): Long =
        s.spiritStones.toLong() + 50L * s.sectReputation + 50L * s.disciples.sumOf { it.combatPower } + if (Story.ending(s) > 0) 1_000_000L else 0L

    fun essence(s: GameState): Int = (sqrt(power(s).coerceAtLeast(0).toDouble()) / 100).toInt()

    fun available(s: GameState): Boolean = Story.ending(s) > 0 || s.currentDay >= MIN_DAY || s.disciples.any { (it.majorRealm?.ordinal ?: 0) >= MajorRealm.MAHAYANA.ordinal }

    /** Uniwersalne prawa świata (nie zależą od ziarna) – przechodzą przez Księgę Dziedzictwa. */
    val UNIVERSAL = listOf(Secrets.CYCLE_EXCESS, Secrets.CYCLE_REBELLION, Secrets.CYCLE_YINYANG) + com.example.model.SecretState.entries.map { Secrets.state(it) }

    fun newSeed(s: GameState): Long = s.worldSeed * 31 + s.meta.era + 7

    fun rebirth(s: GameState, confirmed: Boolean, difficulty: Difficulty?, ironWill: Boolean?): StepResult {
        if (!available(s)) return fail(s, "Odrodzenie możliwe po zakończeniu opowieści, po $MIN_DAY dniach albo gdy uczeń sięgnie Mahajany.")
        if (!confirmed) return fail(s, "Odrodzenie zeruje sektę (zostaje Drzewo Niebios, Księga Dziedzictwa, osiągnięcia). Potwierdź.")
        val gained = essence(s)
        val known = Rebirth.UNIVERSAL.filter { Journal.certain(s, it) }
        val codex = JournalView.entries(s).filter { it.level == KnowledgeLevel.CERTAIN }.groupingBy { it.category }.eachCount()
        val meta = s.meta.copy(
            era = s.meta.era + 1,
            essence = s.meta.essence + gained,
            legacy = s.meta.legacy + known,
            codex = (s.meta.codex.keys + codex.keys).associateWith { maxOf(s.meta.codex[it] ?: 0, codex[it] ?: 0) },
            eraModifier = null,
            difficulty = difficulty ?: s.meta.difficulty,
            ironWill = ironWill ?: s.meta.ironWill,
            stats = s.meta.stats + ("eras" to (s.meta.stats["eras"] ?: 0) + 1)
        )
        val fresh = start(GameEngine.newGame(newSeed(s), s.lastActiveAtMs), meta)
        return ok(fresh, "🌅 Era ${meta.era} się zaczyna. Zebrano $gained Esencji Niebios.")
    }

    /** Nowa era: nowa gra + meta; Księga Dziedzictwa wpisuje uniwersalne prawa jako Pewne. */
    fun start(newGame: GameState, meta: MetaProgress): GameState {
        var s = newGame.copy(meta = meta, sectMasterName = meta.heirName?.let { "Mistrz $it" } ?: newGame.sectMasterName)
        // Premie startowe z Drzewa Niebios (Bogactwo – kamienie, Qi i Wojna – silniejsi uczniowie).
        HeavenTree.BRANCHES.forEach { (branch, _) ->
            s = HeavenTree.startBonus(s, branch, HeavenTree.NODES.count { it.branch == branch && HeavenTree.owned(meta, it.id) })
        }
        meta.legacy.forEach { s = Journal.reveal(s, it, "Księga Dziedzictwa") }
        return s
    }

    /** Sukcesja: wybór następcy; pominięci ambitni mogą odejść. Raz na erę, po pierwszym roku. */
    fun succession(s: GameState, ctx: EngineContext, discipleId: String, confirmed: Boolean): StepResult {
        val heir = s.disciple(discipleId) ?: return ok(s)
        if (s.currentDay < SUCCESSION_DAY) return fail(s, "Sukcesję ogłasza się po pierwszym roku ery.")
        if (s.meta.heirName != null && s.flags["succession_done"] == 1) return fail(s, "Następca tej ery został już wybrany.")
        if (!confirmed) return fail(s, "Pominięci ambitni uczniowie mogą odejść. Potwierdź wybór.")
        val rng = ctx.rng("succession")
        val passedOver = s.disciples.filter { it.id != heir.id && it.combatPower > heir.combatPower && it.loyalty < 70 }
        val leaving = passedOver.filter { rng.nextBoolean() }.map { it.id }.toSet()
        val st = s.copy(
            disciples = s.disciples.filter { it.id !in leaving }.map { if (it.id == heir.id) it.copy(loyalty = 100) else it },
            gates = s.gates.filterValues { it !in leaving },
            sectMasterName = "Mistrz ${heir.name.substringBefore(" (")}",
            flags = s.flags + ("succession_done" to 1),
            meta = s.meta.copy(heirName = heir.name.substringBefore(" ("))
        )
        val left = if (leaving.isEmpty()) "" else " ${leaving.size} pominiętych opuszcza sektę."
        return ok(WorldAI.note(st, "${heir.name} zostaje następcą patriarchy."), "👑 ${heir.name} zostaje następcą.$left")
    }

    fun modifiers(s: GameState): List<Modifier> = buildList {
        addAll(HeavenTree.modifiers(s.meta))
        EraModifiers.of(s.meta)?.let { addAll(it.mods) }
        s.meta.heirName?.let { add(Modifier("Dziedzictwo: $it", Stat.QI_GAIN, ModOp.PERCENT, 10.0)) }
        if (s.meta.difficulty.incomePercent != 0.0) add(Modifier(s.meta.difficulty.polishName, Stat.STONE_INCOME, ModOp.PERCENT, s.meta.difficulty.incomePercent))
    }

    fun raidFactor(s: GameState): Double = 1 + (s.meta.difficulty.raidPercent + (EraModifiers.of(s.meta)?.raidPercent ?: 0.0)) / 100
}

/** Osiągnięcia (60) sprawdzane przez zdarzenia silnika – tylko gdy zaszło coś, co może je odblokować. */
object Achievements {
    enum class Trigger { BREAKTHROUGH, COMBAT, DAY, TOWER, DUNGEON, MERIDIAN, UPGRADE, STORY, REBIRTH, FUSION, MOUNT }

    data class Def(val id: String, val name: String, val description: String, val trigger: Trigger, val secret: Boolean = false, val title: Boolean = false, val test: (GameState) -> Boolean)

    private fun stat(s: GameState, k: String) = s.meta.stats[k] ?: 0L

    val ALL: List<Def> = buildList {
        MajorRealm.entries.filter { it.ordinal >= MajorRealm.FOUNDATION.ordinal }.forEach { r ->
            add(Def("realm_${r.name}", "Pierwszy w: ${r.polishName}", "Uczeń sekty wkracza w ${r.polishName}.", Trigger.BREAKTHROUGH, title = r.ordinal >= MajorRealm.NASCENT_SOUL.ordinal) { s -> s.disciples.any { (it.majorRealm?.ordinal ?: 0) >= r.ordinal } })
        }
        listOf(1, 10, 50, 100, 500).forEach { n -> add(Def("wins_$n", "Zwycięstwa: $n", "Wygraj $n walk.", Trigger.COMBAT, title = n == 500) { s -> stat(s, "wins") >= n }) }
        listOf(10, 50, 200).forEach { n -> add(Def("bt_$n", "Przełomy: $n", "Dokonaj $n przełomów.", Trigger.BREAKTHROUGH) { s -> stat(s, "breakthroughs") >= n }) }
        listOf(1, 10).forEach { n -> add(Def("trib_$n", "Pod piorunami: $n", "Przetrwaj $n tribulacji.", Trigger.BREAKTHROUGH) { s -> stat(s, "tribulations") >= n }) }
        listOf(10, 25, 50, 100).forEach { n -> add(Def("tower_$n", "Wieża Prób: $n", "Zdobądź piętro $n.", Trigger.TOWER, title = n == 100) { s -> s.towerFloor >= n }) }
        listOf(10, 25).forEach { n -> add(Def("dungeon_$n", "Głębiny: $n", "Zejdź na piętro $n lochów.", Trigger.DUNGEON) { s -> s.dungeonDepth >= n }) }
        listOf(10_000, 100_000, 1_000_000, 10_000_000).forEach { n -> add(Def("stones_$n", "Skarbiec: $n", "Zgromadź $n kamieni.", Trigger.DAY) { s -> s.spiritStones >= n }) }
        listOf(1_000, 10_000, 50_000).forEach { n -> add(Def("rep_$n", "Sława: $n", "Osiągnij $n reputacji.", Trigger.DAY, title = n == 50_000) { s -> s.sectReputation >= n }) }
        listOf(10, 20, 30).forEach { n -> add(Def("disciples_$n", "Wielka sekta: $n", "Miej $n uczniów.", Trigger.DAY) { s -> s.disciples.size >= n }) }
        listOf(10, 50, 100, 200).forEach { n -> add(Def("secrets_$n", "Badacz Dao: $n", "Poznaj na pewno $n tajemnic.", Trigger.DAY, title = n == 200) { s -> Journal.discovered(s) >= n }) }
        add(Def("constellation", "Pierwsza gwiazda", "Rozbudź gwiazdozbiór w meridianach.", Trigger.MERIDIAN) { s -> s.journal.keys.any { it.startsWith("const:") && Journal.certain(s, it) } })
        listOf(1, 10).forEach { n -> add(Def("forms_$n", "Formy: $n", "Odkryj $n form walki.", Trigger.COMBAT) { s -> s.journal.count { it.key.startsWith("form:") && it.value.level == KnowledgeLevel.CERTAIN } >= n }) }
        listOf(3, 6, 12).forEach { n -> add(Def("flames_$n", "Płomienie: $n", "Posiadaj $n płomieni.", Trigger.DAY, title = n == 12) { s -> s.flames.size >= n }) }
        add(Def("plus10", "Arcydzieło", "Ulepsz artefakt do +10.", Trigger.UPGRADE) { s -> s.vaultArtifacts.any { it.plus >= 10 } })
        listOf(1, 10).forEach { n -> add(Def("raids_$n", "Mury sekty: $n", "Odeprzyj $n najazdów.", Trigger.DAY) { s -> stat(s, "raids_repelled") >= n }) }
        add(Def("ending_1", "Pieczęć Światła", "Zakończ opowieść światłem.", Trigger.STORY, title = true) { s -> Story.ending(s) == 1 })
        add(Def("ending_2", "Pakt z Demonem", "???", Trigger.STORY, secret = true, title = true) { s -> Story.ending(s) == 2 })
        add(Def("ending_3", "Strażnik Równowagi", "Zakończ opowieść, odnawiając pieczęć.", Trigger.STORY, title = true) { s -> Story.ending(s) == 3 })
        listOf(2, 3, 5).forEach { n -> add(Def("era_$n", "Era $n", "Odródź sektę i dożyj ery $n.", Trigger.REBIRTH, title = n == 5) { s -> s.meta.era >= n }) }
        add(Def("vein_9", "Smocza Żyła", "Pogłęb żyłę do poziomu 9.", Trigger.DAY) { s -> s.veinLevel >= 9 })
        add(Def("formation_7", "Wielka Formacja", "Wbij wszystkie 7 węzłów formacji.", Trigger.DAY) { s -> s.formationNodes.size >= Formation.MAX_NODES })
        add(Def("treasury", "Skarbiec pełen", "Wypełnij 5 miejsc skarbca sekty.", Trigger.DAY) { s -> s.treasury.size >= Identity.TREASURE_SLOTS })
        add(Def("ancestors_10", "Długi rodowód", "10 tablic w Sali Przodków.", Trigger.DAY) { s -> s.ancestors.size >= 10 })
        add(Def("child", "Dziecko sekty", "???", Trigger.DAY, secret = true) { s -> s.companions.any { it.children > 0 } })
        add(Def("fusion", "Kowal płomieni", "???", Trigger.FUSION, secret = true) { s -> (s.meta.stats["fusions"] ?: 0) > 0 })
        add(Def("mount", "Jeździec bestii", "???", Trigger.MOUNT, secret = true) { s -> s.mounts.isNotEmpty() })
    }

    fun triggers(command: GameCommand, before: GameState, after: GameState, events: List<GameEvent>): Set<Trigger> = buildSet {
        if (events.any { it is GameEvent.Breakthrough || it is GameEvent.Tribulation }) add(Trigger.BREAKTHROUGH)
        if (events.any { it is GameEvent.CombatEnded } || command is GameCommand.CombatManualAttack || command is GameCommand.CombatUltimate) add(Trigger.COMBAT)
        if (after.currentDay != before.currentDay) { add(Trigger.DAY); add(Trigger.BREAKTHROUGH) }
        when (command) {
            is GameCommand.ClimbTower -> add(Trigger.TOWER)
            is GameCommand.DelveDungeon -> add(Trigger.DUNGEON)
            is GameCommand.OpenMeridian -> add(Trigger.MERIDIAN)
            is GameCommand.UpgradeArtifact -> add(Trigger.UPGRADE)
            is GameCommand.EventChoice -> add(Trigger.STORY)
            is GameCommand.Rebirth -> add(Trigger.REBIRTH)
            is GameCommand.FuseFlames -> add(Trigger.FUSION)
            is GameCommand.MountBeast -> add(Trigger.MOUNT)
            else -> Unit
        }
    }

    /** Liczniki z zdarzeń + odblokowanie osiągnięć tylko dla zaistniałych wyzwalaczy. */
    fun after(command: GameCommand, before: GameState, result: StepResult): StepResult {
        if (!com.example.game.FeatureFlags.META) return result
        var s = result.state
        val ev = result.events
        val stats = s.meta.stats.toMutableMap()
        fun inc(k: String, n: Long = 1) { stats[k] = (stats[k] ?: 0) + n }
        ev.filterIsInstance<GameEvent.CombatEnded>().forEach { inc(if (it.victory) "wins" else "losses") }
        ev.filterIsInstance<GameEvent.Breakthrough>().filter { it.success }.forEach { inc("breakthroughs") }
        ev.filterIsInstance<GameEvent.Tribulation>().filter { it.result.survived }.forEach { inc("tribulations") }
        ev.filterIsInstance<GameEvent.Notice>().count { it.message.startsWith("🛡 Najazd") }.takeIf { it > 0 }?.let { inc("raids_repelled", it.toLong()) }
        if (command is GameCommand.FuseFlames && s.flames.size == before.flames.size - 1) inc("fusions")
        if (stats != s.meta.stats) s = s.copy(meta = s.meta.copy(stats = stats))
        s = DailyGoals.progress(command, before, s, ev)
        val triggers = triggers(command, before, s, ev)
        if (triggers.isEmpty()) return result.copy(state = s)
        val fresh = ALL.filter { it.id !in s.meta.achievements && it.trigger in triggers && it.test(s) }
        if (fresh.isEmpty()) return result.copy(state = s)
        s = s.copy(meta = s.meta.copy(achievements = s.meta.achievements + fresh.map { it.id }))
        // Komunikaty osiągnięć przed zdarzeniami kroku – główny komunikat akcji zostaje ostatni.
        return StepResult(s, fresh.map { GameEvent.Notice("🏅 Osiągnięcie: ${it.name}") } + ev)
    }

    fun setTitle(s: GameState, id: String?): StepResult {
        if (id == null) return ok(s.copy(meta = s.meta.copy(title = null)))
        val a = ALL.find { it.id == id && it.title } ?: return ok(s)
        if (id !in s.meta.achievements) return fail(s, "Ten tytuł trzeba najpierw zdobyć.")
        return ok(s.copy(meta = s.meta.copy(title = a.name)), "Tytuł patriarchy: ${a.name}.")
    }
}

/**
 * Cele dzienne i Ofiara przodkom – JEDYNE miejsce z czasem realnym. Czas przychodzi jako parametr komendy;
 * cofnięcie zegara względem ostatniego znanego czasu jest ignorowane.
 */
object DailyGoals {
    const val DAY_MS = 86_400_000L
    const val CLOCK_TOLERANCE_MS = 5 * 60_000L
    const val CYCLE = 7
    const val OFFERING_COST = 200
    val KINDS = mapOf(
        "win" to ("Wygraj walkę" to 2), "brew" to ("Uwarz pigułki" to 3), "breakthrough" to ("Dokonaj przełomu" to 2),
        "tower" to ("Zdobądź piętro Wieży" to 1), "expedition" to ("Wyrusz na wyprawę" to 1), "contemplate" to ("Kontempluj" to 2),
        "harvest" to ("Zbierz plony" to 1)
    )

    fun realTime(s: GameState, nowMs: Long): StepResult {
        val m = s.meta
        if (m.lastRealMs > 0 && nowMs < m.lastRealMs - CLOCK_TOLERANCE_MS) return ok(s) // zegar cofnięty – brak postępu
        val day = nowMs / DAY_MS
        var meta = m.copy(lastRealMs = maxOf(m.lastRealMs, nowMs))
        if (day > m.goalsDay) {
            val rng = GameRng.stream(s.worldSeed, day, "daily-goals")
            meta = meta.copy(goalsDay = day, goals = KINDS.keys.shuffled(rng).take(3).map { DailyGoal(it, KINDS.getValue(it).second) })
        }
        return ok(s.copy(meta = meta))
    }

    fun progress(command: GameCommand, before: GameState, s: GameState, events: List<GameEvent>): GameState {
        if (s.meta.goals.isEmpty() || s.meta.goals.all { it.done }) return s
        val hits = buildMap<String, Int> {
            events.filterIsInstance<GameEvent.CombatEnded>().count { it.victory }.takeIf { it > 0 }?.let { put("win", it) }
            events.filterIsInstance<GameEvent.Breakthrough>().count { it.success }.takeIf { it > 0 }?.let { put("breakthrough", it) }
            if (command is GameCommand.BrewPill && Quality.count(s.inventory, GameContent.recipe(command.recipeId)?.pillName ?: "") > Quality.count(before.inventory, GameContent.recipe(command.recipeId)?.pillName ?: "")) put("brew", 1)
            if (command is GameCommand.ClimbTower && s.towerFloor > before.towerFloor) put("tower", 1)
            if ((command is GameCommand.BeginExpedition || command is GameCommand.StartExpedition) && (s.expedition != null || s.combat != null)) put("expedition", 1)
            if (command is GameCommand.Contemplate && s.journal != before.journal) put("contemplate", 1)
            if (command is GameCommand.HarvestFields && s.fields != before.fields) put("harvest", 1)
        }
        if (hits.isEmpty()) return s
        val goals = s.meta.goals.map { g -> hits[g.kind]?.let { g.copy(progress = (g.progress + it).coerceAtMost(g.target)) } ?: g }
        var st = s.copy(meta = s.meta.copy(goals = goals))
        if (goals.all { it.done } && !s.meta.goals.all { it.done }) {
            val pos = st.meta.streak % CYCLE + 1
            val inv = if (pos == CYCLE) st.inventory + (GameContent.BREAKTHROUGH_PILL to (st.inventory[GameContent.BREAKTHROUGH_PILL] ?: 0) + 3) else st.inventory
            st = st.copy(
                spiritStones = st.spiritStones + 300 * pos,
                inventory = inv,
                meta = st.meta.copy(streak = st.meta.streak + 1, essence = st.meta.essence + if (pos == CYCLE) 1 else 0)
            )
        }
        return st
    }

    fun offering(s: GameState): StepResult {
        val day = s.meta.lastRealMs / DAY_MS
        if (s.meta.lastRealMs == 0L) return fail(s, "Przodkowie czekają na właściwą chwilę.")
        if (s.meta.offeringDay == day) return fail(s, "Dziś złożono już ofiarę przodkom.")
        if (s.spiritStones < OFFERING_COST) return fail(s, "Ofiara wymaga $OFFERING_COST kamieni.")
        return ok(s.copy(spiritStones = s.spiritStones - OFFERING_COST, morale = (s.morale + 5).coerceAtMost(100), sectReputation = s.sectReputation + 10,
            meta = s.meta.copy(offeringDay = day)), "🕯 Kadzidło płonie w Sali Przodków. Sekta czuje ich spojrzenie.")
    }
}
