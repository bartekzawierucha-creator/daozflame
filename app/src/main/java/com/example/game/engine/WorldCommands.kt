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

    fun attackWorldBoss(s: GameState, ctx: EngineContext, discipleId: String): StepResult {
        val d = s.disciple(discipleId) ?: return ok(s)
        val damage = d.attack * 12L + ctx.rng("world-boss").nextLong(2000, 5000)
        val boss = s.worldBoss
        val stones = (damage / 25).toInt()
        return ok(
            s.copy(
                worldBoss = boss.copy(
                    currentHp = (boss.currentHp - damage).coerceAtLeast(0),
                    playerContributionDamage = boss.playerContributionDamage + damage
                ),
                spiritStones = s.spiritStones + stones
            ),
            "⚔️ Twój czempion ${d.name} zadał $damage obrażeń Władcy Próżni! Zdobyto $stones Kamieni Ducha!"
        )
    }

    fun arenaDuel(s: GameState, ctx: EngineContext, discipleId: String, rivalSectName: String): StepResult {
        val d = s.disciple(discipleId) ?: return ok(s)
        val win = d.attack > 60 || ctx.rng("arena").nextBoolean()
        return if (win) ok(
            s.copy(spiritStones = s.spiritStones + 200, sectReputation = s.sectReputation + 35),
            "🏆 Zwycięstwo w Niebiańskiej Arenie! ${d.name} pokonał czempiona sekty $rivalSectName! (+200 Kamieni, +35 Reputacji)"
        ) else fail(s, "Pojedynek w Arenie zakończony remisem ze wskazaniem na $rivalSectName.")
    }

    fun eventChoice(s: GameState, index: Int): StepResult {
        val event = GameContent.event(s.activeEventId) ?: return ok(s.copy(activeEventId = null))
        val choice = event.choices.getOrNull(index) ?: return ok(s)
        val inv = choice.rewardItem?.let { s.inventory + (it to (s.inventory[it] ?: 0) + 2) } ?: s.inventory
        return ok(
            s.copy(
                activeEventId = null,
                spiritStones = (s.spiritStones + choice.stoneChange).coerceAtLeast(0),
                sectReputation = s.sectReputation +
                    if (choice.repChange > 0) (choice.repChange * Modifiers.sect(s).factor(Stat.REPUTATION_GAIN)).toInt() else choice.repChange,
                inventory = inv
            ),
            choice.outcomeDescription
        )
    }
}
