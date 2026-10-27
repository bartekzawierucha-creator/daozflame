package com.example.game.engine

import com.example.game.content.ResourceCatalog
import com.example.model.FieldPlot
import com.example.model.MajorRealm
import com.example.model.Season
import com.example.model.SpiritElement
import kotlin.math.roundToInt

/** Etap 4: pola duchowych ziół. */
object Fields {
    const val BASE_SIZE = 4
    const val MAX_SIZE = 6
    const val EXPAND_COST = 5_000
    const val HARVEST_TALISMAN = "Talizman Zbioru"

    private val herbTier by lazy { ResourceCatalog.herbs.associate { it.name to it.tier } }

    fun size(s: GameState): Int = kotlin.math.sqrt(s.fields.size.toDouble()).roundToInt().coerceAtLeast(BASE_SIZE)
    fun empty(size: Int) = List(size * size) { FieldPlot() }

    fun growDays(tier: Int): Int = when (tier) {
        1 -> 2
        2 -> 4
        3 -> 7
        4 -> 12
        else -> 20
    }

    fun plant(s: GameState, index: Int, herb: String): StepResult {
        val plot = s.fields.getOrNull(index) ?: return ok(s)
        if (plot.herb != null) return fail(s, "To poletko jest już obsiane.")
        val tier = herbTier[herb] ?: return fail(s, "$herb nie da się wysiać.")
        if (Seasons.of(s.currentDay) == Season.WINTER) return fail(s, "Zimą ziemia jest zamarznięta.")
        val maxTier = Vein.fieldTier(s.veinLevel)
        if (tier > maxTier) return fail(s, "Zioła $tier. stopnia wymagają mocniejszej żyły duchowej (teraz do $maxTier. stopnia).")
        val have = s.inventory[herb] ?: 0
        if (have < 1) return fail(s, "Potrzebna jedna sztuka $herb jako nasiono.")
        val planted = plot.copy(herb = herb, plantedDay = s.currentDay, readyDay = s.currentDay + growDays(tier))
        return ok(
            s.copy(inventory = s.inventory + (herb to have - 1), fields = s.fields.mapIndexed { i, p -> if (i == index) planted else p }),
            "Wysiano $herb (zbiór za ${growDays(tier)} dni)."
        )
    }

    /** Sąsiedzi tego samego zioła: +10% każdy (4-sąsiedztwo). */
    fun synergy(s: GameState, index: Int): Double {
        val n = size(s)
        val herb = s.fields.getOrNull(index)?.herb ?: return 0.0
        val (r, c) = index / n to index % n
        return listOf(r - 1 to c, r + 1 to c, r to c - 1, r to c + 1)
            .filter { (a, b) -> a in 0 until n && b in 0 until n }
            .count { (a, b) -> s.fields[a * n + b].herb == herb } * 0.10
    }

    fun harvestAll(s: GameState, ctx: EngineContext): StepResult {
        val ready = s.fields.indices.filter { s.fields[it].herb != null && s.fields[it].readyDay <= s.currentDay }
        if (ready.isEmpty()) return fail(s, "Żadne poletko nie jest gotowe do zbioru.")
        val season = Seasons.of(s.currentDay)
        if (season == Season.WINTER) return fail(s, "Zimą zbiory czekają pod śniegiem.")
        val rng = ctx.rng("harvest")
        val woodFarmers = s.disciples.count { it.role == com.example.model.DiscipleRole.HERB_GATHERING && it.spiritRoot?.elements?.contains(SpiritElement.WOOD) == true }.coerceAtMost(3)
        val inv = s.inventory.toMutableMap()
        val talisman = (inv[HARVEST_TALISMAN] ?: 0) > 0
        if (talisman) inv[HARVEST_TALISMAN] = (inv[HARVEST_TALISMAN] ?: 0) - 1
        var total = 0
        ready.forEach { i ->
            val herb = s.fields[i].herb!!
            val factor = Seasons.fieldYieldFactor(season) * (1 + synergy(s, i) + 0.15 * woodFarmers) * (if (talisman) 1.5 else 1.0) * Catastrophes.fieldFactor(s)
            val amount = ((3 + rng.nextInt(3)) * factor).roundToInt().coerceAtLeast(1)
            inv[herb] = (inv[herb] ?: 0) + amount
            total += amount
        }
        return ok(
            s.copy(inventory = inv.toMap(), fields = s.fields.mapIndexed { i, p -> if (i in ready) FieldPlot() else p }),
            "Zebrano $total ziół z ${ready.size} poletek."
        )
    }

