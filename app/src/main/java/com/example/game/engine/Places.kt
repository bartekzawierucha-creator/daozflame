package com.example.game.engine

import com.example.model.BeastEgg
import com.example.model.DaoArt
import com.example.model.Disciple
import com.example.model.FacilityType
import com.example.model.HintSource
import com.example.model.Hint
import com.example.model.SpiritElement
import com.example.model.WorldRules

/** Etap 7b: feng shui na stałych miejscach budynków (scena hubu ma stałe sloty – bez dowolnego budowania). */
object FengShui {
    const val SWAP_COST = 1_500
    const val SWAP_COOLDOWN = 10

    val NATURE: Map<FacilityType, SpiritElement> = mapOf(
        FacilityType.CULTIVATION_PAVILION to SpiritElement.WATER,
        FacilityType.ALCHEMY_CHAMBER to SpiritElement.FIRE,
        FacilityType.DIVINE_FORGE to SpiritElement.METAL,
        FacilityType.MARTIAL_ARCHIVES to SpiritElement.WOOD,
        FacilityType.MISSION_HALL to SpiritElement.EARTH,
        FacilityType.DIPLOMACY_HALL to SpiritElement.WOOD
    )
    private val GENERATES = mapOf(
        SpiritElement.WOOD to SpiritElement.FIRE, SpiritElement.FIRE to SpiritElement.EARTH, SpiritElement.EARTH to SpiritElement.METAL,
        SpiritElement.METAL to SpiritElement.WATER, SpiritElement.WATER to SpiritElement.WOOD
    )

    fun secretId(slot: Int) = "fengshui:$slot"

    /** v2 reguł: osobny strumień „rules-v2”, dopisywany bez ruszania starych reguł. */
    fun extend(r: WorldRules, seed: Long): WorldRules {
        if (r.version >= 2) return r
        val rng = GameRng.stream(seed, 0L, "rules-v2")
        val slots = List(FacilityType.entries.size) { SpiritElement.BASIC.random(rng) }
        val hints = slots.flatMapIndexed { i, e ->
            listOf(
                Hint(secretId(i), HintSource.DESCRIPTION, "Geomanta szepcze: ziemia pod miejscem ${i + 1} pachnie jak ${e.polishName.lowercase()}."),
                Hint(secretId(i), HintSource.DREAM, "We śnie widzisz miejsce ${i + 1} sekty spowite znakiem ${e.glyph}.")
            )
        }
        return r.copy(version = 2, fengShui = slots, hints = r.hints + hints)
    }

    fun slotOf(s: GameState, type: FacilityType): Int = s.buildingSlots[type.name] ?: type.ordinal

    /** Premia procentowa Qi z jednego budynku. */
    fun harmony(slotElement: SpiritElement, building: SpiritElement): Double = when {
        slotElement == building -> 3.0
        GENERATES[slotElement] == building -> 1.5
        Elements.overcomes(slotElement, building) -> -2.0
        else -> 0.0
    }

    fun total(s: GameState): Double {
        val r = s.rules ?: return 0.0
        if (r.fengShui.isEmpty()) return 0.0
        return FacilityType.entries.sumOf { t -> harmony(r.fengShui[slotOf(s, t)], NATURE.getValue(t)) }
    }

    fun swap(s: GameState, a: FacilityType, b: FacilityType): StepResult {
        if (a == b) return ok(s)
        if (s.currentDay - s.buildingsSwappedDay < SWAP_COOLDOWN) return fail(s, "Mistrzowie formacji odpoczywają – przeniesienie za ${SWAP_COOLDOWN - (s.currentDay - s.buildingsSwappedDay)} dni.")
        if (s.spiritStones < SWAP_COST) return fail(s, "Przeniesienie budynków kosztuje $SWAP_COST kamieni.")
        val sa = slotOf(s, a)
        val sb = slotOf(s, b)
        var st = s.copy(
            spiritStones = s.spiritStones - SWAP_COST,
            buildingsSwappedDay = s.currentDay,
            buildingSlots = s.buildingSlots + (a.name to sb) + (b.name to sa)
        )
        // Każde przestawienie to obserwacja: gracz czuje, czy Qi płynie lepiej.
        st = Journal.observe(st, secretId(sa), "${a.polishName} → miejsce ${sb + 1}")
        st = Journal.observe(st, secretId(sb), "${b.polishName} → miejsce ${sa + 1}")
        return ok(st, "${a.polishName} i ${b.polishName} zamieniają się miejscami. Feng shui sekty: %+.1f%% Qi.".format(total(st)))
    }
}

