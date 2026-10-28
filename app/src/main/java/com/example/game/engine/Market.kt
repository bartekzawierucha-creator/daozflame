package com.example.game.engine

import com.example.game.content.ResourceCatalog
import com.example.model.Caravan
import com.example.model.Contract
import com.example.model.ContractType
import com.example.model.MethodGrade
import com.example.model.TimeOfDay
import kotlin.math.roundToInt
import kotlin.random.Random

/**
 * Etap 4: targ z żywymi cenami, karawany, czarny rynek i tablica kontraktów.
 * Ochrona przed nieskończonym zarobkiem: kupno 1,5× drożej niż sprzedaż, każda sprzedana
 * sztuka obniża cenę o 2% (min. 0,6), ceny wracają do normy o 5% dziennie.
 */
object Market {

    /** Nasycenie rynku: zrzucanie towaru może zbić cenę do 10% (wcześniej 60% – źródło nieskończonych kamieni). */
    const val MIN_MULT = 0.1
    const val MAX_MULT = 1.4
    const val SELL_PRESSURE = 0.02
    // Etap 13: dzień gry ~48 s – ceny wracają wolniej (połowa w ~55 min), targ nie jest bezdenny.
    const val RECOVERY = 0.01
    const val BUY_MARKUP = 1.5
    const val CARAVAN_ROBBERY = 10
    const val CARAVAN_ROBBERY_GUARDED = 5
    const val SPIRIT_PAPER = "Papier Duchowy"

    private val TIER_PRICE = listOf(10, 25, 60, 150, 400, 1000)

    private val herbTiers by lazy { ResourceCatalog.herbs.associate { it.name to it.tier } }
    private val mineralTiers by lazy { ResourceCatalog.minerals.associate { it.name to it.tier } }
    private val bossDrops by lazy { com.example.game.WuxiaGenerator.getInitialRegions().mapNotNull { it.bossBeast?.dropItemName }.toSet() }

    /** Cena bazowa (jakość Zwykła). null = przedmiot niehandlowy. */
    fun basePrice(item: String): Int? {
        val (base, _) = Quality.parse(item)
        herbTiers[base]?.let { return TIER_PRICE[(it - 1).coerceIn(0, 5)] }
        mineralTiers[base]?.let { return TIER_PRICE[(it - 1).coerceIn(0, 5)] }
        GameContent.recipeForPill(base)?.let { return TIER_PRICE[(it.tier - 1).coerceIn(0, 5)] * 3 }
        if (base in bossDrops) return 800
        Realms.methodForScroll(base)?.let { m ->
            return when (m.grade) {
                MethodGrade.YELLOW -> 200
                MethodGrade.BLACK -> 1_500
                MethodGrade.MYSTIC -> 6_000
                MethodGrade.HEAVENLY -> 15_000
                MethodGrade.PRIMORDIAL -> 50_000
            }
        }
        return when (base) {
            DiscipleRules.FOOD -> DiscipleRules.FOOD_PRICE
            SPIRIT_PAPER -> 8
            else -> null
        }
    }

    fun multiplier(s: GameState, item: String): Double = s.marketMultipliers[Quality.parse(item).first] ?: 1.0

    fun sellPrice(s: GameState, item: String): Int {
        val base = basePrice(item) ?: return 0
        val q = Quality.parse(item).second
        val disaster = if (herbTiers.containsKey(Quality.parse(item).first)) Catastrophes.herbPriceFactor(s) else 1.0
        return (base * multiplier(s, item) * Seasons.sellFactor(Seasons.of(s.currentDay)) * q.effect * disaster).roundToInt().coerceAtLeast(1)
    }

    /** Na targu kupuje się tylko proste surowce (stopnie 1–3), ryż i papier duchowy. */
    fun buyable(item: String): Boolean {
        val base = Quality.parse(item).first
        if (base == DiscipleRules.FOOD || base == SPIRIT_PAPER) return true
        val tier = herbTiers[base] ?: mineralTiers[base] ?: return false
        return tier <= 3
    }

    fun buyPrice(s: GameState, item: String): Int =
        ((basePrice(item) ?: 0) * multiplier(s, item) * BUY_MARKUP).roundToInt().coerceAtLeast(1)

    fun sell(s: GameState, item: String, quantity: Int): StepResult {
        val have = s.inventory[item] ?: 0
        val qty = quantity.coerceAtMost(have)
        if (qty <= 0) return fail(s, "Brak $item w magazynie.")
        basePrice(item) ?: return fail(s, "Kupcy nie przyjmują: $item.")
        var mult = multiplier(s, item)
        var total = 0
        repeat(qty) {
            total += (basePrice(item)!! * mult * Seasons.sellFactor(Seasons.of(s.currentDay)) * Quality.parse(item).second.effect).roundToInt().coerceAtLeast(1)
            mult = (mult * (1 - SELL_PRESSURE)).coerceAtLeast(MIN_MULT)
        }
        return ok(
            s.copy(
                spiritStones = s.spiritStones + total,
                inventory = s.inventory + (item to have - qty),
                marketMultipliers = s.marketMultipliers + (Quality.parse(item).first to mult)
            ),
            "Sprzedano $qty× $item za $total kamieni."
        )
    }

