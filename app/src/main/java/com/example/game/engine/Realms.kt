package com.example.game.engine

import com.example.model.*
import kotlin.math.pow

/** Etap 3: drabina królestw, metody kultywacji, mapowanie na dawny `CultivationRealm`. */
object Realms {

    const val STAGE_GROWTH = 1.6
    const val MINOR_STAT_GROWTH = 1.06
    const val MAJOR_STAT_GROWTH = 1.25

    /** Qi potrzebne do ukończenia etapu. */
    fun required(major: MajorRealm, stage: Int): Double = major.baseQi * STAGE_GROWTH.pow(stage - 1)

    fun required(d: Disciple): Double = required(d.majorRealm ?: MajorRealm.BODY_TEMPERING, d.stage)

    /** Dawny enum dla UI i systemów Etapu 2 (rangi). Monotoniczny względem nowej drabiny. */
    fun toLegacy(major: MajorRealm, stage: Int): CultivationRealm = when (major) {
        MajorRealm.BODY_TEMPERING -> CultivationRealm.QI_CONDENSATION_1
        MajorRealm.QI_CONDENSATION -> when {
            stage >= 9 -> CultivationRealm.QI_CONDENSATION_9
            stage >= 5 -> CultivationRealm.QI_CONDENSATION_5
            else -> CultivationRealm.QI_CONDENSATION_1
        }
        MajorRealm.FOUNDATION -> if (stage >= 3) CultivationRealm.FOUNDATION_LATE else CultivationRealm.FOUNDATION_EARLY
        MajorRealm.CORE_FORMATION -> CultivationRealm.CORE_FORMATION
        MajorRealm.NASCENT_SOUL -> CultivationRealm.NASCENT_SOUL
        MajorRealm.SOUL_TRANSFORMATION -> CultivationRealm.SOUL_TRANSFORMATION
        MajorRealm.VOID_TRIBULATION -> CultivationRealm.VOID_TRIBULATION
        MajorRealm.MAHAYANA -> CultivationRealm.MAHAYANA_GREAT_VEHICLE
        MajorRealm.HEAVENLY_IMMORTAL -> CultivationRealm.HEAVENLY_IMMORTAL
        MajorRealm.CHAOS_GOLDEN_IMMORTAL -> CultivationRealm.GOLDEN_IMMORTAL_OF_CHAOS
    }

    fun fromLegacy(realm: CultivationRealm): Pair<MajorRealm, Int> = when (realm) {
        CultivationRealm.QI_CONDENSATION_1 -> MajorRealm.QI_CONDENSATION to 1
        CultivationRealm.QI_CONDENSATION_5 -> MajorRealm.QI_CONDENSATION to 5
        CultivationRealm.QI_CONDENSATION_9 -> MajorRealm.QI_CONDENSATION to 9
        CultivationRealm.FOUNDATION_EARLY -> MajorRealm.FOUNDATION to 1
        CultivationRealm.FOUNDATION_LATE -> MajorRealm.FOUNDATION to 3
        CultivationRealm.CORE_FORMATION -> MajorRealm.CORE_FORMATION to 1
        CultivationRealm.NASCENT_SOUL -> MajorRealm.NASCENT_SOUL to 1
        CultivationRealm.SOUL_TRANSFORMATION -> MajorRealm.SOUL_TRANSFORMATION to 1
        CultivationRealm.VOID_TRIBULATION -> MajorRealm.VOID_TRIBULATION to 1
        CultivationRealm.MAHAYANA_GREAT_VEHICLE -> MajorRealm.MAHAYANA to 1
        CultivationRealm.HEAVENLY_IMMORTAL -> MajorRealm.HEAVENLY_IMMORTAL to 1
        CultivationRealm.GOLDEN_IMMORTAL_OF_CHAOS -> MajorRealm.CHAOS_GOLDEN_IMMORTAL to 1
    }

