package com.example.game.engine

import com.example.game.engine.DiscipleRules.has

import com.example.model.BiomeType
import com.example.model.Disciple
import com.example.model.ExpNode
import com.example.model.ExpNodeType
import com.example.model.ExpeditionRun
import com.example.model.Season
import com.example.model.SpiritElement
import com.example.model.WorldRegion
import kotlin.math.pow
import kotlin.random.Random

/** Etap 5a: jawne cykle żywiołów w walce (ukryte cykle Nadmiaru i Buntu dopiero w E6). */
object Elements {
    const val ADVANTAGE = 0.30

    fun of(biome: BiomeType): SpiritElement = when (biome) {
        BiomeType.BAMBOO_MIST -> SpiritElement.WOOD
        BiomeType.KUNLUN_PEAK -> SpiritElement.ICE
        BiomeType.LAVA_CHASM -> SpiritElement.FIRE
        BiomeType.VENOM_SWAMP -> SpiritElement.WATER
        BiomeType.SPIRIT_DESERT -> SpiritElement.EARTH
        BiomeType.DRAGON_ABYSS -> SpiritElement.WATER
        BiomeType.THUNDER_PLATEAU -> SpiritElement.THUNDER
        BiomeType.SUNKEN_PALACE -> SpiritElement.WATER
        BiomeType.BONE_WASTES -> SpiritElement.METAL
        BiomeType.CELESTIAL_ISLES -> SpiritElement.WIND
        BiomeType.STAR_RUINS -> SpiritElement.METAL
        BiomeType.PRIMORDIAL_CHAOS -> SpiritElement.CHAOS
    }

    /** Cykl przezwyciężania: Drewno→Ziemia→Woda→Ogień→Metal→Drewno; Ogień→Lód→Wiatr→Grom→Woda. */
    private val OVERCOMES: Map<SpiritElement, Set<SpiritElement>> = mapOf(
        SpiritElement.WOOD to setOf(SpiritElement.EARTH),
        SpiritElement.EARTH to setOf(SpiritElement.WATER),
        SpiritElement.WATER to setOf(SpiritElement.FIRE),
        SpiritElement.FIRE to setOf(SpiritElement.METAL, SpiritElement.ICE),
        SpiritElement.METAL to setOf(SpiritElement.WOOD),
        SpiritElement.ICE to setOf(SpiritElement.WIND),
        SpiritElement.WIND to setOf(SpiritElement.THUNDER),
        SpiritElement.THUNDER to setOf(SpiritElement.WATER)
    )

    fun overcomes(a: SpiritElement, b: SpiritElement): Boolean = b in (OVERCOMES[a] ?: emptySet())

    fun elementsOf(d: Disciple): List<SpiritElement> = d.spiritRoot?.elements ?: emptyList()

    /** Mnożnik obrażeń ucznia przeciw żywiołowi bestii. */
    fun attackFactor(d: Disciple, beast: SpiritElement): Double =
        if (elementsOf(d).any { overcomes(it, beast) }) 1 + ADVANTAGE else 1.0

    /** Mnożnik obrażeń bestii przeciw uczniowi. */
    fun defenseFactor(d: Disciple, beast: SpiritElement): Double =
        if (elementsOf(d).any { overcomes(beast, it) }) 1 + ADVANTAGE else 1.0
}

/** Szybka walka rozstrzygana bez animacji (węzły wypraw, tryb Auto, Wieża Prób). */
object Skirmish {
    data class Result(val win: Boolean, val hpLeft: Int, val rounds: Int)

