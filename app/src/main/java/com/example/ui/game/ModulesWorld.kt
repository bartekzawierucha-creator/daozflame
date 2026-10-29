package com.example.ui.game

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.game.WuxiaGameViewModel
import com.example.game.WuxiaUiState
import com.example.model.Artifact
import com.example.model.Disciple
import com.example.model.DiplomaticStatus
import com.example.model.RivalSect
import com.example.model.TerritoryNode
import com.example.model.WorldRegion
import com.example.ui.aaa.Aaa
import com.example.ui.aaa.AaaGlyph
import com.example.ui.aaa.AaaType
import com.example.ui.aaa.formatGameNumber
import com.example.ui.audio.WuxiaAudioEngine
import com.example.ui.screens.DiplomacyScreen
import com.example.ui.screens.MultiplayerScreen
import com.example.ui.screens.WorldMapScreen
import com.example.game.content.ResourceCatalog
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Najsilniejszy adept – wysyłany na wyprawy, do areny i rajdu. */
internal fun champion(ui: WuxiaUiState): Disciple? = ui.disciples.maxByOrNull { it.combatPower }

// ─────────────────────────────────────────────────────────────────────────────
//  ŚWIAT – mapa tuszem
// ─────────────────────────────────────────────────────────────────────────────

private val REGION_SPOTS = listOf(
    Offset(0.28f, 0.52f), Offset(0.70f, 0.27f), Offset(0.22f, 0.35f),
    Offset(0.60f, 0.45f), Offset(0.80f, 0.56f), Offset(0.42f, 0.19f),
    Offset(0.86f, 0.40f), Offset(0.12f, 0.24f), Offset(0.36f, 0.33f),
    Offset(0.70f, 0.12f), Offset(0.12f, 0.55f), Offset(0.50f, 0.56f)
)

private fun spotFor(i: Int): Offset =
    REGION_SPOTS.getOrNull(i) ?: Offset(0.15f + h01(i, 3.1f) * 0.7f, 0.18f + h01(i, 4.7f) * 0.4f)

private fun dangerColor(d: Int): Color = when {
    d <= 2 -> Aaa.Jade
    d <= 5 -> Aaa.Gold
    else -> Aaa.Cinnabar
}

@Composable
fun WorldModule(ui: WuxiaUiState, viewModel: WuxiaGameViewModel, onBack: () -> Unit) {
    var tab by remember { mutableIntStateOf(0) }
    var regionSel by remember { mutableIntStateOf(0) }
    var terrSel by remember { mutableIntStateOf(0) }
    var legacy by remember { mutableStateOf(false) }
    var expoOpen by remember { mutableStateOf(ui.expedition != null) }
    var towerOpen by remember { mutableStateOf(false) }
    var realmsOpen by remember { mutableStateOf(false) }
    ConsumeShortcut(GameModule.WORLD) { sc ->
        when (sc) {
            Shortcut.EXPEDITION -> expoOpen = true
            Shortcut.SECRET_REALMS -> realmsOpen = true
            Shortcut.TOWER -> towerOpen = true
            else -> Unit
        }
    }
    val celebration = rememberCelebration()
    val champ = champion(ui)

    Box(Modifier.fillMaxSize()) {
        ModuleScaffold(
            module = GameModule.WORLD,
            subtitle = "Kontynent Dziewięciu Smoków · ${ui.regions.size} krain",
            onBack = onBack,
            glyphAlpha = 0.05f,
            bottomShade = 0.42f,
            background = {
                InkMap(ui.regions, Modifier.matchParentSize())
            },
            topRight = { CurrencyPill(AaaGlyph.SPIRIT_STONE, ui.spiritStones) }
        ) {
            TextTabs(
                listOf("Regiony", "Terytoria"),
                tab,
                { tab = it },
                Modifier
                    .safeTop()
                    .padding(start = 60.dp, top = 58.dp)
            )

            BoxWithConstraints(Modifier.matchParentSize()) {
                val w = maxWidth
                val h = maxHeight
                if (tab == 0) {
                    ui.regions.forEachIndexed { i, r ->
                        val p = spotFor(i)
                        MapNode(
                            x = w * p.x, y = h * p.y,
                            label = if (ui.discoveredRegionIds.isNotEmpty() && r.id !in ui.discoveredRegionIds) "???" else shortName(r.name, 2),
                            color = dangerColor(r.dangerLevel),
                            fog = ui.discoveredRegionIds.isNotEmpty() && r.id !in ui.discoveredRegionIds,
                            selected = i == regionSel,
                            flag = null,
                            onClick = { regionSel = i },
                            tag = "world_region_${r.id}"
                        )
                    }
                } else {
                    ui.territories.forEachIndexed { i, t ->
                        val p = spotFor(i)
                        val mine = t.controllingSectId == "player"
                        MapNode(
                            x = w * p.x, y = h * p.y,
                            label = shortName(t.name, 2),
                            color = if (mine) Aaa.Jade else if (t.isUnderAttack) Aaa.Cinnabar else Aaa.GoldMid,
                            fog = false,
                            selected = i == terrSel,
                            flag = if (mine) "Twoja sekta" else shortName(t.controllingSectName, 2),
                            onClick = { terrSel = i },
                            tag = "world_territory_${t.id}"
                        )
                    }
                }
            }

            ModuleSheet {
                if (tab == 0) {
                    val r = ui.regions.getOrNull(regionSel)
                    val hidden = r != null && ui.discoveredRegionIds.isNotEmpty() && r.id !in ui.discoveredRegionIds
                    if (r != null && hidden) FogSheet(r, ui, champ, viewModel, onRealms = { realmsOpen = true })
                    else if (r != null) RegionSheet(r, champ, onExpedition = {
                        WuxiaAudioEngine.playSwordClash()
                        expoOpen = true
                    }, onBook = { legacy = true }, onTower = { towerOpen = true }, onRealms = { realmsOpen = true })
                } else {
                    val t = ui.territories.getOrNull(terrSel)
                    if (t != null) TerritorySheet(t, ui, champ, viewModel, onBook = { legacy = true })
                }
            }
            CelebrationOverlay(celebration)
        }
        if (legacy) {
            LegacyPanel("Mapa Świata", onClose = { legacy = false }) {
                WorldMapScreen(viewModel = viewModel, uiState = ui, showCombatOverlay = false)
            }
        }
        if (expoOpen) ExpeditionPanel(ui.regions.getOrNull(regionSel), ui, viewModel, onClose = { expoOpen = false })
        if (towerOpen) TowerPanel(ui, viewModel, onClose = { towerOpen = false })
        if (realmsOpen) SecretRealmsPanel(ui, viewModel, onClose = { realmsOpen = false })
    }
}

