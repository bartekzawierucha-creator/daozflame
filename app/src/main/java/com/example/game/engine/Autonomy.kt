package com.example.game.engine

import com.example.game.content.ResourceCatalog
import com.example.game.engine.DiscipleRules.has
import com.example.model.Disciple
import com.example.model.DiscipleRole
import kotlin.random.Random

/**
 * Etap 10: uczniowie żyją sami. Każdego dnia wybierają zajęcie, decydują o wielkim przełomie i czasem
 * podejmują wyzwanie (wieża, wyprawa, odosobnienie, kontemplacja). Mistrz nie rozkazuje – doradza:
 * rada przechodzi przez [advise], a posłuszeństwo zależy od lojalności, charakteru i zaufania ([Disciple.trust]),
 * które rośnie, gdy rady okazują się trafne.
 *
 * Rozkazami mistrza pozostają: rekrutacja, awans/kara/ułaskawienie, leczenie i dary, obrona bramy, bitwy sekty, sukcesja.
 */
object Autonomy {
    const val BASE_OBEDIENCE = 40
    const val NAG_PENALTY = 15
    const val COMMIT_DAYS = 10
    const val HOLD_DAYS = 10
    const val VENTURE_COOLDOWN = 8
    const val ROLE_INERTIA = 12
    const val DIARY_SIZE = 8

    enum class Kind { DANGER, WORK, STUDY, BREAKTHROUGH, CAUTION, REST }

    // --- Migracja ---

    fun normalize(s: GameState): GameState {
        if (s.autonomy || !com.example.game.FeatureFlags.AUTONOMY) return s
        return s.copy(autonomy = true, disciples = s.disciples.map { it.copy(trust = (40 + it.loyalty / 4).coerceIn(0, 100)) })
    }

    // --- Pomocnicze ---

    fun short(d: Disciple): String = d.name.substringBefore(" (")
    private fun Disciple.v(m: String, f: String) = if (gender == "Kobieta") f else m
    private fun Disciple.note(day: Int, text: String) = copy(diary = (diary + "Dz. $day: $text").takeLast(DIARY_SIZE))
    private fun GameState.update(id: String, f: (Disciple) -> Disciple) = copy(disciples = disciples.map { if (it.id == id) f(it) else it })
    private fun hurt(s: GameState, d: Disciple) = d.injuredUntilDay >= s.currentDay || d.illUntilDay >= s.currentDay || d.currentHp < d.maxHp / 2
    private fun hasPill(s: GameState) = s.inventory.any { (k, v) -> v > 0 && k.startsWith(GameContent.BREAKTHROUGH_PILL) }

    // --- Rady ---

    /** Uczeń, którego dotyczy komenda. null = rozkaz mistrza (nie podlega autonomii). */
    fun subject(c: GameCommand): String? = when (c) {
        is GameCommand.AssignRole -> c.discipleId
        is GameCommand.UnlockSkill -> c.discipleId
        is GameCommand.ChoosePath -> c.discipleId
        is GameCommand.LearnMethod -> c.discipleId
        is GameCommand.ChooseLaw -> c.discipleId
        is GameCommand.Seclude -> c.discipleId
        is GameCommand.EndSeclusion -> c.discipleId
        is GameCommand.AwakenBloodline -> c.discipleId
        is GameCommand.Ascend -> c.discipleId
        is GameCommand.AttemptBreakthrough -> c.discipleId
        is GameCommand.BeginExpedition -> c.discipleId
        is GameCommand.StartExpedition -> c.discipleId
        is GameCommand.ClimbTower -> c.discipleId
        is GameCommand.ScoutRegion -> c.discipleId
        is GameCommand.EnterSecretRealm -> c.discipleId
        is GameCommand.OpenMeridian -> c.discipleId
        is GameCommand.SetBreath -> c.discipleId
        is GameCommand.Contemplate -> c.discipleId
        is GameCommand.DelveDungeon -> c.discipleId
        is GameCommand.AssignArt -> c.discipleId
        is GameCommand.SendSpy -> c.discipleId
        is GameCommand.HonorDuel -> c.discipleId.takeIf { !c.toDeath || c.confirmed }
        is GameCommand.CaptureTerritory -> c.discipleId
        is GameCommand.AttackWorldBoss -> c.discipleId
        is GameCommand.ArenaDuel -> c.discipleId
        is GameCommand.AssignGarrison -> c.discipleId
        is GameCommand.SetResearcher -> c.discipleId
        is GameCommand.Hold -> c.discipleId
        else -> null
    }