    fun resolve(attack: Int, defense: Int, hp: Int, enemyHp: Int, enemyAttack: Int, enemyDefense: Int, rng: Random): Result {
        var my = hp
        var their = enemyHp
        var round = 0
        while (my > 0 && their > 0 && round < ActiveCombat.MAX_ROUNDS) {
            round++
            val crit = rng.nextInt(100) < CombatSystem.CRIT_CHANCE
            their -= ((attack - enemyDefense * 0.4) * (if (crit) CombatSystem.CRIT_MULTIPLIER else 1.0)).toInt().coerceAtLeast(20)
            if (their <= 0) break
            my -= (enemyAttack - defense * 0.35).toInt().coerceAtLeast(12)
        }
        return Result(their <= 0 && my > 0, my.coerceAtLeast(0), round)
    }
}

/** Etap 5a: zdobywanie płomieni – trop → Źródło na wyprawie → poskromienie w 3 fazach → płomień w skarbcu. */
object FlameSources {
    data class Source(val regionId: String, val season: Season?)

    val SOURCES: Map<String, Source> = mapOf(
        "flame_1" to Source("reg_4", null),
        "flame_2" to Source("reg_2", Season.WINTER),
        "flame_3" to Source("reg_5", Season.SUMMER),
        "flame_4" to Source("reg_11", null),
        "flame_5" to Source("reg_8", Season.WINTER),
        "flame_6" to Source("reg_6", null),
        "flame_7" to Source("reg_10", Season.SUMMER),
        "flame_8" to Source("reg_9", Season.AUTUMN),
        "flame_9" to Source("reg_7", null),
        "flame_10" to Source("reg_1", Season.SPRING),
        "flame_11" to Source("reg_3", Season.AUTUMN),
        "flame_12" to Source("reg_12", null)
    )

    const val PHASES = 3
    fun trailFlag(flameId: String) = "trop:$flameId"

    /** Trop płomienia krainy pojawia się, gdy kraina zostaje zbadana w całości. */
    fun onRegionExplored(s: GameState, regionId: String): Pair<GameState, String?> {
        val owned = s.flames.map { it.id }.toSet()
        val flameId = SOURCES.entries.firstOrNull { it.value.regionId == regionId && it.key !in owned }?.key ?: return s to null
        if ((s.flags[trailFlag(flameId)] ?: 0) > 0) return s to null
        val name = allFlames[flameId]?.name ?: flameId
        val season = SOURCES.getValue(flameId).season?.let { " (${it.polishName.lowercase()})" } ?: ""
        return s.copy(flags = s.flags + (trailFlag(flameId) to 1)) to "🜂 Trop: $name kryje się w tej krainie$season."
    }

    val allFlames by lazy { com.example.game.WuxiaGenerator.getGeocentricFlames().associateBy { it.id } }

    /** Płomień, którego Źródło może się pokazać na tej wyprawie. */
    fun available(s: GameState, regionId: String): String? = SOURCES.entries.firstOrNull { (id, src) ->
        src.regionId == regionId && (s.flags[trailFlag(id)] ?: 0) > 0 && s.flames.none { it.id == id } &&
            (src.season == null || src.season == Seasons.of(s.currentDay))
    }?.key

    /** Szansa (%) na jedną fazę poskromienia. */
    fun phaseChance(d: Disciple, flameId: String): Int {
        val need = Flames.info(flameId).baseTemperature / 4.0
        val power = (d.attack + d.defense).toDouble()
        return (50 + (power - need) / need * 50).toInt().coerceIn(10, 90)
    }
}

/** Etap 5a: wyprawy jako mapa węzłów (3–5 warstw po 2 ścieżki, na końcu legowisko bossa). */
object Expeditions {
    const val AUTO_LOOT = 0.8
    const val DEFEAT_LOOT = 0.5
    const val RING = "Pierścień Przestrzenny"
    const val SLOTS = 4
    const val RING_SLOTS = 10
    const val RING_STONES = 2_000
    const val RING_MINERAL = "Gwiezdne Żelazo"
    const val RING_MINERAL_COUNT = 5

    fun slots(s: GameState): Int = if ((s.inventory[RING] ?: 0) > 0) RING_SLOTS else SLOTS