    fun expand(s: GameState): StepResult {
        if (size(s) >= MAX_SIZE) return fail(s, "Pola zajmują już całe zbocze.")
        if (s.spiritStones < EXPAND_COST) return fail(s, "Poszerzenie pól kosztuje $EXPAND_COST kamieni.")
        val n = size(s)
        val bigger = List(MAX_SIZE * MAX_SIZE) { i ->
            val (r, c) = i / MAX_SIZE to i % MAX_SIZE
            if (r < n && c < n) s.fields[r * n + c] else FieldPlot()
        }
        return ok(s.copy(spiritStones = s.spiritStones - EXPAND_COST, fields = bigger), "Pola ziół rozrosły się do ${MAX_SIZE}×$MAX_SIZE.")
    }
}

/** Etap 4: Lista Płomieni Niebios – ranga, temperatura, miejsce (kocioł, kuźnia, nosiciel). */
object Flames {

    enum class Rank(val polishName: String, val bearerRealm: MajorRealm) {
        EARTH("Ziemska", MajorRealm.QI_CONDENSATION),
        HEAVEN("Niebiańska", MajorRealm.FOUNDATION),
        ANCIENT("Starożytna", MajorRealm.CORE_FORMATION),
        DIVINE("Boska", MajorRealm.NASCENT_SOUL),
        PRIMORDIAL("Pierwotna", MajorRealm.MAHAYANA)
    }

    data class Info(val position: Int, val rank: Rank, val baseTemperature: Int)

    /** Pozycja na Liście (1 = najsilniejszy), ranga i temperatura bazowa. */
    val LIST: Map<String, Info> = mapOf(
        "flame_12" to Info(1, Rank.PRIMORDIAL, 12_000),
        "flame_11" to Info(2, Rank.DIVINE, 9_000),
        "flame_7" to Info(3, Rank.DIVINE, 7_500),
        "flame_9" to Info(4, Rank.ANCIENT, 6_100),
        "flame_4" to Info(5, Rank.ANCIENT, 5_200),
        "flame_6" to Info(6, Rank.ANCIENT, 4_400),
        "flame_8" to Info(7, Rank.HEAVEN, 3_600),
        "flame_3" to Info(8, Rank.HEAVEN, 3_000),
        "flame_5" to Info(9, Rank.HEAVEN, 2_400),
        "flame_10" to Info(10, Rank.EARTH, 1_200),
        "flame_2" to Info(11, Rank.EARTH, 1_600),
        "flame_1" to Info(12, Rank.EARTH, 1_800)
    )

    const val STARTING_FLAME = "flame_1"
    const val MAX_REFINEMENT = 9
    const val MOVE_FROM_BODY_PENALTY = 3

    fun info(id: String): Info = LIST[id] ?: Info(12, Rank.EARTH, 1_500)

    fun temperature(f: com.example.model.GeocentricFlame): Int = info(f.id).baseTemperature + (f.refinementLevel - 1) * 350

    /** Minimalna temperatura do warzenia pigułek danego stopnia. */
    fun alchemyTemperature(tier: Int): Int = when {
        tier >= 6 -> 8_000
        tier == 5 -> 5_000
        tier == 4 -> 3_000
        else -> 0
    }

    /** Minimalna temperatura do kucia artefaktów danej rzadkości. */
    fun forgeTemperature(rarity: String): Int = when (rarity) {
        "Wieczne Dao" -> 9_000
        "Nieśmiertelny" -> 6_000
        "Boski" -> 4_000
        else -> 0
    }

