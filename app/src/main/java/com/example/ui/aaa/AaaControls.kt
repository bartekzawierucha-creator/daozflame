package com.example.ui.aaa

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

enum class AaaButtonStyle { GOLD, JADE, CINNABAR, GHOST }

private data class ButtonPalette(
    val top: Color,
    val mid: Color,
    val bottom: Color,
    val edge: Color,
    val text: Color
)

private fun paletteFor(style: AaaButtonStyle): ButtonPalette = when (style) {
    AaaButtonStyle.GOLD -> ButtonPalette(Aaa.Gold, Aaa.GoldMid, Aaa.GoldDeep, Aaa.GoldHi, Aaa.Ink1)
    AaaButtonStyle.JADE -> ButtonPalette(Color(0xFF2E8F72), Aaa.JadeDeep, Color(0xFF082A21), Aaa.Jade, Aaa.TextHi)
    AaaButtonStyle.CINNABAR -> ButtonPalette(Color(0xFFB6352A), Aaa.CinnabarDeep, Color(0xFF3A0B09), Color(0xFFFF8A73), Aaa.GoldHi)
    AaaButtonStyle.GHOST -> ButtonPalette(Aaa.Ink4, Aaa.Ink3, Aaa.Ink2, Aaa.GoldMid, Aaa.Gold)
}

/**
 * Przycisk AAA: wypukły metal / lakier, faza światła, ugięcie przy nacisku,
 * okresowy refleks przesuwający się po powierzchni (tylko styl GOLD).
 */
