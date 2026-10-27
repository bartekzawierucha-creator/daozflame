package com.example.game.engine

import com.example.game.content.ResourceCatalog
import com.example.model.BreathOptimum
import com.example.model.CombatForm
import com.example.model.Constellation
import com.example.model.DerivedLaw
import com.example.model.Disciple
import com.example.model.Hint
import com.example.model.HintSource
import com.example.model.Knowledge
import com.example.model.KnowledgeLevel
import com.example.model.LawResonance
import com.example.model.MajorRealm
import com.example.model.SecretState
import com.example.model.SpiritElement
import com.example.model.TimeOfDay
import com.example.model.WorldRules
import kotlin.random.Random

/** Etap 6, krok 1: generator reguł świata – inne odpowiedzi w każdym świecie. */
object RulesGen {
    const val GRID = 6
    const val CONSTELLATIONS = 24
    const val FORMS = 40
    const val LAW_OBJECTS = 60
    val CENTER = setOf(14, 15, 20, 21)

    val PLACES = listOf("Kuźnia", "Wodospad", "Bambusowy Gaj", "Grota", "Szczyt")
    val PLACE_ELEMENTS = listOf(SpiritElement.FIRE, SpiritElement.WATER, SpiritElement.WOOD, SpiritElement.EARTH, SpiritElement.METAL)
    val BREATHS = listOf("Oddech Żółwia", "Oddech Żurawia", "Oddech Smoka", "Oddech Węża", "Oddech Tygrysa")
    val ATTRIBUTES = listOf("Ognisty", "Lodowy", "Leczniczy", "Trujący", "Kojący", "Pobudzający", "Twardniejący", "Lotny", "Ziemisty", "Świetlisty", "Mroczny", "Duchowy", "Krwisty")
    /** Kody ruchów gracza w walce: M – cios ręczny, U – technika ostateczna, P – pigułka, T – talizman. */
    const val MOVES = "MUPT"
    private val MOVE_NAMES = mapOf('M' to "cios", 'U' to "technika", 'P' to "pigułka", 'T' to "talizman")

    private val STAR_ADJ = listOf("Lazurowy", "Biały", "Cynobrowy", "Czarny", "Złoty", "Srebrny", "Purpurowy", "Zielony")
    private val STAR_NOUN = listOf("Smok", "Tygrys", "Feniks", "Żółw", "Wóz", "Lotos", "Miecz", "Pasterz", "Kocioł", "Wąż", "Żuraw", "Wilk")
    private val FORM_WORDS = listOf("Fala", "Klinga", "Grom", "Lotos", "Wicher", "Pazur", "Gwiazda", "Burza", "Cień", "Płomień")
    private val FORM_ADJ = listOf("Trzech", "Pękniętej", "Spadającej", "Cichej", "Podwójnej", "Wiecznej", "Krwawej", "Złotej")
    private val NATURE = listOf("Spadająca gwiazda", "Stara sosna", "Burza nad szczytem", "Księżyc w pełni", "Wschód słońca", "Zachód słońca",
        "Rwący potok", "Śnieg na wierzchołku", "Pajęczyna o świcie", "Kamień w strumieniu", "Ognisko", "Mgła nad bagnem", "Grób przodka",
        "Dzwon klasztoru", "Lustro wody", "Pęknięta skała", "Płatek lotosu", "Echo w grocie", "Kruk na gałęzi", "Popiół z kadzidła",
        "Wiatr w bambusach", "Szron na kamieniu", "Błyskawica", "Kropla rosy", "Stary miecz w kamieniu", "Opadający liść", "Wir w rzece",
        "Ślad tygrysa", "Gniazdo żurawia", "Rdza na dzwonie", "Pierwszy śnieg", "Pękający lód", "Ćma przy lampie", "Cień o południu", "Gwiazda polarna")

    fun neighbours(p: Int): List<Int> {
        val r = p / GRID
        val c = p % GRID
        return listOf(r - 1 to c, r + 1 to c, r to c - 1, r to c + 1).filter { (a, b) -> a in 0 until GRID && b in 0 until GRID }.map { (a, b) -> a * GRID + b }
    }

