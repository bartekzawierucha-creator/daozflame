package com.example.ui.graphics

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.TimeOfDay
import com.example.ui.pixelart.PixelPalette
import kotlin.math.cos
import kotlin.math.sin

enum class SanctuaryBuilding(val title: String, val icon: String) {
    MAIN_HALL("Główna Sala Sekty", "🏛️"),
    ALCHEMY_PAVILION("Kocioł Alchemii", "⚗️"),
    DIVINE_FORGE("Boska Kuźnia", "🔨"),
    CULTIVATION_PEAK("Szczyt Kultywacji Qi", "🧘"),
    MARTIAL_GROUNDS("Plac Treningowy", "⚔️")
}

@Composable
fun Wuxia2dSanctuaryCanvas(
    timeOfDay: TimeOfDay,
    disciplesCount: Int,
    readyBreakthroughCount: Int,
    onBuildingClick: (SanctuaryBuilding) -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "sanctuary_anim")
    val timeAnim by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(24000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "time_float"
    )

    val bounceAnim by infiniteTransition.animateFloat(
        initialValue = -4f,
        targetValue = 4f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bounce"
    )

    val particleSystem = remember { WuxiaParticleSystem(35) }
    var tappedBuilding by remember { mutableStateOf<SanctuaryBuilding?>(null) }
    val bgManhwa = rememberWuxiaGraphic("bg_sect_sanctuary")

    var quoteIndex by remember { mutableStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(4500)
            quoteIndex = (quoteIndex + 1) % 4
        }
    }
    val discipleQuotes = listOf(
        "Mistrzu Chen Tian! Dziś w nocy moje Dao doznało oświecenia!",
        "Mój miecz czeka na wielką próbę w Niebiańskiej Arenie!",
        "Kocioł w Pawilonie Alchemii buzuje geocentrycznym żarem!",
        "Wchłaniam gwiezdne promienie Qi wprost do rdzenia Dantian..."
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(290.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFF090610))
            .border(2.dp, Color(0xFFFFD700), RoundedCornerShape(14.dp))
    ) {
        // Tło Manhwa Góry Sekty
        if (bgManhwa != null) {
            androidx.compose.foundation.Image(
                bitmap = bgManhwa,
                contentDescription = "Góra Sekty Manhwa",
                contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color.Black.copy(alpha = 0.2f),
                                Color.Black.copy(alpha = 0.45f)
                            )
                        )
                    )
            )
        }

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures { offset ->
                        val w = size.width
                        val h = size.height

                        // Interactive hotspot detection on 2D scenery
                        when {
                            // Top left: Cultivation Peak
                            offset.x < w * 0.35f && offset.y < h * 0.5f -> {
                                tappedBuilding = SanctuaryBuilding.CULTIVATION_PEAK
                                onBuildingClick(SanctuaryBuilding.CULTIVATION_PEAK)
                            }
                            // Center: Main Sect Pagoda
                            offset.x in (w * 0.35f)..(w * 0.65f) && offset.y < h * 0.7f -> {
                                tappedBuilding = SanctuaryBuilding.MAIN_HALL
                                onBuildingClick(SanctuaryBuilding.MAIN_HALL)
                            }
                            // Top right: Alchemy Pavilion
                            offset.x > w * 0.65f && offset.y < h * 0.5f -> {
                                tappedBuilding = SanctuaryBuilding.ALCHEMY_PAVILION
                                onBuildingClick(SanctuaryBuilding.ALCHEMY_PAVILION)
                            }
                            // Bottom left: Divine Forge
                            offset.x < w * 0.5f && offset.y >= h * 0.5f -> {
                                tappedBuilding = SanctuaryBuilding.DIVINE_FORGE
                                onBuildingClick(SanctuaryBuilding.DIVINE_FORGE)
                            }
                            // Bottom right: Martial Training Grounds
                            else -> {
                                tappedBuilding = SanctuaryBuilding.MARTIAL_GROUNDS
                                onBuildingClick(SanctuaryBuilding.MARTIAL_GROUNDS)
                            }
                        }
                    }
                }
        ) {
            val w = size.width
            val h = size.height
            val tSec = timeAnim * 0.05f

            if (bgManhwa == null) {
                // Fallback proceduralny
                draw2dSky(w, h, timeOfDay, timeAnim)
                drawCelestialBody(w, h, timeOfDay, timeAnim)
                drawAuspiciousCloud(w * 0.32f, h * 0.20f, scale = 0.8f, color = GildedGold.copy(alpha = 0.4f))
                drawAuspiciousCloud(w * 0.70f, h * 0.15f, scale = 0.7f, color = GildedGold.copy(alpha = 0.35f))
                drawDistantMountains(w, h)
                drawFloatingIslands(w, h, bounceAnim)
                drawSanctuaryPavilions(w, h, timeAnim)
            }

            // Uczniowie medytujący i trenujący
            drawDisciplesInScene(w, h, bounceAnim, readyBreakthroughCount)
            drawBambooForeground(w, h)

            // Cząsteczki płatków wiśni i mgły Qi
            particleSystem.updateAndDraw(this, w, h, tSec)
        }

        // Dymek dialogowy adepta Manhwa
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 12.dp)
                .background(Color.Black.copy(alpha = 0.85f), RoundedCornerShape(8.dp))
                .border(1.dp, Color(0xFFFFD700), RoundedCornerShape(8.dp))
                .padding(horizontal = 10.dp, vertical = 5.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = "💬", fontSize = 12.sp)
                Spacer(Modifier.width(6.dp))
                Text(
                    text = discipleQuotes[quoteIndex],
                    color = Color(0xFFFFE082),
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        // HUD Overlay on 2D Canvas Scene
        Row(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(8.dp)
                .background(Color.Black.copy(alpha = 0.75f), RoundedCornerShape(6.dp))
                .border(1.dp, Color(0xFFFFD700), RoundedCornerShape(6.dp))
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "🏯 GÓRA ŚWIĘTEGO SMOKA (KLIKNIJ BUDYNEK NA WZGÓRZU)",
                color = Color(0xFFFFE082),
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )
        }

        if (readyBreakthroughCount > 0) {
            Row(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
                    .background(Color(0xFF991B1B).copy(alpha = 0.9f), RoundedCornerShape(6.dp))
                    .border(1.dp, Color(0xFFFFD700), RoundedCornerShape(6.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "⚡ GOTOWYCH DO PRZEŁOMU: $readyBreakthroughCount",
                    color = Color(0xFFFFE082),
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

private fun DrawScope.draw2dSky(w: Float, h: Float, timeOfDay: TimeOfDay, timeAnim: Float) {
    val skyColors = when (timeOfDay) {
        TimeOfDay.DAWN -> listOf(Color(0xFF831843), Color(0xFFC2410C), Color(0xFF1E1B4B))
        TimeOfDay.DAY -> listOf(Color(0xFF0369A1), Color(0xFF0284C7), Color(0xFF0C4A6E))
        TimeOfDay.DUSK -> listOf(Color(0xFF4C1D95), Color(0xFF991B1B), Color(0xFF1E1B4B))
        TimeOfDay.NIGHT -> listOf(Color(0xFF090D16), Color(0xFF0B132B), Color(0xFF1C2541))
        TimeOfDay.FULL_MOON -> listOf(Color(0xFF1E1B4B), Color(0xFF2E1065), Color(0xFF0A0A1A))
    }

    drawRect(
        brush = Brush.verticalGradient(skyColors),
        size = Size(w, h)
    )

    // Starry night twinkling stars
    if (timeOfDay == TimeOfDay.NIGHT || timeOfDay == TimeOfDay.FULL_MOON) {
        val starSeeds = listOf(
            Offset(w * 0.15f, h * 0.12f),
            Offset(w * 0.28f, h * 0.08f),
            Offset(w * 0.45f, h * 0.15f),
            Offset(w * 0.62f, h * 0.07f),
            Offset(w * 0.82f, h * 0.14f),
            Offset(w * 0.90f, h * 0.22f)
        )
        starSeeds.forEachIndexed { i, pos ->
            val twinkle = (sin(timeAnim * 0.1f + i) + 1f) * 0.5f
            drawCircle(
                color = Color.White.copy(alpha = 0.3f + twinkle * 0.7f),
                radius = 1.5f + twinkle * 1.5f,
                center = pos
            )
        }
    }
}

private fun DrawScope.drawCelestialBody(w: Float, h: Float, timeOfDay: TimeOfDay, timeAnim: Float) {
    if (timeOfDay == TimeOfDay.DAY || timeOfDay == TimeOfDay.DAWN) {
        // Golden Daoist Sun
        val sunCenter = Offset(w * 0.75f, h * 0.22f)
        // Outer radiant glow rings
        drawCircle(
            brush = Brush.radialGradient(
                listOf(Color(0xFFFDE68A).copy(alpha = 0.6f), Color.Transparent),
                center = sunCenter,
                radius = 45f
            ),
            radius = 45f,
            center = sunCenter
        )
        drawCircle(color = Color(0xFFFBBF24), radius = 18f, center = sunCenter)
    } else {
        // Celestial Jade Moon
        val moonCenter = Offset(w * 0.8f, h * 0.18f)
        drawCircle(
            brush = Brush.radialGradient(
                listOf(Color(0xFF67E8F9).copy(alpha = 0.45f), Color.Transparent),
                center = moonCenter,
                radius = 40f
            ),
            radius = 40f,
            center = moonCenter
        )
        drawCircle(color = Color(0xFFE0F2FE), radius = 16f, center = moonCenter)
    }
}

private fun DrawScope.drawDistantMountains(w: Float, h: Float) {
    // Back ridge (Kunlun peaks)
    val path1 = Path().apply {
        moveTo(0f, h * 0.65f)
        lineTo(w * 0.18f, h * 0.32f)
        lineTo(w * 0.38f, h * 0.52f)
        lineTo(w * 0.60f, h * 0.28f)
        lineTo(w * 0.82f, h * 0.48f)
        lineTo(w, h * 0.35f)
        lineTo(w, h)
        lineTo(0f, h)
        close()
    }
    drawPath(path1, color = Color(0xFF0F172A).copy(alpha = 0.85f))

    // Middle misty mountain pass
    val path2 = Path().apply {
        moveTo(0f, h * 0.75f)
        lineTo(w * 0.25f, h * 0.55f)
        lineTo(w * 0.5f, h * 0.68f)
        lineTo(w * 0.75f, h * 0.52f)
        lineTo(w, h * 0.68f)
        lineTo(w, h)
        lineTo(0f, h)
        close()
    }
    drawPath(path2, color = Color(0xFF1E293B).copy(alpha = 0.95f))
}

private fun DrawScope.drawFloatingIslands(w: Float, h: Float, bounce: Float) {
    // Left Floating Cultivation Island
    val islLeftY = h * 0.38f + bounce
    drawOval(
        brush = Brush.verticalGradient(listOf(Color(0xFF10B981), Color(0xFF064E3B), Color(0xFF022C22))),
        topLeft = Offset(w * 0.08f, islLeftY),
        size = Size(w * 0.26f, 32f)
    )

    // Right Floating Alchemy Island
    val islRightY = h * 0.36f - bounce
    drawOval(
        brush = Brush.verticalGradient(listOf(Color(0xFFD97706), Color(0xFF78350F), Color(0xFF451A03))),
        topLeft = Offset(w * 0.68f, islRightY),
        size = Size(w * 0.25f, 30f)
    )

    // Ethereal waterfall from left floating island
    val waterfallPath = Path().apply {
        moveTo(w * 0.20f, islLeftY + 16f)
        lineTo(w * 0.22f, h * 0.75f)
        lineTo(w * 0.25f, h * 0.75f)
        lineTo(w * 0.23f, islLeftY + 16f)
        close()
    }
    drawPath(waterfallPath, color = Color(0xFF38BDF8).copy(alpha = 0.5f))
}

private fun DrawScope.drawSanctuaryPavilions(w: Float, h: Float, timeAnim: Float) {
    // 1. Central Great Main Pagoda (Siedziba Sekty)
    val pagodaCenterX = w * 0.5f
    val pagodaBaseY = h * 0.72f

    // Pagoda Foundation Terrace
    drawRoundRect(
        color = Color(0xFF334155),
        topLeft = Offset(pagodaCenterX - 55f, pagodaBaseY),
        size = Size(110f, 35f),
        cornerRadius = CornerRadius(4f, 4f)
    )

    // Pagoda Tier 1 Roof (Curved Chinese Eaves)
    drawPagodaRoof(pagodaCenterX, pagodaBaseY - 10f, width = 130f, height = 24f, roofColor = Color(0xFFF59E0B))

    // Pagoda Tier 2 Body
    drawRect(
        color = Color(0xFFB45309),
        topLeft = Offset(pagodaCenterX - 35f, pagodaBaseY - 45f),
        size = Size(70f, 35f)
    )
    // Pagoda Doors/Windows
    drawRect(color = Color(0xFF0F172A), topLeft = Offset(pagodaCenterX - 14f, pagodaBaseY - 32f), size = Size(28f, 22f))

    // Pagoda Tier 2 Roof
    drawPagodaRoof(pagodaCenterX, pagodaBaseY - 55f, width = 100f, height = 20f, roofColor = Color(0xFFF59E0B))

    // Golden Spire on Top
    drawLine(
        color = Color(0xFFFDE68A),
        start = Offset(pagodaCenterX, pagodaBaseY - 55f),
        end = Offset(pagodaCenterX, pagodaBaseY - 78f),
        strokeWidth = 4f
    )
    drawCircle(color = Color(0xFFF59E0B), radius = 5f, center = Offset(pagodaCenterX, pagodaBaseY - 80f))

    // 2. Left Peak: Cultivation Terrace
    val cultX = w * 0.21f
    val cultY = h * 0.32f
    drawPagodaRoof(cultX, cultY, width = 60f, height = 15f, roofColor = Color(0xFF10B981))

    // 3. Right Peak: Alchemy Chamber
    val alchX = w * 0.80f
    val alchY = h * 0.30f
    drawPagodaRoof(alchX, alchY, width = 60f, height = 15f, roofColor = Color(0xFFEF4444))

    // Alchemy Smoke puff
    val smokeY = alchY - 20f - (timeAnim % 30f)
    drawCircle(color = Color(0xFFA855F7).copy(alpha = 0.5f), radius = 8f, center = Offset(alchX, smokeY))

    // 4. Bottom Left: Divine Forge
    val forgeX = w * 0.25f
    val forgeY = h * 0.78f
    drawRect(color = Color(0xFF475569), topLeft = Offset(forgeX - 25f, forgeY), size = Size(50f, 30f))
    drawPagodaRoof(forgeX, forgeY - 6f, width = 65f, height = 14f, roofColor = Color(0xFF991B1B))
    // Glowing Forge window
    drawRect(color = Color(0xFFF97316), topLeft = Offset(forgeX - 8f, forgeY + 8f), size = Size(16f, 14f))

    // 5. Bottom Right: Martial Training Grounds
    val groundsX = w * 0.78f
    val groundsY = h * 0.78f
    drawRoundRect(
        color = Color(0xFF1E293B),
        topLeft = Offset(groundsX - 45f, groundsY),
        size = Size(90f, 26f),
        cornerRadius = CornerRadius(4f, 4f)
    )
}

private fun DrawScope.drawPagodaRoof(cx: Float, cy: Float, width: Float, height: Float, roofColor: Color) {
    val halfW = width * 0.5f
    val path = Path().apply {
        moveTo(cx - halfW - 8f, cy + height) // Left sweeping eave
        quadraticBezierTo(cx - halfW * 0.4f, cy + height * 0.4f, cx, cy) // Peak
        quadraticBezierTo(cx + halfW * 0.4f, cy + height * 0.4f, cx + halfW + 8f, cy + height) // Right sweeping eave
        lineTo(cx + halfW - 4f, cy + height + 4f)
        lineTo(cx - halfW + 4f, cy + height + 4f)
        close()
    }
    drawPath(path, color = roofColor)
}

private fun DrawScope.drawDisciplesInScene(w: Float, h: Float, bounce: Float, readyBreakthrough: Int) {
    // Disciple 1: Meditating on Left Cultivation Peak with rotating Qi aura
    val d1Center = Offset(w * 0.21f, h * 0.35f + bounce)
    drawCircle(color = Color(0xFF38BDF8).copy(alpha = 0.4f), radius = 14f, center = d1Center)
    drawCircle(color = Color(0xFF38BDF8), radius = 14f, center = d1Center, style = Stroke(width = 1.5f))
    // Meditating disciple silhouette
    drawCircle(color = Color(0xFFF1F5F9), radius = 4f, center = Offset(d1Center.x, d1Center.y - 4f))
    drawRect(color = Color(0xFF0F766E), topLeft = Offset(d1Center.x - 5f, d1Center.y), size = Size(10f, 6f))

    // Disciple 2: Training on Martial Grounds with flying sword
    val d2Center = Offset(w * 0.74f, h * 0.81f)
    drawCircle(color = Color(0xFFF1F5F9), radius = 4f, center = Offset(d2Center.x, d2Center.y - 7f))
    drawRect(color = Color(0xFF3B82F6), topLeft = Offset(d2Center.x - 4f, d2Center.y - 3f), size = Size(8f, 10f))

    // Animated Flying Sword circulating above the swordsman
    val swordOrbitRadius = 18f
    val swordX = d2Center.x + cos(bounce * 0.5f) * swordOrbitRadius
    val swordY = d2Center.y - 12f + sin(bounce * 0.5f) * 8f
    drawLine(
        color = Color(0xFF67E8F9),
        start = Offset(swordX - 6f, swordY - 6f),
        end = Offset(swordX + 6f, swordY + 6f),
        strokeWidth = 2.5f
    )

    // Disciple 3: Near Grand Hall
    val d3Center = Offset(w * 0.44f, h * 0.80f)
    drawCircle(color = Color(0xFFF1F5F9), radius = 4f, center = Offset(d3Center.x, d3Center.y - 7f))
    drawRect(color = Color(0xFFD97706), topLeft = Offset(d3Center.x - 4f, d3Center.y - 3f), size = Size(8f, 10f))

    // Glowing breakthrough pillar if disciples are ready
    if (readyBreakthrough > 0) {
        drawLine(
            brush = Brush.verticalGradient(listOf(Color(0xFFF59E0B).copy(alpha = 0.8f), Color.Transparent)),
            start = Offset(w * 0.5f, h * 0.85f),
            end = Offset(w * 0.5f, h * 0.2f),
            strokeWidth = 14f
        )
    }
}

private fun DrawScope.drawBambooForeground(w: Float, h: Float) {
    // Left bamboo stalks & leaves
    drawLine(color = Color(0xFF064E3B).copy(alpha = 0.85f), start = Offset(12f, h), end = Offset(8f, h * 0.45f), strokeWidth = 5f)
    drawLine(color = Color(0xFF047857).copy(alpha = 0.85f), start = Offset(24f, h), end = Offset(20f, h * 0.35f), strokeWidth = 4f)
    // Left bamboo leaves
    drawCircle(color = Color(0xFF10B981).copy(alpha = 0.7f), radius = 6f, center = Offset(16f, h * 0.48f))
    drawCircle(color = Color(0xFF10B981).copy(alpha = 0.7f), radius = 5f, center = Offset(28f, h * 0.38f))

    // Right bamboo stalks
    drawLine(color = Color(0xFF064E3B).copy(alpha = 0.85f), start = Offset(w - 14f, h), end = Offset(w - 10f, h * 0.42f), strokeWidth = 5f)
    drawCircle(color = Color(0xFF10B981).copy(alpha = 0.7f), radius = 6f, center = Offset(w - 20f, h * 0.45f))
}