    private val WEIGHTS = listOf(
        ExpNodeType.FIGHT to 35, ExpNodeType.HERB to 20, ExpNodeType.TREASURE to 15,
        ExpNodeType.SHRINE to 10, ExpNodeType.TRAP to 10, ExpNodeType.REST to 10
    )

    private fun roll(rng: Random): ExpNodeType {
        var r = rng.nextInt(WEIGHTS.sumOf { it.second })
        for ((t, w) in WEIGHTS) { if (r < w) return t; r -= w }
        return ExpNodeType.FIGHT
    }

    /** Mapa zależy tylko od ziarna świata, kroku i krainy – ten sam stan daje tę samą mapę. */
    fun generate(s: GameState, region: WorldRegion): List<List<ExpNode>> {
        val rng = GameRng.stream(s.worldSeed, s.step, "expedition:${region.id}")
        val count = 3 + rng.nextInt(3)
        val layers = MutableList(count) {
            // Etap 11: siła węzła waha się 70–150% – zwiad ma sens.
            List(2) { val t = roll(rng); ExpNode(t, power = if (t == ExpNodeType.FIGHT) region.dangerLevel * (70 + rng.nextInt(81)) / 100 else region.dangerLevel) }.distinctBy { n -> n.type }
                .let { l -> if (l.size == 1) l + ExpNode(ExpNodeType.REST) else l }
        }
        FlameSources.available(s, region.id)?.let { id ->
            layers[count - 1] = listOf(layers[count - 1].first(), ExpNode(ExpNodeType.FLAME_SOURCE, flameId = id))
        }
        return layers
    }

    /** Udział towarzysza w sile drużyny – zależy od więzi (przysięgli walczą jak jedno ciało). */
    fun companionShare(s: GameState, leader: String, companion: String): Double = when (com.example.model.BondKind.of(Bonds.value(s, leader, companion))) {
        com.example.model.BondKind.SWORN -> 0.75
        com.example.model.BondKind.FRIEND -> 0.6
        com.example.model.BondKind.RIVAL -> 0.35
        com.example.model.BondKind.ENEMY -> 0.2
        com.example.model.BondKind.NEUTRAL -> 0.5
    }

    /** Siła drużyny: (atak, obrona) prowadzącego plus część towarzysza. */
    fun team(s: GameState, run: ExpeditionRun, d: com.example.model.Disciple): Pair<Int, Int> {
        val c = run.companionId?.let { s.disciple(it) } ?: return d.attack to d.defense
        val f = companionShare(s, d.id, c.id)
        return (d.attack + c.attack * f).toInt() to (d.defense + c.defense * f).toInt()
    }

    /** Dopasowanie żywiołu ucznia do krainy w % (100 = obojętne). */
    fun matchup(d: com.example.model.Disciple, region: WorldRegion): Int = (Elements.attackFactor(d, Elements.of(region.biome)) * 100).toInt()

    /** Ile warstw naprzód widać: 1, +1 z towarzyszem, +1 dla wnikliwego. */
    fun sightOf(d: com.example.model.Disciple, companion: com.example.model.Disciple?): Int =
        1 + (if (companion != null) 1 else 0) + (if (d.has("insightful") || companion?.has("insightful") == true) 1 else 0)