    fun generate(seed: Long): WorldRules {
        val rng = GameRng.stream(seed, 0L, "rules-v1")
        val constellations = (0 until CONSTELLATIONS).map { i ->
            val size = 4 + rng.nextInt(2)
            val points = linkedSetOf(rng.nextInt(GRID * GRID))
            while (points.size < size) points += neighbours(points.random(rng)).random(rng)
            val stat = listOf(Stat.QI_GAIN, Stat.COMBAT_DAMAGE, Stat.BREAKTHROUGH_CHANCE, Stat.EPIPHANY_CHANCE).random(rng)
            Constellation("c$i", "${STAR_ADJ[i % STAR_ADJ.size]} ${STAR_NOUN[(i * 5 + i / STAR_ADJ.size) % STAR_NOUN.size]}", points.toList(), stat.name, 3 + rng.nextInt(5))
        }.distinctBy { it.name }
        val blockages = (0 until 6).map { rng.nextInt(GRID * GRID) }.filter { it !in CENTER }.toSet()

        val methods = Realms.methods.filter { it.id != Realms.BASIC_METHOD }
        val methodElements = methods.associate { it.id to SpiritElement.BASIC.random(rng) }
        val breath = methods.associate { m ->
            val el = methodElements.getValue(m.id)
            // Miejsce nie może kłócić się z żywiołem metody (metoda Wody nigdy w Kuźni).
            val allowed = PLACES.indices.filter { i -> !Elements.overcomes(PLACE_ELEMENTS[i], el) && !Elements.overcomes(el, PLACE_ELEMENTS[i]) }
            m.id to BreathOptimum(rng.nextInt(BREATHS.size), allowed.random(rng), TimeOfDay.entries.random(rng))
        }

        val laws = com.example.model.DaoLaw.entries.map { it.polishName }
        val objects = (NATURE + ResourceCatalog.herbs.map { it.name } + ResourceCatalog.minerals.map { it.name }).distinct().take(LAW_OBJECTS)
        val lawObjects = objects.associateWith { LawResonance(laws.random(rng), 50 + rng.nextInt(151)) }
        val derived = laws.shuffled(rng).chunked(2).take(6).mapIndexed { i, (a, b) ->
            DerivedLaw(a, b, "Prawo " + listOf("Burzy Ognia", "Martwej Wody", "Wiecznej Wiosny", "Złamanego Czasu", "Pustej Ziemi", "Krwawego Księżyca")[i])
        }

        val herbAttributes = ResourceCatalog.herbs.associate { h ->
            h.name to ATTRIBUTES.shuffled(rng).take(9 + rng.nextInt(4)).associateWith { 1 + rng.nextInt(5) }
        }

        val allSeq = MOVES.flatMap { a -> MOVES.flatMap { b -> MOVES.map { c -> "$a$b$c" } } }.filter { it.toSet().size > 1 }
        val forms = allSeq.shuffled(rng).take(FORMS).mapIndexed { i, seq ->
            CombatForm("f$i", "Forma ${FORM_ADJ[i % FORM_ADJ.size]} ${FORM_WORDS[(i / FORM_ADJ.size + i) % FORM_WORDS.size]}", seq, 20 + rng.nextInt(41))
        }.distinctBy { it.name }

        val base = WorldRules(1, constellations, blockages, breath, methodElements, lawObjects, derived, herbAttributes, forms)
        return base.copy(hints = hints(base, rng))
    }

    /** Wskazówki: każda tajemnica dostaje dwie z różnych źródeł (sen, zwój, opowieść, opis, badania). */
    private fun hints(r: WorldRules, rng: Random): List<Hint> = buildList {
        fun two(id: String, a: Pair<HintSource, String>, b: Pair<HintSource, String>) {
            add(Hint(id, a.first, a.second)); add(Hint(id, b.first, b.second))
        }
        r.constellations.forEach { c ->
            val rows = c.points.map { it / GRID }.toSet().sorted().joinToString("–") { (it + 1).toString() }
            two(Secrets.constellation(c.id),
                HintSource.DREAM to "We śnie gwiazdy układają się w kształt: ${c.name} świeci w rzędach $rows.",
                HintSource.RESEARCH to "Mapa nieba z archiwum: ${c.name} obejmuje ${c.points.size} punktów meridianów.")
        }
        r.breath.forEach { (method, b) ->
            val name = Realms.method(method).name
            two(Secrets.breath(method),
                HintSource.SCROLL to "Fragment zwoju „$name”: „…${BREATHS[b.breath].lowercase()}, gdy ${b.time.polishName.lowercase()} kładzie się na świat…”",
                HintSource.NPC to "Wędrowcy mówią, że mistrzowie „$name” medytowali w miejscu zwanym ${PLACES[b.place]}.")
        }
        r.lawObjects.forEach { (obj, res) ->
            two(Secrets.law(obj),
                HintSource.DESCRIPTION to "$obj: mędrcy widzą w tym ślad – ${res.law.removePrefix("Prawo ").lowercase()}.",
                HintSource.DREAM to "Śnisz o rzeczy „$obj”, a w tle słychać: ${res.law}.")
        }
        r.derivedLaws.forEach { d ->
            two(Secrets.derived(d.name),
                HintSource.SCROLL to "Stary traktat: kto zgłębi ${d.first} i ${d.second}, ujrzy trzecią drogę.",
                HintSource.RESEARCH to "Badacz notuje: ${d.first} i ${d.second} splatają się w ${d.name}.")
        }
        r.herbAttributes.forEach { (herb, attrs) ->
            attrs.keys.forEach { a ->
                two(Secrets.herb(herb, a),
                    HintSource.DESCRIPTION to "$herb pachnie jak coś ${a.lowercase()}ego.",
                    HintSource.RESEARCH to "Notatki alchemika: $herb – cecha: $a.")
            }
        }
        r.forms.forEach { f ->
            val words = f.sequence.map { MOVE_NAMES.getValue(it) }
            two(Secrets.form(f.id),
                HintSource.NPC to "Stary szermierz nuci: „${words.joinToString(", ")} – i niebo pęka”.",
                HintSource.SCROLL to "Rycina w zwoju: ${f.name} zaczyna się od ruchu: ${words.first()}.")
        }
        listOf(
            Secrets.CYCLE_EXCESS to ("Matka karmi dziecko, aż dziecko staje się olbrzymem." to "Badacz: żywioł, który rodzi drugi, wzmacnia go także w walce."),
            Secrets.CYCLE_REBELLION to ("Mrówka może przewrócić słonia, jeśli jest dość silna." to "Badacz: przy dużej przewadze siły cykl przezwyciężania odwraca się."),
            Secrets.CYCLE_YINYANG to ("Księżyc sprzyja wodzie, słońce sprzyja ogniowi." to "Badacz: żywioły Yin silniejsze nocą, Yang – za dnia.")
        ).forEach { (id, t) -> two(id, HintSource.DREAM to t.first, HintSource.RESEARCH to t.second) }
        r.blockages.forEach { p ->
            two(Secrets.blockage(p),
                HintSource.DREAM to "We śnie czujesz ucisk w punkcie ${p + 1} – jakby Qi napotykało kamień.",
                HintSource.DESCRIPTION to "Stare mapy meridianów oznaczają punkt ${p + 1} czerwonym tuszem.")
        }
        SecretState.entries.forEach { st ->
            two(Secrets.state(st), HintSource.NPC to "Opowiadają o stanie zwanym „${st.polishName}”.", HintSource.SCROLL to st.description)
        }
    }

