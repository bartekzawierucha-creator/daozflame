package com.example.game.engine

import com.example.model.*
import kotlin.random.Random

/** Etap 2: korzenie duchowe, cechy charakteru, rangi i ich efekty. */
object DiscipleRules {

    const val FOOD = "Racje Ryżu Duchowego"
    const val STARTING_FOOD = 160
    const val FOOD_PRICE = 4
    const val EXAM_EVERY_DAYS = 30
    const val EXAM_OPEN_DAYS = 5
    const val FREE_ADMISSIONS = 2
    const val ADMISSION_STEP_COST = 300
    const val ROOT_TEST_COST = 50
    const val TOURNAMENT_EVERY_DAYS = 60
    const val FEAST_COOLDOWN_DAYS = 10
    const val DIRECT_LIMIT = 3
    const val GRAND_ELDER_LIMIT = 1
    const val HUNGER_MORALE = 10
    const val BASE_CAPACITY = 10
    const val CAPACITY_PER_PAVILION_LEVEL = 5

    /** Pola ryżu duchowego dają codziennie połowę zapotrzebowania sekty. */
    fun dailyFoodProduction(disciples: Int): Int = (disciples + 1) / 2

    /** Ilu uczniów pomieści sekta (Pawilon Kultywacji). */
    fun capacity(s: GameState): Int =
        BASE_CAPACITY + CAPACITY_PER_PAVILION_LEVEL * (s.facilities.find { it.type == FacilityType.CULTIVATION_PAVILION }?.level ?: 1)

    // --- Cechy charakteru ---

    val TRAITS: List<PersonalityTrait> = listOf(
        PersonalityTrait("diligent", "Pracowity", true, "+15% Qi z medytacji."),
        PersonalityTrait("loyal", "Lojalny", true, "Lojalność nigdy nie spada poniżej 60."),
        PersonalityTrait("charismatic", "Charyzmatyczny", true, "+1 morale sekty dziennie."),
        PersonalityTrait("fearless", "Nieustraszony", true, "+10% obrażeń w walce."),
        PersonalityTrait("insightful", "Wnikliwy", true, "+50% szansy na olśnienie."),
        PersonalityTrait("humble", "Skromny", true, "Pobiera połowę pensji."),
        PersonalityTrait("gatherer", "Urodzony Zbieracz", true, "+50% szansy na udany zbiór."),
        PersonalityTrait("alchemist", "Geniusz Alchemii", true, "−10% kosztu alchemii sekty (łącznie maks. −30%)."),
        PersonalityTrait("lazy", "Leniwy", false, "−15% Qi z medytacji."),
        PersonalityTrait("ambitious", "Ambitny", false, "Żąda awansu – bez niego traci lojalność."),
        PersonalityTrait("jealous", "Zazdrosny", false, "Traci lojalność, gdy awansuje ktoś inny."),
        PersonalityTrait("greedy", "Chciwy", false, "Przy niskiej lojalności kradnie z magazynu."),
        PersonalityTrait("coward", "Tchórz", false, "−15% obrażeń w walce."),
        PersonalityTrait("hotheaded", "Porywczy", false, "+10% obrażeń, ale lojalność powoli spada."),
        PersonalityTrait("traitor", "Zdrajca", false, "Ukryta. Przy niskiej lojalności sprzedaje sekrety rywalom."),
        PersonalityTrait("blood_eyes", "Krwawe Oczy", false, "Mutacja z odchylenia Qi: +15% obrażeń, ale demon serca.")
    )
    private val traitById = TRAITS.associateBy { it.id }
    fun trait(id: String): PersonalityTrait? = traitById[id]
    fun Disciple.has(traitId: String): Boolean = traitId in traitIds || traitId in hiddenTraitIds

    fun rollTraits(rng: Random): Pair<List<String>, List<String>> {
        val positive = TRAITS.filter { it.positive }
        val negative = TRAITS.filter { !it.positive && it.id != "traitor" && it.id != "blood_eyes" }
        val first = positive.random(rng).id
        val traitor = rng.nextInt(100) < 5
        val second = when {
            traitor -> "traitor"
            rng.nextBoolean() -> positive.filter { it.id != first }.random(rng).id
            else -> negative.random(rng).id
        }
        // Druga cecha jest ukryta, dopóki ktoś jej nie zbada (Test Korzenia, zdarzenie).
        return listOf(first) to listOf(second)
    }

    // --- Korzenie duchowe ---

    fun rollRoot(rng: Random): SpiritRoot {
        var roll = rng.nextInt(1000)
        val grade = RootGrade.entries.first { g -> roll -= g.permille; roll < 0 }
        val elements = when (grade) {
            RootGrade.MIXED -> SpiritElement.BASIC.shuffled(rng).take(4 + rng.nextInt(2))
            RootGrade.TRIPLE -> SpiritElement.BASIC.shuffled(rng).take(3)
            RootGrade.DUAL -> SpiritElement.BASIC.shuffled(rng).take(2)
            RootGrade.SINGLE -> listOf(SpiritElement.BASIC.random(rng))
            RootGrade.HEAVENLY -> listOf(SpiritElement.HEAVENLY.random(rng))
            RootGrade.CHAOS -> listOf(SpiritElement.CHAOS)
        }
        return SpiritRoot(grade, elements.sortedBy { it.ordinal })
    }

