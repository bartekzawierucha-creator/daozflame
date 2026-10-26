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
import androidx.compose.material.icons.filled.Handshake
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
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
import com.example.game.WuxiaUiState
import com.example.model.DiplomaticStatus
import com.example.model.RivalSect
import com.example.ui.audio.WuxiaAudioEngine
import com.example.ui.components.WuxiaBadge
import com.example.ui.components.WuxiaCard
import com.example.ui.graphics.WuxiaAssetLoader
import com.example.ui.graphics.rememberWuxiaGraphic
import com.example.ui.theme.*

import com.example.model.SectPolicy

@Composable
fun DiplomacyScreen(
    viewModel: WuxiaGameViewModel,
    uiState: WuxiaUiState,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(top = 10.dp, bottom = 24.dp)
    ) {
        item {
            WuxiaCard(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "⚖️ Wielka Rada Patriarchów i Dyplomacja Frakcji",
                    color = TextGold,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Układ sił w świecie kultywacji kształtuje los twoich uczniów. Wybierz politykę sekty, zawieraj sojusze zyskując unikalne bonusy kultywacyjne lub tocz wojny o terytoria duchowe.",
                    color = TextSecondary,
                    fontSize = 11.sp,
                    lineHeight = 15.sp
                )
            }
        }

        // 1. Doktryna Polityczna Naszej Sekty (Sect Policy Selector)
        item {
            SectPolicySelectorCard(
                currentPolicy = uiState.currentPolicy,
                onSelectPolicy = { policy ->
                    WuxiaAudioEngine.playDivineGong()
                    viewModel.changeSectPolicy(policy)
                }
            )
        }

        // 2. Aktywne Bonusy Kultywacyjne Przymierzy (Alliance Buffs Summary)
        item {
            val activeAlliances = uiState.rivalSects.filter { it.status == DiplomaticStatus.ALLIANCE }
            WuxiaCard(
                borderColor = if (activeAlliances.isNotEmpty()) JadeSpiritLight else InkCardBorder,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "✨ Rezonans Przymierzy Kultywacyjnych",
                        color = TextGold,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    WuxiaBadge(
                        text = if (activeAlliances.isNotEmpty()) "${activeAlliances.size} Aktywne Sojusze" else "Brak Sojuszy",
                        containerColor = if (activeAlliances.isNotEmpty()) JadeSpiritPrimary.copy(alpha = 0.25f) else InkSurfaceVariant,
                        contentColor = if (activeAlliances.isNotEmpty()) JadeSpiritLight else TextSecondary
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                if (activeAlliances.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        activeAlliances.forEach { sect ->
                            Text(
                                text = "• ${sect.name}: ${sect.cultivationBuffName} (+${sect.cultivationBuffBonusPercent}% Qi)",
                                color = JadeSpiritLight,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                } else {
                    Text(
                        text = "Zawrzyj wieczysty sojusz z bratnimi sektami, by odblokować unikalne bonusy do medytacji i przełomów Qi dla wszystkich swoich adeptów.",
                        color = TextSecondary,
                        fontSize = 10.sp
                    )
                }
            }
        }

        items(uiState.rivalSects) { sect ->
            RivalSectCard(
                sect = sect,
                currentStones = uiState.spiritStones,
                onSendGift = {
                    WuxiaAudioEngine.playDivineGong()
                    viewModel.interactRivalSect(sect.id, "GIFT")
                },
                onProposeAlliance = {
                    WuxiaAudioEngine.playDivineGong()
                    viewModel.interactRivalSect(sect.id, "ALLIANCE")
                },
                onTrade = {
                    WuxiaAudioEngine.playDivineGong()
                    viewModel.interactRivalSect(sect.id, "TRADE")
                },
                onDeclareWar = {
                    WuxiaAudioEngine.playSwordClash()
                    viewModel.interactRivalSect(sect.id, "DECLARE_WAR")
                }
            )
        }
    }
}

@Composable
fun RivalSectCard(
    sect: RivalSect,
    currentStones: Int,
    onSendGift: () -> Unit,
    onProposeAlliance: () -> Unit,
    onTrade: () -> Unit,
    onDeclareWar: () -> Unit
) {
    val statusColor = Color(sect.status.colorCode)

    val leaderAssetKey = when {
        sect.alignment.contains("Praw", ignoreCase = true) || sect.name.contains("Miecza", ignoreCase = true) -> "avatar_sword_immortal"
        sect.alignment.contains("Demoniczn", ignoreCase = true) || sect.name.contains("Krwi", ignoreCase = true) -> "avatar_shadow_viper"
        sect.name.contains("Alchemii", ignoreCase = true) || sect.specialty.contains("Pigułk", ignoreCase = true) -> "avatar_elemental_taoist"
        else -> "avatar_body_asura"
    }
    val leaderBitmap = rememberWuxiaGraphic(leaderAssetKey)

    WuxiaCard(
        borderColor = if (sect.status == DiplomaticStatus.TOTAL_WAR) VermillionRed else if (sect.status == DiplomaticStatus.ALLIANCE) JadeSpiritLight else DaoGold,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Portret Patriarchy Obcej Sekty
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .border(1.5.dp, statusColor, RoundedCornerShape(10.dp))
                    .background(Color.Black)
            ) {
                if (leaderBitmap != null) {
                    Image(
                        bitmap = leaderBitmap,
                        contentDescription = sect.leaderName,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = sect.name, color = TextGold, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    WuxiaBadge(
                        text = sect.status.polishName,
                        containerColor = statusColor.copy(alpha = 0.25f),
                        contentColor = statusColor
                    )
                }
                Text(text = "Patriarcha: ${sect.leaderName} • ${sect.alignment}", color = TextSecondary, fontSize = 11.sp)
                Text(text = "Doktryna: ${sect.policy.polishName}", color = TextGold, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                Text(text = "Specjalność: ${sect.specialty}", color = AstralCyanLight, fontSize = 10.sp)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Bonus Kultywacyjny Sekty
        Surface(
            color = if (sect.status == DiplomaticStatus.ALLIANCE) JadeSpiritPrimary.copy(alpha = 0.15f) else InkSurfaceVariant,
            shape = RoundedCornerShape(8.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, if (sect.status == DiplomaticStatus.ALLIANCE) JadeSpiritLight else DaoGoldDark.copy(alpha = 0.3f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "📜 Bonus Kultywacyjny: ${sect.cultivationBuffName}",
                        color = if (sect.status == DiplomaticStatus.ALLIANCE) JadeSpiritLight else TextGold,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${sect.cultivationBuffDescription} (+${sect.cultivationBuffBonusPercent}% Qi)",
                        color = TextSecondary,
                        fontSize = 10.sp
                    )
                }
                if (sect.status == DiplomaticStatus.ALLIANCE) {
                    WuxiaBadge(
                        text = "AKTYWNY",
                        containerColor = JadeSpiritPrimary,
                        contentColor = Color.Black
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Relations & Territories
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Relacje: ${sect.relations} / 100",
                color = if (sect.relations >= 0) JadeSpiritLight else VermillionRed,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "🚩 Terytoria: ${sect.controlledTerritoryIds.size}",
                color = AstralCyanLight,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Active Trade Caravan Offer
        if (sect.activeTrade != null) {
            Surface(
                color = InkSurfaceVariant,
                shape = RoundedCornerShape(8.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, DaoGoldLight.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.LocalShipping, contentDescription = null, tint = DaoGoldLight, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Column {
                            Text(text = "Karawana Handlowa", color = TextGold, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Text(text = sect.activeTrade.offerText, color = TextSecondary, fontSize = 10.sp)
                        }
                    }

                    Button(
                        onClick = onTrade,
                        enabled = currentStones >= sect.activeTrade.costStones,
                        colors = ButtonDefaults.buttonColors(containerColor = DaoGoldDark),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        modifier = Modifier.testTag("trade_${sect.id}")
                    ) {
                        Text("Kup", fontSize = 10.sp, color = TextGold, fontWeight = FontWeight.Bold)
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        // Action Buttons Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            OutlinedButton(
                onClick = onSendGift,
                modifier = Modifier
                    .weight(1f)
                    .testTag("gift_${sect.id}"),
                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp)
            ) {
                Text("Dary (100 💎)", fontSize = 10.sp, color = JadeSpiritLight)
            }

            if (sect.status != DiplomaticStatus.ALLIANCE) {
                Button(
                    onClick = onProposeAlliance,
                    colors = ButtonDefaults.buttonColors(containerColor = JadeSpiritPrimary),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("alliance_${sect.id}"),
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp)
                ) {
                    Text("Sojusz", fontSize = 10.sp, color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }

            if (sect.status != DiplomaticStatus.TOTAL_WAR) {
                OutlinedButton(
                    onClick = onDeclareWar,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = VermillionRed),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("war_${sect.id}"),
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp)
                ) {
                    Text("Wojna", fontSize = 10.sp, color = VermillionRed)
                }
            }
        }
    }
}

@Composable
fun SectPolicySelectorCard(
    currentPolicy: SectPolicy,
    onSelectPolicy: (SectPolicy) -> Unit
) {
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
                text = "📜 Doktryna Pałacowa Sekty",
                color = TextGold,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            WuxiaBadge(
                text = currentPolicy.polishName,
                containerColor = DaoGold.copy(alpha = 0.2f),
                contentColor = DaoGoldLight
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Filozofia: ${currentPolicy.philosophy}",
            color = TextSecondary,
            fontSize = 11.sp
        )
        Text(
            text = "Efekt Pałacowy: ${currentPolicy.buffDescription}",
            color = JadeSpiritLight,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = "Wybierz Dekret Doktrynalny:",
            color = TextSecondary,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium
        )
        Spacer(modifier = Modifier.height(6.dp))

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            SectPolicy.values().forEach { policy ->
                val isSelected = policy == currentPolicy
                Surface(
                    color = if (isSelected) DaoGoldDark.copy(alpha = 0.35f) else InkSurfaceVariant,
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (isSelected) DaoGold else InkCardBorder
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelectPolicy(policy) }
                        .testTag("policy_${policy.name}")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = policy.polishName,
                                color = if (isSelected) TextGold else TextPrimary,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                            Text(
                                text = policy.buffDescription,
                                color = if (isSelected) JadeSpiritLight else TextSecondary,
                                fontSize = 10.sp
                            )
                        }
                        if (isSelected) {
                            Text(
                                text = "✓ AKTYWNA",
                                color = DaoGoldLight,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

