package com.example.ui.dialogs

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.model.RandomEvent
import com.example.model.RandomEventChoice
import com.example.ui.aaa.*
import com.example.ui.audio.WuxiaAudioEngine
import com.example.ui.graphics.rememberWuxiaGraphic

/**
 * Zdarzenie losowe jako kinowy "kadr przeznaczenia":
 * ilustracja pełnej szerokości z winietą, tytuł kaligraficzny, opis na zwoju
 * i karty ścieżek (Prawe Dao / Ścieżka Siły) z podglądem konsekwencji.
 */
@Composable
fun RandomEventDialog(
    event: RandomEvent,
    onChoiceSelected: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val eventAssetKey = when {
        event.title.contains("Demoniczn", ignoreCase = true) || event.title.contains("Krwi", ignoreCase = true) -> "avatar_shadow_viper"
        event.title.contains("Besti", ignoreCase = true) || event.title.contains("Smok", ignoreCase = true) -> "beast_azure_dragon"
        event.title.contains("Mistrz", ignoreCase = true) || event.title.contains("Starożytn", ignoreCase = true) -> "avatar_elemental_taoist"
        else -> "avatar_sword_immortal"
    }
    val heroBitmap = rememberWuxiaGraphic(eventAssetKey)

    val appear = remember { Animatable(0f) }
    LaunchedEffect(event) {
        appear.snapTo(0f)
        appear.animateTo(1f, spring(dampingRatio = 0.72f, stiffness = Spring.StiffnessLow))
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 18.dp, vertical = 24.dp)
                .fillMaxWidth()
                .graphicsLayer {
                    val p = appear.value
                    alpha = p.coerceIn(0f, 1f)
                    val s = 0.92f + 0.08f * p
                    scaleX = s
                    scaleY = s
                    translationY = (1f - p) * 24.dp.toPx()
                }
                .aaaFrame(
                    accent = Aaa.Gold,
                    fill = Brush.verticalGradient(listOf(Aaa.Ink2, Aaa.Ink1, Aaa.Ink0)),
                    elevation = 24.dp,
                    chamfer = 14.dp
                )
                .verticalScroll(rememberScrollState())
        ) {
            // 1. Kadr ilustracji
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(190.dp)
            ) {
                if (heroBitmap != null) {
                    Image(
                        bitmap = heroBitmap,
                        contentDescription = event.title,
                        contentScale = ContentScale.Crop,
                        alignment = Alignment.TopCenter,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                Box(
                    Modifier
                        .matchParentSize()
                        .drawBehind {
                            drawRect(
                                Brush.verticalGradient(
                                    0f to Aaa.Ink0.copy(alpha = 0.15f),
                                    0.55f to Aaa.Ink0.copy(alpha = 0.35f),
                                    1f to Aaa.Ink2
                                )
                            )
                            drawRect(
                                Brush.horizontalGradient(
                                    listOf(Aaa.Ink0.copy(alpha = 0.55f), Color.Transparent, Aaa.Ink0.copy(alpha = 0.55f))
                                )
                            )
                        }
                )
                AaaSeal(
                    char = "命",
                    size = 38.dp,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(12.dp)
                )
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Text(text = "OMEN NIEBIOS", style = AaaType.Overline.copy(color = Aaa.Cinnabar.copy(alpha = 0.95f)))
                    Text(text = event.title, style = AaaType.Display.copy(fontSize = 21.sp))
                }
            }

            Column(
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // 2. Opis na zwoju z pionowym złotym prowadnikiem
                Row(Modifier.height(IntrinsicSize.Min)) {
                    Box(
                        Modifier
                            .width(2.dp)
                            .fillMaxHeight()
                            .drawBehind {
                                drawRect(Brush.verticalGradient(listOf(Aaa.Gold, Aaa.Gold.copy(alpha = 0f))))
                            }
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = event.description,
                        style = AaaType.Body.copy(fontSize = 13.sp, lineHeight = 19.sp, color = Aaa.TextHi.copy(alpha = 0.92f))
                    )
                }

                AaaDivider()

                Text(
                    text = "WYBIERZ ŚCIEŻKĘ PRZEZNACZENIA",
                    style = AaaType.Overline,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )

                // 3. Karty ścieżek
                event.choices.forEachIndexed { index, choice ->
                    val ruthless = isRuthlessChoice(choice)
                    DestinyPathCard(
                        choice = choice,
                        ruthless = ruthless,
                        testTag = "event_choice_$index",
                        onClick = {
                            if (ruthless) WuxiaAudioEngine.playSwordClash() else WuxiaAudioEngine.playDivineGong()
                            onChoiceSelected(index)
                        }
                    )
                }

                Text(
                    text = "Zignoruj omen",
                    style = AaaType.Label.copy(color = Aaa.TextLow, fontSize = 11.sp),
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            role = Role.Button
                        ) { onDismiss() }
                        .padding(8.dp)
                        .testTag("event_dismiss")
                )
            }
        }
    }
}