    /** Walidator rozwiązywalności: ≥ 2 wskazówki z różnych źródeł; każdy gwiazdozbiór osiągalny z centrum przy limicie meridianów. */
    fun validate(r: WorldRules): List<String> = buildList {
        val bySecret = r.hints.groupBy { it.secretId }
        Secrets.allIds(r).forEach { id ->
            val sources = bySecret[id].orEmpty().map { it.source }.toSet()
            if (sources.size < 2) add("$id ma za mało niezależnych wskazówek: $sources")
        }
        val cap = Meridians.cap(MajorRealm.MAHAYANA)
        r.constellations.forEach { c ->
            val dist = distanceFromCenter(c.points.toSet())
            if (dist + c.points.size > cap) add("${c.name} poza zasięgiem ($dist + ${c.points.size} > $cap)")
            val connected = connected(c.points.toSet())
            if (!connected) add("${c.name} nie jest spójny")
        }
        r.breath.forEach { (m, b) ->
            val el = r.methodElements[m] ?: return@forEach
            val place = PLACE_ELEMENTS[b.place]
            if (Elements.overcomes(place, el) || Elements.overcomes(el, place)) add("Optimum $m sprzeczne z żywiołem")
        }
    }

    fun distanceFromCenter(target: Set<Int>): Int {
        var frontier = CENTER.toSet()
        val seen = frontier.toMutableSet()
        var d = 0
        while (frontier.none { it in target }) {
            frontier = frontier.flatMap { neighbours(it) }.filter { seen.add(it) }.toSet()
            d++
            if (frontier.isEmpty()) return 99
        }
        return d
    }

    private fun connected(points: Set<Int>): Boolean {
        val start = points.first()
        val seen = mutableSetOf(start)
        val stack = ArrayDeque(listOf(start))
        while (stack.isNotEmpty()) neighbours(stack.removeLast()).filter { it in points && seen.add(it) }.forEach { stack.add(it) }
        return seen.size == points.size
    }
}

/** Identyfikatory tajemnic i migracja. */
object Secrets {
    const val CYCLE_EXCESS = "cycle:excess"
    const val CYCLE_REBELLION = "cycle:rebellion"
    const val CYCLE_YINYANG = "cycle:yinyang"
    fun constellation(id: String) = "const:$id"
    fun breath(methodId: String) = "breath:$methodId"
    fun law(obj: String) = "law:$obj"
    fun derived(name: String) = "derived:$name"
    fun herb(herb: String, attr: String) = "herb:$herb:$attr"
    fun form(id: String) = "form:$id"
    fun state(st: SecretState) = "state:${st.name}"
    fun blockage(p: Int) = "block:$p"

    fun allIds(r: WorldRules): List<String> =
        r.constellations.map { constellation(it.id) } + r.breath.keys.map { breath(it) } + r.lawObjects.keys.map { law(it) } +
            r.derivedLaws.map { derived(it.name) } + r.herbAttributes.flatMap { (h, a) -> a.keys.map { herb(h, it) } } +
            r.forms.map { form(it.id) } + r.blockages.map { blockage(it) } + r.fengShui.indices.map { FengShui.secretId(it) } + listOf(CYCLE_EXCESS, CYCLE_REBELLION, CYCLE_YINYANG) + SecretState.entries.map { state(it) }

    /** Reguły losuje się raz – stare zapisy dostają je przy pierwszym wczytaniu, nowe przy tworzeniu gry. */
    fun normalize(s: GameState): GameState {
        val base = s.rules ?: RulesGen.generate(s.worldSeed)
        // Nowa treść (v2) dopisywana z osobnego strumienia, bez ruszania starych reguł.
        val extended = FengShui.extend(base, s.worldSeed)
        return if (extended === s.rules) s else s.copy(rules = extended)
    }
}

/** Etap 6, krok 2: Dziennik – jedyne miejsce zapisu wiedzy o tajemnicach. */
object Journal {
    const val LIKELY_AT = 3
    const val CERTAIN_AT = 5

    fun level(s: GameState, id: String): KnowledgeLevel? = s.journal[id]?.level

    fun certain(s: GameState, id: String): Boolean = level(s, id) == KnowledgeLevel.CERTAIN