    fun kind(c: GameCommand): Kind = when (c) {
        is GameCommand.AssignRole -> if (c.role == DiscipleRole.MEDITATING || c.role == DiscipleRole.COMBAT_TRAINING) Kind.STUDY else Kind.WORK
        is GameCommand.AttemptBreakthrough, is GameCommand.Ascend -> Kind.BREAKTHROUGH
        is GameCommand.EndSeclusion -> Kind.REST
        is GameCommand.Hold -> Kind.CAUTION
        is GameCommand.AssignGarrison, is GameCommand.SetResearcher -> Kind.WORK
        is GameCommand.BeginExpedition, is GameCommand.StartExpedition, is GameCommand.ClimbTower, is GameCommand.ScoutRegion,
        is GameCommand.EnterSecretRealm, is GameCommand.DelveDungeon, is GameCommand.SendSpy, is GameCommand.HonorDuel,
        is GameCommand.CaptureTerritory, is GameCommand.AttackWorldBoss, is GameCommand.ArenaDuel -> Kind.DANGER
        else -> Kind.STUDY
    }

    /** Krótki opis rady (do komunikatów i dziennika). */
    fun label(c: GameCommand): String = when (c) {
        is GameCommand.AssignRole -> c.role.polishName
        is GameCommand.AttemptBreakthrough -> "próba przełomu"
        is GameCommand.Ascend -> "wniebowstąpienie"
        is GameCommand.Seclude -> "odosobnienie"
        is GameCommand.EndSeclusion -> "przerwanie odosobnienia"
        is GameCommand.ChoosePath -> "wybór ścieżki"
        is GameCommand.LearnMethod -> "nauka metody"
        is GameCommand.ChooseLaw -> "wybór Prawa"
        is GameCommand.AwakenBloodline -> "przebudzenie krwi"
        is GameCommand.UnlockSkill -> "nowa technika"
        is GameCommand.BeginExpedition, is GameCommand.StartExpedition -> "wyprawa"
        is GameCommand.ClimbTower -> "Wieża Prób"
        is GameCommand.ScoutRegion -> "zwiad"
        is GameCommand.EnterSecretRealm -> "tajne królestwo"
        is GameCommand.OpenMeridian -> "otwarcie meridianu"
        is GameCommand.SetBreath -> "technika oddechu"
        is GameCommand.Contemplate -> "kontemplacja"
        is GameCommand.DelveDungeon -> "zejście do lochów"
        is GameCommand.AssignArt -> "sztuka Dao"
        is GameCommand.SendSpy -> "misja szpiegowska"
        is GameCommand.HonorDuel -> "pojedynek honorowy"
        is GameCommand.CaptureTerritory -> "podbój terytorium"
        is GameCommand.AttackWorldBoss -> "walka z bestią świata"
        is GameCommand.ArenaDuel -> "arena"
        is GameCommand.AssignGarrison -> "garnizon"
        is GameCommand.SetResearcher -> "badania"
        is GameCommand.Hold -> "wstrzymać się z ryzykiem"
        else -> "rada"
    }