private fun isRuthlessChoice(choice: RandomEventChoice): Boolean =
    listOf("Zabierz", "Siłą", "Zniszcz", "Atak").any { choice.text.contains(it, ignoreCase = true) }

@Composable
private fun DestinyPathCard(
    choice: RandomEventChoice,
    ruthless: Boolean,
    testTag: String,
    onClick: () -> Unit
) {
    val accent = if (ruthless) Aaa.Cinnabar else Aaa.Jade
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val press = remember { Animatable(1f) }
    LaunchedEffect(pressed) { press.animateTo(if (pressed) 0.97f else 1f, tween(120)) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = press.value
                scaleY = press.value
            }
            .aaaFrame(
                accent = accent,
                fill = Brush.horizontalGradient(listOf(accent.copy(alpha = 0.20f), Aaa.Ink2, Aaa.Ink1)),
                chamfer = 9.dp,
                elevation = 3.dp,
                glowStrength = 0.6f
            )
            .drawWithContent {
                drawContent()
                if (pressed) {
                    drawRect(accent.copy(alpha = 0.10f))
                }
            }
            .clickable(interactionSource = interaction, indication = null, role = Role.Button, onClick = onClick)
            .testTag(testTag)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AaaSeal(
            char = if (ruthless) "殺" else "仁",
            size = 32.dp,
            color = if (ruthless) Aaa.Cinnabar else Aaa.JadeDeep
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = if (ruthless) "ŚCIEŻKA SIŁY" else "PRAWDZIWE DAO",
                style = AaaType.Overline.copy(color = accent, fontSize = 9.sp)
            )
            Text(
                text = choice.text,
                style = AaaType.Body.copy(fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            )
            val hasStones = choice.stoneChange != 0
            val hasRep = choice.repChange != 0
            val item = choice.rewardItem
            if (hasStones || hasRep || item != null) {
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (hasStones) {
                        ConsequenceChip(AaaGlyph.SPIRIT_STONE, choice.stoneChange)
                    }
                    if (hasRep) {
                        ConsequenceChip(AaaGlyph.REPUTATION, choice.repChange)
                    }
                    if (item != null) {
                        AaaTag(text = item, color = Aaa.Gold, leading = "🎁")
                    }
                }
            }
        }
        Spacer(Modifier.width(8.dp))
        // Strzałka-romb kierunku
        Box(
            Modifier
                .size(14.dp)
                .drawBehind {
                    val c = center
                    val r = size.minDimension / 2f
                    val p = androidx.compose.ui.graphics.Path().apply {
                        moveTo(c.x - r * 0.4f, c.y - r)
                        lineTo(c.x + r * 0.6f, c.y)
                        lineTo(c.x - r * 0.4f, c.y + r)
                    }
                    drawPath(p, accent, style = Stroke(width = 1.6.dp.toPx()))
                }
        )
    }
}

@Composable
private fun ConsequenceChip(glyph: AaaGlyph, delta: Int) {
    val positive = delta > 0
    val color = if (positive) Aaa.Jade else Aaa.Cinnabar
    Row(
        modifier = Modifier
            .drawBehind {
                drawRect(color.copy(alpha = 0.12f))
                drawLine(color.copy(alpha = 0.6f), Offset(0f, size.height), Offset(size.width, size.height), 1.dp.toPx())
            }
            .padding(horizontal = 6.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AaaGlyphIcon(glyph, Modifier.size(12.dp))
        Spacer(Modifier.width(4.dp))
        Text(
            text = if (positive) "+$delta" else "$delta",
            style = AaaType.Numeric.copy(fontSize = 11.sp, color = color)
        )
    }
}
