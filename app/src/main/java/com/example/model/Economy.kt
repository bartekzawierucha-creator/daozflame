package com.example.model

/** Etap 4: pory roku – rok gry ma 120 dni (4 × 30). */
enum class Season(val polishName: String, val glyph: String) {
    SPRING("Wiosna", "春"),
    SUMMER("Lato", "夏"),
    AUTUMN("Jesień", "秋"),
    WINTER("Zima", "冬")
}

/** Jakość wyrobu: mnożnik efektu pigułki, toksyczności i statystyk artefaktu. */
enum class ItemQuality(
    val polishName: String,
    val effect: Double,
    val toxicity: Double,
    val artifactStats: Double,
    /** Przyrostek w nazwie przedmiotu w magazynie (Zwykła = bez przyrostka). */
    val suffix: String
) {
    CRACKED("Popękana", 0.6, 1.5, 0.7, " · Popękana"),
    NORMAL("Zwykła", 1.0, 1.0, 1.0, ""),
    EXCELLENT("Doskonała", 1.4, 0.7, 1.25, " · Doskonała"),
    LEGENDARY("Legendarna", 2.0, 0.3, 1.6, " · Legendarna"),
    HEAVENLY("Niebiańska", 3.0, 0.0, 2.2, " · Niebiańska")
}

/** Poletko duchowych ziół. */
data class FieldPlot(
    val herb: String? = null,
    val plantedDay: Int = 0,
    val readyDay: Int = 0
)

/** Karawana handlowa w drodze. */
data class Caravan(
    val id: String = "",
    val item: String = "",
    val quantity: Int = 0,
    val returnDay: Int = 0,
    /** Uczeń-eskorta (zmniejsza ryzyko napadu). */
    val guardId: String? = null,
    /** Cena za sztukę ustalona przy wyjeździe. */
    val unitPrice: Int = 0
)

enum class ContractType(val polishName: String) {
    DELIVER("Dostawa"),
    HUNT("Łowy")
}

/** Zlecenie z tablicy kontraktów. */
data class Contract(
    val id: String = "",
    val client: String = "",
    val type: ContractType = ContractType.DELIVER,
    val item: String = "",
    val quantity: Int = 0,
    val regionId: String? = null,
    val rewardStones: Int = 0,
    val rewardReputation: Int = 0,
    val expiresDay: Int = 0,
    val done: Boolean = false
)
