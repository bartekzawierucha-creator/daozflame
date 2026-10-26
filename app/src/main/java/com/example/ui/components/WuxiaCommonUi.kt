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
import com.example.ui.aaa.Aaa
import com.example.ui.aaa.AaaTag
import com.example.ui.aaa.AaaTopHud
import com.example.ui.aaa.AaaType
import com.example.ui.aaa.aaaFrame
import androidx.compose.ui.graphics.lerp

/**
 * Karta AAA – zachowuje dotychczasowe API, ale renderuje się jako rama lakowa
 * ze szczotkowanym złotem i okuciami. Jaskrawe żółcie starego motywu są
 * automatycznie mapowane na antyczne złoto; kolory znaczeniowe (żywioł,
 * zagrożenie) pozostają akcentem ramy.
 */
@Composable
fun WuxiaCard(
    modifier: Modifier = Modifier,
    borderColor: Color = DaoGold,
    backgroundColor: Color = Color(0xFF141017),
    content: @Composable ColumnScope.() -> Unit
) {
    val accent = aaaAccentFor(borderColor)
    val inactive = borderColor == InkCardBorder
    Column(
        modifier = modifier
            .aaaFrame(
                accent = accent,
                fill = Brush.verticalGradient(
                    listOf(lerp(Aaa.Ink3, backgroundColor, 0.25f), Aaa.Ink2, Aaa.Ink1)
                ),
                ornaments = !inactive,
                glowStrength = if (inactive) 0.3f else 1f
            )
            .padding(14.dp),
        content = content
    )
}

/** Mapuje kolory starego motywu na paletę AAA. */
fun aaaAccentFor(c: Color): Color {
    if (c == InkCardBorder) return Aaa.GoldDeep.copy(alpha = 0.8f)
    val isOldGold = c.red > 0.85f && c.green > 0.55f && c.green < 0.92f && c.blue < 0.35f
    return if (isOldGold) Aaa.Gold.copy(alpha = c.alpha) else c
}

@Composable
fun WuxiaBadge(
    text: String,
    containerColor: Color = Color(0xFF241C22),
    contentColor: Color = TextPrimary,
    leadingEmoji: String? = null
) {
    val tone = if (contentColor == TextPrimary) Aaa.TextHi else aaaAccentFor(contentColor)
    Box(Modifier.padding(2.dp)) {
        AaaTag(text = text, color = tone, leading = leadingEmoji)
    }
}

/**
 * Górny HUD sekty – deleguje do [AaaTopHud].
 */
@Composable
fun SectHeaderBar(
    sectName: String,
    currentDay: Int,
    timeOfDay: TimeOfDay,
    spiritStones: Int,
    sectReputation: Int,
    gameSpeed: Int,
    isMuted: Boolean,
    onToggleMute: () -> Unit,
    onSpeedChange: (Int) -> Unit
) {
    AaaTopHud(
        sectName = sectName,
        currentDay = currentDay,
        timeOfDay = timeOfDay,
        spiritStones = spiritStones,
        sectReputation = sectReputation,
        gameSpeed = gameSpeed,
        isMuted = isMuted,
        onToggleMute = onToggleMute,
        onSpeedChange = onSpeedChange
    )
}

@Composable
fun GildedResourcePill(
    iconText: String,
    label: String,
    value: String,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    val accent = aaaAccentFor(accentColor)
    Row(
        modifier = modifier
            .heightIn(min = 42.dp)
            .aaaFrame(
                accent = accent,
                fill = Brush.horizontalGradient(listOf(accent.copy(alpha = 0.16f), Aaa.Ink2, Aaa.Ink1)),
                chamfer = 8.dp,
                elevation = 2.dp,
                ornaments = false,
                glowStrength = 0.4f
            )
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(Brush.radialGradient(listOf(accent.copy(alpha = 0.30f), Color.Transparent))),
            contentAlignment = Alignment.Center
        ) {
            Text(text = iconText, fontSize = 14.sp)
        }
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            Text(text = value, style = AaaType.Numeric.copy(color = accent, fontSize = 14.sp))
            Text(text = label, style = AaaType.Label.copy(fontSize = 9.sp, color = Aaa.TextLow))
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
