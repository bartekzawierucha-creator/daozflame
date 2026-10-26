package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fireplace
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.game.WuxiaGameViewModel
import com.example.game.WuxiaGenerator
import com.example.game.WuxiaUiState
import com.example.model.AlchemyRecipe
import com.example.model.ForgingRecipe
import com.example.model.FacilityType
import com.example.model.GeocentricFlame
import com.example.model.SectDivineBeast
import com.example.ui.audio.WuxiaAudioEngine
import com.example.ui.components.WuxiaBadge
import com.example.ui.components.WuxiaCard
import com.example.ui.graphics.*
import com.example.ui.pixelart.*
import com.example.ui.theme.*

@Composable
fun CraftingScreen(
    viewModel: WuxiaGameViewModel,
    uiState: WuxiaUiState,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableStateOf(0) } // 0: Alchemia, 1: Kuźnia, 2: Płomienie, 3: Bestie, 4: Magazyn

    val alchemyRecipes = remember { WuxiaGenerator.getAlchemyRecipes() }
    val forgingRecipes = remember { WuxiaGenerator.getForgingRecipes() }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp)
    ) {
        Spacer(modifier = Modifier.height(10.dp))

        // Navigation Tabs (Imperial Lacquer Bar)
        Surface(
            color = Color(0xFF0F0918),
            shape = RoundedCornerShape(12.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFFD700)),
            modifier = Modifier.fillMaxWidth()
        ) {
            ScrollableTabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color(0xFF0F0918),
                contentColor = Color(0xFFFFD700),
                edgePadding = 6.dp,
                indicator = {}
            ) {
                listOf(
                    "⚗️ Kocioł Alchemii" to 0,
                    "🔨 Boska Kuźnia" to 1,
                    "🔥 SYSTEM PŁOMIENI" to 2,
                    "🐉 Bestie Sekty" to 3,
                    "📦 Magazyn Sekty" to 4
                ).forEach { (label, idx) ->
                    val isSel = selectedTab == idx
                    Tab(
                        selected = isSel,
                        onClick = {
                            if (selectedTab != idx) {
                                when (idx) {
                                    0 -> WuxiaAudioEngine.playCauldronFlame()
                                    1 -> WuxiaAudioEngine.playSwordClash()
                                    2 -> WuxiaAudioEngine.playCauldronFlame()
                                    3 -> WuxiaAudioEngine.playDivineGong()
                                    else -> WuxiaAudioEngine.playDivineGong()
                                }
                                selectedTab = idx
                            }
                        },
                        text = {
                            Text(
                                text = label,
                                fontSize = 11.sp,
                                fontWeight = if (isSel) FontWeight.Black else FontWeight.Medium,
                                color = if (isSel) Color(0xFFFFE082) else Color(0xFF94A3B8)
                            )
                        },
                        modifier = if (isSel) Modifier.background(Color(0xFFB45309).copy(alpha = 0.4f)) else Modifier
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        when (selectedTab) {
            0 -> AlchemyTabContent(uiState, viewModel, alchemyRecipes)
            1 -> ForgingTabContent(uiState, viewModel, forgingRecipes)
            2 -> FlamesTabContent(uiState, viewModel)
            3 -> BeastsTabContent(uiState, viewModel)
            4 -> InventoryVaultContent(uiState)
        }
    }
}

@Composable
fun AlchemyTabContent(
    uiState: WuxiaUiState,
    viewModel: WuxiaGameViewModel,
    recipes: List<AlchemyRecipe>
) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        item {
            ManhwaHeroBanner(
                assetName = "bg_alchemy_hall",
                title = "Starożytna Komnata Alchemii",
                subtitle = "Geocentryczny Płomień Serca Ziemi • Formowanie Złotego Rdzenia",
                height = 160.dp
            )
        }

        // AAA 2D Interactive Animated Cauldron View
        item {
            val breakthroughRecipe = recipes.firstOrNull { it.pillName.contains("Przełomu") } ?: recipes.firstOrNull()
            Wuxia2dCauldronInteractive(
                flameName = uiState.activeFlame?.name ?: "Płomień Serca Lawy",
                bonusRate = uiState.activeFlame?.bonusCraftingRate ?: 25,
                onBrewSuccess = {
                    if (breakthroughRecipe != null) {
                        viewModel.brewPill(breakthroughRecipe)
                    }
                }
            )
        }

        // Geocentric Flame Selector
        item {
            val flameImg = rememberWuxiaGraphic("item_geocentric_flame")
            WuxiaCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (flameImg != null) {
                        androidx.compose.foundation.Image(
                            bitmap = flameImg,
                            contentDescription = "Geocentryczny Płomień",
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                            modifier = Modifier
                                .size(50.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .border(1.5.dp, Color(0xFFFF5252), RoundedCornerShape(8.dp))
                        )
                    } else {
                        PixelSpriteView(
                            spriteMatrix = PixelSprites.AlchemyCauldron16,
                            primaryColor = PixelPalette.Gold,
                            secondaryColor = PixelPalette.DarkSlate,
                            accentColor = PixelPalette.Crimson,
                            pixelSizeDp = 3.5.dp
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "🔥 GEOCENTRYCZNY PŁOMIEŃ DUSZY",
                            color = DaoGold,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Płomień podtrzymuje temperaturę kotła i potęguje moc tworzonych pigułek:",
                            color = TextSecondary,
                            fontSize = 10.sp
                        )
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))

                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(uiState.flames) { flame ->
                        val isSelected = uiState.activeFlame?.id == flame.id
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) CrimsonFlameDark.copy(alpha = 0.5f) else InkSurfaceVariant)
                                .border(1.dp, if (isSelected) DaoGold else InkCardBorder, RoundedCornerShape(8.dp))
                                .clickable { viewModel.selectGeocentricFlame(flame) }
                                .padding(10.dp)
                        ) {
                            Column {
                                Text(text = flame.name, color = if (isSelected) TextGold else TextPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                Text(text = "${flame.tierName} • +${flame.bonusCraftingRate}% Sukcesu", color = JadeSpiritLight, fontSize = 10.sp)
                            }
                        }
                    }
                }
            }
        }

        val chamberFacility = uiState.facilities.find { it.type == FacilityType.ALCHEMY_CHAMBER }
        val chamberLevel = chamberFacility?.level ?: 1

        item {
            Surface(
                color = InkSurfaceVariant,
                shape = RoundedCornerShape(8.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, DaoGold),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "⚗️ Status Sali Alchemii: Poziom $chamberLevel",
                            color = TextGold,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Odblokowuje pigułki do Stopnia $chamberLevel kultywacji",
                            color = JadeSpiritLight,
                            fontSize = 11.sp
                        )
                    }
                    Button(
                        onClick = { viewModel.upgradeFacility(FacilityType.ALCHEMY_CHAMBER) },
                        colors = ButtonDefaults.buttonColors(containerColor = DaoGoldDark),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        modifier = Modifier.testTag("upgrade_alchemy_chamber")
                    ) {
                        Text("Rozbuduj (${chamberFacility?.upgradeCostStones ?: 200} 💎)", fontSize = 10.sp, color = TextGold, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        item {
            Text(
                text = "Receptury Pigułek i Eliksirów Kultywacji (Stopnie 1 - 5)",
                color = TextGold,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
        }

        items(recipes) { recipe ->
            val isUnlocked = chamberLevel >= recipe.requiredAlchemyChamberLevel
            val herbCount = uiState.inventory[recipe.requiredHerbName] ?: 0
            val secHerbCount = if (recipe.secondaryHerbName != null) uiState.inventory[recipe.secondaryHerbName] ?: 0 else 0
            val hasSec = recipe.secondaryHerbName == null || secHerbCount >= recipe.secondaryHerbCount
            val canAfford = isUnlocked && herbCount >= recipe.requiredHerbCount && hasSec && uiState.spiritStones >= recipe.requiredSpiritStones
            val pillImg = rememberWuxiaGraphic("item_pill_breakthrough")

            WuxiaCard(
                borderColor = if (!isUnlocked) Color(0xFF475569) else if (canAfford) JadeSpiritLight else DaoGoldDark,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (pillImg != null) {
                        androidx.compose.foundation.Image(
                            bitmap = pillImg,
                            contentDescription = recipe.pillName,
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                            modifier = Modifier
                                .size(46.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .border(1.dp, if (isUnlocked) DaoGold else Color.Gray, RoundedCornerShape(8.dp))
                        )
                    } else {
                        Icon(Icons.Filled.Science, contentDescription = null, tint = if (isUnlocked) JadeSpiritLight else Color.Gray, modifier = Modifier.size(32.dp))
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = recipe.pillName, color = if (isUnlocked) TextGold else Color(0xFF94A3B8), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            WuxiaBadge(
                                text = recipe.tierName,
                                containerColor = if (isUnlocked) DaoGoldDark.copy(alpha = 0.3f) else Color(0xFF334155),
                                contentColor = if (isUnlocked) DaoGoldLight else Color(0xFF94A3B8)
                            )
                        }
                        Text(text = recipe.effectDescription, color = TextSecondary, fontSize = 11.sp)
                        Spacer(modifier = Modifier.height(4.dp))

                        if (!isUnlocked) {
                            Text(
                                text = "🔒 Zablokowane: Wymagana Sala Alchemii Poz. ${recipe.requiredAlchemyChamberLevel}",
                                color = VermillionRed,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        } else {
                            Text(
                                text = "Główny: ${recipe.requiredHerbCount}x ${recipe.requiredHerbName} (Masz: $herbCount) ${if (herbCount >= recipe.requiredHerbCount) "✓" else "✗"}",
                                color = if (herbCount >= recipe.requiredHerbCount) AstralCyanLight else VermillionRed,
                                fontSize = 10.sp
                            )
                            if (recipe.secondaryHerbName != null && recipe.secondaryHerbCount > 0) {
                                Text(
                                    text = "Pomocniczy: ${recipe.secondaryHerbCount}x ${recipe.secondaryHerbName} (Masz: $secHerbCount) ${if (hasSec) "✓" else "✗"}",
                                    color = if (hasSec) AstralCyanLight else VermillionRed,
                                    fontSize = 10.sp
                                )
                            }
                            Text(
                                text = "Koszt Ducha: ${recipe.requiredSpiritStones} 💎 (Masz: ${uiState.spiritStones})",
                                color = if (uiState.spiritStones >= recipe.requiredSpiritStones) JadeSpiritLight else VermillionRed,
                                fontSize = 10.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = { viewModel.brewPill(recipe) },
                        enabled = canAfford,
                        modifier = Modifier.testTag("brew_${recipe.id}"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = JadeSpiritPrimary,
                            disabledContainerColor = InkCardBorder
                        ),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = if (!isUnlocked) "Zablokowane" else "Warz",
                            fontSize = 11.sp,
                            color = if (canAfford) Color.Black else TextMuted,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ForgingTabContent(
    uiState: WuxiaUiState,
    viewModel: WuxiaGameViewModel,
    recipes: List<ForgingRecipe>
) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        item {
            ManhwaHeroBanner(
                assetName = "bg_forge_hall",
                title = "Boska Kuźnia Spadającej Gwiazdy",
                subtitle = "Gwiezdny Meteoryt i Płynna Ruda Qi • Wykuwanie Boskich Artefaktów",
                height = 160.dp
            )
        }

        // AAA 2D Interactive Animated Forge View
        item {
            Wuxia2dForgeInteractive()
        }

        item {
            WuxiaCard(
                borderColor = Color(0xFFFFD700),
                backgroundColor = Color(0xFF130D20),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFFB45309).copy(alpha = 0.2f))
                            .border(1.dp, Color(0xFFFFD700), RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("🔨", fontSize = 22.sp)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(text = "RYTMICZNE WYKUWANIE ARTEFAKTÓW", color = Color(0xFFFFE8A3), fontSize = 13.sp, fontWeight = FontWeight.Black)
                        Text(text = "Dotykaj kowadła w rytm iskier, aby podtrzymać żar kucia i wytwarzać broń o statusie Boskim.", color = Color(0xFF94A3B8), fontSize = 10.sp)
                    }
                }
            }
        }

        item {
            Text(
                text = "Schematy Kucia Oręża i Talizmanów",
                color = TextGold,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
        }

        items(recipes) { recipe ->
            val mineralCount = uiState.inventory[recipe.requiredMineralName] ?: 0
            val canAfford = mineralCount >= recipe.requiredMineralCount && uiState.spiritStones >= recipe.requiredSpiritStones

            WuxiaCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.Shield, contentDescription = null, tint = DaoGoldLight, modifier = Modifier.size(32.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = recipe.artifactName, color = TextGold, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.width(4.dp))
                            WuxiaBadge(text = recipe.artifactResult.rarity, containerColor = DaoGoldDark.copy(alpha = 0.3f), contentColor = DaoGoldLight)
                        }
                        Text(text = "⚔️ Atak +${recipe.artifactResult.attackBonus} • 🛡️ Obrona +${recipe.artifactResult.defenseBonus}", color = JadeSpiritLight, fontSize = 11.sp)
                        Text(text = recipe.artifactResult.specialEffect, color = TextSecondary, fontSize = 10.sp)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Wymaga: ${recipe.requiredMineralCount}x ${recipe.requiredMineralName} (Masz: $mineralCount) • ${recipe.requiredSpiritStones} 💎",
                            color = if (canAfford) AstralCyanLight else VermillionRed,
                            fontSize = 10.sp
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = { viewModel.forgeArtifact(recipe) },
                        enabled = canAfford,
                        modifier = Modifier.testTag("forge_${recipe.id}"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = DaoGoldDark,
                            disabledContainerColor = InkCardBorder
                        ),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text("Wykuj", fontSize = 11.sp, color = if (canAfford) Color.White else TextMuted, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun InventoryVaultContent(uiState: WuxiaUiState) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        item {
            Text(
                text = "Zasoby i Przedmioty w Magazynie",
                color = TextGold,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Zgromadzone zioła alchemiczne, rzadkie minerały oraz uwarzone pigułki:",
                color = TextSecondary,
                fontSize = 11.sp
            )
        }

        items(uiState.inventory.entries.toList()) { entry ->
            Surface(
                color = InkSurfaceVariant,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val emoji = when {
                        entry.key.contains("Lotos") || entry.key.contains("Trawa") || entry.key.contains("Korzeń") || entry.key.contains("Żeń-szeń") -> "🌿"
                        entry.key.contains("Żelazo") || entry.key.contains("Nefryt") || entry.key.contains("Obsydian") || entry.key.contains("Kryształ") || entry.key.contains("Piasek") -> "💎"
                        entry.key.contains("Pigułka") || entry.key.contains("Eliksir") -> "💊"
                        else -> "📜"
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = emoji, fontSize = 20.sp)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(text = entry.key, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    }

                    Text(text = "x${entry.value}", color = TextGold, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun FlamesTabContent(
    uiState: WuxiaUiState,
    viewModel: WuxiaGameViewModel
) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        item {
            ManhwaHeroBanner(
                assetName = "bg_alchemy_hall",
                title = "Święte Sanktuarium Płomieni Duszy",
                subtitle = "Płomienie Geocentryczne i Niebiańskie • Uszlachetnianie & Fuzja Temperatur",
                height = 150.dp
            )
        }

        item {
            val active = uiState.activeFlame
            WuxiaCard(
                borderColor = Color(0xFFFF5252),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "🔥 AKTYWNY PŁOMIEŃ SEKTY",
                        color = Color(0xFFFF8A80),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    WuxiaBadge(
                        text = "${active?.temperatureCelsius ?: 1800}°C",
                        containerColor = CrimsonFlameDark,
                        contentColor = Color.White
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = active?.name ?: "Brak wybranego płomienia",
                    color = TextGold,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Afinacja: ${active?.elementAffinity ?: "Czysty Yang"} • Uszlachetnienie: Poziom ${active?.refinementLevel ?: 1}",
                    color = AstralCyanLight,
                    fontSize = 11.sp
                )
                Text(
                    text = "Efekt Płomienia: ${active?.specialEffect ?: "Brak"}",
                    color = JadeSpiritLight,
                    fontSize = 11.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Bonusy: +${active?.bonusCraftingRate ?: 0}% do Szansy Sukcesu Alchemii • +${active?.combatDamageBonus ?: 0} do Obrażeń w Walce",
                    color = TextSecondary,
                    fontSize = 10.sp
                )
            }
        }

        item {
            Text(
                text = "Wszystkie Posiadane Płomienie Duchowe",
                color = TextGold,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
        }

        items(uiState.flames) { flame ->
            val isActive = uiState.activeFlame?.id == flame.id
            val refineCostStones = flame.refinementLevel * 150
            val crystalCost = (flame.refinementLevel / 2) + 1
            val crystalsInInv = uiState.inventory["Kryształ Geocentrycznego Ognia"] ?: 0
            val canRefine = uiState.spiritStones >= refineCostStones && crystalsInInv >= crystalCost

            WuxiaCard(
                borderColor = if (isActive) Color(0xFFFF5252) else DaoGoldDark,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(text = flame.name, color = TextGold, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            Text(text = "${flame.tierName} • ${flame.temperatureCelsius}°C", color = Color(0xFFFF8A80), fontSize = 11.sp)
                        }
                        if (isActive) {
                            WuxiaBadge(
                                text = "W UŻYCIU",
                                containerColor = CrimsonFlameDark,
                                contentColor = Color.White
                            )
                        } else {
                            OutlinedButton(
                                onClick = { viewModel.selectGeocentricFlame(flame) },
                                modifier = Modifier.testTag("select_flame_${flame.id}"),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text("Użyj Płomienia", fontSize = 10.sp, color = TextGold)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(text = flame.description, color = TextSecondary, fontSize = 11.sp)
                    Text(text = "Uszlachetnienie: Poz. ${flame.refinementLevel} • ${flame.specialEffect}", color = JadeSpiritLight, fontSize = 10.sp)

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Koszt Uszlachetnienia: $refineCostStones 💎 + ${crystalCost}x Kryształ Ognia (Masz: $crystalsInInv)",
                            color = if (canRefine) AstralCyanLight else VermillionRed,
                            fontSize = 10.sp
                        )
                        Button(
                            onClick = {
                                WuxiaAudioEngine.playCauldronFlame()
                                viewModel.refineFlame(flame.id)
                            },
                            enabled = canRefine,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                            modifier = Modifier.testTag("refine_flame_${flame.id}")
                        ) {
                            Text("Uszlachetnij", fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BeastsTabContent(
    uiState: WuxiaUiState,
    viewModel: WuxiaGameViewModel
) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        item {
            ManhwaHeroBanner(
                assetName = "bg_combat_arena",
                title = "Sanktuarium Świętych Bestii Sekty",
                subtitle = "Pradawne Bóstwa Opiekuńcze • Karmienie Ziołami, Ewolucja i Aury Bojowe",
                height = 150.dp
            )
        }

        item {
            val active = uiState.activeBeast
            WuxiaCard(
                borderColor = JadeSpiritLight,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "🐉 AKTYWNA BESTIA OPIEKUŃCZA",
                        color = JadeSpiritLight,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    WuxiaBadge(
                        text = "Rząd ${active?.tier ?: 1} Bóstwa",
                        containerColor = JadeSpiritPrimary.copy(alpha = 0.3f),
                        contentColor = JadeSpiritLight
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = active?.emoji ?: "🐉", fontSize = 32.sp)
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(text = active?.name ?: "Brak strażnika", color = TextGold, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        Text(text = active?.title ?: "", color = TextSecondary, fontSize = 11.sp)
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = "Żywioł: ${active?.element ?: "Czysty Yang"}", color = AstralCyanLight, fontSize = 11.sp)
                Text(text = "Globalny Buff Sekty: ${active?.passiveSectBuff ?: ""}", color = JadeSpiritLight, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                Text(text = "Zdolność Bojowa: ${active?.combatSkillName ?: ""} - ${active?.combatSkillDesc ?: ""}", color = TextSecondary, fontSize = 10.sp)
            }
        }

        item {
            Text(
                text = "Poczet Mitycznych Bestii Sekty",
                color = TextGold,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
        }

        items(uiState.sectBeasts) { beast ->
            val isActive = uiState.activeBeast?.id == beast.id
            val favHerbCount = uiState.inventory[beast.favoriteHerb] ?: 0
            val canFeed = favHerbCount > 0
            val evolveCost = beast.tier * 300
            val canEvolve = beast.intimacy >= 60 && uiState.spiritStones >= evolveCost

            WuxiaCard(
                borderColor = if (isActive) JadeSpiritLight else DaoGoldDark,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = beast.emoji, fontSize = 28.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(text = beast.name, color = TextGold, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                Text(text = "${beast.title} • Rząd ${beast.tier}", color = AstralCyanLight, fontSize = 10.sp)
                            }
                        }

                        if (isActive) {
                            WuxiaBadge(
                                text = "STRAŻNIK",
                                containerColor = JadeSpiritPrimary,
                                contentColor = Color.Black
                            )
                        } else {
                            OutlinedButton(
                                onClick = {
                                    WuxiaAudioEngine.playDivineGong()
                                    viewModel.setActiveBeast(beast.id)
                                },
                                modifier = Modifier.testTag("activate_beast_${beast.id}"),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text("Ustanów Strażnikiem", fontSize = 10.sp, color = JadeSpiritLight)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Intimacy Bar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "Intymność i Więź:", color = TextSecondary, fontSize = 10.sp)
                        Text(text = "${beast.intimacy}% / 100%", color = JadeSpiritLight, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(Color(0xFF1E142B))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(beast.intimacy / 100f)
                                .background(JadeSpiritLight)
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(text = "Aura Sekty: ${beast.passiveSectBuff}", color = JadeSpiritLight, fontSize = 10.sp)
                    Text(text = "Ulubiony Przysmak: ${beast.favoriteHerb} (Masz: $favHerbCount)", color = AstralCyanLight, fontSize = 10.sp)

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                WuxiaAudioEngine.playCauldronFlame()
                                viewModel.feedSectBeast(beast.id, beast.favoriteHerb)
                            },
                            enabled = canFeed,
                            colors = ButtonDefaults.buttonColors(containerColor = JadeSpiritPrimary),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("feed_beast_${beast.id}"),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                        ) {
                            Text("Nakarm Ziółkiem", fontSize = 10.sp, color = if (canFeed) Color.Black else TextMuted, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = {
                                WuxiaAudioEngine.playDivineGong()
                                viewModel.evolveSectBeast(beast.id)
                            },
                            enabled = canEvolve,
                            colors = ButtonDefaults.buttonColors(containerColor = DaoGoldDark),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("evolve_beast_${beast.id}"),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                        ) {
                            Text("Ewolucja ($evolveCost 💎)", fontSize = 10.sp, color = if (canEvolve) TextGold else TextMuted, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

