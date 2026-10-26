package com.example.ui.game

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.StrokeCap
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
import com.example.game.WuxiaGenerator
import com.example.game.WuxiaUiState
import com.example.model.AlchemyRecipe
import com.example.model.FacilityType
import com.example.model.ForgingRecipe
import com.example.model.SectDivineBeast
import com.example.ui.aaa.Aaa
import com.example.ui.aaa.AaaGlyph
import com.example.ui.aaa.AaaGlyphIcon
import com.example.ui.aaa.AaaType
import com.example.ui.aaa.rememberAaaClock
import com.example.ui.audio.WuxiaAudioEngine
import com.example.ui.graphics.WuxiaAssetLoader
import com.example.ui.screens.CraftingScreen
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** Wywołuje [onIncrease], gdy obserwowana liczba wzrośnie (np. nowa pigułka w magazynie). */
@Composable
internal fun WatchIncrease(key: Any?, value: Int, onIncrease: (Int) -> Unit) {
    val last = remember(key) { intArrayOf(value) }
    val cb by rememberUpdatedState(onIncrease)
    LaunchedEffect(key, value) {
        if (value > last[0]) cb(value - last[0])
        last[0] = value
    }
}

internal fun rarityColor(r: String): Color = when {
    r.contains("Legend", true) || r.contains("Boski", true) || r.contains("Niebiań", true) -> Aaa.Gold
    r.contains("Epick", true) || r.contains("Mityczn", true) || r.contains("Mistyczn", true) -> Aaa.Violet
    r.contains("Rzadk", true) || r.contains("Duchow", true) -> Aaa.Azure
    else -> Aaa.TextMid
}

@Composable
private fun IngredientChip(name: String, have: Int, need: Int, glyph: @Composable () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        glyph()
        Spacer(Modifier.width(5.dp))
        Text(shortName(name, 2), style = AaaType.Label.copy(fontSize = 10.5.sp, color = Aaa.TextMid), maxLines = 1)
        Spacer(Modifier.width(4.dp))
        Text(
            "$have/$need",
            style = AaaType.Numeric.copy(fontSize = 10.5.sp, color = if (have >= need) Aaa.Jade else Aaa.Cinnabar)
        )
    }
}