    /** Funkcja formatująca – jedyna droga, którą UI dostaje ukryte liczby. */
    fun show(s: GameState, id: String, value: String): String = if (certain(s, id)) value else "?"

    /** Obserwacja z dowolnego systemu; [cap] ogranicza poziom (np. sny nie dają pewności). */
    fun observe(s: GameState, id: String, note: String? = null, cap: KnowledgeLevel = KnowledgeLevel.CERTAIN): GameState {
        val k = s.journal[id] ?: Knowledge()
        val obs = k.observations + 1
        val raw = when {
            obs >= CERTAIN_AT -> KnowledgeLevel.CERTAIN
            obs >= LIKELY_AT -> KnowledgeLevel.LIKELY
            else -> KnowledgeLevel.GUESS
        }
        val lvl = maxOf(k.level, minOf(raw, cap))
        val notes = if (note != null && note !in k.notes) (k.notes + note).takeLast(6) else k.notes
        return s.copy(journal = s.journal + (id to k.copy(level = lvl, observations = obs, notes = notes)))
    }

    /** Odkrycie wprost (np. rozbłysk gwiazdozbioru, odszyfrowany zwój). */
    fun reveal(s: GameState, id: String, note: String? = null): GameState {
        val k = s.journal[id] ?: Knowledge()
        val notes = if (note != null && note !in k.notes) (k.notes + note).takeLast(6) else k.notes
        return s.copy(journal = s.journal + (id to k.copy(level = KnowledgeLevel.CERTAIN, observations = maxOf(k.observations, CERTAIN_AT), notes = notes)))
    }

    fun discovered(s: GameState): Int = s.journal.values.count { it.level == KnowledgeLevel.CERTAIN }
}

/** Mapa meridianów: siatka 6×6, start w centrum, otwiera się punkty sąsiednie; gwiazdozbiór rozbłyska po otwarciu wszystkich jego punktów. */
object Meridians {
    fun cap(realm: MajorRealm?): Int = 6 + 3 * (realm?.ordinal ?: 0)

    fun cost(rules: WorldRules, point: Int, opened: Int): Int = 60 * (opened + 1) * if (point in rules.blockages) 3 else 1

    fun completed(rules: WorldRules, d: Disciple): List<Constellation> = rules.constellations.filter { c -> d.openedMeridians.containsAll(c.points) }

    fun open(s: GameState, discipleId: String, point: Int): StepResult {
        val rules = s.rules ?: return ok(s)
        val d = s.disciple(discipleId) ?: return ok(s)
        if (point !in 0 until RulesGen.GRID * RulesGen.GRID || point in d.openedMeridians) return ok(s)
        val adjacent = point in RulesGen.CENTER || RulesGen.neighbours(point).any { it in d.openedMeridians }
        if (!adjacent) return fail(s, "Qi płynie tylko do punktu sąsiadującego z otwartym (albo z centrum).")
        if (d.openedMeridians.size >= cap(d.majorRealm)) return fail(s, "${d.name} nie udźwignie więcej meridianów w obecnym królestwie (${cap(d.majorRealm)}).")
        val cost = cost(rules, point, d.openedMeridians.size)
        if (s.spiritStones < cost) return fail(s, "Otwarcie punktu kosztuje $cost kamieni" + if (point in rules.blockages) " (zator!)." else ".")
        val before = completed(rules, d).map { it.id }.toSet()
        val opened = d.copy(openedMeridians = d.openedMeridians + point)
        var st = s.copy(spiritStones = s.spiritStones - cost, disciples = s.disciples.map { if (it.id == d.id) opened else it })
        val fresh = completed(rules, opened).filter { it.id !in before }
        fresh.forEach { c -> st = Journal.reveal(st, Secrets.constellation(c.id), "Rozbłysł u: ${d.name}") }
        val zator = if (point in rules.blockages) " Zator ustąpił z trudem." else ""
        if (point in rules.blockages) st = Journal.reveal(st, Secrets.blockage(point), "Zator w punkcie ${point + 1}")
        return ok(st, if (fresh.isNotEmpty()) "✨ ${fresh.joinToString { it.name }} rozbłyska w meridianach ${d.name}!" else "Otwarto punkt meridianu ${point + 1}.$zator")
    }
}

/** Oddech, miejsce i pora medytacji – każda metoda ma swoje optimum. */
object Breath {
    fun matches(rules: WorldRules, d: Disciple, time: TimeOfDay): Int {
        val o = rules.breath[d.methodId] ?: return 0
        return listOf(d.breath == o.breath, d.meditationPlace == o.place, time == o.time).count { it }
    }

    fun bonusPercent(m: Int): Double = when (m) {
        3 -> 35.0
        2 -> 20.0
        1 -> 8.0
        else -> 0.0
    }

    fun set(s: GameState, discipleId: String, breath: Int, place: Int): StepResult {
        val d = s.disciple(discipleId) ?: return ok(s)
        val b = breath.coerceIn(0, RulesGen.BREATHS.lastIndex)
        val p = place.coerceIn(0, RulesGen.PLACES.lastIndex)
        return ok(s.copy(disciples = s.disciples.map { if (it.id == d.id) it.copy(breath = b, meditationPlace = p) else it }),
            "${d.name}: ${RulesGen.BREATHS[b]}, ${RulesGen.PLACES[p]}.")
    }