@Composable
private fun ColumnScope.RegionSheet(r: WorldRegion, champ: Disciple?, onExpedition: () -> Unit, onBook: () -> Unit, onTower: () -> Unit, onRealms: () -> Unit) {
    val unknown = r.explorationProgress == 0
    Row(verticalAlignment = Alignment.CenterVertically) {
        DangerPips(r.dangerLevel)
        Spacer(Modifier.weight(1f))
        Text("Zagrożenie ${r.dangerLevel} / 10", style = AaaType.Numeric.copy(fontSize = 10.sp, color = dangerColor(r.dangerLevel)))
    }
    SheetTitle(if (unknown) "Nieznana kraina" else r.name, size = 21)
    SheetBody("${r.biome.polishName} · ${r.hazard.polishName}", maxLines = 1, modifier = Modifier.offset(y = (-4).dp))
    KvRow("Zbadano", "${r.explorationProgress}%", valueColor = Aaa.Jade)
    ThinBar(r.explorationProgress / 100f, Aaa.Jade)
    if (unknown) {
        SheetBody("Zasoby nieznane. Wyślij wyprawę, żeby rozwiać mgłę.")
    } else {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ResourceText("Minerał", r.mineralResource)
            ResourceText("Zioło", r.herbResource)
        }
        r.bossBeast?.let { KvRow("Strażnik krainy", it.name, valueColor = Aaa.Cinnabar) }
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        GhostButton("Księga", onClick = onBook, modifier = Modifier.weight(1f))
        GhostButton("Wieża", onClick = onTower, modifier = Modifier.weight(1f))
        GhostButton("Tajne", onClick = onRealms, modifier = Modifier.weight(1f))
        val can = champ != null && r.bossBeast != null && !r.isConquered
        GoldCta(
            text = when {
                r.isConquered -> "Podbita"
                r.bossBeast == null -> "Bezpieczna"
                unknown -> "Zwiad"
                else -> "Wyprawa"
            },
            trailing = champ?.name?.substringBefore(" (")?.substringBefore(" ")?.takeIf { can },
            enabled = can,
            onClick = onExpedition,
            modifier = Modifier.weight(1.7f),
            testTag = "world_expedition"
        )
    }
}

@Composable
private fun ColumnScope.TerritorySheet(t: TerritoryNode, ui: WuxiaUiState, champ: Disciple?, viewModel: WuxiaGameViewModel, onBook: () -> Unit) {
    val mine = t.controllingSectId == "player"
    val c = if (mine) Aaa.Jade else Aaa.Cinnabar
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        EdgeTag(if (mine) "Twoje terytorium" else t.controllingSectName, c)
        if (t.isUnderAttack) Overline("Pod atakiem!", color = Aaa.Cinnabar)
    }
    SheetTitle(t.name, size = 21)
    SheetBody(t.regionType, maxLines = 1, modifier = Modifier.offset(y = (-4).dp))
    StatTriplet(
        listOf(
            "Zasób" to shortName(t.resourceType, 1),
            "Na cykl" to "+${t.yieldAmountPerCycle}",
            "Garnizon" to "${t.garrisonDefensePower}"
        )
    )
    val guard = ui.disciples.firstOrNull { it.id == t.assignedDiscipleId }
    if (mine) KvRow("Dowódca garnizonu", guard?.name?.substringBefore(" (") ?: "brak", valueColor = if (guard != null) Aaa.GoldHi else Aaa.TextLow)
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        GhostButton("Księga", onClick = onBook, modifier = Modifier.weight(1f))
        GoldCta(
            text = if (mine) "Garnizon" else "Zdobądź",
            danger = !mine,
            enabled = champ != null,
            onClick = {
                if (champ == null) return@GoldCta
                WuxiaAudioEngine.playSwordClash()
                if (mine) viewModel.assignGarrison(t.id, champ.id) else viewModel.captureTerritory(t.id, champ.id)
            },
            modifier = Modifier.weight(1.7f),
            testTag = "world_territory_action"
        )
    }
}

