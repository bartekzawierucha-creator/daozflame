package com.example.ui.aaa

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Spacer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.dp
import com.example.model.TimeOfDay
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sin

/** Zegar animacji w sekundach, odczytywany wyłącznie w fazie rysowania (bez rekompozycji). */
@Composable
fun rememberAaaClock(): State<Float> = produceState(0f) {
    var start = -1L
    while (true) {
        withFrameNanos { now ->
            if (start < 0L) start = now
            value = (((now - start) / 1_000_000_000.0) % 3600.0).toFloat()
        }
    }
}

private data class SkyPalette(val top: Color, val mid: Color, val bottom: Color)

private fun skyFor(t: TimeOfDay): SkyPalette = when (t) {
    TimeOfDay.DAWN -> SkyPalette(Color(0xFF1A1226), Color(0xFF4A2638), Color(0xFF8A4F4A))
    TimeOfDay.DAY -> SkyPalette(Color(0xFF14161F), Color(0xFF3B3328), Color(0xFF7A6240))
    TimeOfDay.DUSK -> SkyPalette(Color(0xFF150D16), Color(0xFF4A1E1C), Color(0xFF9A4A24))
    TimeOfDay.NIGHT -> SkyPalette(Color(0xFF05070D), Color(0xFF0D1626), Color(0xFF1C2A3E))
    TimeOfDay.FULL_MOON -> SkyPalette(Color(0xFF07060F), Color(0xFF1A1433), Color(0xFF3A2C5E))
}

/** Deterministyczny pseudo-los 0..1 (stabilny między klatkami). */
private fun hash01(i: Int, salt: Float): Float {
    val v = sin(i * 12.9898f + salt * 78.233f) * 43758.547f
    return v - kotlin.math.floor(v)
}

/** Grzbiet gór w stylu malarstwa tuszem: ostre szczyty, miękkie doliny. */
private fun ridgePath(w: Float, h: Float, baseY: Float, amp: Float, seed: Float): Path {
    val path = Path()
    val steps = 64
    path.moveTo(0f, h)
    for (i in 0..steps) {
        val x = w * i / steps
        val u = x / w
        val a = (1f - abs(sin(u * PI.toFloat() * 2.3f + seed))).pow(1.6f)
        val b = (1f - abs(sin(u * PI.toFloat() * 5.1f + seed * 1.7f))).pow(2.2f)
        val c = sin(u * PI.toFloat() * 13f + seed * 3.1f) * 0.06f
        val y = baseY - amp * (0.62f * a + 0.34f * b + c)
        path.lineTo(x, y)
    }
    path.lineTo(w, h)
    path.close()
    return path
}

private fun DrawScope.drawMistPuff(center: Offset, radius: Float, color: Color, stretch: Float) {
    scale(scaleX = stretch, scaleY = 1f, pivot = center) {
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(color, Color.Transparent),
                center = center,
                radius = radius
            ),
            radius = radius,
            center = center
        )
    }
}

/**
 * Pełnoekranowe, żywe tło: niebo fazy dnia, ciało niebieskie z halo,
 * trzy warstwy gór tuszowych, dryfujące mgły, unoszące się iskry Qi i winieta.
 * Wszystko liczone w fazie rysowania – zero rekompozycji na klatkę.
 */