    /** Raz na dzień: uczniowie z co najmniej dwoma trafieniami „czują”, że Qi płynie lepiej – obserwacja w Dzienniku. */
    fun daily(s0: GameState): GameState {
        val rules = s0.rules ?: return s0
        var s = s0
        s.disciples.filter { it.breath != null && it.methodId != null && it.methodId != Realms.BASIC_METHOD }.forEach { d ->
            val m = TimeOfDay.entries.maxOf { t -> matches(rules, d, t) }
            if (m >= 2) s = Journal.observe(s, Secrets.breath(d.methodId!!), "${d.name}: Qi płynie gładko (${RulesGen.BREATHS[d.breath!!]}, ${RulesGen.PLACES[d.meditationPlace ?: 0]}).")
        }
        return s
    }

    const val FRAGMENTS = 5
    fun fragmentItem(methodId: String) = "Fragment: ${Realms.method(methodId).name}"

    /** Pięć fragmentów zwoju odsłania optimum metody. */
    fun decipher(s: GameState, methodId: String): StepResult {
        val rules = s.rules ?: return ok(s)
        val item = fragmentItem(methodId)
        val have = s.inventory[item] ?: 0
        if (have < FRAGMENTS) return fail(s, "Do odczytania potrzeba $FRAGMENTS fragmentów (masz $have).")
        val o = rules.breath[methodId] ?: return ok(s)
        val st = Journal.reveal(s.copy(inventory = s.inventory + (item to have - FRAGMENTS)), Secrets.breath(methodId),
            "Zwój: ${RulesGen.BREATHS[o.breath]}, ${RulesGen.PLACES[o.place]}, ${o.time.polishName}.")
        return ok(st, "📜 Odszyfrowano zwój „${Realms.method(methodId).name}”: ${RulesGen.BREATHS[o.breath]} – ${RulesGen.PLACES[o.place]} – ${o.time.polishName}.")
    }
}

/** Kontemplacja obiektów: rezonans z Prawem, malejący efekt powtórzeń; dwa Prawa ≥ 50% odsłaniają Prawo pochodne. */
object Contemplation {
    const val BASE = 2.0
    const val REPEAT_DECAY = 0.8

    fun available(s: GameState, obj: String): Boolean = obj !in ResourceCatalog.herbs.map { it.name } + ResourceCatalog.minerals.map { it.name } || (s.inventory[obj] ?: 0) > 0

    fun contemplate(s: GameState, discipleId: String, obj: String): StepResult {
        val rules = s.rules ?: return ok(s)
        val d = s.disciple(discipleId) ?: return ok(s)
        val res = rules.lawObjects[obj] ?: return fail(s, "W tym nie ma nic do kontemplacji.")
        if (!available(s, obj)) return fail(s, "Potrzebujesz „$obj” w magazynie.")
        val times = d.contemplations[obj] ?: 0
        val base = d.copy(contemplations = d.contemplations + (obj to times + 1))
        if (res.law !in d.laws) {
            val st = Journal.observe(s.copy(disciples = s.disciples.map { if (it.id == d.id) base else it }), Secrets.law(obj),
                "${d.name} nie czuje rezonansu – to nie jego Prawo.", cap = KnowledgeLevel.LIKELY)
            return ok(st, "${d.name} kontempluje „$obj”… nic nie rezonuje z jego Prawami.")
        }
        val gain = BASE * res.affinity / 100.0 * Math.pow(REPEAT_DECAY, times.toDouble())
        val now = ((d.laws[res.law] ?: 0.0) + gain).coerceAtMost(100.0)
        var updated = base.copy(laws = d.laws + (res.law to now))
        var st = Journal.observe(s.copy(disciples = s.disciples.map { if (it.id == d.id) updated else it }), Secrets.law(obj),
            "${d.name}: „$obj” rezonuje z ${res.law}.")
        var extra = ""
        rules.derivedLaws.filter { it.name !in updated.laws && (updated.laws[it.first] ?: 0.0) >= 50 && (updated.laws[it.second] ?: 0.0) >= 50 }.forEach { dl ->
            updated = updated.copy(laws = updated.laws + (dl.name to 1.0))
            st = Journal.reveal(st.copy(disciples = st.disciples.map { if (it.id == d.id) updated else it }), Secrets.derived(dl.name), "Odkrył ${d.name}.")
            extra = " Splatają się Prawa – rodzi się ${dl.name}!"
        }
        return ok(st, "${d.name} kontempluje „$obj”: ${res.law} +%.1f%%.".format(gain) + extra)
    }
}

/** Alchemia jako chemia: atrybuty ziół i esencje emergentne (z limitem siły). */
object Chemistry {
    const val THRESHOLD = 8
    const val ESSENCE = "Esencja: "

    fun dominant(rules: WorldRules, main: String, second: String?): Pair<String, Int>? {
        val sum = mutableMapOf<String, Int>()
        rules.herbAttributes[main]?.forEach { (a, v) -> sum[a] = (sum[a] ?: 0) + 2 * v }
        second?.let { h -> rules.herbAttributes[h]?.forEach { (a, v) -> sum[a] = (sum[a] ?: 0) + v } }
        return sum.maxByOrNull { it.value }?.takeIf { it.value >= THRESHOLD }?.toPair()
    }

