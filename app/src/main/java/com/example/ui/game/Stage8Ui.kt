package com.example.ui.game

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.game.WuxiaGameViewModel
import com.example.game.WuxiaUiState
import com.example.game.engine.DailyGoals
import com.example.game.engine.EraModifiers
import com.example.game.engine.HeavenTree
import com.example.model.Difficulty
import com.example.ui.aaa.Aaa
import com.example.ui.aaa.AaaType

enum class MetaTab(val label: String) { REBIRTH("Odrodzenie"), TREE("Drzewo Niebios"), ACHIEVEMENTS("Osiągnięcia"), DAILY("Cele dnia"), CODEX("Kodeks") }

/** Panel meta: odrodzenie sekty, Drzewo Niebios, osiągnięcia i tytuły, cele dzienne, Kodeks. */
@Composable
fun MetaPanel(ui: WuxiaUiState, viewModel: WuxiaGameViewModel, initial: MetaTab = MetaTab.REBIRTH, onClose: () -> Unit) {
    var tab by remember { mutableStateOf(initial) }
    TabbedPanel(
        title = "Niebiosa · Era ${ui.meta.era}",
        tabs = MetaTab.entries.map { it.label },
        selected = tab.ordinal,
        onSelect = { tab = MetaTab.entries[it] },
        onClose = onClose,
        header = {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                StateTag("Esencja ${ui.meta.essence}", Aaa.GoldHi)
                StateTag("Osiągnięcia ${ui.achievements.count { it.unlocked }}/${ui.achievements.size}", Aaa.Gold)
                ui.meta.title?.let { StateTag(it, Aaa.Violet) }
                if (ui.meta.ironWill) StateTag("Żelazna Wola", Aaa.Cinnabar)
            }
        }
    ) {
        when (tab) {
            MetaTab.REBIRTH -> RebirthTab(ui, viewModel)
            MetaTab.TREE -> TreeTab(ui, viewModel)
            MetaTab.ACHIEVEMENTS -> AchievementsTab(ui, viewModel)
            MetaTab.DAILY -> DailyTab(ui, viewModel)
            MetaTab.CODEX -> {
                RuleHint("Kodeks liczy, ile tajemnic poznałeś w najlepszej erze. Nie mówi, czego brakuje.", maxLines = 2)
                KvRow("Ta era", "${ui.secrets.discovered} / ${ui.secrets.total}")
                ui.meta.codex.forEach { (cat, n) -> KvRow(cat, "$n") }
                if (ui.meta.legacy.isNotEmpty()) SheetBody("Księga Dziedzictwa: ${ui.meta.legacy.size} praw świata przechodzi do każdej ery.", color = Aaa.Gold, maxLines = 2)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RebirthTab(ui: WuxiaUiState, viewModel: WuxiaGameViewModel) {
    val m = ui.meta
    KvRow("Esencja Niebios", "${m.essence}")
    KvRow("Po odrodzeniu", "+${ui.essenceOnRebirth}", valueColor = Aaa.Jade)
    RuleHint(
        "Odrodzenie zeruje sektę i świat (nowe ziarno – nowe tajemnice). Zostają: Drzewo Niebios, Esencja, Księga Dziedzictwa, osiągnięcia, tytuły, Kodeks, liczniki, poziom trudności.",
        maxLines = 5
    )

    Section("Znak ery")
    val era = EraModifiers.of(m)
    if (era != null) SheetBody("${era.name}: ${era.description}", color = Aaa.Gold, maxLines = 2)
    else EraModifiers.ALL.forEach { e ->
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(e.name, style = AaaType.Label.copy(fontSize = 11.sp, color = Aaa.TextHi))
                Text(e.description, style = AaaType.Label.copy(fontSize = 9.5.sp, color = Aaa.TextLow), maxLines = 2)
            }
            GhostButton("Wybierz", onClick = { viewModel.chooseEra(e.id) }, height = 30.dp)
        }
    }

    Section("Sukcesja")
    if (m.heirName != null) SheetBody("Następca: ${m.heirName}. W nowej erze poprowadzi sektę (+10% Qi).", color = Aaa.Gold, maxLines = 2)
    else {
        RuleHint("Po pierwszym roku ery wybierz następcę. Pominięci ambitni (lojalność < 70, silniejsi od następcy) mogą odejść.", color = Aaa.TextLow, maxLines = 3)
        var pick by remember { mutableStateOf<String?>(null) }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            ui.disciples.sortedByDescending { it.combatPower }.take(8).forEach { d ->
                val on = pick == d.id
                SelectChip(d.name.substringBefore(" (").substringBefore(" "), on, Aaa.Gold) { pick = d.id }
            }
        }
        pick?.let { id -> GoldCta("Ogłoś następcę", onClick = { viewModel.succession(id) }, modifier = Modifier.fillMaxWidth()) }
    }

    Section("Nowa era")
    var difficulty by remember { mutableStateOf(m.difficulty) }
    var iron by remember { mutableStateOf(m.ironWill) }
    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Difficulty.entries.forEach { d ->
            val on = d == difficulty
            SelectChip(d.polishName, on, Aaa.Azure) { difficulty = d }
        }
        val on = iron
        SelectChip("Żelazna Wola", on, Aaa.Cinnabar) { iron = !iron }
    }
    if (iron) SheetBody("Żelazna Wola: jeden zapis, bez wczytywania kopii.", color = Aaa.Cinnabar)
    if (!ui.rebirthAvailable) {
        GoldCta("Odrodzenie po zakończeniu opowieści lub 1200 dniach", enabled = false, onClick = {}, modifier = Modifier.fillMaxWidth())
    } else {
        ConfirmCta(listOf("Odródź sektę (+${ui.essenceOnRebirth} Esencji)", "Na pewno? Sekta zniknie", "Tak – zacznij nową erę"), Modifier.fillMaxWidth()) {
            viewModel.rebirth(difficulty, iron)
        }
    }
}

