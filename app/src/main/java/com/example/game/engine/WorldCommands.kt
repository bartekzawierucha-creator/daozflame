package com.example.game.engine

import com.example.model.DiplomaticStatus

/** Dyplomacja, terytoria, boss świata, arena i decyzje w zdarzeniach. */
object WorldCommands {

    fun interactRivalSect(s: GameState, sectId: String, action: String): StepResult {
        val sect = s.rivalSects.find { it.id == sectId } ?: return ok(s)
        fun update(f: (com.example.model.RivalSect) -> com.example.model.RivalSect) =
            s.rivalSects.map { if (it.id == sectId) f(it) else it }

        return when (action) {
            "GIFT" -> {
                val cost = 100
                if (s.spiritStones < cost) return fail(s, "Brak Kamieni Ducha na poselstwo darów ($cost wymagane)!")
                val rel = (sect.relations + 20).coerceAtMost(100)
                val status = when {
                    rel >= 60 -> DiplomaticStatus.ALLIANCE
                    rel >= 20 -> DiplomaticStatus.FRIENDLY
                    else -> DiplomaticStatus.NEUTRAL
                }
                ok(
                    s.copy(spiritStones = s.spiritStones - cost, rivalSects = update { it.copy(relations = rel, status = status) }),
                    "Wysłano dary do sekty ${sect.name}. Relacje wzrosły do $rel!"
                )
            }
            "TRADE" -> {
                val trade = sect.activeTrade ?: return fail(s, "Sekta nie oferuje obecnie wolnych karawan handlowych.")
                if (s.spiritStones < trade.costStones) return fail(s, "Brak ${trade.costStones} Kamieni Ducha na zakup towarów karawany!")
                ok(
                    s.copy(
                        spiritStones = s.spiritStones - trade.costStones,
                        inventory = s.inventory + (trade.rewardItem to (s.inventory[trade.rewardItem] ?: 0) + 3)
                    ),
                    "Pomyślnie zrealizowano handel z ${sect.name}: odebrano 3x ${trade.rewardItem}!"
                )
            }
            "ALLIANCE" -> {
                if (sect.relations < 40) return fail(s, "Relacje muszą wynosić co najmniej 40 punktów, aby zaproponować sojusz!")
                ok(
                    s.copy(rivalSects = update { it.copy(status = DiplomaticStatus.ALLIANCE) }, sectReputation = s.sectReputation + 30),
                    "Podpisano Wieczysty Sojusz z ${sect.name}!"
                )
            }
            "DECLARE_WAR" -> ok(
                s.copy(rivalSects = update { it.copy(status = DiplomaticStatus.TOTAL_WAR, relations = -90) }),
                "Wypowiedziano Wojnę Totalną sekcie ${sect.name}! Ich wojownicy będą atakować twoje kopalnie!"
            )
            else -> ok(s)
        }
    }

    fun garrisonPower(attack: Int, comprehension: Int): Int = attack * 8 + comprehension * 3

    fun captureTerritory(s: GameState, ctx: EngineContext, territoryId: String, discipleId: String): StepResult {
        val territory = s.territories.find { it.id == territoryId } ?: return ok(s)
        val d = s.disciple(discipleId) ?: return ok(s)
        if (territory.controllingSectId == "player") return fail(s, "To terytorium jest już pod jurysdykcją twojej sekty!")
        val power = garrisonPower(d.attack, d.comprehension)
        val success = power >= territory.garrisonDefensePower || ctx.rng("territory").nextInt(100) < 60
        if (!success) {
            return fail(s, "Garnizon ${territory.controllingSectName} odparł atak na ${territory.name}. Zwiększ moc swoich adeptów!")
        }
        return ok(
            s.copy(
                territories = s.territories.map {
                    if (it.id == territoryId) it.copy(
                        controllingSectId = "player",
                        controllingSectName = s.sectName,
                        assignedDiscipleId = d.id,
                        garrisonDefensePower = power
                    ) else it
                },
                spiritStones = s.spiritStones + 200,
                sectReputation = s.sectReputation + 40
            ),
            "🚩 Sukces ekspansji! Czempion ${d.name} podbił węzeł ${territory.name}! Węzeł generuje teraz ${territory.yieldAmountPerCycle}x ${territory.resourceType} co cykl!"
        )
    }