    val SCHOOL_ELEMENTS: Map<MartialSchool, Set<SpiritElement>> = mapOf(
        MartialSchool.SWORD_IMMORTAL to setOf(SpiritElement.METAL, SpiritElement.WIND),
        MartialSchool.BODY_ASURA to setOf(SpiritElement.EARTH, SpiritElement.THUNDER),
        MartialSchool.ELEMENTAL_TAOIST to setOf(SpiritElement.FIRE, SpiritElement.WATER, SpiritElement.WOOD, SpiritElement.THUNDER, SpiritElement.ICE),
        MartialSchool.SHADOW_VIPER to setOf(SpiritElement.WATER, SpiritElement.WIND, SpiritElement.ICE)
    )

    /** Czy korzeń ucznia pasuje do jego szkoły walki. Chaos pasuje do wszystkiego. */
    fun hasAffinity(d: Disciple): Boolean {
        val root = d.spiritRoot ?: return true
        if (SpiritElement.CHAOS in root.elements) return true
        return root.elements.any { it in (SCHOOL_ELEMENTS[d.martialSchool] ?: emptySet()) }
    }

    /** Koszt nauki techniki w punktach Dao: +30% (w górę) bez powinowactwa korzenia. */
    fun skillCost(d: Disciple, base: Int): Int = if (hasAffinity(d)) base else kotlin.math.ceil(base * 1.3).toInt()

    // --- Rangi ---

    fun naturalRank(realm: CultivationRealm): SectRank =
        SectRank.entries.last { realm.ordinal >= it.requiredRealm.ordinal }

    fun rankLimit(s: GameState, rank: SectRank): Int = when (rank) {
        SectRank.OUTER, SectRank.INNER -> Int.MAX_VALUE
        SectRank.DIRECT -> DIRECT_LIMIT
        SectRank.ELDER -> s.facilities.find { it.type == FacilityType.CULTIVATION_PAVILION }?.level ?: 1
        SectRank.GRAND_ELDER -> GRAND_ELDER_LIMIT
    }

    fun nextRank(d: Disciple): SectRank? = SectRank.entries.getOrNull(d.rank.ordinal + 1)

    /** Czy uczeń może teraz dostać awans (wymagane królestwo i wolne miejsce). */
    fun canPromote(s: GameState, d: Disciple): Boolean {
        val next = nextRank(d) ?: return false
        if (d.realm.ordinal < next.requiredRealm.ordinal) return false
        return s.disciples.count { it.rank == next } < rankLimit(s, next)
    }

    fun salary(d: Disciple): Int = if (d.has("humble")) d.rank.salaryPerDay / 2 else d.rank.salaryPerDay

    // --- Modyfikatory z rang, cech i korzeni ---

    /** Premie dla całej sekty: Starsi, Wielki Starszy, morale, głód, Geniusze Alchemii. */
    fun sectModifiers(s: GameState): List<Modifier> = buildList {
        if (!s.e2Initialized) return@buildList
        val elders = s.disciples.count { it.rank == SectRank.ELDER }
        if (elders > 0) add(Modifier("Starsi sekty ($elders)", Stat.QI_GAIN, ModOp.PERCENT, 5.0 * elders))
        if (s.disciples.any { it.rank == SectRank.GRAND_ELDER }) {
            add(Modifier("Wielki Starszy", Stat.QI_GAIN, ModOp.PERCENT, 10.0))
            add(Modifier("Wielki Starszy", Stat.STONE_INCOME, ModOp.PERCENT, 10.0))
        }
        val moraleEffect = (s.morale - 50) * 0.4
        if (moraleEffect != 0.0) {
            add(Modifier("Morale sekty (${s.morale})", Stat.QI_GAIN, ModOp.PERCENT, moraleEffect))
            add(Modifier("Morale sekty (${s.morale})", Stat.STONE_INCOME, ModOp.PERCENT, moraleEffect))
        }
        if (s.hungryOnDay == s.currentDay) add(Modifier("Głód w sekcie", Stat.QI_GAIN, ModOp.PERCENT, -20.0))
        val alchemists = s.disciples.count { "alchemist" in it.traitIds }.coerceAtMost(3)
        if (alchemists > 0) add(Modifier("Geniusze Alchemii ($alchemists)", Stat.ALCHEMY_COST, ModOp.PERCENT, -10.0 * alchemists))
    }