    fun where(s: GameState, flameId: String): String = when {
        s.activeFlameId == flameId -> "kocioł alchemii"
        s.forgeFlameId == flameId -> "palenisko kuźni"
        else -> s.disciples.find { it.flameId == flameId }?.let { "ciało: ${it.name.substringBefore(" (")}" } ?: "zapieczętowany"
    }

    private fun detach(s: GameState, flameId: String): GameState = s.copy(
        activeFlameId = s.activeFlameId.takeIf { it != flameId },
        forgeFlameId = s.forgeFlameId.takeIf { it != flameId },
        disciples = s.disciples.map { if (it.flameId == flameId) it.copy(flameId = null) else it }
    )

    private fun penaltyIfInBody(s: GameState, flameId: String): GameState {
        if (s.disciples.none { it.flameId == flameId }) return s
        return s.copy(flames = s.flames.map {
            if (it.id == flameId) it.copy(refinementLevel = (it.refinementLevel - MOVE_FROM_BODY_PENALTY).coerceAtLeast(1)) else it
        })
    }

    fun toForge(s: GameState, flameId: String): StepResult {
        val f = s.flames.find { it.id == flameId } ?: return ok(s)
        val moved = detach(penaltyIfInBody(s, flameId), flameId).copy(forgeFlameId = flameId)
        return ok(moved, "${f.name} płonie teraz w palenisku kuźni.")
    }

    fun toCauldron(s: GameState, flameId: String): StepResult {
        val f = s.flames.find { it.id == flameId } ?: return ok(s)
        val moved = detach(penaltyIfInBody(s, flameId), flameId).copy(activeFlameId = flameId)
        return ok(moved, "Wybrano Płomień Geocentryczny: ${f.name}!")
    }

    fun toDisciple(s: GameState, flameId: String, discipleId: String): StepResult {
        val f = s.flames.find { it.id == flameId } ?: return ok(s)
        val d = s.disciple(discipleId) ?: return ok(s)
        val need = info(flameId).rank.bearerRealm
        if ((d.majorRealm?.ordinal ?: 0) < need.ordinal) return fail(s, "Płomień rangi ${info(flameId).rank.polishName} wymaga nosiciela w ${need.polishName}.")
        if (d.flameId != null) return fail(s, "${d.name} nosi już płomień.")
        val moved = detach(s, flameId).let { st -> st.copy(disciples = st.disciples.map { if (it.id == discipleId) it.copy(flameId = flameId) else it }) }
        return ok(moved, "${d.name} wchłania ${f.name}!")
    }
}

/** Etap 4: talizmany – przedmioty jednorazowe z papieru duchowego. */
object Talismans {
    data class Recipe(val name: String, val paper: Int, val ingredient: String, val ingredientCount: Int, val stones: Int, val description: String)

    val recipes: List<Recipe> = listOf(
        Recipe("Talizman Ognistej Kuli", 2, "Płomienny Żeń-szeń Smoka", 1, 40, "W walce: 250 obrażeń."),
        Recipe("Talizman Złotej Tarczy", 2, "Duchowy Nefryt", 2, 40, "W walce: leczy 300 HP."),
        Recipe("Talizman Pieczęci", 3, "Kwiat Niebiańskiego Zrozumienia", 1, 80, "W walce: przeciwnik traci następny ruch."),
        Recipe("Talizman Ucieczki", 2, "Fioletowy Korzeń Skorpiona", 1, 60, "W walce: ucieczka bez utraty reputacji."),
        Recipe("Talizman Uzdrowienia", 2, "Trawa Wiecznej Wiosny", 3, 30, "Poza walką: leczy ucznia o połowę HP."),
        Recipe("Talizman Ochrony", 4, "Gwiezdne Żelazo", 2, 120, "Chroni artefakt przed pęknięciem przy ulepszaniu."),
        Recipe("Talizman Piorunochronu", 4, "Kryształ Gromu", 1, 200, "Osłabia pioruny tribulacji o 15%."),
        Recipe(Fields.HARVEST_TALISMAN, 2, "Grzyb Księżycowej Rosy", 1, 50, "Następny zbiór z pól ×1,5.")
    )

    fun craft(s: GameState, name: String): StepResult {
        val r = recipes.find { it.name == name } ?: return ok(s)
        val paper = s.inventory[Market.SPIRIT_PAPER] ?: 0
        val ing = s.inventory[r.ingredient] ?: 0
        if (paper < r.paper) return fail(s, "${r.name} wymaga ${r.paper}× ${Market.SPIRIT_PAPER} (masz $paper).")
        if (ing < r.ingredientCount) return fail(s, "${r.name} wymaga ${r.ingredientCount}× ${r.ingredient}.")
        if (s.spiritStones < r.stones) return fail(s, "${r.name} kosztuje ${r.stones} kamieni.")
        return ok(
            s.copy(
                spiritStones = s.spiritStones - r.stones,
                inventory = s.inventory + mapOf(
                    Market.SPIRIT_PAPER to paper - r.paper,
                    r.ingredient to ing - r.ingredientCount,
                    r.name to (s.inventory[r.name] ?: 0) + 1
                )
            ),
            "Wypisano ${r.name}."
        )
    }

    /** Uzdrowienie poza walką. */
    fun heal(s: GameState, discipleId: String): StepResult {
        val name = "Talizman Uzdrowienia"
        val have = s.inventory[name] ?: 0
        if (have < 1) return fail(s, "Brak $name.")
        val d = s.disciple(discipleId) ?: return ok(s)
        return ok(
            s.copy(
                inventory = s.inventory + (name to have - 1),
                disciples = s.disciples.map { if (it.id == discipleId) it.copy(currentHp = (it.currentHp + it.maxHp / 2).coerceAtMost(it.maxHp)) else it }
            ),
            "${d.name} odzyskuje siły (+${d.maxHp / 2} HP)."
        )
    }
}
