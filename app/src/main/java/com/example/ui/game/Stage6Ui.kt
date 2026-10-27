package com.example.ui.game

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.game.WuxiaGameViewModel
import com.example.game.WuxiaUiState
import com.example.model.Disciple
import com.example.model.KnowledgeLevel
import com.example.ui.aaa.Aaa
import com.example.ui.aaa.AaaType

private fun levelColor(l: KnowledgeLevel) = when (l) {
    KnowledgeLevel.GUESS -> Aaa.TextLow
    KnowledgeLevel.LIKELY -> Aaa.Azure
    KnowledgeLevel.CERTAIN -> Aaa.GoldHi
}

/** Księga Tajemnic – Dziennik odkryć. Liczby tylko przy wpisach „Pewne”. */
@Composable
fun JournalPanel(ui: WuxiaUiState, onClose: () -> Unit) {
    val sec = ui.secrets
    var cat by remember { mutableStateOf<String?>(null) }
    LegacyPanel(title = "Księga Tajemnic · ${sec.discovered}/${sec.total}", onClose = onClose) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SheetBody("Świat ma swoje prawa, inne w każdej grze. Obserwuj, eksperymentuj i notuj. Przypuszczenie → Prawdopodobne → Pewne.", maxLines = 3)
            val cats = sec.journal.map { it.category }.distinct()
            if (cats.isNotEmpty()) {
                TextTabs(listOf("Wszystko") + cats, cats.indexOf(cat) + 1, { cat = if (it == 0) null else cats[it - 1] })
            }
            if (sec.journal.isEmpty()) SheetBody("Księga jest pusta. Sny, badania, wyprawy i walki zapełnią ją z czasem.", color = Aaa.TextLow)
            sec.journal.filter { cat == null || it.category == cat }.forEach { e ->
                Column(
                    Modifier
                        .fillMaxWidth()
                        .background(Aaa.Ink3.copy(alpha = 0.7f))
                        .border(1.dp, levelColor(e.level).copy(alpha = 0.5f))
                        .padding(9.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(e.title, style = AaaType.Label.copy(fontSize = 11.5.sp, color = Aaa.TextHi), modifier = Modifier.weight(1f), maxLines = 1)
                        Text(e.level.polishName, style = AaaType.Label.copy(fontSize = 9.5.sp, color = levelColor(e.level)))
                    }
                    Text("${e.category} · ${e.value}", style = AaaType.Label.copy(fontSize = 9.5.sp, color = Aaa.TextMid))
                    e.notes.takeLast(3).forEach { Text("· $it", style = AaaType.Label.copy(fontSize = 9.sp, color = Aaa.TextLow), maxLines = 3) }
                }
            }
        }
    }
}

