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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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

/** Księga Tajemnic – Dziennik odkryć, zakładka na kategorię. Liczby tylko przy wpisach „Pewne”. */
@Composable
fun JournalPanel(ui: WuxiaUiState, initialTab: Int = 0, onClose: () -> Unit) {
    val sec = ui.secrets
    val cats = sec.journal.map { it.category }.distinct()
    var tab by remember { mutableIntStateOf(initialTab) }
    TabbedPanel(
        title = "Księga Tajemnic",
        tabs = listOf("Wszystko") + cats,
        selected = tab,
        onSelect = { tab = it },
        onClose = onClose,
        header = {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                StateTag("Pewne ${sec.discovered}/${sec.total}", Aaa.GoldHi)
                StateTag("Prawdopodobne ${sec.journal.count { it.level == KnowledgeLevel.LIKELY }}", Aaa.Azure)
                StateTag("Przypuszczenia ${sec.journal.count { it.level == KnowledgeLevel.GUESS }}", Aaa.TextLow)
            }
            ThinBar(if (sec.total > 0) sec.discovered.toFloat() / sec.total else 0f, Aaa.Gold)
        }
    ) {
        val cat = cats.getOrNull(tab - 1)
        if (sec.journal.isEmpty()) {
            SheetBody("Świat ma swoje prawa, inne w każdej grze. Sny, badania, wyprawy, walki i eksperymenty zapełnią tę księgę.", maxLines = 3)
        }
        sec.journal.filter { cat == null || it.category == cat }.forEach { e ->
            InfoCard(accent = levelColor(e.level).takeIf { e.level != KnowledgeLevel.GUESS }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(e.title, style = AaaType.Label.copy(fontSize = 11.5.sp, color = Aaa.TextHi), modifier = Modifier.weight(1f), maxLines = 1)
                    StateTag(e.level.polishName, levelColor(e.level))
                }
                Text("${e.category} · ${e.value}", style = AaaType.Label.copy(fontSize = 9.5.sp, color = if (e.level == KnowledgeLevel.CERTAIN) Aaa.Gold else Aaa.TextMid))
                e.notes.takeLast(3).forEach { Text("· $it", style = AaaType.Label.copy(fontSize = 9.sp, color = Aaa.TextLow), maxLines = 3) }
            }
        }
    }
}

private enum class DaoTab(val label: String) { MERIDIANS("Meridiany"), BREATH("Oddech"), CONTEMPLATION("Kontemplacja"), TOOLS("Narzędzia") }

