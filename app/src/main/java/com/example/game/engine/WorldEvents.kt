package com.example.game.engine

import com.example.model.Catastrophe
import com.example.model.CatastropheType
import com.example.model.FacilityType
import com.example.model.MajorRealm
import com.example.model.ScheduledEvent

/** Etap 5c: systemy dobowe świata w stałej kolejności (po fazach cyklu, raz na dzień gry). */
object WorldDaily {
    fun run(start: GameState, ctx: EngineContext): Pair<GameState, List<String>> {
        val notes = mutableListOf<String>()
        var s = start
        WorldAI.daily(s, ctx).let { (st, n) -> s = st; notes += n }
        Empire.daily(s, ctx).let { (st, n) -> s = st; notes += n }
        Catastrophes.daily(s, ctx).let { (st, n) -> s = st; notes += n }
        SecretRealms.daily(s).let { (st, n) -> s = st; notes += n }
        // Etap 7: fabuła i cykl życia.
        Story.daily(s).let { (st, n) -> s = st; notes += n }
        Lifecycle.daily(s, ctx).let { (st, n) -> s = st; notes += n }
        if (s.e7Initialized) {
            Creatures.daily(s).let { (st, n) -> s = st; notes += n }
            Creatures.awakenFlameSpirits(s).let { (st, n) -> s = st; notes += n }
            DailyLife.daily(s, ctx).let { (st, n) -> s = st; notes += n }
        }
        // Etap 6: tajemnice.
        s = Breath.daily(s)
        s = SecretStates.daily(s, ctx)
        SecretTools.daily(s, ctx).let { (st, n) -> s = st; notes += n }
        return s to notes
    }
}

/** Imperium: podatek co 30 dni, rozkazy co 45 dni, wojna domowa po pierwszym roku. Treść zdarzeń – [com.example.game.content.NpcCatalog]. */
object Empire {
    const val FAVOR = "imperial_favor"
    const val FALLEN = "empire_fallen"
    const val CIVIL_WAR = "civil_war"
    const val TAX_EVERY = 30
    const val ORDER_EVERY = 45
    const val CIVIL_WAR_FROM_DAY = 360
    val ORDERS = listOf("emp_order_bandits", "emp_order_herbs", "emp_order_escort")

    fun favor(s: GameState): Int = s.flags[FAVOR] ?: 0

    private fun schedule(s: GameState, id: String): GameState =
        if (s.scheduledEvents.any { it.eventId == id } || s.activeEventId == id) s
        else s.copy(scheduledEvents = s.scheduledEvents + ScheduledEvent(s.currentDay, id))

    fun daily(s0: GameState, ctx: EngineContext): Pair<GameState, List<String>> {
        if (!s0.e5Initialized || (s0.flags[FALLEN] ?: 0) > 0) return s0 to emptyList()
        var s = s0
        val notes = mutableListOf<String>()
        if (s.currentDay % TAX_EVERY == 0) s = schedule(s, "emp_tax")
        if (s.currentDay % ORDER_EVERY == 15) s = schedule(s, ORDERS[(s.currentDay / ORDER_EVERY) % ORDERS.size])
        val unrest = favor(s) <= -60 || (s.currentDay >= CIVIL_WAR_FROM_DAY && s.currentDay % TAX_EVERY == 10 && ctx.rng("civil-war").nextInt(100) < 10)
        if ((s.flags[CIVIL_WAR] ?: 0) == 0 && s.currentDay >= 60 && unrest) {
            s = schedule(s.copy(flags = s.flags + (CIVIL_WAR to 1)), "emp_civil_1")
            notes += "👑 W stolicy wrze. Wojna domowa w Imperium wisi w powietrzu."
        }
        return s to notes
    }
}

/** Mgła wojny: krainy odkrywa się zwiadem, po kolei od najbliższych. */
object Fog {
    const val START_DISCOVERED = 3

    fun discovered(s: GameState, regionId: String): Boolean = regionId in s.discoveredRegionIds

    /** Migracja: kraina, w której gracz już był (zbadana > 0%), jest odkryta. */
    fun normalize(s: GameState): GameState {
        if (s.discoveredRegionIds.isNotEmpty() || !com.example.game.FeatureFlags.LIVING_WORLD) return s
        val base = s.regions.take(START_DISCOVERED).map { it.id } + s.regions.filter { it.explorationProgress > 0 }.map { it.id }
        return s.copy(discoveredRegionIds = base.toSet())
    }

    fun scoutCost(index: Int): Int = 100 * (index + 1)

