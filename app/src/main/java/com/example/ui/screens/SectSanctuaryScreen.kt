package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.game.WuxiaGameViewModel
import com.example.game.WuxiaGenerator
import com.example.game.WuxiaUiState
import com.example.model.FacilityType
import com.example.model.SectFacility
import com.example.ui.audio.WuxiaAudioEngine
import com.example.ui.components.QiProgressBar
import com.example.ui.components.WuxiaBadge
import com.example.ui.components.WuxiaCard
import com.example.ui.graphics.*
import com.example.ui.pixelart.*
import com.example.ui.theme.*
import com.example.ui.aaa.*

/**
 * Zakładki nawigacyjne Kampusu Sekty (Sect Campus Navigation)
 */
enum class SectCampusTab(val title: String, val icon: String, val testTag: String) {
    PAVILION("Pawilon Sekty", "🏛️", "campus_nav_pavilion"),
    ALCHEMY_LAB("Lab Alchemii", "⚗️", "campus_nav_alchemy_lab"),
    CULTIVATION_HALL("Sala Kultywacji", "🧘", "campus_nav_cultivation_hall")
}

@Composable
fun SectSanctuaryScreen(
    viewModel: WuxiaGameViewModel,
    uiState: WuxiaUiState,
    onNavigateToDisciples: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedCampusTab by remember { mutableStateOf(SectCampusTab.PAVILION) }
    var showRecruitDialog by remember { mutableStateOf(false) }

    val readyBreakthroughCount = uiState.disciples.count { it.realmProgress >= 100 }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(top = 10.dp, bottom = 24.dp)
    ) {
        // 1. Interaktywna Scena 2D Kampusu na Świętej Górze
        item {
            Wuxia2dSanctuaryCanvas(
                timeOfDay = uiState.timeOfDay,
                disciplesCount = uiState.disciples.size,
                readyBreakthroughCount = readyBreakthroughCount,
                onBuildingClick = { building ->
                    when (building) {
                        SanctuaryBuilding.MAIN_HALL -> {
                            selectedCampusTab = SectCampusTab.PAVILION
                            WuxiaAudioEngine.playDivineGong()
                        }
                        SanctuaryBuilding.ALCHEMY_PAVILION -> {
                            selectedCampusTab = SectCampusTab.ALCHEMY_LAB
                            WuxiaAudioEngine.playCauldronFlame()
                        }
                        SanctuaryBuilding.CULTIVATION_PEAK,
                        SanctuaryBuilding.MARTIAL_GROUNDS -> {
                            selectedCampusTab = SectCampusTab.CULTIVATION_HALL
                            WuxiaAudioEngine.playSwordClash()
                        }
                        SanctuaryBuilding.DIVINE_FORGE -> {
                            viewModel.upgradeFacility(FacilityType.DIVINE_FORGE)
                            WuxiaAudioEngine.playSwordClash()
                        }
                    }
                }
            )
        }

        // 2. Nawigacja kampusu – segmentowy przełącznik AAA
        item {
            AaaSegmentedTabs(
                items = SectCampusTab.values().map { it.title },
                selectedIndex = selectedCampusTab.ordinal,
                onSelect = { idx ->
                    val tab = SectCampusTab.values()[idx]
                    selectedCampusTab = tab
                    when (tab) {
                        SectCampusTab.PAVILION -> WuxiaAudioEngine.playDivineGong()
                        SectCampusTab.ALCHEMY_LAB -> WuxiaAudioEngine.playCauldronFlame()
                        SectCampusTab.CULTIVATION_HALL -> WuxiaAudioEngine.playThunderStrike()
                    }
                },
                badges = listOf(
                    if (uiState.availableRecruits.isNotEmpty()) "${uiState.availableRecruits.size}" else null,
                    null,
                    if (readyBreakthroughCount > 0) "$readyBreakthroughCount" else null
                ),
                testTags = SectCampusTab.values().map { it.testTag },
                modifier = Modifier.testTag("campus_navigation_bar")
            )
        }

        // 3. Dynamic Zone Content        // 3. Dynamic Zone Content based on Selected Campus Tab
        when (selectedCampusTab) {
            SectCampusTab.PAVILION -> {
                item {
                    PavilionHeroCard(
                        uiState = uiState,
                        viewModel = viewModel,
                        onRecruitClick = { showRecruitDialog = true },
                        onNavigateToDisciples = onNavigateToDisciples,
                        onToggleAutoBreakthrough = { viewModel.toggleAutoBreakthrough() }
                    )
                }

                item {
                    ParchmentScrollFrame(
                        title = "Święte Pawilony Cytadeli Sekty",
                        sealChar = "宗",
                        borderColor = GildedGold,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Wznoś pagody i umacniaj pozycję w świecie kultywacji:",
                                color = Color(0xFFD6CCB6),
                                fontSize = 11.sp
                            )
                            uiState.facilities.forEach { facility ->
                                FacilityItemCard(
                                    facility = facility,
                                    currentStones = uiState.spiritStones,
                                    onUpgrade = {
                                        WuxiaAudioEngine.playDivineGong()
                                        viewModel.upgradeFacility(facility.type)
                                    }
                                )
                            }
                        }
                    }
                }
            }

            SectCampusTab.ALCHEMY_LAB -> {
                item {
                    AlchemyLabHeroBanner()
                }

                item {
                    Wuxia2dCauldronInteractive(
                        flameName = uiState.activeFlame?.name ?: "Geocentryczny Płomień Serca Ziemi",
                        bonusRate = uiState.activeFlame?.bonusCraftingRate ?: 25,
                        onBrewSuccess = {
                            val breakthroughRecipe = WuxiaGenerator.getAlchemyRecipes().firstOrNull { it.pillName.contains("Przełomu") }
                                ?: WuxiaGenerator.getAlchemyRecipes().first()
                            viewModel.brewPill(breakthroughRecipe)
                        }
                    )
                }

                item {
                    AlchemyLabGardenVaultCard(uiState = uiState)
                }

                item {
                    CampusSectionHeader(
                        title = "Święte Receptury Pigułek Dao",
                        subtitle = "Warz esencje przełomu oraz eliksiry oczyszczające meridiany"
                    )
                }

                items(WuxiaGenerator.getAlchemyRecipes()) { recipe ->
                    AlchemyQuickRecipeCard(
                        recipe = recipe,
                        inventory = uiState.inventory,
                        currentStones = uiState.spiritStones,
                        onBrew = {
                            WuxiaAudioEngine.playCauldronFlame()
                            viewModel.brewPill(recipe)
                        }
                    )
                }
            }

            SectCampusTab.CULTIVATION_HALL -> {
                item {
                    CultivationHallOverviewCard(
                        uiState = uiState,
                        readyCount = readyBreakthroughCount,
                        onMassBreakthrough = {
                            WuxiaAudioEngine.playDivineGong()
                            viewModel.breakthroughAllReadyDisciples()
                        },
                        onToggleAuto = { viewModel.toggleAutoBreakthrough() },
                        onNavigateToDisciples = onNavigateToDisciples
                    )
                }

                item {
                    CampusSectionHeader(
                        title = "Adeptowie w Sali Medytacji (${uiState.disciples.size})",
                        subtitle = "Zgromadzona energia Qi w meridianach Dantian"
                    )
                }

                items(uiState.disciples) { disciple ->
                    CultivationHallDiscipleCard(
                        disciple = disciple,
                        onBreakthrough = {
                            WuxiaAudioEngine.playThunderStrike()
                            viewModel.attemptBreakthrough(disciple.id, usePill = false)
                        },
                        onOpenChamber = onNavigateToDisciples
                    )
                }
            }
        }
    }

    // Recruitment Ceremony Dialog
    if (showRecruitDialog) {
        RecruitmentDialog(
            uiState = uiState,
            viewModel = viewModel,
            onDismiss = { showRecruitDialog = false }
        )
    }
}