@Composable
private fun HerbDot(color: Color) {
    Canvas(Modifier.size(14.dp)) {
        drawCircle(color.copy(alpha = 0.3f), radius = size.minDimension / 2f)
        drawCircle(color, radius = size.minDimension / 4f)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  ALCHEMIA
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun AlchemyModule(ui: WuxiaUiState, viewModel: WuxiaGameViewModel, onBack: () -> Unit) {
    val recipes = remember { WuxiaGenerator.getAlchemyRecipes() }
    var sel by remember { mutableIntStateOf(0) }
    val recipe: AlchemyRecipe = recipes.getOrElse(sel) { recipes.first() }
    val chamber = ui.facilities.firstOrNull { it.type == FacilityType.ALCHEMY_CHAMBER }?.level ?: 1
    var legacy by remember { mutableStateOf(false) }
    val celebration = rememberCelebration()
    val scope = rememberCoroutineScope()
    val brew = remember { Animatable(0f) }
    val flame = ui.activeFlame ?: ui.flames.firstOrNull()

    WatchIncrease(recipe.pillName, ui.inventory[recipe.pillName] ?: 0) { gained ->
        WuxiaAudioEngine.playUltimateBurst()
        celebration.fire("${shortName(recipe.pillName, 3)} +$gained")
    }

    Box(Modifier.fillMaxSize()) {
        ModuleScaffold(
            module = GameModule.ALCHEMY,
            subtitle = "Kocioł Dziewięciu Płomieni · poziom $chamber",
            onBack = onBack,
            accent = Aaa.Ember,
            bottomShade = 0.5f,
            background = {
                ArtImage("bg_alchemy_hall", Modifier.matchParentSize(), colorFilter = dimFilter(0.66f))
            },
            topRight = { CurrencyPill(AaaGlyph.SPIRIT_STONE, ui.spiritStones) }
        ) {
            HeatRing(rate = flame?.bonusCraftingRate ?: 20, brewing = brew.value, modifier = Modifier.align(BiasAlignment(0f, -0.08f)))

            // Wybór płomienia
            if (flame != null && ui.flames.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .safeTop()
                        .padding(start = 12.dp, end = 12.dp, top = 64.dp)
                        .fillMaxWidth()
                        .background(Brush.horizontalGradient(listOf(Aaa.Ink0.copy(alpha = 0.8f), Aaa.Ink0.copy(alpha = 0.3f))))
                        .drawBehind { drawRect(Aaa.Ember, size = Size(2.dp.toPx(), size.height)) }
                        .padding(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ArtImage(
                        "item_geocentric_flame",
                        Modifier.size(38.dp).clip(CircleShape),
                        alignment = BiasAlignment(0f, 0.1f)
                    )
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(flame.name, style = AaaType.Title.copy(fontSize = 11.5.sp, color = Aaa.TextHi, letterSpacing = 0.sp), maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(
                            "${flame.tierName} · ${flame.temperatureCelsius}°C · +${flame.bonusCraftingRate}% wytopu",
                            style = AaaType.Numeric.copy(fontSize = 9.5.sp, color = Aaa.Ember),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    val idx = ui.flames.indexOfFirst { it.id == flame.id }.coerceAtLeast(0)
                    ArrowButton("‹") { viewModel.selectGeocentricFlame(ui.flames[(idx - 1 + ui.flames.size) % ui.flames.size]); WuxiaAudioEngine.playCauldronFlame() }
                    Spacer(Modifier.width(4.dp))
                    ArrowButton("›") { viewModel.selectGeocentricFlame(ui.flames[(idx + 1) % ui.flames.size]); WuxiaAudioEngine.playCauldronFlame() }
                }
            }

            ModuleSheet {
                // Karuzela receptur
                Row(
                    Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    recipes.forEachIndexed { i, r ->
                        val locked = chamber < r.requiredAlchemyChamberLevel
                        val on = i == sel
                        Column(
                            modifier = Modifier
                                .width(66.dp)
                                .clickable(role = Role.Button) { sel = i }
                                .testTag("alchemy_recipe_${r.id}"),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                Modifier
                                    .size(50.dp)
                                    .clip(CircleShape)
                                    .background(if (on) Aaa.GoldHi else Aaa.Gold.copy(alpha = 0.25f))
                                    .padding(if (on) 2.dp else 1.dp)
                                    .clip(CircleShape)
                            ) {
                                ArtImage(
                                    "item_pill_breakthrough",
                                    Modifier.matchParentSize(),
                                    colorFilter = when {
                                        locked -> dimFilter(0.35f, 0f)
                                        on -> ColorFilter.tint(tierTint(r.tier), BlendMode.Modulate)
                                        else -> dimFilter(0.65f)
                                    }
                                )
                                if (locked) Text("🔒", fontSize = 14.sp, modifier = Modifier.align(Alignment.Center))
                            }
                            Spacer(Modifier.height(4.dp))
                            Text(
                                shortName(r.pillName.removePrefix("Pigułka ").removePrefix("Eliksir "), 2),
                                style = AaaType.Label.copy(
                                    fontFamily = FontFamily.Serif,
                                    fontSize = 8.5.sp,
                                    lineHeight = 10.sp,
                                    color = if (on) Aaa.GoldHi else Aaa.TextLow,
                                    letterSpacing = 0.sp
                                ),
                                textAlign = TextAlign.Center,
                                maxLines = 2
                            )
                        }
                    }
                }

                Overline(recipe.tierName, color = Aaa.Ember)
                SheetTitle(recipe.pillName, size = 22)
                SheetBody(recipe.effectDescription, maxLines = 2)
                val main = ui.inventory[recipe.requiredHerbName] ?: 0
                val sec = recipe.secondaryHerbName
                Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    IngredientChip(recipe.requiredHerbName, main, recipe.requiredHerbCount) { HerbDot(Color(0xFF7BD88F)) }
                    if (sec != null) {
                        IngredientChip(sec, ui.inventory[sec] ?: 0, recipe.secondaryHerbCount) { HerbDot(Color(0xFFE8F4FF)) }
                    }
                }
                val locked = chamber < recipe.requiredAlchemyChamberLevel
                val canBrew = !locked &&
                    main >= recipe.requiredHerbCount &&
                    (sec == null || (ui.inventory[sec] ?: 0) >= recipe.secondaryHerbCount) &&
                    ui.spiritStones >= recipe.requiredSpiritStones
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    GhostButton("Księga", onClick = { legacy = true }, modifier = Modifier.weight(1f))
                    GoldCta(
                        text = if (locked) "Sala poz. ${recipe.requiredAlchemyChamberLevel}" else "Wytop",
                        trailing = if (locked) null else "${recipe.requiredSpiritStones}",
                        enabled = canBrew && !brew.isRunning,
                        progress = brew.value,
                        onClick = {
                            WuxiaAudioEngine.playCauldronFlame()
                            scope.launch {
                                brew.snapTo(0f)
                                brew.animateTo(1f, tween(1400, easing = LinearEasing))
                                viewModel.brewPill(recipe)
                                brew.snapTo(0f)
                            }
                        },
                        modifier = Modifier.weight(1.6f),
                        testTag = "alchemy_brew"
                    )
                }
            }
            CelebrationOverlay(celebration, flashColor = Color(0xFFFFD9A8))
        }
        if (legacy) {
            LegacyPanel("Rzemiosło Sekty", onClose = { legacy = false }) {
                CraftingScreen(viewModel = viewModel, uiState = ui)
            }
        }
    }
}

private fun tierTint(tier: Int): Color = when (tier) {
    1 -> Color(0xFFFFE0A0)
    2 -> Color(0xFFB8F0FF)
    3 -> Color(0xFFE0B8FF)
    else -> Color(0xFFFFB0A0)
}

@Composable
private fun ArrowButton(label: String, onClick: () -> Unit) {
    Box(
        Modifier
            .size(30.dp)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.07f))
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(label, color = Aaa.Gold, fontSize = 17.sp, fontWeight = FontWeight.Bold)
    }
}

