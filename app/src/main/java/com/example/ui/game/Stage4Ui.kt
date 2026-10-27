package com.example.ui.game

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import com.example.model.ContractType
import com.example.ui.aaa.Aaa
import com.example.ui.aaa.AaaType

/** Zakładki panelu gospodarki (Etap 4). */
enum class EconomyTab(val label: String) {
    MARKET("Targ"),
    CONTRACTS("Kontrakty"),
    FIELDS("Pola"),
    VEIN("Żyła"),
    WORKSHOP("Warsztat"),
    FLAMES("Płomienie")
}

private fun short(name: String, words: Int = 2) = name.split(" ").take(words).joinToString(" ")

/** Panel gospodarki: targ, kontrakty, pola ziół, żyła duchowa, warsztat (eksperymenty, ulepszanie, talizmany) i Sala Płomieni. */
@Composable
fun EconomyPanel(ui: WuxiaUiState, viewModel: WuxiaGameViewModel, initial: EconomyTab, onClose: () -> Unit) {
    var tab by remember { mutableIntStateOf(initial.ordinal) }
    val e = ui.economy
    TabbedPanel(
        title = "Gospodarka sekty",
        tabs = EconomyTab.entries.map { it.label },
        selected = tab,
        onSelect = { tab = it },
        onClose = onClose,
        header = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                StateTag("${e.season.glyph} ${e.season.polishName} · dzień ${e.seasonDay}/30", Aaa.Jade)
                StateTag("Żyła ${e.veinLevel}", Aaa.Azure)
                if (e.blackOpen) StateTag("Czarny rynek", Aaa.Violet)
                ui.catastrophe?.takeIf { ui.currentDay in it.startDay..it.endDay }?.let { StateTag(it.type.polishName, Aaa.Ember) }
            }
        }
    ) {
        if (!e.enabled) {
            SheetBody("Gospodarka sekty jeszcze się nie obudziła.")
        } else when (EconomyTab.entries[tab]) {
            EconomyTab.MARKET -> MarketTab(ui, viewModel)
            EconomyTab.CONTRACTS -> ContractsTab(ui, viewModel)
            EconomyTab.FIELDS -> FieldsTab(ui, viewModel)
            EconomyTab.VEIN -> VeinTab(ui, viewModel)
            EconomyTab.WORKSHOP -> WorkshopTab(ui, viewModel)
            EconomyTab.FLAMES -> FlamesTab(ui, viewModel)
        }
    }
}

@Composable
private fun MarketTab(ui: WuxiaUiState, viewModel: WuxiaGameViewModel) {
    val e = ui.economy
    RuleHint(
        "Każda sprzedaż obniża cenę o 2% – ceny wracają z czasem. Jesienią kupcy płacą więcej. Kupić można tylko proste surowce.",
        maxLines = 3
    )
    Section("Sprzedaż")
    if (e.sellable.isEmpty()) SheetBody("Magazyn nie ma nic na sprzedaż.", color = Aaa.TextLow)
    e.sellable.forEach { row ->
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            ItemIcon(row.item)
            Column(Modifier.weight(1f)) {
                Text(row.item, style = AaaType.Label.copy(fontSize = 11.sp, color = Aaa.TextHi), maxLines = 1)
                Text("${row.have} szt. · ${row.sellPrice} kam./szt.", style = AaaType.Label.copy(fontSize = 9.5.sp, color = Aaa.TextLow))
            }
            GhostButton("1", onClick = { viewModel.sell(row.item, 1) }, height = 32.dp)
            if (row.have >= 10) GhostButton("10", onClick = { viewModel.sell(row.item, 10) }, height = 32.dp)
            GhostButton("🐫 ${row.have}", onClick = { viewModel.sendCaravan(row.item, row.have, null) }, height = 32.dp)
        }
    }
    Section("Zakup")
    e.buyable.forEach { row ->
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            ItemIcon(row.item)
            Column(Modifier.weight(1f)) {
                Text(row.item, style = AaaType.Label.copy(fontSize = 11.sp, color = Aaa.TextHi), maxLines = 1)
                Text("masz ${row.have} · ${row.buyPrice} kam./szt.", style = AaaType.Label.copy(fontSize = 9.5.sp, color = Aaa.TextLow))
            }
            GhostButton("+1", onClick = { viewModel.buy(row.item, 1) }, enabled = ui.spiritStones >= (row.buyPrice ?: 0), height = 32.dp)
            GhostButton("+10", onClick = { viewModel.buy(row.item, 10) }, enabled = ui.spiritStones >= 10 * (row.buyPrice ?: 0), height = 32.dp)
        }
    }
    if (e.caravans.isNotEmpty()) {
        Section("Karawany w drodze")
        e.caravans.forEach { c ->
            KvRow("${c.quantity}× ${short(c.item, 3)}", "wraca dnia ${c.returnDay} · ${c.unitPrice}/szt.", valueColor = Aaa.TextMid)
        }
    }
    Section("Czarny rynek")
    if (!e.blackOpen) {
        SheetBody("Zaułki milczą. Handlarze cieni pojawiają się nocą.", color = Aaa.TextLow)
    } else {
        SheetBody("Towar bez pytań o pochodzenie. Część to podróbki.", color = Aaa.Violet)
        e.blackOffers.forEachIndexed { i, o ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(o.item, style = AaaType.Label.copy(fontSize = 11.sp, color = Aaa.TextHi), modifier = Modifier.weight(1f), maxLines = 2)
                GhostButton("${o.price}", onClick = { viewModel.buyBlackMarket(i) }, enabled = ui.spiritStones >= o.price, height = 32.dp)
            }
        }
    }
}

