package com.example.ui.game

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.game.TribulationUi
import com.example.game.WuxiaGameViewModel
import com.example.game.WuxiaUiState
import com.example.game.engine.CultivationSystem
import com.example.game.engine.Realms
import com.example.model.CultivationPath
import com.example.model.DaoLaw
import com.example.model.Disciple
import com.example.model.MajorRealm
import com.example.ui.aaa.Aaa
import com.example.ui.aaa.AaaType
import com.example.ui.aaa.aaaFrame
import com.example.ui.audio.WuxiaAudioEngine
import kotlinx.coroutines.delay

private fun qi(v: Double): String = when {
    v >= 1e9 -> "%.1f mld".format(v / 1e9)
    v >= 1e6 -> "%.1f mln".format(v / 1e6)
    v >= 1e3 -> "%.1f tys.".format(v / 1e3)
    else -> "%.0f".format(v)
}

/** Panel kultywacji ucznia: królestwo i etap, metoda, ścieżka, Prawa, odosobnienie, krew, przełom. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CultivationPanel(d: Disciple, ui: WuxiaUiState, viewModel: WuxiaGameViewModel, onClose: () -> Unit) {
    var confirmTribulation by remember(d.id) { mutableStateOf(false) }
    SimplePanel(title = "Kultywacja · ${d.name.substringBefore(" (")}", onClose = onClose) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            val major = d.majorRealm
            Text(Realms.label(d), fontFamily = BrushFont, color = Aaa.GoldHi, fontSize = 26.sp)
            if (major != null) {
                KvRow("Qi etapu", "${qi(d.stageQi)} / ${qi(Realms.required(d))}")
                ThinBar(d.realmProgress / 100f, Aaa.Gold)
                val method = Realms.method(d.methodId)
                KvRow("Metoda", "${method.name} (${method.grade.polishName})")
                KvRow("Sufit metody", Realms.ceiling(d).polishName, valueColor = Aaa.TextMid)
                d.foundationQuality?.let { KvRow("Fundament", it.polishName) }
                if (d.coreGrade > 0) KvRow("Złoty Rdzeń", "grad ${d.coreGrade}")
                d.soulShape?.let { KvRow("Kształt duszy", it) }
                KvRow("Toksyczność pigułek", "${d.toxicity}", valueColor = if (d.toxicity > CultivationSystem.DEVIATION_TOXICITY) Aaa.Cinnabar else Aaa.TextMid)
                if (d.injuredUntilDay >= ui.currentDay) KvRow("Rany", "do dnia ${d.injuredUntilDay} (Qi ×0,5)", valueColor = Aaa.Cinnabar)
                if (d.heartDemon) KvRow("Demon serca", "blokuje wyższe królestwa", valueColor = Aaa.Cinnabar)
            }

            // Ścieżka
            Overline("Ścieżka kultywacji")
            if (major == null || major.ordinal < MajorRealm.FOUNDATION.ordinal) {
                SheetBody("Ścieżkę wybiera się przy wejściu w Fundamenty.")
            } else {
                d.path?.let { SheetBody("${it.polishName}: ${it.description}", maxLines = 3) }
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    CultivationPath.entries.filter { it != d.path }.forEach { p ->
                        GhostButton(p.polishName, onClick = { viewModel.choosePath(d.id, p) }, height = 34.dp)
                    }
                }
                if (d.path != null) SheetBody("Zmiana ścieżki kosztuje Pigułkę Ciszy Umysłu i postęp bieżącego etapu.", color = Aaa.TextLow)
            }

            // Zwoje
            if (ui.scrolls.isNotEmpty()) {
                Overline("Zwoje w magazynie")
                ui.scrolls.forEach { scroll ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(scroll.removePrefix("Zwój: "), style = AaaType.Label.copy(fontSize = 11.sp, color = Aaa.TextHi), modifier = Modifier.weight(1f))
                        GhostButton("Naucz", onClick = { viewModel.learnMethod(d.id, scroll) }, height = 32.dp)
                    }
                }
            }

            // Prawa Dao
            if (major != null && major.ordinal >= MajorRealm.NASCENT_SOUL.ordinal) {
                Overline("Prawa Dao")
                d.laws.forEach { (law, pct) ->
                    KvRow(law, "%.1f%%".format(pct))
                    ThinBar((pct / 100.0).toFloat(), Aaa.Azure)
                }
                if (d.laws.size < 2) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        DaoLaw.entries.filter { it.polishName !in d.laws }.forEach { law ->
                            GhostButton(law.polishName.removePrefix("Prawo "), onClick = { viewModel.chooseLaw(d.id, law) }, height = 32.dp)
                        }
                    }
                }
            }

            // Odosobnienie i krew
            Overline("Odosobnienie i krew")
            if (d.secludedUntilDay >= ui.currentDay) {
                SheetBody("W odosobnieniu do dnia ${d.secludedUntilDay} (Qi ×3). Przerwanie grozi odchyleniem Qi.")
                GhostButton("Przerwij odosobnienie", danger = true, onClick = { viewModel.endSeclusion(d.id) })
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(5, 15, 30).forEach { days ->
                        GhostButton("$days dni", onClick = { viewModel.seclude(d.id, days) }, modifier = Modifier.weight(1f), height = 36.dp)
                    }
                }
            }
            if (d.bloodlineAwakening < 3) {
                GhostButton(
                    "Przebudź krew (${d.bloodlineAwakening + 1}. stopień): ${Realms.bloodlineCatalyst(d.bloodline)} ×${d.bloodlineAwakening + 1}",
                    onClick = { viewModel.awakenBloodline(d.id) },
                    height = 38.dp
                )
            }

            // Przełom
            Overline("Przełom")
            val block = ui.bottlenecks[d.id]
            val ready = major != null && CultivationSystem.ready(d)
            val atPeak = Realms.atPeak(d)
            val next = major?.next
            when {
                !ready -> SheetBody("Uczeń gromadzi Qi w etapie (${d.realmProgress}%).")
                block != null -> SheetBody(block, color = Aaa.Cinnabar, maxLines = 4)
                atPeak && major == MajorRealm.MAHAYANA -> {
                    SheetBody("Szczyt świata śmiertelników. Wniebowstąpienie zabierze ucznia z sekty, zostawiając jej stałą premię.", maxLines = 3)
                    GoldCta("Wniebowstąpienie", onClick = { viewModel.ascend(d.id) }, modifier = Modifier.fillMaxWidth())
                }
                atPeak && next != null && next.tribulationStrikes > 0 -> {
                    SheetBody(
                        "Wejście w ${next.polishName} wywoła tribulację: ${next.tribulationStrikes} uderzeń pioruna. " +
                            "Porażka to rany i 60% postępu; 2% szansy na śmierć. Pigułka Dziewięciu Gromów Tribulacji osłabia pioruny.",
                        maxLines = 5
                    )
                    if (!confirmTribulation) {
                        GoldCta("Przygotuj tribulację", onClick = { confirmTribulation = true }, modifier = Modifier.fillMaxWidth())
                    } else {
                        GoldCta("Wezwij pioruny", danger = true, onClick = {
                            confirmTribulation = false
                            WuxiaAudioEngine.playThunderStrike()
                            viewModel.attemptBreakthrough(d.id, usePill = ui.autoUsePillsForBreakthrough)
                        }, modifier = Modifier.fillMaxWidth())
                    }
                }
                else -> GoldCta(
                    if (atPeak) "Wielki przełom" else "Przełom etapu",
                    onClick = { viewModel.attemptBreakthrough(d.id, usePill = ui.autoUsePillsForBreakthrough) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

/** Scena tribulacji: kolejne uderzenia pioruna i pasek życia. */
@Composable
fun TribulationOverlay(t: TribulationUi, onDone: () -> Unit) {
    var shown by remember(t.id) { mutableIntStateOf(0) }
    val flash = remember(t.id) { Animatable(0f) }
    LaunchedEffect(t.id) {
        for (i in t.damage.indices) {
            delay(900)
            shown = i + 1
            WuxiaAudioEngine.playThunderStrike()
            flash.snapTo(1f)
            flash.animateTo(0f, tween(500))
        }
    }
    val hp = (t.maxHp - t.damage.take(shown).sum()).coerceAtLeast(0)
    val finished = shown >= t.damage.size
    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.82f))
            .background(Aaa.Azure.copy(alpha = 0.35f * flash.value))
            .clickable(remember { MutableInteractionSource() }, indication = null) { if (finished) onDone() },
        contentAlignment = Alignment.Center
    ) {
        Column(
            Modifier
                .padding(24.dp)
                .widthIn(max = 420.dp)
                .fillMaxWidth()
                .aaaFrame(accent = Aaa.Azure, fill = Brush.verticalGradient(listOf(Aaa.Ink3, Aaa.Ink1)))
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Overline("Niebiańska tribulacja", color = Aaa.Azure)
            Text(t.discipleName.substringBefore(" ("), fontFamily = BrushFont, color = Aaa.GoldHi, fontSize = 30.sp)
            Text("Uderzenie $shown / ${t.strikes}", color = Aaa.TextHi, fontSize = 15.sp)
            ThinBar(hp.toFloat() / t.maxHp, if (hp > t.maxHp * 0.3) Aaa.Jade else Aaa.Cinnabar, height = 8.dp)
            Text("HP $hp / ${t.maxHp}", color = Aaa.TextMid, fontSize = 12.sp)
            if (finished) {
                val (title, color) = when {
                    t.catastrophe -> "Katastrofa" to Aaa.Cinnabar
                    !t.survived -> "Porażka – rany" to Aaa.Cinnabar
                    t.perfect -> "Doskonała tribulacja!" to Aaa.GoldHi
                    else -> "Przetrwał!" to Aaa.Jade
                }
                Text(title, fontFamily = BrushFont, color = color, fontSize = 26.sp, textAlign = TextAlign.Center)
                GoldCta("Dalej", onClick = onDone, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}
