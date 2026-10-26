package com.example.ui.graphics

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.CombatState
import com.example.model.MartialSchool
import com.example.ui.pixelart.PixelPalette
import com.example.ui.pixelart.PixelSegmentedBar
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * Moduł I: Dynamiczny Wizualny Auto-Battler 2D (Klasa AAA Xianxia)
 * - Pełnoformatowa arena manhwa z lewitującą platformą i runami Bagua
 * - Animowane smugi cięć miecza, deszcze strzał Qi i wstrząsy ekranu
 * - Pływające liczby obrażeń (Floating Damage Numbers)
 * - Płynna wymiana ciosów i techniki ostateczne Dao
 */
@Composable
fun Wuxia2dCombatArena(
    combatState: CombatState,
    onManualAttack: () -> Unit,
    onUsePill: () -> Unit,
    onUltimate: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "battle_arena_loop")

    // Płynny oddech i lewitacja postaci
    val heroFloatAnim by infiniteTransition.animateFloat(
        initialValue = -5f,
        targetValue = 5f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "hero_float"
    )

    val beastFloatAnim by infiniteTransition.animateFloat(
        initialValue = 4f,
        targetValue = -4f,
        animationSpec = infiniteRepeatable(
            animation = tween(1600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "beast_float"
    )

    // Animacja rotacji kręgów Bagua pod stopami
    val baguaRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(12000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "bagua_rot"
    )

    // Dynamiczny Screen Shake przy uderzeniu
    var shakeX by remember { mutableStateOf(0f) }
    var shakeY by remember { mutableStateOf(0f) }
    var flashWhite by remember { mutableStateOf(false) }

    LaunchedEffect(combatState.effectCounter) {
        if (combatState.effectCounter > 0) {
            when (combatState.currentEffect) {
                "CRIT", "ULTIMATE" -> {
                    flashWhite = true
                    for (i in 0..4) {
                        shakeX = (Random.nextFloat() - 0.5f) * 18f
                        shakeY = (Random.nextFloat() - 0.5f) * 18f
                        kotlinx.coroutines.delay(25)
                    }
                    flashWhite = false
                    shakeX = 0f
                    shakeY = 0f
                }
                "SLASH", "BEAST_ATTACK" -> {
                    for (i in 0..2) {
                        shakeX = (Random.nextFloat() - 0.5f) * 8f
                        shakeY = (Random.nextFloat() - 0.5f) * 8f
                        kotlinx.coroutines.delay(20)
                    }
                    shakeX = 0f
                    shakeY = 0f
                }
            }
        }
    }

    val arenaBg = rememberWuxiaGraphic("bg_combat_arena")
    val beastAvatarKey = WuxiaAssetLoader.getBeastAvatarKey(combatState.enemyName)
    val beastBitmap = rememberWuxiaGraphic(beastAvatarKey)
    val discipleAvatarKey = WuxiaAssetLoader.getSchoolAvatarKey(combatState.discipleSchool)
    val discipleBitmap = rememberWuxiaGraphic(discipleAvatarKey)

    val schoolAuraColor = when (combatState.discipleSchool) {
        MartialSchool.SWORD_IMMORTAL -> Color(0xFF00E5FF)
        MartialSchool.BODY_ASURA -> Color(0xFFFF5252)
        MartialSchool.ELEMENTAL_TAOIST -> Color(0xFFEBCB7A)
        MartialSchool.SHADOW_VIPER -> Color(0xFFE040FB)
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .offset { IntOffset(shakeX.toInt(), shakeY.toInt()) }
            .background(Color(0xFF09060E))
            .border(2.dp, Color(0xFFE8C37E), RoundedCornerShape(14.dp))
            .padding(10.dp)
    ) {
        // Nagłówek Areny z Tytułem
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp, 16.dp)
                        .background(Color(0xFFE8C37E), RoundedCornerShape(2.dp))
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = "ARENA DZIEWIĘCIU NIEBIOS",
                    color = Color(0xFFF3DDA6),
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    letterSpacing = 0.5.sp
                )
            }

            if (combatState.isVictory != null) {
                Surface(
                    color = if (combatState.isVictory == true) Color(0xFF065F46) else Color(0xFF7F1D1D),
                    shape = RoundedCornerShape(6.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE8C37E))
                ) {
                    Text(
                        text = if (combatState.isVictory == true) "🏆 ZWYCIĘSTWO" else "☠️ PORAŻKA",
                        color = Color.White,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Black,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            } else {
                Surface(
                    color = Color(0xFF1D1720),
                    shape = RoundedCornerShape(6.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF6366F1))
                ) {
                    Text(
                        text = "⚔️ AUTO-BATTLER",
                        color = Color(0xFFA5B4FC),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Główna Scena Walki 2D (Kinematyczna Manhwa)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(260.dp)
                .clip(RoundedCornerShape(12.dp))
                .border(1.5.dp, Color(0xFFE8C37E).copy(alpha = 0.8f), RoundedCornerShape(12.dp))
                .background(Color.Black)
        ) {
            // 1. Tło Areny (Ilustracja Manhwa lub gradient)
            if (arenaBg != null) {
                Image(
                    bitmap = arenaBg,
                    contentDescription = "Combat Arena Background",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                // Winieta nocna i mgła Qi
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    Color.Black.copy(alpha = 0.25f),
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.65f)
                                )
                            )
                        )
                )
            }

            // 2. Warstwa Canvas z efektami ciosów i cząsteczkami
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height

                val heroCenterX = w * 0.26f
                val beastCenterX = w * 0.74f
                val platformY = h * 0.76f

                // Runy Bagua pod uczniem
                drawCircle(
                    color = schoolAuraColor.copy(alpha = 0.25f),
                    radius = 42f,
                    center = Offset(heroCenterX, platformY)
                )
                drawCircle(
                    color = schoolAuraColor.copy(alpha = 0.5f),
                    radius = 35f,
                    center = Offset(heroCenterX, platformY),
                    style = Stroke(width = 2.5f)
                )

                // Krąg aury bestii
                drawCircle(
                    color = Color(0xFFFF3333).copy(alpha = 0.3f),
                    radius = 48f,
                    center = Offset(beastCenterX, platformY)
                )
                drawCircle(
                    color = Color(0xFFE8C37E).copy(alpha = 0.45f),
                    radius = 40f,
                    center = Offset(beastCenterX, platformY),
                    style = Stroke(width = 2f)
                )

                // Rysowanie efektów cięcia miecza w locie
                when (combatState.currentEffect) {
                    "SLASH" -> {
                        // Błękitno-złote cięcie miecza
                        val slashStart = Offset(heroCenterX + 20f, platformY - 60f)
                        val slashEnd = Offset(beastCenterX - 10f, platformY - 30f)
                        drawLine(
                            brush = Brush.linearGradient(listOf(Color.White, Color(0xFF00E5FF), Color.Transparent)),
                            start = slashStart,
                            end = slashEnd,
                            strokeWidth = 7f,
                            cap = StrokeCap.Round
                        )
                        // Rozbłysk uderzenia
                        drawCircle(
                            color = Color(0xFF00E5FF).copy(alpha = 0.7f),
                            radius = 28f,
                            center = slashEnd
                        )
                    }
                    "CRIT", "ULTIMATE" -> {
                        // Potężna fala energii rozbłysku
                        drawLine(
                            brush = Brush.linearGradient(listOf(Color.White, Color(0xFFE8C37E), Color(0xFFFF5252))),
                            start = Offset(heroCenterX, platformY - 70f),
                            end = Offset(beastCenterX, platformY - 40f),
                            strokeWidth = 12f,
                            cap = StrokeCap.Round
                        )
                        // Promienie eksplozji
                        for (i in 0..7) {
                            val angle = (i * 45) * (PI.toFloat() / 180f)
                            val rayLen = 45f
                            drawLine(
                                color = Color(0xFFE8C37E),
                                start = Offset(beastCenterX, platformY - 40f),
                                end = Offset(beastCenterX + cos(angle) * rayLen, platformY - 40f + sin(angle) * rayLen),
                                strokeWidth = 3f
                            )
                        }
                    }
                    "BEAST_ATTACK" -> {
                        // Szkarłatne pazury bestii
                        val clawCenter = Offset(heroCenterX, platformY - 40f)
                        for (offset in listOf(-16f, 0f, 16f)) {
                            drawLine(
                                color = Color(0xFFFF1744),
                                start = Offset(clawCenter.x + offset - 18f, clawCenter.y - 25f),
                                end = Offset(clawCenter.x + offset + 18f, clawCenter.y + 25f),
                                strokeWidth = 5f,
                                cap = StrokeCap.Round
                            )
                        }
                    }
                    "HEAL" -> {
                        // Zielone cząsteczki uzdrowienia wznoszące się w górę
                        for (i in 0..5) {
                            val px = heroCenterX + (i - 2.5f) * 12f
                            val py = platformY - 20f - (i * 10f)
                            drawCircle(color = Color(0xFF10B981), radius = 4f, center = Offset(px, py))
                        }
                    }
                }
            }

            // 3. Postać Adepta (Lewa Strona)
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 18.dp, bottom = (18f + heroFloatAnim).dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(contentAlignment = Alignment.Center) {
                        // Pulsująca aura
                        Box(
                            modifier = Modifier
                                .size(96.dp)
                                .background(
                                    Brush.radialGradient(
                                        listOf(schoolAuraColor.copy(alpha = 0.6f), Color.Transparent)
                                    ),
                                    shape = CircleShape
                                )
                        )

                        if (discipleBitmap != null) {
                            Image(
                                bitmap = discipleBitmap,
                                contentDescription = combatState.discipleName,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(86.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .border(2.dp, Color(0xFFE8C37E), RoundedCornerShape(12.dp))
                            )
                        } else {
                            ManhwaSchoolAvatar(
                                school = combatState.discipleSchool,
                                size = 86.dp
                            )
                        }
                    }

                    Spacer(Modifier.height(4.dp))
                    Surface(
                        color = Color.Black.copy(alpha = 0.8f),
                        shape = RoundedCornerShape(6.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, schoolAuraColor)
                    ) {
                        Text(
                            text = combatState.discipleName,
                            color = Color(0xFFF3DDA6),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            // 4. Mityczna Bestia / Rywal (Prawa Strona)
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 18.dp, bottom = (18f + beastFloatAnim).dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(contentAlignment = Alignment.Center) {
                        Box(
                            modifier = Modifier
                                .size(100.dp)
                                .background(
                                    Brush.radialGradient(
                                        listOf(Color(0xFFFF3333).copy(alpha = 0.5f), Color.Transparent)
                                    ),
                                    shape = CircleShape
                                )
                        )

                        if (beastBitmap != null) {
                            Image(
                                bitmap = beastBitmap,
                                contentDescription = combatState.enemyName,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(90.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .border(2.dp, Color(0xFFFF5252), RoundedCornerShape(12.dp))
                            )
                        }
                    }

                    Spacer(Modifier.height(4.dp))
                    Surface(
                        color = Color.Black.copy(alpha = 0.8f),
                        shape = RoundedCornerShape(6.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFF5252))
                    ) {
                        Text(
                            text = combatState.enemyName.take(18),
                            color = Color(0xFFFF8A80),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            // 5. Pływające Liczby Obrażeń (Floating Damage Numbers)
            combatState.floatingDamages.forEach { dmg ->
                val ageMs = (System.currentTimeMillis() - dmg.createdAt).coerceAtLeast(0)
                if (ageMs < 1200) {
                    val progress = ageMs / 1200f
                    val yOff = -progress * 48f
                    val alpha = (1f - progress).coerceIn(0f, 1f)

                    Box(
                        modifier = Modifier
                            .align(if (dmg.isDisciple) Alignment.CenterStart else Alignment.CenterEnd)
                            .padding(
                                start = if (dmg.isDisciple) 50.dp else 0.dp,
                                end = if (!dmg.isDisciple) 50.dp else 0.dp
                            )
                            .offset(y = yOff.dp)
                    ) {
                        Text(
                            text = dmg.text,
                            color = when {
                                dmg.isHeal -> Color(0xFF10B981)
                                dmg.isCrit -> Color(0xFFE8C37E)
                                dmg.isDisciple -> Color(0xFFFF5252)
                                else -> Color(0xFF00E5FF)
                            }.copy(alpha = alpha),
                            fontSize = if (dmg.isCrit) 16.sp else 13.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Black,
                            modifier = Modifier
                                .background(Color.Black.copy(alpha = 0.7f * alpha), RoundedCornerShape(4.dp))
                                .padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                    }
                }
            }

            // 6. Impact Frame Flash (60ms)
            if (flashWhite) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.White.copy(alpha = 0.55f))
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Paski Życia i Energii Qi
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            // Pasek Życia Adepta
            PixelSegmentedBar(
                current = combatState.discipleHp,
                max = combatState.discipleMaxHp,
                color = PixelPalette.Jade,
                label = "ADEPT HP (${combatState.discipleName})",
                segments = 16
            )

            // Pasek Qi Adepta
            PixelSegmentedBar(
                current = combatState.discipleQi,
                max = combatState.discipleMaxQi,
                color = PixelPalette.Cyan,
                label = "REZERWA QI (MIN. 30 DO BOSKIEGO DAO)",
                segments = 16
            )

            // Pasek Życia Bestii
            PixelSegmentedBar(
                current = combatState.enemyHp,
                max = combatState.enemyMaxHp,
                color = PixelPalette.Crimson,
                label = "MITYCZNA BESTIA (${combatState.enemyName.take(16)})",
                segments = 16
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Ostatni Wpis z Dziennika Bojowego (Komiksowy pasek narracji)
        val latestLog = combatState.logs.lastOrNull()?.text ?: "Wymiana spojrzeń przed uderzeniem..."
        Surface(
            color = Color.Black.copy(alpha = 0.75f),
            shape = RoundedCornerShape(6.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF4A4239)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "📜 $latestLog",
                color = if (latestLog.contains("krytyczne") || latestLog.contains("BOSKĄ") || latestLog.contains("💥")) {
                    Color(0xFFE8C37E)
                } else {
                    Color(0xFFEDE4CF)
                },
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Interaktywne Przyciski Bojowe
        if (combatState.isVictory == null) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Button(
                    onClick = onManualAttack,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F766E)),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("battle_attack_btn"),
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("⚔️ Cios Miecza", fontSize = 11.sp, color = Color(0xFFF3DDA6), fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = onUsePill,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF9A6B2C)),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("battle_pill_btn"),
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("💊 Ulecz Qi (+85)", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                }

                val hasEnoughQi = combatState.discipleQi >= 30
                Button(
                    onClick = onUltimate,
                    enabled = hasEnoughQi,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF991B1B),
                        disabledContainerColor = Color(0xFF374151)
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("battle_ult_btn"),
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = if (hasEnoughQi) "💥 Boskie Dao" else "⏳ Brak Qi",
                        fontSize = 11.sp,
                        color = if (hasEnoughQi) Color(0xFFE8C37E) else Color.Gray,
                        fontWeight = FontWeight.Black
                    )
                }
            }
        } else {
            Button(
                onClick = onClose,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669)),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("battle_finish_btn")
            ) {
                Text(
                    text = "🏆 Odbierz Łupy i Zakończ Walkę",
                    color = Color.White,
                    fontWeight = FontWeight.Black,
                    fontSize = 13.sp
                )
            }
        }
    }
}
