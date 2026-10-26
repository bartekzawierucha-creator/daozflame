package com.example.ui.game

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.aaa.Aaa
import com.example.ui.aaa.AaaGlyph
import com.example.ui.aaa.AaaGlyphIcon
import com.example.ui.aaa.AaaType
import com.example.ui.aaa.formatGameNumber
import com.example.ui.graphics.rememberWuxiaGraphic
import kotlinx.coroutines.launch
import kotlin.math.hypot
import kotlin.math.max

/** Kaligrafia pędzlem (Ma Shan Zheng, OFL) – wyłącznie do wielkich znaków-symboli. */
val BrushFont = FontFamily(Font(R.font.ma_shan_zheng))

/** Moduły gry otwierane ze sceny sekty. Znak = symbol pędzlem w tle modułu. */
enum class GameModule(val title: String, val glyph: String) {
    PALACE("Pałac Sekty", "殿"),
    ADEPTS("Adepci", "剑"),
    ALCHEMY("Alchemia", "丹"),
    FORGE("Kuźnia", "锻"),
    BEASTS("Bestie", "兽"),
    WORLD("Świat", "界"),
    CLANS("Klany", "盟"),
    DAO_NET("Sieć Dao", "网"),
    VAULT("Magazyn", "库"),
    OMEN("Omen niebios", "命")
}

typealias OpenModule = (GameModule, Offset) -> Unit

/** Odstęp od wycięcia ekranu / paska u góry (paski są ukryte w trybie pełnoekranowym). */
@Composable
fun Modifier.safeTop(): Modifier = windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top))

@Composable
fun Modifier.safeBottom(): Modifier = windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom))

/** Zapamiętuje środek elementu w układzie okna – punkt startu przejścia tuszem. */
class CenterRef {
    var value: Offset = Offset.Unspecified
}

fun Modifier.trackCenter(ref: CenterRef): Modifier =
    onGloballyPositioned { ref.value = it.boundsInRoot().center }

// ─────────────────────────────────────────────────────────────────────────────
//  Kształty
// ─────────────────────────────────────────────────────────────────────────────

/** Przycisk-strzała: szpiczaste boki jak grot. */
val CtaShape = GenericShape { size, _ ->
    val n = size.height * 0.24f
    moveTo(n, 0f)
    lineTo(size.width - n, 0f)
    lineTo(size.width, size.height / 2f)
    lineTo(size.width - n, size.height)
    lineTo(n, size.height)
    lineTo(0f, size.height / 2f)
    close()
}

/** Ścięty lewy górny i prawy dolny róg – portrety, sloty. */
fun cutCornerTicket(cut: Float = 0.16f) = GenericShape { size, _ ->
    val c = size.minDimension * cut
    moveTo(c, 0f)
    lineTo(size.width, 0f)
    lineTo(size.width, size.height - c)
    lineTo(size.width - c, size.height)
    lineTo(0f, size.height)
    lineTo(0f, c)
    close()
}

/** Okrąg odsłaniający moduł z punktu dotknięcia. */
class CircleRevealShape(private val progress: Float, private val origin: Offset) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val c = Offset(origin.x * size.width, origin.y * size.height)
        val far = max(
            max(hypot(c.x, c.y), hypot(size.width - c.x, c.y)),
            max(hypot(c.x, size.height - c.y), hypot(size.width - c.x, size.height - c.y))
        )
        val r = (far * progress).coerceAtLeast(0.01f)
        return Outline.Generic(Path().apply { addOval(Rect(center = c, radius = r)) })
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Grafika
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun ArtImage(
    assetKey: String,
    modifier: Modifier = Modifier,
    alignment: Alignment = Alignment.Center,
    colorFilter: ColorFilter? = null
) {
    val bmp = rememberWuxiaGraphic(assetKey)
    if (bmp != null) {
        Image(
            bitmap = bmp,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            alignment = alignment,
            colorFilter = colorFilter,
            modifier = modifier
        )
    } else {
        Box(modifier.background(Brush.verticalGradient(listOf(Aaa.Ink3, Aaa.Ink0))))
    }
}

