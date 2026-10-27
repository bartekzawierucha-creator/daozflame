package com.example.ui.game

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import com.example.model.ExpNode
import com.example.model.ExpNodeType
import com.example.model.WorldRegion
import com.example.ui.aaa.Aaa
import com.example.ui.aaa.AaaType

private fun nodeColor(t: ExpNodeType) = when (t) {
    ExpNodeType.FIGHT, ExpNodeType.TRAP -> Aaa.Cinnabar
    ExpNodeType.HERB, ExpNodeType.REST -> Aaa.Jade
    ExpNodeType.TREASURE -> Aaa.Gold
    ExpNodeType.SHRINE -> Aaa.Azure
    ExpNodeType.FLAME_SOURCE -> Aaa.Ember
    ExpNodeType.BOSS -> Aaa.Violet
}

private fun nodeHint(t: ExpNodeType) = when (t) {
    ExpNodeType.FIGHT -> "Strażnicy krainy. Łupy i kamienie, ale rany."
    ExpNodeType.HERB -> "Zioła krainy do pierścienia."
    ExpNodeType.TREASURE -> "Minerały i kamienie."
    ExpNodeType.SHRINE -> "Medytacja: Qi dla ucznia po powrocie."
    ExpNodeType.TRAP -> "Formacja pułapki. Wytrzymała obrona łagodzi rany."
    ExpNodeType.REST -> "Odzyskanie 30% HP."
    ExpNodeType.FLAME_SOURCE -> "Poskromienie płomienia w 3 fazach. Liczy się atak i obrona ucznia."
    ExpNodeType.BOSS -> "Legowisko bestii."
}

/** Planowanie i przebieg wyprawy (mapa węzłów). */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ExpeditionPanel(region: WorldRegion?, ui: WuxiaUiState, viewModel: WuxiaGameViewModel, onClose: () -> Unit) {
    val run = ui.expedition
    val runRegion = run?.let { r -> ui.regions.find { it.id == r.regionId } } ?: region
    LegacyPanel(title = "Wyprawa · ${runRegion?.name ?: "—"}", onClose = onClose) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            when {
                run != null -> RunView(run, ui, viewModel)
                runRegion != null -> PlanView(runRegion, ui, viewModel)
                else -> SheetBody("Wybierz krainę na mapie.")
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PlanView(region: WorldRegion, ui: WuxiaUiState, viewModel: WuxiaGameViewModel) {
    val healthy = ui.disciples.filter { it.currentHp >= it.maxHp / 3 }.sortedByDescending { it.combatPower }
    var pick by remember { mutableStateOf(healthy.firstOrNull()?.id) }
    SheetBody(
        "Wyprawa to 3–5 rozwidleń, a na końcu legowisko strażnika. Z każdej warstwy wybierasz jedną ścieżkę. " +
            "Odwrót przed bossem zachowuje połowę łupów. Tryb Auto wybiera bezpieczniej, ale przynosi 20% mniej.",
        maxLines = 5
    )
    ui.flameTrails[region.id]?.let { SheetBody("🜂 Trop płomienia: $it", color = Aaa.Ember, maxLines = 2) }
    KvRow("Miejsca w pierścieniu", "${ui.lootSlots} rodzajów łupów", valueColor = Aaa.TextMid)
    if (!ui.hasRing) {
        GhostButton("Wykuj Pierścień Przestrzenny · 2000 + 5× Gwiezdne Żelazo", onClick = { viewModel.craftRing() }, modifier = Modifier.fillMaxWidth(), height = 36.dp)
    }
    Overline("Uczeń")
    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        healthy.take(12).forEach { d ->
            val on = d.id == pick
            Text(
                "${d.name.substringBefore(" (").substringBefore(" ")} · ${d.combatPower}",
                style = AaaType.Label.copy(fontSize = 10.5.sp, color = if (on) Aaa.Ink0 else Aaa.TextHi),
                modifier = Modifier
                    .background(if (on) Aaa.Gold else Aaa.Ink3)
                    .clickable(role = Role.Button) { pick = d.id }
                    .padding(horizontal = 8.dp, vertical = 5.dp)
            )
        }
    }
    if (healthy.isEmpty()) SheetBody("Wszyscy uczniowie są ranni.", color = Aaa.Cinnabar)
    val id = pick
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        GhostButton("Auto", enabled = id != null, onClick = { id?.let { viewModel.beginExpedition(region.id, it, auto = true) } }, modifier = Modifier.weight(1f))
        GoldCta("Wyrusz", enabled = id != null, onClick = { id?.let { viewModel.beginExpedition(region.id, it, auto = false) } }, modifier = Modifier.weight(1.6f))
    }
    Overline("Szybkie starcie")
    SheetBody("Bez mapy: od razu walka ze strażnikiem krainy.", color = Aaa.TextLow)
    GhostButton("Starcie ze strażnikiem", enabled = id != null && region.bossBeast != null, onClick = { id?.let { viewModel.startExpedition(region.id, it) } }, modifier = Modifier.fillMaxWidth())
}

@Composable
private fun RunView(run: com.example.model.ExpeditionRun, ui: WuxiaUiState, viewModel: WuxiaGameViewModel) {
    val d = ui.disciples.find { it.id == run.discipleId }
    KvRow("Uczeń", d?.name?.substringBefore(" (") ?: "—")
    KvRow("HP", "${run.hp} / ${d?.maxHp ?: run.hp}", valueColor = if (d != null && run.hp < d.maxHp / 3) Aaa.Cinnabar else Aaa.Jade)
    ThinBar(if (d != null) run.hp.toFloat() / d.maxHp else 1f, Aaa.Jade)
    KvRow("Warstwa", "${(run.layer + 1).coerceAtMost(run.layers.size)} / ${run.layers.size}")
    val loot = run.loot.filterKeys { !it.startsWith("@") }
    KvRow("Łupy", if (loot.isEmpty() && run.stones == 0) "brak" else "${loot.size}/${ui.lootSlots} rodzajów · ${run.stones} kam.", valueColor = Aaa.TextMid)
    if (run.loot.keys.any { it.startsWith("@flame:") }) SheetBody("🜂 Poskromiony płomień jedzie z wyprawą!", color = Aaa.Ember)

    run.layers.forEachIndexed { i, layer ->
        val current = i == run.layer && !run.atBoss
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("${i + 1}", style = AaaType.Numeric.copy(fontSize = 11.sp, color = if (current) Aaa.GoldHi else Aaa.TextLow))
            layer.forEachIndexed { j, node ->
                NodeTile(node, enabled = current, modifier = Modifier.weight(1f)) { viewModel.chooseNode(j) }
            }
        }
    }
    if (run.layer >= run.layers.size && !run.atBoss) {
        GoldCta("Wejdź do legowiska", onClick = { viewModel.enterLair() }, modifier = Modifier.fillMaxWidth())
    }
    if (!run.atBoss) GhostButton("Odwrót (połowa łupów)", danger = true, onClick = { viewModel.retreatExpedition() }, modifier = Modifier.fillMaxWidth())
    Overline("Dziennik")
    run.log.takeLast(8).reversed().forEach { SheetBody(it, color = Aaa.TextMid, maxLines = 2) }
}

