package com.example.ui.game

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.game.WuxiaGameViewModel
import com.example.game.WuxiaUiState
import com.example.model.DiplomaticStatus
import com.example.model.FacilityType
import com.example.ui.aaa.Aaa
import com.example.ui.aaa.AaaGlyph
import com.example.ui.aaa.AaaType
import com.example.ui.aaa.rememberAaaClock
import com.example.ui.audio.WuxiaAudioEngine
import kotlin.math.PI
import kotlin.math.floor
import kotlin.math.sin

private const val SCENE_W = 768f
private const val SCENE_H = 1376f

/**
 * Scena sekty: malowana góra na cały ekran, budynki są nawigacją, interfejs
 * trzyma się krawędzi (gracz ↖, zasoby ↗, moduły ←, kronika ↙, akcja ↘).
 */
@Composable
fun SectHub(
    ui: WuxiaUiState,
    viewModel: WuxiaGameViewModel,
    onOpen: OpenModule,
    modifier: Modifier = Modifier
) {
    val ready = ui.disciples.count { it.realmProgress >= 100 }
    val celebration = rememberCelebration()
    LaunchedEffect(ui.celebration?.id) {
        ui.celebration?.let { celebration.fire(it.text) }
    }
    val chronicle = rememberChronicle(ui)

    Box(modifier.background(Aaa.Ink0)) {
        // 1. Świat
        SceneLayer(ui = ui, ready = ready, onOpen = onOpen)

        // 2. Cienie krawędzi – czytelność HUD bez ramek
        Box(
            Modifier
                .matchParentSize()
                .drawBehind {
                    drawRect(Brush.verticalGradient(listOf(Aaa.Ink0.copy(alpha = 0.8f), Color.Transparent), endY = size.height * 0.2f))
                    drawRect(Brush.verticalGradient(listOf(Color.Transparent, Aaa.Ink0.copy(alpha = 0.92f)), startY = size.height * 0.72f))
                    drawRect(
                        Brush.radialGradient(
                            0.55f to Color.Transparent,
                            1f to Color.Black.copy(alpha = 0.55f),
                            center = Offset(size.width / 2f, size.height * 0.45f),
                            radius = size.maxDimension * 0.75f
                        )
                    )
                }
        )

        // 3. Górny pasek: gracz + zasoby
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .safeTop()
                .padding(start = 12.dp, end = 12.dp, top = 10.dp),
            verticalAlignment = Alignment.Top
        ) {
            PlayerBlock(ui, Modifier.weight(1f))
            Spacer(Modifier.width(8.dp))
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                CurrencyPill(AaaGlyph.SPIRIT_STONE, ui.spiritStones)
                CurrencyPill(AaaGlyph.REPUTATION, ui.sectReputation)
                SpeedAndSound(ui.gameSpeedMultiplier, onSpeed = { viewModel.setGameSpeed(it) })
                EdictTracker(ui, onOpen)
                HubAlertRail(ui, onOpen)
            }
        }

        // 4. Omen + kolumna modułów po lewej
        Column(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 10.dp)
                .offset(y = (-6).dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OmenButton(active = ui.activeRandomEvent != null, onOpen = onOpen)
            Spacer(Modifier.height(6.dp))
            val hostile = ui.rivalSects.count { it.status == DiplomaticStatus.TOTAL_WAR || it.status == DiplomaticStatus.COLD_WAR }
            val worldBadge = when {
                ui.expedition != null -> "⛰"
                ui.secretRealms.any { it.open && !it.visited } -> "!"
                else -> null
            }
            SideModuleButton(GameModule.WORLD, GameModule.WORLD.title, SideIcon.WORLD, badge = worldBadge, onOpen = onOpen)
            SideModuleButton(GameModule.CLANS, GameModule.CLANS.title, SideIcon.CLAN, badge = if (ui.raid != null) "⚠" else if (hostile > 0) "$hostile" else null, onOpen = onOpen)
            SideModuleButton(GameModule.DAO_NET, GameModule.DAO_NET.title, SideIcon.NET, badge = null, onOpen = onOpen)
            SideModuleButton(GameModule.VAULT, GameModule.VAULT.title, SideIcon.CHEST, badge = null, onOpen = onOpen)
        }

        // 5. Kronika + główna akcja
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .safeBottom()
                .padding(start = 12.dp, end = 10.dp, bottom = 14.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            Chronicle(chronicle, Modifier.weight(1f).padding(bottom = 8.dp))
            Spacer(Modifier.width(10.dp))
            val ctaRef = remember { CenterRef() }
            DiamondCta(
                top = if (ready > 0) "⚡$ready" else "${ui.disciples.size}",
                label = if (ready > 0) "PRZEŁOM" else "ADEPCI",
                modifier = Modifier.trackCenter(ctaRef),
                onClick = {
                    if (ready > 0) {
                        WuxiaAudioEngine.playThunderStrike()
                        viewModel.breakthroughAllReadyDisciples()
                        celebration.fire("Przełom $ready ${if (ready == 1) "adepta" else "adeptów"}!")
                    } else onOpen(GameModule.ADEPTS, ctaRef.value)
                }
            )
        }

        CelebrationOverlay(celebration)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Scena z budynkami
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun SceneLayer(ui: WuxiaUiState, ready: Int, onOpen: OpenModule) {
    val drift = rememberInfiniteTransition(label = "scene_drift")
    val d by drift.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(26000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "scene_drift_t"
    )
    val clock = rememberAaaClock()

    BoxWithConstraints(Modifier.fillMaxSize()) {
        // Grafika wypełnia ekran (cover), zachowując proporcje – budynki są w % grafiki.
        val byHeightW = maxHeight * (SCENE_W / SCENE_H)
        val sceneW: Dp
        val sceneH: Dp
        if (byHeightW >= maxWidth) {
            sceneW = byHeightW; sceneH = maxHeight
        } else {
            sceneW = maxWidth; sceneH = maxWidth * (SCENE_H / SCENE_W)
        }

        Box(
            Modifier
                .align(Alignment.Center)
                .requiredSize(sceneW, sceneH)
                .graphicsLayer {
                    val s = 1.04f + 0.06f * d
                    scaleX = s
                    scaleY = s
                    translationX = (-0.015f + 0.035f * d) * size.width
                    translationY = (-0.012f * d) * size.height
                }
        ) {
            ArtImage("bg_sect_sanctuary", Modifier.matchParentSize())

            // Płatki i iskry Qi (tylko faza rysowania)
            Canvas(Modifier.matchParentSize()) {
                val t = clock.value
                val w = size.width
                val h = size.height
                for (i in 0 until 26) {
                    val sp = 0.03f + h01(i, 1.1f) * 0.04f
                    val life = (t * sp + h01(i, 2.2f)) % 1f
                    val x = (h01(i, 3.3f) * 1.2f - 0.1f) * w + sin(t * 0.9f + i) * 18.dp.toPx() + life * w * 0.25f
                    val y = life * h * 1.1f - h * 0.05f
                    translate(x, y) {
                        rotate(degrees = (t * (0.8f + h01(i, 4.4f)) + i) * 57.3f, pivot = Offset.Zero) {
                            scale(scaleX = 1f, scaleY = 0.55f + 0.45f * sin(t * 2f + i), pivot = Offset.Zero) {
                                drawOval(
                                    Color(0xFFFFAAC8).copy(alpha = 0.75f * sin(life * PI.toFloat())),
                                    topLeft = Offset(-3.2.dp.toPx(), -1.8.dp.toPx()),
                                    size = Size(6.4.dp.toPx(), 3.6.dp.toPx())
                                )
                            }
                        }
                    }
                }
                for (i in 0 until 30) {
                    val sp = 0.02f + h01(i, 5.5f) * 0.03f
                    val life = (t * sp + h01(i, 6.6f)) % 1f
                    val x = h01(i, 7.7f) * w + sin(t * 0.5f + i) * 10.dp.toPx()
                    val y = h * (1f - life)
                    val a = sin(life * PI.toFloat())
                    drawCircle(Color(0xFF8CE6FF).copy(alpha = 0.18f * a), radius = 5.dp.toPx(), center = Offset(x, y))
                    drawCircle(Color(0xFFC8F5FF).copy(alpha = 0.8f * a), radius = 1.3.dp.toPx(), center = Offset(x, y))
                }
            }

            val alchemyLevel = ui.facilities.firstOrNull { it.type == FacilityType.ALCHEMY_CHAMBER }?.level ?: 1
            val forge = ui.facilities.firstOrNull { it.type == FacilityType.DIVINE_FORGE }
            val n = ui.disciples.size

            Building(sceneW, sceneH, 0.84f, 0.37f, labelLeft = true,
                title = "Pawilon Adeptów",
                sub = "$n uczniów · $ready ${if (ready == 1) "gotowy" else "gotowych"}",
                badge = if (ready > 0) "$ready" else null,
                tag = "hub_building_adepts"
            ) { onOpen(GameModule.ADEPTS, it) }

            Building(sceneW, sceneH, 0.58f, 0.44f, labelLeft = false,
                title = "Pałac Sekty",
                sub = "${ui.sectLife.karmaPath.ifEmpty { "Nabór" }} · ${ui.availableRecruits.size} kandydatów",
                badge = hubAlerts(ui).count { it.shortcut.module == GameModule.PALACE }.takeIf { it > 0 }?.toString(),
                tag = "hub_building_palace"
            ) { onOpen(GameModule.PALACE, it) }

            Building(sceneW, sceneH, 0.62f, 0.57f, labelLeft = true,
                title = "Sala Alchemii",
                sub = ui.activeFlame?.name?.let { shortName(it, 3) } ?: "Kocioł · poz. $alchemyLevel",
                badge = ui.economy.fields.count { it.herb != null && it.readyDay <= ui.currentDay }.takeIf { it > 0 }?.let { "🌿" },
                tag = "hub_building_alchemy"
            ) { onOpen(GameModule.ALCHEMY, it) }

            Building(sceneW, sceneH, 0.24f, 0.26f, labelLeft = false,
                title = "Gniazdo Bestii",
                sub = ui.activeBeast?.name?.let { shortName(it, 2) } ?: "${ui.sectBeasts.size} strażników",
                badge = ui.sectLife.eggs.size.takeIf { it > 0 }?.let { "🥚$it" },
                tag = "hub_building_beasts"
            ) { onOpen(GameModule.BEASTS, it) }

            Building(sceneW, sceneH, 0.52f, 0.79f, labelLeft = false,
                title = "Boska Kuźnia",
                sub = if (forge != null) "Poz. ${forge.level} · rozbudowa ${forge.upgradeCostStones}" else "Artefakty",
                badge = null,
                tag = "hub_building_forge"
            ) { onOpen(GameModule.FORGE, it) }
        }
    }
}

