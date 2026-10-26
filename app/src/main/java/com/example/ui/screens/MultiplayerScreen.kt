package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.SportsKabaddi
import androidx.compose.material.icons.filled.Wifi
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
import com.example.model.MultiplayerSectRanking
import com.example.ui.components.WuxiaBadge
import com.example.ui.components.WuxiaCard
import com.example.ui.graphics.*
import com.example.ui.theme.*
import com.example.ui.aaa.*

@Composable
fun MultiplayerScreen(
    viewModel: WuxiaGameViewModel,
    uiState: WuxiaUiState,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableStateOf(0) } // 0: Niebiańska Arena, 1: Rajd Bossa Świata, 2: Ranking Sekt

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp)
    ) {
        Spacer(modifier = Modifier.height(10.dp))

        // Low-latency server indicator banner
        Surface(
            color = InkSurfaceVariant,
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(JadeSpiritPrimary)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Serwer Krainy Niebios (Niskie Opóźnienie)",
                        color = TextGold,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Wifi, contentDescription = null, tint = JadeSpiritLight, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${uiState.serverPingMs} ms",
                        color = JadeSpiritLight,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        AaaSegmentedTabs(
            items = listOf("Arena PvP", "Rajd Świata", "Ranking Sekt"),
            selectedIndex = selectedTab,
            onSelect = { selectedTab = it },
            testTags = listOf("mp_tab_arena", "mp_tab_raid", "mp_tab_ranking")
        )

        Spacer(modifier = Modifier.height(12.dp))

        when (selectedTab) {
            0 -> ArenaTabContent(uiState, viewModel)
            1 -> WorldBossTabContent(uiState, viewModel)
            2 -> RankingsTabContent(uiState)
        }
    }
}

@Composable
fun ArenaTabContent(
    uiState: WuxiaUiState,
    viewModel: WuxiaGameViewModel
) {
    val strongestDisciple = uiState.disciples.maxByOrNull { it.attack }

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        item {
            ManhwaHeroBanner(
                assetName = "bg_combat_arena",
                title = "Niebiańska Arena Pojedynków",
                subtitle = "Rywalizacja o Dominację • Ranking Mistrzów Krainy",
                height = 150.dp
            )
            Spacer(modifier = Modifier.height(8.dp))
            WuxiaCard(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Walki Czempionów Sekt",
                    color = TextGold,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Wyślij najsilniejszego czempiona na pojedynek z elitą innych wielkich sekt. Zwycięstwo podnosi rangę kultywacyjną.",
                    color = TextSecondary,
                    fontSize = 11.sp
                )
                if (strongestDisciple != null) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Twój wyznaczony Czempion: ${strongestDisciple.name} (Atak: ${strongestDisciple.attack}, ${strongestDisciple.realm.polishName})",
                        color = JadeSpiritLight,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        items(uiState.multiplayerRankings.filter { it.sectName != "Twoja Sekta (Lider)" }) { rival ->
            WuxiaCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "Ranga #${rival.rank}", color = DaoGoldLight, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = rival.sectName, color = TextGold, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                        Text(text = "Patriarcha: ${rival.grandmasterName} • ${rival.realmRegion}", color = TextSecondary, fontSize = 10.sp)
                        Text(text = "Moc Bojowa: ${rival.totalPower / 1000}k", color = AstralCyanLight, fontSize = 10.sp)
                    }

                    Button(
                        onClick = {
                            if (strongestDisciple != null) {
                                viewModel.fightArenaDuel(strongestDisciple.id, rival)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = DaoGoldDark),
                        modifier = Modifier.testTag("duel_${rival.rank}")
                    ) {
                        Icon(Icons.Filled.SportsKabaddi, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Pojedynek", fontSize = 11.sp, color = TextGold, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun WorldBossTabContent(
    uiState: WuxiaUiState,
    viewModel: WuxiaGameViewModel
) {
    val boss = uiState.worldBoss
    val strongestDisciple = uiState.disciples.maxByOrNull { it.attack }

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        item {
            val dragonImg = rememberWuxiaGraphic("beast_azure_dragon")
            WuxiaCard(
                modifier = Modifier.fillMaxWidth(),
                borderColor = VermillionRed.copy(alpha = 0.8f)
            ) {
                if (dragonImg != null) {
                    androidx.compose.foundation.Image(
                        bitmap = dragonImg,
                        contentDescription = boss.name,
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .border(1.5.dp, Color(0xFF00E5FF), RoundedCornerShape(10.dp))
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (dragonImg == null) {
                        Text(text = "🐲", fontSize = 34.sp)
                        Spacer(modifier = Modifier.width(12.dp))
                    }
                    Column {
                        Text(text = boss.name, color = VermillionRed, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                        Text(text = boss.title, color = TextGold, fontSize = 12.sp)
                        Text(text = "Uczestniczy: ${boss.participatingSectsCount} zjednoczonych sekt • Zostało: ${boss.hoursRemaining}h", color = TextSecondary, fontSize = 10.sp)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Global Boss HP
                Text(
                    text = "Łączne Zdrowie Bossa: ${(boss.currentHp / 1000)}k / ${(boss.maxHp / 1000)}k HP",
                    color = TextPrimary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(4.dp))
                LinearProgressIndicator(
                    progress = { boss.currentHp.toFloat() / boss.maxHp.toFloat().coerceAtLeast(1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                        .clip(CircleShape),
                    color = VermillionRed,
                    trackColor = InkCardBorder
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Twój wkład w zniszczenie bestii: ${boss.playerContributionDamage} obrażeń",
                    color = DaoGoldLight,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = {
                        if (strongestDisciple != null) {
                            viewModel.attackWorldBoss(strongestDisciple.id)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("attack_world_boss_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = CrimsonFlame)
                ) {
                    Icon(Icons.Filled.ElectricBolt, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Uderz Pełnią Mocy Sekty", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun RankingsTabContent(uiState: WuxiaUiState) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        items(uiState.multiplayerRankings) { ranking ->
            val isPlayer = ranking.sectName.contains("Twoja Sekta")
            Surface(
                color = if (isPlayer) DaoGoldDark.copy(alpha = 0.25f) else InkSurfaceVariant,
                shape = RoundedCornerShape(8.dp),
                border = if (isPlayer) ButtonDefaults.outlinedButtonBorder else null,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "#${ranking.rank}",
                            color = if (ranking.rank <= 3) DaoGold else TextMuted,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.width(36.dp)
                        )
                        Column {
                            Text(
                                text = ranking.sectName,
                                color = if (isPlayer) TextGold else TextPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Mistrz: ${ranking.grandmasterName} • Ping: ${ranking.pingMs}ms",
                                color = TextSecondary,
                                fontSize = 10.sp
                            )
                        }
                    }

                    Text(
                        text = "${ranking.totalPower / 1000}k Qi",
                        color = if (isPlayer) DaoGoldLight else AstralCyanLight,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
