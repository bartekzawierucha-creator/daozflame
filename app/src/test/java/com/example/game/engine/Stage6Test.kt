package com.example.game.engine

import com.example.model.KnowledgeLevel
import com.example.model.MajorRealm
import com.example.model.SecretState
import com.example.model.TimeOfDay
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Etap 6: ukryta głębia – reguły świata, Dziennik i tajemnice. */
class Stage6Test {

    private fun fresh(seed: Long = 1L) = GameEngine.newGame(seed)
    private fun apply(s: GameState, c: GameCommand) = GameEngine.apply(s, c)

    // --- Krok 1: reguły świata ---

    @Test
    fun rulesAreSolvableForThousandSeeds() {
        (1L..1000L).forEach { seed ->
            val errors = RulesGen.validate(RulesGen.generate(seed))
            assertTrue("ziarno $seed: ${errors.take(3)}", errors.isEmpty())
        }
    }

    @Test
    fun aboutThreeHundredSecretsDifferentPerWorld() {
        val a = RulesGen.generate(1L)
        val count = Secrets.allIds(a).size
        assertTrue("tajemnic: $count", count in 260..340)
        val b = RulesGen.generate(2L)
        assertTrue(a.breath != b.breath)
        assertTrue(a.constellations.map { it.points } != b.constellations.map { it.points })
    }

    @Test
    fun rulesAreGeneratedOnceAndNeverRegeneratedOnLoad() {
        val s = fresh(3L)
        assertNotNull(s.rules)
        val tampered = s.copy(rules = s.rules!!.copy(blockages = setOf(0)))
        assertEquals(setOf(0), GameEngine.normalize(tampered).rules!!.blockages)
        val old = s.copy(rules = null)
        assertEquals(FengShui.extend(RulesGen.generate(3L), 3L), GameEngine.normalize(old).rules)
    }

    @Test
    fun breathOptimumNeverConflictsWithMethodElement() {
        val r = RulesGen.generate(4L)
        r.breath.forEach { (m, b) ->
            val el = r.methodElements.getValue(m)
            val place = RulesGen.PLACE_ELEMENTS[b.place]
            assertTrue(!Elements.overcomes(place, el) && !Elements.overcomes(el, place))
        }
    }

    // --- Krok 2: Dziennik ---

    @Test
    fun journalShowsNumbersOnlyWhenCertain() {
        var s = fresh(5L)
        val id = "breath:x"
        assertEquals("?", Journal.show(s, id, "+35%"))
        repeat(3) { s = Journal.observe(s, id) }
        assertEquals(KnowledgeLevel.LIKELY, Journal.level(s, id))
        assertEquals("?", Journal.show(s, id, "+35%"))
        repeat(2) { s = Journal.observe(s, id) }
        assertEquals("+35%", Journal.show(s, id, "+35%"))
    }

    @Test
    fun dreamsNeverGiveCertainty() {
        var s = fresh(6L)
        repeat(10) { s = Journal.observe(s, "x", cap = KnowledgeLevel.LIKELY) }
        assertEquals(KnowledgeLevel.LIKELY, Journal.level(s, "x"))
    }

    @Test
    fun unknownSecretModifierIsLabelledVaguely() {
        val s = fresh(7L)
        val r = s.rules!!
        val (method, opt) = r.breath.entries.first()
        val d = s.disciples.first().copy(methodId = method, breath = opt.breath, meditationPlace = opt.place)
        val st = s.copy(timeOfDay = opt.time, disciples = listOf(d) + s.disciples.drop(1))
        val mods = Modifiers.disciple(Modifiers.sect(st), d).breakdown(Stat.QI_GAIN)
        assertTrue(mods.toString(), mods.any { it.startsWith("Nieznany wpływ: ×1.35") })
        val known = Journal.reveal(st, Secrets.breath(method))
        assertTrue(Modifiers.disciple(Modifiers.sect(known), d).breakdown(Stat.QI_GAIN).any { it.startsWith("Oddech i miejsce") })
    }