/** Lochy pod sektą: coraz głębsze piętra, 2 zejścia na dzień gry, łupy z głębin. */
object Dungeon {
    const val ATTEMPTS = 2
    private val LOOT = listOf("Duchowy Nefryt", "Gwiezdne Żelazo", "Starożytna Ruda Pustki", GameContent.FLAME_CRYSTAL, "Rdzeń Gwiazdy")

    fun attemptsLeft(s: GameState) = if (s.dungeonDay != s.currentDay) ATTEMPTS else (ATTEMPTS - s.dungeonAttempts).coerceAtLeast(0)

    fun delve(s: GameState, ctx: EngineContext, discipleId: String): StepResult {
        val d = s.disciple(discipleId) ?: return ok(s)
        if (attemptsLeft(s) <= 0) return fail(s, "Lochy zamykają się do jutra.")
        val depth = s.dungeonDepth + 1
        val f = Math.pow(1.12, (depth - 1).toDouble())
        val rng = ctx.rng("dungeon")
        val res = Skirmish.resolve(Power.attack(s, d), d.defense, d.currentHp, (500 * f).toInt(), (50 * f).toInt(), (20 * f).toInt(), rng)
        val base = s.copy(dungeonDay = s.currentDay, dungeonAttempts = (if (s.dungeonDay == s.currentDay) s.dungeonAttempts else 0) + 1,
            disciples = s.disciples.map { if (it.id == d.id) it.copy(currentHp = res.hpLeft.coerceAtLeast(10)) else it })
        if (!res.win) return ok(base, "${d.name} wycofuje się z piętra $depth lochów.")
        val item = LOOT[((depth - 1) / 5).coerceAtMost(LOOT.lastIndex)]
        val n = 2 + depth / 3
        var inv = base.inventory + (item to (base.inventory[item] ?: 0) + n)
        var extra = ""
        if (s.rules != null && depth % 4 == 0) {
            val m = Realms.methods.filter { it.id != Realms.BASIC_METHOD }.random(rng)
            val frag = Breath.fragmentItem(m.id)
            inv = inv + (frag to (inv[frag] ?: 0) + 1)
            extra = " Na ścianie wyryto fragment zwoju."
        }
        if (depth % 5 == 0 && rng.nextInt(100) < 50) {
            inv = inv + (Creatures.EGG to (inv[Creatures.EGG] ?: 0) + 1)
            extra += " W gnieździe leży jajo bestii!"
        }
        return ok(base.copy(dungeonDepth = depth, inventory = inv), "🕳 ${d.name} schodzi na piętro $depth: ${n}× $item.$extra")
    }
}

/** Biblioteka sekty (kopie zwojów) i jaskinie odosobnienia. */
object Library {
    const val PAPER = 3
    const val MAX_CAVE = 3

    fun copyCost(grade: com.example.model.MethodGrade): Int = 500 * (grade.ordinal + 1)

    fun copy(s: GameState, scroll: String): StepResult {
        if ((s.inventory[scroll] ?: 0) < 1) return fail(s, "Brak zwoju do skopiowania.")
        val m = Realms.methodForScroll(scroll) ?: return fail(s, "To nie jest zwój metody.")
        if (m.grade.ordinal > com.example.model.MethodGrade.MYSTIC.ordinal) return fail(s, "Zwojów tego stopnia nie da się przepisać – ich znaki się rozmywają.")
        val cost = copyCost(m.grade)
        if (s.spiritStones < cost) return fail(s, "Kopia kosztuje $cost kamieni.")
        val paper = s.inventory[Market.SPIRIT_PAPER] ?: 0
        if (paper < PAPER) return fail(s, "Kopia wymaga $PAPER× ${Market.SPIRIT_PAPER}.")
        return ok(s.copy(spiritStones = s.spiritStones - cost, inventory = s.inventory + (Market.SPIRIT_PAPER to paper - PAPER) + (scroll to (s.inventory[scroll] ?: 0) + 1)),
            "Skryba przepisał: $scroll.")
    }

