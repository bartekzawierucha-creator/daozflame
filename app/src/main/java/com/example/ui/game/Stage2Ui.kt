package com.example.ui.game

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.game.WuxiaGameViewModel
import com.example.game.WuxiaUiState
import com.example.game.engine.DiscipleRules
import com.example.model.Disciple
import com.example.model.RootGrade
import com.example.ui.aaa.Aaa
import com.example.ui.aaa.AaaType
import com.example.ui.aaa.aaaFrame

fun rootColor(grade: RootGrade?): Color = when (grade) {
    RootGrade.CHAOS -> Aaa.Violet
    RootGrade.HEAVENLY -> Aaa.Azure
    RootGrade.SINGLE -> Aaa.GoldHi
    RootGrade.DUAL -> Aaa.Gold
    RootGrade.TRIPLE -> Aaa.TextMid
    RootGrade.MIXED, null -> Aaa.TextLow
}

fun loyaltyColor(loyalty: Int): Color = when {
    loyalty >= 60 -> Aaa.Jade
    loyalty >= 30 -> Aaa.Gold
    else -> Aaa.Cinnabar
}

/** Korzeń, ranga, lojalność i cechy ucznia – karta w module Adeptów. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DiscipleLifeInfo(d: Disciple, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        val root = d.spiritRoot
        KvRow(
            label = "Korzeń duchowy",
            value = root?.label ?: "nieznany",
            valueColor = rootColor(root?.grade)
        )
        KvRow(label = d.rank.polishName, value = "Lojalność ${d.loyalty}", valueColor = loyaltyColor(d.loyalty))
        if (d.traitIds.isNotEmpty() || d.hiddenTraitIds.isNotEmpty()) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                d.traitIds.forEach { id ->
                    val t = DiscipleRules.trait(id)
                    if (t != null) TraitChip(t.name, if (t.positive) Aaa.Jade else Aaa.Cinnabar)
                }
                d.hiddenTraitIds.forEach { _ -> TraitChip("???", Aaa.TextLow) }
            }
        }
    }
}

/** Jedna linia pod statystykami: korzeń duchowy i cechy (bez rangi – jest na przycisku, lojalność – w statystykach). */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DiscipleTraitsRow(d: Disciple) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        d.spiritRoot?.let { TraitChip("Korzeń: " + it.label.substringBefore(" ("), rootColor(it.grade)) }
        d.traitIds.forEach { id -> DiscipleRules.trait(id)?.let { TraitChip(it.name, if (it.positive) Aaa.Jade else Aaa.Cinnabar) } }
        d.hiddenTraitIds.forEach { _ -> TraitChip("???", Aaa.TextLow) }
    }
}

@Composable
private fun TraitChip(text: String, color: Color) {
    Text(
        text,
        style = AaaType.Label.copy(fontSize = 10.sp, color = color),
        modifier = Modifier
            .background(color.copy(alpha = 0.14f), CutCornerShape(4.dp))
            .padding(horizontal = 7.dp, vertical = 3.dp)
    )
}

/** Egzamin wstępny: kandydaci z korzeniem i cechami, test korzenia, przyjęcie, zaopatrzenie. */
@Composable
fun ExamPanel(ui: WuxiaUiState, viewModel: WuxiaGameViewModel, onClose: () -> Unit) {
    LegacyPanel(title = "Egzamin Wstępny", onClose = onClose) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            val open = ui.examDaysLeft > 0 && ui.availableRecruits.isNotEmpty()
            SheetBody(
                if (open) "Egzamin trwa jeszcze ${ui.examDaysLeft} dni. Pierwszych dwóch kandydatów przyjmujesz za darmo, każdy kolejny kosztuje więcej."
                else "Brama jest zamknięta. Następny egzamin za ${ui.nextExamInDays} dni.",
                maxLines = 4
            )
            KvRow("Morale sekty", "${ui.morale}", valueColor = loyaltyColor(ui.morale))
            KvRow("Ryż duchowy", "${ui.food} racji (${ui.disciples.size} dziennie)", valueColor = if (ui.food < ui.disciples.size * 3) Aaa.Cinnabar else Aaa.Gold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                GhostButton("Kup 100 racji (${100 * DiscipleRules.FOOD_PRICE})", onClick = { viewModel.buyFood(100) }, modifier = Modifier.weight(1f))
                GhostButton(
                    if (ui.feastReadyInDays > 0) "Uczta za ${ui.feastReadyInDays} dni" else "Uczta (+30 morale)",
                    enabled = ui.feastReadyInDays <= 0,
                    onClick = { viewModel.holdFeast() },
                    modifier = Modifier.weight(1f)
                )
            }
            if (open) {
                ui.availableRecruits.forEach { c -> CandidateCard(c, ui.admissionCost, viewModel) }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun CandidateCard(c: Disciple, cost: Int, viewModel: WuxiaGameViewModel) {
    Column(
        Modifier
            .fillMaxWidth()
            .aaaFrame(accent = rootColor(c.spiritRoot?.grade), fill = Brush.verticalGradient(listOf(Aaa.Ink3, Aaa.Ink1)), ornaments = false)
            .padding(12.dp)
            .testTag("candidate_${c.id}"),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(c.name, style = AaaType.Title.copy(fontSize = 18.sp), modifier = Modifier.weight(1f))
            Text("${c.ageYears} lat · ${c.realm.polishName.substringBefore(" (")}", style = AaaType.Label.copy(fontSize = 10.sp, color = Aaa.TextMid))
        }
        DiscipleLifeInfo(c)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (c.hiddenTraitIds.isNotEmpty()) {
                GhostButton("Test Korzenia (${DiscipleRules.ROOT_TEST_COST})", onClick = { viewModel.testCandidate(c.id) }, modifier = Modifier.weight(1f), height = 38.dp)
            }
            GoldCta(
                if (cost == 0) "Przyjmij" else "Przyjmij",
                trailing = if (cost == 0) "za darmo" else "$cost",
                onClick = { viewModel.recruitDisciple(c.id) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}