/** Pierścień żaru wokół kotła: łuk ∝ sile płomienia, pulsuje mocniej podczas wytopu. */
@Composable
private fun HeatRing(rate: Int, brewing: Float, modifier: Modifier = Modifier) {
    val sweep by animateFloatAsState((0.25f + rate / 100f).coerceIn(0.1f, 1f) * 360f, tween(800), label = "heat_sweep")
    val clock = rememberAaaClock()
    Canvas(modifier.size(220.dp)) {
        val t = clock.value
        val stroke = 3.dp.toPx()
        val r = size.minDimension / 2f - stroke * 3f
        val tl = Offset(center.x - r, center.y - r)
        val sz = Size(r * 2f, r * 2f)
        val pulse = 0.5f + 0.5f * sin(t * (2f + brewing * 8f))
        drawCircle(
            Brush.radialGradient(
                listOf(Aaa.Ember.copy(alpha = 0.10f + 0.25f * brewing * pulse), Color.Transparent),
                center = center,
                radius = r * 1.2f
            ),
            radius = r * 1.2f
        )
        drawArc(Color.White.copy(alpha = 0.1f), 0f, 360f, false, topLeft = tl, size = sz, style = Stroke(stroke))
        drawArc(
            Brush.sweepGradient(listOf(Color(0xFFFFD27A), Color(0xFFFF5A2A), Color(0xFFFFD27A)), center),
            -90f, sweep, false, topLeft = tl, size = sz,
            style = Stroke(stroke, cap = StrokeCap.Round)
        )
        // Iskra na końcu łuku
        val a = ((-90f + sweep) * PI / 180f).toFloat()
        val tip = Offset(center.x + cos(a) * r, center.y + sin(a) * r)
        drawCircle(Color(0xFFFFE8B0).copy(alpha = 0.35f + 0.4f * pulse), radius = stroke * 3f, center = tip)
        drawCircle(Color.White, radius = stroke * 0.9f, center = tip)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  KUŹNIA
// ─────────────────────────────────────────────────────────────────────────────

private const val STRIKES = 3

@Composable
fun ForgeModule(ui: WuxiaUiState, viewModel: WuxiaGameViewModel, onBack: () -> Unit) {
    val recipes = remember { WuxiaGenerator.getForgingRecipes() }
    var sel by remember { mutableIntStateOf(0) }
    val recipe: ForgingRecipe = recipes.getOrElse(sel) { recipes.first() }
    var hits by remember(sel) { mutableIntStateOf(0) }
    var legacy by remember { mutableStateOf(false) }
    val celebration = rememberCelebration()
    val scope = rememberCoroutineScope()
    val spark = remember { Animatable(1f) }
    val shake = remember { Animatable(0f) }
    var strikeSeed by remember { mutableIntStateOf(0) }
    val forge = ui.facilities.firstOrNull { it.type == FacilityType.DIVINE_FORGE }

    WatchIncrease(Unit, ui.vaultArtifacts.size) {
        celebration.fire("${recipe.artifactResult.rarity}: ${shortName(recipe.artifactName, 2)}!")
    }

    val have = ui.inventory[recipe.requiredMineralName] ?: 0
    val canForge = have >= recipe.requiredMineralCount && ui.spiritStones >= recipe.requiredSpiritStones
    val rc = rarityColor(recipe.artifactResult.rarity)

    Box(
        Modifier
            .fillMaxSize()
            .graphicsLayer { translationX = shake.value * 7.dp.toPx() }
    ) {
        ModuleScaffold(
            module = GameModule.FORGE,
            subtitle = "Boska Kuźnia · poziom ${forge?.level ?: 1} · ${ui.timeOfDay.polishName}",
            onBack = onBack,
            accent = Aaa.Ember,
            bottomShade = 0.48f,
            background = {
                ArtImage("bg_forge_hall", Modifier.matchParentSize(), colorFilter = dimFilter(0.62f))
            },
            topRight = { CurrencyPill(AaaGlyph.SPIRIT_STONE, ui.spiritStones) }
        ) {
            // Iskry spod młota – deterministyczne z ziarna uderzenia
            Canvas(Modifier.matchParentSize()) {
                val p = spark.value
                if (p < 1f) {
                    val origin = Offset(size.width * 0.5f, size.height * 0.40f)
                    for (i in 0 until 48) {
                        val ang = (-PI / 2 + (h01(i + strikeSeed * 61, 1.7f) - 0.5f) * 2.6).toFloat()
                        val sp = (180f + h01(i + strikeSeed * 61, 2.9f) * 420f) * density / 2.6f
                        val x = origin.x + cos(ang) * sp * p
                        val y = origin.y + sin(ang) * sp * p + 520f * p * p * density / 2.6f
                        val a = (1f - p)
                        drawLine(
                            Color(0xFFFFC870).copy(alpha = a),
                            Offset(x, y),
                            Offset(x - cos(ang) * 10f * a, y - sin(ang) * 10f * a - 6f * p),
                            strokeWidth = 1.8.dp.toPx(),
                            cap = StrokeCap.Round
                        )
                    }
                    drawCircle(
                        Brush.radialGradient(listOf(Color(0xFFFFF0C0).copy(alpha = 0.7f * (1f - p)), Color.Transparent), center = origin, radius = 160.dp.toPx()),
                        radius = 160.dp.toPx(),
                        center = origin
                    )
                }
            }

            ModuleSheet {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    recipes.forEachIndexed { i, r ->
                        val on = i == sel
                        val c = rarityColor(r.artifactResult.rarity)
                        Text(
                            text = shortName(r.artifactName, 2),
                            style = AaaType.Label.copy(
                                fontFamily = FontFamily.Serif,
                                fontSize = 10.sp,
                                color = if (on) Aaa.GoldHi else Aaa.TextLow,
                                letterSpacing = 0.sp
                            ),
                            maxLines = 1,
                            modifier = Modifier
                                .background(if (on) c.copy(alpha = 0.18f) else Aaa.Ink0.copy(alpha = 0.55f))
                                .drawBehind { drawRect(c, topLeft = Offset(0f, size.height - 2.dp.toPx()), size = Size(size.width, 2.dp.toPx())) }
                                .clickable(role = Role.Button) { sel = i }
                                .padding(horizontal = 10.dp, vertical = 7.dp)
                                .testTag("forge_recipe_${r.id}")
                        )
                    }
                }
                Overline(recipe.artifactResult.rarity, color = rc)
                SheetTitle(recipe.artifactName, size = 22)
                val art = recipe.artifactResult
                if (art.attackBonus > 0) StatLine("Atak", "+${art.attackBonus}", art.attackBonus / 400f, rc)
                if (art.defenseBonus > 0) StatLine("Obrona", "+${art.defenseBonus}", art.defenseBonus / 400f, rc)
                SheetBody(art.specialEffect, maxLines = 2)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IngredientChip(recipe.requiredMineralName, have, recipe.requiredMineralCount) {
                        Canvas(Modifier.size(14.dp)) {
                            rotate(45f) { drawRect(Color(0xFF9FB7D8), topLeft = Offset(size.width * 0.2f, size.height * 0.2f), size = Size(size.width * 0.6f, size.height * 0.6f)) }
                        }
                    }
                    Spacer(Modifier.weight(1f))
                    repeat(STRIKES) { i ->
                        val on = i < hits
                        Canvas(Modifier.padding(start = 6.dp).size(11.dp)) {
                            rotate(45f) {
                                val tl = Offset(size.width * 0.15f, size.height * 0.15f)
                                val s = Size(size.width * 0.7f, size.height * 0.7f)
                                if (on) drawRect(Aaa.GoldHi, topLeft = tl, size = s)
                                drawRect(Aaa.Gold, topLeft = tl, size = s, style = Stroke(1.5.dp.toPx()))
                            }
                        }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    GhostButton("Księga", onClick = { legacy = true }, modifier = Modifier.weight(1f))
                    GoldCta(
                        text = if (canForge) "Uderz młotem" else "Brak surowca",
                        trailing = if (canForge) "${recipe.requiredSpiritStones}" else null,
                        enabled = canForge,
                        onClick = {
                            WuxiaAudioEngine.playSwordClash()
                            strikeSeed++
                            hits++
                            scope.launch { spark.snapTo(0f); spark.animateTo(1f, tween(900, easing = LinearOutSlowInEasing)) }
                            scope.launch {
                                shake.snapTo(0f)
                                shake.animateTo(0f, keyframes {
                                    durationMillis = 320
                                    -1f at 40
                                    1f at 100
                                    -0.6f at 170
                                    0.4f at 240
                                })
                            }
                            if (hits >= STRIKES) {
                                hits = 0
                                WuxiaAudioEngine.playUltimateBurst()
                                viewModel.forgeArtifact(recipe)
                            }
                        },
                        modifier = Modifier.weight(1.6f),
                        testTag = "forge_strike"
                    )
                }
            }
            CelebrationOverlay(celebration, flashColor = Color(0xFFFFE0B0))
        }
        if (legacy) {
            LegacyPanel("Rzemiosło Sekty", onClose = { legacy = false }) {
                CraftingScreen(viewModel = viewModel, uiState = ui)
            }
        }
    }
}

@Composable
private fun StatLine(label: String, value: String, fraction: Float, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = AaaType.Label.copy(fontSize = 10.sp, color = Aaa.TextMid), modifier = Modifier.width(56.dp))
        ThinBar(fraction, color, Modifier.weight(1f))
        Text(value, style = AaaType.Numeric.copy(fontSize = 12.sp, color = Aaa.Jade), textAlign = TextAlign.End, modifier = Modifier.width(52.dp))
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  BESTIE SEKTY
// ─────────────────────────────────────────────────────────────────────────────

internal fun beastColor(b: SectDivineBeast): Color = when {
    b.element.contains("Płomie", true) || b.element.contains("Ogie", true) -> Aaa.Ember
    b.element.contains("Błyskaw", true) || b.element.contains("Piorun", true) -> Aaa.Azure
    b.element.contains("Cień", true) || b.element.contains("Woda", true) || b.element.contains("Duch", true) -> Aaa.Violet
    b.element.contains("Lód", true) || b.element.contains("Mróz", true) -> Color(0xFFB9E6FF)
    else -> Aaa.Jade
}

@Composable
fun BeastsModule(ui: WuxiaUiState, viewModel: WuxiaGameViewModel, onBack: () -> Unit) {
    var selId by remember { mutableStateOf(ui.activeBeast?.id ?: ui.sectBeasts.firstOrNull()?.id) }
    val b = ui.sectBeasts.firstOrNull { it.id == selId } ?: ui.sectBeasts.firstOrNull()
    var legacy by remember { mutableStateOf(false) }
    val celebration = rememberCelebration()

    if (b != null) {
        WatchIncrease(b.id, b.intimacy) { gained -> celebration.fire("Więź +$gained%", withFlash = false) }
        WatchIncrease(b.id + "_tier", b.tier) { celebration.fire("Ewolucja: poziom ${b.tier}!") }
    }

    Box(Modifier.fillMaxSize()) {
        ModuleScaffold(
            module = GameModule.BEASTS,
            subtitle = "Strażnicy Świętej Góry · ${ui.sectBeasts.size}",
            onBack = onBack,
            accent = b?.let { beastColor(it) } ?: Aaa.Jade,
            bottomShade = 0.42f,
            background = {
                Crossfade(targetState = b?.name ?: "", animationSpec = tween(450), label = "beast_art") { name ->
                    ArtImage(
                        WuxiaAssetLoader.getBeastAvatarKey(name),
                        Modifier.fillMaxSize(),
                        alignment = BiasAlignment(0f, -0.35f),
                        colorFilter = dimFilter(0.86f)
                    )
                }
            },
            topRight = { CurrencyPill(AaaGlyph.SPIRIT_STONE, ui.spiritStones) }
        ) {
            if (b == null) {
                ModuleSheet { SheetTitle("Gniazdo jest puste"); SheetBody("Sekta nie ma jeszcze strażników.") }
            } else {
                val c = beastColor(b)
                val isActive = ui.activeBeast?.id == b.id
                val herbs = ui.inventory[b.favoriteHerb] ?: 0
                ModuleSheet {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        ui.sectBeasts.forEach { x ->
                            val on = x.id == b.id
                            val xc = beastColor(x)
                            Box(
                                Modifier
                                    .size(56.dp)
                                    .clip(CircleShape)
                                    .background(if (on) xc else Aaa.Gold.copy(alpha = 0.25f))
                                    .padding(if (on) 2.dp else 1.dp)
                                    .clip(CircleShape)
                                    .clickable(role = Role.Button) { selId = x.id }
                                    .testTag("beast_${x.id}")
                            ) {
                                ArtImage(
                                    WuxiaAssetLoader.getBeastAvatarKey(x.name),
                                    Modifier.matchParentSize(),
                                    alignment = BiasAlignment(0f, -0.4f),
                                    colorFilter = if (on) null else dimFilter(0.55f, 0.5f)
                                )
                                if (ui.activeBeast?.id == x.id) {
                                    Box(
                                        Modifier
                                            .align(Alignment.BottomCenter)
                                            .fillMaxWidth()
                                            .background(Aaa.Ink0.copy(alpha = 0.75f))
                                    ) {
                                        Text("★", color = Aaa.Gold, fontSize = 9.sp, modifier = Modifier.align(Alignment.Center))
                                    }
                                }
                            }
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        EdgeTag(b.element, c)
                        Text("★".repeat(b.tier.coerceIn(0, 5)) + "☆".repeat((5 - b.tier).coerceAtLeast(0)), color = Aaa.Gold, fontSize = 11.sp)
                    }
                    SheetTitle(b.name, size = 23)
                    SheetBody(b.title, maxLines = 1, modifier = Modifier.offset(y = (-4).dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            KvRow("Więź", "${b.intimacy}%")
                            ThinBar(b.intimacy / 100f, Color(0xFFFF8FB1))
                        }
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            KvRow("Energia", "${b.currentEnergy} / ${b.maxEnergy}")
                            ThinBar(if (b.maxEnergy > 0) b.currentEnergy / b.maxEnergy.toFloat() else 0f, Aaa.Azure)
                        }
                    }
                    SheetBody(b.passiveSectBuff, color = Aaa.Jade, maxLines = 2)
                    KvRow("Technika", b.combatSkillName, valueColor = Aaa.GoldHi)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        GhostButton(
                            text = if (isActive) "Strażnik ★" else "Strażnikiem",
                            enabled = !isActive,
                            onClick = { viewModel.setActiveBeast(b.id); WuxiaAudioEngine.playDivineGong() },
                            modifier = Modifier.weight(1f)
                        )
                        GhostButton(
                            text = "Ewolucja",
                            onClick = { viewModel.evolveSectBeast(b.id) },
                            modifier = Modifier.weight(1f)
                        )
                        GoldCta(
                            text = "Nakarm",
                            trailing = "$herbs",
                            enabled = herbs > 0,
                            onClick = { WuxiaAudioEngine.playCauldronFlame(); viewModel.feedSectBeast(b.id, b.favoriteHerb) },
                            modifier = Modifier.weight(1.4f),
                            testTag = "beast_feed"
                        )
                    }
                    Text(
                        "Przysmak: ${b.favoriteHerb} · Księga bestii",
                        style = AaaType.Label.copy(fontSize = 9.5.sp, color = Aaa.TextLow),
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .clickable(role = Role.Button) { legacy = true }
                            .padding(4.dp)
                    )
                }
            }
            CelebrationOverlay(celebration, flashColor = b?.let { beastColor(it) } ?: Aaa.GoldHi)
        }
        if (legacy) {
            LegacyPanel("Rzemiosło i Bestie", onClose = { legacy = false }) {
                CraftingScreen(viewModel = viewModel, uiState = ui)
            }
        }
    }
}