    fun caveCost(level: Int) = 3_000 * (level + 1)

    fun upgradeCave(s: GameState): StepResult {
        if (s.caveLevel >= MAX_CAVE) return fail(s, "Jaskinie odosobnienia są już wykute do końca.")
        val cost = caveCost(s.caveLevel)
        if (s.spiritStones < cost) return fail(s, "Pogłębienie jaskiń kosztuje $cost kamieni.")
        return ok(s.copy(spiritStones = s.spiritStones - cost, caveLevel = s.caveLevel + 1), "Jaskinie odosobnienia: poziom ${s.caveLevel + 1} (odosobnienie ×${3 + 0.25 * (s.caveLevel + 1)}).")
    }
}

/** Istoty: jaja, ewolucja (3 etapy), wierzchowce, duchy artefaktów i płomieni, fuzja i pożeranie płomieni. */
object Creatures {
    const val EGG = "Jajo Bestii"
    const val HATCH_DAYS = 25
    const val MAX_TIER = 3
    const val FUSION_EXPLOSION = 30
    const val SPIRIT_PLUS = 7

    fun incubate(s: GameState, ctx: EngineContext): StepResult {
        val have = s.inventory[EGG] ?: 0
        if (have < 1) return fail(s, "Brak jaja bestii.")
        val template = com.example.game.WuxiaGenerator.getInitialSectBeasts().random(ctx.rng("egg")).id
        val egg = BeastEgg(ctx.rng("egg-id").nextId("egg"), s.currentDay + HATCH_DAYS, template)
        return ok(s.copy(inventory = s.inventory + (EGG to have - 1), eggs = s.eggs + egg), "Jajo spoczywa w cieple paleniska. Wykluje się za $HATCH_DAYS dni.")
    }

    fun daily(s0: GameState): Pair<GameState, List<String>> {
        var s = s0
        val notes = mutableListOf<String>()
        val ready = s.eggs.filter { it.hatchDay <= s.currentDay }
        if (ready.isNotEmpty()) {
            val templates = com.example.game.WuxiaGenerator.getInitialSectBeasts().associateBy { it.id }
            ready.forEach { e ->
                val t = templates[e.template] ?: return@forEach
                val young = t.copy(id = e.id, name = "Młody " + t.name, tier = 1, intimacy = 50, currentEnergy = t.maxEnergy / 2)
                s = s.copy(sectBeasts = s.sectBeasts + young, activeBeastId = s.activeBeastId ?: young.id)
                notes += "🥚 Z jaja wykluwa się ${young.name}!"
            }
            s = s.copy(eggs = s.eggs - ready.toSet())
        }
        return s to notes
    }

    fun mount(s: GameState, beastId: String, discipleId: String?): StepResult {
        val b = s.sectBeasts.find { it.id == beastId } ?: return ok(s)
        if (discipleId == null) return ok(s.copy(mounts = s.mounts - beastId), "${b.name} wraca do zagrody.")
        val d = s.disciple(discipleId) ?: return ok(s)
        if (b.intimacy < 50) return fail(s, "${b.name} nie ufa jeszcze jeźdźcowi (więź ≥ 50%).")
        return ok(s.copy(mounts = s.mounts.filterValues { it != discipleId } + (beastId to discipleId)), "${d.name} dosiada ${b.name}.")
    }

    fun flameTemperature(s: GameState, f: com.example.model.GeocentricFlame): Int = Flames.temperature(f) + (s.flameHeat[f.id] ?: 0)

