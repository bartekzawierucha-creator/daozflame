package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.graphics.GildedGold
import com.example.ui.graphics.JadeRoyal
import kotlin.math.sin

/**
 * Struktura cząsteczki aury Qi unoszącej się wokół kultywatora.
 */
private data class QiParticle(
    val relativeX: Float, // 0.0f .. 1.0f (wzdłuż paska)
    val baseOffsetY: Float, // bazowa wysokość unoszenia
    val speed: Float,
    val radius: Float,
    val color: Color,
    val phaseShift: Float
)

/**
 * Kaligraficzny Pasek Postępu Qi z Subtelnym Efektem Cząsteczkowym Aury.
 * Wizualizuje poziom nasycenia meridianów Dantian energią duchową z płynną animacją,
 * unoszącymi się iskrami aury i kaligraficzną pieczęcią [氣].
 */
@Composable
fun CalligraphicQiAuraProgressBar(
    progressPercent: Int,
    modifier: Modifier = Modifier,
    label: String = "Nasycenie Meridianów Qi",
    subLabel: String? = null,
    barHeight: Dp = 14.dp,
    auraColor: Color = Color(0xFF10B981), // Szmaragdowy nefryt
    secondaryAuraColor: Color = Color(0xFF00E5FF), // Cyjan niebios
    sealChar: String = "氣", // Chiński ideogram Qi
    showAuraParticles: Boolean = true
) {
    val clampedProgress = progressPercent.coerceIn(0, 100)
    val progressFraction = clampedProgress / 100f
    val isReady = clampedProgress >= 100

    // Płynna animacja wypełnienia paska
    val animatedProgress by animateFloatAsState(
        targetValue = progressFraction,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "animated_qi_fraction"
    )

    // Nieskończona faza dla falowania cząsteczek aury oraz fali światła Dantian
    val infiniteTransition = rememberInfiniteTransition(label = "qi_aura_transition")
    val auraPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "aura_particle_cycle"
    )

    val breakthroughPulse by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breakthrough_pulse"
    )

    // Stała pula cząsteczek aury Qi
    val particles = remember {
        List(18) { index ->
            val colors = listOf(
                Color(0xFF34D399), // Szmaragd
                Color(0xFF67E8F9), // Cyjan
                Color(0xFFFFD700), // Złoto Dao
                Color(0xFFA7F3D0)  // Czyste Qi
            )
            QiParticle(
                relativeX = (index * 0.055f + (index * 0.037f) % 0.95f).coerceIn(0.02f, 0.98f),
                baseOffsetY = ((index % 5) * 4f),
                speed = 0.7f + (index % 4) * 0.25f,
                radius = 1.5f + (index % 3) * 0.9f,
                color = colors[index % colors.size],
                phaseShift = (index * 0.35f)
            )
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("calligraphic_qi_aura_bar")
    ) {
        // Nagłówek: Kaligraficzna Pieczęć [氣], Etykieta oraz Licznik Procentowy
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Kaligraficzna czerwona pieczęć laku cinnabar
                Box(
                    modifier = Modifier
                        .size(18.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(Color(0xFF991B1B), Color(0xFF7F1D1D))
                            )
                        )
                        .border(0.8.dp, GildedGold.copy(alpha = 0.9f), RoundedCornerShape(3.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = sealChar,
                        color = Color(0xFFFEF08A),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Serif
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

                Column {
                    Text(
                        text = if (isReady) "⚡ DANTIAN PRZEPEŁNIONY (GOTOWY NA PRZEŁOM)" else label,
                        color = if (isReady) Color(0xFFFFD700) else Color(0xFFE2E8F0),
                        fontSize = 11.sp,
                        fontWeight = if (isReady) FontWeight.Black else FontWeight.SemiBold
                    )
                    if (subLabel != null) {
                        Text(
                            text = subLabel,
                            color = Color(0xFF94A3B8),
                            fontSize = 9.sp
                        )
                    }
                }
            }

            // Licznik procentowy w złotej ramce
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "$clampedProgress%",
                    color = if (isReady) Color(0xFFFFD700) else Color(0xFF67E8F9),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Główny Pasek na Canvasie: Drewniane wałki pergaminu, gradient esencji Qi i cząsteczki
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(barHeight + (if (showAuraParticles) 12.dp else 0.dp))
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(barHeight)
                    .align(Alignment.BottomCenter)
            ) {
                val w = size.width
                val h = size.height

                // 1. Tło rynny Dantian (Ciemny heban i fiolet)
                drawRoundRect(
                    brush = Brush.verticalGradient(
                        listOf(Color(0xFF0F0A1A), Color(0xFF1B1228), Color(0xFF0F0A1A))
                    ),
                    topLeft = Offset(0f, 0f),
                    size = Size(w, h),
                    cornerRadius = CornerRadius(h * 0.5f, h * 0.5f)
                )

                // 2. Wypełnienie Energią Duchową Qi (Gradient Nefryt -> Cyjan -> Złoto)
                val fillWidth = (w * animatedProgress).coerceAtLeast(0f)
                if (fillWidth > 0f) {
                    val qiBrush = Brush.horizontalGradient(
                        colors = if (isReady) {
                            listOf(
                                Color(0xFFB45309),
                                Color(0xFFFFD700),
                                Color(0xFFFFFBEB),
                                Color(0xFFFFD700)
                            )
                        } else {
                            listOf(
                                auraColor.copy(alpha = 0.85f),
                                secondaryAuraColor,
                                Color(0xFFFFD700).copy(alpha = 0.9f)
                            )
                        },
                        startX = 0f,
                        endX = fillWidth
                    )

                    drawRoundRect(
                        brush = qiBrush,
                        topLeft = Offset(1.5f, 1.5f),
                        size = Size((fillWidth - 3f).coerceAtLeast(1f), (h - 3f).coerceAtLeast(1f)),
                        cornerRadius = CornerRadius((h - 3f) * 0.5f, (h - 3f) * 0.5f)
                    )

                    // 3. Efekt fali świetlnej Shimmer Wave (oddech Qi)
                    val wavePos = (auraPhase * (fillWidth + 40f)) - 20f
                    if (wavePos in 0f..fillWidth) {
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(Color.White.copy(alpha = 0.75f), Color.Transparent),
                                center = Offset(wavePos, h * 0.5f),
                                radius = h * 1.4f
                            ),
                            radius = h * 1.4f,
                            center = Offset(wavePos, h * 0.5f)
                        )
                    }
                }

                // 4. Kaligraficzne złote obramowanie zwoju
                drawRoundRect(
                    brush = Brush.horizontalGradient(
                        listOf(
                            GildedGold.copy(alpha = 0.5f),
                            GildedGold,
                            Color(0xFFFEF08A),
                            GildedGold,
                            GildedGold.copy(alpha = 0.5f)
                        )
                    ),
                    topLeft = Offset(0f, 0f),
                    size = Size(w, h),
                    cornerRadius = CornerRadius(h * 0.5f, h * 0.5f),
                    style = Stroke(width = 1.2f)
                )

                // Jadeitowe końcówki wałka (ozdobne piny kaligraficzne)
                drawCircle(color = JadeRoyal, radius = 3.5f, center = Offset(3f, h * 0.5f))
                drawCircle(color = Color.White.copy(alpha = 0.8f), radius = 1.2f, center = Offset(2.5f, h * 0.4f))
                drawCircle(color = JadeRoyal, radius = 3.5f, center = Offset(w - 3f, h * 0.5f))
                drawCircle(color = Color.White.copy(alpha = 0.8f), radius = 1.2f, center = Offset(w - 3.5f, h * 0.4f))
            }

            // 5. Unoszące się cząsteczki aury Qi (nad napełnioną częścią paska)
            if (showAuraParticles && animatedProgress > 0.05f) {
                Canvas(
                    modifier = Modifier.fillMaxSize()
                ) {
                    val w = size.width
                    val h = size.height
                    val barTop = h - barHeight.toPx()
                    val activeWidth = w * animatedProgress

                    particles.forEach { p ->
                        if (p.relativeX <= animatedProgress) {
                            val cycle = (auraPhase * p.speed + p.phaseShift) % 1f
                            // Cząsteczka wznosi się pionowo w górę od poziomu paska
                            val py = barTop - (cycle * (h - 4f)) - p.baseOffsetY
                            // Delikatny dryf horyzontalny (ruch aury Qi)
                            val sway = sin((cycle * 6.283f) + p.phaseShift) * 4f
                            val px = (p.relativeX * activeWidth) + sway

                            // Zanikanie na szczycie
                            val alpha = sin(cycle * 3.14159f).coerceIn(0f, 1f) * (if (isReady) breakthroughPulse else 0.85f)

                            if (py >= 0f && px in 0f..w) {
                                drawCircle(
                                    color = p.color.copy(alpha = alpha.coerceIn(0f, 1f)),
                                    radius = p.radius * (if (isReady) 1.4f else 1f),
                                    center = Offset(px, py)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