    fun assignGarrison(s: GameState, territoryId: String, discipleId: String?): StepResult {
        val territory = s.territories.find { it.id == territoryId } ?: return ok(s)
        val d = s.disciple(discipleId)
        return ok(
            s.copy(territories = s.territories.map {
                if (it.id == territoryId) it.copy(
                    assignedDiscipleId = discipleId,
                    garrisonDefensePower = d?.let { x -> garrisonPower(x.attack, x.comprehension) } ?: 200
                ) else it
            }),
            if (d != null) "Obsadzono posterunek ${territory.name} adeptem ${d.name}!" else "Wycofano garnizon z ${territory.name}."
        )
    }

    const val WORLD_BOSS_FLAG = "world_boss_day"
    const val WORLD_BOSS_KILLS = "world_boss_kills"

    /**
     * Etap 15: Bestia Świata – jeden atak dziennie, pokonana zostawia nagrodę (kamienie, reputacja, artefakt)
     * i po chwili wraca silniejsza (każde pokonanie ×1,5 HP).
     */
    fun attackWorldBoss(s: GameState, ctx: EngineContext, discipleId: String): StepResult {
        val d = s.disciple(discipleId) ?: return ok(s)
        if ((s.flags[WORLD_BOSS_FLAG] ?: -1) == s.currentDay) return fail(s, "Bestia Świata odpoczywa – następny atak jutro.")
        val boss = s.worldBoss
        val damage = Power.attack(s, d) * 12L + ctx.rng("world-boss").nextLong(2000, 5000)
        val stones = (damage / 25).toInt()
        val left = (boss.currentHp - damage).coerceAtLeast(0)
        val base = s.copy(flags = s.flags + (WORLD_BOSS_FLAG to s.currentDay), spiritStones = s.spiritStones + stones)
        if (left > 0) return ok(
            base.copy(worldBoss = boss.copy(currentHp = left, playerContributionDamage = boss.playerContributionDamage + damage)),
            "⚔️ ${d.name} zadaje $damage obrażeń ${boss.name}! (+$stones kamieni; zostało ${left * 100 / boss.maxHp.coerceAtLeast(1)}% HP)"
        )
        val kills = (s.flags[WORLD_BOSS_KILLS] ?: 0) + 1
        val loot = Loot.artifactFor(3 + kills * 2, ctx.rng("world-boss-loot"))
        val reward = 5_000 * kills
        val next = boss.copy(maxHp = (boss.maxHp * 1.5).toLong(), currentHp = (boss.maxHp * 1.5).toLong(), playerContributionDamage = 0)
        return ok(
            base.copy(
                worldBoss = next,
                flags = base.flags + (WORLD_BOSS_KILLS to kills),
                spiritStones = base.spiritStones + reward,
                sectReputation = base.sectReputation + 200 * kills,
                vaultArtifacts = base.vaultArtifacts + loot
            ),
            "🐉 ${d.name} zadaje ostatni cios – ${boss.name} pada! Nagroda: +$reward kamieni, +${200 * kills} reputacji, ${loot.name}. Bestia powróci silniejsza."
        )
    }

    const val ARENA_FLAG = "arena_day"

    /** Etap 15: Niebiańska Arena – jedna walka dziennie z czempionem sekty o sile zależnej od jej rangi (było: zawsze wygrana). */
    fun arenaDuel(s: GameState, ctx: EngineContext, discipleId: String, rivalSectName: String): StepResult {
        val d = s.disciple(discipleId) ?: return ok(s)
        if ((s.flags[ARENA_FLAG] ?: -1) == s.currentDay) return fail(s, "Arena jest zamknięta do jutra – jedna walka dziennie.")
        val rival = s.rivalSects.find { it.name == rivalSectName }
        val champ = ((rival?.strength ?: 5_000) / 4).coerceAtLeast(800)
        val res = Skirmish.resolve(Power.attack(s, d), d.defense, d.maxHp, champ * 3, champ / 6, champ / 12, ctx.rng("arena"))
        val win = res.win
        val marked = s.copy(flags = s.flags + (ARENA_FLAG to s.currentDay))
        return if (win) ok(
            marked.copy(spiritStones = marked.spiritStones + 200 + champ / 5, sectReputation = marked.sectReputation + 35),
            "🏆 Zwycięstwo w Niebiańskiej Arenie! ${d.name} pokonał czempiona sekty $rivalSectName! (+200 Kamieni, +35 Reputacji)"
        ) else ok(marked, "Pojedynek w Arenie przegrany – czempion $rivalSectName okazał się silniejszy.")
    }
}