/** Mapa meridianów ucznia (6×6) + oddech + kontemplacja + narzędzia. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SecretsPanel(d: Disciple, ui: WuxiaUiState, viewModel: WuxiaGameViewModel, onClose: () -> Unit) {
    val sec = ui.secrets
    var journal by remember { mutableStateOf(false) }
    LegacyPanel(title = "Meridiany i Dao · ${d.name.substringBefore(" (")}", onClose = onClose) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            GhostButton("Księga Tajemnic (${sec.discovered}/${sec.total})", onClick = { journal = true }, modifier = Modifier.fillMaxWidth(), height = 36.dp)

            Overline("Meridiany · ${d.openedMeridians.size} otwartych")
            SheetBody("Qi płynie od centrum do sąsiednich punktów. Niektóre układy punktów rozbłyskują gwiazdozbiorami.", color = Aaa.TextLow, maxLines = 2)
            val lit = sec.knownConstellations.filterValues { pts -> d.openedMeridians.containsAll(pts) }.values.flatten().toSet()
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                (0 until 6).forEach { r ->
                    Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                        (0 until 6).forEach { c ->
                            val p = r * 6 + c
                            val open = p in d.openedMeridians
                            val center = p in setOf(14, 15, 20, 21)
                            val reachable = !open && (center || listOf(p - 6, p + 6).plus(if (c > 0) listOf(p - 1) else emptyList()).plus(if (c < 5) listOf(p + 1) else emptyList()).any { it in d.openedMeridians })
                            Box(
                                Modifier
                                    .weight(1f)
                                    .aspectRatio(1f)
                                    .background(
                                        when {
                                            p in lit -> Aaa.Gold.copy(alpha = 0.55f)
                                            open -> Aaa.Azure.copy(alpha = 0.45f)
                                            reachable -> Aaa.Ink4
                                            else -> Aaa.Ink2
                                        }
                                    )
                                    .border(1.dp, if (p in sec.knownBlockages) Aaa.Cinnabar else Aaa.Hairline)
                                    .clickable(enabled = reachable, role = Role.Button) { viewModel.openMeridian(d.id, p) },
                                contentAlignment = Alignment.Center
                            ) {
                                Text("${p + 1}", textAlign = TextAlign.Center, style = AaaType.Label.copy(fontSize = 8.sp, color = if (open) Aaa.TextHi else Aaa.TextLow))
                            }
                        }
                    }
                }
            }
            if (sec.knownConstellations.isNotEmpty()) {
                SheetBody("Znane gwiazdozbiory: " + sec.knownConstellations.keys.joinToString(), color = Aaa.Gold, maxLines = 3)
            }

            Overline("Oddech i miejsce medytacji")
            SheetBody("Każda metoda ma swój oddech, miejsce i porę. Gdy trafisz, Qi popłynie wyraźnie lepiej.", color = Aaa.TextLow, maxLines = 2)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                sec.breaths.forEachIndexed { i, b -> Chip(b, d.breath == i) { viewModel.setBreath(d.id, i, d.meditationPlace ?: 0) } }
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                sec.places.forEachIndexed { i, p -> Chip(p, d.meditationPlace == i) { viewModel.setBreath(d.id, d.breath ?: 0, i) } }
            }
            d.methodId?.let { m ->
                val frags = sec.fragments[m] ?: 0
                if (frags > 0) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Fragmenty zwoju metody: $frags/5", style = AaaType.Label.copy(fontSize = 11.sp, color = Aaa.TextHi), modifier = Modifier.weight(1f))
                        GhostButton("Odszyfruj", enabled = frags >= 5, onClick = { viewModel.decipherScroll(m) }, height = 32.dp)
                    }
                }
            }

            Overline("Kontemplacja")
            SheetBody("Rzeczy i zjawiska rezonują z Prawami. Ten sam obiekt uczy coraz mniej.", color = Aaa.TextLow, maxLines = 2)
            var showAll by remember { mutableStateOf(false) }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                sec.contemplationObjects.take(if (showAll) 200 else 12).forEach { o ->
                    Chip("$o${(d.contemplations[o] ?: 0).takeIf { it > 0 }?.let { " ·$it" } ?: ""}", false) { viewModel.contemplate(d.id, o) }
                }
            }
            if (!showAll && sec.contemplationObjects.size > 12) GhostButton("Pokaż wszystkie (${sec.contemplationObjects.size})", onClick = { showAll = true }, height = 30.dp)

            Overline("Narzędzia")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                GhostButton(
                    if (sec.researcherId == d.id) "Badacz ✓" else "Mianuj Badaczem",
                    onClick = { viewModel.setResearcher(if (sec.researcherId == d.id) null else d.id) },
                    modifier = Modifier.weight(1f), height = 36.dp
                )
                GhostButton("Lustro Dao (${sec.mirrors})", enabled = sec.mirrors > 0, onClick = { viewModel.useMirror(d.id) }, modifier = Modifier.weight(1f), height = 36.dp)
            }
            d.secretState?.let { SheetBody("Stan: ${it.polishName} (do dnia ${d.secretStateUntil})", color = Aaa.Violet) }
        }
    }
    if (journal) JournalPanel(ui, onClose = { journal = false })
}

@Composable
private fun Chip(text: String, on: Boolean, onClick: () -> Unit) {
    Text(
        text,
        style = AaaType.Label.copy(fontSize = 10.sp, color = if (on) Aaa.Ink0 else Aaa.TextHi),
        modifier = Modifier
            .background(if (on) Aaa.Azure else Aaa.Ink3)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 5.dp)
    )
}
