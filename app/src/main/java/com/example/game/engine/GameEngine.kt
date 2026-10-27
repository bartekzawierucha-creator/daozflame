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
        UpkeepPhase,
        SectLifePhase,
        CultivationDailyPhase,
        EconomyDailyPhase,
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
        val allFlames = WuxiaGenerator.getGeocentricFlames()
        // Etap 4: nowa sekta zaczyna z jednym płomieniem – resztę trzeba zdobyć.
        val flames = if (com.example.game.FeatureFlags.ECONOMY_V2) allFlames.filter { it.id == Flames.STARTING_FLAME } else allFlames
        val leader = WuxiaGenerator.getInitialSectLeader().let { it.copy(activeDecrees = EdictCatalog.asDecrees(it.activeDecrees)) }
        val beasts = WuxiaGenerator.getInitialSectBeasts()
        val created = GameState(
            worldSeed = worldSeed,
            step = 1L,
            spiritStones = 1200,
            disciples = disciples,
            availableRecruits = SectCommands.recruitPool(rng),
            sectLeader = leader,
            inventory = STARTING_INVENTORY,
            facilities = WuxiaGenerator.getInitialFacilities(),
            regions = WuxiaGenerator.getInitialRegions(),
            rivalSects = WuxiaGenerator.getInitialRivalSects(),
            territories = WuxiaGenerator.getInitialTerritories(),
            flames = flames,
            activeFlameId = flames.firstOrNull()?.id,
            sectBeasts = beasts,
            activeBeastId = beasts.firstOrNull()?.id,
            lastActiveAtMs = nowMs,
            tutorialStep = 0,
            knownRecipeIds = if (com.example.game.FeatureFlags.ECONOMY_V2) CraftV2.startingRecipes() else emptySet(),
            fields = if (com.example.game.FeatureFlags.ECONOMY_V2) Fields.empty(Fields.BASE_SIZE) else emptyList()
        )
        return normalize(created)
    }

    /** Wywoływane po wczytaniu zapisu: jednorazowe migracje logiczne (korzenie w Etapie 2, etapy w Etapie 3). */
    fun normalize(state: GameState): GameState = Autonomy.normalize(Lifecycle.normalize(Secrets.normalize(Fog.normalize(WorldAI.normalize(economy(Realms.normalize(DiscipleRules.normalize(state))))))))

    /** Migracja do Etapu 4: stare zapisy zachowują wszystkie płomienie i znają wszystkie receptury. */
    private fun economy(s: GameState): GameState {
        if (s.e4Initialized || !com.example.game.FeatureFlags.ECONOMY_V2) return s
        return s.copy(
            e4Initialized = true,
            knownRecipeIds = s.knownRecipeIds.ifEmpty { GameContent.alchemyRecipes.map { it.id }.toSet() },
            fields = s.fields.ifEmpty { Fields.empty(Fields.BASE_SIZE) },
            contractsRefreshDay = s.currentDay
        )
    }

    /** Przygotowanie nowego kandydata: korzeń, cechy (Etap 2) i etap kultywacji (Etap 3). */
    fun prepareCandidate(d: com.example.model.Disciple, rng: kotlin.random.Random, state: GameState): com.example.model.Disciple {
        var x = if (state.e2Initialized) DiscipleRules.enrich(d, rng, candidate = true) else d
        if (state.e3Initialized) x = Realms.upgrade(x)
        return x
    }

    /** Jedyne wejście do zmiany stanu gry. */
    fun apply(state: GameState, command: GameCommand): StepResult {
        val ctx = EngineContext(state.worldSeed, state.step)
        val result = Autonomy.afterDirect(command, state, dispatch(state, command, ctx))
        // Przyjęta rada liczy się jak wykonana komenda (samouczek, cele dnia, osiągnięcia).
        val effective = if (command is GameCommand.Advise && result.events.any { it is GameEvent.AdviceOutcome && it.accepted }) command.command else command
        val advanced = Tutorial.afterCommand(state, effective, result.state)
        // Etap 8: liczniki, cele dzienne i osiągnięcia – liczone ze zdarzeń tego kroku.
        val withMeta = Achievements.after(effective, state, result.copy(state = advanced))
        return withMeta.copy(state = withMeta.state.copy(step = state.step + 1))
    }

    private fun dispatch(state: GameState, command: GameCommand, ctx: EngineContext): StepResult =
        when (command) {
            GameCommand.Tick -> tick(state, ctx)

            is GameCommand.Recruit -> SectCommands.recruit(state, command.discipleId)
            GameCommand.RefreshRecruits -> SectCommands.refreshRecruits(state, ctx)
            GameCommand.RecruitAll -> SectCommands.recruitAll(state)
            is GameCommand.AssignRole -> SectCommands.assignRole(state, command.discipleId, command.role)
            is GameCommand.UnlockSkill -> SectCommands.unlockSkill(state, command.discipleId, command.skillId)
            is GameCommand.UsePill -> SectCommands.usePill(state, command.discipleId, command.pillName)
            is GameCommand.Promote -> LifeCommands.promote(state, command.discipleId)
            is GameCommand.Punish -> LifeCommands.punish(state, command.discipleId)
            is GameCommand.Pardon -> LifeCommands.pardon(state, command.discipleId)
            is GameCommand.TestCandidate -> LifeCommands.testCandidate(state, command.candidateId)
            is GameCommand.BuyFood -> LifeCommands.buyFood(state, command.rations)
            GameCommand.HoldFeast -> LifeCommands.holdFeast(state)
            GameCommand.UpgradeVein -> Vein.startUpgrade(state)
            is GameCommand.PlantHerb -> Fields.plant(state, command.plotIndex, command.herb)
            GameCommand.HarvestFields -> Fields.harvestAll(state, ctx)
            GameCommand.ExpandFields -> Fields.expand(state)
            is GameCommand.Sell -> Market.sell(state, command.item, command.quantity)
            is GameCommand.Buy -> Market.buy(state, command.item, command.quantity)
            is GameCommand.SendCaravan -> Market.sendCaravan(state, ctx, command.item, command.quantity, command.guardId)
            is GameCommand.BuyBlackMarket -> Market.buyBlack(state, ctx, command.offerIndex)
            is GameCommand.FulfillContract -> Contracts.fulfill(state, command.contractId)
            is GameCommand.Experiment -> CraftV2.experiment(state, ctx, command.mainHerb, command.secondHerb)
            is GameCommand.UpgradeArtifact -> CraftV2.upgrade(state, ctx, command.vaultIndex)
            is GameCommand.CraftTalisman -> Talismans.craft(state, command.name)
            is GameCommand.HealWithTalisman -> Talismans.heal(state, command.discipleId)
            is GameCommand.CombatTalisman -> CombatSystem.withForm(CombatSystem.talisman(state, command.name), 'T')
            is GameCommand.BeginExpedition -> Expeditions.begin(state, command.regionId, command.discipleId, command.auto, ctx)
            is GameCommand.ChooseNode -> Expeditions.choose(state, command.index, ctx)
            GameCommand.EnterLair -> Expeditions.toBoss(state)
            GameCommand.RetreatExpedition -> if (state.expedition?.atBoss == true) fail(state, "Walka trwa.") else Expeditions.retreat(state)
            GameCommand.CraftRing -> Expeditions.craftRing(state)
            is GameCommand.ClimbTower -> Tower.climb(state, command.discipleId, ctx)
            is GameCommand.GuardGate -> Raids.assign(state, command.gate, command.discipleId)
            is GameCommand.ScoutRegion -> Fog.scout(state, ctx, command.regionId, command.discipleId)
            is GameCommand.OpenMeridian -> Meridians.open(state, command.discipleId, command.point)
            is GameCommand.SetBreath -> Breath.set(state, command.discipleId, command.breath, command.place)
            is GameCommand.DecipherScroll -> Breath.decipher(state, command.methodId)
            is GameCommand.Contemplate -> Contemplation.contemplate(state, command.discipleId, command.objectName)
            is GameCommand.SetResearcher -> SecretTools.setResearcher(state, command.discipleId)
            is GameCommand.UseMirror -> SecretTools.mirror(state, command.discipleId)
            is GameCommand.AssignMentor -> Lifecycle.assignMentor(state, command.studentId, command.mentorId)
            is GameCommand.BindCompanions -> Lifecycle.bind(state, command.a, command.b)
            is GameCommand.ChoosePatron -> Identity.choosePatron(state, command.patronId)
            GameCommand.UpgradeTemple -> Identity.upgradeTemple(state)
            is GameCommand.PlaceTreasure -> Identity.placeTreasure(state, command.vaultIndex)
            is GameCommand.RemoveTreasure -> Identity.removeTreasure(state, command.index)
            is GameCommand.SwapBuildings -> FengShui.swap(state, command.a, command.b)
            is GameCommand.DelveDungeon -> Dungeon.delve(state, ctx, command.discipleId)
            is GameCommand.CopyScroll -> Library.copy(state, command.scroll)
            GameCommand.UpgradeCave -> Library.upgradeCave(state)
            GameCommand.IncubateEgg -> Creatures.incubate(state, ctx)
            is GameCommand.MountBeast -> Creatures.mount(state, command.beastId, command.discipleId)
            is GameCommand.FuseFlames -> Creatures.fuse(state, ctx, command.a, command.b, command.confirmed, command.confirmedTwice)
            is GameCommand.DevourFlame -> Creatures.devour(state, command.eater, command.food)
            is GameCommand.AssignArt -> DailyLife.assignArt(state, command.discipleId, command.art)
            is GameCommand.Cure -> DailyLife.cure(state, command.discipleId)
            is GameCommand.Cleanse -> DailyLife.cleanse(state, command.discipleId)
            GameCommand.ReadStars -> DailyLife.readStars(state, ctx)
            is GameCommand.Rebirth -> Rebirth.rebirth(state, command.confirmed, command.difficulty, command.ironWill)
            is GameCommand.BuyHeavenNode -> HeavenTree.buy(state, command.nodeId)
            is GameCommand.ChooseEra -> EraModifiers.choose(state, command.eraId)
            is GameCommand.Succession -> Rebirth.succession(state, ctx, command.discipleId, command.confirmed)
            is GameCommand.SetTitle -> Achievements.setTitle(state, command.achievementId)
            is GameCommand.RealTime -> DailyGoals.realTime(state, command.nowMs)
            GameCommand.OfferToAncestors -> DailyGoals.offering(state)
            is GameCommand.PvpDuel -> DaoNet.applyResult(state, command.opponent, command.seed)
            GameCommand.DebugRevealSecrets -> state.rules?.let { r ->
                ok(Secrets.allIds(r).fold(state) { st, id -> Journal.reveal(st, id, "[debug]") }, "[debug] Odsłonięto wszystkie tajemnice")
            } ?: ok(state)
            is GameCommand.EnterSecretRealm -> SecretRealms.enter(state, ctx, command.realmId, command.discipleId)
            is GameCommand.PlaceFormationNode -> Formation.place(state, command.element)
            is GameCommand.SendSpy -> Spies.send(state, ctx, command.discipleId, command.sectId, command.type)
            is GameCommand.SectBattle -> Wars.battle(state, ctx, command.sectId, command.discipleIds)
            is GameCommand.HonorDuel -> Wars.duel(state, ctx, command.sectId, command.discipleId, command.toDeath, command.confirmed)
            is GameCommand.FlameToForge -> Flames.toForge(state, command.flameId)
            is GameCommand.FlameToDisciple -> Flames.toDisciple(state, command.flameId, command.discipleId)
            is GameCommand.ChoosePath -> CultivationCommands.choosePath(state, command.discipleId, command.path)
            is GameCommand.LearnMethod -> CultivationCommands.learnMethod(state, command.discipleId, command.scrollItem)
            is GameCommand.ChooseLaw -> CultivationCommands.chooseLaw(state, command.discipleId, command.law)
            is GameCommand.Seclude -> CultivationCommands.seclude(state, command.discipleId, command.days)
            is GameCommand.EndSeclusion -> CultivationCommands.endSeclusion(state, command.discipleId)
            is GameCommand.AwakenBloodline -> CultivationCommands.awakenBloodline(state, command.discipleId)
            is GameCommand.Ascend -> CultivationCommands.ascend(state, command.discipleId, ctx)
            is GameCommand.ToggleAutoBreakthrough -> SectCommands.toggleAutoBreakthrough(state, command.enabled)
            is GameCommand.ToggleAutoPills -> SectCommands.toggleAutoPills(state, command.enabled)
            GameCommand.BreakthroughAll -> SectCommands.breakthroughAll(state, ctx)
            is GameCommand.AttemptBreakthrough -> SectCommands.attemptBreakthrough(state, ctx, command.discipleId, command.usePill)
            is GameCommand.ToggleDecree -> SectCommands.toggleDecree(state, command.decreeId)
            GameCommand.UpgradePrestige -> SectCommands.upgradePrestige(state)
            is GameCommand.ChangePolicy -> SectCommands.changePolicy(state, command.policy)
            is GameCommand.SetActiveBeast -> SectCommands.setActiveBeast(state, command.beastId)
            is GameCommand.FeedBeast -> SectCommands.feedBeast(state, command.beastId, command.herbName)
            is GameCommand.EvolveBeast ->
                if (state.e7Initialized && (state.sectBeasts.find { it.id == command.beastId }?.tier ?: 0) >= Creatures.MAX_TIER)
                    fail(state, "Bestia osiągnęła ostatni, trzeci etap ewolucji.")
                else SectCommands.evolveBeast(state, command.beastId)

            is GameCommand.BrewPill -> if (state.e4Initialized) CraftV2.brew(state, ctx, command.recipeId) else CraftCommands.brewPill(state, command.recipeId)
            is GameCommand.ForgeArtifact -> if (state.e4Initialized) CraftV2.forge(state, ctx, command.recipeId) else CraftCommands.forgeArtifact(state, command.recipeId)
            is GameCommand.EquipArtifact -> CraftCommands.equipArtifact(state, command.discipleId, command.artifact)
            is GameCommand.SelectFlame -> if (state.e4Initialized) Flames.toCauldron(state, command.flameId) else CraftCommands.selectFlame(state, command.flameId)
            is GameCommand.RefineFlame -> {
                val f = state.flames.find { it.id == command.flameId }
                if (state.e4Initialized && f != null && f.refinementLevel >= Flames.MAX_REFINEMENT) fail(state, "${f.name} osiągnął najwyższy, ${Flames.MAX_REFINEMENT}. poziom rafinacji.")
                else CraftCommands.refineFlame(state, command.flameId)
            }
            is GameCommand.UpgradeFacility -> CraftCommands.upgradeFacility(state, command.type)

            is GameCommand.StartExpedition -> CombatSystem.start(state, command.regionId, command.discipleId)
            GameCommand.CombatStep -> CombatSystem.step(state, ctx)
            GameCommand.CombatManualAttack -> CombatSystem.withForm(CombatSystem.manualAttack(state, ctx), 'M')
            GameCommand.CombatUsePill -> CombatSystem.withForm(CombatSystem.usePill(state), 'P')
            GameCommand.CombatUltimate -> CombatSystem.withForm(CombatSystem.ultimate(state, ctx), 'U')
            GameCommand.CloseCombat -> ok(state.copy(combat = null))

            is GameCommand.InteractRivalSect -> WorldCommands.interactRivalSect(state, command.sectId, command.action)
            is GameCommand.CaptureTerritory -> WorldCommands.captureTerritory(state, ctx, command.territoryId, command.discipleId)
            is GameCommand.AssignGarrison -> WorldCommands.assignGarrison(state, command.territoryId, command.discipleId)
            is GameCommand.AttackWorldBoss -> WorldCommands.attackWorldBoss(state, ctx, command.discipleId)
            is GameCommand.ArenaDuel -> WorldCommands.arenaDuel(state, ctx, command.discipleId, command.rivalSectName)
            is GameCommand.EventChoice -> EventScripts.choose(state, command.choiceIndex)
            // Zdarzeń planowanych (fabuła, łańcuchy) nie da się zgubić – odłożone wracają następnego dnia.
            GameCommand.DismissEvent -> GameContent.script(state.activeEventId)?.takeIf { !it.random }?.let { e ->
                ok(state.copy(activeEventId = null, scheduledEvents = state.scheduledEvents + com.example.model.ScheduledEvent(state.currentDay + 1, e.id)), "Odłożono: ${e.title}.")
            } ?: ok(state.copy(activeEventId = null))

            is GameCommand.OfflineCatchUp -> OfflineProgress.apply(state, command.elapsedMs, ctx)
            GameCommand.TutorialNext -> ok(state.copy(tutorialStep = Tutorial.next(state.tutorialStep)))
            GameCommand.TutorialSkip -> ok(state.copy(tutorialStep = Tutorial.DONE), "Samouczek pominięty. Duch Założyciela czuwa w milczeniu.")

            is GameCommand.DebugAddStones -> ok(state.copy(spiritStones = state.spiritStones + command.amount), "[debug] +${command.amount} kamieni")
            is GameCommand.DebugAddItem -> ok(
                state.copy(inventory = state.inventory + (command.item to (state.inventory[command.item] ?: 0) + command.amount)),
                "[debug] +${command.amount}× ${command.item}"
            )
            GameCommand.DebugForceEvent -> {
                val pool = GameContent.scripts.filter { it.random && it.id !in state.recentEventIds }
                val picked = pool.randomOrNull(ctx.rng("debug")) ?: GameContent.scripts.first()
                ok(EventScripts.activate(state, picked))
            }
            GameCommand.DebugFillQi -> ok(state.copy(disciples = state.disciples.map { it.copy(realmProgress = 100) }), "[debug] Qi wszystkich uczniów = 100%")

            is GameCommand.Advise -> if (!state.autonomy) dispatch(state, command.command, ctx)
                else Autonomy.advise(state, ctx, command.command) { st, c -> Autonomy.afterDirect(c, st, dispatch(st, c, ctx)) }
            is GameCommand.Hold -> Autonomy.hold(state, command.discipleId)
        }

    private fun tick(state: GameState, ctx: EngineContext): StepResult {
        val frame = TickFrame(state, ctx)
        TICK_PHASES.forEach { it.run(frame) }
        val result = frame.toResult()
        if (result.state.currentDay == state.currentDay) return result
        // Systemy dobowe na pełnym stanie (Etap 5b: AI sekt, najazdy, szpiedzy) – po fazach cyklu, raz na dzień.
        val (world, notes) = WorldDaily.run(result.state, ctx)
        // Etap 10: uczniowie sami decydują o dniu.
        val (lived, lives) = Autonomy.daily(world, ctx)
        return StepResult(lived, result.events + notes.map { GameEvent.Notice(it) } + lives)
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