    /** Premie konkretnego ucznia: korzeń, ranga, cechy. */
    fun discipleModifiers(d: Disciple): List<Modifier> = buildList {
        d.spiritRoot?.let { add(Modifier("Korzeń: ${it.grade.polishName}", Stat.QI_GAIN, ModOp.MULTIPLY, it.grade.qiMultiplier)) }
        if (d.rank == SectRank.DIRECT) add(Modifier("Uczeń Bezpośredni", Stat.QI_GAIN, ModOp.PERCENT, 20.0))
        if (d.has("diligent")) add(Modifier("Pracowity", Stat.QI_GAIN, ModOp.PERCENT, 15.0))
        if (d.has("lazy")) add(Modifier("Leniwy", Stat.QI_GAIN, ModOp.PERCENT, -15.0))
        if (d.has("insightful")) add(Modifier("Wnikliwy", Stat.EPIPHANY_CHANCE, ModOp.PERCENT, 50.0))
        if (d.has("gatherer")) add(Modifier("Urodzony Zbieracz", Stat.GATHER_YIELD, ModOp.PERCENT, 50.0))
        if (d.has("fearless")) add(Modifier("Nieustraszony", Stat.COMBAT_DAMAGE, ModOp.PERCENT, 10.0))
        if (d.has("hotheaded")) add(Modifier("Porywczy", Stat.COMBAT_DAMAGE, ModOp.PERCENT, 10.0))
        if (d.has("coward")) add(Modifier("Tchórz", Stat.COMBAT_DAMAGE, ModOp.PERCENT, -15.0))
        if (d.has("blood_eyes")) add(Modifier("Krwawe Oczy", Stat.COMBAT_DAMAGE, ModOp.PERCENT, 15.0))
        val root = d.spiritRoot
        if (root != null && root.grade.ordinal >= RootGrade.SINGLE.ordinal && hasAffinity(d)) {
            add(Modifier("Korzeń zgodny ze szkołą", Stat.COMBAT_DAMAGE, ModOp.PERCENT, 20.0))
        }
    }

    // --- Nowy uczeń i migracja ---

    /** Uzupełnia ucznia o korzeń, cechy, rangę i wiek (nowi kandydaci i stare zapisy). */
    fun enrich(d: Disciple, rng: Random, candidate: Boolean): Disciple {
        if (d.spiritRoot != null) return d
        val (visible, hidden) = rollTraits(rng)
        return d.copy(
            spiritRoot = rollRoot(rng),
            traitIds = visible,
            hiddenTraitIds = hidden,
            rank = if (candidate) SectRank.OUTER else naturalRank(d.realm).coerceAtMost(SectRank.ELDER),
            loyalty = if (candidate) 60 else 70,
            ageYears = 12 + rng.nextInt(14)
        )
    }

    private fun SectRank.coerceAtMost(max: SectRank) = if (ordinal > max.ordinal) max else this

    /**
     * Jednorazowa migracja do Etapu 2: deterministyczna (ziarno świata + id ucznia), więc ponowne
     * wczytanie tego samego zapisu daje te same korzenie i cechy. Pilnuje też limitów rang.
     */
    fun normalize(s: GameState): GameState {
        if (s.e2Initialized || !com.example.game.FeatureFlags.DISCIPLES_V2) return s
        fun rngFor(id: String) = GameRng(GameRng.mix(s.worldSeed xor id.hashCode().toLong() * 31))
        var directs = 0
        var elders = 0
        val elderLimit = rankLimit(s, SectRank.ELDER)
        val members = s.disciples.sortedByDescending { it.realm.ordinal }.map { d ->
            val e = enrich(d, rngFor(d.id), candidate = false)
            // Ujawniamy cechy obecnych członków (gracz zna swoich ludzi), poza Zdrajcą.
            val revealed = e.copy(
                traitIds = e.traitIds + e.hiddenTraitIds.filter { it != "traitor" },
                hiddenTraitIds = e.hiddenTraitIds.filter { it == "traitor" }
            )
            val rank = when (revealed.rank) {
                SectRank.ELDER -> if (elders < elderLimit) { elders++; SectRank.ELDER } else demote(SectRank.ELDER, directs).also { if (it == SectRank.DIRECT) directs++ }
                SectRank.DIRECT -> if (directs < DIRECT_LIMIT) { directs++; SectRank.DIRECT } else SectRank.INNER
                else -> revealed.rank
            }
            revealed.copy(rank = rank)
        }.associateBy { it.id }
        return s.copy(
            disciples = s.disciples.map { members.getValue(it.id) },
            availableRecruits = s.availableRecruits.map { enrich(it, rngFor(it.id), candidate = true) },
            inventory = if (s.inventory.containsKey(FOOD)) s.inventory else s.inventory + (FOOD to STARTING_FOOD),
            examOpenUntilDay = if (s.availableRecruits.isNotEmpty()) s.currentDay + EXAM_OPEN_DAYS else s.examOpenUntilDay,
            e2Initialized = true
        )
    }

    private fun demote(from: SectRank, directs: Int): SectRank =
        if (from == SectRank.ELDER && directs < DIRECT_LIMIT) SectRank.DIRECT else SectRank.INNER
}