/** Filtr przyciemniający tło modułu (jasność × nasycenie). */
fun dimFilter(brightness: Float, saturation: Float = 1f): ColorFilter {
    val m = ColorMatrix().apply { setToSaturation(saturation) }
    val b = ColorMatrix(
        floatArrayOf(
            brightness, 0f, 0f, 0f, 0f,
            0f, brightness, 0f, 0f, 0f,
            0f, 0f, brightness, 0f, 0f,
            0f, 0f, 0f, 1f, 0f
        )
    )
    m.timesAssign(b)
    return ColorFilter.colorMatrix(m)
}

// ─────────────────────────────────────────────────────────────────────────────
//  Kontrolki
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun CurrencyPill(glyph: AaaGlyph, value: Int, modifier: Modifier = Modifier, onPlus: (() -> Unit)? = null) {
    val shown by animateIntAsState(value, tween(600, easing = FastOutSlowInEasing), label = "pill_value")
    Row(
        modifier = modifier
            .height(26.dp)
            .clip(CircleShape)
            .background(Aaa.Ink0.copy(alpha = 0.68f))
            .drawBehind {
                drawRoundRect(
                    color = Aaa.Gold.copy(alpha = 0.24f),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(size.height / 2f),
                    style = Stroke(width = 1.dp.toPx())
                )
            }
            .padding(start = 4.dp, end = 3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AaaGlyphIcon(glyph, Modifier.size(18.dp))
        Spacer(Modifier.width(5.dp))
        Text(
            text = formatGameNumber(shown),
            style = AaaType.Numeric.copy(fontSize = 12.5.sp, color = Aaa.TextHi)
        )
        Spacer(Modifier.width(5.dp))
        Box(
            modifier = Modifier
                .size(20.dp)
                .clip(CircleShape)
                .background(Brush.verticalGradient(listOf(Aaa.Gold, Aaa.GoldDeep)))
                .then(if (onPlus != null) Modifier.clickable(onClick = onPlus) else Modifier),
            contentAlignment = Alignment.Center
        ) {
            Text("+", color = Aaa.Ink0, fontWeight = FontWeight.Black, fontSize = 14.sp)
        }
    }
}

/** Główny przycisk akcji: złoty grot z refleksem, ugięcie przy dotyku. */
@Composable
fun GoldCta(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    danger: Boolean = false,
    trailing: String? = null,
    progress: Float = 0f,
    testTag: String? = null
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.95f else 1f, spring(dampingRatio = 0.5f), label = "cta_scale")
    val sheen = rememberInfiniteTransition(label = "cta_sheen")
    val sx by sheen.animateFloat(
        initialValue = -0.6f,
        targetValue = 1.6f,
        animationSpec = infiniteRepeatable(
            keyframes {
                durationMillis = 3600
                -0.6f at 0
                -0.6f at 2100 using FastOutSlowInEasing
                1.6f at 3100
                1.6f at 3600
            }
        ),
        label = "cta_sheen_x"
    )
    val fill = if (danger) listOf(Color(0xFFFF8A73), Aaa.Cinnabar, Color(0xFF5A120E))
    else listOf(Aaa.GoldHi, Aaa.Gold, Aaa.GoldDeep)
    val textColor = if (danger) Aaa.GoldHi else Color(0xFF2A1606)

    var mod = modifier
        .height(46.dp)
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
            alpha = if (enabled) 1f else 0.45f
        }
        .clip(CtaShape)
        .background(Brush.verticalGradient(fill))
        .drawWithContent {
            if (progress > 0f) drawRect(Color.White.copy(alpha = 0.3f), size = Size(size.width * progress.coerceIn(0f, 1f), size.height))
            drawContent()
            if (enabled) {
                val cx = sx * size.width
                drawRect(
                    Brush.linearGradient(
                        listOf(Color.Transparent, Color.White.copy(alpha = 0.55f), Color.Transparent),
                        start = Offset(cx - size.width * 0.18f, 0f),
                        end = Offset(cx + size.width * 0.18f, size.height)
                    )
                )
            }
        }
        .clickable(interactionSource = interaction, indication = null, enabled = enabled, role = Role.Button, onClick = onClick)
    if (testTag != null) mod = mod.testTag(testTag)

    Row(
        modifier = mod.padding(horizontal = 18.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = text.uppercase(),
            style = AaaType.Button.copy(color = textColor, fontSize = 12.5.sp, letterSpacing = 1.8.sp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        if (trailing != null) {
            Spacer(Modifier.width(7.dp))
            Text(trailing, style = AaaType.Numeric.copy(color = textColor.copy(alpha = 0.72f), fontSize = 10.5.sp))
        }
    }
}

@Composable
fun GhostButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    danger: Boolean = false,
    enabled: Boolean = true,
    height: Dp = 46.dp
) {
    val c = if (danger) Color(0xFFFF8A73) else Aaa.Gold
    Box(
        modifier = modifier
            .height(height)
            .background(Aaa.Ink0.copy(alpha = 0.55f))
            .drawBehind {
                drawRect(c.copy(alpha = if (enabled) 0.38f else 0.15f), style = Stroke(width = 1.dp.toPx()))
            }
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = AaaType.Button.copy(color = c.copy(alpha = if (enabled) 1f else 0.4f), fontSize = 11.sp, letterSpacing = 0.8.sp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/** Zakładki tekstowe z podkreśleniem – lekkie, bez ramek. */
@Composable
fun TextTabs(items: List<String>, selected: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(18.dp)) {
        items.forEachIndexed { i, label ->
            val on = i == selected
            Column(
                modifier = Modifier.clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    role = Role.Tab
                ) { onSelect(i) },
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = label,
                    style = AaaType.Label.copy(
                        fontFamily = FontFamily.Serif,
                        fontSize = 11.5.sp,
                        color = if (on) Aaa.GoldHi else Aaa.TextLow,
                        fontWeight = if (on) FontWeight.Bold else FontWeight.Medium
                    ),
                    modifier = Modifier.padding(vertical = 4.dp)
                )
                Box(
                    Modifier
                        .height(2.dp)
                        .width(if (on) 28.dp else 0.dp)
                        .background(Aaa.goldSheen())
                )
            }
        }
    }
}