    /** Składniki posłuszeństwa: (powód, punkty). Ujemne składniki tłumaczą odmowę. */
    fun factors(s: GameState, d: Disciple, c: GameCommand?): List<Pair<String, Int>> {
        val f = mutableListOf<Pair<String, Int>>()
        f += "loyalty" to (d.loyalty - 50) / 2
        f += "trust" to (d.trust - 50) * 7 / 10
        if (d.has("loyal")) f += "loyal" to 15
        if (d.has("humble")) f += "humble" to 8
        if (d.has("hotheaded")) f += "hotheaded" to -10
        if (d.has("traitor")) f += "traitor" to -15
        if (d.adviceDay == s.currentDay && d.adviceCount > 0) f += "nag" to -NAG_PENALTY * d.adviceCount
        if (c == null) return f
        val trib = CultivationSystem.tribulationAhead(d)
        when (kind(c)) {
            Kind.DANGER -> {
                if (d.has("fearless")) f += "fearless" to 20
                if (d.has("hotheaded")) f += "bold" to 15
                if (d.has("ambitious")) f += "ambitious" to 10
                if (d.has("coward")) f += "coward" to -35
                if (d.currentHp < d.maxHp * 6 / 10 || d.injuredUntilDay >= s.currentDay) f += "wounded" to -25
            }
            Kind.WORK -> {
                if (d.has("gatherer") && (c as? GameCommand.AssignRole)?.role == DiscipleRole.HERB_GATHERING) f += "gatherer" to 20
                if (d.has("greedy") && (c as? GameCommand.AssignRole)?.role == DiscipleRole.MINING) f += "greedy" to 10
                if (d.has("diligent")) f += "diligent" to 10
                if (d.has("lazy")) f += "lazy" to -20
                if (d.has("ambitious")) f += "ambitious" to -15
            }
            Kind.STUDY -> {
                if (d.has("insightful")) f += "insightful" to 15
                if (d.has("diligent")) f += "diligent" to 10
                if (d.has("ambitious")) f += "ambitious" to 5
                if (d.has("lazy")) f += "lazy" to -10
            }
            Kind.BREAKTHROUGH -> {
                if (d.has("ambitious")) f += "ambitious" to 20
                if (d.has("fearless")) f += "fearless" to 10
                if (d.has("coward")) f += "coward" to if (trib) -30 else -10
                if (trib && d.currentHp < d.maxHp) f += "wounded" to -20
            }
            Kind.CAUTION -> {
                if (d.has("coward")) f += "coward" to 25
                if (d.has("lazy")) f += "lazy" to 10
                if (d.has("fearless")) f += "fearless" to -15
                if (d.has("ambitious")) f += "ambitious" to -15
                if (d.has("hotheaded")) f += "bold" to -10
            }
            Kind.REST -> {
                f += "interrupt" to -20
                if (d.has("lazy")) f += "lazy" to 10
            }
        }
        if (c is GameCommand.AssignRole) {
            if (c.role == d.role) f += "wish" to 40 else if (c.role == preferredRole(s, d)) f += "wish" to 25
        }
        return f
    }

    /** Szansa (%), że uczeń posłucha tej rady. [c] = null → ogólne posłuszeństwo. */
    fun chance(s: GameState, d: Disciple, c: GameCommand? = null): Int =
        (BASE_OBEDIENCE + factors(s, d, c).sumOf { it.second }).coerceIn(5, 95)

    private fun refusal(s: GameState, d: Disciple, c: GameCommand): String {
        val worst = factors(s, d, c).filter { it.second < 0 }.minByOrNull { it.second }?.first
        return when (worst) {
            "loyalty" -> "Nie czuję, bym był${d.v("", "a")} ci winn${d.v("y", "a")} posłuszeństwo, mistrzu."
            "trust" -> "Twoje ostatnie rady nie przyniosły mi szczęścia."
            "hotheaded", "bold" -> "Sam${d.v("", "a")} wiem najlepiej!"
            "traitor" -> "Mam własne plany."
            "coward" -> "To zbyt niebezpieczne, mistrzu… nie jestem gotow${d.v("y", "a")}."
            "wounded" -> "Rany jeszcze się nie zagoiły."
            "lazy" -> "Może jutro…"
            "ambitious" -> if (kind(c) == Kind.CAUTION) "Nie po to kultywuję, by czekać." else "Marnował${d.v("bym", "abym")} talent na takie zajęcia."
            "fearless" -> "Strach to nie moja droga."
            "interrupt" -> "Przerwanie odosobnienia rozerwałoby mi meridiany."
            "nag" -> "Mistrzu, słyszał${d.v("em", "am")} cię już dziś."
            else -> "Mam inny zamiar."
        }
    }

    /** Wynik rady dla zaufania: trafna rada buduje więź, chybiona ją nadweręża. */
    private fun outcome(events: List<GameEvent>, id: String): Int {
        var sum = 0
        for (e in events) sum += when (e) {
            is GameEvent.Breakthrough -> if (e.discipleId != id) 0 else if (e.success) 6 else -6
            is GameEvent.CombatEnded -> if (e.victory) 4 else -4
            else -> 0
        }
        return sum
    }