/** Meridiany i Dao ucznia: siatka 6×6, oddech, kontemplacja, narzędzia. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SecretsPanel(d: Disciple, ui: WuxiaUiState, viewModel: WuxiaGameViewModel, initialTab: Int = 0, onClose: () -> Unit) {
    val sec = ui.secrets
    var journal by remember { mutableStateOf(false) }
    var tab by remember { mutableStateOf(DaoTab.entries[initialTab.coerceIn(0, DaoTab.entries.lastIndex)]) }
    val cap = com.example.game.engine.Meridians.cap(d.majorRealm)
    TabbedPanel(
        title = "Dao · ${d.name.substringBefore(" (")}",
        tabs = DaoTab.entries.map { it.label },
        selected = tab.ordinal,
        onSelect = { tab = DaoTab.entries[it] },
        onClose = onClose,
        header = {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                StateTag("Meridiany ${d.openedMeridians.size}/$cap", Aaa.Azure)
                d.secretState?.let { StateTag(it.polishName, Aaa.Violet) }
                if (sec.researcherId == d.id) StateTag("Badacz", Aaa.Jade)
                Spacer(Modifier.weight(1f))
                Text("Księga ${sec.discovered}/${sec.total} ›", style = AaaType.Label.copy(fontSize = 10.sp, color = Aaa.Gold),
                    modifier = Modifier.clickable(role = Role.Button) { journal = true })
            }
        }
    ) {
        when (tab) {
            DaoTab.MERIDIANS -> {
                SheetBody("Qi płynie od centrum do sąsiednich punktów. Niektóre układy rozbłyskują gwiazdozbiorami. Czerwone obramowanie: znany zator (×3 kosztu).", color = Aaa.TextLow, maxLines = 3)
                val lit = sec.knownConstellations.filterValues { pts -> d.openedMeridians.containsAll(pts) }.values.flatten().toSet()
                val full = d.openedMeridians.size >= cap
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    (0 until 6).forEach { r ->
                        Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                            (0 until 6).forEach { c ->
                                val p = r * 6 + c
                                val open = p in d.openedMeridians
                                val center = p in setOf(14, 15, 20, 21)
                                val reachable = !full && !open && (center || com.example.game.engine.RulesGen.neighbours(p).any { it in d.openedMeridians })
                                Box(
                                    Modifier
                                        .weight(1f)
                                        .aspectRatio(1f)
                                        .background(
                                            when {
                                                p in lit -> Aaa.Gold.copy(alpha = 0.6f)
                                                open -> Aaa.Azure.copy(alpha = 0.5f)
                                                reachable -> Aaa.Azure.copy(alpha = 0.12f)
                                                else -> Aaa.Ink2
                                            }
                                        )
                                        .border(if (p in sec.knownBlockages) 2.dp else 1.dp, when {
                                            p in sec.knownBlockages -> Aaa.Cinnabar
                                            reachable -> Aaa.Azure.copy(alpha = 0.6f)
                                            else -> Aaa.Hairline
                                        })
                                        .clickable(enabled = reachable, role = Role.Button) { viewModel.openMeridian(d.id, p) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(if (open) "●" else "${p + 1}", textAlign = TextAlign.Center,
                                        style = AaaType.Label.copy(fontSize = if (open) 11.sp else 8.sp, color = if (p in lit) Aaa.Ink0 else if (open) Aaa.TextHi else Aaa.TextLow))
                                }
                            }
                        }
                    }
                }
                if (full) SheetBody("Limit meridianów dla obecnego królestwa – przełom otworzy więcej.", color = Aaa.Ember, maxLines = 2)
                Section("Gwiazdozbiory", "${sec.knownConstellations.size} znanych")
                if (sec.knownConstellations.isEmpty()) SheetBody("Żaden gwiazdozbiór nie rozbłysnął. Sny i Lustro Dao podpowiadają kształty.", color = Aaa.TextLow, maxLines = 2)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    sec.knownConstellations.forEach { (name, pts) -> StateTag("✦ $name", if (d.openedMeridians.containsAll(pts)) Aaa.GoldHi else Aaa.TextMid) }
                }
            }
            DaoTab.BREATH -> {
                SheetBody("Każda metoda ma swój oddech, miejsce i porę dnia. Gdy trafisz, Qi popłynie wyraźnie lepiej – Księga odnotuje to po kilku dniach.", color = Aaa.TextLow, maxLines = 3)
                KvRow("Metoda", com.example.game.engine.Realms.method(d.methodId).name)
                KvRow("Pora teraz", ui.timeOfDay.polishName, valueColor = Aaa.TextMid)
                Section("Oddech")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    sec.breaths.forEachIndexed { i, b -> SelectChip(b, d.breath == i, Aaa.Azure) { viewModel.setBreath(d.id, i, d.meditationPlace ?: 0) } }
                }
                Section("Miejsce")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    sec.places.forEachIndexed { i, p -> SelectChip(p, d.meditationPlace == i, Aaa.Jade) { viewModel.setBreath(d.id, d.breath ?: 0, i) } }
                }
                d.methodId?.let { m ->
                    val frags = sec.fragments[m] ?: 0
                    Section("Szyfrowany zwój", "$frags/5")
                    ThinBar(frags / 5f, Aaa.Gold)
                    ActionRow("Fragmenty zwoju tej metody", "Pięć fragmentów odsłania pełne optimum. Szukaj ich w skrytkach wypraw i w lochach.") {
                        GhostButton("Odszyfruj", enabled = frags >= 5, onClick = { viewModel.decipherScroll(m) }, height = 32.dp)
                    }
                }
            }
            DaoTab.CONTEMPLATION -> {
                SheetBody("Rzeczy i zjawiska rezonują z Prawami. Ten sam obiekt uczy coraz mniej. Dwa Prawa powyżej 50% mogą się spleść.", color = Aaa.TextLow, maxLines = 3)
                if (d.laws.isNotEmpty()) {
                    Section("Prawa ucznia")
                    d.laws.forEach { (law, pct) ->
                        KvRow(law, "%.1f%%".format(pct))
                        ThinBar((pct / 100.0).toFloat(), Aaa.Azure)
                    }
                } else SheetBody("Uczeń nie wybrał jeszcze Praw (od Narodzin Duszy) – kontemplacja tylko podpowie, co z czym rezonuje.", color = Aaa.Ember, maxLines = 2)
                Section("Obiekty", "${sec.contemplationObjects.size}")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    sec.contemplationObjects.forEach { o ->
                        val n = d.contemplations[o] ?: 0
                        SelectChip(o + if (n > 0) " ·$n" else "", false) { viewModel.contemplate(d.id, o) }
                    }
                }
            }
            DaoTab.TOOLS -> {
                InfoCard(accent = Aaa.Jade.takeIf { sec.researcherId == d.id }) {
                    ActionRow("Badacz tajemnic", "Codziennie szansa na wskazówkę (lepsza przy wysokim zrozumieniu: ${d.comprehension}).") {
                        GhostButton(if (sec.researcherId == d.id) "Odwołaj" else "Mianuj", onClick = { viewModel.setResearcher(if (sec.researcherId == d.id) null else d.id) }, height = 32.dp)
                    }
                }
                InfoCard {
                    ActionRow("Lustro Dao · ${sec.mirrors}", "Pokazuje gwiazdozbiór, do którego uczeń jest najbliżej. Znajdziesz je w Zatopionej Bibliotece.") {
                        GhostButton("Użyj", enabled = sec.mirrors > 0, onClick = { viewModel.useMirror(d.id) }, height = 32.dp)
                    }
                }
                d.secretState?.let { st ->
                    InfoCard(accent = Aaa.Violet) {
                        Text(st.polishName, style = AaaType.Label.copy(fontSize = 12.sp, color = Aaa.Violet))
                        SheetBody(st.description + " (do dnia ${d.secretStateUntil})", maxLines = 4)
                    }
                }
            }
        }
    }
    if (journal) JournalPanel(ui, onClose = { journal = false })
}