    /** Obserwacje po eksperymencie: wyczuwasz najsilniejsze cechy użytych ziół. */
    fun observe(s0: GameState, main: String, second: String?): GameState {
        val rules = s0.rules ?: return s0
        var s = s0
        listOfNotNull(main, second).forEach { h ->
            rules.herbAttributes[h]?.maxByOrNull { it.value }?.let { (a, _) -> s = Journal.observe(s, Secrets.herb(h, a), "Eksperyment z $h") }
        }
        return s
    }

    /** Efekt esencji – zawsze ograniczony, żeby nie złamać balansu. */
    fun use(s: GameState, d: Disciple, essence: String): Disciple? {
        val attr = essence.removePrefix(ESSENCE)
        return when (attr) {
            "Duchowy", "Świetlisty", "Pobudzający" -> CultivationSystem.gainQi(d, CultivationSystem.ticksWorth(s, d, 60))
            "Leczniczy", "Kojący" -> d.copy(currentHp = (d.currentHp + d.maxHp * 3 / 10).coerceAtMost(d.maxHp), toxicity = (d.toxicity - 5).coerceAtLeast(0))
            "Lodowy" -> d.copy(toxicity = (d.toxicity - 10).coerceAtLeast(0))
            "Ognisty", "Krwisty", "Twardniejący" -> d.copy(attack = d.attack + 2)
            "Lotny", "Ziemisty" -> d.copy(defense = d.defense + 2)
            "Trujący", "Mroczny" -> CultivationSystem.gainQi(d.copy(toxicity = d.toxicity + 10), CultivationSystem.ticksWorth(s, d, 80))
            else -> null
        }
    }
}

/** Ukryte cykle żywiołów w walce: działają od początku gry, ale nazwy poznaje się dopiero z Dziennika. */
object Cycles {
    private val GENERATES = mapOf(
        SpiritElement.WOOD to SpiritElement.FIRE, SpiritElement.FIRE to SpiritElement.EARTH, SpiritElement.EARTH to SpiritElement.METAL,
        SpiritElement.METAL to SpiritElement.WATER, SpiritElement.WATER to SpiritElement.WOOD
    )
    val YIN = setOf(SpiritElement.WATER, SpiritElement.ICE, SpiritElement.METAL)
    val YANG = setOf(SpiritElement.FIRE, SpiritElement.WOOD, SpiritElement.THUNDER)

    data class Effect(val attack: Double, val defense: Double, val observed: List<Pair<String, String>>)

    fun apply(s: GameState, d: Disciple, beast: SpiritElement, beastDefense: Int): Effect {
        var atk = 1.0
        var def = 1.0
        val seen = mutableListOf<Pair<String, String>>()
        val els = Elements.elementsOf(d)
        if (els.any { GENERATES[it] == beast }) {
            def *= 1.15
            seen += Secrets.CYCLE_EXCESS to "Bestia rośnie w siłę – jakby żywioł ucznia ją karmił."
        }
        if (els.any { Elements.overcomes(beast, it) } && d.attack > beastDefense * 2) {
            atk *= 1.2
            seen += Secrets.CYCLE_REBELLION to "Mimo słabszego żywiołu uczeń przełamuje bestię siłą."
        }
        val night = s.timeOfDay == TimeOfDay.NIGHT || s.timeOfDay == TimeOfDay.FULL_MOON
        val day = s.timeOfDay == TimeOfDay.DAY
        if ((night && els.any { it in YIN }) || (day && els.any { it in YANG })) {
            atk *= 1.1
            seen += Secrets.CYCLE_YINYANG to "Pora dnia sprzyja żywiołowi ucznia."
        }
        return Effect(atk, def, seen)
    }

    fun label(s: GameState, id: String, vague: String): String = if (Journal.certain(s, id)) when (id) {
        Secrets.CYCLE_EXCESS -> "Cykl Nadmiaru"
        Secrets.CYCLE_REBELLION -> "Cykl Buntu"
        else -> "Harmonia Yin-Yang"
    } + ": " + vague else vague
}

/** Formy: sekwencje ruchów gracza w walce wykrywane w historii ruchów. */
object Forms {
    fun detect(s: GameState, history: String, triggered: Set<String>): CombatForm? {
        val rules = s.rules ?: return null
        if (history.length < 3) return null
        val last = history.takeLast(3)
        return rules.forms.firstOrNull { it.sequence == last && it.id !in triggered }
    }
}

/** Ukryte stany: wejście i koniec (żaden nie trwa wiecznie). */
object SecretStates {
    const val HALF_STEP_FAILS = 3
    const val HALF_STEP_DAYS = 30
    const val FALSE_IMMORTAL_DAYS = 60
    const val FALSE_IMMORTAL_TOXICITY = 60
    const val CLEAR_HEART_DAYS = 45