    /** Ustawia pola zgodności (realm, realmProgress) z pól Etapu 3. */
    fun sync(d: Disciple): Disciple {
        val major = d.majorRealm ?: return d
        val pct = (d.stageQi / required(major, d.stage) * 100).toInt().coerceIn(0, 100)
        return d.copy(realm = toLegacy(major, d.stage), realmProgress = pct)
    }

    fun label(d: Disciple): String {
        val major = d.majorRealm ?: return d.realm.polishName
        val st = major.stageName(d.stage)
        return if (st.isEmpty()) major.polishName else "${major.polishName} · $st"
    }

    fun atPeak(d: Disciple): Boolean = d.majorRealm != null && d.stage >= d.majorRealm.stages

    // --- Metody kultywacji ---

    data class Method(val id: String, val name: String, val path: CultivationPath?, val grade: MethodGrade) {
        val scrollItem: String get() = "Zwój: $name"
    }

    const val BASIC_METHOD = "m_basic"

    private val NAMES: Map<CultivationPath, List<String>> = mapOf(
        CultivationPath.ORTHODOX to listOf("Sutra Spokojnego Oddechu", "Kanon Dziewięciu Obiegów", "Sutra Północnej Gwiazdy", "Kanon Niebiańskiego Źródła", "Sutra Pierwszego Oddechu Wszechświata"),
        CultivationPath.BODY to listOf("Trening Żelaznej Skóry", "Kanon Brązowych Kości", "Sutra Złotego Ciała Wadżry", "Kanon Nieśmiertelnego Tytana", "Ciało Pierwotnego Chaosu"),
        CultivationPath.SWORD to listOf("Oddech Ostrza", "Kanon Siedmiu Kling", "Sutra Serca Miecza", "Kanon Dziesięciu Tysięcy Mieczy", "Pierwszy Miecz Stworzenia"),
        CultivationPath.PILLS to listOf("Receptura Ciepłego Kotła", "Kanon Dziewięciu Pieców", "Sutra Płomiennego Dantian", "Kanon Złotego Eliksiru", "Pierwotny Kocioł Nieba i Ziemi"),
        CultivationPath.DEMONIC to listOf("Oddech Krwawej Mgły", "Kanon Krwawego Morza", "Sutra Pożeracza Dusz", "Kanon Demonicznego Cesarza", "Pierwotna Otchłań"),
        CultivationPath.HEART to listOf("Sutra Czystego Serca", "Kanon Lotosowego Umysłu", "Sutra Oświecenia Bodhi", "Kanon Niebiańskiego Spokoju", "Serce Pierwotnego Dao")
    )

    val methods: List<Method> = listOf(Method(BASIC_METHOD, "Podstawowa Technika Oddechu", null, MethodGrade.YELLOW)) +
        NAMES.flatMap { (path, names) ->
            MethodGrade.entries.mapIndexed { i, g -> Method("m_${path.name.lowercase()}_${g.name.lowercase()}", names[i], path, g) }
        }

    private val methodById = methods.associateBy { it.id }
    private val methodByScroll = methods.associateBy { it.scrollItem }

    fun method(id: String?): Method = methodById[id] ?: methodById.getValue(BASIC_METHOD)
    fun methodForScroll(item: String): Method? = methodByScroll[item]
    fun methodOf(path: CultivationPath, grade: MethodGrade): Method = methods.first { it.path == path && it.grade == grade }

    /** Najwyższe królestwo dostępne z metodą ucznia. */
    fun ceiling(d: Disciple): MajorRealm = method(d.methodId).grade.ceiling

    // --- Kształty duszy ---

