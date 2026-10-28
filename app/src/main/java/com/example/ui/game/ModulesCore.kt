package com.example.ui.game

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.game.WuxiaGameViewModel
import com.example.game.WuxiaUiState
import com.example.model.Disciple
import com.example.model.DiscipleRole
import com.example.model.MartialSchool
import com.example.model.RandomEvent
import com.example.model.RandomEventChoice
import com.example.ui.aaa.Aaa
import com.example.ui.aaa.AaaGlyph
import com.example.ui.aaa.AaaGlyphIcon
import com.example.ui.aaa.AaaType
import com.example.ui.audio.WuxiaAudioEngine
import com.example.ui.graphics.WuxiaAssetLoader
import com.example.ui.screens.DisciplesScreen
import com.example.ui.screens.SectSanctuaryScreen
import kotlinx.coroutines.delay

internal fun schoolColor(s: MartialSchool): Color = when (s) {
    MartialSchool.SWORD_IMMORTAL -> Color(0xFF7FE3F7)
    MartialSchool.BODY_ASURA -> Color(0xFFFF6B5E)
    MartialSchool.ELEMENTAL_TAOIST -> Color(0xFFF0C85A)
    MartialSchool.SHADOW_VIPER -> Color(0xFFC48CFF)
}

internal fun schoolGlyph(s: MartialSchool): String = when (s) {
    MartialSchool.SWORD_IMMORTAL -> "剑"
    MartialSchool.BODY_ASURA -> "拳"
    MartialSchool.ELEMENTAL_TAOIST -> "雷"
    MartialSchool.SHADOW_VIPER -> "影"
}

internal const val PILL_BREAKTHROUGH = "Pigułka Przełomu Qi"

/** Pełnowymiarowa postać wtopiona w tło z lewej strony. */
@Composable
internal fun HeroArt(assetKey: String, modifier: Modifier = Modifier, fadeStart: Float = 0.30f) {
    ArtImage(
        assetKey = assetKey,
        alignment = Alignment.TopCenter,
        modifier = modifier
            .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
            .drawWithContent {
                drawContent()
                drawRect(
                    Brush.horizontalGradient(0f to Color.Transparent, fadeStart to Color.Black),
                    blendMode = BlendMode.DstIn
                )
            }
    )
}