@Composable
private fun ResourceText(label: String, value: String) {
    Column {
        Text(label.uppercase(), style = AaaType.Overline.copy(fontSize = 8.sp, color = Aaa.TextLow))
        Text(value, style = AaaType.Label.copy(fontSize = 11.sp, color = Aaa.TextHi), maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun DangerPips(level: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        repeat(10) { i ->
            Canvas(Modifier.size(8.dp)) {
                rotate(45f) {
                    drawRect(
                        if (i < level) Aaa.Cinnabar else Color.White.copy(alpha = 0.12f),
                        topLeft = Offset(size.width * 0.15f, size.height * 0.15f),
                        size = Size(size.width * 0.7f, size.height * 0.7f)
                    )
                }
            }
        }
    }
}

@Composable
private fun MapNode(
    x: androidx.compose.ui.unit.Dp,
    y: androidx.compose.ui.unit.Dp,
    label: String,
    color: Color,
    fog: Boolean,
    selected: Boolean,
    flag: String?,
    onClick: () -> Unit,
    tag: String
) {
    val t = rememberInfiniteTransition(label = "node_$tag")
    val pulse by t.animateFloat(0.6f, 1f, infiniteRepeatable(tween(900), RepeatMode.Reverse), label = "node_p_$tag")
    Box(Modifier.offset(x = x, y = y).size(0.dp)) {
        Column(
            modifier = Modifier
                .wrapContentSize(Alignment.TopCenter, unbounded = true)
                .offset(y = (-11).dp)
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, role = Role.Button, onClick = onClick)
                .testTag(tag),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Canvas(Modifier.size(22.dp)) {
                val c = if (fog) Color.White.copy(alpha = 0.25f) else color
                val s = if (selected) 1.25f * pulse + 0.1f else 1f
                val r = size.minDimension * 0.32f * s
                if (!fog) drawCircle(color.copy(alpha = if (selected) 0.4f else 0.22f), radius = r * 1.8f)
                rotate(45f) {
                    val tl = Offset(center.x - r, center.y - r)
                    val sz = Size(r * 2f, r * 2f)
                    drawRect(if (selected) c else Color.Black.copy(alpha = 0.55f), topLeft = tl, size = sz)
                    drawRect(c, topLeft = tl, size = sz, style = Stroke(2.dp.toPx()))
                }
            }
            Text(
                label,
                style = AaaType.Label.copy(
                    fontFamily = FontFamily.Serif,
                    fontSize = 9.5.sp,
                    color = if (fog) Aaa.TextLow else Aaa.TextHi,
                    shadow = androidx.compose.ui.graphics.Shadow(Color.Black, Offset(0f, 1f), 6f),
                    letterSpacing = 0.sp
                ),
                maxLines = 1
            )
            if (flag != null) {
                Text(
                    flag,
                    style = AaaType.Label.copy(fontSize = 8.sp, color = color, letterSpacing = 0.sp),
                    maxLines = 1,
                    modifier = Modifier
                        .background(Aaa.Ink0.copy(alpha = 0.7f))
                        .drawBehind { drawRect(color, size = Size(2.dp.toPx(), size.height)) }
                        .padding(start = 5.dp, end = 4.dp, top = 1.dp, bottom = 1.dp)
                )
            }
        }
    }
}

/** Mapa malowana tuszem: papier, rzeka, góry kreską, szlaki, mgła na niezbadanych krainach. */
@Composable
private fun InkMap(regions: List<WorldRegion>, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        drawRect(Brush.radialGradient(listOf(Color(0xFF2A2219), Color(0xFF0B090A)), center = Offset(w * 0.5f, h * 0.42f), radius = h * 0.8f))
        for (i in 0 until 700) {
            drawRect(
                Aaa.Gold.copy(alpha = h01(i, 1f) * 0.045f),
                topLeft = Offset(h01(i, 2f) * w, h01(i, 3f) * h),
                size = Size(1.3f, 1.3f)
            )
        }
        val river = Path().apply {
            moveTo(-10f, h * 0.40f)
            cubicTo(w * 0.25f, h * 0.34f, w * 0.35f, h * 0.52f, w * 0.55f, h * 0.42f)
            cubicTo(w * 0.75f, h * 0.32f, w * 0.85f, h * 0.50f, w + 10f, h * 0.46f)
        }
        drawPath(river, Aaa.Azure.copy(alpha = 0.32f), style = Stroke(3.dp.toPx(), cap = StrokeCap.Round))
        drawPath(river, Aaa.Azure.copy(alpha = 0.08f), style = Stroke(12.dp.toPx(), cap = StrokeCap.Round))
        for (i in 0 until 42) {
            val x = h01(i, 4f) * w
            val y = h * 0.14f + h01(i, 5f) * h * 0.5f
            val s = (8f + h01(i, 6f) * 16f) * density / 2.6f * 2f
            val m = Path().apply {
                moveTo(x - s, y)
                lineTo(x - s * 0.2f, y - s * 0.9f)
                lineTo(x + s * 0.1f, y - s * 0.55f)
                lineTo(x + s * 0.45f, y - s * 1.1f)
                lineTo(x + s, y)
            }
            drawPath(m, Aaa.Gold.copy(alpha = 0.14f + h01(i, 7f) * 0.18f), style = Stroke(1.2.dp.toPx()))
        }
        val links = listOf(0 to 2, 0 to 3, 3 to 1, 3 to 4, 2 to 5, 1 to 5)
        val dash = PathEffect.dashPathEffect(floatArrayOf(3.dp.toPx(), 5.dp.toPx()))
        links.forEach { (a, b) ->
            if (a < regions.size && b < regions.size) {
                val pa = spotFor(a)
                val pb = spotFor(b)
                val A = Offset(pa.x * w, pa.y * h)
                val B = Offset(pb.x * w, pb.y * h)
                val path = Path().apply {
                    moveTo(A.x, A.y)
                    quadraticBezierTo((A.x + B.x) / 2f + 20f, (A.y + B.y) / 2f - 18f, B.x, B.y)
                }
                drawPath(path, Aaa.Gold.copy(alpha = 0.45f), style = Stroke(1.2.dp.toPx(), pathEffect = dash))
            }
        }
        regions.forEachIndexed { i, r ->
            if (r.explorationProgress == 0) {
                val p = spotFor(i)
                val c = Offset(p.x * w, p.y * h)
                drawCircle(
                    Brush.radialGradient(listOf(Color(0xEE1E1A1E), Color.Transparent), center = c, radius = 72.dp.toPx()),
                    radius = 72.dp.toPx(),
                    center = c
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  KLANY – dyplomacja
// ─────────────────────────────────────────────────────────────────────────────

internal fun sectGlyph(name: String): String = when {
    name.contains("Miecz", true) || name.contains("Wudang", true) -> "武"
    name.contains("Smok", true) || name.contains("Płon", true) -> "炎"
    name.contains("Zioł", true) -> "药"
    name.contains("Krwaw", true) -> "血"
    name.contains("Kupiec", true) -> "商"
    name.contains("Klasztor", true) || name.contains("Wadżr", true) -> "佛"
    name.contains("Cesar", true) -> "帝"
    name.contains("Ostrz", true) -> "刃"
    name.contains("Gromów", true) -> "雷"
    name.contains("Mórz", true) || name.contains("Smoczego Króla", true) -> "龙"
    name.contains("Księżyc", true) || name.contains("Pijanego", true) -> "仙"
    name.contains("Bestii", true) -> "兽"
    name.contains("Lodow", true) -> "冰"
    name.contains("Kości", true) -> "毒"
    name.contains("Gwiezd", true) || name.contains("Niebiań", true) -> "天"
    else -> "山"
}

internal fun statusColor(s: DiplomaticStatus): Color = when (s) {
    DiplomaticStatus.ALLIANCE -> Aaa.Jade
    DiplomaticStatus.FRIENDLY -> Aaa.Azure
    DiplomaticStatus.NEUTRAL -> Aaa.GoldMid
    DiplomaticStatus.COLD_WAR -> Aaa.Ember
    DiplomaticStatus.TOTAL_WAR -> Aaa.Cinnabar
}

@Composable
fun ClansModule(ui: WuxiaUiState, viewModel: WuxiaGameViewModel, onBack: () -> Unit) {
    var selId by remember { mutableStateOf(ui.rivalSects.firstOrNull()?.id) }
    val s: RivalSect? = ui.rivalSects.firstOrNull { it.id == selId } ?: ui.rivalSects.firstOrNull()
    var legacy by remember { mutableStateOf(false) }
    var sects by remember { mutableStateOf<SectsTab?>(null) }
    ConsumeShortcut(GameModule.CLANS) { sc ->
        when (sc) {
            Shortcut.RAID -> sects = SectsTab.DEFENSE
            Shortcut.SPIES -> sects = SectsTab.SPIES
            else -> Unit
        }
    }
    val celebration = rememberCelebration()
    if (s != null) {
        WatchIncrease(s.id, s.status.ordinal.let { if (s.status == DiplomaticStatus.ALLIANCE) 1 else 0 }) {
            celebration.fire("Sojusz z ${shortName(s.name, 2)}!")
        }
    }

    Box(Modifier.fillMaxSize()) {
        ModuleScaffold(
            module = GameModule.CLANS,
            subtitle = "${ui.rivalSects.size} sekt Kontynentu",
            onBack = onBack,
            accent = s?.let { statusColor(it.status) } ?: Aaa.Gold,
            glyph = s?.let { sectGlyph(it.name) } ?: GameModule.CLANS.glyph,
            glyphAlpha = 0f,
            background = {
                // Przywódca wybranej sekty na cały ekran.
                if (s != null) FullFigure(sectLeaderArt(s), topPad = 40.dp, heightFraction = 0.8f, bias = 0.3f)
                else ArtImage("bg_sect_sanctuary", Modifier.matchParentSize().blur(10.dp), colorFilter = dimFilter(0.32f, 0.5f))
            },
            topRight = { CurrencyPill(AaaGlyph.REPUTATION, ui.sectReputation) }
        ) {
            Column(
                modifier = Modifier
                    .safeTop()
                    .padding(start = 10.dp, top = 70.dp)
                    .fillMaxHeight(0.5f)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(9.dp)
            ) {
                ui.rivalSects.forEach { x ->
                    val on = x.id == s?.id
                    val c = statusColor(x.status)
                    Box(
                        Modifier
                            .offset(x = if (on) 4.dp else 0.dp)
                            .size(50.dp)
                    ) {
                        Box(
                            Modifier
                                .size(48.dp)
                                .graphicsLayer { alpha = if (on) 1f else 0.62f }
                                .clip(CircleShape)
                                .background(Brush.radialGradient(listOf(c.copy(alpha = 0.75f), Color(0xFF0A0708))))
                                .drawBehind { drawCircle(c, radius = size.minDimension / 2f - 1.dp.toPx(), style = Stroke(if (on) 2.dp.toPx() else 1.dp.toPx())) }
                                .clickable(role = Role.Button) { selId = x.id }
                                .testTag("clan_${x.id}"),
                            contentAlignment = Alignment.Center
                        ) {
                            val face = sectLeaderFace(x)
                            if (face != null) ArtImage(face, Modifier.matchParentSize().padding(2.dp).clip(CircleShape), alignment = Alignment.TopCenter)
                            else Text(sectGlyph(x.name), fontFamily = BrushFont, fontSize = 24.sp, color = Aaa.GoldHi)
                        }
                        if (x.status == DiplomaticStatus.TOTAL_WAR || x.status == DiplomaticStatus.COLD_WAR || x.activeTrade != null) {
                            Box(Modifier.align(Alignment.TopEnd)) { RedDot(if (x.activeTrade != null) "$" else "!") }
                        }
                    }
                }
            }

            if (s != null) {
                val c = statusColor(s.status)
                ModuleSheet {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        EdgeTag(s.status.polishName, c)
                        Overline("#${s.powerRank} w rankingu mocy", color = Aaa.TextMid)
                    }
                    SheetTitle(s.name, size = 21)
                    SheetBody("${s.leaderName} · ${s.alignment} · ${s.discipleCount} uczniów", maxLines = 2, modifier = Modifier.offset(y = (-4).dp))
                    KvRow("Relacje", (if (s.relations > 0) "+" else "") + s.relations, valueColor = c)
                    RelationGauge(s.relations)
                    val trade = s.activeTrade
                    if (trade != null) {
                        SheetBody("Karawana: ${trade.offerText}", color = Aaa.Gold, maxLines = 2)
                    } else {
                        SheetBody("Specjalność: ${s.specialty}", maxLines = 1)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        GhostButton("Wojna", danger = true, enabled = s.status != DiplomaticStatus.TOTAL_WAR, onClick = {
                            WuxiaAudioEngine.playSwordClash(); viewModel.interactRivalSect(s.id, "DECLARE_WAR")
                        }, modifier = Modifier.weight(1f))
                        GhostButton("Dar −100", onClick = {
                            WuxiaAudioEngine.playDivineGong(); viewModel.interactRivalSect(s.id, "GIFT")
                        }, modifier = Modifier.weight(1.1f))
                        if (trade != null) {
                            GoldCta("Handel", trailing = "${trade.costStones}", enabled = ui.spiritStones >= trade.costStones, onClick = {
                                viewModel.interactRivalSect(s.id, "TRADE")
                            }, modifier = Modifier.weight(1.4f), testTag = "clan_trade")
                        } else {
                            val isAlly = s.status == DiplomaticStatus.ALLIANCE
                            GoldCta(
                                text = if (isAlly) "Sojusznik" else "Sojusz",
                                enabled = !isAlly && s.relations >= 60,
                                onClick = { viewModel.interactRivalSect(s.id, "ALLIANCE") },
                                modifier = Modifier.weight(1.4f),
                                testTag = "clan_alliance"
                            )
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        GhostButton(if (ui.raid != null) "⚠ Obrona" else "Obrona", danger = ui.raid != null, onClick = { sects = SectsTab.DEFENSE }, modifier = Modifier.weight(1f), height = 36.dp)
                        GhostButton("Wywiad", onClick = { sects = SectsTab.SPIES }, modifier = Modifier.weight(1f), height = 36.dp)
                        GhostButton("Wojna", onClick = { sects = SectsTab.WAR }, modifier = Modifier.weight(1f), height = 36.dp)
                        GhostButton("Kronika", onClick = { sects = SectsTab.CHRONICLE }, modifier = Modifier.weight(1f), height = 36.dp)
                    }
                    if (s.nemesisLevel > 0) SheetBody("☠ Nemezis poziomu ${s.nemesisLevel}", color = Aaa.Cinnabar)
                    Text(
                        "Księga dyplomacji",
                        style = AaaType.Label.copy(fontSize = 9.5.sp, color = Aaa.TextLow),
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .clickable(role = Role.Button) { legacy = true }
                            .padding(4.dp)
                    )
                }
            }
            CelebrationOverlay(celebration, flashColor = Aaa.Jade)
        }
        if (legacy) {
            LegacyPanel("Dyplomacja Klanów", onClose = { legacy = false }) {
                DiplomacyScreen(viewModel = viewModel, uiState = ui)
            }
        }
        sects?.let { SectsPanel(ui, viewModel, s, it, onClose = { sects = null }) }
    }
}

@Composable
private fun RelationGauge(relations: Int) {
    val f by animateFloatAsState(((relations + 100) / 200f).coerceIn(0f, 1f), spring(dampingRatio = 0.55f), label = "rel_gauge")
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Canvas(
            Modifier
                .fillMaxWidth()
                .height(16.dp)
        ) {
            val y = size.height / 2f
            val bh = 8.dp.toPx()
            drawRect(
                Brush.horizontalGradient(listOf(Aaa.Cinnabar, Color(0xFF6B4A2A), Color(0xFF3B3A2A), Color(0xFF2B5A46), Aaa.Jade)),
                topLeft = Offset(0f, y - bh / 2f),
                size = Size(size.width, bh)
            )
            val x = size.width * f
            drawRect(Aaa.GoldHi.copy(alpha = 0.35f), topLeft = Offset(x - 5.dp.toPx(), 0f), size = Size(10.dp.toPx(), size.height))
            drawRect(Aaa.GoldHi, topLeft = Offset(x - 2.dp.toPx(), 0f), size = Size(4.dp.toPx(), size.height))
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("WROGOŚĆ", style = AaaType.Overline.copy(fontSize = 8.sp, color = Aaa.TextLow))
            Text("SOJUSZ", style = AaaType.Overline.copy(fontSize = 8.sp, color = Aaa.TextLow))
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  SIEĆ DAO – rajd, arena, ranking
// ─────────────────────────────────────────────────────────────────────────────

private data class DamagePop(val id: Long, val value: Long, val fx: Float, val fy: Float)

@Composable
fun DaoNetModule(ui: WuxiaUiState, viewModel: WuxiaGameViewModel, onBack: () -> Unit) {
    var tab by remember { mutableIntStateOf(0) }
    var shadowOpen by remember { mutableStateOf(false) }
    ConsumeShortcut(GameModule.DAO_NET) { if (it == Shortcut.ARENA) shadowOpen = true }
    var legacy by remember { mutableStateOf(false) }
    val champ = champion(ui)
    val boss = ui.worldBoss
    val scope = rememberCoroutineScope()
    val shake = remember { Animatable(0f) }
    val pops = remember { mutableStateListOf<DamagePop>() }
    var cooldown by remember { mutableStateOf(false) }

    // Prawdziwe obrażenia z ViewModelu → liczby nad bossem
    val lastContrib = remember { longArrayOf(boss.playerContributionDamage) }
    LaunchedEffect(boss.playerContributionDamage) {
        val dmg = boss.playerContributionDamage - lastContrib[0]
        lastContrib[0] = boss.playerContributionDamage
        if (dmg > 0) {
            val id = System.nanoTime()
            pops.add(DamagePop(id, dmg, 0.3f + h01(id.toInt(), 1f) * 0.4f, 0.32f + h01(id.toInt(), 2f) * 0.12f))
            launch { shake.snapTo(1f); shake.animateTo(0f, spring(dampingRatio = 0.3f, stiffness = Spring.StiffnessMedium)) }
            delay(1000)
            pops.removeAll { it.id == id }
        }
    }

    Box(Modifier.fillMaxSize()) {
        ModuleScaffold(
            module = GameModule.DAO_NET,
            subtitle = "Ping ${ui.serverPingMs} ms · ${boss.participatingSectsCount} sekt w rajdzie",
            onBack = onBack,
            accent = Aaa.Violet,
            glyphAlpha = if (tab == 0) 0f else 0.1f,
            bottomShade = if (tab == 0) 0.5f else 0.12f,
            background = {
                ArtImage(
                    "beast_azure_dragon",
                    Modifier
                        .matchParentSize()
                        .graphicsLayer {
                            translationX = shake.value * 10.dp.toPx()
                            val s = 1f + shake.value * 0.02f
                            scaleX = s
                            scaleY = s
                        },
                    alignment = BiasAlignment(0f, -0.3f),
                    colorFilter = dimFilter(if (tab == 0) 0.72f else 0.25f, 0.7f)
                )
                Box(Modifier.matchParentSize().background(Aaa.Violet.copy(alpha = 0.10f)))
            },
            topRight = { CurrencyPill(AaaGlyph.SPIRIT_STONE, ui.spiritStones) }
        ) {
            TextTabs(
                listOf("Rajd", "Arena", "Ranking", "Cienie"),
                tab,
                { if (it == 3) shadowOpen = true else tab = it },
                Modifier
                    .safeTop()
                    .padding(start = 60.dp, top = 58.dp)
            )

            when (tab) {
                0 -> {
                    BossBar(boss, Modifier.safeTop().padding(start = 12.dp, end = 12.dp, top = 96.dp))
                    BoxWithConstraints(Modifier.matchParentSize()) {
                        pops.forEach { p -> key(p.id) { DamageNumber(p, maxWidth, maxHeight) } }
                    }
                    ModuleSheet {
                        StatTriplet(
                            listOf(
                                "Twój wkład" to formatGameNumber(boss.playerContributionDamage.coerceAtMost(Int.MAX_VALUE.toLong()).toInt()),
                                "Czempion" to (champ?.name?.substringBefore(" (")?.substringBefore(" ") ?: "—"),
                                "Do końca" to "${boss.hoursRemaining} h"
                            )
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            GhostButton("Księga", onClick = { legacy = true }, modifier = Modifier.weight(1f))
                            GoldCta(
                                text = if (cooldown) "Odnowa…" else "Atak sekty",
                                danger = true,
                                enabled = champ != null && !cooldown && boss.currentHp > 0,
                                onClick = {
                                    if (champ == null) return@GoldCta
                                    WuxiaAudioEngine.playUltimateBurst()
                                    viewModel.attackWorldBoss(champ.id)
                                    cooldown = true
                                    scope.launch { delay(2500); cooldown = false }
                                },
                                modifier = Modifier.weight(1.7f),
                                testTag = "raid_attack"
                            )
                        }
                    }
                }
                1 -> {
                    val rivals = ui.multiplayerRankings.filter { it.sectName != ui.sectName }.take(5)
                    Column(
                        Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .safeBottom()
                            .padding(start = 14.dp, end = 14.dp, bottom = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Overline("Niebiańska Arena · czempion: ${champ?.name?.substringBefore(" (") ?: "brak"}")
                        Spacer(Modifier.height(6.dp))
                        rivals.forEach { r ->
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .drawBehind { drawLine(Aaa.Gold.copy(alpha = 0.1f), Offset(0f, size.height), Offset(size.width, size.height), 1.dp.toPx()) }
                                    .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    Modifier.size(42.dp).clip(CircleShape).background(Brush.radialGradient(listOf(Aaa.Violet.copy(alpha = 0.6f), Aaa.Ink0))),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(sectGlyph(r.sectName), fontFamily = BrushFont, fontSize = 22.sp, color = Aaa.GoldHi)
                                }
                                Spacer(Modifier.width(10.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(r.sectName, style = AaaType.Title.copy(fontSize = 12.sp, color = Aaa.TextHi, letterSpacing = 0.sp), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text("#${r.rank} · moc ${formatGameNumber(r.totalPower.coerceAtMost(Int.MAX_VALUE.toLong()).toInt())} · ${r.realmRegion}", style = AaaType.Label.copy(fontSize = 9.5.sp, color = Aaa.TextLow), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                                Spacer(Modifier.width(8.dp))
                                GhostButton("Wyzwij", height = 34.dp, enabled = champ != null, onClick = {
                                    if (champ != null) { WuxiaAudioEngine.playSwordClash(); viewModel.fightArenaDuel(champ.id, r) }
                                })
                            }
                        }
                    }
                }
                else -> {
                    Column(
                        Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .safeBottom()
                            .padding(start = 14.dp, end = 14.dp, bottom = 16.dp)
                    ) {
                        Overline("Ranking sekt Sieci Dao")
                        Spacer(Modifier.height(6.dp))
                        ui.multiplayerRankings.take(8).forEach { r ->
                            val me = r.sectName == ui.sectName
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .background(if (me) Brush.horizontalGradient(listOf(Aaa.Gold.copy(alpha = 0.18f), Color.Transparent)) else Brush.horizontalGradient(listOf(Color.Transparent, Color.Transparent)))
                                    .padding(horizontal = 4.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("${r.rank}", style = AaaType.Title.copy(fontSize = 13.sp, color = if (r.rank <= 3) Aaa.Gold else Aaa.TextMid), modifier = Modifier.width(30.dp), textAlign = TextAlign.Center)
                                Text(r.sectName, style = AaaType.Label.copy(fontSize = 11.5.sp, color = if (me) Aaa.GoldHi else Aaa.TextMid), maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                                Text(formatGameNumber(r.totalPower.coerceAtMost(Int.MAX_VALUE.toLong()).toInt()), style = AaaType.Numeric.copy(fontSize = 11.5.sp))
                            }
                        }
                    }
                }
            }
        }
        if (shadowOpen) ShadowArenaPanel(ui, viewModel, onClose = { shadowOpen = false })
        if (legacy) {
            LegacyPanel("Sieć Dao", onClose = { legacy = false }) {
                MultiplayerScreen(viewModel = viewModel, uiState = ui)
            }
        }
    }
}

@Composable
private fun BossBar(boss: com.example.model.WorldBossRaid, modifier: Modifier = Modifier) {
    val frac = if (boss.maxHp > 0) boss.currentHp.toFloat() / boss.maxHp.toFloat() else 0f
    val hp by animateFloatAsState(frac, tween(250), label = "boss_hp")
    val trail by animateFloatAsState(frac, tween(900, delayMillis = 250), label = "boss_trail")
    Column(modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                boss.name,
                style = AaaType.Title.copy(fontSize = 15.sp, color = Color(0xFFE9D4FF), shadow = androidx.compose.ui.graphics.Shadow(Aaa.Violet, Offset.Zero, 14f)),
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text("${boss.hoursRemaining} h", style = AaaType.Numeric.copy(fontSize = 10.sp, color = Aaa.TextMid))
        }
        Spacer(Modifier.height(6.dp))
        Box(
            Modifier
                .fillMaxWidth()
                .height(13.dp)
                .clip(CtaShape)
                .background(Color.Black.copy(alpha = 0.6f))
                .drawBehind {
                    drawRect(Color.White.copy(alpha = 0.55f), size = Size(size.width * trail, size.height))
                    drawRect(
                        Brush.verticalGradient(listOf(Color(0xFFE7A3FF), Color(0xFF8A3FD0), Color(0xFF4A1A7A))),
                        size = Size(size.width * hp, size.height)
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            Text(
                "${formatLong(boss.currentHp)} / ${formatLong(boss.maxHp)}",
                style = AaaType.Numeric.copy(fontSize = 8.5.sp, color = Color.White)
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(boss.title, style = AaaType.Label.copy(fontSize = 9.5.sp, color = Aaa.TextMid), maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

internal fun formatLong(v: Long): String =
    if (v <= Int.MAX_VALUE) formatGameNumber(v.toInt()) else String.format(java.util.Locale.ROOT, "%.1f mld", v / 1e9).replace('.', ',')

@Composable
private fun DamageNumber(p: DamagePop, w: androidx.compose.ui.unit.Dp, h: androidx.compose.ui.unit.Dp) {
    val a = remember { Animatable(0f) }
    LaunchedEffect(Unit) { a.animateTo(1f, tween(950, easing = LinearOutSlowInEasing)) }
    val crit = p.value > 10_000
    Text(
        text = formatLong(p.value),
        style = AaaType.Numeric.copy(
            fontSize = if (crit) 26.sp else 20.sp,
            fontWeight = FontWeight.Black,
            color = if (crit) Aaa.GoldHi else Color.White,
            shadow = androidx.compose.ui.graphics.Shadow(if (crit) Aaa.Gold else Aaa.Violet, Offset.Zero, 16f)
        ),
        modifier = Modifier
            .offset(x = w * p.fx, y = h * p.fy)
            .graphicsLayer {
                val v = a.value
                translationY = -v * 46.dp.toPx()
                alpha = if (v < 0.2f) v / 0.2f else 1f - (v - 0.2f) / 0.8f
                val s = if (v < 0.2f) 0.6f + 2.75f * v else 1.15f - (v - 0.2f) * 0.2f
                scaleX = s
                scaleY = s
            }
    )
}

// ─────────────────────────────────────────────────────────────────────────────
//  MAGAZYN
// ─────────────────────────────────────────────────────────────────────────────

private enum class ItemKind { PILL, ORE, HERB, ARTIFACT }

private data class VaultItem(val name: String, val count: Int, val kind: ItemKind, val rarity: String, val artifact: Artifact? = null)

private val pillNames: Set<String> by lazy { com.example.game.WuxiaGenerator.getAlchemyRecipes().map { it.pillName }.toSet() }

private fun kindOf(name: String): ItemKind = when {
    name in pillNames || name.contains("Pigułka", true) || name.contains("Eliksir", true) -> ItemKind.PILL
    ResourceCatalog.herbs.any { it.name == name } -> ItemKind.HERB
    else -> ItemKind.ORE // rudy i trofea z bossów
}

private fun rarityOf(name: String): String {
    val tier = (ResourceCatalog.herbs + ResourceCatalog.minerals).firstOrNull { it.name == name }?.tier
        ?: com.example.game.WuxiaGenerator.getAlchemyRecipes().firstOrNull { it.pillName == name }?.tier
        ?: 4 // trofea z bossów
    return when {
        tier >= 5 -> "Legendarny"
        tier >= 4 -> "Epicki"
        tier >= 2 -> "Rzadki"
        else -> "Zwykły"
    }
}

private fun itemColor(name: String, kind: ItemKind): Color {
    val hue = h01(name.hashCode(), 9.1f)
    val base = when (kind) {
        ItemKind.PILL -> listOf(Color(0xFFFFCF6B), Color(0xFFFF6B7A), Color(0xFFC48CFF), Color(0xFF7FE3F7))
        ItemKind.ORE -> listOf(Color(0xFF9FB7D8), Color(0xFF6FE0B0), Color(0xFFFF9A4A), Color(0xFF8E7BBF))
        ItemKind.HERB -> listOf(Color(0xFF7BD88F), Color(0xFFE8F4FF), Color(0xFFB48CFF), Color(0xFFFF8A73))
        ItemKind.ARTIFACT -> listOf(Aaa.Gold, Aaa.Azure, Aaa.Violet, Color(0xFFE8F4FF))
    }
    return base[(hue * base.size).toInt().coerceIn(0, base.size - 1)]
}

/** Ikona przedmiotu (pigułka, ruda, zioło, artefakt) – ta sama co w Magazynie; do list w panelach. */
@Composable
fun ItemIcon(name: String, size: androidx.compose.ui.unit.Dp = 26.dp) {
    val kind = kindOf(name)
    val c = itemColor(name, kind)
    // Etap 16: malowana grafika, gdy jest (zwoje wg stopnia, zioła, rudy, pigułki, talizmany, jaja).
    val art = itemArtKey(name)
    if (art != null && com.example.ui.graphics.rememberWuxiaGraphic(art) != null) {
        Box(Modifier.size(size).background(Brush.radialGradient(listOf(c.copy(alpha = 0.22f), Color.Transparent))), contentAlignment = Alignment.Center) {
            GameIcon(art, size * 0.92f)
        }
        return
    }
    Box(
        Modifier
            .size(size)
            .background(Brush.radialGradient(listOf(c.copy(alpha = 0.28f), Aaa.Ink0.copy(alpha = 0.6f))))
            .border(1.dp, c.copy(alpha = 0.45f))
    ) {
        Canvas(Modifier.matchParentSize().padding(3.dp)) { drawItem(kind, c) }
    }
}

private fun DrawScope.drawItem(kind: ItemKind, c: Color) {
    val w = size.width
    val h = size.height
    when (kind) {
        ItemKind.PILL -> {
            drawCircle(c, radius = w * 0.34f)
            drawCircle(Brush.radialGradient(listOf(Color.White.copy(alpha = 0.55f), Color.Black.copy(alpha = 0.45f)), center = Offset(w * 0.4f, h * 0.36f), radius = w * 0.5f), radius = w * 0.34f)
            drawOval(Color.White.copy(alpha = 0.7f), topLeft = Offset(w * 0.3f, h * 0.27f), size = Size(w * 0.22f, h * 0.13f))
        }
        ItemKind.ORE -> {
            val p = Path().apply {
                moveTo(w * 0.18f, h * 0.62f); lineTo(w * 0.34f, h * 0.25f); lineTo(w * 0.62f, h * 0.16f)
                lineTo(w * 0.84f, h * 0.4f); lineTo(w * 0.75f, h * 0.78f); lineTo(w * 0.38f, h * 0.84f); close()
            }
            drawPath(p, c)
            val shade = Path().apply { moveTo(w * 0.18f, h * 0.62f); lineTo(w * 0.34f, h * 0.25f); lineTo(w * 0.5f, h * 0.5f); lineTo(w * 0.38f, h * 0.84f); close() }
            drawPath(shade, Color.Black.copy(alpha = 0.25f))
            drawLine(Color.White.copy(alpha = 0.5f), Offset(w * 0.34f, h * 0.25f), Offset(w * 0.5f, h * 0.5f), 1.dp.toPx())
            drawLine(Color.White.copy(alpha = 0.5f), Offset(w * 0.5f, h * 0.5f), Offset(w * 0.84f, h * 0.4f), 1.dp.toPx())
        }
        ItemKind.HERB -> {
            drawLine(Color(0xFF3A6B3A), Offset(w * 0.5f, h * 0.9f), Offset(w * 0.5f, h * 0.25f), 2.dp.toPx())
            val l = Path().apply { moveTo(w * 0.5f, h * 0.45f); cubicTo(w * 0.2f, h * 0.45f, w * 0.18f, h * 0.2f, w * 0.18f, h * 0.15f); cubicTo(w * 0.4f, h * 0.15f, w * 0.5f, h * 0.28f, w * 0.5f, h * 0.45f); close() }
            val r = Path().apply { moveTo(w * 0.5f, h * 0.6f); cubicTo(w * 0.8f, h * 0.6f, w * 0.82f, h * 0.35f, w * 0.82f, h * 0.3f); cubicTo(w * 0.6f, h * 0.3f, w * 0.5f, h * 0.42f, w * 0.5f, h * 0.6f); close() }
            drawPath(l, c)
            drawPath(r, c)
            drawCircle(Color.White.copy(alpha = 0.8f), radius = w * 0.08f, center = Offset(w * 0.5f, h * 0.2f))
        }
        ItemKind.ARTIFACT -> {
            val blade = Path().apply {
                moveTo(w * 0.74f, h * 0.12f); lineTo(w * 0.88f, h * 0.12f); lineTo(w * 0.88f, h * 0.26f)
                lineTo(w * 0.42f, h * 0.72f); lineTo(w * 0.28f, h * 0.58f); close()
            }
            drawPath(blade, c)
            drawLine(Color.White.copy(alpha = 0.6f), Offset(w * 0.86f, h * 0.14f), Offset(w * 0.52f, h * 0.48f), 1.dp.toPx())
            drawLine(Aaa.GoldMid, Offset(w * 0.22f, h * 0.53f), Offset(w * 0.47f, h * 0.78f), 2.5.dp.toPx(), cap = StrokeCap.Round)
            drawLine(Aaa.GoldMid, Offset(w * 0.32f, h * 0.68f), Offset(w * 0.16f, h * 0.84f), 2.5.dp.toPx(), cap = StrokeCap.Round)
        }
    }
}

@Composable
fun VaultModule(ui: WuxiaUiState, viewModel: WuxiaGameViewModel, onBack: () -> Unit) {
    var cat by remember { mutableIntStateOf(0) }
    var market by remember { mutableStateOf(false) }
    val all = remember(ui.inventory, ui.vaultArtifacts) {
        ui.inventory.filter { it.value > 0 }.map { (n, q) -> VaultItem(n, q, kindOf(n), rarityOf(n)) }
            .sortedWith(compareBy<VaultItem> { it.kind.ordinal }.thenBy { it.name }) +
            ui.vaultArtifacts.map { VaultItem(it.name, 1, ItemKind.ARTIFACT, it.rarity, it) }
    }
    val visible = all.filter {
        when (cat) {
            1 -> it.kind == ItemKind.PILL
            2 -> it.kind == ItemKind.ORE || it.kind == ItemKind.HERB
            3 -> it.kind == ItemKind.ARTIFACT
            else -> true
        }
    }
    var selName by remember { mutableStateOf<String?>(null) }
    val sel = visible.firstOrNull { it.name == selName } ?: visible.firstOrNull()
    val ready = ui.disciples.count { it.realmProgress >= 100 }
    val champ = champion(ui)

    ModuleScaffold(
        module = GameModule.VAULT,
        subtitle = "${all.sumOf { it.count }} przedmiotów · ${all.size} rodzajów",
        onBack = onBack,
        glyphAlpha = 0.07f,
        bottomShade = 0.95f,
        background = {
            Box(
                Modifier
                    .matchParentSize()
                    .background(Brush.radialGradient(listOf(Color(0xFF2A1A14), Aaa.Ink1), radius = 900f))
                    .drawBehind {
                        val step = 14.dp.toPx()
                        var x = -size.height
                        while (x < size.width) {
                            drawLine(Aaa.Gold.copy(alpha = 0.05f), Offset(x, 0f), Offset(x + size.height, size.height), 1f)
                            drawLine(Aaa.Gold.copy(alpha = 0.05f), Offset(x + size.height, 0f), Offset(x, size.height), 1f)
                            x += step
                        }
                    }
            )
        },
        topRight = { CurrencyPill(AaaGlyph.SPIRIT_STONE, ui.spiritStones) }
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .safeTop()
                .padding(start = 60.dp, top = 58.dp, end = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextTabs(listOf("Wszystko", "Pigułki", "Surowce", "Artefakty"), cat, { cat = it }, Modifier.weight(1f))
            if (ui.economy.enabled) GhostButton("Targ", onClick = { market = true }, height = 30.dp)
        }
        Column(
            Modifier
                .fillMaxSize()
                .safeTop()
                .safeBottom()
                .padding(start = 12.dp, end = 12.dp, top = 94.dp, bottom = 12.dp)
        ) {
            if (sel != null) {
                val rc = rarityColor(sel.rarity)
                val ic = itemColor(sel.name, sel.kind)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(92.dp), contentAlignment = Alignment.Center) {
                        Canvas(Modifier.matchParentSize()) {
                            drawCircle(Brush.radialGradient(listOf(rc.copy(alpha = 0.32f), Color.Transparent)), radius = size.minDimension / 2f)
                            rotate(45f) {
                                drawRect(rc.copy(alpha = 0.55f), topLeft = Offset(size.width * 0.16f, size.height * 0.16f), size = Size(size.width * 0.68f, size.height * 0.68f), style = Stroke(1.dp.toPx()))
                            }
                        }
                        Canvas(Modifier.size(62.dp)) { drawItem(sel.kind, ic) }
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Overline(sel.rarity, color = rc)
                        Text(sel.name, style = AaaType.Title.copy(fontSize = 16.sp), maxLines = 2, overflow = TextOverflow.Ellipsis)
                        val desc = when {
                            sel.artifact != null -> "Atak +${sel.artifact.attackBonus} · Obrona +${sel.artifact.defenseBonus}. ${sel.artifact.specialEffect}"
                            sel.kind == ItemKind.PILL -> "W magazynie: ${sel.count}. Pigułki przełomu zużywa auto-przełom."
                            sel.kind == ItemKind.ORE -> "W magazynie: ${sel.count}. Surowiec Boskiej Kuźni."
                            else -> "W magazynie: ${sel.count}. Składnik alchemii i przysmak bestii."
                        }
                        Text(desc, style = AaaType.Body.copy(fontSize = 10.5.sp, lineHeight = 14.sp, color = Aaa.TextMid), maxLines = 3, overflow = TextOverflow.Ellipsis)
                        val art = sel.artifact
                        when {
                            art != null && champ != null -> GoldCta(
                                "Załóż: ${champ.name.substringBefore(" (").substringBefore(" ")}",
                                onClick = { WuxiaAudioEngine.playSwordClash(); viewModel.equipArtifact(champ.id, art) },
                                modifier = Modifier.height(36.dp),
                                testTag = "vault_equip"
                            )
                            sel.name == PILL_BREAKTHROUGH -> GoldCta(
                                if (ready > 0) "Przełom ($ready)" else "Brak gotowych",
                                enabled = ready > 0,
                                onClick = { WuxiaAudioEngine.playThunderStrike(); viewModel.breakthroughAllReadyDisciples() },
                                modifier = Modifier.height(36.dp),
                                testTag = "vault_use_pill"
                            )
                            sel.kind == ItemKind.PILL -> {
                                // Pigułkę dostaje adept najbliżej przełomu (jeszcze niegotowy), a gdy wszyscy gotowi – czempion.
                                val target = ui.disciples.filter { it.realmProgress < 100 }.maxByOrNull { it.realmProgress } ?: champ
                                if (target != null) GoldCta(
                                    "Podaj: ${target.name.substringBefore(" (").substringBefore(" ")}",
                                    onClick = { WuxiaAudioEngine.playCauldronFlame(); viewModel.usePill(target.id, sel.name) },
                                    modifier = Modifier.height(36.dp),
                                    testTag = "vault_give_pill"
                                )
                            }
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
            }
            LazyVerticalGrid(
                columns = GridCells.Fixed(5),
                horizontalArrangement = Arrangement.spacedBy(7.dp),
                verticalArrangement = Arrangement.spacedBy(7.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(visible, key = { it.name + it.kind }) { item ->
                    val on = item.name == sel?.name
                    val rc = rarityColor(item.rarity)
                    Box(
                        Modifier
                            .aspectRatio(1f)
                            .clip(cutCornerTicket(0.14f))
                            .background(Brush.linearGradient(listOf(rc.copy(alpha = 0.28f), Aaa.Ink1), start = Offset.Zero, end = Offset(200f, 260f)))
                            .drawBehind {
                                drawRect(if (on) Aaa.GoldHi else rc.copy(alpha = 0.55f), style = Stroke(if (on) 2.dp.toPx() else 1.dp.toPx()))
                            }
                            .clickable(role = Role.Button) { selName = item.name }
                            .testTag("vault_item_${item.name.hashCode()}")
                    ) {
                        Canvas(Modifier.fillMaxSize(0.62f).align(Alignment.Center)) { drawItem(item.kind, itemColor(item.name, item.kind)) }
                        if (item.count > 1) {
                            Text(
                                "${item.count}",
                                style = AaaType.Numeric.copy(fontSize = 9.5.sp, color = Aaa.TextHi, shadow = androidx.compose.ui.graphics.Shadow(Color.Black, Offset(0f, 1f), 3f)),
                                modifier = Modifier.align(Alignment.BottomEnd).padding(end = 4.dp, bottom = 1.dp)
                            )
                        }
                    }
                }
                val empty = (30 - visible.size).coerceAtLeast(0)
                items(empty) {
                    Box(
                        Modifier
                            .aspectRatio(1f)
                            .clip(cutCornerTicket(0.14f))
                            .background(Color.White.copy(alpha = 0.03f))
                    )
                }
            }
        }
    }
    if (market) EconomyPanel(ui, viewModel, EconomyTab.MARKET, onClose = { market = false })
}
