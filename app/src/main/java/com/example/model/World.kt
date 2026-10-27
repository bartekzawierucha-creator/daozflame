package com.example.model

/** Etap 5a: rodzaje węzłów na mapie wyprawy. */
enum class ExpNodeType(val polishName: String, val glyph: String) {
    FIGHT("Starcie", "⚔"),
    HERB("Zioła", "🌿"),
    TREASURE("Skrytka", "💎"),
    SHRINE("Kapliczka", "⛩"),
    TRAP("Pułapka", "⚠"),
    REST("Odpoczynek", "🔥"),
    FLAME_SOURCE("Źródło Płomienia", "🜂"),
    BOSS("Legowisko bestii", "🐉")
}

data class ExpNode(
    val type: ExpNodeType = ExpNodeType.FIGHT,
    /** Siła względna węzła (starcia, pułapki). */
    val power: Int = 0,
    /** Dla [ExpNodeType.FLAME_SOURCE]: który płomień. */
    val flameId: String? = null
)

/** Wyprawa w toku: kolejne warstwy węzłów, z każdej gracz wybiera jeden; na końcu legowisko bossa. */
data class ExpeditionRun(
    val regionId: String = "",
    val discipleId: String = "",
    val layers: List<List<ExpNode>> = emptyList(),
    /** Indeks bieżącej warstwy; == layers.size oznacza legowisko bossa. */
    val layer: Int = 0,
    val hp: Int = 0,
    val loot: Map<String, Int> = emptyMap(),
    val stones: Int = 0,
    val log: List<String> = emptyList(),
    val auto: Boolean = false,
    /** Walka z bossem trwa (wynik rozliczy [com.example.game.engine.CombatSystem]). */
    val atBoss: Boolean = false
)