    // --- Meridiany ---

    @Test
    fun meridiansOpenFromCenterAndConstellationsLightUp() {
        val s = fresh(8L).copy(spiritStones = 1_000_000)
        val d = s.disciples.first().copy(majorRealm = MajorRealm.MAHAYANA)
        var st = s.copy(disciples = listOf(d) + s.disciples.drop(1))
        assertTrue(apply(st, GameCommand.OpenMeridian(d.id, 0)).notice!!.contains("sąsiadującego"))
        val target = st.rules!!.constellations.minBy { RulesGen.distanceFromCenter(it.points.toSet()) }
        // Ścieżka z centrum do gwiazdozbioru, potem jego punkty.
        val path = bfsPath(target.points.toSet())
        path.forEach { p -> st = apply(st, GameCommand.OpenMeridian(d.id, p)).state }
        repeat(target.points.size) {
            val opened = st.disciple(d.id)!!.openedMeridians
            target.points.filter { it !in opened && RulesGen.neighbours(it).any { n -> n in opened } }
                .forEach { p -> st = apply(st, GameCommand.OpenMeridian(d.id, p)).state }
        }
        assertTrue(st.disciple(d.id)!!.openedMeridians.containsAll(target.points))
        assertTrue(Journal.certain(st, Secrets.constellation(target.id)))
        assertTrue(Modifiers.disciple(Modifiers.sect(st), st.disciple(d.id)!!).breakdown(Stat.valueOf(target.stat)).any { it.contains(target.name) })
    }

    private fun bfsPath(target: Set<Int>): List<Int> {
        val prev = mutableMapOf<Int, Int?>()
        val queue = ArrayDeque(RulesGen.CENTER.toList())
        RulesGen.CENTER.forEach { prev[it] = null }
        var end: Int? = null
        while (queue.isNotEmpty()) {
            val p = queue.removeFirst()
            if (p in target) { end = p; break }
            RulesGen.neighbours(p).filter { it !in prev }.forEach { prev[it] = p; queue.add(it) }
        }
        val out = mutableListOf<Int>()
        var c = end
        while (c != null) { out.add(0, c); c = prev[c] }
        return out
    }

    @Test
    fun meridianCapDependsOnRealm() {
        val s = fresh(9L).copy(spiritStones = 1_000_000)
        val d = s.disciples.first().copy(majorRealm = MajorRealm.BODY_TEMPERING, openedMeridians = (0 until 6).map { 14 + it % 2 + (it / 2) * 6 }.toSet())
        val st = s.copy(disciples = listOf(d) + s.disciples.drop(1))
        val free = RulesGen.neighbours(14).first { it !in d.openedMeridians }
        assertTrue(apply(st, GameCommand.OpenMeridian(d.id, free)).notice!!.contains("udźwignie"))
    }

    // --- Oddech i zwoje ---

    @Test
    fun fiveFragmentsDecipherTheOptimum() {
        val s = fresh(10L)
        val method = s.rules!!.breath.keys.first()
        val item = Breath.fragmentItem(method)
        assertTrue(apply(s.copy(inventory = s.inventory + (item to 4)), GameCommand.DecipherScroll(method)).notice!!.contains("5"))
        val r = apply(s.copy(inventory = s.inventory + (item to 5)), GameCommand.DecipherScroll(method)).state
        assertTrue(Journal.certain(r, Secrets.breath(method)))
        assertEquals(0, r.inventory[item])
    }

    // --- Kontemplacja ---

