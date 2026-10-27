package com.example.game.engine

import com.example.game.FeatureFlags
import com.example.model.FacilityType
import com.example.model.SectDecree

/**
 * Edykty sekty: stała premia za stały koszt. Aktywne maks. 2 (3 od poziomu 5 Sali Dyplomacji),
 * zmiana edyktu ma 3 dni odnowienia, utrzymanie płatne o świcie.
 * Skala: doba ≈ 26 cykli, podstawa dochodu ≈ 52 kamienie na ucznia dziennie.
 *
 * Gdy [FeatureFlags.EDICTS_V2] jest wyłączone, działa tylko dawny Edykt Złotego Smoka (+35% Qi).
 */
object EdictCatalog {

    data class Edict(
        val id: String,
        val title: String,
        val icon: String,
        val description: String,
        val bonusDesc: String,
        val costDesc: String,
        /** Kamienie pobierane codziennie o świcie. */
        val upkeepStones: Int,
        val effects: List<Modifier>
    )

    const val BASE_SLOTS = 2
    const val COOLDOWN_DAYS = 3

    private fun m(title: String, stat: Stat, op: ModOp, v: Double) = Modifier("Edykt: $title", stat, op, v)

    private fun edict(
        id: String, title: String, icon: String, desc: String, bonus: String, cost: String, upkeep: Int,
        vararg effects: Triple<Stat, ModOp, Double>
    ) = Edict(id, title, icon, desc, bonus, cost, upkeep, effects.map { m(title, it.first, it.second, it.third) })

    private infix fun Stat.pct(v: Double) = Triple(this, ModOp.PERCENT, v)
    private infix fun Stat.flat(v: Double) = Triple(this, ModOp.FLAT, v)

    val all: List<Edict> = listOf(
        edict("dec_1", "Edykt Złotego Smoka", "🐉", "Podnosi absorpcję Qi dla wszystkich uczniów.",
            "+35% Qi", "40 kamieni dziennie", 40, Stat.QI_GAIN pct 35.0),
        edict("dec_2", "Dekret Niebiańskiego Zaciągu", "🏮", "Na egzamin wstępny przybywa więcej kandydatów.",
            "+2 kandydatów", "60 kamieni dziennie", 60, Stat.RECRUIT_POOL flat 2.0),
        edict("dec_3", "Tarcza Ochronna Patriarchy", "🛡️", "Patriarcha osłania przełomy uczniów własnym Qi.",
            "+25 pkt szansy przełomu", "−10% Qi", 0, Stat.BREAKTHROUGH_CHANCE flat 25.0, Stat.QI_GAIN pct -10.0),
        edict("dec_4", "Imperialny Pakt Handlowy", "💎", "Szlaki ziół i minerałów są chronione przez Imperium.",
            "+40% zbiorów, +10% kamieni", "−10% reputacji", 0, Stat.GATHER_YIELD pct 40.0, Stat.STONE_INCOME pct 10.0, Stat.REPUTATION_GAIN pct -10.0),
        edict("ed_discipline", "Prawo Żelaznej Dyscypliny", "⛓️", "Twarda dyscyplina medytacji od świtu do nocy.",
            "+15% Qi", "−10% dochodu kamieni", 0, Stat.QI_GAIN pct 15.0, Stat.STONE_INCOME pct -10.0),
        edict("ed_cauldron", "Ogień Kotła", "⚗️", "Sala Alchemii pracuje bez przerwy.",
            "−25% kosztu alchemii", "−10% zbiorów", 0, Stat.ALCHEMY_COST pct -25.0, Stat.GATHER_YIELD pct -10.0),
        edict("ed_iron", "Kując Żelazo", "🔨", "Kowale dostają pierwszeństwo przy rudach.",
            "−15% kosztu kuźni", "−5% Qi", 0, Stat.FORGE_COST pct -15.0, Stat.QI_GAIN pct -5.0),
        edict("ed_warrior", "Droga Wojownika", "⚔️", "Sekta szkoli się do walki ponad wszystko.",
            "+15% obrażeń", "+10% kosztu alchemii", 0, Stat.COMBAT_DAMAGE pct 15.0, Stat.ALCHEMY_COST pct 10.0),
        edict("ed_trade", "Handel z Imperium", "🪙", "Kupcy Imperium mają wstęp na górę.",
            "+25% dochodu kamieni", "−15% reputacji", 0, Stat.STONE_INCOME pct 25.0, Stat.REPUTATION_GAIN pct -15.0),
        edict("ed_silence", "Cisza Medytacji", "🕯️", "Na górze obowiązuje milczenie – umysły są czyste.",
            "+50% szansy olśnienia", "−15% dochodu kamieni", 0, Stat.EPIPHANY_CHANCE pct 50.0, Stat.STONE_INCOME pct -15.0)
    )

    private val byId = all.associateBy { it.id }
    fun get(id: String): Edict? = byId[id]

    fun slots(s: GameState): Int {
        val diplomacy = s.facilities.find { it.type == FacilityType.DIPLOMACY_HALL }?.level ?: 1
        return BASE_SLOTS + (if (diplomacy >= 5) 1 else 0) + if (s.e4Initialized) Vein.extraEdictSlots(s.veinLevel) else 0
    }

    fun activeIds(s: GameState): List<String> = s.sectLeader.activeDecrees.filter { it.isActive }.map { it.id }

    fun activeModifiers(s: GameState): List<Modifier> {
        val active = activeIds(s)
        if (!FeatureFlags.EDICTS_V2) {
            return if ("dec_1" in active) listOf(Modifier("Edykt: Edykt Złotego Smoka", Stat.QI_GAIN, ModOp.PERCENT, 35.0)) else emptyList()
        }
        return active.mapNotNull { get(it) }.flatMap { it.effects }
    }

    fun dailyUpkeep(s: GameState): Int =
        if (!FeatureFlags.EDICTS_V2) 0 else activeIds(s).mapNotNull { get(it) }.sumOf { it.upkeepStones }

    /** Lista edyktów do pokazania w UI – zachowuje stan aktywności z zapisu i dopisuje nowe z katalogu. */
    fun asDecrees(current: List<SectDecree>): List<SectDecree> {
        val activeIds = current.filter { it.isActive }.map { it.id }.toSet()
        if (!FeatureFlags.EDICTS_V2) return current
        return all.map { e ->
            SectDecree(
                id = e.id, name = e.title, title = e.title, description = e.description, icon = e.icon,
                bonusDesc = e.bonusDesc, effectDescription = "${e.bonusDesc} · koszt: ${e.costDesc}",
                isActive = e.id in activeIds
            )
        }
    }
}
