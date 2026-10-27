package com.example.game.engine

import com.example.game.WuxiaGenerator
import com.example.model.BloodlineType
import com.example.model.CultivationRealm
import com.example.model.MartialSchool

/** Dostęp do losowości dla jednego kroku silnika. */
class EngineContext(private val worldSeed: Long, private val step: Long) {
    fun rng(stream: String): GameRng = GameRng.stream(worldSeed, step, stream)
}

internal fun ok(state: GameState, message: String? = null, vararg extra: GameEvent): StepResult =
    StepResult(state, listOfNotNull(message?.let { GameEvent.Notice(it) }) + extra)

internal fun fail(state: GameState, message: String): StepResult = StepResult(state, listOf(GameEvent.Notice(message)))

/**
 * Silnik gry w czystym Kotlinie: bez Androida, bez globalnej losowości, bez zegara.
 * Ten sam stan + ta sama komenda = zawsze ten sam wynik.
 */
object GameEngine {

    /** Kolejność faz cyklu gry. Nowy system cykliczny = nowa faza dopisana tutaj. */
    val TICK_PHASES: List<TickPhase> = listOf(
        TimePhase,
        IncomePhase,
        DiscipleWorkPhase,
        RandomEventPhase,
        AutoBreakthroughPhase
    )

    fun newGame(worldSeed: Long, nowMs: Long = 0L): GameState {
        val rng = GameRng.stream(worldSeed, 0L, "new-game")
        fun named(realm: CultivationRealm, blood: BloodlineType, name: String, school: MartialSchool) =
            WuxiaGenerator.generateDisciple(rng, realm, blood).copy(name = name, martialSchool = school)

        val disciples = listOf(
            named(CultivationRealm.FOUNDATION_EARLY, BloodlineType.GOLDEN_DRAGON, "Li Long (Starszy Uczeń)", MartialSchool.SWORD_IMMORTAL),
            named(CultivationRealm.QI_CONDENSATION_9, BloodlineType.ANCIENT_PHOENIX, "Yue Qing (Adepta Ognia)", MartialSchool.ELEMENTAL_TAOIST),
            named(CultivationRealm.QI_CONDENSATION_5, BloodlineType.TITANIC_ASURA, "Wu Zhen (Pięściarz Asury)", MartialSchool.BODY_ASURA),
            named(CultivationRealm.QI_CONDENSATION_5, BloodlineType.HEAVENLY_NETHER_VIPER, "Ye Mo (Cień Otchłani)", MartialSchool.SHADOW_VIPER),
            named(CultivationRealm.FOUNDATION_LATE, BloodlineType.IMMORTAL_LOTUS_HEART, "Lin Xue (Szermierka Lotosu)", MartialSchool.SWORD_IMMORTAL),
            named(CultivationRealm.FOUNDATION_EARLY, BloodlineType.PURPLE_LIGHTNING_QILIN, "Bai Chen (Władca Pioruna)", MartialSchool.ELEMENTAL_TAOIST)
        )
        val flames = WuxiaGenerator.getGeocentricFlames()
        val beasts = WuxiaGenerator.getInitialSectBeasts()
        return GameState(
            worldSeed = worldSeed,
            step = 1L,
            spiritStones = 1200,
            disciples = disciples,
            availableRecruits = SectCommands.recruitPool(rng),
            inventory = STARTING_INVENTORY,
            facilities = WuxiaGenerator.getInitialFacilities(),
            regions = WuxiaGenerator.getInitialRegions(),
            rivalSects = WuxiaGenerator.getInitialRivalSects(),
            territories = WuxiaGenerator.getInitialTerritories(),
            flames = flames,
            activeFlameId = flames.firstOrNull()?.id,
            sectBeasts = beasts,
            activeBeastId = beasts.firstOrNull()?.id,
            lastActiveAtMs = nowMs
        )
    }

