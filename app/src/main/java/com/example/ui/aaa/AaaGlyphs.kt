package com.example.ui.aaa

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** Wektorowe glify zasobów – zastępują emoji w kluczowych miejscach HUD. */
enum class AaaGlyph { SPIRIT_STONE, REPUTATION, CONTRIBUTION }

@Composable
fun AaaGlyphIcon(glyph: AaaGlyph, modifier: Modifier = Modifier, tint: Color? = null) {
    Canvas(modifier) {
        when (glyph) {
            AaaGlyph.SPIRIT_STONE -> drawSpiritStone(tint ?: Aaa.Azure)
            AaaGlyph.REPUTATION -> drawReputationSigil(tint ?: Aaa.Gold)
            AaaGlyph.CONTRIBUTION -> drawContributionSigil(tint ?: Aaa.Jade)
        }
    }
}

/** Kryształ ducha: heksagonalny pryzmat z fasetami i refleksem. */
fun DrawScope.drawSpiritStone(color: Color) {
    val w = size.width
    val h = size.height
    val cx = w / 2f
    val top = Offset(cx, h * 0.04f)
    val bottom = Offset(cx, h * 0.96f)
    val ul = Offset(w * 0.18f, h * 0.30f)
    val ur = Offset(w * 0.82f, h * 0.30f)
    val ll = Offset(w * 0.18f, h * 0.66f)
    val lr = Offset(w * 0.82f, h * 0.66f)
    val waist = Offset(cx, h * 0.40f)

    val body = Path().apply {
        moveTo(top.x, top.y); lineTo(ur.x, ur.y); lineTo(lr.x, lr.y)
        lineTo(bottom.x, bottom.y); lineTo(ll.x, ll.y); lineTo(ul.x, ul.y); close()
    }
    drawCircle(
        Brush.radialGradient(listOf(color.copy(alpha = 0.45f), Color.Transparent), center = center, radius = w * 0.7f),
        radius = w * 0.7f,
        center = center
    )
    drawPath(body, Brush.verticalGradient(listOf(Color.White, color, color.copy(red = color.red * 0.4f, green = color.green * 0.5f))))
    // Fasety
    val leftFacet = Path().apply {
        moveTo(top.x, top.y); lineTo(ul.x, ul.y); lineTo(ll.x, ll.y); lineTo(waist.x, waist.y); close()
    }
    drawPath(leftFacet, Color.White.copy(alpha = 0.35f))
    val edge = Stroke(width = w * 0.045f)
    drawPath(body, Color.White.copy(alpha = 0.7f), style = edge)
    drawLine(Color.White.copy(alpha = 0.5f), top, waist, w * 0.03f)
    drawLine(Color.White.copy(alpha = 0.35f), waist, bottom, w * 0.03f)
}

/** Pieczęć reputacji: ośmioramienna gwiazda z dwóch kwadratów i rubinowym oczkiem. */
fun DrawScope.drawReputationSigil(color: Color) {
    val r = size.minDimension / 2f * 0.92f
    val c = center
    drawCircle(
        Brush.radialGradient(listOf(color.copy(alpha = 0.4f), Color.Transparent), center = c, radius = r * 1.3f),
        radius = r * 1.3f,
        center = c
    )
    val square = Path().apply {
        moveTo(c.x, c.y - r); lineTo(c.x + r, c.y); lineTo(c.x, c.y + r); lineTo(c.x - r, c.y); close()
    }
    val metal = Brush.verticalGradient(listOf(Aaa.GoldHi, color, Aaa.GoldDeep), startY = c.y - r, endY = c.y + r)
    drawPath(square, metal)
    rotate(45f, pivot = c) { drawPath(square, metal) }
    drawCircle(Aaa.GoldDeep, radius = r * 0.42f, center = c)
    drawCircle(
        Brush.radialGradient(listOf(Color(0xFFFF8A73), Aaa.Cinnabar, Aaa.CinnabarDeep), center = Offset(c.x - r * 0.1f, c.y - r * 0.1f), radius = r * 0.36f),
        radius = r * 0.32f,
        center = c
    )
}

/** Znak wkładu: lotos z pięciu płatków. */
fun DrawScope.drawContributionSigil(color: Color) {
    val r = size.minDimension / 2f * 0.9f
    val c = center
    for (i in 0 until 5) {
        val a = (-90f + i * 72f) * (PI / 180.0).toFloat()
        val tip = Offset(c.x + cos(a) * r, c.y + sin(a) * r)
        val petal = Path().apply {
            moveTo(c.x, c.y)
            quadraticBezierTo(
                c.x + cos(a - 0.6f) * r * 0.75f, c.y + sin(a - 0.6f) * r * 0.75f,
                tip.x, tip.y
            )
            quadraticBezierTo(
                c.x + cos(a + 0.6f) * r * 0.75f, c.y + sin(a + 0.6f) * r * 0.75f,
                c.x, c.y
            )
            close()
        }
        drawPath(petal, Brush.radialGradient(listOf(Color.White, color), center = c, radius = r))
    }
    drawCircle(Aaa.GoldHi, radius = r * 0.18f, center = c)
}