@Composable
fun ThinBar(fraction: Float, color: Color, modifier: Modifier = Modifier, height: Dp = 5.dp) {
    val f by animateFloatAsState(fraction.coerceIn(0f, 1f), tween(650, easing = FastOutSlowInEasing), label = "thin_bar")
    Box(
        modifier
            .fillMaxWidth()
            .height(height)
            .background(Color.White.copy(alpha = 0.10f))
            .drawBehind {
                val w = size.width * f
                if (w > 0f) {
                    drawRect(
                        Brush.horizontalGradient(listOf(color.copy(alpha = 0.35f), color), startX = 0f, endX = w),
                        size = Size(w, size.height)
                    )
                    drawRect(Color.White.copy(alpha = 0.6f), topLeft = Offset(w - 1.5.dp.toPx(), 0f), size = Size(1.5.dp.toPx(), size.height))
                }
            }
    )
}

@Composable
fun KvRow(label: String, value: String, modifier: Modifier = Modifier, valueColor: Color = Aaa.Gold) {
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = AaaType.Label.copy(fontSize = 10.sp, color = Aaa.TextMid))
        Text(value, style = AaaType.Numeric.copy(fontSize = 10.5.sp, color = valueColor))
    }
}

@Composable
fun Overline(text: String, color: Color = Aaa.GoldMid, modifier: Modifier = Modifier) {
    Text(
        text = text.uppercase(),
        style = AaaType.Overline.copy(color = color, fontSize = 9.sp),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier
    )
}

/** Etykieta znaczeniowa z pionową kreską (szkoła, status, żywioł). */
@Composable
fun EdgeTag(text: String, color: Color, modifier: Modifier = Modifier) {
    Text(
        text = text.uppercase(),
        style = AaaType.Overline.copy(color = color, fontSize = 9.sp, letterSpacing = 1.6.sp),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier
            .background(color.copy(alpha = 0.14f))
            .drawBehind { drawRect(color, size = Size(2.dp.toPx(), size.height)) }
            .padding(start = 9.dp, end = 8.dp, top = 3.dp, bottom = 3.dp)
    )
}

