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
    SimplePanel(title = "Wyprawa · ${runRegion?.name ?: "—"}", onClose = onClose) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
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
    Section("Uczeń")
    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        healthy.take(12).forEach { d ->
            val on = d.id == pick
            SelectChip("${d.name.substringBefore(" (").substringBefore(" ")} · ${d.combatPower}", on, Aaa.Gold) { pick = d.id }
        }
    }
    if (healthy.isEmpty()) SheetBody("Wszyscy uczniowie są ranni.", color = Aaa.Cinnabar)
    val id = pick
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        GhostButton("Auto", enabled = id != null, onClick = { id?.let { viewModel.beginExpedition(region.id, it, auto = true) } }, modifier = Modifier.weight(1f))
        GoldCta("Wyrusz", enabled = id != null, onClick = { id?.let { viewModel.beginExpedition(region.id, it, auto = false) } }, modifier = Modifier.weight(1.6f))
    }
    Section("Szybkie starcie")
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
    Section("Dziennik")
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
    SimplePanel(title = "Wieża Prób", onClose = onClose) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Piętro ${ui.towerFloor}", fontFamily = BrushFont, color = Aaa.GoldHi, fontSize = 28.sp)
            SheetBody("Każde piętro to strażnik silniejszy o 8%. Co dziesiąte piętro daje Pigułkę Przełomu. Próby odnawiają się co dzień gry.", maxLines = 3)
            KvRow("Próby dziś", "${ui.towerAttemptsLeft} / 3", valueColor = if (ui.towerAttemptsLeft == 0) Aaa.Cinnabar else Aaa.Jade)
            val (hp, atk, def) = ui.towerNext
            KvRow("Strażnik piętra ${ui.towerFloor + 1}", "HP $hp · atak $atk · obrona $def", valueColor = Aaa.TextMid)
            Section("Wyślij ucznia")
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

// ─────────────────────────────────────────────────────────────────────────────
//  Etap 5b – obrona, wywiad, wojna i kronika świata
// ─────────────────────────────────────────────────────────────────────────────

enum class SectsTab(val label: String) { DEFENSE("Obrona"), SPIES("Wywiad"), WAR("Wojna i honor"), CHRONICLE("Kronika") }