    @Test
    fun contemplationHasDiminishingReturnsAndRevealsDerivedLaws() {
        val s = fresh(11L)
        val r = s.rules!!
        val (obj, res) = r.lawObjects.entries.first { it.key in RulesGen.PLACES + listOf("Spadająca gwiazda", "Stara sosna", "Ognisko", "Wschód słońca") || Contemplation.available(s, it.key) }
        val d = s.disciples.first().copy(laws = mapOf(res.law to 10.0))
        var st = s.copy(disciples = listOf(d) + s.disciples.drop(1))
        st = apply(st, GameCommand.Contemplate(d.id, obj)).state
        val first = st.disciple(d.id)!!.laws.getValue(res.law) - 10.0
        val before = st.disciple(d.id)!!.laws.getValue(res.law)
        st = apply(st, GameCommand.Contemplate(d.id, obj)).state
        val second = st.disciple(d.id)!!.laws.getValue(res.law) - before
        assertEquals(first * Contemplation.REPEAT_DECAY, second, 1e-9)

        val dl = r.derivedLaws.first()
        val ready = s.disciples.first().copy(laws = mapOf(dl.first to 60.0, dl.second to 49.9))
        val obj2 = r.lawObjects.entries.firstOrNull { it.value.law == dl.second && Contemplation.available(s, it.key) }
        if (obj2 != null) {
            var st2 = s.copy(disciples = listOf(ready.copy(laws = mapOf(dl.first to 60.0, dl.second to 49.99))) + s.disciples.drop(1))
            repeat(5) { st2 = apply(st2, GameCommand.Contemplate(ready.id, obj2.key)).state }
            assertTrue(dl.name in st2.disciple(ready.id)!!.laws)
            assertTrue(Journal.certain(st2, Secrets.derived(dl.name)))
        }
    }

    // --- Alchemia jako chemia ---

    @Test
    fun strongHerbMixGivesCappedEssence() {
        val s = fresh(12L).copy(spiritStones = 10_000)
        val r = s.rules!!
        val (herb, attrs) = r.herbAttributes.entries.first { e -> e.value.values.max() >= 4 }
        assertNotNull(Chemistry.dominant(r, herb, herb))
        val known = s.copy(knownRecipeIds = com.example.game.engine.GameContent.alchemyRecipes.map { it.id }.toSet(), inventory = s.inventory + (herb to 5))
        val after = apply(known, GameCommand.Experiment(herb, herb)).state
        val essence = after.inventory.keys.firstOrNull { it.startsWith(Chemistry.ESSENCE) }
        assertNotNull(essence)
        assertTrue(after.journal.keys.any { it.startsWith("herb:$herb:") })
        val d = after.disciples.first()
        val used = apply(after, GameCommand.UsePill(d.id, essence!!)).state.disciple(d.id)!!
        assertTrue(used.attack - d.attack <= 2 && used.defense - d.defense <= 2)
    }

    // --- Formy i cykle ---

    @Test
    fun formTriggersOnSequenceAndIsRevealed() {
        val s = fresh(13L).let { it.copy(inventory = it.inventory + ("Talizman Ognistej Kuli" to 5)) }
        val form = s.rules!!.forms.first { !it.sequence.contains('U') }
        var st = apply(s, GameCommand.StartExpedition(s.regions.first().id, s.disciples.first().id)).state
        st = st.copy(combat = st.combat!!.copy(enemyHp = 1_000_000, enemyMaxHp = 1_000_000))
        form.sequence.forEach { m ->
            st = apply(st, when (m) {
                'M' -> GameCommand.CombatManualAttack
                'P' -> GameCommand.CombatUsePill
                else -> GameCommand.CombatTalisman("Talizman Ognistej Kuli")
            }).state
        }
        assertTrue(form.id in st.combat!!.formsUsed)
        assertTrue(Journal.certain(st, Secrets.form(form.id)))
    }

