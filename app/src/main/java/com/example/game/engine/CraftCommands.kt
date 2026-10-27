package com.example.game.engine

import com.example.model.Artifact
import com.example.model.FacilityType

/** Alchemia, kuźnia, płomienie i rozbudowa obiektów. */
object CraftCommands {

    fun brewPill(s: GameState, recipeId: String): StepResult {
        val recipe = GameContent.recipe(recipeId) ?: return ok(s)
        val chamberLevel = s.facilities.find { it.type == FacilityType.ALCHEMY_CHAMBER }?.level ?: 1
        if (chamberLevel < recipe.requiredAlchemyChamberLevel) {
            return fail(s, "Zbyt niski poziom Sali Alchemii! Wymagany: ${recipe.requiredAlchemyChamberLevel} (Obecny: $chamberLevel). Rozbuduj Salę Alchemii w Kampusie!")
        }
        val herbCount = s.inventory[recipe.requiredHerbName] ?: 0
        if (herbCount < recipe.requiredHerbCount) {
            return fail(s, "Brak składnika głównego: ${recipe.requiredHerbName} (${recipe.requiredHerbCount} wymagane)!")
        }
        val second = recipe.secondaryHerbName
        if (second != null && recipe.secondaryHerbCount > 0 && (s.inventory[second] ?: 0) < recipe.secondaryHerbCount) {
            return fail(s, "Brak składnika pomocniczego: $second (${recipe.secondaryHerbCount} wymagane)!")
        }
        if (s.spiritStones < recipe.requiredSpiritStones) {
            return fail(s, "Brak Kamieni Ducha (${recipe.requiredSpiritStones} wymagane)!")
        }
        val inv = s.inventory.toMutableMap()
        inv[recipe.requiredHerbName] = herbCount - recipe.requiredHerbCount
        if (second != null && recipe.secondaryHerbCount > 0) {
            inv[second] = ((inv[second] ?: 0) - recipe.secondaryHerbCount).coerceAtLeast(0)
        }
        inv[recipe.pillName] = (inv[recipe.pillName] ?: 0) + 1
        val flame = s.activeFlame
        return ok(
            s.copy(
                spiritStones = s.spiritStones - recipe.requiredSpiritStones,
                inventory = inv.toMap(),
                sectReputation = s.sectReputation + recipe.tier * 6
            ),
            "Uwarzono [${recipe.tierName}]: ${recipe.pillName}! Płomień: ${flame?.name ?: "Zwykły"} (+${flame?.bonusCraftingRate ?: 10}% Sukcesu)"
        )
    }

    fun forgeArtifact(s: GameState, recipeId: String): StepResult {
        val recipe = GameContent.forging(recipeId) ?: return ok(s)
        val count = s.inventory[recipe.requiredMineralName] ?: 0
        if (count < recipe.requiredMineralCount) {
            return fail(s, "Brak minerału: ${recipe.requiredMineralName} (${recipe.requiredMineralCount} wymagane)!")
        }
        if (s.spiritStones < recipe.requiredSpiritStones) {
            return fail(s, "Brak Kamieni Ducha (${recipe.requiredSpiritStones} wymagane)!")
        }
        return ok(
            s.copy(
                spiritStones = s.spiritStones - recipe.requiredSpiritStones,
                inventory = s.inventory + (recipe.requiredMineralName to count - recipe.requiredMineralCount),
                vaultArtifacts = s.vaultArtifacts + recipe.artifactResult,
                sectReputation = s.sectReputation + 15
            ),
            "Wykuto boski artefakt: ${recipe.artifactResult.name} w Boskiej Kuźni!"
        )
    }

    fun equipArtifact(s: GameState, discipleId: String, artifact: Artifact): StepResult {
        val disciples = s.disciples.map { d ->
            if (d.id != discipleId) return@map d
            val prevAtk = d.equippedArtifact?.attackBonus ?: 0
            val prevDef = d.equippedArtifact?.defenseBonus ?: 0
            d.copy(
                equippedArtifact = artifact,
                attack = d.attack - prevAtk + artifact.attackBonus,
                defense = d.defense - prevDef + artifact.defenseBonus
            )
        }
        return ok(s.copy(disciples = disciples), "Wyposażono artefakt: ${artifact.name}!")
    }

    fun selectFlame(s: GameState, flameId: String): StepResult {
        val flame = s.flames.find { it.id == flameId } ?: return ok(s)
        return ok(s.copy(activeFlameId = flameId), "Wybrano Płomień Geocentryczny: ${flame.name}!")
    }

    fun refineFlame(s: GameState, flameId: String): StepResult {
        val flame = s.flames.find { it.id == flameId } ?: return ok(s)
        val stones = flame.refinementLevel * 150
        val crystals = flame.refinementLevel / 2 + 1
        val have = s.inventory[GameContent.FLAME_CRYSTAL] ?: 0
        if (s.spiritStones < stones) return fail(s, "Brak Kamieni Ducha na uszlachetnienie płomienia ($stones wymagane)!")
        if (have < crystals) return fail(s, "Brak Kryształów Geocentrycznego Ognia ($crystals wymagane)!")
        val flames = s.flames.map {
            if (it.id == flameId) it.copy(
                refinementLevel = it.refinementLevel + 1,
                temperatureCelsius = it.temperatureCelsius + 350,
                bonusCraftingRate = it.bonusCraftingRate + 8,
                combatDamageBonus = it.combatDamageBonus + 15
            ) else it
        }
        return ok(
            s.copy(
                spiritStones = s.spiritStones - stones,
                inventory = s.inventory + (GameContent.FLAME_CRYSTAL to have - crystals),
                flames = flames
            ),
            "🔥 Uszlachetniono ${flame.name}! Poziom Uszlachetnienia: ${flame.refinementLevel + 1}, Temperatura: ${flame.temperatureCelsius + 350}°C!"
        )
    }

    fun upgradeFacility(s: GameState, type: FacilityType): StepResult {
        val facility = s.facilities.find { it.type == type } ?: return ok(s)
        if (s.spiritStones < facility.upgradeCostStones) {
            return fail(s, "Zbyt mało Kamieni Ducha na rozbudowę (${facility.upgradeCostStones} wymagane)!")
        }
        val facilities = s.facilities.map {
            if (it.type == type) it.copy(
                level = it.level + 1,
                upgradeCostStones = (it.upgradeCostStones * 1.8).toInt(),
                effectDescription = "${it.effectDescription} (Poziom ${it.level + 1})"
            ) else it
        }
        return ok(
            s.copy(spiritStones = s.spiritStones - facility.upgradeCostStones, facilities = facilities, sectReputation = s.sectReputation + 20),
            "Ukończono rozbudowę obiektu: ${facility.type.polishName} (Poz. ${facility.level + 1})!"
        )
    }
}