    /** Rada mistrza: uczeń rozważa ją i albo wykonuje [inner] przez [run], albo odmawia z podanym powodem. */
    fun advise(s: GameState, ctx: EngineContext, inner: GameCommand, run: (GameState, GameCommand) -> StepResult): StepResult {
        val id = subject(inner) ?: return run(s, inner)
        val d = s.disciple(id) ?: return run(s, inner)
        val day = s.currentDay
        val p = chance(s, d, inner)
        val base = s.update(id) { it.copy(adviceDay = day, adviceCount = if (it.adviceDay == day) it.adviceCount + 1 else 1) }
        val what = label(inner)
        // Nowicjusze w samouczku słuchają mistrza bez szemrania.
        val tutorial = s.tutorialStep != Tutorial.DONE
        if (!tutorial && ctx.rng("advice").nextInt(100) >= p) {
            val nagged = d.adviceDay == day && d.adviceCount >= 2
            val after = base.update(id) { it.copy(trust = (it.trust - if (nagged) 2 else 0).coerceIn(0, 100)).note(day, "Mistrz radził: $what. Odmówił${d.v("", "a")}.") }
            return StepResult(after, listOf(GameEvent.Notice("🙅 ${short(d)} odmawia ($what): „${refusal(s, d, inner)}”"), GameEvent.AdviceOutcome(id, false)))
        }
        val r = run(base, inner)
        if (r.state == base) return r
        // Więź buduje pierwsza rada dnia i jej skutek, nie zasypywanie ucznia poleceniami.
        val first = !(d.adviceDay == day && d.adviceCount > 0)
        val delta = (if (first) 1 else 0) + outcome(r.events, id)
        val memorable = inner is GameCommand.AssignRole || kind(inner) in setOf(Kind.DANGER, Kind.BREAKTHROUGH, Kind.CAUTION, Kind.REST)
        val st = if (r.state.disciple(id) == null) r.state else r.state.update(id) {
            val x = it.copy(
                trust = (it.trust + delta).coerceIn(0, 100),
                committedUntilDay = if (inner is GameCommand.AssignRole) day + COMMIT_DAYS else it.committedUntilDay
            )
            if (memorable) x.note(day, "Posłuchał${d.v("", "a")} rady: $what.") else x
        }
        val prefix = "🙇 ${short(d)} słucha rady ($what). "
        val notices = r.events.filterIsInstance<GameEvent.Notice>()
        val events = if (notices.isEmpty()) r.events + GameEvent.Notice(prefix.trim())
        else r.events.map { if (it === notices.last()) GameEvent.Notice(prefix + it.message) else it }
        return StepResult(st, events + GameEvent.AdviceOutcome(id, true))
    }

    /** „Wstrzymaj się”: uczeń obiecuje nie ryzykować (wielki przełom, wyprawy) przez [HOLD_DAYS] dni. */
    fun hold(s: GameState, id: String): StepResult {
        val d = s.disciple(id) ?: return ok(s)
        val until = s.currentDay + HOLD_DAYS
        return ok(s.update(id) { it.copy(holdUntilDay = until).note(s.currentDay, "Obiecał${d.v("", "a")} mistrzowi cierpliwość do dnia $until.") },
            "${short(d)} obiecuje nie ryzykować do dnia $until.")
    }

    /** Rozkazy mistrza też budują (albo psują) więź: awans, kara, leczenie, dary. */
    fun afterDirect(c: GameCommand, before: GameState, result: StepResult): StepResult {
        if (!before.autonomy || result.state == before) return result
        val (id, delta) = when (c) {
            is GameCommand.Promote -> c.discipleId to 8
            is GameCommand.Punish -> c.discipleId to -12
            is GameCommand.Pardon -> c.discipleId to 6
            is GameCommand.Cure -> c.discipleId to 5
            is GameCommand.Cleanse -> c.discipleId to 5
            is GameCommand.HealWithTalisman -> c.discipleId to 3
            is GameCommand.UsePill -> c.discipleId to 2
            is GameCommand.EquipArtifact -> c.discipleId to 3
            is GameCommand.FlameToDisciple -> c.discipleId to 5
            is GameCommand.MountBeast -> (c.discipleId ?: return result) to 3
            else -> return result
        }
        if (result.state.disciple(id) == null) return result
        return result.copy(state = result.state.update(id) { it.copy(trust = (it.trust + delta).coerceIn(0, 100)) })
    }