    fun begin(s: GameState, regionId: String, discipleId: String, auto: Boolean, ctx: EngineContext, companionId: String? = null): StepResult {
        if (s.expedition != null || s.combat != null) return fail(s, "Inna wyprawa jest w toku.")
        val region = s.regions.find { it.id == regionId } ?: return ok(s)
        val d = s.disciple(discipleId) ?: return ok(s)
        if (s.discoveredRegionIds.isNotEmpty() && regionId !in s.discoveredRegionIds) return fail(s, "Kraina kryje się we mgle – najpierw wyślij zwiad.")
        if (d.currentHp < d.maxHp / 3) return fail(s, "${d.name} jest zbyt ranny na wyprawę.")
        val comp = companionId?.takeIf { it != discipleId }?.let { s.disciple(it) }
        if (comp != null && comp.currentHp < comp.maxHp / 3) return fail(s, "${comp.name} jest zbyt ranny na wyprawę.")
        if (comp != null && comp.secludedUntilDay >= s.currentDay) return fail(s, "${comp.name} trwa w odosobnieniu.")
        val run = ExpeditionRun(regionId, discipleId, generate(s, region), hp = d.currentHp, auto = auto,
            log = listOf("${d.name} wyrusza do krainy ${region.name}." + (comp?.let { " Towarzyszy ${Autonomy.short(it)}." } ?: "")),
            companionId = comp?.id, sight = sightOf(d, comp))
        val started = s.copy(expedition = run)
        return if (auto) runAuto(started, ctx) else ok(started, "Wyprawa: ${region.name}. Wybierz ścieżkę.")
    }

    /** Wybór węzła w bieżącej warstwie. Po ostatniej warstwie – [toBoss]. */
    fun choose(s: GameState, index: Int, ctx: EngineContext): StepResult {
        val run = s.expedition ?: return ok(s)
        if (run.atBoss || run.layer >= run.layers.size) return fail(s, "Przed tobą legowisko bestii.")
        val node = run.layers[run.layer].getOrNull(index) ?: return ok(s)
        val (after, ended) = resolve(s, run, node, ctx)
        if (ended) return retreat(s, after, "Wyprawa przerwana – uczeń wraca ranny.")
        return ok(s.copy(expedition = after.copy(layer = after.layer + 1)), after.log.last())
    }

    private fun resolve(s: GameState, run: ExpeditionRun, node: ExpNode, ctx: EngineContext): Pair<ExpeditionRun, Boolean> {
        val region = s.regions.first { it.id == run.regionId }
        val d = s.disciple(run.discipleId)!!
        val boss = region.bossBeast
        val rng = ctx.rng("expedition-node")
        fun add(r: ExpeditionRun, item: String, n: Int): ExpeditionRun {
            if (n <= 0) return r
            val full = item !in r.loot && r.loot.size >= slots(s)
            if (full) return r.copy(log = r.log + "Pierścień pełny – $item zostaje na ścieżce.")
            return r.copy(loot = r.loot + (item to (r.loot[item] ?: 0) + n))
        }
        var r = run
        when (node.type) {
            ExpNodeType.FIGHT -> {
                val el = Elements.of(region.biome)
                val (atk, def) = team(s, r, d)
                val f = if (region.dangerLevel > 0) node.power.toDouble() / region.dangerLevel else 1.0
                val res = Skirmish.resolve(
                    attack = (atk * Elements.attackFactor(d, el)).toInt(), defense = def, hp = r.hp,
                    enemyHp = ((boss?.hp ?: 500) * 3 / 10 * f).toInt(), enemyAttack = ((boss?.attack ?: 50) * 0.6 * f * Elements.defenseFactor(d, el)).toInt(),
                    enemyDefense = (boss?.defense ?: 20) / 2, rng = rng
                )
                if (!res.win) return r.copy(hp = res.hpLeft.coerceAtLeast(10), log = r.log + "⚔ Strażnicy krainy przepędzili ucznia.") to true
                r = add(r.copy(hp = res.hpLeft, stones = r.stones + (boss?.spiritStoneReward ?: 100) / 5,
                    log = r.log + "⚔ Pokonano strażników krainy (${res.rounds} rund)."), region.mineralResource, 1)
                r = add(r, region.herbResource, 1)
            }
            ExpNodeType.HERB -> r = add(r.copy(log = r.log + "🌿 Polana ziół."), region.herbResource, 2 + rng.nextInt(3))
            ExpNodeType.TREASURE -> {
                r = add(r.copy(stones = r.stones + (boss?.spiritStoneReward ?: 100) / 4, log = r.log + "💎 Ukryta skrytka."), region.mineralResource, 2)
                // Etap 6: szyfrowane fragmenty zwojów.
                if (s.rules != null && rng.nextInt(100) < 25) {
                    val m = Realms.methods.filter { it.id != Realms.BASIC_METHOD }.random(rng)
                    r = add(r.copy(log = r.log + "📜 Wśród kosztowności leży szyfrowany fragment zwoju."), Breath.fragmentItem(m.id), 1)
                }
            }
            ExpNodeType.SHRINE -> r = r.copy(log = r.log + "⛩ Kapliczka przodków – uczeń medytuje.", loot = r.loot + ("@qi" to (r.loot["@qi"] ?: 0) + 60))
            ExpNodeType.TRAP -> {
                val dmg = (d.maxHp * 0.15 * (if (d.defense > region.dangerLevel * 25) 0.5 else 1.0)).toInt()
                r = r.copy(hp = (r.hp - dmg).coerceAtLeast(1), log = r.log + "⚠ Pułapka formacji: −$dmg HP.")
            }
            ExpNodeType.REST -> {
                val heal = d.maxHp * 3 / 10
                r = r.copy(hp = (r.hp + heal).coerceAtMost(d.maxHp), log = r.log + "🔥 Odpoczynek przy ognisku: +$heal HP.")
            }
            ExpNodeType.FLAME_SOURCE -> {
                val id = node.flameId ?: return r to false
                val chance = FlameSources.phaseChance(d, id)
                var phase = 0
                while (phase < FlameSources.PHASES && rng.nextInt(100) < chance) phase++
                val name = FlameSources.allFlames[id]?.name ?: id
                r = if (phase == FlameSources.PHASES) {
                    r.copy(loot = r.loot + ("@flame:$id" to 1), log = r.log + "🜂 $name poskromiony! (3/3 fazy)")
                } else {
                    val dmg = d.maxHp / 4
                    r.copy(hp = (r.hp - dmg).coerceAtLeast(1), log = r.log + "🜂 $name wyrwał się w fazie ${phase + 1}/3 (−$dmg HP). Źródło czeka.")
                }
            }
            ExpNodeType.BOSS -> Unit
        }
        return r to (r.hp <= 1 && node.type == ExpNodeType.TRAP)
    }