    fun daily(s0: GameState, ctx: EngineContext): GameState {
        var s = s0
        val rng = ctx.rng("secret-states")
        s = s.copy(disciples = s.disciples.map { d ->
            val st = d.secretState
            when {
                st != null && s.currentDay >= d.secretStateUntil -> d.copy(secretState = null, secretStateUntil = 0)
                st == SecretState.FALSE_IMMORTAL && d.toxicity == 0 -> d.copy(secretState = null, secretStateUntil = 0)
                st == SecretState.CLEAR_HEART && s.karma < 30 -> d.copy(secretState = null, secretStateUntil = 0)
                st == null && d.path == com.example.model.CultivationPath.HEART && s.karma >= 60 && !d.heartDemon && rng.nextInt(100) < 3 ->
                    d.copy(secretState = SecretState.CLEAR_HEART, secretStateUntil = s.currentDay + CLEAR_HEART_DAYS)
                else -> d
            }
        })
        s0.disciples.zip(s.disciples).filter { (a, b) -> a.secretState == null && b.secretState != null }.forEach { (_, b) ->
            s = Journal.observe(s, Secrets.state(b.secretState!!), "${b.name}: ${b.secretState.polishName}")
        }
        return s
    }

    fun afterFailure(d: Disciple, today: Int): Disciple {
        val fails = d.majorFailures + 1
        return if (fails >= HALF_STEP_FAILS && d.secretState == null)
            d.copy(majorFailures = fails, secretState = SecretState.HALF_STEP, secretStateUntil = today + HALF_STEP_DAYS)
        else d.copy(majorFailures = fails)
    }

    fun afterSuccess(d: Disciple, pill: Boolean, today: Int): Disciple {
        val cleared = d.copy(majorFailures = 0, secretState = d.secretState.takeIf { it != SecretState.HALF_STEP }, secretStateUntil = if (d.secretState == SecretState.HALF_STEP) 0 else d.secretStateUntil)
        return if (pill && d.toxicity >= FALSE_IMMORTAL_TOXICITY && cleared.secretState == null)
            cleared.copy(secretState = SecretState.FALSE_IMMORTAL, secretStateUntil = today + FALSE_IMMORTAL_DAYS)
        else cleared
    }
}

/** Narzędzia odkrywania: sny, Badacz, Lustro Dao. */
object SecretTools {
    const val DREAM_CHANCE = 15
    const val MIRROR = "Lustro Dao"

    private fun unknownHints(s: GameState): List<Hint> = s.rules?.hints.orEmpty().filter { !Journal.certain(s, it.secretId) }

    /** Raz na dzień: sen (najwyżej „Prawdopodobne”) i praca Badacza. */
    fun daily(s0: GameState, ctx: EngineContext): Pair<GameState, List<String>> {
        if (s0.rules == null) return s0 to emptyList()
        var s = s0
        val notes = mutableListOf<String>()
        val rng = ctx.rng("dreams")
        if (rng.nextInt(100) < DREAM_CHANCE) {
            unknownHints(s).filter { it.source == HintSource.DREAM }.randomOrNull(rng)?.let { h ->
                s = Journal.observe(s, h.secretId, "Sen: ${h.text}", cap = KnowledgeLevel.LIKELY)
                notes += "💤 Patriarsze przyśnił się dziwny sen. (Dziennik)"
            }
        }
        val researcher = s.disciple(s.researcherId)
        if (researcher != null && rng.nextInt(100) < 10 + researcher.comprehension / 10) {
            unknownHints(s).filter { it.source == HintSource.RESEARCH }.randomOrNull(rng)?.let { h ->
                s = Journal.observe(s, h.secretId, "Badania: ${h.text}")
                notes += "📚 ${researcher.name} odnotowuje nowe spostrzeżenie. (Dziennik)"
            }
        }
        return s to notes
    }

    fun setResearcher(s: GameState, discipleId: String?): StepResult {
        val d = s.disciple(discipleId)
        return ok(s.copy(researcherId = d?.id), if (d != null) "${d.name} zostaje Badaczem tajemnic." else "Sekta nie ma Badacza.")
    }

    /** Lustro Dao: pokazuje uczniowi gwiazdozbiór, do którego jest najbliżej (pewność „Prawdopodobne”). */
    fun mirror(s: GameState, discipleId: String): StepResult {
        val rules = s.rules ?: return ok(s)
        val d = s.disciple(discipleId) ?: return ok(s)
        val have = s.inventory[MIRROR] ?: 0
        if (have < 1) return fail(s, "Brak Lustra Dao.")
        val target = rules.constellations.filter { !Journal.certain(s, Secrets.constellation(it.id)) }
            .minByOrNull { c -> c.points.count { it !in d.openedMeridians } } ?: return fail(s, "Lustro nie pokazuje nic nowego.")
        val missing = target.points.filter { it !in d.openedMeridians }.map { it + 1 }
        var st = s.copy(inventory = s.inventory + (MIRROR to have - 1))
        repeat(Journal.LIKELY_AT) { st = Journal.observe(st, Secrets.constellation(target.id), "Lustro: brakuje punktów ${missing.joinToString()}.", cap = KnowledgeLevel.LIKELY) }
        return ok(st, "🪞 W Lustrze Dao ${d.name} widzi kształt gwiazd – brakuje punktów: ${missing.joinToString()}.")
    }
}