// ─────────────────────────────────────────────────────────────────────────────
//  ADEPCI
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun AdeptsModule(ui: WuxiaUiState, viewModel: WuxiaGameViewModel, onBack: () -> Unit) {
    var selId by rememberSaveable { mutableStateOf(ui.disciples.firstOrNull()?.id) }
    val d: Disciple? = ui.disciples.firstOrNull { it.id == selId } ?: ui.disciples.firstOrNull()
    var legacy by remember { mutableStateOf<String?>(null) }
    val celebration = rememberCelebration()
    val ready = ui.disciples.count { it.realmProgress >= 100 }
    val pills = ui.inventory[PILL_BREAKTHROUGH] ?: 0

    // Świętuj tylko udany przełom (zmiana królestwa tego samego ucznia).
    if (d != null) {
        var lastRealm by remember(d.id) { mutableStateOf(d.realm) }
        LaunchedEffect(d.id, d.realm) {
            if (d.realm != lastRealm) {
                WuxiaAudioEngine.playUltimateBurst()
                celebration.fire(d.realm.polishName.substringBefore(" ("))
                lastRealm = d.realm
            }
        }
    }

    Box(Modifier.fillMaxSize()) {
        ModuleScaffold(
            module = GameModule.ADEPTS,
            subtitle = "${ui.disciples.size} adeptów · $ready gotowych · morale ${ui.morale}",
            onBack = onBack,
            accent = d?.let { schoolColor(it.martialSchool) } ?: Aaa.Gold,
            glyph = d?.let { schoolGlyph(it.martialSchool) } ?: GameModule.ADEPTS.glyph,
            background = {
                ArtImage("bg_combat_arena", Modifier.matchParentSize(), colorFilter = dimFilter(0.34f, 0.7f))
            },
            topRight = { CurrencyPill(AaaGlyph.SPIRIT_STONE, ui.spiritStones) }
        ) {
            if (d != null) {
                AnimatedContent(
                    targetState = d.id,
                    transitionSpec = {
                        (fadeIn(tween(320)) + slideInHorizontally(tween(420)) { it / 10 }) togetherWith fadeOut(tween(200))
                    },
                    label = "adept_hero",
                    modifier = Modifier.matchParentSize()
                ) { id ->
                    val who = ui.disciples.firstOrNull { it.id == id } ?: d
                    Box(Modifier.fillMaxSize()) {
                        HeroArt(
                            WuxiaAssetLoader.getSchoolAvatarKey(who.martialSchool),
                            Modifier
                                .align(Alignment.TopEnd)
                                .padding(top = 44.dp)
                                .offset(x = 36.dp)
                                .fillMaxHeight(0.84f)
                                .aspectRatio(768f / 1376f)
                        )
                    }
                }

                // Pasek uczniów
                Column(
                    modifier = Modifier
                        .safeTop()
                        .padding(start = 10.dp, top = 74.dp)
                        .width(70.dp)
                        .fillMaxHeight(0.52f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ui.disciples.forEach { x ->
                        RosterTile(
                            disciple = x,
                            selected = x.id == d.id,
                            onClick = { selId = x.id; WuxiaAudioEngine.playSwordClash() }
                        )
                    }
                    Box(
                        Modifier
                            .size(52.dp)
                            .clip(cutCornerTicket())
                            .background(Aaa.Ink0.copy(alpha = 0.6f))
                            .clickable(role = Role.Button) { legacy = "exam" }
                            .testTag("adepts_recruit"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("+", color = Aaa.Gold, fontSize = 26.sp)
                    }
                }

                ModuleSheet {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        EdgeTag(d.martialSchool.polishName, schoolColor(d.martialSchool))
                        Text(
                            "Talent ${d.talentGrade.code}",
                            style = AaaType.Overline.copy(color = Aaa.Gold, fontSize = 9.sp)
                        )
                    }
                    SheetTitle(d.name.substringBefore(" ("), size = 28)
                    SheetBody("${com.example.game.engine.Realms.label(d)} · ${d.role.polishName}", maxLines = 1, modifier = Modifier.offset(y = (-4).dp))
                    StatTriplet(
                        listOf(
                            "Atak" to "${d.attack}",
                            "Obrona" to "${d.defense}",
                            "Moc" to "${d.combatPower}"
                        )
                    )
                    val isReady = d.realmProgress >= 100
                    KvRow(
                        label = "Qi meridianów ${d.realmProgress}%",
                        value = if (isReady) "Gotowy do przełomu" else d.innateTechnique,
                        valueColor = if (isReady) Aaa.Gold else Aaa.TextMid
                    )
                    ThinBar(d.realmProgress / 100f, schoolColor(d.martialSchool))
                    DiscipleLifeInfo(d)
                    DiscipleStateTags(d, ui)
                    if (ui.autonomy) DiscipleMindStrip(d, ui) { legacy = "mind" }
                    // Rozwój ucznia: trzy panele (kultywacja, Dao, życie) – stałe miejsce, zawsze widoczne.
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        GhostButton("Kultywacja", onClick = { legacy = "cultivation" }, modifier = Modifier.weight(1.2f), height = 36.dp)
                        GhostButton("Dao" + if (d.openedMeridians.isNotEmpty()) " ${d.openedMeridians.size}" else "", onClick = { legacy = "secrets" }, modifier = Modifier.weight(1f), height = 36.dp)
                        GhostButton("Życie", onClick = { legacy = "life" }, modifier = Modifier.weight(1f), height = 36.dp)
                        GhostButton("Księga", onClick = { legacy = "disciples" }, modifier = Modifier.weight(1f), height = 36.dp)
                    }
                    // Etap 11: ekwipunek i więzi.
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        val worn = com.example.game.engine.Gear.items(d).size
                        GhostButton("Ekwipunek $worn/3", onClick = { legacy = "gear" }, modifier = Modifier.weight(1f), height = 32.dp)
                        val ties = ui.bonds.count { it.has(d.id) && it.kind != com.example.model.BondKind.NEUTRAL }
                        GhostButton(if (ui.autonomy) "Mistrz i więzi" + if (ties > 0) " · $ties" else "" else "Więzi", onClick = { legacy = "mind" }, modifier = Modifier.weight(1f), height = 32.dp)
                    }
                    // Dyscyplina: ranga, kara, łaska.
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        GhostButton(
                            text = if (d.id in ui.promotableIds) "Awans ↑" else d.rank.polishName.substringAfter(" "),
                            enabled = d.id in ui.promotableIds,
                            onClick = { viewModel.promote(d.id) },
                            modifier = Modifier.weight(1.2f),
                            height = 32.dp
                        )
                        GhostButton("Kara", onClick = { viewModel.punish(d.id) }, danger = true, modifier = Modifier.weight(1f), height = 32.dp)
                        GhostButton("Łaska", onClick = { viewModel.pardon(d.id) }, modifier = Modifier.weight(1f), height = 32.dp)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        GhostButton(
                            text = if (ui.autonomy) "Doradź" else "Rola",
                            onClick = {
                                if (ui.autonomy) legacy = "mind" else {
                                    val roles = DiscipleRole.values()
                                    viewModel.assignRole(d.id, roles[(d.role.ordinal + 1) % roles.size])
                                }
                            },
                            modifier = Modifier.weight(1f)
                        )
                        val needsPanel = com.example.game.engine.Realms.atPeak(d) &&
                            (d.id in ui.bottlenecks || (d.majorRealm?.next?.tribulationStrikes ?: 0) > 0)
                        if (isReady && needsPanel) {
                            GoldCta(
                                text = "Wielki przełom",
                                onClick = { legacy = "cultivation" },
                                modifier = Modifier.weight(1.6f),
                                testTag = "adepts_major"
                            )
                        } else if (isReady) {
                            GoldCta(
                                text = if (ui.autonomy) "Doradź przełom" else "Przełom",
                                trailing = if (pills > 0) "+pigułka" else null,
                                onClick = {
                                    WuxiaAudioEngine.playThunderStrike()
                                    viewModel.attemptBreakthrough(d.id, usePill = pills > 0 && ui.autoUsePillsForBreakthrough)
                                },
                                modifier = Modifier.weight(1.6f),
                                testTag = "adepts_breakthrough"
                            )
                        } else {
                            val meditating = d.role == DiscipleRole.MEDITATING
                            GoldCta(
                                text = if (meditating) "Medytuje" else if (ui.autonomy) "Doradź medytację" else "Medytacja",
                                enabled = !meditating,
                                onClick = { viewModel.assignRole(d.id, DiscipleRole.MEDITATING) },
                                modifier = Modifier.weight(1.6f),
                                testTag = "adepts_meditate"
                            )
                        }
                    }
                }
            } else {
                ModuleSheet {
                    SheetTitle("Pawilon jest pusty")
                    SheetBody("Przyjmij pierwszych adeptów w Pałacu Sekty.")
                    GoldCta("Nabór", onClick = { legacy = "palace" }, modifier = Modifier.fillMaxWidth())
                }
            }
            CelebrationOverlay(celebration)
        }

        when (legacy) {
            "disciples" -> LegacyPanel("Sala Adeptów", onClose = { legacy = null }) {
                DisciplesScreen(viewModel = viewModel, uiState = ui)
            }
            "palace" -> LegacyPanel("Pałac Sekty · Nabór", onClose = { legacy = null }) {
                SectSanctuaryScreen(viewModel = viewModel, uiState = ui, onNavigateToDisciples = { legacy = null })
            }
            "exam" -> ExamPanel(ui, viewModel, onClose = { legacy = null })
            "cultivation" -> if (d != null) CultivationPanel(d, ui, viewModel, onClose = { legacy = null })
            "secrets" -> if (d != null) SecretsPanel(d, ui, viewModel, onClose = { legacy = null })
            "life" -> if (d != null) LifePanel(d, ui, viewModel, onClose = { legacy = null })
            "mind" -> if (d != null) MindPanel(d, ui, viewModel, onClose = { legacy = null })
            "gear" -> if (d != null) GearPanel(d, ui, viewModel, onClose = { legacy = null })
        }
    }
}

