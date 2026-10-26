package com.example.ui.aaa

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.TimeOfDay
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

// ─────────────────────────────────────────────────────────────────────────────
//  TOP HUD
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Górny HUD: obracający się emblemat sekty, nazwa w kaligraficznym szeryfie,
 * tarcza fazy dnia, przełącznik dźwięku i tempa, zasoby z animowanym licznikiem
 * oraz pływającymi przyrostami (+N).
 */
@Composable
fun AaaTopHud(
    sectName: String,
    currentDay: Int,
    timeOfDay: TimeOfDay,
    spiritStones: Int,
    sectReputation: Int,
    gameSpeed: Int,
    isMuted: Boolean,
    onToggleMute: () -> Unit,
    onSpeedChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val accent by animateColorAsState(Aaa.phaseAccent(timeOfDay), tween(1200), label = "hud_accent")

    Column(
        modifier = modifier
            .fillMaxWidth()
            .drawBehind {
                drawRect(
                    Brush.verticalGradient(
                        listOf(Aaa.Ink0.copy(alpha = 0.94f), Aaa.Ink1.copy(alpha = 0.80f), Aaa.Ink1.copy(alpha = 0.55f))
                    )
                )
                // Dolna złota krawędź z poświatą
                val y = size.height - 1.dp.toPx()
                drawRect(
                    Brush.verticalGradient(
                        listOf(Color.Transparent, accent.copy(alpha = 0.10f)),
                        startY = size.height - 18.dp.toPx(),
                        endY = size.height
                    ),
                    topLeft = Offset(0f, size.height - 18.dp.toPx())
                )
                drawLine(Aaa.goldFade(), Offset(0f, y), Offset(size.width, y), 1.dp.toPx())
            }
            .statusBarsPadding()
            .padding(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            SectEmblem(accent = accent)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = "DZIEŃ $currentDay  ·  ${timeOfDay.polishName.uppercase()}",
                    style = AaaType.Overline.copy(color = accent.copy(alpha = 0.9f), fontSize = 9.sp, letterSpacing = 1.6.sp),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = sectName,
                    style = AaaType.Title.copy(fontSize = 17.sp),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(Modifier.width(8.dp))
            HudRoundButton(onClick = onToggleMute, testTag = "audio_toggle_btn") {
                Icon(
                    imageVector = if (isMuted) Icons.Filled.VolumeOff else Icons.Filled.VolumeUp,
                    contentDescription = if (isMuted) "Włącz dźwięk" else "Wycisz",
                    tint = if (isMuted) Aaa.TextLow else Aaa.Gold,
                    modifier = Modifier.size(17.dp)
                )
            }
            Spacer(Modifier.width(6.dp))
            SpeedSelector(gameSpeed = gameSpeed, onSpeedChange = onSpeedChange)
        }

        Spacer(Modifier.height(9.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AaaResourceChip(
                glyph = AaaGlyph.SPIRIT_STONE,
                label = "Kamienie Ducha",
                value = spiritStones,
                accent = Aaa.Azure,
                modifier = Modifier.weight(1f)
            )
            AaaResourceChip(
                glyph = AaaGlyph.REPUTATION,
                label = "Reputacja Sekty",
                value = sectReputation,
                accent = Aaa.Gold,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(Modifier.height(7.dp))

        // Modyfikator fazy nieba – "ticker" z pulsującym punktem
        Row(verticalAlignment = Alignment.CenterVertically) {
            val pulse = rememberInfiniteTransition(label = "hud_pulse")
            val p by pulse.animateFloat(
                initialValue = 0.35f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(tween(1100, easing = FastOutSlowInEasing), RepeatMode.Reverse),
                label = "hud_pulse_a"
            )
            Canvas(Modifier.size(10.dp)) {
                drawCircle(Aaa.Jade.copy(alpha = 0.25f * p), radius = size.minDimension / 2f)
                drawCircle(Aaa.Jade, radius = size.minDimension / 5f)
            }
            Spacer(Modifier.width(6.dp))
            Text(
                text = timeOfDay.statModifierDesc,
                style = AaaType.Label.copy(color = Aaa.Jade.copy(alpha = 0.9f), fontSize = 10.sp, letterSpacing = 0.3.sp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun SectEmblem(accent: Color) {
    val spin = rememberInfiniteTransition(label = "emblem")
    val angle by spin.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(36000, easing = LinearEasing)),
        label = "emblem_angle"
    )
    Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.matchParentSize()) {
            val r = size.minDimension / 2f
            drawCircle(
                Brush.radialGradient(listOf(accent.copy(alpha = 0.35f), Color.Transparent), center = center, radius = r),
                radius = r
            )
            rotate(angle) {
                // Pierścień z 24 nacięciami – bęben kalendarza dao
                for (i in 0 until 24) {
                    val a = (i * 15f) * (PI / 180.0).toFloat()
                    val len = if (i % 3 == 0) r * 0.16f else r * 0.08f
                    val r0 = r * 0.92f
                    drawLine(
                        Aaa.Gold.copy(alpha = if (i % 3 == 0) 0.95f else 0.5f),
                        Offset(center.x + cos(a) * r0, center.y + sin(a) * r0),
                        Offset(center.x + cos(a) * (r0 - len), center.y + sin(a) * (r0 - len)),
                        1.2.dp.toPx()
                    )
                }
            }
            drawCircle(Aaa.goldBevel(), radius = r * 0.70f, style = Stroke(width = 2.dp.toPx()))
            drawCircle(
                Brush.radialGradient(
                    listOf(Aaa.Cinnabar, Aaa.CinnabarDeep),
                    center = Offset(center.x - r * 0.2f, center.y - r * 0.25f),
                    radius = r * 0.8f
                ),
                radius = r * 0.62f
            )
            drawCircle(Aaa.GoldHi.copy(alpha = 0.4f), radius = r * 0.54f, style = Stroke(width = 0.8.dp.toPx()))
        }
        Text(
            text = "宗",
            color = Aaa.GoldHi,
            fontFamily = FontFamily.Serif,
            fontWeight = FontWeight.Black,
            fontSize = 18.sp
        )
    }
}

@Composable
private fun HudRoundButton(
    onClick: () -> Unit,
    testTag: String,
    content: @Composable BoxScope.() -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    Box(
        modifier = Modifier
            .size(34.dp)
            .drawBehind {
                val r = size.minDimension / 2f
                drawCircle(Brush.verticalGradient(listOf(Aaa.Ink4, Aaa.Ink1)), radius = r)
                drawCircle(Aaa.goldBevel(), radius = r - 0.6.dp.toPx(), style = Stroke(width = 1.2.dp.toPx()))
            }
            .clickable(interactionSource = interaction, indication = null, role = Role.Button, onClick = onClick)
            .testTag(testTag),
        contentAlignment = Alignment.Center,
        content = content
    )
}

@Composable
private fun SpeedSelector(gameSpeed: Int, onSpeedChange: (Int) -> Unit) {
    val speeds = listOf(1, 2, 5)
    val idx = speeds.indexOf(gameSpeed).coerceAtLeast(0)
    val pos by animateFloatAsState(idx.toFloat(), spring(dampingRatio = 0.75f, stiffness = Spring.StiffnessMedium), label = "speed_pos")
    Row(
        modifier = Modifier
            .height(34.dp)
            .drawWithCache {
                val outer = chamferPath(size.width, size.height, 7.dp.toPx())
                val pad = 3.dp.toPx()
                val segW = (size.width - pad * 2f) / speeds.size
                val segH = size.height - pad * 2f
                val seg = chamferPath(segW, segH, 5.dp.toPx())
                onDrawBehind {
                    drawPath(outer, Brush.verticalGradient(listOf(Aaa.Ink3, Aaa.Ink1)))
                    drawPath(outer, Aaa.GoldDeep, style = Stroke(width = 1.2.dp.toPx()))
                    val dx = pad + pos * segW
                    translate(left = dx, top = pad) {
                        drawPath(seg, Aaa.goldBevel())
                    }
                }
            }
            .padding(3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        speeds.forEach { s ->
            val selected = s == gameSpeed
            Box(
                modifier = Modifier
                    .width(30.dp)
                    .fillMaxHeight()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        role = Role.Button
                    ) { onSpeedChange(s) }
                    .testTag("speed_button_$s"),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "${s}×",
                    style = AaaType.Numeric.copy(
                        fontSize = 12.sp,
                        color = if (selected) Aaa.Ink0 else Aaa.TextMid,
                        fontWeight = FontWeight.Black
                    )
                )
            }
        }
    }
}

/**
 * Zasób z animowanym licznikiem (płynne przewijanie cyfr) i pływającym
 * znacznikiem zmiany (+N zielony / −N cynobrowy).
 */
@Composable
fun AaaResourceChip(
    glyph: AaaGlyph,
    label: String,
    value: Int,
    accent: Color,
    modifier: Modifier = Modifier
) {
    val shown by animateIntAsState(value, tween(650, easing = FastOutSlowInEasing), label = "res_count")
    var lastValue by remember { mutableStateOf(value) }
    var delta by remember { mutableStateOf(0) }
    val deltaAnim = remember { Animatable(1f) }
    LaunchedEffect(value) {
        val d = value - lastValue
        lastValue = value
        if (d != 0) {
            delta = d
            deltaAnim.snapTo(0f)
            deltaAnim.animateTo(1f, tween(1200, easing = LinearOutSlowInEasing))
        }
    }

    Box(modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(42.dp)
                .drawWithCache {
                    val p = chamferPath(size.width, size.height, 8.dp.toPx())
                    val fill = Brush.horizontalGradient(listOf(accent.copy(alpha = 0.16f), Aaa.Ink2.copy(alpha = 0.9f), Aaa.Ink1.copy(alpha = 0.9f)))
                    val edge = Brush.horizontalGradient(listOf(accent.copy(alpha = 0.7f), Aaa.GoldDeep.copy(alpha = 0.6f), Aaa.GoldShadow))
                    onDrawBehind {
                        drawPath(p, fill)
                        drawPath(p, edge, style = Stroke(width = 1.dp.toPx()))
                    }
                }
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AaaGlyphIcon(glyph, Modifier.size(24.dp))
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = formatGameNumber(shown),
                    style = AaaType.Numeric.copy(color = Aaa.TextHi, fontSize = 15.sp),
                    maxLines = 1
                )
                Text(
                    text = label,
                    style = AaaType.Label.copy(fontSize = 9.sp, color = Aaa.TextLow, letterSpacing = 0.4.sp),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        val prog = deltaAnim.value
        if (prog < 1f && delta != 0) {
            Text(
                text = if (delta > 0) "+${formatGameNumber(delta)}" else "−${formatGameNumber(-delta)}",
                style = AaaType.Numeric.copy(
                    fontSize = 11.sp,
                    color = if (delta > 0) Aaa.Jade else Aaa.Cinnabar
                ),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(end = 10.dp)
                    .graphicsLayer {
                        translationY = -prog * 14.dp.toPx() + 4.dp.toPx()
                        alpha = (1f - prog).coerceIn(0f, 1f)
                    }
            )
        }
    }
}

/** 1 234 / 12,3 tys. / 4,5 mln – kompaktowy zapis liczb idle-game. */
fun formatGameNumber(v: Int): String {
    val a = kotlin.math.abs(v.toLong())
    val sign = if (v < 0) "-" else ""
    return when {
        a >= 1_000_000 -> sign + String.format(java.util.Locale.ROOT, "%.1f", a / 1_000_000.0).replace('.', ',') + " mln"
        a >= 100_000 -> sign + String.format(java.util.Locale.ROOT, "%.1f", a / 1_000.0).replace('.', ',') + " tys."
        a >= 1_000 -> {
            val s = a.toString()
            sign + s.substring(0, s.length - 3) + " " + s.substring(s.length - 3)
        }
        else -> sign + a.toString()
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  BOTTOM DOCK
// ─────────────────────────────────────────────────────────────────────────────

data class AaaDockItem(
    val title: String,
    val icon: ImageVector,
    val testTag: String,
    val badge: String? = null
)

/**
 * Dolny dok: lakowa listwa, przesuwający się świetlny znacznik z rombem,
 * uniesiona aktywna ikona i plakietki powiadomień.
 */
@Composable
fun AaaDock(
    items: List<AaaDockItem>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    if (items.isEmpty()) return
    val count = items.size
    val pos by animateFloatAsState(
        targetValue = selectedIndex.toFloat(),
        animationSpec = spring(dampingRatio = 0.72f, stiffness = Spring.StiffnessMediumLow),
        label = "dock_pos"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .drawBehind {
                drawRect(Brush.verticalGradient(listOf(Aaa.Ink1.copy(alpha = 0.92f), Aaa.Ink0)))
                drawLine(Aaa.goldFade(), Offset(0f, 0f), Offset(size.width, 0f), 1.dp.toPx())
            }
            .navigationBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(66.dp)
                .drawWithCache {
                    val segW = size.width / count
                    onDrawBehind {
                        val cx = segW * (pos + 0.5f)
                        // Świetlny słup nad aktywną zakładką
                        drawRect(
                            Brush.verticalGradient(
                                listOf(Aaa.Gold.copy(alpha = 0.16f), Color.Transparent),
                                startY = 0f,
                                endY = size.height * 0.9f
                            ),
                            topLeft = Offset(cx - segW * 0.42f, 0f),
                            size = androidx.compose.ui.geometry.Size(segW * 0.84f, size.height)
                        )
                        drawCircle(
                            Brush.radialGradient(
                                listOf(Aaa.Gold.copy(alpha = 0.22f), Color.Transparent),
                                center = Offset(cx, size.height * 0.38f),
                                radius = segW * 0.45f
                            ),
                            radius = segW * 0.45f,
                            center = Offset(cx, size.height * 0.38f)
                        )
                        // Belka i romb
                        val bar = 26.dp.toPx()
                        drawLine(Aaa.goldSheen(), Offset(cx - bar, 1.dp.toPx()), Offset(cx + bar, 1.dp.toPx()), 2.dp.toPx())
                        drawStud(Offset(cx, 2.dp.toPx()), 4.dp.toPx(), Aaa.GoldHi)
                    }
                },
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEachIndexed { index, item ->
                val selected = index == selectedIndex
                val lift by animateFloatAsState(if (selected) 1f else 0f, spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessMediumLow), label = "dock_lift_$index")
                val tint by animateColorAsState(if (selected) Aaa.GoldHi else Aaa.TextLow, tween(220), label = "dock_tint_$index")

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            role = Role.Tab
                        ) { onSelect(index) }
                        .testTag(item.testTag),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .graphicsLayer {
                                translationY = -lift * 3.dp.toPx()
                                val s = 1f + lift * 0.14f
                                scaleX = s
                                scaleY = s
                            }
                    ) {
                        Icon(
                            imageVector = item.icon,
                            contentDescription = item.title,
                            tint = tint,
                            modifier = Modifier
                                .size(22.dp)
                                .align(Alignment.Center)
                        )
                        if (!item.badge.isNullOrEmpty()) {
                            Box(Modifier.align(Alignment.TopEnd).offset(x = 6.dp, y = (-3).dp)) {
                                AaaCountBadge(text = item.badge, highlighted = true)
                            }
                        }
                    }
                    Spacer(Modifier.height(3.dp))
                    Text(
                        text = item.title,
                        style = AaaType.Label.copy(
                            fontFamily = FontFamily.Serif,
                            fontSize = 10.sp,
                            letterSpacing = 0.4.sp,
                            color = tint,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  TOAST – edykt cesarski
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Powiadomienie w formie edyktu: wjeżdża z góry, pieczęć 令, pasek czasu
 * i samoczynne zamknięcie. Dotknięcie zamyka natychmiast.
 */
@Composable
fun AaaToastHost(
    message: String?,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    durationMillis: Int = 3800
) {
    val lastShown = remember { arrayOf("") }
    SideEffect { if (message != null) lastShown[0] = message }
    val text = message ?: lastShown[0]
    val progress = remember { Animatable(1f) }
    val currentOnDismiss by rememberUpdatedState(onDismiss)

    LaunchedEffect(message) {
        if (message != null) {
            progress.snapTo(1f)
            progress.animateTo(0f, tween(durationMillis, easing = LinearEasing))
            delay(60)
            currentOnDismiss()
        }
    }

    AnimatedVisibility(
        visible = message != null,
        enter = slideInVertically(spring(dampingRatio = 0.7f, stiffness = Spring.StiffnessMediumLow)) { -it } + fadeIn(tween(200)),
        exit = slideOutVertically(tween(220)) { -it / 2 } + fadeOut(tween(200)),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 14.dp, vertical = 8.dp)
                .fillMaxWidth()
                .aaaFrame(
                    accent = Aaa.Gold,
                    fill = Brush.verticalGradient(listOf(Aaa.LacquerRed, Aaa.Ink2, Aaa.Ink1)),
                    elevation = 14.dp
                )
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { currentOnDismiss() }
                .testTag("notification_toast")
        ) {
            Row(
                modifier = Modifier.padding(start = 12.dp, end = 10.dp, top = 10.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AaaSeal(char = "令", size = 30.dp)
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(text = "EDYKT SEKTY", style = AaaType.Overline.copy(fontSize = 9.sp))
                    Text(
                        text = text,
                        style = AaaType.Body.copy(fontWeight = FontWeight.SemiBold),
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(Modifier.width(6.dp))
                Icon(
                    Icons.Filled.Close,
                    contentDescription = "Zamknij powiadomienie",
                    tint = Aaa.TextLow,
                    modifier = Modifier.size(16.dp)
                )
            }
            // Pasek czasu edyktu
            Canvas(
                Modifier
                    .fillMaxWidth()
                    .height(2.dp)
                    .padding(horizontal = 12.dp)
            ) {
                drawLine(Aaa.Hairline, Offset(0f, 0f), Offset(size.width, 0f), size.height)
                drawLine(Aaa.goldSheen(), Offset(0f, 0f), Offset(size.width * progress.value, 0f), size.height)
            }
            Spacer(Modifier.height(6.dp))
        }
    }
}
