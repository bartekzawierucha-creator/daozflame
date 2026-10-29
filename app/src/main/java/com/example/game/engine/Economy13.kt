package com.example.game.engine

import com.example.game.engine.DiscipleRules.has
import com.example.model.AuctionLot
import com.example.model.Disciple
import com.example.model.MajorRealm

/**
 * Etap 13: gospodarka z ujściami. Silniejsza sekta więcej kosztuje (pensje rosną z królestwem, budynki wymagają
 * utrzymania), a kamienie mają na co iść: Dom Aukcyjny co kilkanaście minut wystawia artefakty, zwoje metod
 * i rzadkie pigułki w cenach zależnych od stopnia przedmiotu.
 */
object Upkeep {
    /** Mnożnik pensji z królestwa: Fundamenty ×1,5, Rdzeń ×2, Dusza ×2,5 … */
    fun realmFactor(d: Disciple): Double = 1.0 + 0.5 * ((d.majorRealm?.ordinal ?: 1) - MajorRealm.QI_CONDENSATION.ordinal).coerceAtLeast(0)

    const val FACILITY_UPKEEP = 6

    /** Dzienne utrzymanie budynków: poziom² × [FACILITY_UPKEEP]. */
    fun facilities(s: GameState): Int = s.facilities.sumOf { it.level * it.level * FACILITY_UPKEEP }

    /** Bilans dnia (bez zysków z handlu): dochód podstawowy i terytoria minus pensje, budynki i edykty. */
    fun daily(s: GameState): Pair<Int, Int> {
        val income = s.disciples.sumOf { (2 * (1.0 + 0.25 * ((it.majorRealm?.ordinal ?: 1) - 1).coerceAtLeast(0))).toInt() } * TimePhase.TICKS_PER_PHASE * 5 +
            s.territories.filter { it.controllingSectId == "player" && it.resourceType.contains("Kamienie") }.sumOf { it.yieldAmountPerCycle } * TimePhase.TICKS_PER_PHASE * 5
        val costs = s.disciples.sumOf { DiscipleRules.salary(it) } + facilities(s) + EdictCatalog.dailyUpkeep(s)
        return income to costs
    }
}

object Auction {
    const val REFRESH_DAYS = 20
    const val LOTS = 4

    private val TIER_PRICE = listOf(400, 1_200, 3_500, 9_000, 22_000, 50_000)

    fun rarityTier(r: String): Int = when (r) {
        "Rzadki" -> 1
        "Niebiański" -> 2
        "Boski" -> 3
        "Nieśmiertelny" -> 4
        "Wieczne Dao" -> 5
        else -> 0
    }

    /** Najwyższy stopień ofert rośnie z królestwem najlepszego ucznia. */
    fun maxTier(s: GameState): Int = ((s.disciples.maxOfOrNull { it.majorRealm?.ordinal ?: 1 } ?: 1) - 1).coerceIn(1, 5)

    fun generate(s: GameState, ctx: EngineContext): List<AuctionLot> {
        val rng = ctx.rng("auction")
        val top = maxTier(s)
        return List(LOTS) { i ->
            when (if (i == 0) 0 else rng.nextInt(3)) {
                0 -> {
                    val pool = GameContent.forgingRecipes.map { it.artifactResult }.filter { rarityTier(it.rarity) in (top - 2)..top }
                        .ifEmpty { GameContent.forgingRecipes.map { it.artifactResult } }
                    val a0 = pool.random(rng)
                    val q = Quality.roll(rng, 20)
                    val a = a0.copy(quality = q, attackBonus = (a0.attackBonus * q.artifactStats).toInt(), defenseBonus = (a0.defenseBonus * q.artifactStats).toInt())
                    AuctionLot(a.name, (TIER_PRICE[rarityTier(a.rarity)] * q.artifactStats * 1.5).toInt(), artifact = a)
                }
                1 -> {
                    val grade = com.example.model.MethodGrade.entries.getOrElse((top / 2 + rng.nextInt(2)).coerceAtMost(3)) { com.example.model.MethodGrade.BLACK }
                    val path = CombatSystem.wantedPath(s, rng)
                    val m = Realms.methodOf(path, grade)
                    AuctionLot(m.scrollItem, TIER_PRICE[(grade.ordinal + 1).coerceAtMost(5)] * 2, item = m.scrollItem)
                }
                else -> {
                    val pills = listOf(GameContent.BREAKTHROUGH_PILL to 0, CultivationSystem.TRIBULATION_PILL to 2, "Talizman Piorunochronu" to 1)
                    val (name, t) = pills.random(rng)
                    val n = 2 + rng.nextInt(3)
                    AuctionLot("$name ×$n", TIER_PRICE[(t + top / 2).coerceAtMost(5)] / 3 * n, item = "$name×$n")
                }
            }
        }
    }