/** Budynek: świecący romb w punkcie grafiki + etykieta odsunięta w bok. */
@Composable
private fun Building(
    sceneW: Dp,
    sceneH: Dp,
    fx: Float,
    fy: Float,
    labelLeft: Boolean,
    title: String,
    sub: String,
    badge: String?,
    tag: String,
    onClick: (Offset) -> Unit
) {
    val ref = remember { CenterRef() }
    val pulse = rememberInfiniteTransition(label = "pin_$tag")
    val glow by pulse.animateFloat(0.35f, 1f, infiniteRepeatable(tween(1300, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "pin_glow_$tag")

    Box(
        Modifier
            .offset(x = sceneW * fx, y = sceneH * fy)
            .size(0.dp)
    ) {
        Row(
            modifier = Modifier
                .wrapContentSize(align = if (labelLeft) Alignment.CenterEnd else Alignment.CenterStart, unbounded = true)
                .offset(x = if (labelLeft) 8.dp else (-8).dp)
                .trackCenter(ref)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    role = Role.Button
                ) { onClick(ref.value) }
                .testTag(tag),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val pin: @Composable () -> Unit = {
                Canvas(Modifier.size(16.dp)) {
                    val c = center
                    val r = size.minDimension / 2f
                    drawCircle(Aaa.Gold.copy(alpha = 0.35f * glow), radius = r * 1.6f, center = c)
                    rotate(45f, c) {
                        drawRect(Aaa.Gold.copy(alpha = 0.45f), topLeft = Offset(c.x - r * 0.62f, c.y - r * 0.62f), size = Size(r * 1.24f, r * 1.24f))
                        drawRect(Aaa.GoldHi, topLeft = Offset(c.x - r * 0.62f, c.y - r * 0.62f), size = Size(r * 1.24f, r * 1.24f), style = Stroke(1.5.dp.toPx()))
                    }
                }
            }
            val label: @Composable () -> Unit = {
                Row(
                    modifier = Modifier
                        .background(Brush.horizontalGradient(listOf(Aaa.Ink0.copy(alpha = 0.85f), Aaa.Ink0.copy(alpha = 0.55f))))
                        .drawBehind { drawRect(Aaa.Gold, size = Size(2.dp.toPx(), size.height)) }
                        .padding(start = 10.dp, end = 9.dp, top = 5.dp, bottom = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(title, style = AaaType.Title.copy(fontSize = 11.5.sp, letterSpacing = 0.3.sp), maxLines = 1)
                        Text(sub, style = AaaType.Label.copy(fontSize = 9.sp, color = Aaa.TextMid, letterSpacing = 0.sp), maxLines = 1)
                    }
                    if (badge != null) {
                        Spacer(Modifier.width(6.dp))
                        RedDot(badge)
                    }
                }
            }
            if (labelLeft) {
                label(); Spacer(Modifier.width(6.dp)); pin()
            } else {
                pin(); Spacer(Modifier.width(6.dp)); label()
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Elementy krawędziowe
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun PlayerBlock(ui: WuxiaUiState, modifier: Modifier = Modifier) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) {
            Canvas(Modifier.matchParentSize()) {
                val r = size.minDimension / 2f - 2.dp.toPx()
                drawCircle(
                    Brush.radialGradient(listOf(Aaa.Cinnabar, Color(0xFF5A120E)), center = Offset(center.x - r * 0.3f, center.y - r * 0.35f), radius = r * 1.4f),
                    radius = r
                )
                drawCircle(Color(0xFF1A0D07), radius = r + 1.dp.toPx(), style = Stroke(2.dp.toPx()))
                drawCircle(Aaa.goldBevel(), radius = r + 2.6.dp.toPx(), style = Stroke(1.6.dp.toPx()))
            }
            Text("宗", fontFamily = BrushFont, fontSize = 30.sp, color = Aaa.GoldHi, modifier = Modifier.offset(y = 1.dp))
            Text(
                text = "${ui.sectLeader.prestigeLevel}",
                style = AaaType.Numeric.copy(fontSize = 9.sp, color = Aaa.Ink0, fontWeight = FontWeight.Black),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .offset(y = 5.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Aaa.Gold)
                    .padding(horizontal = 5.dp, vertical = 1.dp)
            )
        }
        Spacer(Modifier.width(9.dp))
        Column(Modifier.weight(1f, fill = false)) {
            Text(
                text = ui.sectName,
                style = AaaType.Title.copy(fontSize = 13.5.sp, letterSpacing = 0.2.sp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(5.dp))
            ThinBar((ui.sectReputation % 100) / 100f, Aaa.GoldHi, Modifier.width(100.dp), height = 3.dp)
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Dzień ${ui.currentDay} · ${ui.timeOfDay.polishName}",
                style = AaaType.Label.copy(fontSize = 9.sp, color = Aaa.TextMid, letterSpacing = 0.sp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun SpeedAndSound(speed: Int, onSpeed: (Int) -> Unit) {
    var muted by remember { mutableStateOf(WuxiaAudioEngine.isMuted) }
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        Box(
            Modifier
                .size(26.dp)
                .clip(CircleShape)
                .background(Aaa.Ink0.copy(alpha = 0.68f))
                .clickable(role = Role.Button) {
                    WuxiaAudioEngine.isMuted = !WuxiaAudioEngine.isMuted
                    muted = WuxiaAudioEngine.isMuted
                    if (!muted) WuxiaAudioEngine.playDivineGong()
                }
                .testTag("audio_toggle_btn"),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                if (muted) Icons.Filled.VolumeOff else Icons.Filled.VolumeUp,
                contentDescription = if (muted) "Włącz dźwięk" else "Wycisz",
                tint = if (muted) Aaa.TextLow else Aaa.Gold,
                modifier = Modifier.size(14.dp)
            )
        }
        Row(
            Modifier
                .height(26.dp)
                .clip(RoundedCornerShape(13.dp))
                .background(Aaa.Ink0.copy(alpha = 0.68f))
                .padding(2.dp)
        ) {
            listOf(1, 2, 5).forEach { s ->
                val on = s == speed
                Box(
                    Modifier
                        .fillMaxHeight()
                        .width(28.dp)
                        .clip(RoundedCornerShape(11.dp))
                        .background(if (on) Aaa.Gold else Color.Transparent)
                        .clickable(role = Role.Button) { onSpeed(s) }
                        .testTag("speed_button_$s"),
                    contentAlignment = Alignment.Center
                ) {
                    Text("${s}×", style = AaaType.Numeric.copy(fontSize = 11.sp, color = if (on) Aaa.Ink0 else Aaa.TextMid, fontWeight = FontWeight.Black))
                }
            }
        }
    }
}

/** Edykt dnia: najbliższa sensowna rozbudowa z postępem i skrótem do modułu. */
@Composable
private fun EdictTracker(ui: WuxiaUiState, onOpen: OpenModule) {
    val target = ui.facilities.minByOrNull { it.upgradeCostStones } ?: return
    val ref = remember { CenterRef() }
    val affordable = ui.spiritStones >= target.upgradeCostStones
    Column(
        Modifier
            .width(158.dp)
            .background(Brush.horizontalGradient(listOf(Aaa.Ink0.copy(alpha = 0.72f), Color.Transparent)))
            .drawBehind { drawRect(Aaa.Gold, size = Size(2.dp.toPx(), size.height)) }
            .trackCenter(ref)
            .clickable(role = Role.Button) { onOpen(GameModule.PALACE, ref.value) }
            .padding(start = 10.dp, top = 7.dp, bottom = 7.dp, end = 6.dp)
    ) {
        Overline("Edykt dnia")
        Text(
            text = "Rozbuduj: ${target.type.polishName}",
            style = AaaType.Title.copy(fontSize = 11.sp, lineHeight = 14.sp, letterSpacing = 0.sp, color = Aaa.TextHi),
            maxLines = 2
        )
        Text(
            text = if (affordable) "Gotowe · ${target.upgradeCostStones} kamieni" else "${ui.spiritStones} / ${target.upgradeCostStones} kamieni",
            style = AaaType.Numeric.copy(fontSize = 9.5.sp, color = if (affordable) Aaa.Jade else Aaa.TextMid)
        )
    }
}

@Composable
private fun OmenButton(active: Boolean, onOpen: OpenModule) {
    val ref = remember { CenterRef() }
    val t = rememberInfiniteTransition(label = "omen")
    val p by t.animateFloat(0.3f, 1f, infiniteRepeatable(tween(900, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "omen_p")
    Column(
        modifier = Modifier
            .trackCenter(ref)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, role = Role.Button) {
                onOpen(GameModule.OMEN, ref.value)
            }
            .testTag("hub_omen"),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(Modifier.size(46.dp), contentAlignment = Alignment.Center) {
            Canvas(Modifier.matchParentSize()) {
                val r = size.minDimension / 2f - 2.dp.toPx()
                if (active) drawCircle(Aaa.Cinnabar.copy(alpha = 0.45f * p), radius = r + 6.dp.toPx() * p)
                drawCircle(
                    Brush.radialGradient(
                        if (active) listOf(Color(0xFF7A1F17), Color(0xFF250807)) else listOf(Color(0xFF2C241E), Color(0xFF0D0A0C)),
                        center = Offset(center.x - r * 0.2f, center.y - r * 0.3f),
                        radius = r * 1.3f
                    ),
                    radius = r
                )
                drawCircle(if (active) Aaa.Cinnabar else Aaa.GoldDeep, radius = r, style = Stroke(1.5.dp.toPx()))
            }
            Text("命", fontFamily = BrushFont, fontSize = 24.sp, color = if (active) Aaa.GoldHi else Aaa.TextLow)
        }
        Text("Omen", style = HubLabel)
    }
}

enum class SideIcon { WORLD, CLAN, NET, CHEST }

@Composable
private fun SideModuleButton(module: GameModule, label: String, icon: SideIcon, badge: String?, onOpen: OpenModule) {
    val ref = remember { CenterRef() }
    Column(
        modifier = Modifier
            .trackCenter(ref)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, role = Role.Button) {
                onOpen(module, ref.value)
            }
            .testTag("hub_side_${module.name.lowercase()}"),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(Modifier.size(46.dp)) {
            Canvas(Modifier.matchParentSize()) {
                val r = size.minDimension / 2f - 2.dp.toPx()
                drawCircle(Brush.radialGradient(listOf(Color(0xFF2C241E), Color(0xFF0D0A0C)), center = Offset(center.x, center.y - r * 0.4f), radius = r * 1.4f), radius = r)
                drawCircle(Aaa.GoldDeep, radius = r, style = Stroke(1.5.dp.toPx()))
                drawSideIcon(icon, Aaa.Gold, r * 0.95f)
            }
            if (badge != null) {
                Box(Modifier.align(Alignment.TopEnd).offset(x = 2.dp, y = (-2).dp)) { RedDot(badge) }
            }
        }
        Text(label, style = HubLabel)
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawSideIcon(icon: SideIcon, c: Color, r: Float) {
    val s = Stroke(width = 1.6.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round, join = androidx.compose.ui.graphics.StrokeJoin.Round)
    val o = center
    fun p(x: Float, y: Float) = Offset(o.x + (x - 12f) / 12f * r * 0.55f, o.y + (y - 12f) / 12f * r * 0.55f)
    when (icon) {
        SideIcon.WORLD -> {
            val path = Path().apply {
                val a = p(2f, 20f); moveTo(a.x, a.y)
                p(9f, 8f).let { lineTo(it.x, it.y) }
                p(13f, 14f).let { lineTo(it.x, it.y) }
                p(16f, 10f).let { lineTo(it.x, it.y) }
                p(22f, 20f).let { lineTo(it.x, it.y) }
                close()
            }
            drawPath(path, c, style = s)
            drawCircle(c, radius = r * 0.09f, center = p(17f, 5f), style = s)
        }
        SideIcon.CLAN -> {
            drawLine(c, p(6f, 22f), p(6f, 3f), s.width, cap = androidx.compose.ui.graphics.StrokeCap.Round)
            val path = Path().apply {
                p(6f, 4f).let { moveTo(it.x, it.y) }
                p(18f, 4f).let { lineTo(it.x, it.y) }
                p(15f, 8f).let { lineTo(it.x, it.y) }
                p(18f, 12f).let { lineTo(it.x, it.y) }
                p(6f, 12f).let { lineTo(it.x, it.y) }
            }
            drawPath(path, c, style = s)
        }
        SideIcon.NET -> {
            drawCircle(c, radius = r * 0.14f, center = p(12f, 12f), style = s)
            listOf(p(4f, 5f), p(20f, 5f), p(12f, 21f)).forEach { q ->
                drawCircle(c, radius = r * 0.09f, center = q, style = s)
                drawLine(c, q, p(12f, 12f), s.width)
            }
        }
        SideIcon.CHEST -> {
            drawRect(c, topLeft = p(3f, 10f), size = Size(p(21f, 20f).x - p(3f, 10f).x, p(21f, 20f).y - p(3f, 10f).y), style = s)
            drawArc(c, 180f, 180f, false, topLeft = p(3f, 5f), size = Size(p(21f, 15f).x - p(3f, 5f).x, p(21f, 15f).y - p(3f, 5f).y), style = s)
            drawRect(c, topLeft = p(10f, 13f), size = Size(p(14f, 16f).x - p(10f, 13f).x, p(14f, 16f).y - p(10f, 13f).y), style = s)
        }
    }
}

private val HubLabel = AaaType.Label.copy(
    fontFamily = FontFamily.Serif,
    fontSize = 9.5.sp,
    color = Aaa.TextHi,
    letterSpacing = 0.sp,
    shadow = androidx.compose.ui.graphics.Shadow(Color.Black, Offset(0f, 1f), 6f)
)

@Composable
fun RedDot(text: String) {
    Box(
        Modifier
            .heightIn(min = 16.dp)
            .widthIn(min = 16.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Aaa.Cinnabar)
            .padding(horizontal = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, style = AaaType.Numeric.copy(fontSize = 9.sp, color = Color.White, fontWeight = FontWeight.Bold))
    }
}

/** Główny przycisk ↘: złoty romb z refleksem. */
@Composable
private fun DiamondCta(top: String, label: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val t = rememberInfiniteTransition(label = "diamond")
    val glow by t.animateFloat(0.5f, 1f, infiniteRepeatable(tween(1400, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "diamond_glow")
    val interaction = remember { MutableInteractionSource() }
    Box(
        modifier = modifier
            .size(88.dp)
            .clickable(interactionSource = interaction, indication = null, role = Role.Button, onClick = onClick)
            .testTag("hub_main_cta"),
        contentAlignment = Alignment.Center
    ) {
        Canvas(Modifier.matchParentSize()) {
            val c = center
            val r = size.minDimension * 0.36f
            drawCircle(Aaa.Gold.copy(alpha = 0.28f * glow), radius = r * 1.5f, center = c)
            rotate(45f, c) {
                val tl = Offset(c.x - r, c.y - r)
                val sz = Size(r * 2f, r * 2f)
                drawRect(Brush.linearGradient(listOf(Aaa.GoldHi, Aaa.Gold, Aaa.GoldDeep), start = tl, end = Offset(c.x + r, c.y + r)), topLeft = tl, size = sz)
                drawRect(Color(0xFF1A0D07), topLeft = tl, size = sz, style = Stroke(2.dp.toPx()))
                drawRect(Aaa.GoldMid, topLeft = Offset(tl.x - 3.dp.toPx(), tl.y - 3.dp.toPx()), size = Size(sz.width + 6.dp.toPx(), sz.height + 6.dp.toPx()), style = Stroke(1.4.dp.toPx()))
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(top, style = AaaType.Numeric.copy(fontSize = 19.sp, color = Color(0xFF2A1606), fontWeight = FontWeight.Black))
            Text(label, style = AaaType.Button.copy(fontSize = 10.sp, color = Color(0xFF2A1606), letterSpacing = 1.2.sp))
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Kronika – dwa ostatnie wydarzenia sekty
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun rememberChronicle(ui: WuxiaUiState): SnapshotStateList<String> {
    val list = remember {
        mutableStateListOf(
            "Sekta budzi się na Świętej Górze",
            ui.disciples.firstOrNull()?.let { "${it.name.substringBefore(" (")} medytuje w Pawilonie" } ?: "Pawilon czeka na adeptów"
        )
    }
    LaunchedEffect(ui.notificationMessage) {
        val msg = ui.notificationMessage
        if (msg != null && list.lastOrNull() != msg) {
            list.add(msg)
            while (list.size > 2) list.removeAt(0)
        }
    }
    return list
}

@Composable
private fun Chronicle(lines: List<String>, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        lines.forEachIndexed { i, line ->
            Text(
                text = line,
                style = AaaType.Body.copy(
                    fontSize = 10.sp,
                    lineHeight = 14.sp,
                    color = if (i == lines.lastIndex) Aaa.TextMid else Aaa.TextLow
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────

internal fun h01(i: Int, salt: Float): Float {
    val v = sin(i * 12.9898f + salt * 78.233f) * 43758.547f
    return v - floor(v)
}

/** Skraca nazwę do pierwszych [words] słów. */
internal fun shortName(name: String, words: Int): String =
    name.substringBefore(" (").split(" ").take(words).joinToString(" ")