@Composable
fun StatTriplet(items: List<Pair<String, String>>, modifier: Modifier = Modifier) {
    Row(
        modifier
            .fillMaxWidth()
            .drawBehind {
                val c = Aaa.Gold.copy(alpha = 0.18f)
                drawLine(c, Offset(0f, 0f), Offset(size.width, 0f), 1.dp.toPx())
                drawLine(c, Offset(0f, size.height), Offset(size.width, size.height), 1.dp.toPx())
            }
            .padding(vertical = 8.dp)
    ) {
        items.forEachIndexed { i, (k, v) ->
            Column(
                modifier = Modifier
                    .weight(1f)
                    .drawBehind {
                        if (i > 0) drawLine(Aaa.Gold.copy(alpha = 0.12f), Offset(0f, 0f), Offset(0f, size.height), 1.dp.toPx())
                    },
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(v, style = AaaType.Numeric.copy(fontSize = 17.sp), maxLines = 1)
                Text(k.uppercase(), style = AaaType.Overline.copy(fontSize = 8.5.sp, color = Aaa.TextLow, letterSpacing = 1.4.sp))
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Rama modułu
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Pełnoekranowa rama modułu: tło (grafika), poświata akcentu, wielki znak pędzlem,
 * gradienty czytelności, górny pasek (powrót, tytuł, zasób). Treść rysuje moduł.
 */
@Composable
fun ModuleScaffold(
    module: GameModule,
    subtitle: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    accent: Color = Aaa.Gold,
    glyph: String = module.glyph,
    glyphAlpha: Float = 0.12f,
    titleColor: Color = Aaa.GoldHi,
    bottomShade: Float = 0.56f,
    background: @Composable BoxScope.() -> Unit,
    topRight: @Composable RowScope.() -> Unit = {},
    content: @Composable BoxScope.() -> Unit
) {
    val animAccent by androidx.compose.animation.animateColorAsState(accent, tween(500), label = "mod_accent")
    Box(modifier.fillMaxSize().background(Aaa.Ink0)) {
        background()
        Box(
            Modifier
                .matchParentSize()
                .drawBehind {
                    drawRect(
                        Brush.radialGradient(
                            listOf(animAccent.copy(alpha = 0.22f), Color.Transparent),
                            center = Offset(size.width * 0.7f, size.height * 0.4f),
                            radius = size.maxDimension * 0.6f
                        )
                    )
                }
        )
        Text(
            text = glyph,
            fontFamily = BrushFont,
            fontSize = 240.sp,
            color = animAccent.copy(alpha = glyphAlpha),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = 18.dp, y = 60.dp)
        )
        Box(
            Modifier
                .matchParentSize()
                .drawBehind {
                    drawRect(Brush.verticalGradient(listOf(Aaa.Ink0.copy(alpha = 0.78f), Color.Transparent), endY = size.height * 0.18f))
                    drawRect(
                        Brush.verticalGradient(
                            0f to Color.Transparent,
                            bottomShade to Color.Transparent,
                            (bottomShade + 0.24f).coerceAtMost(0.95f) to Aaa.Ink0.copy(alpha = 0.9f),
                            1f to Aaa.Ink0.copy(alpha = 0.97f)
                        )
                    )
                }
        )
        content()
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .safeTop()
                .padding(start = 12.dp, end = 12.dp, top = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(Aaa.Ink0.copy(alpha = 0.6f))
                    .drawBehind { drawCircle(Aaa.GoldDeep, radius = size.minDimension / 2f - 0.75.dp.toPx(), style = Stroke(1.5.dp.toPx())) }
                    .clickable(role = Role.Button, onClick = onBack)
                    .testTag("module_back"),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.ArrowBack, contentDescription = "Wróć do sekty", tint = Aaa.GoldHi, modifier = Modifier.size(19.dp))
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = module.title.uppercase(),
                    style = AaaType.Title.copy(fontSize = 16.sp, letterSpacing = 2.2.sp, color = titleColor),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = subtitle,
                    style = AaaType.Label.copy(fontSize = 9.5.sp, color = Aaa.TextMid, letterSpacing = 0.2.sp),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically, content = topRight)
        }
    }
}

/** Dolny arkusz informacji modułu (na gradiencie, bez ramki). */
@Composable
fun BoxScope.ModuleSheet(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .align(Alignment.BottomCenter)
            .fillMaxWidth()
            .safeBottom()
            .padding(start = 14.dp, end = 14.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(9.dp),
        content = content
    )
}

@Composable
fun SheetTitle(text: String, modifier: Modifier = Modifier, size: Int = 25) {
    Text(
        text = text,
        style = AaaType.Display.copy(fontSize = size.sp, lineHeight = (size + 3).sp),
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier
    )
}

@Composable
fun SheetBody(text: String, modifier: Modifier = Modifier, color: Color = Aaa.TextMid, maxLines: Int = 3) {
    Text(
        text = text,
        style = AaaType.Body.copy(fontSize = 11.5.sp, lineHeight = 16.5.sp, color = color),
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier
    )
}

// ─────────────────────────────────────────────────────────────────────────────
//  Celebracja: błysk + napis (przełom, wytop, wykucie…)
// ─────────────────────────────────────────────────────────────────────────────

@Stable
class CelebrationState {
    var text by mutableStateOf("")
        private set
    var tick by mutableIntStateOf(0)
        private set
    var flash by mutableStateOf(true)
        private set

    fun fire(text: String, withFlash: Boolean = true) {
        this.text = text
        flash = withFlash
        tick++
    }
}

@Composable
fun rememberCelebration(): CelebrationState = remember { CelebrationState() }

@Composable
fun BoxScope.CelebrationOverlay(state: CelebrationState, flashColor: Color = Aaa.GoldHi) {
    val flash = remember { Animatable(0f) }
    val pop = remember { Animatable(0f) }
    LaunchedEffect(state.tick) {
        if (state.tick == 0) return@LaunchedEffect
        pop.snapTo(0f)
        if (state.flash) {
            flash.snapTo(1f)
            launch { flash.animateTo(0f, tween(850, easing = LinearOutSlowInEasing)) }
        }
        pop.animateTo(1f, tween(1700, easing = LinearEasing))
    }
    Box(
        Modifier
            .matchParentSize()
            .drawBehind {
                val a = flash.value
                if (a > 0f) {
                    drawRect(
                        Brush.radialGradient(
                            listOf(flashColor.copy(alpha = 0.9f * a), flashColor.copy(alpha = 0.3f * a), Color.Transparent),
                            center = Offset(size.width / 2f, size.height * 0.42f),
                            radius = size.maxDimension * 0.6f
                        )
                    )
                }
            }
    )
    val p = pop.value
    if (p > 0f && p < 1f) {
        val alpha = when {
            p < 0.15f -> p / 0.15f
            p > 0.78f -> (1f - p) / 0.22f
            else -> 1f
        }
        val scale = if (p < 0.2f) 0.6f + 2.25f * p else 1f
        Text(
            text = state.text,
            style = AaaType.Display.copy(
                fontSize = 22.sp,
                color = Aaa.GoldHi,
                shadow = Shadow(Aaa.Gold, Offset.Zero, 22f)
            ),
            textAlign = TextAlign.Center,
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = 24.dp)
                .graphicsLayer {
                    this.alpha = alpha.coerceIn(0f, 1f)
                    scaleX = scale
                    scaleY = scale
                    translationY = -(p - 0.78f).coerceAtLeast(0f) * 60.dp.toPx() - 40.dp.toPx()
                }
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Księga – pełny dawny widok w panelu (żadna funkcja nie znika)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun LegacyPanel(
    title: String,
    onClose: () -> Unit,
    content: @Composable BoxScope.() -> Unit
) {
    Column(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Aaa.Ink2, Aaa.Ink0)))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {}
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .safeTop()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Overline("Księga sekty")
                Text(title, style = AaaType.Title, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Box(
                Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Aaa.Ink0.copy(alpha = 0.6f))
                    .clickable(role = Role.Button, onClick = onClose)
                    .testTag("legacy_close"),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.Close, contentDescription = "Zamknij księgę", tint = Aaa.GoldHi, modifier = Modifier.size(18.dp))
            }
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(Aaa.goldFade()))
        Box(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .safeBottom(),
            content = content
        )
    }
}
