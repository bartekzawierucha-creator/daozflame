package com.example.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.game.WuxiaGameViewModel
import com.example.game.WuxiaUiState
import com.example.model.*
import com.example.ui.components.QiProgressBar
import com.example.ui.components.WuxiaBadge
import com.example.ui.components.WuxiaCard
import com.example.ui.graphics.*
import com.example.ui.pixelart.*
import com.example.ui.theme.*
import com.example.ui.aaa.*
import com.example.ui.audio.WuxiaAudioEngine
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.animation.core.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.drawscope.Stroke

@Composable
fun DisciplesScreen(
    viewModel: WuxiaGameViewModel,
    uiState: WuxiaUiState,
    modifier: Modifier = Modifier
) {
    var selectedSchoolFilter by remember { mutableStateOf<MartialSchool?>(null) }
    var selectedDiscipleForDetail by remember { mutableStateOf<Disciple?>(null) }

    // Keep detail disciple in sync with uiState
    val currentDetailDisciple = selectedDiscipleForDetail?.let { current ->
        uiState.disciples.find { it.id == current.id }
    }

    val filteredDisciples = if (selectedSchoolFilter == null) {
        uiState.disciples
    } else {
        uiState.disciples.filter { it.martialSchool == selectedSchoolFilter }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(top = 10.dp, bottom = 24.dp)
    ) {
        // Hero Banner
        item {
            ManhwaHeroBanner(
                assetName = "bg_sect_sanctuary",
                title = "Komnata Mistrzów i Adeptów Sekty",
                subtitle = "Przeglądaj talenty, rozwijaj meridiany Dantian i przeprowadzaj próby Niebiańskiej Kary",
                height = 140.dp
            )
        }

        // Filter Row by Martial School
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Adeptowie Sekty (${uiState.disciples.size})",
                    color = Color(0xFFF6E6BE),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Black
                )
                Text(
                    text = "Filtruj według Szkoły",
                    color = Color(0xFFB0A592),
                    fontSize = 11.sp
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    val isAll = selectedSchoolFilter == null
                    Surface(
                        color = if (isAll) Color(0xFF9A6B2C) else Color(0xFF191124),
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, if (isAll) Color(0xFFE8C37E) else Color(0xFF3A2E35)),
                        modifier = Modifier.clickable { selectedSchoolFilter = null }
                    ) {
                        Text(
                            text = "☯️ Wszyscy",
                            color = if (isAll) Color(0xFFF3DDA6) else Color(0xFFD6CCB6),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }
                items(MartialSchool.values()) { school ->
                    val isSel = selectedSchoolFilter == school
                    val (icon, col) = when (school) {
                        MartialSchool.SWORD_IMMORTAL -> "🗡️" to Color(0xFF00E5FF)
                        MartialSchool.BODY_ASURA -> "👊" to Color(0xFFFF5252)
                        MartialSchool.ELEMENTAL_TAOIST -> "⚡" to Color(0xFFEBCB7A)
                        MartialSchool.SHADOW_VIPER -> "🐍" to Color(0xFFE040FB)
                    }
                    Surface(
                        color = if (isSel) col.copy(alpha = 0.25f) else Color(0xFF191124),
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, if (isSel) col else Color(0xFF3A2E35)),
                        modifier = Modifier.clickable {
                            selectedSchoolFilter = if (selectedSchoolFilter == school) null else school
                        }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = icon, fontSize = 11.sp)
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = school.polishName,
                                color = if (isSel) col else Color(0xFFD6CCB6),
                                fontSize = 11.sp,
                                fontWeight = if (isSel) FontWeight.Black else FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }

        // Auto-Breakthrough Control Panel
        item {
            val readyDisciplesCount = uiState.disciples.count { it.realmProgress >= 100 }
            WuxiaCard(
                borderColor = if (readyDisciplesCount > 0) Color(0xFFE8C37E) else Color(0xFF3A2E35),
                backgroundColor = Color(0xFF16111A),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "⚡ AUTOMATYCZNY PRZEŁOM DAO",
                                color = Color(0xFFF6E6BE),
                                fontWeight = FontWeight.Black,
                                fontSize = 13.sp
                            )
                        }
                        Text(
                            text = if (uiState.autonomy) "Wielkie przełomy: uczniowie decydują sami – możesz doradzić" else if (uiState.autoBreakthroughEnabled) "Uczniowie przy 100% Qi automatycznie przechodzą próbę" else "Wstrzymano próby przełomu (wymagany ręczny rytuał)",
                            color = Color(0xFFB0A592),
                            fontSize = 11.sp
                        )
                    }

                    Switch(
                        checked = uiState.autoBreakthroughEnabled,
                        onCheckedChange = { viewModel.toggleAutoBreakthrough(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color(0xFFE8C37E),
                            checkedTrackColor = Color(0xFF9A6B2C)
                        ),
                        modifier = Modifier.testTag("toggle_auto_breakthrough")
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "💊 Automatycznie zużywaj Pigułki Qi (+35% sukcesu):",
                        color = Color(0xFFEDE4CF),
                        fontSize = 11.sp
                    )
                    Switch(
                        checked = uiState.autoUsePillsForBreakthrough,
                        onCheckedChange = { viewModel.toggleAutoUsePills(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color(0xFF10B981),
                            checkedTrackColor = Color(0xFF065F46)
                        ),
                        modifier = Modifier.testTag("toggle_auto_use_pills")
                    )
                }

                if (readyDisciplesCount > 0) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = {
                            WuxiaAudioEngine.playDivineGong()
                            viewModel.breakthroughAllReadyDisciples()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("breakthrough_all_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF9A6B2C)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "⚡ PRZEŁAM WSZYSTKICH GOTOWYCH ($readyDisciplesCount ADEPTÓW)",
                            color = Color.White,
                            fontWeight = FontWeight.Black,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }

        // Disciple Cards
        items(filteredDisciples) { disciple ->
            DiscipleCard(
                disciple = disciple,
                onClick = { selectedDiscipleForDetail = disciple },
                onQuickBreakthrough = {
                    WuxiaAudioEngine.playThunderStrike()
                    viewModel.attemptBreakthrough(disciple.id, usePill = uiState.autoUsePillsForBreakthrough)
                }
            )
        }
    }

    // Detail Modal Sheet
    if (currentDetailDisciple != null) {
        DiscipleDetailModal(
            disciple = currentDetailDisciple,
            uiState = uiState,
            viewModel = viewModel,
            onDismiss = { selectedDiscipleForDetail = null }
        )
    }
}