    // --- Życie codzienne ---

    /** Zajęcie, które uczeń wybrałby sam (bez losowości). */
    fun preferredRole(s: GameState, d: Disciple): DiscipleRole = DiscipleRole.entries.maxBy { roleScore(s, d, it) }

    private fun foodLow(s: GameState) = (s.inventory[DiscipleRules.FOOD] ?: 0) < s.disciples.size * 3
    private fun stock(s: GameState, names: List<String>) = names.sumOf { s.inventory[it] ?: 0 }
    private val herbNames by lazy { ResourceCatalog.herbs.map { it.name } }
    private val mineralNames by lazy { ResourceCatalog.minerals.map { it.name } }

    fun roleScore(s: GameState, d: Disciple, role: DiscipleRole): Int {
        val hurt = hurt(s, d)
        val dutiful = d.has("loyal") || d.has("humble") || d.has("diligent")
        val body = d.path == com.example.model.CultivationPath.BODY
        var v = when (role) {
            DiscipleRole.MEDITATING -> 45 + (if (d.has("diligent")) 15 else 0) + (if (d.has("ambitious")) 15 else 0) +
                (if (d.has("lazy")) 15 else 0) + (if (d.has("insightful")) 5 else 0) + (if (hurt) 100 else 0)
            DiscipleRole.HERB_GATHERING -> 25 + (if (d.has("gatherer")) 30 else 0) + (if (d.has("alchemist")) 20 else 0) +
                (if (foodLow(s)) (if (dutiful) 25 else 10) else 0) + (if (stock(s, herbNames) < 20) 10 else 0)
            DiscipleRole.MINING -> 20 + (if (d.has("greedy")) 20 else 0) + (if (body) 10 else 0) + (if (stock(s, mineralNames) < 15) 10 else 0)
            DiscipleRole.COMBAT_TRAINING -> 25 + (if (d.has("fearless")) 20 else 0) + (if (d.has("hotheaded")) 25 else 0) +
                (if (d.has("ambitious")) 10 else 0) + (if (body) 15 else 0)
            DiscipleRole.DIPLOMACY -> 10 + (if (d.has("charismatic")) 35 else 0)
            DiscipleRole.SECT_GUARD -> 15 + (if (d.has("loyal")) 15 else 0) +
                (if (s.raid != null) (if (d.has("coward")) -30 else if (d.has("fearless") || d.has("loyal")) 45 else 25) else 0)
        }
        if (d.has("lazy") && role != DiscipleRole.MEDITATING) v -= 20
        return v
    }

    private fun reason(s: GameState, d: Disciple, role: DiscipleRole): String = when (role) {
        DiscipleRole.MEDITATING -> if (hurt(s, d)) "Leczy rany w ciszy pawilonu." else "Kultywuje – chce przebić się wyżej."
        DiscipleRole.HERB_GATHERING -> if (foodLow(s)) "W spiżarni pusto – idzie na pola." else "Lubi zapach ziół i spokój pól."
        DiscipleRole.MINING -> "Szuka rud – kuźnia potrzebuje metalu."
        DiscipleRole.COMBAT_TRAINING -> "Ćwiczy walkę – chce się sprawdzić."
        DiscipleRole.DIPLOMACY -> "Odwiedza sąsiednie sekty – lubi ludzi."
        DiscipleRole.SECT_GUARD -> if (s.raid != null) "Najazd blisko – staje na bramie." else "Pilnuje bramy sekty."
    }