    /** Wejście do legowiska: zwykła walka krok po kroku, łupy z wyprawy rozlicza koniec walki. */
    fun toBoss(s: GameState): StepResult {
        val run = s.expedition ?: return ok(s)
        if (run.layer < run.layers.size) return fail(s, "Najpierw przejdź wszystkie ścieżki.")
        val withHp = s.copy(disciples = s.disciples.map { if (it.id == run.discipleId) it.copy(currentHp = run.hp) else it })
        val started = CombatSystem.start(withHp, run.regionId, run.discipleId)
        // Towarzysz wspiera w legowisku.
        val c = started.state.combat
        val d = s.disciple(run.discipleId)
        val boosted = if (c != null && d != null && run.companionId != null) {
            val (atk, _) = team(s, run, d)
            c.copy(effectiveAttack = c.effectiveAttack + (atk - d.attack).coerceAtLeast(0))
        } else c
        return started.copy(state = started.state.copy(combat = boosted, expedition = run.copy(atBoss = true)))
    }

    /** Odwrót bez bossa: połowa łupów. */
    fun retreat(s: GameState, run: ExpeditionRun? = s.expedition, message: String = "Odwrót z wyprawy."): StepResult {
        if (run == null) return ok(s)
        val paid = payout(s, run, DEFEAT_LOOT)
        return ok(paid.copy(expedition = null), "$message Zabrano połowę łupów.")
    }