    fun scout(s: GameState, ctx: EngineContext, regionId: String, discipleId: String): StepResult {
        val index = s.regions.indexOfFirst { it.id == regionId }
        if (index < 0) return ok(s)
        val region = s.regions[index]
        if (discovered(s, regionId)) return fail(s, "${region.name} jest już na mapie.")
        if (index > 0 && !discovered(s, s.regions[index - 1].id)) return fail(s, "Najpierw odkryj krainę bliżej sekty.")
        val d = s.disciple(discipleId) ?: return ok(s)
        val cost = scoutCost(index)
        if (s.spiritStones < cost) return fail(s, "Zwiad kosztuje $cost kamieni.")
        val paid = s.copy(spiritStones = s.spiritStones - cost)
        val need = region.dangerLevel * 60
        if (d.combatPower < need && ctx.rng("scout").nextInt(100) >= 50) {
            return ok(paid.copy(disciples = paid.disciples.map { if (it.id == d.id) it.copy(currentHp = (it.maxHp / 3).coerceAtLeast(10)) else it }),
                "${d.name} wraca ranny – mgła nad krainą wciąż wisi. (Bezpieczny zwiad: moc ≥ $need.)")
        }
        return ok(paid.copy(discoveredRegionIds = paid.discoveredRegionIds + regionId), "🗺 ${d.name} odkrywa: ${region.name}!")
    }
}

/** Katastrofy: rzadkie (20% co 30 dni), zapowiadane 3 dni wcześniej – 10 dni z Salą Ekspedycji na poziomie 3 (obserwatorium). */
object Catastrophes {
    const val CHANCE = 20
    const val DURATION = 10
    const val EARTHQUAKE_CAP = 2_000

    fun active(s: GameState): CatastropheType? = s.catastrophe?.takeIf { s.currentDay in it.startDay..it.endDay }?.type

    fun warningDays(s: GameState): Int = if (Vein.facilityLevel(s, FacilityType.MISSION_HALL) >= 3) 10 else 3

    fun herbPriceFactor(s: GameState): Double = if (active(s) == CatastropheType.FLOOD || active(s) == CatastropheType.DROUGHT) 1.3 else 1.0
    fun fieldFactor(s: GameState): Double = if (active(s) == CatastropheType.FLOOD || active(s) == CatastropheType.DROUGHT) 0.5 else 1.0
    fun beastFactor(s: GameState): Double = if (active(s) == CatastropheType.BEAST_TIDE) 1.3 else 1.0
    fun qiPenaltyPercent(s: GameState): Double = if (active(s) == CatastropheType.QI_STORM) -20.0 else 0.0

    fun daily(s0: GameState, ctx: EngineContext): Pair<GameState, List<String>> {
        if (!s0.e5Initialized) return s0 to emptyList()
        var s = s0
        val notes = mutableListOf<String>()
        val c = s.catastrophe
        when {
            c != null && s.currentDay > c.endDay -> {
                s = WorldAI.note(s.copy(catastrophe = null), "${c.type.polishName} mija.")
                notes += "🌤 ${c.type.polishName} minęła."
            }
            c != null && s.currentDay == c.startDay -> {
                notes += "🌩 ${c.type.polishName}: ${c.type.description}"
                if (c.type == CatastropheType.EARTHQUAKE) {
                    // Bez nagłej utraty dorobku: 5% skarbca, najwyżej 2000; z długim ostrzeżeniem połowa.
                    val warned = c.startDay - c.announcedDay >= 10
                    val loss = (s.spiritStones / 20).coerceAtMost(EARTHQUAKE_CAP).let { if (warned) it / 2 else it }
                    s = s.copy(spiritStones = s.spiritStones - loss)
                    notes += "Naprawy po trzęsieniu: −$loss kamieni."
                }
            }
            c == null && s.currentDay % 30 == 20 && s.currentDay >= 30 -> {
                val rng = ctx.rng("catastrophe")
                if (rng.nextInt(100) < CHANCE) {
                    val type = CatastropheType.entries.random(rng)
                    val start = s.currentDay + warningDays(s)
                    s = s.copy(catastrophe = Catastrophe(type, s.currentDay, start, start + DURATION - 1))
                    s = WorldAI.note(s, "Obserwatorium zapowiada: ${type.polishName}.")
                    notes += "🔭 Obserwatorium ostrzega: ${type.polishName} za ${warningDays(s)} dni. ${type.description}"
                }
            }
        }
        return s to notes
    }
}

/** Tajne królestwa: 6 miejsc, każde otwiera się co 40 dni na 5 dni; wejść mogą tylko uczniowie do określonego królestwa. */
object SecretRealms {
    const val EVERY = 40
    const val OPEN_DAYS = 5

    data class Realm(
        val id: String,
        val name: String,
        val offset: Int,
        val maxRealm: MajorRealm,
        val guardian: Int,
        val loot: List<Pair<String, Int>>,
        val description: String
    )