@Composable
private fun NodeTile(node: ExpNode, enabled: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val c = nodeColor(node.type)
    Column(
        modifier
            .background(if (enabled) c.copy(alpha = 0.18f) else Aaa.Ink3.copy(alpha = 0.6f))
            .border(1.dp, if (enabled) c else Aaa.Hairline)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text("${node.type.glyph} ${node.type.polishName}", style = AaaType.Label.copy(fontSize = 11.sp, color = if (enabled) Aaa.TextHi else Aaa.TextLow))
        if (enabled) Text(nodeHint(node.type), style = AaaType.Label.copy(fontSize = 9.sp, color = Aaa.TextMid), maxLines = 3)
    }
}

/** Wieża Prób: piętra coraz silniejszych strażników, 3 próby na dzień gry. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TowerPanel(ui: WuxiaUiState, viewModel: WuxiaGameViewModel, onClose: () -> Unit) {
    LegacyPanel(title = "Wieża Prób", onClose = onClose) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("Piętro ${ui.towerFloor}", fontFamily = BrushFont, color = Aaa.GoldHi, fontSize = 28.sp)
            SheetBody("Każde piętro to strażnik silniejszy o 8%. Co dziesiąte piętro daje Pigułkę Przełomu. Próby odnawiają się co dzień gry.", maxLines = 3)
            KvRow("Próby dziś", "${ui.towerAttemptsLeft} / 3", valueColor = if (ui.towerAttemptsLeft == 0) Aaa.Cinnabar else Aaa.Jade)
            val (hp, atk, def) = ui.towerNext
            KvRow("Strażnik piętra ${ui.towerFloor + 1}", "HP $hp · atak $atk · obrona $def", valueColor = Aaa.TextMid)
            Overline("Wyślij ucznia")
            ui.disciples.sortedByDescending { it.combatPower }.take(8).forEach { d ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(d.name.substringBefore(" ("), style = AaaType.Label.copy(fontSize = 11.sp, color = Aaa.TextHi), maxLines = 1)
                        Text("atak ${d.attack} · obrona ${d.defense} · HP ${d.maxHp}", style = AaaType.Label.copy(fontSize = 9.5.sp, color = Aaa.TextLow))
                    }
                    GhostButton("Próba", enabled = ui.towerAttemptsLeft > 0, onClick = { viewModel.climbTower(d.id) }, height = 32.dp)
                }
            }
        }
    }
}