    /** Jedyne wejście do zmiany stanu gry. */
    fun apply(state: GameState, command: GameCommand): StepResult {
        val ctx = EngineContext(state.worldSeed, state.step)
        val result = when (command) {
            GameCommand.Tick -> tick(state, ctx)

            is GameCommand.Recruit -> SectCommands.recruit(state, command.discipleId)
            GameCommand.RefreshRecruits -> SectCommands.refreshRecruits(state, ctx)
            GameCommand.RecruitAll -> SectCommands.recruitAll(state)
            is GameCommand.AssignRole -> SectCommands.assignRole(state, command.discipleId, command.role)
            is GameCommand.UnlockSkill -> SectCommands.unlockSkill(state, command.discipleId, command.skillId)
            is GameCommand.UsePill -> SectCommands.usePill(state, command.discipleId, command.pillName)
            is GameCommand.ToggleAutoBreakthrough -> SectCommands.toggleAutoBreakthrough(state, command.enabled)
            is GameCommand.ToggleAutoPills -> SectCommands.toggleAutoPills(state, command.enabled)
            GameCommand.BreakthroughAll -> SectCommands.breakthroughAll(state, ctx)
            is GameCommand.AttemptBreakthrough -> SectCommands.attemptBreakthrough(state, ctx, command.discipleId, command.usePill)
            is GameCommand.ToggleDecree -> SectCommands.toggleDecree(state, command.decreeId)
            GameCommand.UpgradePrestige -> SectCommands.upgradePrestige(state)
            is GameCommand.ChangePolicy -> SectCommands.changePolicy(state, command.policy)
            is GameCommand.SetActiveBeast -> SectCommands.setActiveBeast(state, command.beastId)
            is GameCommand.FeedBeast -> SectCommands.feedBeast(state, command.beastId, command.herbName)
            is GameCommand.EvolveBeast -> SectCommands.evolveBeast(state, command.beastId)

            is GameCommand.BrewPill -> CraftCommands.brewPill(state, command.recipeId)
            is GameCommand.ForgeArtifact -> CraftCommands.forgeArtifact(state, command.recipeId)
            is GameCommand.EquipArtifact -> CraftCommands.equipArtifact(state, command.discipleId, command.artifact)
            is GameCommand.SelectFlame -> CraftCommands.selectFlame(state, command.flameId)
            is GameCommand.RefineFlame -> CraftCommands.refineFlame(state, command.flameId)
            is GameCommand.UpgradeFacility -> CraftCommands.upgradeFacility(state, command.type)

            is GameCommand.StartExpedition -> CombatSystem.start(state, command.regionId, command.discipleId)
            GameCommand.CombatStep -> CombatSystem.step(state, ctx)
            GameCommand.CombatManualAttack -> CombatSystem.manualAttack(state, ctx)
            GameCommand.CombatUsePill -> CombatSystem.usePill(state)
            GameCommand.CombatUltimate -> CombatSystem.ultimate(state, ctx)
            GameCommand.CloseCombat -> ok(state.copy(combat = null))

            is GameCommand.InteractRivalSect -> WorldCommands.interactRivalSect(state, command.sectId, command.action)
            is GameCommand.CaptureTerritory -> WorldCommands.captureTerritory(state, ctx, command.territoryId, command.discipleId)
            is GameCommand.AssignGarrison -> WorldCommands.assignGarrison(state, command.territoryId, command.discipleId)
            is GameCommand.AttackWorldBoss -> WorldCommands.attackWorldBoss(state, ctx, command.discipleId)
            is GameCommand.ArenaDuel -> WorldCommands.arenaDuel(state, ctx, command.discipleId, command.rivalSectName)
            is GameCommand.EventChoice -> WorldCommands.eventChoice(state, command.choiceIndex)
            GameCommand.DismissEvent -> ok(state.copy(activeEventId = null))

            is GameCommand.DebugAddStones -> ok(state.copy(spiritStones = state.spiritStones + command.amount), "[debug] +${command.amount} kamieni")
            is GameCommand.DebugAddItem -> ok(
                state.copy(inventory = state.inventory + (command.item to (state.inventory[command.item] ?: 0) + command.amount)),
                "[debug] +${command.amount}× ${command.item}"
            )
            GameCommand.DebugForceEvent -> {
                val pool = GameContent.events.filter { it.id !in state.recentEventIds }
                val picked = pool.randomOrNull(ctx.rng("debug")) ?: GameContent.events.first()
                ok(state.copy(activeEventId = picked.id, recentEventIds = (state.recentEventIds + picked.id).takeLast(RandomEventPhase.MEMORY)))
            }
            GameCommand.DebugFillQi -> ok(state.copy(disciples = state.disciples.map { it.copy(realmProgress = 100) }), "[debug] Qi wszystkich uczniów = 100%")
        }
        return result.copy(state = result.state.copy(step = state.step + 1))
    }

    private fun tick(state: GameState, ctx: EngineContext): StepResult {
        val frame = TickFrame(state, ctx)
        TICK_PHASES.forEach { it.run(frame) }
        return frame.toResult()
    }

    val STARTING_INVENTORY: Map<String, Int> = mapOf(
        "Trawa Wiecznej Wiosny" to 35,
        "Śnieżny Lotos Oświecenia" to 25,
        "Fioletowy Korzeń Skorpiona" to 20,
        "Płomienny Żeń-szeń Smoka" to 18,
        "Kwiat Niebiańskiego Zrozumienia" to 15,
        "Duchowy Nefryt" to 30,
        "Gwiezdne Żelazo" to 24,
        GameContent.FLAME_CRYSTAL to 18,
        "Starożytna Ruda Pustki" to 12,
        GameContent.BREAKTHROUGH_PILL to 10
    )
}