@Composable
fun DiscipleCard(
    disciple: Disciple,
    onClick: () -> Unit,
    onQuickBreakthrough: () -> Unit = {}
) {
    val isReadyForBreakthrough = disciple.realmProgress >= 100

    WuxiaCard(
        borderColor = if (isReadyForBreakthrough) Color(0xFFE8C37E) else Color(0xFF332042),
        backgroundColor = Color(0xFF16111A),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("disciple_card_${disciple.id}")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // High-Res Manhwa Standee Avatar with Elemental Aura
            ManhwaSchoolAvatar(
                school = disciple.martialSchool,
                size = 64.dp,
                showAura = true
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                // Name + Talent Grade + Realm Badge
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = disciple.name,
                            color = Color(0xFFF6E6BE),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Black
                        )
                        Spacer(Modifier.width(6.dp))
                        val (gradeColor, gradeBg) = when (disciple.talentGrade) {
                            TalentGrade.SSS -> Color(0xFFE8C37E) to Color(0xFF78350F)
                            TalentGrade.SS -> Color(0xFFF43F5E) to Color(0xFF881337)
                            TalentGrade.S -> Color(0xFF00E5FF) to Color(0xFF164E63)
                            TalentGrade.A -> Color(0xFF34D399) to Color(0xFF064E3B)
                            TalentGrade.B -> Color(0xFFA78BFA) to Color(0xFF3B0764)
                        }
                        Surface(
                            color = gradeBg,
                            shape = RoundedCornerShape(4.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, gradeColor)
                        ) {
                            Text(
                                text = disciple.talentGrade.name,
                                color = gradeColor,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Black,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                    }
                    Surface(
                        color = if (isReadyForBreakthrough) Color(0xFF831843) else Color(0xFF2E1065),
                        shape = RoundedCornerShape(6.dp),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isReadyForBreakthrough) Color(0xFFE8C37E) else Color(0xFFA855F7)
                        )
                    ) {
                        Text(
                            text = disciple.realm.polishName,
                            color = if (isReadyForBreakthrough) Color(0xFFE8C37E) else Color(0xFFE9D5FF),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Text(
                    text = "🏰 ${disciple.ancientClan.name} • ${disciple.ancientClan.reputationTitle}",
                    color = Color(0xFFEBCB7A),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${disciple.martialSchool.polishName} • Zadanie: ${disciple.role.polishName}",
                        color = Color(0xFF00E5FF),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "⚔️ ${disciple.combatPower} CP",
                        color = Color(0xFFE8C37E),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black
                    )
                }

                Spacer(modifier = Modifier.height(3.dp))

                // Bloodline and Constitution Badges
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Surface(
                        color = Color(0xFF450A0A),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = "🩸 ${disciple.bloodline.polishName}",
                            color = Color(0xFFFCA5A5),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                        )
                    }
                    Surface(
                        color = Color(0xFF083344),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = "✨ ${disciple.constitution.polishName}",
                            color = Color(0xFF67E8F9),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                        )
                    }
                }

                if (disciple.fatedTraits.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "✦ Cechy: ${disciple.fatedTraits.joinToString(" • ") { it.name }}",
                        color = Color(0xFFF2D58A),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                Text(
                    text = "🌀 Wrodzona: ${disciple.innateTechnique}",
                    color = Color(0xFFA5B4FC),
                    fontSize = 9.sp
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Stats preview chips
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "⚔️ Atak: ${disciple.attack}", color = Color(0xFFE8C37E), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Text(text = "🛡️ Obrona: ${disciple.defense}", color = Color(0xFF00E5FF), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Text(text = "✨ Dao: ${disciple.daoPoints} pkt", color = Color(0xFFF2D58A), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Smooth HP & Qi Progress
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(Color(0xFF1D1720))
                ) {
                    val hpFrac = (disciple.currentHp.toFloat() / disciple.maxHp.coerceAtLeast(1)).coerceIn(0f, 1f)
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(hpFrac)
                            .background(Color(0xFFEF4444))
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Qi Progress Bar
                QiProgressBar(
                    progressPercent = disciple.realmProgress,
                    label = if (isReadyForBreakthrough) "⚡ DANTIAN PRZEPEŁNIONY ENERGIĄ QI!" else "Nasycenie Meridianów Qi",
                    barColor = if (isReadyForBreakthrough) Color(0xFFE8C37E) else Color(0xFF10B981)
                )

                if (isReadyForBreakthrough) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = onQuickBreakthrough,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF9A6B2C)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("quick_breakthrough_${disciple.id}")
                    ) {
                        Text(
                            text = "⚡ RYTUAŁ PRZEŁOMU NIEBIAŃSKIEGO",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DiscipleDetailModal(
    disciple: Disciple,
    uiState: WuxiaUiState,
    viewModel: WuxiaGameViewModel,
    onDismiss: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) } // 0: Profil & Przełom, 1: Drzewo Umiejętności

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    ManhwaSchoolAvatar(
                        school = disciple.martialSchool,
                        size = 64.dp
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(text = disciple.name, color = TextGold, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                        Text(text = "${disciple.realm.polishName} • ${disciple.martialSchool.polishName}", color = JadeSpiritLight, fontSize = 11.sp)
                    }
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Filled.Close, contentDescription = "Zamknij", tint = TextSecondary)
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 520.dp)
            ) {
                AaaSegmentedTabs(
                    items = listOf("Kultywacja", "Drzewo Dao"),
                    selectedIndex = selectedTab,
                    onSelect = { selectedTab = it },
                    badges = listOf(null, if (disciple.daoPoints > 0) "${disciple.daoPoints}" else null)
                )

                Spacer(modifier = Modifier.height(10.dp))

                if (selectedTab == 0) {
                    DiscipleCultivationTab(disciple, uiState, viewModel)
                } else {
                    DiscipleSkillTreeTab(disciple, uiState, viewModel)
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Gotowe", color = TextGold)
            }
        },
        containerColor = InkSurface
    )
}