    @Test
    fun hiddenCyclesActFromTheStartAndAreObserved() {
        val s = fresh(14L)
        val lava = s.regions.first { it.biome == com.example.model.BiomeType.LAVA_CHASM }
        // Drewno rodzi Ogień → bestia z lawy rośnie w siłę (Nadmiar).
        val wood = s.copy(timeOfDay = TimeOfDay.DUSK, discoveredRegionIds = s.discoveredRegionIds + lava.id, disciples = s.disciples.map {
            it.copy(spiritRoot = it.spiritRoot!!.copy(elements = listOf(com.example.model.SpiritElement.WOOD)))
        })
        val r = apply(wood, GameCommand.StartExpedition(lava.id, wood.disciples.first().id)).state
        assertTrue(r.combat!!.enemyAttackFactor > 1.0)
        assertEquals(1, r.journal[Secrets.CYCLE_EXCESS]!!.observations)
        assertTrue(r.combat!!.logs.none { it.text.contains("Cykl Nadmiaru") })
    }

    // --- Ukryte stany ---

    @Test
    fun halfStepAfterThreeFailuresAndAlwaysEnds() {
        var d = fresh(15L).disciples.first()
        repeat(3) { d = SecretStates.afterFailure(d, 10) }
        assertEquals(SecretState.HALF_STEP, d.secretState)
        assertEquals(SecretState.HALF_STEP, d.secretState)
        val s = fresh(15L).let { it.copy(currentDay = 10 + SecretStates.HALF_STEP_DAYS, disciples = listOf(d) + it.disciples.drop(1)) }
        assertNull(SecretStates.daily(s, EngineContext(1, 1)).disciple(d.id)!!.secretState)
        assertNull(SecretStates.afterSuccess(d, pill = false, today = 12).secretState)
    }

    @Test
    fun falseImmortalClearsWhenToxinsAreGone() {
        val d0 = fresh(16L).disciples.first().copy(toxicity = 70)
        val d = SecretStates.afterSuccess(d0, pill = true, today = 5)
        assertEquals(SecretState.FALSE_IMMORTAL, d.secretState)
        val s = fresh(16L).let { it.copy(currentDay = 6, disciples = listOf(d.copy(toxicity = 0)) + it.disciples.drop(1)) }
        assertNull(SecretStates.daily(s, EngineContext(1, 1)).disciple(d.id)!!.secretState)
    }

    // --- Narzędzia ---

    @Test
    fun mirrorShowsClosestConstellation() {
        val s = fresh(17L).let { it.copy(inventory = it.inventory + (SecretTools.MIRROR to 1)) }
        val r = apply(s, GameCommand.UseMirror(s.disciples.first().id))
        assertTrue(r.notice!!.contains("Lustrze"))
        assertTrue(r.state.journal.keys.any { it.startsWith("const:") })
        assertEquals(KnowledgeLevel.LIKELY, r.state.journal.values.first().level)
    }

    @Test
    fun researcherAndDreamsFillTheJournalOverTime() {
        var s = fresh(18L)
        s = apply(s, GameCommand.SetResearcher(s.disciples.maxBy { it.comprehension }.id)).state
        repeat(200) { day ->
            s = s.copy(currentDay = s.currentDay + 1, step = s.step + 1)
            s = SecretTools.daily(s, EngineContext(s.worldSeed, s.step)).first
        }
        assertTrue(s.journal.size > 10)
    }

    // --- Zasada 70/30 ---

    @Test
    fun secretsGiveRoughlyThirtyPercentPower() {
        val s = fresh(19L)
        val r = s.rules!!
        val (method, opt) = r.breath.entries.first()
        val plain = s.disciples.first().copy(methodId = method)
        val base = Modifiers.disciple(Modifiers.sect(s.copy(timeOfDay = opt.time)), plain).factor(Stat.QI_GAIN)
        val qiStars = r.constellations.filter { it.stat == "QI_GAIN" }.sortedBy { RulesGen.distanceFromCenter(it.points.toSet()) }.take(2)
        val master = plain.copy(breath = opt.breath, meditationPlace = opt.place, openedMeridians = qiStars.flatMap { it.points }.toSet())
        val boosted = Modifiers.disciple(Modifiers.sect(s.copy(timeOfDay = opt.time)), master).factor(Stat.QI_GAIN)
        val ratio = base / boosted
        assertTrue("bez odkryć $ratio mocy", ratio in 0.6..0.8)
    }
}