    /** Fuzja dwóch płomieni: 30% wybuchu niszczy oba, sukces daje silniejszy płomień. Zawsze z podwójnym potwierdzeniem. */
    fun fuse(s: GameState, ctx: EngineContext, a: String, b: String, confirmed: Boolean, confirmedTwice: Boolean): StepResult {
        if (a == b) return ok(s)
        val fa = s.flames.find { it.id == a } ?: return ok(s)
        val fb = s.flames.find { it.id == b } ?: return ok(s)
        if (!confirmed || !confirmedTwice) return fail(s, "Fuzja może zniszczyć oba płomienie – potwierdź dwukrotnie.")
        val keep = if (Flames.info(a).position <= Flames.info(b).position) fa else fb
        val gone = if (keep == fa) fb else fa
        val detach = { st: GameState, id: String -> st.copy(
            activeFlameId = st.activeFlameId.takeIf { it != id }, forgeFlameId = st.forgeFlameId.takeIf { it != id },
            disciples = st.disciples.map { if (it.flameId == id) it.copy(flameId = null) else it }) }
        if (ctx.rng("fusion").nextInt(100) < FUSION_EXPLOSION) {
            val st = detach(detach(s, a), b).copy(flames = s.flames.filter { it.id != a && it.id != b })
            return ok(WorldAI.note(st, "Fuzja płomieni zakończona wybuchem."), "💥 Płomienie ${fa.name} i ${fb.name} eksplodują! Oba przepadły.")
        }
        val fused = keep.copy(refinementLevel = (maxOf(fa.refinementLevel, fb.refinementLevel) + 2).coerceAtMost(Flames.MAX_REFINEMENT))
        val st = detach(s, gone.id).copy(
            flames = s.flames.filter { it.id != gone.id }.map { if (it.id == keep.id) fused else it },
            flameHeat = s.flameHeat - gone.id + (keep.id to (s.flameHeat[keep.id] ?: 0) + 500)
        )
        return ok(WorldAI.note(st, "${keep.name} pochłania ${gone.name} w fuzji."), "🔥 Fuzja udana! ${keep.name} płonie mocniej (+500°, oczyszczenie ${fused.refinementLevel}).")
    }

    /** Pożeranie: płomień zjada słabszy – bez ryzyka, ale z mniejszym zyskiem. */
    fun devour(s: GameState, eater: String, food: String): StepResult {
        if (eater == food) return ok(s)
        val fe = s.flames.find { it.id == eater } ?: return ok(s)
        val ff = s.flames.find { it.id == food } ?: return ok(s)
        if (Flames.info(food).position < Flames.info(eater).position) return fail(s, "Słabszy płomień nie pożre silniejszego.")
        val gain = if (Flames.info(food).rank.ordinal >= Flames.info(eater).rank.ordinal) 2 else 1
        val st = s.copy(
            activeFlameId = s.activeFlameId.takeIf { it != food }, forgeFlameId = s.forgeFlameId.takeIf { it != food },
            disciples = s.disciples.map { if (it.flameId == food) it.copy(flameId = null) else it },
            flames = s.flames.filter { it.id != food }.map { if (it.id == eater) it.copy(refinementLevel = (it.refinementLevel + gain).coerceAtMost(Flames.MAX_REFINEMENT)) else it }
        )
        return ok(st, "${fe.name} pożera ${ff.name} (+$gain oczyszczenia).")
    }

    /** Duch płomienia budzi się przy pełnym oczyszczeniu. */
    fun awakenFlameSpirits(s: GameState): Pair<GameState, List<String>> {
        val fresh = s.flames.filter { it.refinementLevel >= Flames.MAX_REFINEMENT && it.id !in s.flameSpirits }
        if (fresh.isEmpty()) return s to emptyList()
        return s.copy(flameSpirits = s.flameSpirits + fresh.map { it.id }) to fresh.map { "🜂 W ${it.name} budzi się duch płomienia!" }
    }

