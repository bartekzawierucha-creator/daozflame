package com.example.game.engine

import com.example.game.content.ResourceCatalog
import com.example.game.engine.DiscipleRules.has
import com.example.model.ContractType
import com.example.model.StewardPolicy

/**
 * Etap 11: zarządca skarbca. Mistrz ustala politykę (sprzedawaj nadwyżki, oddawaj kontrakty, pilnuj spiżarni),
 * a wybrany uczeń prowadzi handel codziennie. Charyzmatyczny targuje się lepiej, chciwy przy niskiej lojalności podbiera.
 * Przedmioty obiecane uczniom są nietykalne.
 */
object Steward {
    const val MAX_SALES = 3
    const val FOOD_DAYS = 5
    const val CHARISMA_BONUS = 5
    const val GREED_SKIM = 5

    fun set(s: GameState, policy: StewardPolicy): StepResult {
        val id = policy.stewardId
        if (id != null && s.disciple(id) == null) return ok(s)
        val p = policy.copy(keepAbove = policy.keepAbove.coerceIn(0, 999))
        val who = id?.let { s.disciple(it) }?.let { Autonomy.short(it) }
        return ok(s.copy(steward = p), if (who == null) "Skarbiec bez zarządcy." else "Zarządca skarbca: $who.")
    }

    private fun reserved(s: GameState): Set<String> = s.disciples.mapNotNull { it.promise?.item }.toSet()

    private val tradeGoods by lazy { (ResourceCatalog.herbs + ResourceCatalog.minerals).map { it.name }.toSet() }

    const val EVERY_DAYS = 5

    fun daily(s0: GameState): Pair<GameState, List<String>> {
        if (s0.currentDay % EVERY_DAYS != 0) return s0 to emptyList()
        val p = s0.steward
        val d = p.stewardId?.let { s0.disciple(it) } ?: return s0 to emptyList()
        if (s0.expedition?.discipleId == d.id) return s0 to emptyList()
        var s = s0
        val done = mutableListOf<String>()
        val keep = reserved(s)
        // Spiżarnia.
        if (p.autoFood) {
            val need = s.disciples.size * FOOD_DAYS - (s.inventory[DiscipleRules.FOOD] ?: 0)
            if (need > 0) {
                val r = LifeCommands.buyFood(s, need)
                if (r.state != s) { s = r.state; done += "ryż ×$need" }
            }
        }
        // Kontrakty dostaw.
        if (p.autoContracts) {
            s.contracts.filter { !it.done && it.type == ContractType.DELIVER && it.item !in keep && Quality.count(s.inventory, it.item) >= it.quantity }.forEach { c ->
                val r = Contracts.fulfill(s, c.id)
                if (r.state.contracts.count { it.done } > s.contracts.count { it.done }) {
                    s = r.state.copy(meta = r.state.meta.copy(stats = r.state.meta.stats + ("contracts" to (r.state.meta.stats["contracts"] ?: 0) + 1)))
                    done += "kontrakt: ${c.client}"
                }
            }
        }
        // Nadwyżki – od największej, najwyżej [MAX_SALES] pozycji dziennie.
        if (p.keepAbove > 0) {
            val surplus = s.inventory.filter { (k, v) -> Quality.parse(k).first in tradeGoods && k !in keep && v > p.keepAbove }
                .entries.sortedByDescending { it.value }.take(MAX_SALES)
            for ((item, n) in surplus) {
                val before = s.spiritStones
                val r = Market.sell(s, item, n - p.keepAbove)
                if (r.state == s) continue
                val earned = r.state.spiritStones - before
                val adj = when {
                    d.has("charismatic") -> earned * CHARISMA_BONUS / 100
                    d.has("greedy") && d.loyalty < 50 -> -earned * GREED_SKIM / 100
                    else -> 0
                }
                s = r.state.copy(spiritStones = r.state.spiritStones + adj)
                done += "${n - p.keepAbove}× $item"
            }
        }
        if (done.isEmpty()) return s to emptyList()
        return s to listOf("🧮 ${Autonomy.short(d)} (zarządca): " + done.joinToString(", ") + ".")
    }
}