/** Premie z tajemnic – dołączane do modyfikatorów ucznia. Źródło bez pewnej wiedzy podpisane jest „Nieznany wpływ”. */
object SecretModifiers {
    fun of(s: GameState, d: Disciple): List<Modifier> {
        val rules = s.rules ?: return emptyList()
        fun label(id: String, name: String) = if (Journal.certain(s, id)) name else "Nieznany wpływ"
        return buildList {
            Meridians.completed(rules, d).forEach { c ->
                add(Modifier("Gwiazdozbiór: ${c.name}", Stat.valueOf(c.stat), ModOp.MULTIPLY, 1 + c.percent / 100.0))
            }
            val m = Breath.matches(rules, d, s.timeOfDay)
            if (m > 0 && d.methodId != null) add(Modifier(label(Secrets.breath(d.methodId), "Oddech i miejsce"), Stat.QI_GAIN, ModOp.MULTIPLY, 1 + Breath.bonusPercent(m) / 100.0))
            rules.derivedLaws.filter { it.name in d.laws }.forEach { add(Modifier(it.name, Stat.COMBAT_DAMAGE, ModOp.PERCENT, 10.0)) }
            when (d.secretState) {
                SecretState.HALF_STEP -> {
                    add(Modifier(label(Secrets.state(SecretState.HALF_STEP), "Półkrok"), Stat.COMBAT_DAMAGE, ModOp.PERCENT, 10.0))
                    add(Modifier(label(Secrets.state(SecretState.HALF_STEP), "Półkrok"), Stat.QI_GAIN, ModOp.MULTIPLY, 0.8))
                }
                SecretState.FALSE_IMMORTAL -> {
                    add(Modifier(label(Secrets.state(SecretState.FALSE_IMMORTAL), "Fałszywy Nieśmiertelny"), Stat.QI_GAIN, ModOp.MULTIPLY, 1.2))
                    add(Modifier(label(Secrets.state(SecretState.FALSE_IMMORTAL), "Fałszywy Nieśmiertelny"), Stat.BREAKTHROUGH_CHANCE, ModOp.FLAT, -10.0))
                }
                SecretState.CLEAR_HEART -> add(Modifier(label(Secrets.state(SecretState.CLEAR_HEART), "Czyste Serce"), Stat.EPIPHANY_CHANCE, ModOp.PERCENT, 100.0))
                null -> Unit
            }
        }
    }
}

/** Opis wpisu Dziennika dla UI – liczby tylko przez [Journal.show]. */
object JournalView {
    data class Entry(val id: String, val category: String, val title: String, val value: String, val level: KnowledgeLevel, val notes: List<String>)

    fun entries(s: GameState): List<Entry> {
        val r = s.rules ?: return emptyList()
        return s.journal.map { (id, k) ->
            val (cat, title, value) = describe(s, r, id)
            Entry(id, cat, title, Journal.show(s, id, value), k.level, k.notes)
        }.sortedWith(compareBy({ it.category }, { -it.level.ordinal }, { it.title }))
    }

    private fun describe(s: GameState, r: WorldRules, id: String): Triple<String, String, String> {
        val parts = id.split(":")
        return when (parts[0]) {
            "const" -> r.constellations.find { it.id == parts[1] }.let { c ->
                Triple("Gwiazdozbiory", c?.name ?: "?", c?.let { "punkty ${it.points.map { p -> p + 1 }.sorted().joinToString()} · ${Stat.valueOf(it.stat).label} +${it.percent}%" } ?: "")
            }
            "breath" -> r.breath[parts[1]].let { o ->
                Triple("Oddech", Realms.method(parts[1]).name, o?.let { "${RulesGen.BREATHS[it.breath]} · ${RulesGen.PLACES[it.place]} · ${it.time.polishName}" } ?: "")
            }
            "law" -> Triple("Kontemplacja", parts.drop(1).joinToString(":"), r.lawObjects[parts.drop(1).joinToString(":")]?.let { "${it.law} · rezonans ${it.affinity}%" } ?: "")
            "derived" -> Triple("Prawa pochodne", parts[1], r.derivedLaws.find { it.name == parts[1] }?.let { "${it.first} + ${it.second}" } ?: "")
            "herb" -> Triple("Alchemia", "${parts[1]} – ${parts[2]}", "siła ${r.herbAttributes[parts[1]]?.get(parts[2]) ?: 0}/5")
            "form" -> r.forms.find { it.id == parts[1] }.let { f -> Triple("Formy", f?.name ?: "?", f?.let { "${it.sequence} · +${it.bonusPercent}% ataku" } ?: "") }
            "cycle" -> Triple("Cykle", when (id) {
                Secrets.CYCLE_EXCESS -> "Cykl Nadmiaru"
                Secrets.CYCLE_REBELLION -> "Cykl Buntu"
                else -> "Harmonia Yin-Yang"
            }, when (id) {
                Secrets.CYCLE_EXCESS -> "bestia, którą żywioł ucznia rodzi: +15% jej obrażeń"
                Secrets.CYCLE_REBELLION -> "atak > 2× obrona bestii odwraca przewagę: +20%"
                else -> "Yin nocą, Yang za dnia: +10%"
            })
            "state" -> SecretState.valueOf(parts[1]).let { Triple("Stany", it.polishName, it.description) }
            "block" -> Triple("Zatory", "Punkt ${parts[1].toInt() + 1}", "otwarcie ×3 kosztu")
            "fengshui" -> Triple("Feng shui", "Miejsce ${parts[1].toInt() + 1}", r.fengShui.getOrNull(parts[1].toInt())?.let { "${it.glyph} ${it.polishName}" } ?: "")
            else -> Triple("Inne", id, "")
        }
    }
}