@Composable
fun AaaAtmosphere(
    timeOfDay: TimeOfDay,
    modifier: Modifier = Modifier
) {
    val sky = skyFor(timeOfDay)
    val skyTop by animateColorAsState(sky.top, tween(2400), label = "sky_top")
    val skyMid by animateColorAsState(sky.mid, tween(2400), label = "sky_mid")
    val skyBottom by animateColorAsState(sky.bottom, tween(2400), label = "sky_bottom")
    val accent by animateColorAsState(Aaa.phaseAccent(timeOfDay), tween(2400), label = "sky_accent")
    val clock = rememberAaaClock()
    val isNight = timeOfDay == TimeOfDay.NIGHT || timeOfDay == TimeOfDay.FULL_MOON

    Spacer(
        modifier = modifier.drawWithCache {
            val w = size.width
            val h = size.height
            val far = ridgePath(w, h, h * 0.50f, h * 0.13f, 0.9f)
            val mid = ridgePath(w, h, h * 0.63f, h * 0.11f, 2.4f)
            val near = ridgePath(w, h, h * 0.80f, h * 0.08f, 4.1f)
            val vignette = Brush.radialGradient(
                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.35f), Color.Black.copy(alpha = 0.82f)),
                center = Offset(w / 2f, h * 0.42f),
                radius = (maxOf(w, h) * 0.78f).coerceAtLeast(1f)
            )
            val readability = Brush.verticalGradient(
                0f to Color.Transparent,
                0.45f to Aaa.Ink0.copy(alpha = 0.25f),
                1f to Aaa.Ink0.copy(alpha = 0.78f)
            )
            val moteCount = 46
            val dp1 = 1.dp.toPx()

            onDrawBehind {
                val t = clock.value

                // 1. Niebo
                drawRect(Brush.verticalGradient(listOf(skyTop, skyMid, skyBottom)))

                // 2. Gwiazdy (noc)
                if (isNight) {
                    for (i in 0 until 70) {
                        val sx = hash01(i, 1.3f) * w
                        val sy = hash01(i, 7.7f) * h * 0.5f
                        val tw = 0.45f + 0.55f * (0.5f + 0.5f * sin(t * (0.6f + hash01(i, 3.3f) * 1.8f) + i))
                        drawCircle(
                            color = Color.White.copy(alpha = 0.55f * tw * (1f - sy / (h * 0.5f))),
                            radius = dp1 * (0.5f + hash01(i, 5.1f) * 0.9f),
                            center = Offset(sx, sy)
                        )
                    }
                }

                // 3. Ciało niebieskie z oddychającym halo
                val sun = Offset(w * 0.76f, h * 0.17f)
                val breathe = 1f + 0.04f * sin(t * 0.8f)
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(accent.copy(alpha = 0.32f), accent.copy(alpha = 0.08f), Color.Transparent),
                        center = sun,
                        radius = w * 0.55f * breathe
                    ),
                    radius = w * 0.55f * breathe,
                    center = sun
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color.White.copy(alpha = 0.95f), accent.copy(alpha = 0.85f), accent.copy(alpha = 0f)),
                        center = sun,
                        radius = w * 0.075f
                    ),
                    radius = w * 0.075f,
                    center = sun
                )

                // 4. Daleki łańcuch gór
                drawPath(
                    far,
                    Brush.verticalGradient(
                        listOf(lerp(skyMid, Aaa.Ink3, 0.35f).copy(alpha = 0.85f), skyBottom.copy(alpha = 0.4f)),
                        startY = h * 0.36f,
                        endY = h * 0.75f
                    )
                )

                // 5. Mgła – warstwa dalsza
                val mistColor = lerp(skyBottom, Color.White, 0.25f).copy(alpha = 0.16f)
                for (i in 0 until 5) {
                    val speed = 6f + hash01(i, 2.2f) * 10f
                    val span = w * 1.8f
                    val x = ((hash01(i, 9.1f) * span + t * speed) % span) - w * 0.4f
                    drawMistPuff(Offset(x, h * (0.56f + hash01(i, 4.4f) * 0.06f)), h * 0.07f, mistColor, 3.2f)
                }

                // 6. Środkowy łańcuch
                drawPath(
                    mid,
                    Brush.verticalGradient(
                        listOf(lerp(skyMid, Aaa.Ink1, 0.62f), lerp(skyBottom, Aaa.Ink0, 0.7f)),
                        startY = h * 0.5f,
                        endY = h
                    )
                )

                // 7. Mgła – warstwa bliższa
                for (i in 0 until 4) {
                    val speed = 12f + hash01(i, 6.6f) * 14f
                    val span = w * 2f
                    val x = ((hash01(i, 3.9f) * span + t * speed) % span) - w * 0.5f
                    drawMistPuff(Offset(x, h * (0.72f + hash01(i, 8.8f) * 0.05f)), h * 0.06f, mistColor.copy(alpha = 0.12f), 4f)
                }

                // 8. Bliski grzbiet (niemal czarny tusz)
                drawPath(near, Brush.verticalGradient(listOf(Aaa.Ink2, Aaa.Ink0), startY = h * 0.68f, endY = h))

                // 9. Iskry Qi wznoszące się ku niebu
                for (i in 0 until moteCount) {
                    val speed = 0.018f + hash01(i, 1.9f) * 0.035f
                    val life = (t * speed + hash01(i, 5.5f)) % 1f
                    val y = h * (1.02f - life * 1.05f)
                    val x = hash01(i, 0.7f) * w + sin(t * (0.4f + hash01(i, 2.8f)) + i) * 14f * dp1
                    val fade = sin(life * PI.toFloat())
                    val r = dp1 * (0.8f + hash01(i, 4.2f) * 1.8f)
                    val c = if (i % 5 == 0) Aaa.Jade else accent
                    drawCircle(c.copy(alpha = 0.18f * fade), radius = r * 4f, center = Offset(x, y))
                    drawCircle(c.copy(alpha = 0.85f * fade), radius = r, center = Offset(x, y))
                }

                // 10. Czytelność treści + winieta kinowa
                drawRect(readability)
                drawRect(vignette)
            }
        }
    )
}
