package com.example.model

/** Żywioły korzeni duchowych. Pięć podstawowych + trzy niebiańskie + Chaos. */
enum class SpiritElement(val polishName: String, val glyph: String) {
    METAL("Metal", "金"),
    WOOD("Drewno", "木"),
    WATER("Woda", "水"),
    FIRE("Ogień", "火"),
    EARTH("Ziemia", "土"),
    THUNDER("Grom", "雷"),
    ICE("Lód", "冰"),
    WIND("Wiatr", "风"),
    CHAOS("Chaos", "混");

    companion object {
        val BASIC = listOf(METAL, WOOD, WATER, FIRE, EARTH)
        val HEAVENLY = listOf(THUNDER, ICE, WIND)
    }
}

/** Ranga korzenia: szansa przy rekrutacji (‰) i mnożnik tempa kultywacji. */
enum class RootGrade(val polishName: String, val permille: Int, val qiMultiplier: Double) {
    MIXED("Mieszany", 550, 0.6),
    TRIPLE("Potrójny", 250, 0.9),
    DUAL("Podwójny", 130, 1.2),
    SINGLE("Pojedynczy", 60, 1.6),
    HEAVENLY("Niebiański", 9, 2.0),
    CHAOS("Chaosu", 1, 2.5)
}

data class SpiritRoot(
    val grade: RootGrade = RootGrade.MIXED,
    val elements: List<SpiritElement> = emptyList()
) {
    val label: String get() = "${grade.polishName} (${elements.joinToString("/") { it.polishName }})"
}

/** Ranga w hierarchii sekty. [maxTechniqueTier] = najwyższy stopień technik, których może się uczyć. */
enum class SectRank(
    val polishName: String,
    val requiredRealm: CultivationRealm,
    val salaryPerDay: Int,
    val maxTechniqueTier: Int
) {
    OUTER("Uczeń Zewnętrzny", CultivationRealm.QI_CONDENSATION_1, 5, 1),
    INNER("Uczeń Wewnętrzny", CultivationRealm.FOUNDATION_EARLY, 20, 2),
    DIRECT("Uczeń Bezpośredni", CultivationRealm.CORE_FORMATION, 60, 3),
    ELDER("Starszy", CultivationRealm.NASCENT_SOUL, 150, 4),
    GRAND_ELDER("Wielki Starszy", CultivationRealm.MAHAYANA_GREAT_VEHICLE, 400, 5)
}

/** Cecha charakteru ucznia. Efekty liczone w silniku (DiscipleRules). */
data class PersonalityTrait(
    val id: String,
    val name: String,
    val positive: Boolean,
    val description: String
)