    fun modifiers(s: GameState, d: Disciple): List<Modifier> = buildList {
        val beast = s.mounts.entries.firstOrNull { it.value == d.id }?.key?.let { id -> s.sectBeasts.find { it.id == id } }
        if (beast != null) add(Modifier("Wierzchowiec: ${beast.name}", Stat.COMBAT_DAMAGE, ModOp.PERCENT, 5.0 * beast.tier))
        if (d.flameId != null && d.flameId in s.flameSpirits) add(Modifier("Duch płomienia", Stat.COMBAT_DAMAGE, ModOp.PERCENT, 10.0))
    }
}

/** Codzienność: sztuki Dao, choroby (z limitem zarazy) i klątwy, astrologia. */
object DailyLife {
    const val SICK_CHANCE = 2
    const val SPREAD_CHANCE = 20
    const val MAX_SICK = 3
    const val SICK_DAYS = 8
    const val CURSE_CHANCE = 2
    const val CLEANSE_COST = 1_000
    const val CURE_COST = 300
    const val STARS_COST = 100

    fun sick(s: GameState, d: Disciple) = d.illUntilDay >= s.currentDay

    fun assignArt(s: GameState, discipleId: String, art: DaoArt?): StepResult {
        val d = s.disciple(discipleId) ?: return ok(s)
        return ok(s.copy(disciples = s.disciples.map { if (it.id == d.id) it.copy(art = art) else it }),
            art?.let { "${d.name} poświęca wolne chwile sztuce: ${it.polishName}." } ?: "${d.name} porzuca sztuki.")
    }

    fun daily(s0: GameState, ctx: EngineContext): Pair<GameState, List<String>> {
        var s = s0
        val notes = mutableListOf<String>()
        val rng = ctx.rng("daily-life")
        // Sztuki Dao.
        val guqin = s.disciples.count { it.art == DaoArt.GUQIN }.coerceAtMost(3)
        if (guqin > 0 && s.e2Initialized) s = s.copy(morale = (s.morale + guqin).coerceAtMost(100))
        if (s.currentDay % 30 == 0) s = s.copy(disciples = s.disciples.map { if (it.art == DaoArt.CALLIGRAPHY) it.copy(comprehension = it.comprehension + 1) else it })
        // Zaraza: nowa choroba rzadko, rozprzestrzenianie z limitem 3 chorych.
        val sickNow = s.disciples.count { sick(s, it) }
        if (sickNow == 0 && s.disciples.size > 3 && rng.nextInt(100) < SICK_CHANCE) {
            val v = s.disciples.random(rng)
            s = s.copy(disciples = s.disciples.map { if (it.id == v.id) it.copy(illUntilDay = s.currentDay + SICK_DAYS) else it })
            notes += "🤒 ${v.name} zachorował na gorączkę meridianów. Lecz go, zanim zaraza się rozniesie."
        } else if (sickNow in 1 until MAX_SICK && rng.nextInt(100) < SPREAD_CHANCE) {
            s.disciples.filter { !sick(s, it) }.randomOrNull(rng)?.let { v ->
                s = s.copy(disciples = s.disciples.map { if (it.id == v.id) it.copy(illUntilDay = s.currentDay + SICK_DAYS) else it })
                notes += "🤒 Gorączka przechodzi na: ${v.name}."
            }
        }
        // Klątwy w demonicznej sekcie.
        if (s.karma <= -Identity.KARMA_THRESHOLD && rng.nextInt(100) < CURSE_CHANCE) {
            s.disciples.filter { !it.cursed }.randomOrNull(rng)?.let { v ->
                s = s.copy(disciples = s.disciples.map { if (it.id == v.id) it.copy(cursed = true) else it })
                notes += "☠ Klątwa spada na ${v.name}. Oczyść go w świątyni."
            }
        }
        return s to notes
    }

