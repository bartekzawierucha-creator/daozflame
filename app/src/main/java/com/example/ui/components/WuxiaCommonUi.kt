package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.example.ui.audio.WuxiaAudioEngine
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.Canvas
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.TimeOfDay
import com.example.ui.graphics.GildedGold
import com.example.ui.graphics.drawOrnateCorner
import com.example.ui.theme.*

@Composable
fun WuxiaCard(
    modifier: Modifier = Modifier,
    borderColor: Color = DaoGold,
    backgroundColor: Color = Color(0xFF110C1C),
    content: @Composable ColumnScope.() -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(backgroundColor)
            .border(
                width = 1.5.dp,
                brush = Brush.linearGradient(
                    colors = listOf(
                        borderColor,
                        borderColor.copy(alpha = 0.5f),
                        Color(0xFF6B4E15),
                        borderColor
                    )
                ),
                shape = RoundedCornerShape(12.dp)
            )
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val w = size.width
            val h = size.height
            val gold = if (borderColor != InkCardBorder) borderColor else GildedGold
            drawOrnateCorner(8f, 8f, isRight = false, isBottom = false, color = gold.copy(alpha = 0.75f))
            drawOrnateCorner(w - 8f, 8f, isRight = true, isBottom = false, color = gold.copy(alpha = 0.75f))
            drawOrnateCorner(8f, h - 8f, isRight = false, isBottom = true, color = gold.copy(alpha = 0.75f))
            drawOrnateCorner(w - 8f, h - 8f, isRight = true, isBottom = true, color = gold.copy(alpha = 0.75f))
        }

        Column(
            modifier = Modifier.padding(14.dp),
            content = content
        )
    }
}

@Composable
fun WuxiaBadge(
    text: String,
    containerColor: Color = Color(0xFF261A35),
    contentColor: Color = TextPrimary,
    leadingEmoji: String? = null
) {
    Surface(
        color = containerColor,
        shape = RoundedCornerShape(6.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, contentColor.copy(alpha = 0.35f)),
        modifier = Modifier.padding(2.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (leadingEmoji != null) {
                Text(text = leadingEmoji, fontSize = 11.sp, modifier = Modifier.padding(end = 4.dp))
            }
            Text(
                text = text,
                color = contentColor,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

/**
 * AAA Imperial Wuxia Game Top HUD (Gilded Dragon Lacquer Header)
 */
@Composable
fun SectHeaderBar(
    sectName: String,
    currentDay: Int,
    timeOfDay: TimeOfDay,
    spiritStones: Int,
    sectReputation: Int,
    gameSpeed: Int,
    onSpeedChange: (Int) -> Unit
) {
    val dayNightGradient = when (timeOfDay) {
        TimeOfDay.DAWN -> Brush.verticalGradient(listOf(Color(0xFF2D0F2E), Color(0xFF140D20)))
        TimeOfDay.DAY -> Brush.verticalGradient(listOf(Color(0xFF2C1E0F), Color(0xFF140D20)))
        TimeOfDay.DUSK -> Brush.verticalGradient(listOf(Color(0xFF2E1313), Color(0xFF140D20)))
        TimeOfDay.NIGHT -> Brush.verticalGradient(listOf(Color(0xFF0C1322), Color(0xFF0A0713)))
        TimeOfDay.FULL_MOON -> Brush.verticalGradient(listOf(Color(0xFF1A1336), Color(0xFF0A0713)))
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(dayNightGradient)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 8.dp)
        ) {
            // Row 1: Sect Imperial Seal & Time + Controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Imperial Red Wax Seal
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFF8B1E1E))
                            .border(1.dp, Color(0xFFFFD700), RoundedCornerShape(6.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "宗",
                            color = Color(0xFFFFE082),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Text(
                            text = sectName,
                            color = Color(0xFFFFE8A3),
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 0.5.sp
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Dzień $currentDay",
                                color = Color(0xFFE2E8F0),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(text = " • ", color = Color(0xFFFFD700), fontSize = 10.sp)
                            Text(
                                text = "${timeOfDay.symbol} ${timeOfDay.polishName}",
                                color = Color(0xFFFFD700),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Controls: Bell + Game Speed
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    var isAudioMuted by remember { mutableStateOf(WuxiaAudioEngine.isMuted) }
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF1E142B))
                            .border(1.dp, Color(0xFFFFD700).copy(alpha = 0.6f), CircleShape)
                            .clickable {
                                WuxiaAudioEngine.isMuted = !WuxiaAudioEngine.isMuted
                                isAudioMuted = WuxiaAudioEngine.isMuted
                                if (!isAudioMuted) WuxiaAudioEngine.playDivineGong()
                            }
                            .testTag("audio_toggle_btn"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(if (isAudioMuted) "🔇" else "🔔", fontSize = 13.sp)
                    }

                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF1E142B))
                            .border(1.dp, Color(0xFFFFD700).copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                            .padding(2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        listOf(1, 2, 5).forEach { speed ->
                            val isCurrent = gameSpeed == speed
                            Box(
                                modifier = Modifier
                                    .testTag("speed_button_$speed")
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isCurrent) Color(0xFFFFD700) else Color.Transparent)
                                    .clickable { onSpeedChange(speed) }
                                    .padding(horizontal = 7.dp, vertical = 3.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "${speed}x",
                                    color = if (isCurrent) Color.Black else Color(0xFFCBD5E1),
                                    fontWeight = FontWeight.Black,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Row 2: AAA Jewel Resource Cards (Kamienie Ducha & Reputacja)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                GildedResourcePill(
                    iconText = "💎",
                    label = "Kamienie Ducha",
                    value = spiritStones.toString(),
                    accentColor = Color(0xFF00E5FF),
                    modifier = Modifier.weight(1f)
                )
                GildedResourcePill(
                    iconText = "🐉",
                    label = "Reputacja Sekty",
                    value = sectReputation.toString(),
                    accentColor = Color(0xFFFFD700),
                    modifier = Modifier.weight(1f)
                )
            }

            // Atmospheric Daoist Mod Text
            Row(
                modifier = Modifier.padding(top = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(4.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF10B981))
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = timeOfDay.statModifierDesc,
                    color = Color(0xFF6EE7B7),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        // Bottom Ornate Gold Filigree Border Line
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(2.dp)
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color(0xFFFFD700).copy(alpha = 0.3f),
                            Color(0xFFFFD700),
                            Color(0xFFFFD700).copy(alpha = 0.3f),
                            Color.Transparent
                        )
                    )
                )
        )
    }
}

@Composable
fun GildedResourcePill(
    iconText: String,
    label: String,
    value: String,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        color = Color(0xFF170F24),
        shape = RoundedCornerShape(10.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, accentColor.copy(alpha = 0.45f)),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(accentColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Text(text = iconText, fontSize = 14.sp)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = value,
                    color = accentColor,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Black
                )
                Text(
                    text = label,
                    color = Color(0xFF94A3B8),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
fun QiProgressBar(
    progressPercent: Int,
    label: String = "Postęp Qi",
    barColor: Color = JadeSpiritPrimary,
    modifier: Modifier = Modifier
) {
    CalligraphicQiAuraProgressBar(
        progressPercent = progressPercent,
        label = label,
        auraColor = barColor,
        modifier = modifier
    )
}