    fun daily(s: GameState, ctx: EngineContext): Pair<GameState, List<String>> {
        if (s.tutorialStep != Tutorial.DONE || s.currentDay < s.auctionRefreshDay) return s to emptyList()
        val lots = generate(s, ctx)
        return s.copy(auction = lots, auctionRefreshDay = s.currentDay + REFRESH_DAYS) to
            listOf("🔨 Dom Aukcyjny wystawia nowe przedmioty: ${lots.first().name} i inne.")
    }

    fun buy(s: GameState, index: Int): StepResult {
        val lot = s.auction.getOrNull(index) ?: return ok(s)
        if (lot.sold) return fail(s, "Ten przedmiot już sprzedano.")
        if (s.spiritStones < lot.price) return fail(s, "Potrzeba ${lot.price} kamieni.")
        var st = s.copy(spiritStones = s.spiritStones - lot.price, auction = s.auction.mapIndexed { i, l -> if (i == index) l.copy(sold = true) else l })
        lot.artifact?.let { st = st.copy(vaultArtifacts = st.vaultArtifacts + it) }
        lot.item?.let { raw ->
            val (name, n) = if ("×" in raw) raw.substringBefore("×") to (raw.substringAfter("×").toIntOrNull() ?: 1) else raw to 1
            st = st.copy(inventory = st.inventory + (name to (st.inventory[name] ?: 0) + n))
        }
        return ok(st, "🔨 Wylicytowano: ${lot.name} za ${lot.price} kamieni.")
    }
}

/** Etap 13: nowe źródła ekwipunku – bossowie i kamienie milowe wieży. */
object Loot {
    const val BOSS_ARTIFACT_CHANCE = 35

    /** Artefakt na miarę krainy (indeks krainy → stopień rzadkości). */
    fun artifactFor(regionIndex: Int, rng: kotlin.random.Random): com.example.model.Artifact {
        val tier = (1 + regionIndex / 3).coerceIn(1, 5)
        val pool = GameContent.forgingRecipes.map { it.artifactResult }.filter { Auction.rarityTier(it.rarity) in (tier - 1)..tier }
            .ifEmpty { GameContent.forgingRecipes.map { it.artifactResult } }
        val a = pool.random(rng)
        val q = Quality.roll(rng, 10)
        return a.copy(quality = q, attackBonus = (a.attackBonus * q.artifactStats).toInt(), defenseBonus = (a.defenseBonus * q.artifactStats).toInt())
    }
}

/**
 * Etap 13: uczniowie rozwijają się sami – wybierają ścieżkę, uczą się technik za punkty Dao i sięgają po lepszy zwój
 * z magazynu, gdy metoda blokuje królestwo. Dzięki temu sekta rośnie także pod nieobecność mistrza.
 */
object SelfStudy {
    const val EVERY_DAYS = 5