    val REALMS: List<Realm> = listOf(
        Realm("sr_bamboo", "Ogród Tysiąca Bambusów", 5, MajorRealm.QI_CONDENSATION, 300,
            listOf("Trawa Wiecznej Wiosny" to 10, "Kwiat Niebiańskiego Zrozumienia" to 3), "Ogród pustelnika, do którego nie wpuszcza nikogo starszego niż Kondensacja Qi."),
        Realm("sr_tomb", "Grobowiec Generała Miecza", 12, MajorRealm.FOUNDATION, 900,
            listOf("Gwiezdne Żelazo" to 8, GameContent.BREAKTHROUGH_PILL to 2), "Krypta pełna mieczy; bariera łamie każdego powyżej Fundamentów."),
        Realm("sr_lotus", "Staw Lodowego Lotosu", 19, MajorRealm.FOUNDATION, 1_200,
            listOf("Śnieżny Lotos Oświecenia" to 8, GameContent.FLAME_CRYSTAL to 3), "Staw zamarza na kamień, gdy wchodzi zbyt silna dusza."),
        Realm("sr_forge", "Kuźnia Upadłego Boga", 26, MajorRealm.CORE_FORMATION, 3_000,
            listOf("Starożytna Ruda Pustki" to 6, GameContent.FLAME_CRYSTAL to 5), "Ruiny boskiej kuźni dla adeptów do Złotego Rdzenia."),
        Realm("sr_library", "Zatopiona Biblioteka", 33, MajorRealm.CORE_FORMATION, 3_500,
            listOf("Kwiat Niebiańskiego Zrozumienia" to 6, "Duchowy Nefryt" to 15, SecretTools.MIRROR to 1), "Półki pod wodą; czasem leży tam zwój zapomnianej metody."),
        Realm("sr_palace", "Pałac Gwiezdnego Cesarza", 38, MajorRealm.NASCENT_SOUL, 9_000,
            listOf("Rdzeń Gwiazdy" to 2, GameContent.BREAKTHROUGH_PILL to 4), "Pałac między gwiazdami, otwarty dla Narodzonych Dusz i młodszych.")
    )

    fun openingDay(r: Realm, day: Int): Int? {
        if (day < r.offset) return null
        val start = day - (day - r.offset) % EVERY
        return start.takeIf { day - it < OPEN_DAYS }
    }

    fun open(s: GameState): List<Realm> = REALMS.filter { openingDay(it, s.currentDay) != null }

    fun daily(s: GameState): Pair<GameState, List<String>> {
        if (!s.e5Initialized) return s to emptyList()
        val opened = REALMS.filter { openingDay(it, s.currentDay) == s.currentDay }
        return s to opened.map { "🌀 Otwiera się tajne królestwo: ${it.name} (5 dni)." }
    }

    fun enter(s: GameState, ctx: EngineContext, realmId: String, discipleId: String): StepResult {
        val r = REALMS.find { it.id == realmId } ?: return ok(s)
        val opening = openingDay(r, s.currentDay) ?: return fail(s, "${r.name} jest zamknięte.")
        if (s.realmVisits[r.id] == opening) return fail(s, "Sekta była już w ${r.name} w tym otwarciu.")
        val d = s.disciple(discipleId) ?: return ok(s)
        val realm = d.majorRealm ?: MajorRealm.QI_CONDENSATION
        if (realm.ordinal > r.maxRealm.ordinal) return fail(s, "Bariera ${r.name} odrzuca kultywatorów powyżej: ${r.maxRealm.polishName}.")
        val rng = ctx.rng("secret-realm:${r.id}")
        val res = Skirmish.resolve(d.attack, d.defense, d.currentHp, r.guardian, r.guardian / 12, r.guardian / 30, rng)
        val visited = s.copy(realmVisits = s.realmVisits + (r.id to opening))
        if (!res.win) {
            return ok(visited.copy(disciples = visited.disciples.map { if (it.id == d.id) it.copy(currentHp = res.hpLeft.coerceAtLeast(10)) else it }),
                "${d.name} nie sprostał strażnikom ${r.name} i wraca ranny.")
        }
        val inv = visited.inventory.toMutableMap()
        r.loot.forEach { (item, n) -> inv[item] = (inv[item] ?: 0) + n }
        var extra = ""
        if (r.id == "sr_library" && visited.e3Initialized && rng.nextInt(100) < 40) {
            val m = Realms.methodOf(com.example.model.CultivationPath.entries.random(rng), com.example.model.MethodGrade.MYSTIC)
            inv[m.scrollItem] = (inv[m.scrollItem] ?: 0) + 1
            extra = " Znaleziono: ${m.scrollItem}!"
        }
        return ok(
            visited.copy(inventory = inv.toMap(), disciples = visited.disciples.map { if (it.id == d.id) it.copy(currentHp = res.hpLeft.coerceAtLeast(10)) else it }),
            "🌀 ${d.name} wraca z ${r.name}: " + r.loot.joinToString { "${it.second}× ${it.first}" } + "." + extra
        )
    }
}