    /** Rozliczenie łupów do skarbca. Klucze z „@” to nagrody specjalne (Qi, płomień). */
    fun payout(s: GameState, run: ExpeditionRun, factor: Double): GameState {
        var st = s
        val inv = st.inventory.toMutableMap()
        run.loot.forEach { (k, v) ->
            when {
                k == "@qi" -> st = st.copy(disciples = st.disciples.map { d ->
                    if (d.id != run.discipleId) d
                    else if (st.e3Initialized) CultivationSystem.gainQi(d, CultivationSystem.ticksWorth(st, d, (v * factor).toInt()))
                    else d.copy(realmProgress = (d.realmProgress + (v * factor).toInt() / 12).coerceAtMost(100))
                })
                k.startsWith("@flame:") -> {
                    // Płomień poskromiony zostaje zawsze – nawet przy odwrocie.
                    val id = k.removePrefix("@flame:")
                    FlameSources.allFlames[id]?.let { f -> if (st.flames.none { it.id == id }) st = st.copy(flames = st.flames + f.copy(refinementLevel = 1)) }
                    st = st.copy(flags = st.flags - FlameSources.trailFlag(id))
                }
                else -> inv[k] = (inv[k] ?: 0) + (v * factor).toInt().coerceAtLeast(if (factor >= 1.0) v else 0)
            }
        }
        run.companionId?.let { st = Bonds.shift(st, run.discipleId, it, 10) }
        return st.copy(
            inventory = inv.toMap(),
            spiritStones = st.spiritStones + (run.stones * factor).toInt(),
            disciples = st.disciples.map { if (it.id == run.discipleId && !run.atBoss) it.copy(currentHp = run.hp.coerceAtLeast(10)) else it }
        )
    }

    /** Tryb Auto: bezpieczniejsze ścieżki, boss rozstrzygnięty od razu, łupy ×0,8. */
    private fun runAuto(s: GameState, ctx: EngineContext): StepResult {
        var run = s.expedition ?: return ok(s)
        val d = s.disciple(run.discipleId)!!
        val prefer = listOf(ExpNodeType.FLAME_SOURCE, ExpNodeType.TREASURE, ExpNodeType.HERB, ExpNodeType.SHRINE, ExpNodeType.REST, ExpNodeType.FIGHT, ExpNodeType.TRAP)
        while (run.layer < run.layers.size) {
            val options = run.layers[run.layer]
            val pick = if (run.hp < d.maxHp * 4 / 10) options.firstOrNull { it.type == ExpNodeType.REST } ?: options.minBy { prefer.indexOf(it.type) }
            else options.minBy { prefer.indexOf(it.type) }
            val (after, ended) = resolve(s, run, pick, ctx)
            if (ended) return retreat(s, after, "Wyprawa (auto) przerwana.")
            run = after.copy(layer = after.layer + 1)
        }
        val region = s.regions.first { it.id == run.regionId }
        val boss = region.bossBeast ?: return ok(payout(s, run, AUTO_LOOT).copy(expedition = null), "Wyprawa (auto) zakończona.")
        val el = Elements.of(region.biome)
        val (tAtk, tDef) = team(s, run, d)
        val res = Skirmish.resolve(
            attack = (tAtk * Elements.attackFactor(d, el)).toInt(), defense = tDef, hp = run.hp,
            enemyHp = boss.hp, enemyAttack = (boss.attack * Elements.defenseFactor(d, el)).toInt(), enemyDefense = boss.defense,
            rng = ctx.rng("expedition-boss")
        )
        val finalRun = run.copy(hp = res.hpLeft.coerceAtLeast(10))
        if (!res.win) {
            val paid = payout(s, finalRun, AUTO_LOOT * DEFEAT_LOOT)
            return ok(paid.copy(expedition = null, sectReputation = paid.sectReputation - 5), "Wyprawa (auto): ${boss.name} odparł ucznia. Zabrano część łupów.")
        }
        var paid = payout(s, finalRun.copy(stones = finalRun.stones + boss.spiritStoneReward), AUTO_LOOT)
        val inv = paid.inventory.toMutableMap()
        inv[boss.dropItemName] = (inv[boss.dropItemName] ?: 0) + 1
        paid = paid.copy(
            inventory = inv.toMap(),
            expedition = null,
            sectReputation = paid.sectReputation + 25,
            regions = paid.regions.map { if (it.id == region.id) it.copy(explorationProgress = (it.explorationProgress + 25).coerceAtMost(100)) else it }
        )
        val notes = mutableListOf("Wyprawa (auto): pokonano ${boss.name}.")
        if (paid.regions.first { it.id == region.id }.explorationProgress >= 100) {
            val (withTrail, msg) = FlameSources.onRegionExplored(paid, region.id)
            paid = withTrail
            msg?.let { notes += it }
        }
        if (paid.e4Initialized) {
            val (withContracts, msg) = Contracts.onBossDefeated(paid, region.id)
            paid = withContracts
            msg?.let { notes += it }
        }
        return ok(paid, notes.joinToString(" "))
    }

