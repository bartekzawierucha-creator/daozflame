package com.example.ui.aaa

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Prostokąt ze ściętymi narożnikami – sygnaturowy kształt całego UI. */
internal fun chamferPath(w: Float, h: Float, c: Float, inset: Float = 0f): Path = Path().apply {
    val l = inset
    val t = inset
    val r = w - inset
    val b = h - inset
    val cc = c.coerceAtMost(minOf(r - l, b - t) / 2f).coerceAtLeast(0f)
    moveTo(l + cc, t)
    lineTo(r - cc, t)
    lineTo(r, t + cc)
    lineTo(r, b - cc)
    lineTo(r - cc, b)
    lineTo(l + cc, b)
    lineTo(l, b - cc)
    lineTo(l, t + cc)
    close()
}

/** Mały romb (ćwiek) – detal okuć. */
internal fun DrawScope.drawStud(center: Offset, radius: Float, color: Color) {
    val p = Path().apply {
        moveTo(center.x, center.y - radius)
        lineTo(center.x + radius, center.y)
        lineTo(center.x, center.y + radius)
        lineTo(center.x - radius, center.y)
        close()
    }
    drawPath(p, color)
}

/**
 * Rama lakowa AAA: wypełnienie tuszowe, poświata akcentu od góry, podwójna
 * krawędź (metaliczna zewnętrzna + włoskowa wewnętrzna), okucia narożników.
 */
fun Modifier.aaaFrame(
    accent: Color = Aaa.Gold,
    fill: Brush? = null,
    chamfer: Dp = 10.dp,
    elevation: Dp = 6.dp,
    ornaments: Boolean = true,
    glowStrength: Float = 1f
): Modifier = this
    .shadow(
        elevation = elevation,
        shape = CutCornerShape(chamfer),
        clip = true,
        ambientColor = Color.Black,
        spotColor = Color.Black
    )
    .drawWithCache {
        val w = size.width
        val h = size.height
        val c = chamfer.toPx()
        val stroke = 1.2.dp.toPx()
        val insetPx = 4.dp.toPx()
        val outer = chamferPath(w, h, c)
        val inner = chamferPath(w, h, c * 0.7f, insetPx)
        val body = fill ?: Brush.verticalGradient(listOf(Aaa.Ink3, Aaa.Ink2, Aaa.Ink1))
        val topGlow = Brush.radialGradient(
            colors = listOf(accent.copy(alpha = 0.13f * glowStrength), Color.Transparent),
            center = Offset(w / 2f, 0f),
            radius = (w * 0.75f).coerceAtLeast(1f)
        )
        val border = Brush.linearGradient(
            colors = listOf(accent, Aaa.GoldDeep, accent.copy(alpha = 0.5f), Aaa.GoldDeep, accent),
            start = Offset.Zero,
            end = Offset(w, h)
        )
        val capLen = 18.dp.toPx().coerceAtMost(w / 4f)
        val cap = Aaa.GoldHi.copy(alpha = 0.85f)
        val studR = 2.4.dp.toPx()
        val studIn = c / 2f + 2.5.dp.toPx()

        onDrawBehind {
            drawPath(outer, body)
            drawPath(outer, topGlow)
            // Faza górna – odbicie światła na krawędzi
            drawLine(
                color = Aaa.GoldHi.copy(alpha = 0.16f),
                start = Offset(c, stroke * 2f),
                end = Offset(w - c, stroke * 2f),
                strokeWidth = stroke
            )
            drawPath(inner, color = accent.copy(alpha = 0.14f), style = Stroke(width = 0.8.dp.toPx()))
            drawPath(outer, border, style = Stroke(width = stroke * 2f))

            if (ornaments) {
                // Metalowe okucia przy każdym narożniku
                val y0 = stroke
                val y1 = h - stroke
                val x0 = stroke
                val x1 = w - stroke
                drawLine(cap, Offset(c, y0), Offset(c + capLen, y0), stroke * 1.6f)
                drawLine(cap, Offset(w - c, y0), Offset(w - c - capLen, y0), stroke * 1.6f)
                drawLine(cap, Offset(c, y1), Offset(c + capLen, y1), stroke * 1.6f)
                drawLine(cap, Offset(w - c, y1), Offset(w - c - capLen, y1), stroke * 1.6f)
                drawLine(cap, Offset(x0, c), Offset(x0, c + capLen), stroke * 1.6f)
                drawLine(cap, Offset(x1, c), Offset(x1, c + capLen), stroke * 1.6f)
                drawLine(cap, Offset(x0, h - c), Offset(x0, h - c - capLen), stroke * 1.6f)
                drawLine(cap, Offset(x1, h - c), Offset(x1, h - c - capLen), stroke * 1.6f)

                drawStud(Offset(studIn, studIn), studR, accent)
                drawStud(Offset(w - studIn, studIn), studR, accent)
                drawStud(Offset(studIn, h - studIn), studR, accent)
                drawStud(Offset(w - studIn, h - studIn), studR, accent)
            }
        }
    }

/**
 * Panel AAA z opcjonalnym nagłówkiem: pieczęć cynobrowa, nadtytuł, tytuł szeryfowy
 * i złoty ornament rozdzielający.
 */
@Composable
fun AaaPanel(
    modifier: Modifier = Modifier,
    title: String? = null,
    overline: String? = null,
    seal: String? = null,
    accent: Color = Aaa.Gold,
    fill: Brush? = null,
    contentPadding: PaddingValues = PaddingValues(14.dp),
    trailing: (@Composable RowScope.() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .aaaFrame(accent = accent, fill = fill)
            .padding(contentPadding)
    ) {
        if (title != null) {
            AaaPanelHeader(
                title = title,
                overline = overline,
                seal = seal,
                accent = accent,
                trailing = trailing
            )
            Spacer(Modifier.height(10.dp))
        }
        content()
    }
}