    fun buy(s: GameState, item: String, quantity: Int): StepResult {
        if (!buyable(item)) return fail(s, "Kupcy nie sprzedają: $item.")
        if (quantity <= 0) return ok(s)
        // Każda kupiona sztuka podbija cenę o 2% – brak spekulacji na krachu.
        val base = basePrice(item) ?: 0
        var mult = multiplier(s, item)
        var cost = 0
        repeat(quantity) {
            cost += (base * mult * BUY_MARKUP).roundToInt().coerceAtLeast(1)
            mult = (mult * (1 + SELL_PRESSURE)).coerceAtMost(MAX_MULT)
        }
        if (s.spiritStones < cost) return fail(s, "Zakup $quantity× $item kosztuje $cost kamieni.")
        return ok(
            s.copy(
                spiritStones = s.spiritStones - cost,
                inventory = s.inventory + (item to (s.inventory[item] ?: 0) + quantity),
                marketMultipliers = s.marketMultipliers + (Quality.parse(item).first to mult)
            ),
            "Kupiono $quantity× $item za $cost kamieni."
        )
    }

    // --- Karawany ---

    fun sendCaravan(s: GameState, ctx: EngineContext, item: String, quantity: Int, guardId: String?): StepResult {
        val have = s.inventory[item] ?: 0
        if (quantity <= 0 || have < quantity) return fail(s, "Karawana potrzebuje $quantity× $item (masz $have).")
        basePrice(item) ?: return fail(s, "Tego towaru nie sprzedasz w miastach.")
        val rng = ctx.rng("caravan")
        val days = 2 + rng.nextInt(5)
        val premium = 1.3 + rng.nextDouble() * 0.5
        val unit = (sellPrice(s, item) * premium).roundToInt()
        val caravan = Caravan(rng.nextId("car"), item, quantity, s.currentDay + days, guardId, unit)
        return ok(
            s.copy(inventory = s.inventory + (item to have - quantity), caravans = s.caravans + caravan),
            "Karawana z $quantity× $item wyrusza do miasta ($days dni, ok. ${unit * quantity} kamieni)."
        )
    }

    // --- Czarny rynek (tylko nocą) ---

    data class BlackOffer(val item: String, val price: Int, val fakeChance: Int)

    fun blackMarketOpen(s: GameState) = s.timeOfDay == TimeOfDay.NIGHT || s.timeOfDay == TimeOfDay.FULL_MOON

    /** Oferty czarnego rynku zależą od ziarna i dnia – ta sama noc, te same oferty. */
    fun blackOffers(s: GameState): List<BlackOffer> {
        val rng = GameRng.stream(s.worldSeed, s.currentDay.toLong(), "black-market")
        val path = com.example.model.CultivationPath.entries.random(rng)
        val grade = if (rng.nextInt(100) < 25) MethodGrade.MYSTIC else MethodGrade.BLACK
        val scroll = Realms.methodOf(path, grade).scrollItem
        return listOf(
            BlackOffer(scroll, (basePrice(scroll) ?: 1500) * 2, fakeChance = 15),
            BlackOffer(CultivationSystem.TRIBULATION_PILL, 1_500, fakeChance = 10)
        )
    }

    fun buyBlack(s: GameState, ctx: EngineContext, index: Int): StepResult {
        if (!blackMarketOpen(s)) return fail(s, "Czarny rynek otwiera się dopiero nocą.")
        val offer = blackOffers(s).getOrNull(index) ?: return ok(s)
        if (s.spiritStones < offer.price) return fail(s, "Handlarz chce ${offer.price} kamieni.")
        val rng = ctx.rng("black-deal")
        val fake = rng.nextInt(100) < offer.fakeChance
        val caught = rng.nextInt(100) < 10
        var st = s.copy(spiritStones = s.spiritStones - offer.price)
        if (!fake) st = st.copy(inventory = st.inventory + (offer.item to (st.inventory[offer.item] ?: 0) + 1))
        if (caught) st = st.copy(sectReputation = st.sectReputation - 10, karma = (st.karma - 3).coerceAtLeast(-100))
        val msg = buildString {
            append(if (fake) "Podróbka! Handlarz zniknął w ciemności z ${offer.price} kamieniami." else "Kupiono na czarnym rynku: ${offer.item}.")
            if (caught) append(" Ktoś was widział (−10 reputacji).")
        }
        return ok(st, msg)
    }

    // --- Dzienne: ceny, karawany ---