@Composable
fun SectsPanel(ui: WuxiaUiState, viewModel: WuxiaGameViewModel, sect: com.example.model.RivalSect?, initial: SectsTab, onClose: () -> Unit) {
    var tab by remember { mutableStateOf(initial) }
    TabbedPanel(
        title = sect?.name ?: "Sekty świata",
        tabs = SectsTab.entries.map { it.label },
        selected = tab.ordinal,
        onSelect = { tab = SectsTab.entries[it] },
        onClose = onClose,
        header = {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                ui.raid?.let { StateTag("⚠ Najazd za ${(it.arriveDay - ui.currentDay).coerceAtLeast(0)} d.", Aaa.Cinnabar) }
                StateTag("Formacja ${ui.formationNodes.size}/7", Aaa.Gold)
                if (ui.spies.isNotEmpty()) StateTag("Szpiedzy ${ui.spies.size}", Aaa.Violet)
                sect?.takeIf { it.nemesisLevel > 0 }?.let { StateTag("☠ Nemezis ${it.nemesisLevel}", Aaa.Cinnabar) }
            }
        }
    ) {
        when (tab) {
            SectsTab.DEFENSE -> DefenseTab(ui, viewModel)
            SectsTab.SPIES -> SpiesTab(ui, viewModel, sect)
            SectsTab.WAR -> WarTab(ui, viewModel, sect)
            SectsTab.CHRONICLE -> {
                WorldStatus(ui)
                if (ui.chronicle.isEmpty()) SheetBody("Świat jeszcze milczy.", color = Aaa.TextLow)
                ui.chronicle.reversed().forEach { SheetBody(it, color = Aaa.TextMid, maxLines = 3) }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DefenseTab(ui: WuxiaUiState, viewModel: WuxiaGameViewModel) {
    val raid = ui.raid
    if (raid != null) {
        val days = raid.arriveDay - ui.currentDay
        SheetBody("⚠ ${raid.sectName} uderzy ${if (days <= 0) "dziś" else "za $days dni"}. Siła na bramę ok. ${raid.strength / 3}.", color = Aaa.Cinnabar, maxLines = 3)
    } else {
        SheetBody("Bramy są spokojne. Wrogie sekty zapowiadają najazd 2 dni wcześniej. Najazd rozstrzyga się w 3 rundach – po jednej na bramę; ostatnie rozstawienie działa także, gdy cię nie ma.", maxLines = 5)
    }
    com.example.game.engine.Raids.GATES.forEach { gate ->
        val guardId = ui.gates[gate]
        val guard = ui.disciples.find { it.id == guardId }
        val power = ui.gatePowers[gate] ?: 0
        val safe = raid == null || power >= raid.strength / 3
        Column(
            Modifier
                .fillMaxWidth()
                .background(Aaa.Ink3.copy(alpha = 0.8f))
                .border(1.dp, if (safe) Aaa.Hairline else Aaa.Cinnabar)
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            KvRow(gate, "moc $power", valueColor = if (safe) Aaa.Jade else Aaa.Cinnabar)
            SheetBody(guard?.let { "Straż: ${it.name.substringBefore(" (")}" } ?: "Bez straży – broni tylko mur.", color = Aaa.TextMid)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                ui.disciples.sortedByDescending { it.combatPower }.take(6).forEach { d ->
                    val on = d.id == guardId
                    SelectChip(d.name.substringBefore(" (").substringBefore(" "), on, Aaa.Gold) { viewModel.guardGate(gate, if (on) null else d.id) }
                }
            }
        }
    }
    Section("Wielka Formacja ${ui.formationNodes.size}/7")
    KvRow("Mnożnik obrony bram", "×%.2f".format(ui.formationFactor))
    if (ui.formationNodes.isNotEmpty()) SheetBody(ui.formationNodes.joinToString(" ") { it.glyph }, color = Aaa.Gold)
    SheetBody("Każdy węzeł +8% obrony. Pięć podstawowych żywiołów: +20% obrony i +5% Qi sekty. Komplet 7 węzłów: kolejne +10%.", color = Aaa.TextLow, maxLines = 3)
    ui.formationNextCost?.let { cost ->
        SheetBody("Następny węzeł: $cost", color = Aaa.TextHi)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            com.example.model.SpiritElement.BASIC.forEach { e ->
                GhostButton("${e.glyph} ${e.polishName}", onClick = { viewModel.placeFormationNode(e) }, height = 32.dp)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SpiesTab(ui: WuxiaUiState, viewModel: WuxiaGameViewModel, sect: com.example.model.RivalSect?) {
    if (sect == null) { SheetBody("Wybierz sektę."); return }
    val scouted = ui.intel[sect.id]
    KvRow("Siła sekty", if (scouted != null) "${sect.strength} (zwiad z dnia $scouted)" else "nieznana – wyślij zwiad", valueColor = Aaa.TextMid)
    if (sect.nemesisLevel > 0) KvRow("Nemezis", "poziom ${sect.nemesisLevel}", valueColor = Aaa.Cinnabar)
    if (ui.spies.isNotEmpty()) {
        Section("Na misjach")
        ui.spies.forEach { m ->
            val who = ui.disciples.find { it.id == m.discipleId }?.name?.substringBefore(" (") ?: "?"
            val where = ui.rivalSects.find { it.id == m.sectId }?.name ?: "?"
            KvRow("$who · ${m.type.polishName}", "$where · wraca dnia ${m.returnDay}", valueColor = Aaa.TextMid)
        }
    }
    var type by remember { mutableStateOf(com.example.model.SpyType.SCOUT) }
    Section("Nowa misja")
    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        com.example.model.SpyType.entries.forEach { t ->
            val on = t == type
            SelectChip(t.polishName, on, Aaa.Violet) { type = t }
        }
    }
    SheetBody(type.description + " Misja trwa 3 dni. Wykrycie: relacje −20 i ranny szpieg.", color = Aaa.TextLow, maxLines = 3)
    val busy = ui.spies.map { it.discipleId }.toSet()
    ui.disciples.filter { it.id !in busy }.sortedByDescending { it.comprehension }.take(6).forEach { d ->
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("${d.name.substringBefore(" (")} · zrozumienie ${d.comprehension}", style = AaaType.Label.copy(fontSize = 11.sp, color = Aaa.TextHi), modifier = Modifier.weight(1f), maxLines = 1)
            GhostButton("Wyślij", onClick = { viewModel.sendSpy(d.id, sect.id, type) }, height = 32.dp)
        }
    }
}

@Composable
private fun WarTab(ui: WuxiaUiState, viewModel: WuxiaGameViewModel, sect: com.example.model.RivalSect?) {
    if (sect == null) { SheetBody("Wybierz sektę."); return }
    val atWar = sect.status == com.example.model.DiplomaticStatus.TOTAL_WAR
    Section("Bitwa")
    val army = ui.disciples.filter { it.currentHp > it.maxHp / 3 }.sortedByDescending { it.combatPower }.take(5)
    SheetBody(
        if (atWar) "Do bitwy ruszy 5 najsilniejszych zdrowych uczniów: ${army.joinToString { it.name.substringBefore(" (").substringBefore(" ") }}. Zwycięstwo daje kamienie i terytorium."
        else "Bitwy toczy się tylko w stanie wojny.",
        maxLines = 4
    )
    GoldCta("Bitwa z ${sect.name}", enabled = atWar && army.isNotEmpty(), onClick = { viewModel.sectBattle(sect.id, army.map { it.id }) }, modifier = Modifier.fillMaxWidth())

    Section("Honorowy pojedynek")
    val champ = ui.disciples.maxByOrNull { it.combatPower }
    var confirmDeath by remember(sect.id) { mutableStateOf(false) }
    if (champ != null) {
        SheetBody("Czempion: ${champ.name.substringBefore(" (")}. Pojedynek do pierwszej krwi kosztuje tylko rany. Na śmierć – przegrany ginie.", maxLines = 3)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            GhostButton("Do pierwszej krwi", onClick = { viewModel.honorDuel(sect.id, champ.id, toDeath = false, confirmed = false) }, modifier = Modifier.weight(1f))
            if (!confirmDeath) {
                GhostButton("Na śmierć…", danger = true, onClick = { confirmDeath = true }, modifier = Modifier.weight(1f))
            } else {
                GoldCta("Potwierdzam", danger = true, onClick = {
                    confirmDeath = false
                    viewModel.honorDuel(sect.id, champ.id, toDeath = true, confirmed = true)
                }, modifier = Modifier.weight(1f))
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Etap 5c – mgła, tajne królestwa, Imperium i katastrofy
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun androidx.compose.foundation.layout.ColumnScope.FogSheet(
    r: WorldRegion,
    ui: WuxiaUiState,
    champ: com.example.model.Disciple?,
    viewModel: WuxiaGameViewModel,
    onRealms: () -> Unit
) {
    val index = ui.regions.indexOfFirst { it.id == r.id }
    val reachable = index <= 0 || ui.regions[index - 1].id in ui.discoveredRegionIds
    val cost = com.example.game.engine.Fog.scoutCost(index)
    SheetTitle("Kraina we mgle", size = 21)
    SheetBody(
        if (reachable) "Zwiadowca odkryje krainę i jej strażnika. Bezpiecznie przy mocy ≥ ${r.dangerLevel * 60}; słabszy może wrócić ranny."
        else "Za daleko. Najpierw odkryj krainę bliżej sekty.",
        maxLines = 3
    )
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        GhostButton("Tajne", onClick = onRealms, modifier = Modifier.weight(1f))
        GoldCta(
            "Zwiad",
            trailing = "$cost",
            enabled = reachable && champ != null && ui.spiritStones >= cost,
            onClick = { champ?.let { viewModel.scoutRegion(r.id, it.id) } },
            modifier = Modifier.weight(1.7f)
        )
    }
}

@Composable
fun SecretRealmsPanel(ui: WuxiaUiState, viewModel: WuxiaGameViewModel, onClose: () -> Unit) {
    SimplePanel(title = "Tajne królestwa", onClose = onClose) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            SheetBody("Każde królestwo otwiera się co 40 dni na 5 dni. Bariera wpuszcza tylko młodszych kultywatorów – jedno wejście na otwarcie.", maxLines = 3)
            ui.secretRealms.forEach { r ->
                Column(
                    Modifier
                        .fillMaxWidth()
                        .background(Aaa.Ink3.copy(alpha = 0.8f))
                        .border(1.dp, if (r.open) Aaa.Violet else Aaa.Hairline)
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(r.name, style = AaaType.Label.copy(fontSize = 12.sp, color = if (r.open) Aaa.Violet else Aaa.TextHi))
                    SheetBody(r.description, color = Aaa.TextMid, maxLines = 2)
                    KvRow("Bariera", "do: ${r.maxRealm}", valueColor = Aaa.TextMid)
                    KvRow("Łupy", r.loot, valueColor = Aaa.Gold)
                    when {
                        r.visited -> SheetBody("Sekta już tu była w tym otwarciu.", color = Aaa.TextLow)
                        !r.open -> SheetBody("Otwarcie za ${r.inDays} dni.", color = Aaa.TextLow)
                        else -> {
                            val allowed = ui.disciples.filter { d ->
                                val realm = d.majorRealm?.polishName
                                realm != null && com.example.model.MajorRealm.entries.indexOfFirst { it.polishName == realm } <=
                                    com.example.model.MajorRealm.entries.indexOfFirst { it.polishName == r.maxRealm }
                            }.sortedByDescending { it.combatPower }.take(4)
                            if (allowed.isEmpty()) SheetBody("Żaden uczeń nie przejdzie przez barierę.", color = Aaa.Cinnabar)
                            allowed.forEach { d ->
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("${d.name.substringBefore(" (")} · ${d.combatPower}", style = AaaType.Label.copy(fontSize = 11.sp, color = Aaa.TextHi), modifier = Modifier.weight(1f), maxLines = 1)
                                    GhostButton("Wejdź", onClick = { viewModel.enterSecretRealm(r.id, d.id) }, height = 32.dp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/** Nagłówek kroniki: Imperium i katastrofa. */
@Composable
fun WorldStatus(ui: WuxiaUiState) {
    KvRow(
        "Imperium",
        if (ui.empireFallen) "upadło – brak danin" else "łaska dworu ${ui.imperialFavor}",
        valueColor = when {
            ui.empireFallen -> Aaa.TextMid
            ui.imperialFavor < -30 -> Aaa.Cinnabar
            else -> Aaa.Gold
        }
    )
    ui.catastrophe?.let { c ->
        val now = ui.currentDay in c.startDay..c.endDay
        SheetBody(
            if (now) "🌩 ${c.type.polishName} do dnia ${c.endDay}: ${c.type.description}"
            else "🔭 Za ${c.startDay - ui.currentDay} dni: ${c.type.polishName}. ${c.type.description}",
            color = if (now) Aaa.Cinnabar else Aaa.Ember,
            maxLines = 3
        )
    }
}
