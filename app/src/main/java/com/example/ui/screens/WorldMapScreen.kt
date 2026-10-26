package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Warning
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
import com.example.game.WuxiaUiState
import com.example.model.Disciple
import com.example.model.EnvironmentalHazard
import com.example.model.WorldRegion
import com.example.ui.audio.WuxiaAudioEngine
import com.example.ui.components.QiProgressBar
import com.example.ui.components.WuxiaBadge
import com.example.ui.components.WuxiaCard
import com.example.ui.graphics.*
import com.example.ui.pixelart.*
import com.example.ui.theme.*
import com.example.ui.aaa.*

import com.example.model.TerritoryNode

@Composable
fun WorldMapScreen(
    viewModel: WuxiaGameViewModel,
    uiState: WuxiaUiState,
    modifier: Modifier = Modifier,
    showCombatOverlay: Boolean = true
) {
    var selectedWorldTab by remember { mutableStateOf(0) } // 0: Regiony, 1: Terytoria Sekty
    var selectedRegionForExpedition by remember { mutableStateOf<WorldRegion?>(null) }
    var selectedTerritoryForAction by remember { mutableStateOf<TerritoryNode?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp)
    ) {
        Spacer(modifier = Modifier.height(10.dp))

        AaaSegmentedTabs(
            items = listOf("Regiony i Wyprawy", "Terytoria"),
            selectedIndex = selectedWorldTab,
            onSelect = { idx ->
                if (selectedWorldTab != idx) {
                    WuxiaAudioEngine.playDivineGong()
                    selectedWorldTab = idx
                }
            },
            testTags = listOf("world_tab_regions", "world_tab_territories")
        )

        Spacer(modifier = Modifier.height(10.dp))

        if (selectedWorldTab == 0) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                item {
                    ManhwaHeroBanner(
                        assetName = "bg_combat_arena",
                        title = "Bezkresne Krainy i Ziemie Kultywacji",
                        subtitle = "Eksploruj proceduralne biomy, poluj na mityczne bestie i pozyskuj legendarne surowce",
                        height = 145.dp
                    )
                }

                item {
                    PixelWorldTilemap(
                        regions = uiState.regions,
                        selectedRegionId = selectedRegionForExpedition?.id,
                        onSelectRegion = { region ->
                            selectedRegionForExpedition = region
                        }
                    )
                }

                items(uiState.regions) { region ->
                    RegionCard(
                        region = region,
                        onSelectExpedition = { selectedRegionForExpedition = region }
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                item {
                    ManhwaHeroBanner(
                        assetName = "bg_cultivation_peak",
                        title = "Węzły Terytorialne Sekty",
                        subtitle = "Kontroluj żyły duchowe, doliny alchemiczne i wulkany, by czerpać pasywny dochód co cykl",
                        height = 145.dp
                    )
                }

                item {
                    val playerOwnedCount = uiState.territories.count { it.controllingSectId == "player" }
                    WuxiaCard(
                        borderColor = DaoGold,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "🚩 Jurysdykcja Sekty Niebiańskiego Smoka",
                                color = TextGold,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                            WuxiaBadge(
                                text = "$playerOwnedCount / ${uiState.territories.size} Węzłów",
                                containerColor = DaoGold.copy(alpha = 0.2f),
                                contentColor = DaoGoldLight
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Każdy kontrolowany węzeł zasila magazyn sekty surowcami w czasie rzeczywistym. Wrogie frakcje mogą najeżdżać słabo chronione posterunki!",
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                    }
                }

                items(uiState.territories) { territory ->
                    TerritoryNodeCard(
                        territory = territory,
                        disciples = uiState.disciples,
                        onAction = { selectedTerritoryForAction = territory }
                    )
                }
            }
        }
    }

    // Modal: Select Disciple for Expedition
    if (selectedRegionForExpedition != null) {
        val region = selectedRegionForExpedition!!
        AlertDialog(
            onDismissRequest = { selectedRegionForExpedition = null },
            title = {
                Text(
                    text = "Wyprawa: ${region.name}",
                    color = TextGold,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Wyzwanie środowiskowe: ${region.hazard.polishName}",
                        color = if (region.hazard != EnvironmentalHazard.NONE) VermillionRed else JadeSpiritLight,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Zalecenie: ${region.hazard.counterMeasure}",
                        color = TextMuted,
                        fontSize = 10.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Wybierz adepta do poprowadzenia ekspedycji:",
                        color = TextPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )

                    LazyColumn(
                        modifier = Modifier.heightIn(max = 240.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(uiState.disciples) { disciple ->
                            Surface(
                                color = InkSurfaceVariant,
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        viewModel.startExpedition(region.id, disciple.id)
                                        selectedRegionForExpedition = null
                                    }
                                    .testTag("send_${disciple.id}")
                            ) {
                                Row(
                                    modifier = Modifier.padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        ManhwaSchoolAvatar(disciple.martialSchool, size = 38.dp)
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text(text = disciple.name, color = TextGold, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                            Text(text = "${disciple.realm.polishName} • Atak: ${disciple.attack}", color = TextSecondary, fontSize = 10.sp)
                                        }
                                    }
                                    Text("Wybierz", color = JadeSpiritLight, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { selectedRegionForExpedition = null }) {
                    Text("Anuluj", color = TextSecondary)
                }
            },
            containerColor = InkSurface
        )
    }

    // Modal: Select Disciple for Territory Action
    if (selectedTerritoryForAction != null) {
        val terr = selectedTerritoryForAction!!
        val isPlayer = terr.controllingSectId == "player"

        AlertDialog(
            onDismissRequest = { selectedTerritoryForAction = null },
            title = {
                Text(
                    text = if (isPlayer) "Obsadź Garnizon: ${terr.name}" else "Wyprawa Wojenna: ${terr.name}",
                    color = TextGold,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = if (isPlayer) "Wybierz adepta, który będzie strzegł tego węzła i odpierał najazdy:" else "Wybierz czempiona do pokonania garnizonu ${terr.controllingSectName} (Obrona: ${terr.garrisonDefensePower}):",
                        color = TextSecondary,
                        fontSize = 11.sp
                    )
                    LazyColumn(
                        modifier = Modifier.heightIn(max = 240.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(uiState.disciples) { disciple ->
                            val combatPower = disciple.attack * 8 + disciple.comprehension * 3
                            Surface(
                                color = InkSurfaceVariant,
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        if (isPlayer) {
                                            viewModel.assignGarrison(terr.id, disciple.id)
                                        } else {
                                            viewModel.captureTerritory(terr.id, disciple.id)
                                        }
                                        selectedTerritoryForAction = null
                                    }
                                    .testTag("select_terr_disciple_${disciple.id}")
                            ) {
                                Row(
                                    modifier = Modifier.padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text(text = disciple.name, color = TextGold, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        Text(text = "${disciple.realm.polishName} • Siła: $combatPower CP", color = AstralCyanLight, fontSize = 10.sp)
                                    }
                                    Text(
                                        text = if (isPlayer) "Obsadź" else "Atakuj",
                                        color = if (isPlayer) JadeSpiritLight else VermillionRed,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { selectedTerritoryForAction = null }) {
                    Text("Anuluj", color = TextSecondary)
                }
            },
            containerColor = InkSurface
        )
    }

    // Active Combat Dialog
    if (showCombatOverlay && uiState.combatState.active) {
        CombatScreenModal(
            combatState = uiState.combatState,
            onManualAttack = { viewModel.combatManualAttack() },
            onUsePill = { viewModel.combatUsePill() },
            onUltimate = { viewModel.combatUltimate() },
            onClose = { viewModel.closeCombat() }
        )
    }
}

@Composable
fun TerritoryNodeCard(
    territory: TerritoryNode,
    disciples: List<Disciple>,
    onAction: () -> Unit
) {
    val isPlayer = territory.controllingSectId == "player"
    val assignedDisciple = disciples.find { it.id == territory.assignedDiscipleId }

    WuxiaCard(
        borderColor = if (isPlayer) JadeSpiritLight else VermillionRed,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(text = territory.name, color = TextGold, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Text(text = "Typ: ${territory.regionType}", color = AstralCyanLight, fontSize = 10.sp)
                }
                WuxiaBadge(
                    text = if (isPlayer) "TWOJA SEKTA" else territory.controllingSectName,
                    containerColor = if (isPlayer) JadeSpiritPrimary.copy(alpha = 0.25f) else VermillionRed.copy(alpha = 0.25f),
                    contentColor = if (isPlayer) JadeSpiritLight else VermillionRed
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Surface(
                color = InkSurfaceVariant,
                shape = RoundedCornerShape(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "💎 Przychód: +${territory.yieldAmountPerCycle}x ${territory.resourceType} / cykl",
                        color = JadeSpiritLight,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Obrona: ${territory.garrisonDefensePower}",
                        color = DaoGoldLight,
                        fontSize = 11.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Posterunek: ${assignedDisciple?.let { "${it.name} (${it.realm.polishName})" } ?: "Brak garnizonu"}",
                    color = TextSecondary,
                    fontSize = 10.sp
                )
                Button(
                    onClick = onAction,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isPlayer) DaoGoldDark else Color(0xFFDC2626)
                    ),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier.testTag("territory_action_${territory.id}")
                ) {
                    Text(
                        text = if (isPlayer) "Obsadź" else "⚔️ Podbij Węzeł",
                        fontSize = 10.sp,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun RegionCard(
    region: WorldRegion,
    onSelectExpedition: () -> Unit
) {
    val beastImg = region.bossBeast?.let { rememberWuxiaGraphic(WuxiaAssetLoader.getBeastAvatarKey(it.name)) }
    val isDangerous = region.dangerLevel > 5

    WuxiaCard(
        borderColor = if (isDangerous) Color(0xFFFF5252) else Color(0xFFE8C37E),
        backgroundColor = Color(0xFF16111A),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Beast Portrait or Biome Emblem
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF1F142D))
                    .border(
                        1.5.dp,
                        if (isDangerous) Color(0xFFFF5252) else Color(0xFFE8C37E),
                        RoundedCornerShape(12.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (beastImg != null) {
                    androidx.compose.foundation.Image(
                        bitmap = beastImg,
                        contentDescription = region.bossBeast?.name,
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Text(text = region.biome.emoji, fontSize = 32.sp)
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
                        text = region.name,
                        color = Color(0xFFF6E6BE),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Black
                    )
                    Surface(
                        color = if (isDangerous) Color(0xFF831843) else Color(0xFF451A03),
                        shape = RoundedCornerShape(6.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, if (isDangerous) Color(0xFFFF5252) else Color(0xFFE8C37E))
                    ) {
                        Text(
                            text = "POZ. ${region.dangerLevel}",
                            color = if (isDangerous) Color(0xFFFCA5A5) else Color(0xFFE8C37E),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Black,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Text(
                    text = "Biom: ${region.biome.polishName}",
                    color = Color(0xFF00E5FF),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )

                if (region.hazard != EnvironmentalHazard.NONE) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(vertical = 2.dp)
                    ) {
                        Icon(Icons.Filled.Warning, contentDescription = null, tint = Color(0xFFFF5252), modifier = Modifier.size(13.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Zagrożenie: ${region.hazard.polishName}",
                            color = Color(0xFFFCA5A5),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = "Surowce: 🌿 ${region.herbResource} • 💎 ${region.mineralResource}",
                    color = Color(0xFFEDE4CF),
                    fontSize = 10.sp
                )

                if (region.bossBeast != null) {
                    Text(
                        text = "Mityczna Bestia: ${region.bossBeast.emoji} ${region.bossBeast.name}",
                        color = Color(0xFFE8C37E),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                QiProgressBar(
                    progressPercent = region.explorationProgress,
                    label = "Zbadano krainę",
                    barColor = Color(0xFF00E5FF)
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Button(
            onClick = {
                WuxiaAudioEngine.playSwordClash()
                onSelectExpedition()
            },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("expedition_${region.id}"),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF9A6B2C)),
            shape = RoundedCornerShape(8.dp)
        ) {
            Icon(Icons.Filled.Explore, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color(0xFFE8C37E))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Wyślij Adepta na Ekspedycję", color = Color(0xFFF3DDA6), fontSize = 12.sp, fontWeight = FontWeight.Black)
        }
    }
}

@Composable
fun CombatScreenModal(
    combatState: com.example.model.CombatState,
    onManualAttack: () -> Unit,
    onUsePill: () -> Unit,
    onUltimate: () -> Unit,
    onClose: () -> Unit
) {
    AlertDialog(
        onDismissRequest = {
            if (combatState.isVictory != null) onClose()
        },
        title = null,
        text = {
            Wuxia2dCombatArena(
                combatState = combatState,
                onManualAttack = onManualAttack,
                onUsePill = onUsePill,
                onUltimate = onUltimate,
                onClose = onClose,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {},
        containerColor = Color.Transparent
    )
}