    fun daily(frame: TickFrame) {
        val s = frame.before
        val rng = frame.ctx.rng("market-daily")
        val drift = frame.day % 5 == 0
        frame.marketMultipliers = s.marketMultipliers.mapValues { (_, m) ->
            var v = m + (1.0 - m) * RECOVERY
            if (drift) v *= 0.9 + rng.nextDouble() * 0.2
            v.coerceIn(MIN_MULT, MAX_MULT)
        }
        val (arrived, travelling) = frame.caravans.partition { it.returnDay <= frame.day }
        frame.caravans = travelling
        arrived.forEach { c ->
            val risk = if (c.guardId != null && frame.disciples.any { it.id == c.guardId }) CARAVAN_ROBBERY_GUARDED else CARAVAN_ROBBERY
            if (rng.nextInt(100) < risk) {
                frame.notice = frame.notice ?: "Bandyci napadli karawanę z ${c.quantity}× ${c.item}! Towar przepadł."
            } else {
                frame.stonesBonus += c.unitPrice * c.quantity
                frame.notice = frame.notice ?: "Karawana wróciła: ${c.quantity}× ${c.item} sprzedane za ${c.unitPrice * c.quantity} kamieni."
            }
        }
    }
}

/** Etap 4: tablica kontraktów – co 7 dni nowe zlecenia od wiosek, miast i Dworu. */
object Contracts {
    const val REFRESH_DAYS = 7
    const val BOARD_SIZE = 4
    val CLIENTS = listOf("Wioska Bambusowa", "Miasto Jadeitowe", "Dwór Cesarski", "Gildia Kupców", "Klasztor Wadżry")

    fun generate(s: GameState, rng: GameRng, day: Int): List<Contract> {
        val herbs = ResourceCatalog.herbs.filter { it.tier <= 3 }.map { it.name }
        val pills = GameContent.alchemyRecipes.filter { it.tier <= 2 }.map { it.pillName }
        // Etap 5c: łowy tylko w odkrytych krainach.
        val regions = s.regions.filter { it.bossBeast != null && (s.discoveredRegionIds.isEmpty() || it.id in s.discoveredRegionIds) }
        return List(BOARD_SIZE) { i ->
            val client = CLIENTS.random(rng)
            if (i == BOARD_SIZE - 1 && regions.isNotEmpty()) {
                val r = regions.take(4 + s.regions.count { it.explorationProgress > 0 }).random(rng)
                Contract(rng.nextId("ct"), client, ContractType.HUNT, r.bossBeast!!.name, 1, r.id,
                    rewardStones = r.bossBeast.spiritStoneReward, rewardReputation = 30, expiresDay = day + 2 * REFRESH_DAYS)
            } else {
                val item = (if (rng.nextBoolean()) herbs else pills).random(rng)
                val qty = if (item in pills) 1 + rng.nextInt(3) else 5 + rng.nextInt(11)
                val value = (Market.basePrice(item) ?: 20) * qty
                Contract(rng.nextId("ct"), client, ContractType.DELIVER, item, qty, null,
                    rewardStones = (value * 1.6).roundToInt(), rewardReputation = 10 + rng.nextInt(16), expiresDay = day + REFRESH_DAYS)
            }
        }
    }

    fun fulfill(s: GameState, id: String): StepResult {
        val c = s.contracts.find { it.id == id && !it.done } ?: return ok(s)
        if (c.type != ContractType.DELIVER) return fail(s, "Zlecenie łowów wykonuje się, pokonując bossa w krainie.")
        val have = Quality.count(s.inventory, c.item)
        if (have < c.quantity) return fail(s, "Zlecenie wymaga ${c.quantity}× ${c.item} (masz $have).")
        val inv = s.inventory.toMutableMap()
        repeat(c.quantity) { Quality.takeBest(inv, c.item) }
        return ok(
            s.copy(
                inventory = inv.toMap(),
                spiritStones = s.spiritStones + c.rewardStones,
                sectReputation = s.sectReputation + c.rewardReputation,
                contracts = s.contracts.map { if (it.id == id) it.copy(done = true) else it }
            ),
            "${c.client}: zlecenie wykonane! +${c.rewardStones} kamieni, +${c.rewardReputation} reputacji."
        )
    }

    /** Pokonanie bossa zalicza zlecenia łowów w tej krainie. */
    fun onBossDefeated(s: GameState, regionId: String): Pair<GameState, String?> {
        val hunts = s.contracts.filter { it.type == ContractType.HUNT && !it.done && it.regionId == regionId }
        if (hunts.isEmpty()) return s to null
        val stones = hunts.sumOf { it.rewardStones }
        val rep = hunts.sumOf { it.rewardReputation }
        return s.copy(
            spiritStones = s.spiritStones + stones,
            sectReputation = s.sectReputation + rep,
            contracts = s.contracts.map { c -> if (hunts.any { it.id == c.id }) c.copy(done = true) else c }
        ) to "Zlecenie łowów wykonane! +$stones kamieni, +$rep reputacji."
    }

    fun daily(frame: TickFrame) {
        val day = frame.day
        frame.contracts = frame.contracts.filter { !it.done && it.expiresDay >= day }
        if (day >= frame.before.contractsRefreshDay) {
            val fresh = generate(frame.before, frame.ctx.rng("contracts"), day)
            frame.contracts = (frame.contracts + fresh).takeLast(BOARD_SIZE + 2)
            frame.contractsRefreshDay = day + REFRESH_DAYS
        }
    }
}
