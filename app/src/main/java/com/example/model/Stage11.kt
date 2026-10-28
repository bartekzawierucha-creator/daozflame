package com.example.model

/*
 * Etap 11: ekwipunek, cel tygodnia, narzędzia mentora, więzi uczniów, okazje, zarządca.
 * Każde pole ma wartość domyślną – stare zapisy wczytują się bez migracji.
 */

/** Miejsca ekwipunku ucznia (lalka postaci: lewa kolumna broń–dłonie–pierścień, prawa głowa–szata–relikwia). */
enum class EquipSlot(val polishName: String, val glyph: String) {
    WEAPON("Broń", "⚔"),
    HEAD("Głowa", "👑"),
    ARMOR("Szata", "🛡"),
    HANDS("Dłonie", "🧤"),
    RING("Pierścień", "💍"),
    RELIC("Relikwia", "🔔")
}

/** Obietnica mistrza: nagroda za osiągnięcie wskazanego wielkiego królestwa przed terminem. */
data class MasterPromise(
    /** Przedmiot z magazynu (klucz inventory) albo null, gdy nagrodą jest artefakt. */
    val item: String? = null,
    /** Artefakt: id + nazwa (do odnalezienia w skarbcu w chwili spełnienia). */
    val artifactId: String? = null,
    val artifactName: String? = null,
    /** Ordinal [MajorRealm], który trzeba osiągnąć. */
    val targetMajor: Int = 0,
    val madeDay: Int = 0,
    val untilDay: Int = 0
) {
    val rewardName: String get() = artifactName ?: item ?: "nagroda"
}

/** Więź między dwoma uczniami: −100 (wrogowie) … +100 (bracia przysięgli). */
data class Bond(val a: String = "", val b: String = "", val value: Int = 0, val sinceDay: Int = 0) {
    val kind: BondKind get() = BondKind.of(value)
    fun has(id: String) = a == id || b == id
    fun other(id: String) = if (a == id) b else a
}

enum class BondKind(val polishName: String, val glyph: String) {
    SWORN("Bracia przysięgli", "🩸"),
    FRIEND("Przyjaciele", "🤝"),
    NEUTRAL("Znajomi", "·"),
    RIVAL("Rywale", "⚡"),
    ENEMY("Wrogowie", "☠");

    companion object {
        fun of(v: Int) = when {
            v >= 75 -> SWORN
            v >= 40 -> FRIEND
            v <= -75 -> ENEMY
            v <= -40 -> RIVAL
            else -> NEUTRAL
        }
    }
}

/** Rodzaj celu tygodnia sekty. */
enum class GoalKind { BREAKTHROUGHS, WINS, TOWER, CONTRACTS, REPUTATION, STONES, TOURNAMENT }

/** Cel tygodnia: jedno wyzwanie z terminem, nagrodą i ceną porażki. */
data class SectGoal(
    val kind: GoalKind = GoalKind.BREAKTHROUGHS,
    val title: String = "",
    val description: String = "",
    val target: Long = 1,
    /** Wartość licznika w chwili ogłoszenia (postęp = licznik − base). */
    val base: Long = 0,
    val startDay: Int = 0,
    val deadline: Int = 0,
    val rewardStones: Int = 0,
    val rewardReputation: Int = 0,
    val rewardItem: String? = null
)

/** Rodzaje rzadkich okazji – ograniczone w czasie, z wyborem. */
enum class OpportunityKind(val polishName: String, val glyph: String) {
    WANDERING_MASTER("Wędrowny mistrz", "🧙"),
    RARE_MERCHANT("Kupiec rarytasów", "🐫"),
    IMPERIAL_EXAM("Egzamin cesarski", "📜"),
    HIDDEN_CAVE("Ukryta grota", "🕳"),
    REFUGEES("Uchodźcy", "🏕")
}

data class Opportunity(
    val kind: OpportunityKind = OpportunityKind.WANDERING_MASTER,
    val untilDay: Int = 0,
    val cost: Int = 0,
    /** Dla kupca: oferowany artefakt. */
    val artifact: Artifact? = null
)

/** Zarządca skarbca: uczeń prowadzi handel według polityki mistrza. */
data class StewardPolicy(
    val stewardId: String? = null,
    /** Sprzedawaj nadwyżki zasobów powyżej tej liczby sztuk (0 = nie sprzedawaj). */
    val keepAbove: Int = 0,
    val autoContracts: Boolean = false,
    val autoFood: Boolean = false
)

/** Etap 13: oferta Domu Aukcyjnego – artefakt, zwój metody albo rzadka pigułka za kamienie. */
data class AuctionLot(
    val name: String = "",
    val price: Int = 0,
    val artifact: Artifact? = null,
    /** Przedmiot do magazynu (gdy [artifact] == null). */
    val item: String? = null,
    val sold: Boolean = false
)
