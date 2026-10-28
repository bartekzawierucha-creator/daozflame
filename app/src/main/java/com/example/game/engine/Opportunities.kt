package com.example.game.engine

import com.example.game.WuxiaGenerator
import com.example.model.CultivationRealm
import com.example.model.Opportunity
import com.example.model.OpportunityKind

/**
 * Etap 11: rzadkie okazje, które przerywają rutynę środka gry. Pojawiają się co kilkanaście dni,
 * trwają [DURATION] dni i wymagają decyzji – kogo wysłać, czy zapłacić, czy odmówić.
 */
object Opportunities {
    const val DURATION = 3
    const val MIN_GAP = 8
    const val DAILY_CHANCE = 15
    const val MASTER_COMPREHENSION = 4
    const val MASTER_ATTACK_PERCENT = 8
    const val CAVE_QI_TICKS = 400
    const val REFUGEE_FOOD = 30

    fun title(o: Opportunity): String = o.kind.polishName

    fun examChance(d: com.example.model.Disciple): Int = (30 + d.comprehension).coerceIn(10, 90)
    fun caveChance(d: com.example.model.Disciple): Int = (d.combatPower / 40 + 40).coerceIn(30, 85)

    fun description(o: Opportunity): String = when (o.kind) {
        OpportunityKind.WANDERING_MASTER -> "Wędrowny mistrz miecza zatrzymał się u bram. Za ${o.cost} kamieni nauczy jednego ucznia (+$MASTER_COMPREHENSION zrozumienia, +$MASTER_ATTACK_PERCENT% ataku)."
        OpportunityKind.RARE_MERCHANT -> "Kupiec z Zachodnich Pustyń oferuje ${o.artifact?.name ?: "artefakt"} (${o.artifact?.quality?.polishName ?: ""}) za ${o.cost} kamieni."
        OpportunityKind.IMPERIAL_EXAM -> "Dwór ogłasza egzamin cesarski. Mądry uczeń przyniesie sławę – głupi wstyd. Liczy się zrozumienie."
        OpportunityKind.HIDDEN_CAVE -> "Zbieracze ziół znaleźli grotę dawnego mistrza. Śmiały uczeń zdobędzie Qi i skarb – albo rany."
        OpportunityKind.REFUGEES -> "Uchodźcy z wioski spalonej przez bestie proszą o schronienie. Kosztuje $REFUGEE_FOOD racji, ale wśród nich jest młody talent."
    }

    /** Czy okazja wymaga wskazania ucznia. */
    fun needsDisciple(k: OpportunityKind) = k == OpportunityKind.WANDERING_MASTER || k == OpportunityKind.IMPERIAL_EXAM || k == OpportunityKind.HIDDEN_CAVE

    fun daily(s0: GameState, ctx: EngineContext): Pair<GameState, List<String>> {
        if (s0.tutorialStep != Tutorial.DONE) return s0 to emptyList()
        var s = s0
        val notes = mutableListOf<String>()
        s.opportunity?.let { o ->
            if (s.currentDay > o.untilDay) {
                s = s.copy(opportunity = null)
                notes += "${o.kind.glyph} Okazja minęła: ${title(o)}."
            }
        }
        if (s.opportunity != null || s.currentDay - s.lastOpportunityDay < MIN_GAP) return s to notes
        val rng = ctx.rng("opportunity")
        if (rng.nextInt(100) >= DAILY_CHANCE) return s to notes
        val kind = OpportunityKind.entries[rng.nextInt(OpportunityKind.entries.size)]
        val scale = 1 + s.currentDay / 30
        val o = when (kind) {
            OpportunityKind.WANDERING_MASTER -> Opportunity(kind, s.currentDay + DURATION, cost = 600 * scale)
            OpportunityKind.RARE_MERCHANT -> {
                val r = GameContent.forgingRecipes.random(rng).artifactResult
                val q = Quality.roll(rng, 25)
                val a = r.copy(quality = q, attackBonus = (r.attackBonus * q.artifactStats).toInt(), defenseBonus = (r.defenseBonus * q.artifactStats).toInt())
                Opportunity(kind, s.currentDay + DURATION, cost = (r.attackBonus + r.defenseBonus) * 12 * scale, artifact = a)
            }
            else -> Opportunity(kind, s.currentDay + DURATION)
        }
        s = s.copy(opportunity = o, lastOpportunityDay = s.currentDay)
        notes += "${kind.glyph} Okazja: ${title(o)} (do dnia ${o.untilDay}). ${description(o)}"
        return s to notes
    }

    fun decline(s: GameState): StepResult {
        val o = s.opportunity ?: return ok(s)
        return ok(s.copy(opportunity = null), "Odprawiono: ${title(o)}.")
    }