    /** Raz na dzień: każdy uczeń sam decyduje, co robi. Zdarzenia (przełomy, tribulacje) trafiają do UI. */
    fun daily(s0: GameState, ctx: EngineContext): Pair<GameState, List<GameEvent>> {
        if (!s0.autonomy) return s0 to emptyList()
        // W samouczku uczniowie nie ruszają na wyzwania – nauka nie może się rozjechać z ich planami.
        val tutorial = s0.tutorialStep != Tutorial.DONE
        var s = heal(s0)
        val events = mutableListOf<GameEvent>()
        for (id in s0.disciples.map { it.id }) {
            val d = s.disciple(id) ?: continue
            val day = s.currentDay
            if (s.expedition?.discipleId == id || s.combat?.discipleId == id) { s = s.update(id) { it.copy(plan = "Na wyprawie.") }; continue }
            if (d.secludedUntilDay >= day) { s = s.update(id) { it.copy(plan = "W odosobnieniu do dnia ${d.secludedUntilDay}.") }; continue }
            val rng = ctx.rng("autonomy-$id")
            val atGate = CultivationSystem.atMajorGate(s, d)
            if (atGate != (d.gateSinceDay > 0)) s = s.update(id) { it.copy(gateSinceDay = if (atGate) day.coerceAtLeast(1) else 0) }
            if (!hurt(s, d) && atGate) {
                val r = breakthrough(s, s.disciple(id)!!, ctx, rng)
                s = r.state; events += r.events
                if (r.events.isNotEmpty()) continue
            }
            val fresh = s.disciple(id) ?: continue
            if (!tutorial && !hurt(s, fresh) && day - fresh.ventureDay >= VENTURE_COOLDOWN) {
                val r = venture(s, fresh, ctx, rng)
                if (r != null) { s = r.state; events += r.events; continue }
            }
            s = chooseRole(s, s.disciple(id) ?: continue, rng)
        }
        return s to events
    }

    const val REST_HEAL_PERCENT = 8
    const val WORK_HEAL_PERCENT = 3

    /** Rany goją się same: w medytacji szybciej, przy pracy wolniej, z ciężką raną o połowę wolniej. */
    fun heal(s: GameState): GameState = s.copy(disciples = s.disciples.map { d ->
        if (d.currentHp >= d.maxHp || s.expedition?.discipleId == d.id) return@map d
        var pct = if (d.role == DiscipleRole.MEDITATING || d.secludedUntilDay >= s.currentDay) REST_HEAL_PERCENT else WORK_HEAL_PERCENT
        if (d.injuredUntilDay >= s.currentDay) pct /= 2
        d.copy(currentHp = (d.currentHp + (d.maxHp * pct / 100).coerceAtLeast(1)).coerceAtMost(d.maxHp))
    })

    /** Wielki przełom: śmiali próbują przy niższych szansach, ostrożni czekają na pigułkę lub lepszą chwilę. */
    private fun breakthrough(s: GameState, d: Disciple, ctx: EngineContext, rng: Random): StepResult {
        val day = s.currentDay
        val pill = s.autoUsePillsForBreakthrough && hasPill(s)
        val bold = d.has("fearless") || d.has("hotheaded")
        // Cierpliwość: im dłużej czeka u progu, tym niższej szansy mu wystarczy.
        val waited = if (d.gateSinceDay > 0) day - d.gateSinceDay else 0
        fun wait(text: String) = StepResult(s.update(d.id) { it.copy(plan = text) })
        if (d.holdUntilDay >= day) return wait("Czeka z przełomem, jak radził mistrz (do dnia ${d.holdUntilDay}).")
        if (CultivationSystem.tribulationAhead(d)) {
            if (d.currentHp < d.maxHp) return wait("Leczy się przed tribulacją.")
            val daring = when { d.has("coward") -> waited / 10; bold -> 25; d.has("ambitious") -> 20; else -> 6 + waited / 5 }
            if (rng.nextInt(100) >= daring) return wait(if (daring == 0) "Boi się gromów tribulacji." else "Zbiera odwagę przed tribulacją.")
        } else {
            val odds = CultivationSystem.majorOdds(s, d, pill) ?: return wait("Czeka u progu królestwa.")
            val need = when { d.has("coward") -> 75; bold -> 30; d.has("ambitious") -> 40; else -> 55 } - 2 * waited
            if (odds < need) return wait("Czeka na lepszą chwilę do przełomu (szansa ok. $odds%).")
        }
        val r = SectCommands.attemptBreakthrough(s, ctx, d.id, pill)
        val st = if (r.state.disciple(d.id) == null) r.state
        else r.state.update(d.id) { it.copy(plan = null).note(day, "Sam${d.v("", "a")} podjął${d.v("", "a")} próbę przełomu.") }
        return StepResult(st, listOf(GameEvent.Notice("🧘 ${short(d)} sam${d.v("", "a")} sięga po przełom. " + (r.notice ?: ""))) + r.events.filter { it !is GameEvent.Notice })
    }

