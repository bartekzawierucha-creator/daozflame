package com.example.ui.graphics

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.audio.WuxiaAudioEngine
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

@Composable
fun Wuxia2dForgeInteractive(
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "forge_anim")
    val hammerCycle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1300, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "hammer_strike"
    )

    // Odtwórz dźwięk uderzenia młota w kowadło
    var lastImpactPlayed by remember { mutableStateOf(false) }
    val isImpact = hammerCycle in 0.46f..0.54f
    LaunchedEffect(isImpact) {
        if (isImpact && !lastImpactPlayed) {
            WuxiaAudioEngine.playSwordClash()
            lastImpactPlayed = true
        } else if (!isImpact) {
            lastImpactPlayed = false
        }
    }

    val forgeBg = rememberWuxiaGraphic("bg_forge_hall")

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(200.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF0B0710))
            .border(2.dp, Color(0xFFE8C37E), RoundedCornerShape(12.dp))
    ) {
        // Tło Manhwa Świętej Kuźni
        if (forgeBg != null) {
            Image(
                bitmap = forgeBg,
                contentDescription = "Święta Kuźnia Artefaktów",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color.Black.copy(alpha = 0.3f),
                                Color.Black.copy(alpha = 0.55f)
                            )
                        )
                    )
            )
        }

        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val cx = w * 0.5f
            val cy = h * 0.65f

            // Żar i lawa w tle kowadła
            drawOval(
                brush = Brush.radialGradient(
                    listOf(Color(0xFFF97316).copy(alpha = 0.6f), Color(0xFFEF4444).copy(alpha = 0.3f), Color.Transparent),
                    center = Offset(cx, cy - 20f),
                    radius = 80f
                ),
                topLeft = Offset(cx - 80f, cy - 50f),
                size = Size(160f, 60f)
            )

            // Kowadło z rzeźbionego gwiezdnego żelaza
            drawRect(color = Color(0xFF4A4239), topLeft = Offset(cx - 40f, cy + 5f), size = Size(80f, 35f))
            drawRect(color = Color(0xFF6E6557), topLeft = Offset(cx - 50f, cy - 8f), size = Size(100f, 15f))

            // Rozżarzone do białości ostrze miecza
            drawRect(
                brush = Brush.horizontalGradient(listOf(Color(0xFFFFFBEB), Color(0xFFE9C06A), Color(0xFFEF4444))),
                topLeft = Offset(cx - 30f, cy - 14f),
                size = Size(60f, 7f)
            )

            // Młot Niebiańskiego Kowala uderzający rytmicznie
            val hammerAngle = if (hammerCycle < 0.5f) {
                -50f + (hammerCycle / 0.5f) * 50f // swing down
            } else {
                -((hammerCycle - 0.5f) / 0.5f) * 50f // reset
            }

            val hammerPivotX = cx + 32f
            val hammerPivotY = cy - 40f
            val rad = Math.toRadians(hammerAngle.toDouble())
            val headX = hammerPivotX - cos(rad).toFloat() * 36f
            val headY = hammerPivotY - sin(rad).toFloat() * 36f

            drawLine(color = Color(0xFF78350F), start = Offset(hammerPivotX, hammerPivotY), end = Offset(headX, headY), strokeWidth = 5f)
            drawRect(
                brush = Brush.verticalGradient(listOf(Color(0xFFEDE4CF), Color(0xFF64748B))),
                topLeft = Offset(headX - 12f, headY - 8f),
                size = Size(24f, 16f)
            )

            // Kaskada złotych iskier przy uderzeniu
            if (isImpact) {
                val sparkColors = listOf(Color(0xFFFFFBEB), Color(0xFFE9C06A), Color(0xFFF97316), Color(0xFFEF4444))
                for (i in 0 until 18) {
                    val angle = (i * 20) * (Math.PI / 180f)
                    val dist = Random.nextFloat() * 45f + 15f
                    val sx = cx + cos(angle).toFloat() * dist
                    val sy = (cy - 12f) + sin(angle).toFloat() * dist * 0.7f - 10f
                    drawCircle(color = sparkColors[i % sparkColors.size], radius = Random.nextFloat() * 3.5f + 1.5f, center = Offset(sx, sy))
                }
            }
        }

        // Podpis kuźni
        Column(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(10.dp)
        ) {
            Text(
                text = "🔨 ŚWIĘTA KUŹNIA ARTEFAKTÓW DAO",
                color = Color(0xFFF3DDA6),
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp
            )
            Text(
                text = "Nasycanie Gwiezdnego Żelaza Pieczęciami Dusz",
                color = Color(0xFFB0A592),
                fontSize = 10.sp
            )
        }
    }
}
