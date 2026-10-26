package com.example.ui.graphics

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.audio.WuxiaAudioEngine
import com.example.ui.pixelart.PixelPalette
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

enum class AlchemyFusionPhase {
    IDLE,
    HEATING_SEAL,
    CONDENSATION_VORTEX,
    BREAKTHROUGH_REVEAL
}

/**
 * Moduł II: Kinematyczna Ceremonia Alchemii 2D (Klasa AAA Xianxia)
 * - Trójfazowa fuzja z wyborem katalizatorów i esencji
 * - Dynamiczny wir płomienia geocentrycznego
 * - Lewitacja legendarnej pigułki 9 Chmur z wybuchem boskiego blasku
 */
@Composable
fun Wuxia2dCauldronInteractive(
    flameName: String,
    bonusRate: Int,
    onBrewSuccess: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()

    var fusionPhase by remember { mutableStateOf(AlchemyFusionPhase.IDLE) }
    var selectedCatalyst by remember { mutableStateOf("Lotos Geocentrycznego Płomienia") }
    var qiHeatLevel by remember { mutableStateOf(85) } // 50 - 100%
    var pillQuality by remember { mutableStateOf("Boska Jakość (9 Chmur)") }

    // Wstrząsy kotła podczas fazy kondensacji
    var cauldronShakeX by remember { mutableStateOf(0f) }
    var cauldronShakeY by remember { mutableStateOf(0f) }

    val infiniteTransition = rememberInfiniteTransition(label = "cauldron_flame_loop")
    val flameFlicker by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.35f,
        animationSpec = infiniteRepeatable(
            animation = tween(380, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "flame_flicker"
    )

    val vortexRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "vortex"
    )

    // Grafiki Manhwa
    val flameLotusImg = rememberWuxiaGraphic("item_geocentric_flame")
    val pillImg = rememberWuxiaGraphic("item_pill_breakthrough")

    fun triggerCinematicBrew() {
        if (fusionPhase != AlchemyFusionPhase.IDLE) return
        coroutineScope.launch {
            // Faza 1: Ogrzewanie i zapieczętowanie runami
            fusionPhase = AlchemyFusionPhase.HEATING_SEAL
            WuxiaAudioEngine.playCauldronFlame()
            delay(1200)

            // Faza 2: Wir kondensacji i wibracja pieca
            fusionPhase = AlchemyFusionPhase.CONDENSATION_VORTEX
            val shakeJob = launch {
                for (i in 0..25) {
                    cauldronShakeX = (Random.nextFloat() - 0.5f) * 14f
                    cauldronShakeY = (Random.nextFloat() - 0.5f) * 10f
                    delay(35)
                }
                cauldronShakeX = 0f
                cauldronShakeY = 0f
            }
            WuxiaAudioEngine.playUltimateBurst()
            delay(1500)
            shakeJob.join()

            // Faza 3: Przełom i boski rozbłysk
            fusionPhase = AlchemyFusionPhase.BREAKTHROUGH_REVEAL
            WuxiaAudioEngine.playDivineGong()
            pillQuality = when (Random.nextInt(100)) {
                in 0..15 -> "Czysta Jakość (6 Chmur)"
                in 16..60 -> "Niebiańska Jakość (8 Chmur)"
                else -> "Boska Jakość (9 Chmur - Bezbłędna)"
            }
            onBrewSuccess?.invoke()
            delay(3500)
            fusionPhase = AlchemyFusionPhase.IDLE
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xFF0B0713))
            .border(2.dp, Color(0xFFFFD700), RoundedCornerShape(14.dp))
            .padding(10.dp)
    ) {
        // Tytuł i Stan Pieca
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = "⚗️", fontSize = 16.sp)
                Spacer(Modifier.width(6.dp))
                Column {
                    Text(
                        text = "KOCIOŁ GEOCENTRYCZNEJ FUZJI",
                        color = Color(0xFFFFE082),
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                    Text(
                        text = "$flameName • +$bonusRate% Szansy na Przełom",
                        color = Color(0xFF00E5FF),
                        fontSize = 10.sp
                    )
                }
            }

            Surface(
                color = when (fusionPhase) {
                    AlchemyFusionPhase.IDLE -> Color(0xFF1E1B4B)
                    AlchemyFusionPhase.HEATING_SEAL -> Color(0xFFB45309)
                    AlchemyFusionPhase.CONDENSATION_VORTEX -> Color(0xFF7C2D12)
                    AlchemyFusionPhase.BREAKTHROUGH_REVEAL -> Color(0xFF065F46)
                },
                shape = RoundedCornerShape(6.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFFD700))
            ) {
                Text(
                    text = when (fusionPhase) {
                        AlchemyFusionPhase.IDLE -> "STAN: GOTOWY"
                        AlchemyFusionPhase.HEATING_SEAL -> "FAZA I: ZAPECZĘTOWANIE"
                        AlchemyFusionPhase.CONDENSATION_VORTEX -> "FAZA II: WIR KONDENSACJI"
                        AlchemyFusionPhase.BREAKTHROUGH_REVEAL -> "FAZA III: PRZEŁOM!"
                    },
                    color = Color.White,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Główna Scena Kinematyczna Kotła (Canvas + Manhwa)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(210.dp)
                .clip(RoundedCornerShape(12.dp))
                .border(1.5.dp, Color(0xFFD97706), RoundedCornerShape(12.dp))
                .background(Color(0xFF08040C))
        ) {
            // Tło ognistych iskier
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height
                val cx = w * 0.5f + cauldronShakeX
                val cy = h * 0.52f + cauldronShakeY

                // Podłoże komory lawy
                drawRect(
                    brush = Brush.verticalGradient(
                        listOf(Color(0xFF0E0716), Color(0xFF260D04), Color(0xFF431407))
                    ),
                    size = Size(w, h)
                )

                // Płomienie pod kotłem
                val flameMult = if (fusionPhase == AlchemyFusionPhase.CONDENSATION_VORTEX) 1.8f else 1.1f
                val flameH = 45f * flameFlicker * flameMult
                val flamePath = Path().apply {
                    moveTo(cx - 50f, cy + 50f)
                    quadraticBezierTo(cx - 20f, cy + 40f - flameH, cx, cy + 50f - flameH * 1.2f)
                    quadraticBezierTo(cx + 20f, cy + 40f - flameH, cx + 50f, cy + 50f)
                    close()
                }
                drawPath(
                    path = flamePath,
                    brush = Brush.verticalGradient(
                        listOf(Color(0xFFFDE047), Color(0xFFF97316), Color(0xFFDC2626)),
                        startY = cy + 50f - flameH * 1.2f,
                        endY = cy + 50f
                    )
                )

                // Faza 2: Wirujące cząsteczki Qi wpadające do pieca
                if (fusionPhase == AlchemyFusionPhase.CONDENSATION_VORTEX) {
                    for (i in 0..11) {
                        val angle = (vortexRotation + i * 30f) * (PI.toFloat() / 180f)
                        val dist = 55f + (i % 3) * 15f
                        val px = cx + cos(angle) * dist
                        val py = cy + sin(angle) * dist * 0.55f
                        drawCircle(
                            color = if (i % 2 == 0) Color(0xFF00E5FF) else Color(0xFFFFD700),
                            radius = 4f,
                            center = Offset(px, py)
                        )
                    }
                }

                // Ciało z brązu/nefrytu kotła
                drawOval(
                    brush = Brush.radialGradient(
                        listOf(Color(0xFFB45309), Color(0xFF78350F), Color(0xFF1E0B02)),
                        center = Offset(cx - 15f, cy - 5f)
                    ),
                    topLeft = Offset(cx - 55f, cy - 25f),
                    size = Size(110f, 65f)
                )

                // Złote runy pieca Bagua
                drawCircle(
                    color = Color(0xFFFFD700).copy(alpha = if (fusionPhase != AlchemyFusionPhase.IDLE) 0.9f else 0.5f),
                    radius = 18f,
                    center = Offset(cx, cy + 5f),
                    style = Stroke(width = 2.5f)
                )

                // Nogi trójnogu
                drawLine(color = Color(0xFF475569), start = Offset(cx - 40f, cy + 25f), end = Offset(cx - 55f, cy + 60f), strokeWidth = 6f, cap = StrokeCap.Round)
                drawLine(color = Color(0xFF475569), start = Offset(cx + 40f, cy + 25f), end = Offset(cx + 55f, cy + 60f), strokeWidth = 6f, cap = StrokeCap.Round)
                drawLine(color = Color(0xFF334155), start = Offset(cx, cy + 30f), end = Offset(cx, cy + 62f), strokeWidth = 6f, cap = StrokeCap.Round)

                // Uszy kotła
                drawArc(
                    color = Color(0xFFB45309),
                    startAngle = 140f,
                    sweepAngle = 180f,
                    useCenter = false,
                    topLeft = Offset(cx - 70f, cy - 20f),
                    size = Size(25f, 35f),
                    style = Stroke(width = 5f)
                )
                drawArc(
                    color = Color(0xFFB45309),
                    startAngle = 220f,
                    sweepAngle = 180f,
                    useCenter = false,
                    topLeft = Offset(cx + 45f, cy - 20f),
                    size = Size(25f, 35f),
                    style = Stroke(width = 5f)
                )

                // Faza 3: Filar boskiego światła przełomu
                if (fusionPhase == AlchemyFusionPhase.BREAKTHROUGH_REVEAL) {
                    drawRect(
                        brush = Brush.verticalGradient(
                            listOf(Color.White, Color(0xFFFFD700).copy(alpha = 0.8f), Color.Transparent)
                        ),
                        topLeft = Offset(cx - 40f, 0f),
                        size = Size(80f, cy + 10f)
                    )
                }
            }

            // Lewitująca Pigułka Manhwa podczas Fazy Przełomu
            if (fusionPhase == AlchemyFusionPhase.BREAKTHROUGH_REVEAL) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 18.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // Poświata aury
                    Box(
                        modifier = Modifier
                            .size(105.dp)
                            .background(
                                Brush.radialGradient(
                                    listOf(Color(0xFFFFD700).copy(alpha = 0.8f), Color(0xFF00E5FF).copy(alpha = 0.4f), Color.Transparent)
                                ),
                                shape = CircleShape
                            )
                    )

                    if (pillImg != null) {
                        Image(
                            bitmap = pillImg,
                            contentDescription = "Pigułka 9 Chmur",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(88.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .border(2.dp, Color(0xFFFFD700), RoundedCornerShape(12.dp))
                        )
                    }

                    // Etykieta Jakości Pigułki
                    Surface(
                        color = Color.Black.copy(alpha = 0.85f),
                        shape = RoundedCornerShape(6.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFFD700)),
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .offset(y = 18.dp)
                    ) {
                        Text(
                            text = "✨ $pillQuality",
                            color = Color(0xFFFFE082),
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Black,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Wybór Katalizatorów i Esencji
        Text(
            text = "Święte Esencje i Katalizatory Kultywacji:",
            color = Color(0xFFFFE082),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(4.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            listOf(
                "Lotos Geocentrycznego Płomienia" to "🔥",
                "Tysiącletni Żeń-szeń" to "🌿",
                "Rosa Jutrzenki" to "💧"
            ).forEach { (catName, icon) ->
                val isSelected = selectedCatalyst == catName
                Surface(
                    color = if (isSelected) Color(0xFF78350F) else Color(0xFF1E1B4B),
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (isSelected) Color(0xFFFFD700) else Color(0xFF4338CA)
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .clickable { selectedCatalyst = catName }
                ) {
                    Column(
                        modifier = Modifier.padding(vertical = 6.dp, horizontal = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(text = icon, fontSize = 16.sp)
                        Text(
                            text = catName.take(13),
                            color = if (isSelected) Color(0xFFFFE082) else Color(0xFFCBD5E1),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Przycisk Uruchomienia Ceremonii Fuzji
        Button(
            onClick = { triggerCinematicBrew() },
            enabled = fusionPhase == AlchemyFusionPhase.IDLE,
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFFD97706),
                disabledContainerColor = Color(0xFF451A03)
            ),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("cinematic_fusion_btn")
        ) {
            Text(
                text = if (fusionPhase == AlchemyFusionPhase.IDLE) {
                    "🔥 ROZPOCZNIJ CEREMONIE FUZJI ALCHEMICZNEJ"
                } else {
                    "⏳ TRWA PRZEŁOM KOSMICZNEGO PŁOMIENIA..."
                },
                color = if (fusionPhase == AlchemyFusionPhase.IDLE) Color.White else Color(0xFFFFD700),
                fontWeight = FontWeight.Black,
                fontSize = 11.sp
            )
        }
    }
}