@Composable
private fun RosterTile(disciple: Disciple, selected: Boolean, onClick: () -> Unit) {
    val c = schoolColor(disciple.martialSchool)
    Box(
        Modifier
            .offset(x = if (selected) 4.dp else 0.dp)
            .size(56.dp)
    ) {
        Box(
            Modifier
                .size(54.dp)
                .clip(cutCornerTicket())
                .background(c.copy(alpha = if (selected) 1f else 0.45f))
                .padding(2.dp)
                .clip(cutCornerTicket())
                .clickable(role = Role.Button, onClick = onClick)
                .testTag("adept_tile_${disciple.id}")
        ) {
            ArtImage(
                WuxiaAssetLoader.getSchoolAvatarKey(disciple.martialSchool),
                Modifier.matchParentSize(),
                alignment = BiasAlignment(0f, -0.62f),
                colorFilter = if (selected) null else dimFilter(0.62f, 0.55f)
            )
            Text(
                text = disciple.name.substringBefore(" (").substringBefore(" "),
                style = AaaType.Label.copy(fontSize = 8.sp, color = Aaa.TextHi, letterSpacing = 0.sp),
                maxLines = 1,
                overflow = TextOverflow.Clip,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(Brush.verticalGradient(listOf(Color.Transparent, Aaa.Ink0.copy(alpha = 0.85f))))
                    .padding(horizontal = 3.dp, vertical = 1.dp)
            )
        }
        if (disciple.realmProgress >= 100) {
            Box(Modifier.align(Alignment.TopEnd).offset(x = 2.dp, y = (-4).dp)) { RedDot("⚡") }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  PAŁAC SEKTY – zarządzanie (pawilony, nabór, dekrety): pełny dotychczasowy widok
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun PalaceModule(ui: WuxiaUiState, viewModel: WuxiaGameViewModel, onBack: () -> Unit) {
    var eco by remember { mutableStateOf<EconomyTab?>(null) }
    var sect by remember { mutableStateOf<SectTab?>(null) }
    var metaOpen by remember { mutableStateOf<MetaTab?>(null) }
    var journal by remember { mutableStateOf(false) }
    var e11 by remember { mutableStateOf<String?>(null) }
    ConsumeShortcut(GameModule.PALACE) { sc ->
        when (sc) {
            Shortcut.WEEKLY_GOAL -> e11 = "goal"
            Shortcut.OPPORTUNITY -> e11 = "opportunity"
            Shortcut.MARKET -> eco = EconomyTab.MARKET
            Shortcut.CONTRACTS -> eco = EconomyTab.CONTRACTS
            Shortcut.VEIN -> eco = EconomyTab.VEIN
            Shortcut.SECT -> sect = SectTab.IDENTITY
            Shortcut.SECT_CHRONICLE -> sect = SectTab.CHRONICLE
            Shortcut.META -> metaOpen = MetaTab.REBIRTH
            Shortcut.META_DAILY -> metaOpen = MetaTab.DAILY
            Shortcut.JOURNAL -> journal = true
            else -> Unit
        }
    }
    ModuleScaffold(
        module = GameModule.PALACE,
        subtitle = "${ui.sectLeader.name} · prestiż ${ui.sectLeader.prestigeLevel}",
        onBack = onBack,
        glyphAlpha = 0.07f,
        bottomShade = 0.9f,
        background = {
            ArtImage("bg_sect_sanctuary", Modifier.matchParentSize(), colorFilter = dimFilter(0.28f, 0.6f))
        },
        topRight = { CurrencyPill(AaaGlyph.SPIRIT_STONE, ui.spiritStones) }
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .safeTop()
                .padding(top = 62.dp)
                .safeBottom()
        ) {
            Column(Modifier.fillMaxSize()) {
                if (ui.economy.enabled) PalaceTiles(ui,
                    onEco = { eco = it }, onSect = { sect = it }, onMeta = { metaOpen = it }, onJournal = { journal = true })
                Box(Modifier.padding(horizontal = 12.dp, vertical = 2.dp)) { Stage11Tiles(ui) { e11 = it } }
                Box(Modifier.weight(1f)) {
                    SectSanctuaryScreen(viewModel = viewModel, uiState = ui, onNavigateToDisciples = onBack)
                }
            }
        }
    }
    eco?.let { EconomyPanel(ui, viewModel, it, onClose = { eco = null }) }
    sect?.let { SectPanel(ui, viewModel, it, onClose = { sect = null }) }
    metaOpen?.let { MetaPanel(ui, viewModel, it, onClose = { metaOpen = null }) }
    if (journal) JournalPanel(ui, onClose = { journal = false })
    when (e11) {
        "goal" -> WeeklyGoalPanel(ui, onClose = { e11 = null })
        "opportunity" -> OpportunityPanel(ui, viewModel, onClose = { e11 = null })
        "steward" -> StewardPanel(ui, viewModel, onClose = { e11 = null })
    }
}

/** Zarząd sekty: 2 rzędy kafli z najważniejszą liczbą każdego systemu i odznaką, gdy coś czeka. */
@Composable
private fun PalaceTiles(
    ui: WuxiaUiState,
    onEco: (EconomyTab) -> Unit,
    onSect: (SectTab) -> Unit,
    onMeta: (MetaTab) -> Unit,
    onJournal: () -> Unit
) {
    val e = ui.economy
    val deliverable = e.contracts.count { it.type == com.example.model.ContractType.DELIVER && (ui.inventory[it.item] ?: 0) >= it.quantity }
    val goals = ui.meta.goals
    val goalsLeft = goals.count { !it.done }
    Column(Modifier.padding(horizontal = 12.dp, vertical = 4.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            ActionTile("脉", "Żyła", "${e.veinName} · ${e.veinLevel}/9", Modifier.weight(1f), accent = Aaa.Azure,
                badge = if (e.veinUpgradeDoneDay > 0) "…" else null) { onEco(EconomyTab.VEIN) }
            ActionTile("约", "Kontrakty", "${e.contracts.size} na tablicy", Modifier.weight(1f),
                badge = deliverable.takeIf { it > 0 }?.toString()) { onEco(EconomyTab.CONTRACTS) }
            ActionTile("市", "Targ", if (e.blackOpen) "czarny rynek otwarty" else "${e.season.glyph} ${e.season.polishName}", Modifier.weight(1f),
                accent = if (e.blackOpen) Aaa.Violet else Aaa.Gold) { onEco(EconomyTab.MARKET) }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            ActionTile("宗", "Sekta", ui.sectLife.karmaPath.ifEmpty { "Tożsamość" }, Modifier.weight(1f), accent = Aaa.Jade,
                badge = ui.disciples.count { it.illUntilDay >= ui.currentDay || it.cursed }.takeIf { it > 0 }?.toString()) { onSect(SectTab.IDENTITY) }
            ActionTile("天", "Niebiosa", "Era ${ui.meta.era} · ${ui.meta.essence} Esencji", Modifier.weight(1f), accent = Aaa.GoldHi,
                badge = when {
                    ui.rebirthAvailable -> "🌅"
                    goalsLeft > 0 -> "$goalsLeft"
                    else -> null
                }) { onMeta(if (ui.rebirthAvailable || goals.isEmpty() || goalsLeft == 0) MetaTab.REBIRTH else MetaTab.DAILY) }
            ActionTile("秘", "Tajemnice", "${ui.secrets.discovered}/${ui.secrets.total}", Modifier.weight(1f), accent = Aaa.Violet) { onJournal() }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  OMEN NIEBIOS – zdarzenie losowe jako pełny kadr
// ─────────────────────────────────────────────────────────────────────────────

private fun eventArt(e: RandomEvent?): String = when {
    e == null -> "bg_sect_sanctuary"
    e.title.contains("Demoniczn", true) || e.title.contains("Krwi", true) -> "avatar_shadow_viper"
    e.title.contains("Besti", true) || e.title.contains("Smok", true) -> "beast_azure_dragon"
    e.title.contains("Mistrz", true) || e.title.contains("Uzdrowiciel", true) -> "avatar_elemental_taoist"
    e.title.contains("Meteoryt", true) || e.title.contains("Grobowc", true) -> "bg_combat_arena"
    else -> "bg_combat_arena"
}

private fun isRuthless(c: RandomEventChoice) =
    listOf("Zabierz", "Siłą", "Zniszcz", "Atak", "opłat", "Odpraw").any { c.text.contains(it, ignoreCase = true) }

@Composable
fun OmenModule(ui: WuxiaUiState, viewModel: WuxiaGameViewModel, onBack: () -> Unit) {
    val event = ui.activeRandomEvent
    var outcome by remember { mutableStateOf<String?>(null) }
    val celebration = rememberCelebration()
    // Zachowaj ostatnie zdarzenie, by po wyborze pokazać jego skutek (ViewModel czyści je od razu).
    val lastEvent = remember { arrayOfNulls<RandomEvent>(1) }
    if (event != null) lastEvent[0] = event
    val e = event ?: lastEvent[0]?.takeIf { outcome != null }

    LaunchedEffect(outcome) {
        if (outcome != null) {
            delay(2600)
            onBack()
        }
    }

    ModuleScaffold(
        module = GameModule.OMEN,
        subtitle = if (e != null) "Przeznaczenie czeka na decyzję" else "Niebo milczy",
        onBack = onBack,
        accent = Aaa.Cinnabar,
        titleColor = Color(0xFFFF8A73),
        glyphAlpha = 0.16f,
        bottomShade = 0.36f,
        background = {
            ArtImage(
                eventArt(e),
                Modifier.fillMaxWidth().fillMaxHeight(0.62f),
                alignment = BiasAlignment(0f, -0.3f),
                colorFilter = dimFilter(0.78f)
            )
        }
    ) {
        ModuleSheet {
            if (e == null) {
                SheetTitle("Niebo milczy")
                SheetBody("Omen pojawi się, gdy los przyniesie sekcie zdarzenie. Czerwony znak 命 na scenie da ci znać.")
                GhostButton("Wróć do sekty", onClick = onBack, modifier = Modifier.fillMaxWidth())
            } else {
                Overline("Omen niebios", color = Aaa.Cinnabar)
                SheetTitle(e.title, size = 23)
                SheetBody(e.description, maxLines = 4, color = Aaa.TextHi.copy(alpha = 0.9f))
                Overline("Wybierz ścieżkę", modifier = Modifier.align(Alignment.CenterHorizontally))
                e.choices.forEachIndexed { i, c ->
                    ChoiceRow(
                        choice = c,
                        enabled = outcome == null,
                        dimmed = outcome != null,
                        onClick = {
                            if (isRuthless(c)) WuxiaAudioEngine.playSwordClash() else WuxiaAudioEngine.playDivineGong()
                            viewModel.handleRandomEventChoice(i)
                            outcome = c.outcomeDescription
                            celebration.fire("Przeznaczenie wybrane")
                        },
                        modifier = Modifier.testTag("event_choice_$i")
                    )
                }
                val o = outcome
                if (o != null) {
                    SheetBody(o, color = Aaa.GoldHi)
                } else {
                    Text(
                        "Zignoruj omen",
                        style = AaaType.Label.copy(color = Aaa.TextLow),
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .clickable(role = Role.Button) {
                                viewModel.dismissRandomEvent()
                                onBack()
                            }
                            .padding(6.dp)
                            .testTag("event_dismiss")
                    )
                }
            }
        }
        CelebrationOverlay(celebration, flashColor = Color(0xFFFFC2B0))
    }
}

@Composable
private fun ChoiceRow(
    choice: RandomEventChoice,
    enabled: Boolean,
    dimmed: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val ruthless = isRuthless(choice)
    val a = if (ruthless) Aaa.Cinnabar else Aaa.Jade
    Row(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer { alpha = if (dimmed) 0.4f else 1f }
            .background(Brush.horizontalGradient(listOf(a.copy(alpha = 0.22f), Aaa.Ink0.copy(alpha = 0.6f))))
            .drawBehind { drawRect(a, size = Size(2.dp.toPx(), size.height)) }
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(start = 12.dp, end = 10.dp, top = 9.dp, bottom = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(Brush.radialGradient(listOf(a, Color(0xFF0A0708)))),
            contentAlignment = Alignment.Center
        ) {
            Text(if (ruthless) "血" else "道", fontFamily = BrushFont, fontSize = 20.sp, color = Aaa.GoldHi)
        }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(choice.text, style = AaaType.Body.copy(fontWeight = FontWeight.SemiBold, fontSize = 12.sp, lineHeight = 16.sp))
            val bits = buildList {
                if (choice.stoneChange != 0) add(Triple(AaaGlyph.SPIRIT_STONE, choice.stoneChange, null as String?))
                if (choice.repChange != 0) add(Triple(AaaGlyph.REPUTATION, choice.repChange, null as String?))
            }
            if (bits.isNotEmpty() || choice.rewardItem != null) {
                Spacer(Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    bits.forEach { (g, v, _) ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            AaaGlyphIcon(g, Modifier.size(12.dp))
                            Spacer(Modifier.width(3.dp))
                            Text(
                                if (v > 0) "+$v" else "−${-v}",
                                style = AaaType.Numeric.copy(fontSize = 10.sp, color = if (v > 0) Aaa.Jade else Aaa.Cinnabar)
                            )
                        }
                    }
                    choice.rewardItem?.let {
                        Text(it, style = AaaType.Label.copy(fontSize = 10.sp, color = Aaa.Gold), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
    }
}

/** Znaczniki stanu ucznia: Wybraniec, zdrowie, ukryty stan, płomień, mistrz, wiek, wierzchowiec. */
@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun DiscipleStateTags(d: Disciple, ui: WuxiaUiState) {
    val life = ui.sectLife
    val span = life.lifespans[d.id] ?: 0
    val tags = buildList {
        if (d.id == life.chosenId) add("✋ Wybraniec" to Aaa.GoldHi)
        if (d.illUntilDay >= ui.currentDay) add("🤒 Gorączka" to Aaa.Ember)
        if (d.cursed) add("☠ Klątwa" to Aaa.Violet)
        d.secretState?.let { add(it.polishName to Aaa.Violet) }
        d.flameId?.let { id -> ui.flames.find { it.id == id }?.let { f -> add("🜂 ${shortName(f.name, 2)}" to Aaa.Ember) } }
        ui.disciples.find { it.id == d.mentorId }?.let { add("Mistrz: ${it.name.substringBefore(" (").substringBefore(" ")}" to Aaa.Jade) }
        if (life.mounts.containsValue(d.id)) add("🐉 Jeździec" to Aaa.Jade)
        if (ui.gates.containsValue(d.id)) add("🛡 Brama" to Aaa.Gold)
        if (ui.spies.any { it.discipleId == d.id }) add("🕵 Na misji" to Aaa.Violet)
        if (span > 0 && span - d.ageYears <= 5) add("⏳ ${d.ageYears}/$span lat" to Aaa.Cinnabar)
    }
    if (tags.isEmpty()) return
    androidx.compose.foundation.layout.FlowRow(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
        tags.forEach { (t, c) -> StateTag(t, c) }
    }
}