@Composable
fun DiscipleCultivationTab(
    disciple: Disciple,
    uiState: WuxiaUiState,
    viewModel: WuxiaGameViewModel
) {
    val coroutineScope = rememberCoroutineScope()
    var isTribulating by remember { mutableStateOf(false) }
    var tribulationFlash by remember { mutableStateOf(false) }
    var tribulationSuccess by remember { mutableStateOf<Boolean?>(null) }

    val standeeAssetKey = WuxiaAssetLoader.getSchoolAvatarKey(disciple.martialSchool)
    val standeeBitmap = rememberWuxiaGraphic(standeeAssetKey)

    val infiniteTransition = rememberInfiniteTransition(label = "standee_meridians")
    val dantianPulse by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dantian_pulse"
    )

    fun performHeavenlyBreakthrough(usePill: Boolean) {
        if (isTribulating) return
        coroutineScope.launch {
            isTribulating = true
            tribulationFlash = true
            WuxiaAudioEngine.playThunderStrike()
            delay(350)
            tribulationFlash = false
            delay(300)
            tribulationFlash = true
            delay(200)
            tribulationFlash = false

            delay(600)
            WuxiaAudioEngine.playDivineGong()
            viewModel.attemptBreakthrough(disciple.id, usePill = usePill)
            tribulationSuccess = true
            delay(2500)
            isTribulating = false
            tribulationSuccess = null
        }
    }

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // 1. Manhwa Character Standee with 8 Meridians & Dantian Qi Core
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(240.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .border(2.dp, Color(0xFFE8C37E), RoundedCornerShape(12.dp))
                    .background(Color(0xFF090611))
            ) {
                // Manhwa Character Standee
                if (standeeBitmap != null) {
                    androidx.compose.foundation.Image(
                        bitmap = standeeBitmap,
                        contentDescription = disciple.name,
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                // Ciemna winieta dla widoczności meridianów
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

                // 8 Meridianów Qi & Dantian Core na Canvasie
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height
                    val cx = w * 0.5f
                    val cy = h * 0.55f

                    // Główny kanał witalny Du Mai / Ren Mai
                    drawLine(
                        brush = Brush.verticalGradient(
                            listOf(Color(0xFF00E5FF).copy(alpha = 0.7f), Color(0xFFE8C37E).copy(alpha = 0.9f))
                        ),
                        start = Offset(cx, h * 0.22f),
                        end = Offset(cx, h * 0.82f),
                        strokeWidth = 3.5f
                    )

                    // Boczne meridiany ramion
                    drawLine(
                        color = Color(0xFF00E5FF).copy(alpha = 0.6f),
                        start = Offset(cx - 55f, h * 0.38f),
                        end = Offset(cx, h * 0.32f),
                        strokeWidth = 2f
                    )
                    drawLine(
                        color = Color(0xFF00E5FF).copy(alpha = 0.6f),
                        start = Offset(cx, h * 0.32f),
                        end = Offset(cx + 55f, h * 0.38f),
                        strokeWidth = 2f
                    )

                    // Pulsujący Dantian Qi Core w centrum brzucha
                    val dantianRadius = 18f * dantianPulse
                    drawCircle(
                        brush = Brush.radialGradient(
                            listOf(Color(0xFFE8C37E), Color(0xFF00E5FF).copy(alpha = 0.6f), Color.Transparent),
                            center = Offset(cx, cy),
                            radius = dantianRadius * 1.8f
                        ),
                        radius = dantianRadius * 1.8f,
                        center = Offset(cx, cy)
                    )
                    drawCircle(
                        color = Color.White,
                        radius = dantianRadius * 0.45f,
                        center = Offset(cx, cy)
                    )

                    // Błyskawica Niebiańskiej Kary
                    if (tribulationFlash) {
                        drawLine(
                            color = Color.White,
                            start = Offset(cx + 15f, 0f),
                            end = Offset(cx - 8f, h * 0.35f),
                            strokeWidth = 6f
                        )
                        drawLine(
                            color = Color(0xFFC084FC),
                            start = Offset(cx - 8f, h * 0.35f),
                            end = Offset(cx, cy),
                            strokeWidth = 5f
                        )
                    }
                }

                // Etykieta Szkoły i Dantian
                Surface(
                    color = Color.Black.copy(alpha = 0.8f),
                    shape = RoundedCornerShape(6.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE8C37E)),
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp)
                ) {
                    Text(
                        text = "8 MERIDIANÓW • DANTIAN QI: ${disciple.realmProgress}%",
                        color = Color(0xFFF3DDA6),
                        fontSize = 9.sp,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                // Powiadomienie o sukcesie przełomu
                if (tribulationSuccess == true) {
                    Surface(
                        color = Color(0xFF065F46).copy(alpha = 0.9f),
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFFE8C37E)),
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(16.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(text = "⚡ PRZEŁOM NIEBIAŃSKI!", color = Color(0xFFE8C37E), fontSize = 14.sp, fontWeight = FontWeight.Black)
                            Text(text = "Meridiany poszerzone • Rezerwuar Qi wzmocniony", color = Color.White, fontSize = 10.sp)
                        }
                    }
                }
            }
        }

        item {
            // Origin & Clan & Bloodline Card
            Surface(
                color = InkSurfaceVariant,
                shape = RoundedCornerShape(8.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE8C37E).copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "🏰 ${disciple.ancientClan.name} • ${disciple.ancientClan.reputationTitle}",
                            color = Color(0xFFF6E6BE),
                            fontWeight = FontWeight.Black,
                            fontSize = 13.sp
                        )
                        val (gCol, gBg) = when (disciple.talentGrade) {
                            TalentGrade.SSS -> Color(0xFFE8C37E) to Color(0xFF78350F)
                            TalentGrade.SS -> Color(0xFFF43F5E) to Color(0xFF881337)
                            TalentGrade.S -> Color(0xFF00E5FF) to Color(0xFF164E63)
                            TalentGrade.A -> Color(0xFF34D399) to Color(0xFF064E3B)
                            TalentGrade.B -> Color(0xFFA78BFA) to Color(0xFF3B0764)
                        }
                        Surface(
                            color = gBg,
                            shape = RoundedCornerShape(4.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, gCol)
                        ) {
                            Text(
                                text = "TALENT: ${disciple.talentGrade.name}",
                                color = gCol,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Black,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Text(
                        text = disciple.ancientClan.buffDescription,
                        color = Color(0xFFEBCB7A),
                        fontSize = 10.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "⚔️ Całkowita Siła Bojowa: ${disciple.combatPower} CP",
                        color = Color(0xFFE8C37E),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        text = "🌀 Wrodzona Technika Rodowa: ${disciple.innateTechnique}",
                        color = Color(0xFFA5B4FC),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "🩸 Linia Krwi: ${disciple.bloodline.polishName} (${disciple.bloodline.combatBonus})",
                        color = VermillionRed,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "✨ Konstytucja: ${disciple.constitution.polishName} (${disciple.constitution.hazardImmunity})",
                        color = AstralCyanLight,
                        fontSize = 10.sp
                    )

                    if (disciple.fatedTraits.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "✦ Cechy Przeznaczenia:",
                            color = Color(0xFFF6E6BE),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                        disciple.fatedTraits.forEach { trait ->
                            Text(
                                text = " • ${trait.name}: ${trait.description}",
                                color = Color(0xFFF2D58A),
                                fontSize = 9.sp
                            )
                        }
                    }
                }
            }
        }

        item {
            // Breakthrough Actions (Niebiańska Kara)
            Surface(
                color = InkSurfaceVariant,
                shape = RoundedCornerShape(8.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE8C37E)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "⚡ Rytuał Niebiańskiej Kary (Przełom)",
                            color = TextGold,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                        Text(
                            text = "${disciple.realmProgress}/100% Qi",
                            color = if (disciple.realmProgress >= 100) Color(0xFFE8C37E) else TextSecondary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    QiProgressBar(
                        progressPercent = disciple.realmProgress,
                        label = "Nasycenie Meridianu Dantian"
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    val hasPills = (uiState.inventory["Pigułka Przełomu Qi"] ?: 0) > 0
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { performHeavenlyBreakthrough(usePill = false) },
                            enabled = !isTribulating,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("breakthrough_raw_${disciple.id}"),
                            colors = ButtonDefaults.buttonColors(containerColor = DaoGoldDark)
                        ) {
                            Text(
                                text = if (isTribulating) "⚡ Spada Piorun..." else "Czyste Qi",
                                fontSize = 10.sp,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Button(
                            onClick = { performHeavenlyBreakthrough(usePill = true) },
                            enabled = hasPills && !isTribulating,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("breakthrough_pill_${disciple.id}"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = JadeSpiritPrimary,
                                disabledContainerColor = InkCardBorder
                            )
                        ) {
                            Text(
                                text = "💊 Użyj Pigułki (+35%)",
                                fontSize = 10.sp,
                                color = if (hasPills && !isTribulating) Color.Black else TextMuted,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        item {
            // Role Selection
            Text(text = "Przydzielone Zadanie w Sekcie:", color = TextGold, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                DiscipleRole.values().forEach { role ->
                    Surface(
                        color = if (disciple.role == role) JadeSpiritDark.copy(alpha = 0.4f) else InkSurfaceVariant,
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.assignRole(disciple.id, role) }
                            .testTag("role_${role.name}")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = role.polishName,
                                    color = if (disciple.role == role) TextGold else TextPrimary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(text = role.bonusDesc, color = TextMuted, fontSize = 10.sp)
                            }
                            if (disciple.role == role) {
                                Icon(Icons.Filled.Check, contentDescription = null, tint = JadeSpiritLight, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DiscipleSkillTreeTab(
    disciple: Disciple,
    uiState: WuxiaUiState,
    viewModel: WuxiaGameViewModel
) {
    var selectedCategoryFilter by remember { mutableStateOf<TechniqueCategory?>(null) }

    val schoolSkills = uiState.skills.filter {
        it.martialSchool == disciple.martialSchool && (selectedCategoryFilter == null || it.category == selectedCategoryFilter)
    }
    val groupedByTier = schoolSkills.groupBy { it.tier }

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Text(
                text = "Drzewo Dao: ${disciple.martialSchool.polishName}",
                color = TextGold,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Dostępne Punkty Zrozumienia Dao: ${disciple.daoPoints} pkt",
                color = AstralCyanLight,
                fontSize = 11.sp
            )
            Spacer(modifier = Modifier.height(6.dp))

            // Category Filter Chips
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                item {
                    val isAll = selectedCategoryFilter == null
                    Surface(
                        color = if (isAll) DaoGoldDark else InkSurfaceVariant,
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, if (isAll) DaoGold else InkCardBorder),
                        modifier = Modifier
                            .clickable { selectedCategoryFilter = null }
                            .testTag("filter_all_techniques")
                    ) {
                        Text(
                            text = "Wszystkie",
                            color = if (isAll) Color.White else TextSecondary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
                items(TechniqueCategory.values()) { cat ->
                    val isSelected = selectedCategoryFilter == cat
                    Surface(
                        color = if (isSelected) DaoGoldDark else InkSurfaceVariant,
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, if (isSelected) DaoGold else InkCardBorder),
                        modifier = Modifier
                            .clickable { selectedCategoryFilter = cat }
                            .testTag("filter_cat_${cat.name}")
                    ) {
                        Text(
                            text = "${cat.icon} ${cat.polishName}",
                            color = if (isSelected) Color.White else TextSecondary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }

        items(listOf(1, 2, 3, 4)) { tier ->
            val tierSkills = groupedByTier[tier] ?: emptyList()
            if (tierSkills.isNotEmpty()) {
                Surface(
                    color = InkSurfaceVariant,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = "KRĄG $tier (Stopień Zaawansowania)",
                            color = DaoGold,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        tierSkills.forEach { skill ->
                            val isUnlocked = disciple.unlockedSkillIds.contains(skill.id)
                            val canAfford = disciple.daoPoints >= skill.costDaoPoints

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = skill.name,
                                            color = if (isUnlocked) JadeSpiritLight else TextPrimary,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        WuxiaBadge(
                                            text = "${skill.category.icon} ${skill.gradeName}",
                                            containerColor = DaoGoldDark.copy(alpha = 0.25f),
                                            contentColor = DaoGoldLight
                                        )
                                        if (isUnlocked) {
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = JadeSpiritLight, modifier = Modifier.size(14.dp))
                                        }
                                    }
                                    Text(text = skill.description, color = TextSecondary, fontSize = 10.sp)

                                    // Stat Badges
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        modifier = Modifier.padding(top = 2.dp)
                                    ) {
                                        if (skill.attackBonus > 0) Text(text = "+${skill.attackBonus} Atak", color = VermillionRed, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                        if (skill.defenseBonus > 0) Text(text = "+${skill.defenseBonus} Obrona", color = AstralCyanLight, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                        if (skill.hpBonus > 0) Text(text = "+${skill.hpBonus} HP", color = JadeSpiritLight, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                        if (skill.evasionBonusPercent > 0) Text(text = "+${skill.evasionBonusPercent}% Uniki", color = Color(0xFFA78BFA), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                        if (skill.qiGainBonusPercent > 0) Text(text = "+${skill.qiGainBonusPercent}% Qi", color = DaoGoldLight, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                        if (skill.critBonusPercent > 0) Text(text = "+${skill.critBonusPercent}% Kryt", color = Color(0xFFF97316), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                    }

                                    if (skill.activeTechniqueName != null) {
                                        Text(
                                            text = "⚔️ Cios Bojowy: ${skill.activeTechniqueName}",
                                            color = CrimsonFlame,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }

                                if (!isUnlocked) {
                                    Button(
                                        onClick = { viewModel.unlockSkill(disciple.id, skill.id) },
                                        enabled = canAfford,
                                        modifier = Modifier.testTag("unlock_skill_${skill.id}"),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = DaoGoldDark,
                                            disabledContainerColor = InkCardBorder
                                        ),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                    ) {
                                        Text("${skill.costDaoPoints} Dao", fontSize = 10.sp, color = if (canAfford) Color.White else TextMuted)
                                    }
                                }
                            }
                            HorizontalDivider(color = InkCardBorder.copy(alpha = 0.5f), modifier = Modifier.padding(vertical = 4.dp))
                        }
                    }
                }
            }
        }
    }
}