@Composable
fun AaaButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: AaaButtonStyle = AaaButtonStyle.GOLD,
    enabled: Boolean = true,
    leading: String? = null,
    height: Dp = 44.dp,
    testTag: String? = null
) {
    val palette = paletteFor(style)
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.955f else 1f,
        animationSpec = spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessMediumLow),
        label = "aaa_btn_scale"
    )

    val shine = if (style == AaaButtonStyle.GOLD && enabled) {
        val t = rememberInfiniteTransition(label = "aaa_btn_shine")
        t.animateFloat(
            initialValue = -0.4f,
            targetValue = 1.6f,
            animationSpec = infiniteRepeatable(
                animation = keyframes {
                    durationMillis = 4200
                    -0.4f at 0
                    -0.4f at 2600 using FastOutSlowInEasing
                    1.6f at 3500
                    1.6f at 4200
                },
                repeatMode = RepeatMode.Restart
            ),
            label = "aaa_btn_shine_x"
        )
    } else null

    val alphaMul = if (enabled) 1f else 0.38f

    var mod = modifier
        .heightIn(min = height)
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
            alpha = if (enabled) 1f else 0.55f
        }
        .drawWithCache {
            val w = size.width
            val h = size.height
            val c = 8.dp.toPx()
            val outer = chamferPath(w, h, c)
            val inner = chamferPath(w, h, c * 0.65f, 3.dp.toPx())
            val fill = Brush.verticalGradient(
                listOf(palette.top.copy(alpha = alphaMul), palette.mid.copy(alpha = alphaMul), palette.bottom.copy(alpha = alphaMul))
            )
            val edge = Brush.verticalGradient(listOf(palette.edge, palette.bottom))
            val sw = 1.2.dp.toPx()
            onDrawBehind {
                drawPath(outer, fill)
                // Odbicie górnej fazy
                drawLine(
                    Color.White.copy(alpha = if (style == AaaButtonStyle.GOLD) 0.45f else 0.14f),
                    Offset(c, sw * 1.6f),
                    Offset(w - c, sw * 1.6f),
                    sw
                )
                drawPath(inner, palette.edge.copy(alpha = 0.22f), style = Stroke(width = 0.8.dp.toPx()))
                val sx = shine?.value
                if (sx != null) {
                    val cx = sx * w
                    val band = w * 0.22f
                    drawPath(
                        outer,
                        Brush.linearGradient(
                            colors = listOf(Color.Transparent, Color.White.copy(alpha = 0.42f), Color.Transparent),
                            start = Offset(cx - band, 0f),
                            end = Offset(cx + band, h)
                        )
                    )
                }
                drawPath(outer, edge, style = Stroke(width = sw * 1.4f))
            }
        }
        .clickable(
            interactionSource = interaction,
            indication = null,
            enabled = enabled,
            role = Role.Button,
            onClick = onClick
        )
    if (testTag != null) mod = mod.testTag(testTag)

    Box(modifier = mod, contentAlignment = Alignment.Center) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (leading != null) {
                Text(text = leading, fontSize = 14.sp)
                Spacer(Modifier.width(8.dp))
            }
            Text(
                text = text.uppercase(),
                style = AaaType.Button.copy(color = palette.text),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * Przełącznik segmentowy z przesuwającym się, świecącym wskaźnikiem.
 * Zastępuje surowe TabRow z Material.
 */
@Composable
fun AaaSegmentedTabs(
    items: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    badges: List<String?> = emptyList(),
    testTags: List<String?> = emptyList(),
    accent: Color = Aaa.Gold
) {
    if (items.isEmpty()) return
    val count = items.size
    val safeIndex = selectedIndex.coerceIn(0, count - 1)
    val indicator by animateFloatAsState(
        targetValue = safeIndex.toFloat(),
        animationSpec = spring(dampingRatio = 0.78f, stiffness = Spring.StiffnessMediumLow),
        label = "aaa_tab_indicator"
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(46.dp)
            .aaaFrame(accent = accent, chamfer = 9.dp, elevation = 3.dp, ornaments = false, glowStrength = 0.5f)
            .drawWithCache {
                val segW = size.width / count
                val pad = 4.dp.toPx()
                onDrawBehind {
                    val left = indicator * segW + pad
                    val w = segW - pad * 2f
                    val h = size.height - pad * 2f
                    val p = chamferPath(w, h, 6.dp.toPx())
                    translate(left = left, top = pad) {
                    drawPath(
                        p,
                        Brush.verticalGradient(
                            listOf(accent.copy(alpha = 0.30f), accent.copy(alpha = 0.10f)),
                            startY = 0f,
                            endY = h
                        )
                    )
                    drawPath(p, accent.copy(alpha = 0.75f), style = Stroke(width = 1.dp.toPx()))
                    drawLine(
                        Aaa.GoldHi,
                        Offset(w * 0.25f, h - 1.dp.toPx()),
                        Offset(w * 0.75f, h - 1.dp.toPx()),
                        2.dp.toPx()
                    )
                    }
                }
            },
        verticalAlignment = Alignment.CenterVertically
    ) {
        items.forEachIndexed { index, label ->
            val selected = index == safeIndex
            val color by animateColorAsState(
                targetValue = if (selected) Aaa.GoldHi else Aaa.TextLow,
                animationSpec = tween(220),
                label = "aaa_tab_color_$index"
            )
            var itemMod = Modifier
                .weight(1f)
                .fillMaxHeight()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    role = Role.Tab
                ) { onSelect(index) }
            val tag = testTags.getOrNull(index)
            if (tag != null) itemMod = itemMod.testTag(tag)

            Row(
                modifier = itemMod,
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = label,
                    style = AaaType.Label.copy(
                        color = color,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Serif,
                        fontWeight = if (selected) androidx.compose.ui.text.font.FontWeight.Bold
                        else androidx.compose.ui.text.font.FontWeight.Medium
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                val badge = badges.getOrNull(index)
                if (!badge.isNullOrEmpty()) {
                    Spacer(Modifier.width(5.dp))
                    AaaCountBadge(text = badge, highlighted = selected)
                }
            }
        }
    }
}

/** Mała emaliowana plakietka licznika. */
@Composable
fun AaaCountBadge(
    text: String,
    highlighted: Boolean = false,
    color: Color = Aaa.Cinnabar
) {
    Box(
        modifier = Modifier
            .heightIn(min = 16.dp)
            .widthIn(min = 16.dp)
            .drawWithCache {
                val p = chamferPath(size.width, size.height, 4.dp.toPx())
                val fill = if (highlighted) color else color.copy(alpha = 0.55f)
                onDrawBehind {
                    drawPath(p, fill)
                    drawPath(p, Aaa.GoldHi.copy(alpha = 0.5f), style = Stroke(width = 0.8.dp.toPx()))
                }
            }
            .padding(horizontal = 4.dp, vertical = 1.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = AaaType.Label.copy(color = Aaa.GoldHi, fontSize = 9.sp, letterSpacing = 0.sp),
            maxLines = 1
        )
    }
}

/** Emaliowany znacznik (status, żywioł, rzadkość). */
@Composable
fun AaaTag(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Aaa.Gold,
    leading: String? = null
) {
    Row(
        modifier = modifier
            .drawWithCache {
                val p = chamferPath(size.width, size.height, 5.dp.toPx())
                val fill = Brush.verticalGradient(listOf(color.copy(alpha = 0.20f), color.copy(alpha = 0.07f)))
                onDrawBehind {
                    drawPath(p, fill)
                    drawPath(p, color.copy(alpha = 0.45f), style = Stroke(width = 0.9.dp.toPx()))
                }
            }
            .padding(horizontal = 8.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (leading != null) {
            Text(text = leading, fontSize = 10.sp)
            Spacer(Modifier.width(4.dp))
        }
        Text(
            text = text,
            style = AaaType.Label.copy(color = color, fontSize = 10.5.sp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