@Composable
private fun ContractsTab(ui: WuxiaUiState, viewModel: WuxiaGameViewModel) {
    val e = ui.economy
    RuleHint("Tablica odświeża się co tydzień. Łowy kończą się same po pokonaniu bestii w danej krainie.", maxLines = 3)
    if (e.contracts.isEmpty()) SheetBody("Brak otwartych zleceń.", color = Aaa.TextLow)
    e.contracts.forEach { c ->
        InfoCard {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (c.type == ContractType.DELIVER) ItemIcon(c.item, 30.dp) else Text("⚔", fontSize = 20.sp, color = Aaa.Cinnabar)
                Text("${c.type.polishName} · ${c.client}", style = AaaType.Label.copy(fontSize = 12.sp, color = Aaa.GoldHi))
            }
            val what = if (c.type == ContractType.HUNT) {
                "Pokonaj bestię: ${ui.regions.find { it.id == c.regionId }?.name ?: "nieznana kraina"}"
            } else {
                "Dostarcz ${c.quantity}× ${c.item} (masz ${ui.inventory[c.item] ?: 0})"
            }
            SheetBody(what, color = Aaa.TextHi, maxLines = 2)
            KvRow("Nagroda", "${c.rewardStones} kam. · +${c.rewardReputation} rep.")
            KvRow("Termin", "dzień ${c.expiresDay}", valueColor = if (c.expiresDay - ui.currentDay <= 1) Aaa.Cinnabar else Aaa.TextMid)
            if (c.type == ContractType.DELIVER) {
                GoldCta(
                    "Dostarcz",
                    enabled = (ui.inventory[c.item] ?: 0) >= c.quantity,
                    onClick = { viewModel.fulfillContract(c.id) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FieldsTab(ui: WuxiaUiState, viewModel: WuxiaGameViewModel) {
    val e = ui.economy
    var seed by remember { mutableStateOf<String?>(null) }
    val chosen = seed?.takeIf { it in e.seedHerbs } ?: e.seedHerbs.firstOrNull()
    RuleHint(
        "Wiosną plony są podwójne, zimą ziemia zamarza. Sąsiednie poletka z tym samym ziołem dają +10% każde. " +
            "Żyła pozwala uprawiać zioła do ${e.fieldTier}. stopnia.",
        maxLines = 4
    )
    Section("Nasiona")
    if (e.seedHerbs.isEmpty()) {
        SheetBody("Brak nasion – jedna sztuka zioła w magazynie wystarczy na jedno poletko.", color = Aaa.TextLow)
    } else {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            e.seedHerbs.forEach { h -> SelectChip("${short(h)} ×${ui.inventory[h] ?: 0}", h == chosen, Aaa.Jade) { seed = h } }
        }
    }
    Section("Poletka ${e.fieldSize}×${e.fieldSize}")
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        e.fields.chunked(e.fieldSize).forEachIndexed { r, row ->
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                row.forEachIndexed { c, plot ->
                    val index = r * e.fieldSize + c
                    val ready = plot.herb != null && plot.readyDay <= ui.currentDay
                    Box(
                        Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .background(
                                when {
                                    plot.herb == null -> Aaa.Ink3
                                    ready -> Aaa.JadeDeep
                                    else -> Aaa.Ink4
                                }
                            )
                            .border(1.dp, if (ready) Aaa.Jade else Aaa.Hairline)
                            .clickable(role = Role.Button) {
                                if (plot.herb == null && chosen != null) viewModel.plantHerb(index, chosen)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        val label = when {
                            plot.herb == null -> "+"
                            ready -> "${short(plot.herb, 1)}\nzbiór"
                            else -> "${short(plot.herb, 1)}\n${plot.readyDay - ui.currentDay} dni"
                        }
                        Text(label, textAlign = TextAlign.Center, style = AaaType.Label.copy(fontSize = 8.5.sp, color = if (ready) Aaa.GoldHi else Aaa.TextMid))
                    }
                }
            }
        }
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        GoldCta("Zbierz plony", onClick = { viewModel.harvestFields() }, modifier = Modifier.weight(1.4f))
        if (e.fieldSize < 6) GhostButton("Powiększ · 5000", onClick = { viewModel.expandFields() }, enabled = ui.spiritStones >= 5_000, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun VeinTab(ui: WuxiaUiState, viewModel: WuxiaGameViewModel) {
    val e = ui.economy
    Text("${e.veinName} · poziom ${e.veinLevel}", fontFamily = BrushFont, color = Aaa.GoldHi, fontSize = 24.sp)
    ThinBar(e.veinLevel / 9f, Aaa.Azure)
    KvRow("Mnożnik Qi sekty", "×%.2f".format(e.veinQi))
    KvRow("Zioła na polach", "do ${e.fieldTier}. stopnia")
    KvRow("Dodatkowy slot edyktu", if (e.veinLevel >= 5) "tak" else "od poziomu 5", valueColor = Aaa.TextMid)
    KvRow("Łagodzenie tribulacji", if (e.veinLevel >= 9) "−10%" else "Smocza Żyła (9)", valueColor = Aaa.TextMid)
    Section("Pogłębianie")
    when {
        e.veinUpgradeDoneDay > 0 -> SheetBody("Mistrzowie formacji pracują pod górą. Koniec dnia ${e.veinUpgradeDoneDay}.")
        e.veinNextCost == null -> SheetBody("Żyła osiągnęła pełnię – Smocza Żyła.")
        else -> {
            SheetBody("Koszt: ${e.veinNextCost}", color = Aaa.TextHi, maxLines = 2)
            GoldCta("Pogłęb żyłę", onClick = { viewModel.upgradeVein() }, modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun WorkshopTab(ui: WuxiaUiState, viewModel: WuxiaGameViewModel) {
    val e = ui.economy
    // Eksperyment
    Section("Eksperyment alchemiczny")
    RuleHint("Połącz zioła w kotle (30 kamieni). Trafna mieszanka może odsłonić nową recepturę – reszta zostaje popiołem.", maxLines = 3)
    val herbs = ui.inventory.filter { it.value > 0 && it.key in com.example.game.content.ResourceCatalog.herbs.map { h -> h.name } }.keys.sorted()
    var main by remember { mutableStateOf<String?>(null) }
    var second by remember { mutableStateOf<String?>(null) }
    HerbPicker("Główne zioło", herbs, main) { main = it }
    HerbPicker("Drugie zioło (opcjonalne)", herbs.filter { it != main }, second) { second = if (second == it) null else it }
    GoldCta(
        "Eksperymentuj",
        enabled = main != null && ui.spiritStones >= 30,
        onClick = { main?.let { viewModel.experiment(it, second) } },
        modifier = Modifier.fillMaxWidth()
    )
    KvRow("Znane receptury", "${e.knownRecipeIds.size}")

    // Ulepszanie
    Section("Ulepszanie artefaktów")
    if (ui.vaultArtifacts.isEmpty()) SheetBody("Brak artefaktów w skarbcu.", color = Aaa.TextLow)
    ui.vaultArtifacts.forEachIndexed { i, a ->
        val up = e.upgrades.getOrNull(i)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Column(Modifier.weight(1f)) {
                Text("${a.name}${if (a.plus > 0) " +${a.plus}" else ""}", style = AaaType.Label.copy(fontSize = 11.sp, color = rarityColor(a.rarity)), maxLines = 1)
                Text("${a.quality.polishName} · atak ${a.attackBonus} · obrona ${a.defenseBonus}", style = AaaType.Label.copy(fontSize = 9.5.sp, color = Aaa.TextLow))
            }
            if (up != null) GhostButton("${up.second}% · ${up.third}", onClick = { viewModel.upgradeArtifact(i) }, height = 32.dp, danger = a.plus >= 6)
        }
    }
    RuleHint("Od +6 nieudane ulepszenie może skruszyć artefakt. Talizman Ochrony go ratuje.", color = Aaa.TextLow, maxLines = 2)

    // Talizmany
    Section("Talizmany (masz ${ui.inventory[com.example.game.engine.Market.SPIRIT_PAPER] ?: 0} papieru)")
    e.talismans.forEach { t ->
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Column(Modifier.weight(1f)) {
                Text("${t.name} ×${t.have}", style = AaaType.Label.copy(fontSize = 11.sp, color = Aaa.TextHi), maxLines = 1)
                Text(t.description, style = AaaType.Label.copy(fontSize = 9.5.sp, color = Aaa.TextMid), maxLines = 2)
                Text(t.cost, style = AaaType.Label.copy(fontSize = 9.sp, color = Aaa.TextLow), maxLines = 2)
            }
            GhostButton("Wykreśl", onClick = { viewModel.craftTalisman(t.name) }, height = 32.dp)
        }
    }
    val healing = ui.inventory["Talizman Uzdrowienia"] ?: 0
    val wounded = ui.disciples.filter { it.currentHp < it.maxHp }.minByOrNull { it.currentHp.toDouble() / it.maxHp }
    if (healing > 0 && wounded != null) {
        GhostButton("Ulecz: ${wounded.name.substringBefore(" (")}", onClick = { viewModel.healWithTalisman(wounded.id) }, modifier = Modifier.fillMaxWidth())
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun HerbPicker(label: String, herbs: List<String>, selected: String?, onPick: (String) -> Unit) {
    Text(label, style = AaaType.Label.copy(fontSize = 10.sp, color = Aaa.TextLow))
    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        herbs.forEach { h -> SelectChip(short(h), h == selected, Aaa.Ember) { onPick(h) } }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FlamesTab(ui: WuxiaUiState, viewModel: WuxiaGameViewModel) {
    val e = ui.economy
    RuleHint(
        "Płomień grzeje kocioł, palenisko kuźni albo ciało ucznia – nigdy dwa naraz. Wyższe pigułki i artefakty wymagają wyższej temperatury. " +
            "Wyjęcie płomienia z ciała kosztuje 3 poziomy oczyszczenia.",
        maxLines = 4
    )
    val bearer = ui.disciples.firstOrNull { it.id == ui.currentSelectedDiscipleId && it.flameId == null }
        ?: ui.disciples.filter { it.flameId == null }.maxByOrNull { it.combatPower }
    e.flames.forEach { f ->
        InfoCard {
            Text("#${f.position} ${f.name}", style = AaaType.Label.copy(fontSize = 12.sp, color = Aaa.Ember), maxLines = 1)
            KvRow("Ranga", f.rank)
            KvRow("Temperatura", "${f.temperature}°")
            KvRow("Oczyszczenie", "${f.refinement}/9")
            KvRow("Gdzie", f.where, valueColor = Aaa.TextMid)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                GhostButton("Kocioł", onClick = { viewModel.flameToCauldron(f.id) }, height = 32.dp)
                GhostButton("Kuźnia", onClick = { viewModel.flameToForge(f.id) }, height = 32.dp)
                if (bearer != null) GhostButton("Wchłoń: ${bearer.name.substringBefore(" (").substringBefore(" ")}", onClick = { viewModel.flameToDisciple(f.id, bearer.id) }, height = 32.dp)
                GhostButton("Oczyść", onClick = { viewModel.refineFlame(f.id) }, height = 32.dp)
            }
            Text("Nosiciel: od ${f.bearerRealm}", style = AaaType.Label.copy(fontSize = 9.sp, color = Aaa.TextLow))
        }
    }
}

/** Przyciski talizmanów w trakcie walki. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CombatTalismanBar(ui: WuxiaUiState, viewModel: WuxiaGameViewModel, modifier: Modifier = Modifier) {
    val names = listOf("Talizman Ognistej Kuli", "Talizman Złotej Tarczy", "Talizman Pieczęci", "Talizman Ucieczki")
    val have = names.filter { (ui.inventory[it] ?: 0) > 0 }
    if (have.isEmpty()) return
    FlowRow(modifier, horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        have.forEach { n ->
            GhostButton("${n.removePrefix("Talizman ")} ×${ui.inventory[n]}", onClick = { viewModel.combatTalisman(n) }, height = 34.dp)
        }
    }
}