    fun cure(s: GameState, discipleId: String): StepResult {
        val d = s.disciple(discipleId) ?: return ok(s)
        if (!sick(s, d)) return fail(s, "${d.name} jest zdrowy.")
        val talisman = s.inventory["Talizman Uzdrowienia"] ?: 0
        val st = when {
            talisman > 0 -> s.copy(inventory = s.inventory + ("Talizman Uzdrowienia" to talisman - 1))
            s.spiritStones >= CURE_COST -> s.copy(spiritStones = s.spiritStones - CURE_COST)
            else -> return fail(s, "Leczenie kosztuje $CURE_COST kamieni albo Talizman Uzdrowienia.")
        }
        return ok(st.copy(disciples = st.disciples.map { if (it.id == d.id) it.copy(illUntilDay = 0) else it }), "${d.name} wraca do zdrowia.")
    }

    fun cleanse(s: GameState, discipleId: String): StepResult {
        val d = s.disciple(discipleId) ?: return ok(s)
        if (!d.cursed) return fail(s, "Na ${d.name} nie ciąży klątwa.")
        val cost = if (s.templeLevel >= 2) 0 else CLEANSE_COST
        if (s.spiritStones < cost) return fail(s, "Oczyszczenie kosztuje $cost kamieni (świątynia poz. 2 – za darmo).")
        return ok(s.copy(spiritStones = s.spiritStones - cost, disciples = s.disciples.map { if (it.id == d.id) it.copy(cursed = false) else it }),
            "Klątwa opuszcza ${d.name}.")
    }

    /** Astrologia: prognoza z tego, co gwiazdy już „wiedzą” (zapowiedzi, otwarcia, fabuła) i jedna wskazówka. */
    fun readStars(s: GameState, ctx: EngineContext): StepResult {
        if (s.starsReadDay == s.currentDay) return fail(s, "Gwiazdy przemówiły już dziś.")
        if (s.spiritStones < STARS_COST) return fail(s, "Astrolog bierze $STARS_COST kamieni.")
        val lines = buildList {
            s.raid?.let { add("Czerwona gwiazda nad bramą: ${it.sectName} za ${it.arriveDay - s.currentDay} dni.") }
            s.catastrophe?.let { add("Ciemne niebo: ${it.type.polishName} od dnia ${it.startDay}.") }
            SecretRealms.REALMS.map { r -> r to (s.currentDay..s.currentDay + SecretRealms.EVERY).firstOrNull { SecretRealms.openingDay(r, it) == it } }
                .filter { it.second != null }.minByOrNull { it.second!! }?.let { (r, d) -> add("${r.name} otworzy się dnia $d.") }
            if (Story.ending(s) == 0 && Story.act(s) > 0) add("Góra wciąż drży. Akt ${Story.act(s)} trwa.")
        }
        var st = s.copy(spiritStones = s.spiritStones - STARS_COST, starsReadDay = s.currentDay)
        val hint = s.rules?.hints?.filter { !Journal.certain(s, it.secretId) }?.randomOrNull(ctx.rng("stars"))
        if (hint != null) st = Journal.observe(st, hint.secretId, "Gwiazdy: ${hint.text}", cap = com.example.model.KnowledgeLevel.LIKELY)
        return ok(st, "🔭 " + (lines.ifEmpty { listOf("Niebo jest spokojne.") }).joinToString(" ") + (hint?.let { " Gwiazdy szepczą też o czymś ukrytym (Dziennik)." } ?: ""))
    }

    fun modifiers(s: GameState, d: Disciple): List<Modifier> = buildList {
        when (d.art) {
            DaoArt.PAINTING -> add(Modifier("Malarstwo", Stat.EPIPHANY_CHANCE, ModOp.PERCENT, 20.0))
            DaoArt.GO -> add(Modifier("Go", Stat.BREAKTHROUGH_CHANCE, ModOp.FLAT, 3.0))
            else -> Unit
        }
        if (sick(s, d)) add(Modifier("Gorączka meridianów", Stat.QI_GAIN, ModOp.MULTIPLY, 0.7))
        if (d.cursed) add(Modifier("Klątwa", Stat.QI_GAIN, ModOp.MULTIPLY, 0.8))
        if (s.caveLevel > 0 && d.secludedUntilDay >= s.currentDay) add(Modifier("Jaskinia odosobnienia", Stat.QI_GAIN, ModOp.MULTIPLY, (3 + 0.25 * s.caveLevel) / 3))
    }
}
