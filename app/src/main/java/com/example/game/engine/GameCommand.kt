package com.example.game.engine

import com.example.model.Artifact
import com.example.model.DiscipleRole
import com.example.model.FacilityType
import com.example.model.SectPolicy

/** Każda akcja gracza i każdy krok czasu to komenda. UI nigdy nie zmienia stanu bezpośrednio. */
sealed interface GameCommand {
    /** Jeden cykl pętli gry (pora dnia, kultywacja, zbiory, zdarzenia). */
    data object Tick : GameCommand

    // Uczniowie
    data class Recruit(val discipleId: String) : GameCommand
    data object RefreshRecruits : GameCommand
    data object RecruitAll : GameCommand
    data class AssignRole(val discipleId: String, val role: DiscipleRole) : GameCommand
    data class UnlockSkill(val discipleId: String, val skillId: String) : GameCommand
    data class UsePill(val discipleId: String, val pillName: String) : GameCommand

    // Etap 2: życie sekty
    data class Promote(val discipleId: String) : GameCommand
    data class Punish(val discipleId: String) : GameCommand
    data class Pardon(val discipleId: String) : GameCommand
    data class TestCandidate(val candidateId: String) : GameCommand
    data class BuyFood(val rations: Int) : GameCommand
    data object HoldFeast : GameCommand

    // Etap 3: kultywacja
    data class ChoosePath(val discipleId: String, val path: com.example.model.CultivationPath) : GameCommand
    data class LearnMethod(val discipleId: String, val scrollItem: String) : GameCommand
    data class ChooseLaw(val discipleId: String, val law: com.example.model.DaoLaw) : GameCommand
    data class Seclude(val discipleId: String, val days: Int) : GameCommand
    data class EndSeclusion(val discipleId: String) : GameCommand
    data class AwakenBloodline(val discipleId: String) : GameCommand
    data class Ascend(val discipleId: String) : GameCommand

    // Etap 4: gospodarka i rzemiosło
    data object UpgradeVein : GameCommand
    data class PlantHerb(val plotIndex: Int, val herb: String) : GameCommand
    data object HarvestFields : GameCommand
    data object ExpandFields : GameCommand
    data class Sell(val item: String, val quantity: Int) : GameCommand
    data class Buy(val item: String, val quantity: Int) : GameCommand
    data class SendCaravan(val item: String, val quantity: Int, val guardId: String?) : GameCommand
    data class BuyBlackMarket(val offerIndex: Int) : GameCommand
    data class FulfillContract(val contractId: String) : GameCommand
    data class Experiment(val mainHerb: String, val secondHerb: String?) : GameCommand
    data class UpgradeArtifact(val vaultIndex: Int) : GameCommand
    data class CraftTalisman(val name: String) : GameCommand
    data class HealWithTalisman(val discipleId: String) : GameCommand
    data class CombatTalisman(val name: String) : GameCommand
    data class FlameToForge(val flameId: String) : GameCommand
    data class FlameToDisciple(val flameId: String, val discipleId: String) : GameCommand

    // Etap 5a
    data class BeginExpedition(val regionId: String, val discipleId: String, val auto: Boolean = false) : GameCommand
    data class ChooseNode(val index: Int) : GameCommand
    data object EnterLair : GameCommand
    data object RetreatExpedition : GameCommand
    data object CraftRing : GameCommand
    data class ClimbTower(val discipleId: String) : GameCommand

    // Etap 5b
    data class GuardGate(val gate: String, val discipleId: String?) : GameCommand
    data class PlaceFormationNode(val element: com.example.model.SpiritElement) : GameCommand
    data class SendSpy(val discipleId: String, val sectId: String, val type: com.example.model.SpyType) : GameCommand
    data class SectBattle(val sectId: String, val discipleIds: List<String>) : GameCommand
    data class HonorDuel(val sectId: String, val discipleId: String, val toDeath: Boolean, val confirmed: Boolean = false) : GameCommand

    // Kultywacja
    data class ToggleAutoBreakthrough(val enabled: Boolean? = null) : GameCommand
    data class ToggleAutoPills(val enabled: Boolean? = null) : GameCommand
    data object BreakthroughAll : GameCommand
    data class AttemptBreakthrough(val discipleId: String, val usePill: Boolean) : GameCommand

    // Rzemiosło
    data class BrewPill(val recipeId: String) : GameCommand
    data class ForgeArtifact(val recipeId: String) : GameCommand
    data class EquipArtifact(val discipleId: String, val artifact: Artifact) : GameCommand
    data class SelectFlame(val flameId: String) : GameCommand
    data class RefineFlame(val flameId: String) : GameCommand
    data class UpgradeFacility(val type: FacilityType) : GameCommand

    // Walka
    data class StartExpedition(val regionId: String, val discipleId: String) : GameCommand
    data object CombatStep : GameCommand
    data object CombatManualAttack : GameCommand
    data object CombatUsePill : GameCommand
    data object CombatUltimate : GameCommand
    data object CloseCombat : GameCommand

    // Świat
    data class InteractRivalSect(val sectId: String, val action: String) : GameCommand
    data class CaptureTerritory(val territoryId: String, val discipleId: String) : GameCommand
    data class AssignGarrison(val territoryId: String, val discipleId: String?) : GameCommand
    data class AttackWorldBoss(val discipleId: String) : GameCommand
    data class ArenaDuel(val discipleId: String, val rivalSectName: String) : GameCommand

    // Zdarzenia
    data class EventChoice(val choiceIndex: Int) : GameCommand
    data object DismissEvent : GameCommand

    // Patriarcha i sekta
    data class ToggleDecree(val decreeId: String) : GameCommand
    data object UpgradePrestige : GameCommand
    data class ChangePolicy(val policy: SectPolicy) : GameCommand
    data class SetActiveBeast(val beastId: String) : GameCommand
    data class FeedBeast(val beastId: String, val herbName: String) : GameCommand
    data class EvolveBeast(val beastId: String) : GameCommand

    // Czas i samouczek
    /** Nadrobienie czasu nieobecności (postęp offline). */
    data class OfflineCatchUp(val elapsedMs: Long) : GameCommand
    data object TutorialNext : GameCommand
    data object TutorialSkip : GameCommand

    // Narzędzia debug (panel tylko w buildzie debug)
    data class DebugAddStones(val amount: Int) : GameCommand
    data class DebugAddItem(val item: String, val amount: Int) : GameCommand
    data object DebugForceEvent : GameCommand
    data object DebugFillQi : GameCommand
}

/** Co się wydarzyło – UI zamienia to na komunikaty, dźwięki i animacje. */
sealed interface GameEvent {
    data class Notice(val message: String) : GameEvent

    enum class HitKind { SLASH, CRIT, BEAST_ATTACK, HEAL, ULTIMATE }

    data class CombatHit(
        val kind: HitKind,
        val amount: Int,
        /** true = trafiony/uleczony uczeń, false = przeciwnik. */
        val onDisciple: Boolean
    ) : GameEvent

    data class CombatEnded(val victory: Boolean) : GameEvent
    data class Breakthrough(val discipleId: String, val success: Boolean) : GameEvent
    data class Tribulation(val discipleId: String, val result: CultivationSystem.TribulationResult) : GameEvent
    data class OfflineSummary(val report: OfflineProgress.Report) : GameEvent
}

data class StepResult(val state: GameState, val events: List<GameEvent> = emptyList()) {
    val notice: String? get() = events.filterIsInstance<GameEvent.Notice>().lastOrNull()?.message
}
