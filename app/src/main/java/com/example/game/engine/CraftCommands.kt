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
        val cost = (recipe.requiredSpiritStones * Modifiers.sect(s).factor(Stat.ALCHEMY_COST)).toInt().coerceAtLeast(0)
        if (s.spiritStones < cost) {
            return fail(s, "Brak Kamieni Ducha ($cost wymagane)!")
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
                spiritStones = s.spiritStones - cost,
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
        val cost = (recipe.requiredSpiritStones * Modifiers.sect(s).factor(Stat.FORGE_COST)).toInt().coerceAtLeast(0)
        if (s.spiritStones < cost) {
            return fail(s, "Brak Kamieni Ducha ($cost wymagane)!")
        }
        return ok(
            s.copy(
                spiritStones = s.spiritStones - cost,
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

/** Etap 4: rzemiosło z jakością, temperaturą płomienia, odkrywaniem receptur i ulepszaniem. */
object CraftV2 {

    const val EXPERIMENT_COST = 30
    const val EXPERIMENT_SUCCESS = 60
    const val UPGRADE_MAX = 10
    const val PROTECTION_TALISMAN = "Talizman Ochrony"

    /** Receptury znane na starcie nowej gry: wszystkie 1. stopnia. */
    fun startingRecipes(): Set<String> = GameContent.alchemyRecipes.filter { it.tier <= 1 }.map { it.id }.toSet() +
        listOfNotNull(GameContent.recipeForPill(GameContent.BREAKTHROUGH_PILL)?.id)

    fun knows(s: GameState, recipeId: String) = s.knownRecipeIds.isEmpty() || recipeId in s.knownRecipeIds

    private fun qualityBonus(s: GameState, flame: com.example.model.GeocentricFlame?, facility: com.example.model.FacilityType): Int {
        val flameBonus = (flame?.bonusCraftingRate ?: 0) / 5
        val level = Vein.facilityLevel(s, facility) * 2
        val alchemists = if (facility == com.example.model.FacilityType.ALCHEMY_CHAMBER) s.disciples.count { "alchemist" in it.traitIds }.coerceAtMost(3) * 3 else 0
        return flameBonus + level + alchemists
    }

    fun brew(s: GameState, ctx: EngineContext, recipeId: String): StepResult {
        val recipe = GameContent.recipe(recipeId) ?: return ok(s)
        if (!knows(s, recipeId)) return fail(s, "Receptura ${recipe.pillName} jest nieznana. Odkryj ją eksperymentem.")
        val flame = s.activeFlame
        val temp = flame?.let { Flames.temperature(it) } ?: 0
        val need = Flames.alchemyTemperature(recipe.tier)
        if (temp < need) return fail(s, "${recipe.pillName} wymaga płomienia o temperaturze $need°C (kocioł: $temp°C).")
        val base = CraftCommands.brewPill(s, recipeId)
        if (base.state.spiritStones == s.spiritStones && base.state.inventory == s.inventory) return base
        // Podstawowy wynik dodał 1 Zwykłą pigułkę – zamieniamy ją na wylosowaną jakość.
        val q = Quality.roll(ctx.rng("alchemy-quality"), qualityBonus(s, flame, com.example.model.FacilityType.ALCHEMY_CHAMBER))
        val inv = base.state.inventory.toMutableMap()
        inv[recipe.pillName] = (inv[recipe.pillName] ?: 1) - 1
        val k = Quality.key(recipe.pillName, q)
        inv[k] = (inv[k] ?: 0) + 1
        return ok(base.state.copy(inventory = inv.toMap()), "Uwarzono: ${recipe.pillName} – jakość ${q.polishName}.")
    }

    fun forge(s: GameState, ctx: EngineContext, recipeId: String): StepResult {
        val recipe = GameContent.forging(recipeId) ?: return ok(s)
        val flame = s.flames.find { it.id == s.forgeFlameId } ?: s.activeFlame
        val temp = flame?.let { Flames.temperature(it) } ?: 0
        val need = Flames.forgeTemperature(recipe.artifactResult.rarity)
        if (temp < need) return fail(s, "${recipe.artifactName} wymaga paleniska $need°C (kuźnia: $temp°C).")
        val base = CraftCommands.forgeArtifact(s, recipeId)
        if (base.state.vaultArtifacts.size == s.vaultArtifacts.size) return base
        val q = Quality.roll(ctx.rng("forge-quality"), qualityBonus(s, flame, com.example.model.FacilityType.DIVINE_FORGE))
        val a = recipe.artifactResult
        val made = a.copy(
            quality = q,
            attackBonus = (a.attackBonus * q.artifactStats).toInt(),
            defenseBonus = (a.defenseBonus * q.artifactStats).toInt()
        )
        return ok(base.state.copy(vaultArtifacts = s.vaultArtifacts + made), "Wykuto: ${a.name} – jakość ${q.polishName}.")
    }

    /** Eksperyment: zioło główne (+ opcjonalnie drugie) w kotle. Trafienie w nieznaną recepturę ją odkrywa. */
    fun experiment(s: GameState, ctx: EngineContext, main: String, second: String?): StepResult {
        if ((s.inventory[main] ?: 0) < 1 || (second != null && (s.inventory[second] ?: 0) < 1)) return fail(s, "Brak składników do eksperymentu.")
        if (s.spiritStones < EXPERIMENT_COST) return fail(s, "Eksperyment kosztuje $EXPERIMENT_COST kamieni.")
        val inv = s.inventory.toMutableMap()
        inv[main] = (inv[main] ?: 0) - 1
        second?.let { inv[it] = (inv[it] ?: 0) - 1 }
        val paid = s.copy(inventory = inv.toMap(), spiritStones = s.spiritStones - EXPERIMENT_COST)
        val unknown = GameContent.alchemyRecipes.filter { !knows(s, it.id) }
        val exact = unknown.firstOrNull { it.requiredHerbName == main && it.secondaryHerbName == second }
            ?: unknown.firstOrNull { second != null && it.requiredHerbName == second && it.secondaryHerbName == main }
        val rng = ctx.rng("experiment")
        if (exact != null && rng.nextInt(100) < EXPERIMENT_SUCCESS) {
            return ok(paid.copy(knownRecipeIds = paid.knownRecipeIds + exact.id), "Odkrycie! Kocioł wydał ${exact.pillName}. Receptura zapisana w księdze.")
        }
        val near = unknown.any { it.requiredHerbName == main || it.requiredHerbName == second }
        // Etap 6: przy braku receptury dominująca cecha ziół daje esencję emergentną.
        val observed = Chemistry.observe(paid, main, second)
        val essence = if (exact == null) observed.rules?.let { Chemistry.dominant(it, main, second) } else null
        if (essence != null) {
            val name = Chemistry.ESSENCE + essence.first
            return ok(observed.copy(inventory = observed.inventory + (name to (observed.inventory[name] ?: 0) + 1)),
                "Kocioł wydaje coś nieznanego: $name. Cecha „${essence.first}” przeważyła w mieszaninie.")
        }
        return ok(
            observed,
            when {
                exact != null -> "Kocioł zabulgotał obiecująco… ale mieszanina się zważyła. Spróbuj jeszcze raz."
                near -> "Kocioł zadrżał – ten składnik pasuje do czegoś, ale czegoś brakuje."
                else -> "Czarny dym. Ta mieszanina do niczego nie prowadzi."
            }
        )
    }

    /** Szansa ulepszenia na kolejny poziom: 100% do +3, potem −10% za poziom. */
    fun upgradeChance(plus: Int): Int = if (plus < 3) 100 else (100 - 10 * (plus - 2)).coerceAtLeast(30)

    /** Ulepszenie artefaktu z magazynu (indeks) – +8% statystyk za poziom, ryzyko pęknięcia od +6. */
    fun upgrade(s: GameState, ctx: EngineContext, vaultIndex: Int): StepResult {
        val a = s.vaultArtifacts.getOrNull(vaultIndex) ?: return ok(s)
        if (a.plus >= UPGRADE_MAX) return fail(s, "${a.name} osiągnął +$UPGRADE_MAX.")
        val recipe = GameContent.forgingRecipes.firstOrNull { it.artifactResult.id == a.id }
        val mineral = recipe?.requiredMineralName ?: "Gwiezdne Żelazo"
        val mineralCount = (recipe?.requiredMineralCount ?: 2) * (a.plus + 1)
        val stones = 100 * (a.plus + 1) * (a.plus + 1)
        val have = s.inventory[mineral] ?: 0
        if (have < mineralCount) return fail(s, "Ulepszenie wymaga ${mineralCount}× $mineral (masz $have).")
        if (s.spiritStones < stones) return fail(s, "Ulepszenie kosztuje $stones kamieni.")
        val inv = (s.inventory + (mineral to have - mineralCount)).toMutableMap()
        val rng = ctx.rng("upgrade")
        val success = rng.nextInt(100) < upgradeChance(a.plus)
        val paid = s.copy(spiritStones = s.spiritStones - stones)
        if (success) {
            val f = 1.08
            val up = a.copy(plus = a.plus + 1, attackBonus = (a.attackBonus * f).toInt() + 1, defenseBonus = (a.defenseBonus * f).toInt() + 1)
            return ok(paid.copy(inventory = inv.toMap(), vaultArtifacts = s.vaultArtifacts.mapIndexed { i, x -> if (i == vaultIndex) up else x }),
                "${a.name} +${up.plus}! Statystyki rosną.")
        }
        val protected = (inv[PROTECTION_TALISMAN] ?: 0) > 0
        if (a.plus >= 6 && rng.nextInt(100) < 20) {
            if (protected) {
                inv[PROTECTION_TALISMAN] = (inv[PROTECTION_TALISMAN] ?: 0) - 1
                return ok(paid.copy(inventory = inv.toMap()), "Ulepszenie nieudane – Talizman Ochrony uratował ${a.name} przed pęknięciem.")
            }
            val down = a.copy(plus = a.plus - 1, attackBonus = (a.attackBonus / 1.08).toInt(), defenseBonus = (a.defenseBonus / 1.08).toInt())
            return ok(paid.copy(inventory = inv.toMap(), vaultArtifacts = s.vaultArtifacts.mapIndexed { i, x -> if (i == vaultIndex) down else x }),
                "${a.name} pękł! Spada do +${down.plus}.")
        }
        return ok(paid.copy(inventory = inv.toMap()), "Ulepszenie ${a.name} nieudane – materiały przepadły.")
    }
}