    /** Wyzwanie z własnej woli – najwyżej jedno na [VENTURE_COOLDOWN] dni. */
    private fun venture(s: GameState, d: Disciple, ctx: EngineContext, rng: Random): StepResult? {
        val day = s.currentDay
        val bold = d.has("fearless") || d.has("hotheaded") || d.has("ambitious")
        val careful = d.has("coward") || d.holdUntilDay >= day
        val roll = rng.nextInt(1000)
        fun done(r: StepResult, text: String, diary: String): StepResult? {
            if (r.state == s) return null
            val st = r.state.disciple(d.id)?.let { r.state.update(d.id) { x -> x.copy(ventureDay = day, plan = text).note(day, diary) } } ?: r.state
            return StepResult(st, listOf(GameEvent.Notice("✦ ${short(d)}: ${r.notice ?: text}")) + r.events.filter { it !is GameEvent.Notice })
        }
        // Wieża Prób.
        if (!careful && bold && d.currentHp >= d.maxHp && roll < 60 && Tower.attemptsLeft(s) > 0) {
            return done(Tower.climb(s, d.id, ctx), "Sprawdza się w Wieży Prób.", "Sam${d.v("", "a")} poszedł${d.v("", "a")} do Wieży Prób.")
        }
        // Wyprawa (auto) – gdy żadna nie trwa.
        if (!careful && s.expedition == null && s.combat == null && roll in 60 until (60 + if (bold) 40 else 10)) {
            val known = s.regions.filter { s.discoveredRegionIds.isEmpty() || it.id in s.discoveredRegionIds }
            known.randomOrNull(rng)?.let { region ->
                return done(Expeditions.begin(s, region.id, d.id, auto = true, ctx = ctx), "Wrócił${d.v("", "a")} z wyprawy.", "Wyruszył${d.v("", "a")} na wyprawę: ${region.name}.")
            }
        }
        // Odosobnienie – gdy Qi daleko od progu.
        if (roll in 100 until (100 + if (d.has("diligent") || d.has("ambitious")) 50 else 10) && d.stageQi < Realms.required(d) * 0.5) {
            return done(CultivationCommands.seclude(s, d.id, 7), "Zamyka się w odosobnieniu.", "Zamknął${d.v("", "a")} się w odosobnieniu na 7 dni.")
        }
        // Kontemplacja – darmowe obiekty (nie zużywa zapasów).
        val rules = s.rules
        if (rules != null && roll in 150 until (150 + if (d.has("insightful")) 80 else 20)) {
            val obj = rules.lawObjects.keys.filter { Contemplation.available(s, it) }.minByOrNull { (d.contemplations[it] ?: 0) * 100 + rng.nextInt(100) }
            if (obj != null) return done(Contemplation.contemplate(s, d.id, obj), "Kontempluje „$obj”.", "Kontemplował${d.v("", "a")} „$obj”.")
        }
        return null
    }

    /** Zajęcie: trzyma się przyjętej rady, poza tym wybiera to, co lubi i czego sekta potrzebuje (z bezwładnością). */
    private fun chooseRole(s: GameState, d: Disciple, rng: Random): GameState {
        val day = s.currentDay
        if (d.committedUntilDay >= day) return s.update(d.id) { it.copy(plan = "Trzyma się rady mistrza: ${d.role.polishName} (do dnia ${d.committedUntilDay}).") }
        val scores = DiscipleRole.entries.associateWith { roleScore(s, d, it) + rng.nextInt(8) }
        val best = scores.maxBy { it.value }.key
        val switch = best != d.role && scores.getValue(best) > scores.getValue(d.role) + ROLE_INERTIA
        val role = if (switch) best else d.role
        return s.update(d.id) {
            val x = it.copy(role = role, plan = reason(s, d, role))
            if (switch) x.note(day, "${role.polishName}: ${reason(s, d, role)}") else x
        }
    }
}
