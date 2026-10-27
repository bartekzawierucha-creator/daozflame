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

/** Etap 5b: zapowiedziany najazd rywala. */
data class Raid(
    val sectId: String = "",
    val sectName: String = "",
    val arriveDay: Int = 0,
    val strength: Int = 0
)

enum class SpyType(val polishName: String, val description: String) {
    SCOUT("Zwiad", "Poznaj siłę i zamiary sekty."),
    STEAL("Kradzież", "Wynieś kamienie ze skarbca rywala."),
    SABOTAGE("Sabotaż", "Osłab sektę o 10%."),
    RUMOR("Plotki", "Skłóć sektę z sąsiadami na 30 dni.")
}

data class SpyMission(
    val id: String = "",
    val discipleId: String = "",
    val sectId: String = "",
    val type: SpyType = SpyType.SCOUT,
    val returnDay: Int = 0
)

/** Etap 5c: katastrofy – rzadkie i zawsze zapowiadane. */
enum class CatastropheType(val polishName: String, val description: String) {
    FLOOD("Wielka Powódź", "Zioła drożeją o 30%, pola rodzą o połowę mniej."),
    DROUGHT("Susza", "Pola rodzą o połowę mniej, zioła drożeją o 30%."),
    QI_STORM("Burza Qi", "Qi całej sekty −20%."),
    BEAST_TIDE("Fala Bestii", "Strażnicy krain biją o 30% mocniej."),
    EARTHQUAKE("Trzęsienie Ziemi", "Naprawy kosztują 5% skarbca (najwyżej 2000 kamieni).")
}

data class Catastrophe(
    val type: CatastropheType = CatastropheType.FLOOD,
    val announcedDay: Int = 0,
    val startDay: Int = 0,
    val endDay: Int = 0
)

/** Etap 7: wpis w Sali Przodków. */
data class AncestorRecord(
    val name: String = "",
    val realm: String = "",
    val majorOrdinal: Int = 0,
    val diedDay: Int = 0,
    val cause: String = ""
)

/** Para towarzyszy Dao – po roku rodzi się dziecko-kandydat. */
data class Companionship(val a: String = "", val b: String = "", val sinceDay: Int = 0, val children: Int = 0)

/** Jajo bestii wysiadywane w sekcie. */
data class BeastEgg(val id: String = "", val hatchDay: Int = 0, val template: String = "")

/** Sztuki Dao – codzienne zajęcia uczniów. */
enum class DaoArt(val polishName: String, val description: String) {
    GUQIN("Guqin", "Muzyka koi sektę: +1 morale dziennie (do 3 grających)."),
    CALLIGRAPHY("Kaligrafia", "Pędzel ćwiczy umysł: +1 zrozumienia co 30 dni."),
    PAINTING("Malarstwo", "Obraz świata: +20% szansy olśnienia."),
    GO("Go", "Plansza uczy przewidywania: +3 pkt szansy przełomu.")
}