@Composable
fun AaaPanelHeader(
    title: String,
    overline: String? = null,
    seal: String? = null,
    accent: Color = Aaa.Gold,
    trailing: (@Composable RowScope.() -> Unit)? = null
) {
    Column(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (seal != null) {
                AaaSeal(char = seal, size = 28.dp)
                Spacer(Modifier.width(10.dp))
            }
            Column(Modifier.weight(1f)) {
                if (overline != null) {
                    Text(
                        text = overline.uppercase(),
                        style = AaaType.Overline.copy(color = accent.copy(alpha = 0.85f)),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Text(
                    text = title,
                    style = AaaType.Title,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (trailing != null) {
                Spacer(Modifier.width(8.dp))
                trailing()
            }
        }
        Spacer(Modifier.height(8.dp))
        AaaDivider(accent = accent)
    }
}

/** Pieczęć z czerwonego laku z ideogramem, lekko przekrzywiona jak odbita ręcznie. */
@Composable
fun AaaSeal(
    char: String,
    modifier: Modifier = Modifier,
    size: Dp = 28.dp,
    color: Color = Aaa.Cinnabar
) {
    Box(
        modifier = modifier
            .size(size)
            .graphicsLayer { rotationZ = -4f }
            .drawBehind {
                val c = this.size.minDimension * 0.18f
                val outer = chamferPath(this.size.width, this.size.height, c)
                drawPath(
                    outer,
                    Brush.radialGradient(
                        colors = listOf(color, Aaa.CinnabarDeep),
                        center = Offset(this.size.width * 0.35f, this.size.height * 0.3f),
                        radius = this.size.maxDimension
                    )
                )
                val inner = chamferPath(this.size.width, this.size.height, c * 0.6f, 2.5.dp.toPx())
                drawPath(inner, Aaa.GoldHi.copy(alpha = 0.55f), style = Stroke(width = 0.8.dp.toPx()))
                drawPath(outer, Aaa.GoldDeep, style = Stroke(width = 1.dp.toPx()))
            },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = char,
            color = Aaa.GoldHi,
            fontFamily = FontFamily.Serif,
            fontWeight = FontWeight.Black,
            fontSize = (size.value * 0.52f).sp,
            textAlign = TextAlign.Center
        )
    }
}

/** Złoty ornament: linie zanikające do krawędzi, romb w centrum, ćwieki boczne. */
@Composable
fun AaaDivider(
    modifier: Modifier = Modifier,
    accent: Color = Aaa.Gold
) {
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(9.dp)
    ) {
        val cy = size.height / 2f
        val cx = size.width / 2f
        val gap = 12.dp.toPx()
        val sw = 1.dp.toPx()
        drawLine(
            brush = Brush.horizontalGradient(
                listOf(Color.Transparent, accent.copy(alpha = 0.7f)),
                startX = 0f,
                endX = cx - gap
            ),
            start = Offset(0f, cy),
            end = Offset(cx - gap, cy),
            strokeWidth = sw
        )
        drawLine(
            brush = Brush.horizontalGradient(
                listOf(accent.copy(alpha = 0.7f), Color.Transparent),
                startX = cx + gap,
                endX = size.width
            ),
            start = Offset(cx + gap, cy),
            end = Offset(size.width, cy),
            strokeWidth = sw
        )
        drawStud(Offset(cx, cy), 4.dp.toPx(), accent)
        drawStud(Offset(cx, cy), 1.8.dp.toPx(), Aaa.Ink1)
        drawStud(Offset(cx - gap + 2.dp.toPx(), cy), 1.6.dp.toPx(), accent)
        drawStud(Offset(cx + gap - 2.dp.toPx(), cy), 1.6.dp.toPx(), accent)
    }
}

/** Tytuł sekcji poza panelem: wyśrodkowany, z ornamentami po bokach. */
@Composable
fun AaaSectionTitle(
    title: String,
    modifier: Modifier = Modifier,
    overline: String? = null,
    accent: Color = Aaa.Gold
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (overline != null) {
            Text(text = overline.uppercase(), style = AaaType.Overline.copy(color = accent.copy(alpha = 0.8f)))
            Spacer(Modifier.height(2.dp))
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Canvas(Modifier.width(36.dp).height(9.dp)) {
                drawLine(
                    Brush.horizontalGradient(listOf(Color.Transparent, accent)),
                    Offset(0f, size.height / 2f),
                    Offset(size.width - 6.dp.toPx(), size.height / 2f),
                    1.dp.toPx()
                )
                drawStud(Offset(size.width - 3.dp.toPx(), size.height / 2f), 2.6.dp.toPx(), accent)
            }
            Spacer(Modifier.width(10.dp))
            Text(text = title, style = AaaType.Title, textAlign = TextAlign.Center)
            Spacer(Modifier.width(10.dp))
            Canvas(Modifier.width(36.dp).height(9.dp)) {
                drawStud(Offset(3.dp.toPx(), size.height / 2f), 2.6.dp.toPx(), accent)
                drawLine(
                    Brush.horizontalGradient(listOf(accent, Color.Transparent)),
                    Offset(6.dp.toPx(), size.height / 2f),
                    Offset(size.width, size.height / 2f),
                    1.dp.toPx()
                )
            }
        }
    }
}