    fun accept(s: GameState, ctx: EngineContext, discipleId: String?): StepResult {
        val o = s.opportunity ?: return ok(s)
        val d = discipleId?.let { s.disciple(it) }
        if (needsDisciple(o.kind) && d == null) return fail(s, "Wskaż ucznia.")
        if (s.spiritStones < o.cost) return fail(s, "Potrzeba ${o.cost} kamieni.")
        val paid = s.copy(spiritStones = s.spiritStones - o.cost, opportunity = null)
        val rng = ctx.rng("opportunity-accept")
        fun upd(f: (com.example.model.Disciple) -> com.example.model.Disciple) = paid.copy(disciples = paid.disciples.map { if (it.id == d!!.id) f(it) else it })
        return when (o.kind) {
            OpportunityKind.WANDERING_MASTER -> ok(
                upd { it.copy(comprehension = it.comprehension + MASTER_COMPREHENSION, attack = it.attack * (100 + MASTER_ATTACK_PERCENT) / 100,
                    diary = (it.diary + "Dz. ${s.currentDay}: Wędrowny mistrz pokazał mi cięcie, którego nie zapomnę.").takeLast(Autonomy.DIARY_SIZE)) },
                "🧙 Wędrowny mistrz uczy ${Autonomy.short(d!!)} przez trzy noce. +$MASTER_COMPREHENSION zrozumienia, +$MASTER_ATTACK_PERCENT% ataku."
            )
            OpportunityKind.RARE_MERCHANT -> ok(paid.copy(vaultArtifacts = paid.vaultArtifacts + listOfNotNull(o.artifact)),
                "🐫 Kupiec zostawia ${o.artifact?.name}. Załóż go uczniowi w ekwipunku.")
            OpportunityKind.IMPERIAL_EXAM -> {
                val chance = examChance(d!!)
                if (rng.nextInt(100) < chance) {
                    ok(upd { it.copy(loyalty = (it.loyalty + 5).coerceAtMost(100)) }.let { st -> st.copy(sectReputation = st.sectReputation + 120 + 10 * (s.currentDay / 30)) },
                        "📜 ${Autonomy.short(d)} zdaje egzamin cesarski z wyróżnieniem! Sława sekty rośnie (szansa była $chance%).")
                } else {
                    ok(upd { it.copy(trust = (it.trust - 5).coerceAtLeast(0)) }.let { st -> st.copy(sectReputation = (st.sectReputation - 20).coerceAtLeast(0)) },
                        "📜 ${Autonomy.short(d)} oblewa egzamin (szansa była $chance%). Dwór kręci nosem.")
                }
            }
            OpportunityKind.HIDDEN_CAVE -> {
                val brave = caveChance(d!!)
                if (rng.nextInt(100) < brave) {
                    val loot = GameContent.forgingRecipes.random(rng).artifactResult
                    var st = upd { if (s.e3Initialized) CultivationSystem.gainQi(it, CultivationSystem.ticksWorth(s, it, CAVE_QI_TICKS)) else it.copy(realmProgress = (it.realmProgress + 25).coerceAtMost(100)) }
                    st = st.copy(vaultArtifacts = st.vaultArtifacts + loot)
                    ok(st, "🕳 ${Autonomy.short(d)} wraca z groty z Qi dawnego mistrza i artefaktem: ${loot.name}.")
                } else {
                    ok(upd { it.copy(currentHp = (it.currentHp - it.maxHp / 2).coerceAtLeast(1), injuredUntilDay = s.currentDay + 5) },
                        "🕳 Grota była pułapką. ${Autonomy.short(d)} wraca ranny – 5 dni leczenia.")
                }
            }
            OpportunityKind.REFUGEES -> {
                val food = s.inventory[DiscipleRules.FOOD] ?: 0
                if (food < REFUGEE_FOOD) return fail(s, "Potrzeba $REFUGEE_FOOD racji ryżu.")
                var st = paid.copy(inventory = paid.inventory + (DiscipleRules.FOOD to food - REFUGEE_FOOD), karma = (paid.karma + 5).coerceAtMost(100))
                val full = st.e2Initialized && st.disciples.size >= DiscipleRules.capacity(st)
                if (full) return ok(st.copy(sectReputation = st.sectReputation + 15), "🏕 Uchodźcy znaleźli schronienie. Pawilon jest pełny, więc młody talent zostaje sługą sekty. Karma +5.")
                val kid = GameEngine.prepareCandidate(WuxiaGenerator.generateDisciple(rng, CultivationRealm.QI_CONDENSATION_1), rng, st)
                st = st.copy(disciples = st.disciples + kid.copy(loyalty = 85, trust = 70))
                ok(st, "🏕 Uchodźcy znaleźli schronienie. ${kid.name} prosi o przyjęcie do sekty – z wdzięcznością (lojalność 85). Karma +5.")
            }
        }
    }
}