// -------------------------------------------------------------
// SECT PAVILION COMPONENTS
// -------------------------------------------------------------

@Composable
fun PavilionHeroCard(
    uiState: WuxiaUiState,
    viewModel: WuxiaGameViewModel? = null,
    onRecruitClick: () -> Unit,
    onNavigateToDisciples: () -> Unit,
    onToggleAutoBreakthrough: () -> Unit
) {
    val masterAvatar = rememberWuxiaGraphic("avatar_sword_immortal")
    val leader = uiState.sectLeader

    ParchmentScrollFrame(
        title = "Święty Tron Patriarchy ${leader.name}",
        sealChar = "宗",
        borderColor = GildedGold,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth()
        ) {
            // Patriarch Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Standee Portrait
                Box(
                    modifier = Modifier
                        .size(68.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .border(1.5.dp, Color(0xFFE8C37E), RoundedCornerShape(12.dp))
                        .background(Color(0xFF0F0B18)),
                    contentAlignment = Alignment.Center
                ) {
                    if (masterAvatar != null) {
                        Image(
                            bitmap = masterAvatar,
                            contentDescription = leader.name,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Text(text = "🥋", fontSize = 34.sp)
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = leader.name,
                            color = Color(0xFFF6E6BE),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Black
                        )
                        Surface(
                            color = Color(0xFF78350F),
                            shape = RoundedCornerShape(6.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE8C37E))
                        ) {
                            Text(
                                text = "PRESTIŻ: POZ. ${leader.prestigeLevel}",
                                color = Color(0xFFE8C37E),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Black,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "🏰 ${leader.ancientClan.name} • ${leader.ancientClan.reputationTitle}",
                        color = Color(0xFFEBCB7A),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "🧬 ${leader.lineage} | ${leader.divineConstitution}",
                        color = Color(0xFF00E5FF),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "⚡ Sfera: ${leader.realm} • Autorytet: ${leader.divineAuthority} pkt",
                        color = Color(0xFFD6CCB6),
                        fontSize = 10.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Prestiż Patriarchy Awans Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF261B3D))
                    .border(1.dp, Color(0xFFE8C37E).copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "👑 Autorytet i Prestiż Sekty",
                        color = Color(0xFFF3DDA6),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Bonus kultywacji adeptów: +${leader.prestigeLevel * 5}%",
                        color = Color(0xFFB0A592),
                        fontSize = 9.sp
                    )
                }
                val prestigeCost = leader.prestigeLevel * 250
                Button(
                    onClick = {
                        WuxiaAudioEngine.playDivineGong()
                        viewModel?.upgradePatriarchPrestige()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF9A6B2C)),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.testTag("upgrade_prestige_button")
                ) {
                    Text(
                        text = "Awans ($prestigeCost 💎)",
                        fontSize = 10.sp,
                        color = Color.White,
                        fontWeight = FontWeight.Black
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Dekrety Sekty (Sect Decrees)
            Text(
                text = "📜 Święte Dekrety Sekty (Wpływ Patriarchy):",
                color = Color(0xFFF6E6BE),
                fontSize = 11.sp,
                fontWeight = FontWeight.Black
            )
            Spacer(modifier = Modifier.height(6.dp))

            leader.activeDecrees.forEach { decree ->
                Surface(
                    color = if (decree.isActive) Color(0xFF451A03) else Color(0xFF1E172B),
                    shape = RoundedCornerShape(6.dp),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (decree.isActive) Color(0xFFE8C37E) else Color(0xFF3A2E35)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp)
                        .clickable {
                            WuxiaAudioEngine.playDivineGong()
                            viewModel?.toggleSectDecree(decree.id)
                        }
                        .testTag("decree_${decree.id}")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "${decree.icon} ${decree.title}",
                                color = if (decree.isActive) Color(0xFFF6E6BE) else Color(0xFFD6CCB6),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = decree.effectDescription,
                                color = if (decree.isActive) Color(0xFFF3E0A8) else Color(0xFFB0A592),
                                fontSize = 9.sp
                            )
                        }
                        Surface(
                            color = if (decree.isActive) Color(0xFF10B981) else Color(0xFF374151),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = if (decree.isActive) "AKTYWNY" else "NIEAKTYWNY",
                                color = Color.White,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Black,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Auto-Breakthrough & Info Strip
            Surface(
                color = Color(0xFF1B1228),
                shape = RoundedCornerShape(8.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE8C37E).copy(alpha = 0.35f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "⚡ Automatyczny Przełom Dantian",
                            color = Color(0xFFF3DDA6),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (uiState.autoBreakthroughEnabled) "Adeptowie przy 100% Qi przełamują się na bieżąco" else "Wymagany ręczny rytuał Niebiańskiej Kary",
                            color = Color(0xFFB0A592),
                            fontSize = 9.sp
                        )
                    }

                    Switch(
                        checked = uiState.autoBreakthroughEnabled,
                        onCheckedChange = { onToggleAutoBreakthrough() },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color(0xFFE8C37E),
                            checkedTrackColor = Color(0xFF9A6B2C)
                        ),
                        modifier = Modifier.testTag("toggle_auto_breakthrough_sanctuary")
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onRecruitClick,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("recruit_ceremony_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF9A6B2C)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Filled.PersonAdd, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color(0xFFE8C37E))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Nabór Uczniów", color = Color(0xFFF3DDA6), fontSize = 12.sp, fontWeight = FontWeight.Black)
                }

                OutlinedButton(
                    onClick = onNavigateToDisciples,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("manage_disciples_button"),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE8C37E)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Filled.Groups, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color(0xFF00E5FF))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Adeptowie (${uiState.disciples.size})", color = Color(0xFFF3DDA6), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// -------------------------------------------------------------
// ALCHEMY LAB COMPONENTS
// -------------------------------------------------------------

@Composable
fun AlchemyLabHeroBanner() {
    ManhwaHeroBanner(
        assetName = "bg_alchemy_hall",
        title = "Laboratorium Alchemii i Kocioł Qi",
        subtitle = "Geocentryczny Płomień Serca Ziemi • Destylacja Pigułek 9 Chmur",
        height = 150.dp
    )
}

@Composable
fun AlchemyLabGardenVaultCard(uiState: WuxiaUiState) {
    WuxiaCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = "🌿", fontSize = 22.sp)
                Spacer(Modifier.width(8.dp))
                Column {
                    Text(
                        text = "Ogród Duchowych Ziół i Magazyn",
                        color = TextGold,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Zbiory z ekspedycji gotowe do destylacji",
                        color = TextSecondary,
                        fontSize = 11.sp
                    )
                }
            }
            WuxiaBadge(
                text = "${uiState.inventory.size} Surowców",
                containerColor = JadeSpiritDark.copy(alpha = 0.4f),
                contentColor = JadeSpiritLight
            )
        }

        Spacer(Modifier.height(8.dp))

        // Inventory Herbs Chips
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            val herbs = listOf(
                "Trawa Wiecznej Wiosny" to (uiState.inventory["Trawa Wiecznej Wiosny"] ?: 8),
                "Śnieżny Lotos" to (uiState.inventory["Śnieżny Lotos Oświecenia"] ?: 4),
                "Fioletowy Korzeń" to (uiState.inventory["Fioletowy Korzeń Skorpiona"] ?: 3)
            )
            herbs.forEach { (name, count) ->
                Surface(
                    color = InkSurfaceVariant,
                    shape = RoundedCornerShape(6.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, JadeSpiritPrimary.copy(alpha = 0.5f)),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(text = name.take(10), color = TextPrimary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        Text(text = "$count szt.", color = JadeSpiritLight, fontSize = 9.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun AlchemyQuickRecipeCard(
    recipe: com.example.model.AlchemyRecipe,
    inventory: Map<String, Int>,
    currentStones: Int,
    onBrew: () -> Unit
) {
    val herbCount = inventory[recipe.requiredHerbName] ?: 0
    val canAfford = herbCount >= recipe.requiredHerbCount && currentStones >= recipe.requiredSpiritStones

    WuxiaCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "💊", fontSize = 24.sp)
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = recipe.pillName, color = TextGold, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Text(text = recipe.effectDescription, color = TextSecondary, fontSize = 10.sp, lineHeight = 14.sp)
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Wymóg: ${recipe.requiredHerbCount}x ${recipe.requiredHerbName} (Masz: $herbCount) • ${recipe.requiredSpiritStones} 💎",
                    color = if (canAfford) AstralCyanLight else VermillionRed,
                    fontSize = 10.sp
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Button(
                onClick = onBrew,
                enabled = canAfford,
                colors = ButtonDefaults.buttonColors(
                    containerColor = JadeSpiritPrimary,
                    disabledContainerColor = InkCardBorder
                ),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.testTag("quick_brew_${recipe.id}")
            ) {
                Text(
                    text = "Warz",
                    fontSize = 11.sp,
                    color = if (canAfford) Color.Black else TextMuted,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

// -------------------------------------------------------------
// CULTIVATION HALL COMPONENTS
// -------------------------------------------------------------

@Composable
fun CultivationHallOverviewCard(
    uiState: WuxiaUiState,
    readyCount: Int,
    onMassBreakthrough: () -> Unit,
    onToggleAuto: () -> Unit,
    onNavigateToDisciples: () -> Unit
) {
    WuxiaCard(
        borderColor = if (readyCount > 0) Color(0xFFE8C37E) else InkCardBorder,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "🧘", fontSize = 18.sp)
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "Sala Kultywacji Qi",
                        color = TextGold,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    text = "Gęstość duchowej aury: 880 pkt/h • 8 Cudownych Meridianów",
                    color = AstralCyanLight,
                    fontSize = 11.sp
                )
            }

            Surface(
                color = if (uiState.autoBreakthroughEnabled) JadeSpiritDark.copy(alpha = 0.4f) else InkSurfaceVariant,
                shape = RoundedCornerShape(6.dp),
                modifier = Modifier.clickable { onToggleAuto() }
            ) {
                Text(
                    text = if (uiState.autoBreakthroughEnabled) "⚡ Auto: WŁ" else "⏸️ Auto: WYŁ",
                    color = if (uiState.autoBreakthroughEnabled) JadeSpiritLight else TextMuted,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (readyCount > 0) {
            Button(
                onClick = onMassBreakthrough,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD97706)),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("mass_breakthrough_button")
            ) {
                Text(
                    text = "⚡ DOKONAJ MASOWEGO PRZEŁOMU ($readyCount ADEPTÓW)",
                    color = Color.White,
                    fontWeight = FontWeight.Black,
                    fontSize = 11.sp
                )
            }
        } else {
            OutlinedButton(
                onClick = onNavigateToDisciples,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                border = ButtonDefaults.outlinedButtonBorder
            ) {
                Icon(Icons.Filled.SelfImprovement, contentDescription = null, modifier = Modifier.size(16.dp), tint = DaoGoldLight)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Zarządzaj Technikami i Meridianami Adeptów", color = TextGold, fontSize = 11.sp)
            }
        }
    }
}

@Composable
fun CultivationHallDiscipleCard(
    disciple: com.example.model.Disciple,
    onBreakthrough: () -> Unit,
    onOpenChamber: () -> Unit
) {
    val isReady = disciple.realmProgress >= 100

    WuxiaCard(
        borderColor = if (isReady) Color(0xFFE8C37E) else InkCardBorder,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpenChamber)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ManhwaSchoolAvatar(
                school = disciple.martialSchool,
                size = 48.dp
            )

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = disciple.name, color = TextGold, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    WuxiaBadge(
                        text = disciple.realm.polishName,
                        containerColor = if (isReady) Color(0xFF991B1B).copy(alpha = 0.5f) else JadeSpiritDark.copy(alpha = 0.4f),
                        contentColor = if (isReady) Color(0xFFE8C37E) else JadeSpiritLight
                    )
                }

                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${disciple.martialSchool.polishName} • Zadanie: ${disciple.role.polishName}",
                    color = TextSecondary,
                    fontSize = 10.sp
                )

                Spacer(modifier = Modifier.height(4.dp))
                PixelSegmentedBar(
                    current = disciple.realmProgress,
                    max = 100,
                    color = if (isReady) PixelPalette.Gold else PixelPalette.Jade,
                    label = if (isReady) "QI GOTOWE DO PRZEŁOMU" else "QI MERIDIANU",
                    segments = 12
                )
            }

            if (isReady) {
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = onBreakthrough,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD97706)),
                    shape = RoundedCornerShape(6.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                    modifier = Modifier.testTag("quick_breakthrough_${disciple.id}")
                ) {
                    Text("⚡ Przełom", fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.Black)
                }
            }
        }
    }
}