    fun preferredPath(d: Disciple): com.example.model.CultivationPath = when {
        d.martialSchool == com.example.model.MartialSchool.BODY_ASURA -> com.example.model.CultivationPath.BODY
        d.martialSchool == com.example.model.MartialSchool.SWORD_IMMORTAL -> com.example.model.CultivationPath.SWORD
        d.has("alchemist") -> com.example.model.CultivationPath.PILLS
        d.has("insightful") -> com.example.model.CultivationPath.HEART
        else -> com.example.model.CultivationPath.ORTHODOX
    }

    fun daily(s0: GameState): Pair<GameState, List<String>> {
        if (!s0.autonomy || !s0.e3Initialized || s0.currentDay % EVERY_DAYS != 0) return s0 to emptyList()
        var s = s0
        val notes = mutableListOf<String>()
        for (id in s0.disciples.map { it.id }) {
            var d = s.disciple(id) ?: continue
            // Ścieżka: przy Fundamentach, gdy jej brak.
            if (d.path == null && (d.majorRealm?.ordinal ?: 0) >= MajorRealm.FOUNDATION.ordinal) {
                val r = CultivationCommands.choosePath(s, id, preferredPath(d))
                if (r.state != s) { s = r.state; notes += "📖 ${Autonomy.short(d)} sam wybiera ścieżkę: ${preferredPath(d).polishName}." }
                d = s.disciple(id) ?: continue
            }
            // Techniki: najtańsza dostępna, na którą starcza punktów Dao.
            val skill = GameContent.skills.filter { it.martialSchool == d.martialSchool && it.id !in d.unlockedSkillIds && it.tier <= d.rank.maxTechniqueTier }
                .sortedBy { it.costDaoPoints }.firstOrNull { DiscipleRules.skillCost(d, it.costDaoPoints) <= d.daoPoints }
            if (skill != null) {
                val r = SectCommands.unlockSkill(s, id, skill.id)
                if (r.state != s) { s = r.state; notes += "📖 ${Autonomy.short(d)} opanowuje technikę: ${skill.name}." }
                d = s.disciple(id) ?: continue
            }
            // Zwój: gdy metoda nie sięga następnego królestwa, a w magazynie leży lepsza tej samej ścieżki.
            val next = d.majorRealm?.next
            if (next != null && next.ordinal > Realms.ceiling(d).ordinal) {
                val scroll = s.inventory.filter { it.value > 0 }.keys.mapNotNull { k -> Realms.methodForScroll(k)?.let { k to it } }
                    .filter { (_, m) -> m.grade.ceiling.ordinal > Realms.ceiling(d).ordinal && (m.path == d.path || d.path == null) }
                    .maxByOrNull { it.second.grade.ordinal }
                if (scroll != null) {
                    val r = CultivationCommands.learnMethod(s, id, scroll.first)
                    if (r.state != s) { s = r.state; notes += "📖 ${Autonomy.short(d)} studiuje zwój: ${scroll.second.name}." }
                }
            }
        }
        return s to notes
    }
}

/**
 * Etap 13: Rada Starszych awansuje uczniów, gdy mistrz zwleka zbyt długo – sekta nie stoi w miejscu
 * pod nieobecność gracza (a ambitni nie odchodzą przez zapomnienie).
 */
object Elders {
    const val GRACE_DAYS = 30

    fun daily(s0: GameState): Pair<GameState, List<String>> {
        if (!s0.e2Initialized || s0.currentDay % 5 != 0) return s0 to emptyList()
        var s = s0
        val notes = mutableListOf<String>()
        s0.disciples.filter { DiscipleRules.canPromote(s0, it) }.forEach { d ->
            val since = d.promotionDueSinceDay ?: return@forEach
            if (s.currentDay - since < GRACE_DAYS) return@forEach
            val r = LifeCommands.promote(s, d.id)
            if (r.state != s) { s = r.state; notes += "📜 Rada Starszych awansuje ${Autonomy.short(d)} – mistrz zwlekał." }
        }
        return s to notes
    }
}