    fun craftRing(s: GameState): StepResult {
        if ((s.inventory[RING] ?: 0) > 0) return fail(s, "Sekta ma już pierścień przestrzenny.")
        if (s.spiritStones < RING_STONES) return fail(s, "Pierścień kosztuje $RING_STONES kamieni.")
        val have = s.inventory[RING_MINERAL] ?: 0
        if (have < RING_MINERAL_COUNT) return fail(s, "Pierścień wymaga ${RING_MINERAL_COUNT}× $RING_MINERAL (masz $have).")
        return ok(
            s.copy(spiritStones = s.spiritStones - RING_STONES, inventory = s.inventory + (RING_MINERAL to have - RING_MINERAL_COUNT) + (RING to 1)),
            "Wykuto Pierścień Przestrzenny – wyprawy uniosą $RING_SLOTS rodzajów łupów."
        )
    }
}

/** Etap 5a: Wieża Prób – piętra coraz silniejszych strażników, 3 próby na dzień gry (licznik od dnia gry, nie od zegara). */
object Tower {
    const val ATTEMPTS_PER_DAY = 3
    const val GROWTH = 1.08

    data class Guardian(val hp: Int, val attack: Int, val defense: Int)

    fun guardian(floor: Int): Guardian {
        val f = GROWTH.pow(floor - 1)
        return Guardian((400 * f).toInt(), (45 * f).toInt(), (18 * f).toInt())
    }

    fun attemptsLeft(s: GameState): Int = if (s.towerDay != s.currentDay) ATTEMPTS_PER_DAY else (ATTEMPTS_PER_DAY - s.towerAttempts).coerceAtLeast(0)

    fun climb(s: GameState, discipleId: String, ctx: EngineContext): StepResult {
        val d = s.disciple(discipleId) ?: return ok(s)
        if (attemptsLeft(s) <= 0) return fail(s, "Wieża Prób jest zamknięta do jutra (3 próby dziennie).")
        val floor = s.towerFloor + 1
        val g = guardian(floor)
        val res = Skirmish.resolve(d.attack, d.defense, d.maxHp, g.hp, g.attack, g.defense, ctx.rng("tower"))
        val used = (if (s.towerDay == s.currentDay) s.towerAttempts else 0) + 1
        val base = s.copy(towerDay = s.currentDay, towerAttempts = used)
        if (!res.win) return ok(base, "Wieża Prób: strażnik piętra $floor okazał się silniejszy.")
        val reward = 50 * floor
        val bonus = if (floor % 10 == 0) GameContent.BREAKTHROUGH_PILL else null
        val inv = bonus?.let { base.inventory + (it to (base.inventory[it] ?: 0) + 1) } ?: base.inventory
        return ok(
            base.copy(towerFloor = floor, spiritStones = base.spiritStones + reward, inventory = inv),
            "Wieża Prób: ${d.name} zdobywa piętro $floor! +$reward kamieni" + (bonus?.let { ", nagroda: $it" } ?: "") + "."
        )
    }
}