// -------------------------------------------------------------
// COMMON CAMPUS SHARED HELPERS
// -------------------------------------------------------------

@Composable
fun CampusSectionHeader(title: String, subtitle: String) {
    Column(modifier = Modifier.padding(top = 4.dp)) {
        Text(
            text = title,
            color = Color(0xFFF3DDA6),
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
        Text(
            text = subtitle,
            color = TextMuted,
            fontSize = 10.sp
        )
    }
}

@Composable
fun FacilityItemCard(
    facility: SectFacility,
    currentStones: Int,
    onUpgrade: () -> Unit
) {
    val (icon, emblemColor) = when (facility.type) {
        FacilityType.CULTIVATION_PAVILION -> Icons.Filled.Spa to Color(0xFF00E5FF)
        FacilityType.ALCHEMY_CHAMBER -> Icons.Filled.Science to Color(0xFFFF5252)
        FacilityType.DIVINE_FORGE -> Icons.Filled.Fireplace to Color(0xFFFF9100)
        FacilityType.MARTIAL_ARCHIVES -> Icons.Filled.MenuBook to Color(0xFFE8C37E)
        FacilityType.MISSION_HALL -> Icons.Filled.Explore to Color(0xFF10B981)
        FacilityType.DIPLOMACY_HALL -> Icons.Filled.Handshake to Color(0xFFE040FB)
    }

    val canAfford = currentStones >= facility.upgradeCostStones

    WuxiaCard(
        borderColor = if (canAfford) Color(0xFFE8C37E).copy(alpha = 0.8f) else Color(0xFF332042),
        backgroundColor = Color(0xFF16111A),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Ornate Pedestal Emblem
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(emblemColor.copy(alpha = 0.15f))
                    .border(1.dp, emblemColor.copy(alpha = 0.6f), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = emblemColor, modifier = Modifier.size(26.dp))
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = facility.type.polishName,
                        color = Color(0xFFF6E6BE),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Black
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Surface(
                        color = Color(0xFF451A03),
                        shape = RoundedCornerShape(4.dp),
                        border = androidx.compose.foundation.BorderStroke(0.5.dp, Color(0xFFE8C37E))
                    ) {
                        Text(
                            text = "POZ. ${facility.level}",
                            color = Color(0xFFE8C37E),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Black,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = facility.effectDescription,
                    color = Color(0xFFB0A592),
                    fontSize = 11.sp,
                    lineHeight = 15.sp
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Button(
                onClick = onUpgrade,
                enabled = canAfford,
                modifier = Modifier.testTag("upgrade_${facility.type.name}"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF9A6B2C),
                    disabledContainerColor = Color(0xFF261D33)
                ),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                shape = RoundedCornerShape(8.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "Ulepsz",
                        fontSize = 11.sp,
                        color = if (canAfford) Color(0xFFF3DDA6) else Color(0xFF71717A),
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        "${facility.upgradeCostStones} 💎",
                        fontSize = 10.sp,
                        color = if (canAfford) Color.White else Color(0xFF71717A),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun RecruitmentDialog(
    uiState: WuxiaUiState,
    viewModel: WuxiaGameViewModel,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = "🏮", fontSize = 20.sp)
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Ceremonia Naboru Uczniów Sekty",
                    color = Color(0xFFF6E6BE),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Black
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Kandydaci z odległych prowincji oczekują przed bramą sekty na próbę czystości meridianów:",
                    color = Color(0xFFD6CCB6),
                    fontSize = 12.sp
                )

                uiState.availableRecruits.forEach { recruit ->
                    Surface(
                        color = Color(0xFF140D20),
                        shape = RoundedCornerShape(10.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE8C37E).copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(10.dp)
                                .fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                ManhwaSchoolAvatar(
                                    school = recruit.martialSchool,
                                    size = 46.dp
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = recruit.name,
                                            color = Color(0xFFF3DDA6),
                                            fontWeight = FontWeight.Black,
                                            fontSize = 13.sp
                                        )
                                        Spacer(Modifier.width(6.dp))
                                        val (gradeColor, gradeBg) = when (recruit.talentGrade) {
                                            com.example.model.TalentGrade.SSS -> Color(0xFFE8C37E) to Color(0xFF78350F)
                                            com.example.model.TalentGrade.SS -> Color(0xFFF43F5E) to Color(0xFF881337)
                                            com.example.model.TalentGrade.S -> Color(0xFF00E5FF) to Color(0xFF164E63)
                                            com.example.model.TalentGrade.A -> Color(0xFF34D399) to Color(0xFF064E3B)
                                            com.example.model.TalentGrade.B -> Color(0xFFA78BFA) to Color(0xFF3B0764)
                                        }
                                        Surface(
                                            color = gradeBg,
                                            shape = RoundedCornerShape(4.dp),
                                            border = androidx.compose.foundation.BorderStroke(1.dp, gradeColor)
                                        ) {
                                            Text(
                                                text = recruit.talentGrade.name,
                                                color = gradeColor,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Black,
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                            )
                                        }
                                    }
                                    Text(
                                        text = "🏰 ${recruit.ancientClan.name} • ${recruit.ancientClan.reputationTitle}",
                                        color = Color(0xFFEBCB7A),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "⚔️ Siła Bojowa: ${recruit.combatPower} CP | ${recruit.martialSchool.polishName}",
                                        color = Color(0xFF00E5FF),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = "🩸 ${recruit.bloodline.polishName} | ✨ ${recruit.constitution.polishName}",
                                        color = Color(0xFFFCA5A5),
                                        fontSize = 9.sp
                                    )
                                    if (recruit.fatedTraits.isNotEmpty()) {
                                        Text(
                                            text = "✦ Przeznaczenie: ${recruit.fatedTraits.joinToString(", ") { it.name }}",
                                            color = Color(0xFFF2D58A),
                                            fontSize = 9.sp
                                        )
                                    }
                                    Text(
                                        text = "🌀 Technika: ${recruit.innateTechnique}",
                                        color = Color(0xFFA5B4FC),
                                        fontSize = 9.sp
                                    )
                                }
                            }

                            Button(
                                onClick = {
                                    WuxiaAudioEngine.playDivineGong()
                                    viewModel.recruitDisciple(recruit.id)
                                },
                                modifier = Modifier.testTag("recruit_${recruit.id}"),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF047857)),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Przyjmij\n(120 💎)", fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.Black, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                            }
                        }
                    }
                }

                if (uiState.availableRecruits.isNotEmpty()) {
                    Button(
                        onClick = {
                            WuxiaAudioEngine.playDivineGong()
                            viewModel.recruitAllAvailable()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("recruit_all_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF9A6B2C)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            "👑 Masowy Zaciąg Wszystkich (${uiState.availableRecruits.size * 120} 💎)",
                            fontSize = 11.sp,
                            color = Color(0xFFFEF3C7),
                            fontWeight = FontWeight.Black
                        )
                    }
                }

                OutlinedButton(
                    onClick = {
                        WuxiaAudioEngine.playDivineGong()
                        viewModel.refreshRecruits()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("refresh_recruits_button"),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE8C37E)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Zwołaj próbę (6 adeptów) (60 💎)", fontSize = 11.sp, color = Color(0xFFF3DDA6), fontWeight = FontWeight.Bold)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Zamknij", color = Color(0xFFE8C37E), fontWeight = FontWeight.Bold)
            }
        },
        containerColor = Color(0xFF100C12)
    )
}