@Composable
private fun TreeTab(ui: WuxiaUiState, viewModel: WuxiaGameViewModel) {
    KvRow("Esencja Niebios", "${ui.meta.essence}")
    RuleHint("Węzły gałęzi otwiera się po kolei; poziom N kosztuje N Esencji. Węzeł kupiony pierwszego dnia ery daje też premię startową.", color = Aaa.TextLow, maxLines = 3)
    HeavenTree.BRANCHES.forEach { (branch, sp) ->
        Section("$branch · +${sp.second.toInt()}% ${sp.first.label} za poziom")
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            HeavenTree.NODES.filter { it.branch == branch }.forEach { n ->
                val owned = HeavenTree.owned(ui.meta, n.id)
                val prevOwned = n.tier == 1 || HeavenTree.owned(ui.meta, "${branch.lowercase()}_${n.tier - 1}")
                val can = !owned && prevOwned && ui.meta.essence >= n.cost
                Text(
                    "${n.tier}",
                    style = AaaType.Label.copy(fontSize = 10.sp, color = if (owned) Aaa.Ink0 else Aaa.TextHi),
                    modifier = Modifier
                        .weight(1f)
                        .background(if (owned) Aaa.Gold else if (can) Aaa.Ink4 else Aaa.Ink2)
                        .border(1.dp, if (can) Aaa.Gold else Aaa.Hairline)
                        .clickable(enabled = can, role = Role.Button) { viewModel.buyHeavenNode(n.id) }
                        .padding(vertical = 8.dp)
                )
            }
        }
    }
}

@Composable
private fun AchievementsTab(ui: WuxiaUiState, viewModel: WuxiaGameViewModel) {
    val got = ui.achievements.count { it.unlocked }
    KvRow("Zdobyte", "$got / ${ui.achievements.size}")
    ui.achievements.sortedByDescending { it.unlocked }.forEach { a ->
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text((if (a.unlocked) "🏅 " else "· ") + a.name, style = AaaType.Label.copy(fontSize = 11.sp, color = if (a.unlocked) Aaa.GoldHi else Aaa.TextLow))
                Text(a.description, style = AaaType.Label.copy(fontSize = 9.5.sp, color = Aaa.TextLow), maxLines = 2)
            }
            if (a.unlocked && a.title) GhostButton(if (ui.meta.title == a.name) "Tytuł ✓" else "Tytuł", onClick = { viewModel.setTitle(if (ui.meta.title == a.name) null else a.id) }, height = 30.dp)
        }
    }
}

@Composable
private fun DailyTab(ui: WuxiaUiState, viewModel: WuxiaGameViewModel) {
    val m = ui.meta
    RuleHint("Trzy cele na każdy prawdziwy dzień. Cykl 7 dni nie zeruje się, gdy pominiesz dzień – tylko czeka.", maxLines = 2)
    KvRow("Cykl", "dzień ${m.streak % DailyGoals.CYCLE + 1} z ${DailyGoals.CYCLE} (łącznie ${m.streak})")
    if (m.goals.isEmpty()) SheetBody("Cele pojawią się za chwilę.", color = Aaa.TextLow)
    m.goals.forEach { g ->
        val name = DailyGoals.KINDS[g.kind]?.first ?: g.kind
        KvRow(name, "${g.progress}/${g.target}", valueColor = if (g.done) Aaa.Jade else Aaa.TextMid)
    }
    GhostButton("Złóż ofiarę przodkom · ${DailyGoals.OFFERING_COST}", onClick = { viewModel.offerToAncestors() }, modifier = Modifier.fillMaxWidth(), height = 36.dp)
}