    val SOUL_SHAPES: List<Pair<String, Modifier>> = listOf(
        "Smok" to Modifier("Dusza Smoka", Stat.COMBAT_DAMAGE, ModOp.PERCENT, 15.0),
        "Feniks" to Modifier("Dusza Feniksa", Stat.QI_GAIN, ModOp.PERCENT, 10.0),
        "Tygrys" to Modifier("Dusza Tygrysa", Stat.COMBAT_DAMAGE, ModOp.PERCENT, 12.0),
        "Żółw" to Modifier("Dusza Żółwia", Stat.BREAKTHROUGH_CHANCE, ModOp.FLAT, 10.0),
        "Lotos" to Modifier("Dusza Lotosu", Stat.EPIPHANY_CHANCE, ModOp.PERCENT, 50.0),
        "Miecz" to Modifier("Dusza Miecza", Stat.COMBAT_DAMAGE, ModOp.PERCENT, 18.0),
        "Lis" to Modifier("Dusza Lisa", Stat.QI_GAIN, ModOp.PERCENT, 8.0)
    )

    // --- Katalizatory przebudzenia krwi ---

    fun bloodlineCatalyst(b: BloodlineType): String = when (b) {
        BloodlineType.GOLDEN_DRAGON -> "Boska Smocza Łuska"
        BloodlineType.ANCIENT_PHOENIX -> "Pióro Nieśmiertelnego Ognia"
        BloodlineType.MOON_SHADOW_WOLF -> "Grzyb Księżycowej Rosy"
        BloodlineType.TITANIC_ASURA -> "Kość Smoka Pradawnego"
        BloodlineType.PURPLE_LIGHTNING_QILIN -> "Kryształ Gromu"
        BloodlineType.IMMORTAL_LOTUS_HEART -> "Śnieżny Lotos Oświecenia"
        BloodlineType.PRIMORDIAL_CHAOS -> "Jadeit Chaosu"
        BloodlineType.HEAVENLY_NETHER_VIPER -> "Kieł Trującego Monstrum"
        BloodlineType.VOID_DEVOURER -> "Szkło Pustki"
        else -> "Kwiat Niebiańskiego Zrozumienia"
    }

    /** Królestwo potrzebne do kolejnego stopnia przebudzenia krwi (1–3). */
    fun awakeningRealm(level: Int): MajorRealm? = when (level) {
        1 -> MajorRealm.FOUNDATION
        2 -> MajorRealm.NASCENT_SOUL
        3 -> MajorRealm.MAHAYANA
        else -> null
    }

    // --- Migracja do Etapu 3 ---

    fun upgrade(d: Disciple): Disciple {
        if (d.majorRealm != null) return sync(d)
        val (major, stage) = fromLegacy(d.realm)
        val qi = d.realmProgress.coerceIn(0, 99) / 100.0 * required(major, stage)
        return sync(
            d.copy(
                majorRealm = major,
                stage = stage,
                stageQi = qi,
                foundationQuality = if (major.ordinal >= MajorRealm.FOUNDATION.ordinal) FoundationQuality.NORMAL else null,
                coreGrade = if (major.ordinal >= MajorRealm.CORE_FORMATION.ordinal) 3 else 0,
                methodId = d.methodId ?: BASIC_METHOD
            )
        )
    }

    fun normalize(s: GameState): GameState {
        if (s.e3Initialized || !com.example.game.FeatureFlags.CULTIVATION_V2) return s
        // Stare zapisy mogą mieć uczniów ponad sufitem podstawowej metody – dostają metodę sięgającą ich królestwa.
        fun withMethod(d: Disciple): Disciple {
            val u = upgrade(d)
            if (u.majorRealm!!.ordinal <= method(u.methodId).grade.ceiling.ordinal) return u
            val grade = MethodGrade.entries.first { it.ceiling.ordinal >= u.majorRealm.ordinal }
            return u.copy(path = u.path ?: CultivationPath.ORTHODOX, methodId = methodOf(u.path ?: CultivationPath.ORTHODOX, grade).id)
        }
        return s.copy(
            disciples = s.disciples.map(::withMethod),
            availableRecruits = s.availableRecruits.map(::withMethod),
            e3Initialized = true
        )
    }
}
