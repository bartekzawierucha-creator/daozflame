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
import com.example.model.DaoArt
import com.example.model.Disciple
import com.example.model.FacilityType
import com.example.ui.aaa.Aaa
import com.example.ui.aaa.AaaType

enum class SectTab(val label: String) { IDENTITY("Tożsamość"), ANCESTORS("Przodkowie"), PLACES("Miejsca"), CREATURES("Istoty"), CHRONICLE("Kronika") }

private fun firstName(d: Disciple?) = d?.name?.substringBefore(" (")?.substringBefore(" ") ?: "—"

/** Panel sekty: tożsamość, Sala Przodków, miejsca, istoty i kronika. */
@Composable
fun SectPanel(ui: WuxiaUiState, viewModel: WuxiaGameViewModel, initial: SectTab = SectTab.IDENTITY, onClose: () -> Unit) {
    var tab by remember { mutableStateOf(initial) }
    TabbedPanel(
        title = "Sekta · ${ui.sectLife.karmaPath}",
        tabs = SectTab.entries.map { it.label },
        selected = tab.ordinal,
        onSelect = { tab = SectTab.entries[it] },
        onClose = onClose,
        header = {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                StateTag("Karma ${ui.karma}", if (ui.karma >= 0) Aaa.Jade else Aaa.Cinnabar)
                StateTag("Akt ${ui.sectLife.storyAct.coerceIn(1, 4)}/4", Aaa.Gold)
                StateTag("Przodkowie ${ui.sectLife.ancestors.size}", Aaa.TextMid)
            }
        }
    ) {
        when (tab) {
            SectTab.IDENTITY -> IdentityTab(ui, viewModel)
            SectTab.ANCESTORS -> AncestorsTab(ui)
            SectTab.PLACES -> PlacesTab(ui, viewModel)
            SectTab.CREATURES -> CreaturesTab(ui, viewModel)
            SectTab.CHRONICLE -> {
                WorldStatus(ui)
                GhostButton(if (ui.sectLife.starsReadToday) "Gwiazdy już przemówiły" else "Odczytaj gwiazdy · 100", enabled = !ui.sectLife.starsReadToday,
                    onClick = { viewModel.readStars() }, modifier = Modifier.fillMaxWidth(), height = 36.dp)
                ui.chronicle.reversed().forEach { SheetBody(it, color = Aaa.TextMid, maxLines = 3) }
                if (ui.sectLife.chapters.isNotEmpty()) {
                    Section("Dawne rozdziały")
                    ui.sectLife.chapters.reversed().forEach { SheetBody(it, color = Aaa.TextLow, maxLines = 5) }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun IdentityTab(ui: WuxiaUiState, viewModel: WuxiaGameViewModel) {
    val life = ui.sectLife
    Text(life.karmaPath, fontFamily = BrushFont, color = Aaa.GoldHi, fontSize = 24.sp)
    KvRow("Karma", "${ui.karma}", valueColor = if (ui.karma >= 0) Aaa.Jade else Aaa.Cinnabar)
    RuleHint("Karma ≥ 50: Sekta Prawa (+20% reputacji). Karma ≤ −50: Sekta Demoniczna (+15% obrażeń, −10% reputacji, klątwy).", color = Aaa.TextLow, maxLines = 3)
    KvRow("Opowieść", when {
        life.storyEnding == 1 -> "zakończona: Pieczęć Światła"
        life.storyEnding == 2 -> "zakończona: Pakt z Demonem"
        life.storyEnding == 3 -> "zakończona: Odnowiona Pieczęć"
        life.storyAct == 0 -> "jeszcze milczy"
        else -> "akt ${life.storyAct.coerceAtMost(4)} z 4"
    })
    KvRow("Wybraniec Losu", firstName(ui.disciples.find { it.id == life.chosenId }))

    Section("Patron i świątynia")
    com.example.game.engine.Identity.PATRONS.forEach { p ->
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(p.name, style = AaaType.Label.copy(fontSize = 11.sp, color = if (p.id == life.patronId) Aaa.GoldHi else Aaa.TextHi))
                Text(p.description, style = AaaType.Label.copy(fontSize = 9.5.sp, color = Aaa.TextLow))
            }
            if (p.id != life.patronId) GhostButton("Poświęć", onClick = { viewModel.choosePatron(p.id) }, height = 30.dp)
        }
    }
    if (life.patronId != null) {
        GhostButton(if (life.templeLevel >= 5) "Świątynia w pełni" else "Rozbuduj świątynię (poz. ${life.templeLevel}) · ${life.templeNextCost}",
            enabled = life.templeLevel < 5, onClick = { viewModel.upgradeTemple() }, modifier = Modifier.fillMaxWidth(), height = 36.dp)
    }

    Section("Skarby sekty ${life.treasury.size}/5")
    RuleHint("Artefakty w skarbcu wzmacniają Qi całej sekty, ale rabusie mogą je wynieść przy przełamanych bramach.", color = Aaa.TextLow, maxLines = 2)
    life.treasury.forEachIndexed { i, a ->
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("${a.name} (${a.rarity})", style = AaaType.Label.copy(fontSize = 11.sp, color = rarityColor(a.rarity)), modifier = Modifier.weight(1f), maxLines = 1)
            GhostButton("Zdejmij", onClick = { viewModel.removeTreasure(i) }, height = 30.dp)
        }
    }
    if (life.treasury.size < 5) ui.vaultArtifacts.forEachIndexed { i, a ->
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(a.name + (a.spirit?.let { " · $it" } ?: ""), style = AaaType.Label.copy(fontSize = 10.5.sp, color = Aaa.TextMid), modifier = Modifier.weight(1f), maxLines = 1)
            GhostButton("Do skarbca", onClick = { viewModel.placeTreasure(i) }, height = 30.dp)
        }
    }
}

@Composable
private fun AncestorsTab(ui: WuxiaUiState) {
    val list = ui.sectLife.ancestors
    RuleHint("Każdy przodek od Fundamentów wzwyż daje sekcie +1% Qi (najwyżej +15%).", maxLines = 2)
    if (list.isEmpty()) SheetBody("Sala Przodków jest jeszcze pusta.", color = Aaa.TextLow)
    list.reversed().forEach { a ->
        InfoCard {
            Text("🕯 ${a.name}", style = AaaType.Label.copy(fontSize = 11.5.sp, color = Aaa.TextHi))
            Text("${a.realm} · zmarł dnia ${a.diedDay} · ${a.cause}", style = AaaType.Label.copy(fontSize = 9.5.sp, color = Aaa.TextLow))
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PlacesTab(ui: WuxiaUiState, viewModel: WuxiaGameViewModel) {
    val life = ui.sectLife
    Overline("Feng shui · %+.1f%% Qi".format(life.fengShuiTotal))
    RuleHint("Każde z 6 miejsc sekty ma swój żywioł. Budynek na zgodnym miejscu wzmacnia Qi, na wrogim – osłabia. Przeniesienie: 1500 kamieni, co 10 dni.", color = Aaa.TextLow, maxLines = 3)
    var first by remember { mutableStateOf<FacilityType?>(null) }
    FacilityType.entries.sortedBy { life.buildingSlots[it] ?: it.ordinal }.forEach { t ->
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Miejsce ${(life.buildingSlots[t] ?: t.ordinal) + 1}: ${t.polishName}", style = AaaType.Label.copy(fontSize = 11.sp, color = if (first == t) Aaa.GoldHi else Aaa.TextHi), modifier = Modifier.weight(1f))
            GhostButton(if (first == null) "Przenieś" else if (first == t) "Anuluj" else "Zamień", height = 30.dp, onClick = {
                val f = first
                when {
                    f == null -> first = t
                    f == t -> first = null
                    else -> { viewModel.swapBuildings(f, t); first = null }
                }
            })
        }
    }

    Section("Lochy pod sektą · piętro ${life.dungeonDepth}")
    RuleHint("Każde piętro silniejsze o 12%. Łupy: rudy, kryształy, fragmenty zwojów, czasem jajo bestii. 2 zejścia dziennie (zostało ${life.dungeonAttemptsLeft}).", color = Aaa.TextLow, maxLines = 3)
    ui.disciples.filter { it.currentHp > it.maxHp / 3 }.sortedByDescending { it.combatPower }.take(3).forEach { d ->
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("${d.name.substringBefore(" (")} · ${d.combatPower}", style = AaaType.Label.copy(fontSize = 11.sp, color = Aaa.TextHi), modifier = Modifier.weight(1f), maxLines = 1)
            GhostButton("Zejdź", enabled = life.dungeonAttemptsLeft > 0, onClick = { viewModel.delveDungeon(d.id) }, height = 30.dp)
        }
    }

    Section("Jaskinie odosobnienia · poziom ${life.caveLevel}/3")
    SheetBody("Odosobnienie daje Qi ×${3 + 0.25 * life.caveLevel}.", color = Aaa.TextLow)
    if (life.caveLevel < 3) GhostButton("Wykuj głębiej · ${com.example.game.engine.Library.caveCost(life.caveLevel)}", onClick = { viewModel.upgradeCave() }, modifier = Modifier.fillMaxWidth(), height = 36.dp)

    Section("Biblioteka")
    SheetBody("Skryba przepisze zwój do stopnia Mistycznego (3× papier duchowy).", color = Aaa.TextLow)
    ui.scrolls.forEach { sc ->
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(sc.removePrefix("Zwój: "), style = AaaType.Label.copy(fontSize = 10.5.sp, color = Aaa.TextHi), modifier = Modifier.weight(1f), maxLines = 1)
            GhostButton("Kopiuj", onClick = { viewModel.copyScroll(sc) }, height = 30.dp)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CreaturesTab(ui: WuxiaUiState, viewModel: WuxiaGameViewModel) {
    val life = ui.sectLife
    Section("Jaja bestii")
    val eggs = ui.inventory[com.example.game.engine.Creatures.EGG] ?: 0
    KvRow("W magazynie", "$eggs")
    life.eggs.forEach { KvRow("Wysiadywane", "wykluje się dnia ${it.hatchDay}", valueColor = Aaa.TextMid) }
    if (eggs > 0) GhostButton("Złóż jajo do wysiadywania", onClick = { viewModel.incubateEgg() }, modifier = Modifier.fillMaxWidth(), height = 36.dp)

    Section("Wierzchowce")
    RuleHint("Bestia z więzią ≥ 50% nosi jeźdźca: +5% obrażeń za każdy etap ewolucji (najwyżej 3).", color = Aaa.TextLow, maxLines = 2)
    val champ = ui.disciples.maxByOrNull { it.combatPower }
    ui.sectBeasts.forEach { b ->
        val rider = ui.disciples.find { it.id == life.mounts[b.id] }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("${b.emoji} ${b.name} · etap ${b.tier}/3", style = AaaType.Label.copy(fontSize = 11.sp, color = Aaa.TextHi), maxLines = 1)
                Text("więź ${b.intimacy}% · jeździec: ${firstName(rider)}", style = AaaType.Label.copy(fontSize = 9.5.sp, color = Aaa.TextLow))
            }
            if (rider != null) GhostButton("Zsiądź", onClick = { viewModel.mountBeast(b.id, null) }, height = 30.dp)
            else if (champ != null) GhostButton("Dosiądź: ${firstName(champ)}", enabled = b.intimacy >= 50, onClick = { viewModel.mountBeast(b.id, champ.id) }, height = 30.dp)
        }
    }

    Section("Fuzja i pożeranie płomieni")
    RuleHint("Fuzja: 30% szansy, że oba płomienie wybuchną; sukces daje +500° i +2 oczyszczenia. Pożeranie: silniejszy zjada słabszy bez ryzyka.", color = Aaa.TextLow, maxLines = 3)
    var a by remember { mutableStateOf<String?>(null) }
    var b by remember { mutableStateOf<String?>(null) }
    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        ui.flames.forEach { f ->
            val tag = when (f.id) { a -> "①"; b -> "②"; else -> "" }
            SelectChip("$tag ${f.name.split(" ").take(2).joinToString(" ")}" + if (f.id in life.flameSpirits) " ✦" else "", f.id == a || f.id == b) {
                when {
                    a == null || (b != null) -> { a = f.id; b = null }
                    f.id != a -> b = f.id
                }
            }
        }
    }
    val fa = a
    val fb = b
    if (fa != null && fb != null) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            GhostButton("① pożera ②", onClick = { viewModel.devourFlame(fa, fb); a = null; b = null }, modifier = Modifier.weight(1f), height = 36.dp)
            ConfirmCta(listOf("Fuzja…", "Na pewno? 30% wybuchu", "Ostatecznie!"), Modifier.weight(1f), resetKey = fa + fb) {
                viewModel.fuseFlames(fa, fb); a = null; b = null
            }
        }
    }
}

/** Życie ucznia: wiek, mistrz, towarzysz Dao, sztuka, zdrowie. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LifePanel(d: Disciple, ui: WuxiaUiState, viewModel: WuxiaGameViewModel, onClose: () -> Unit) {
    val life = ui.sectLife
    SimplePanel(title = "Życie · ${d.name.substringBefore(" (")}", onClose = onClose) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            val span = life.lifespans[d.id] ?: 0
            KvRow("Wiek", "${d.ageYears} / $span lat", valueColor = if (span - d.ageYears <= 5) Aaa.Cinnabar else Aaa.Gold)
            ThinBar(if (span > 0) d.ageYears.toFloat() / span else 0f, Aaa.Ember)
            SheetBody("Przełom do wyższego królestwa wydłuża życie.", color = Aaa.TextLow)
            if (d.id == life.chosenId) SheetBody("✋ Wybraniec Losu (+10% Qi)", color = Aaa.GoldHi)

            Section("Mistrz")
            val mentor = ui.disciples.find { it.id == d.mentorId }
            KvRow("Obecnie", mentor?.name?.substringBefore(" (") ?: "brak")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                ui.disciples.filter { it.id != d.id && it.combatPower > d.combatPower }.sortedByDescending { it.combatPower }.take(5).forEach { m ->
                    SelectChip(firstName(m), m.id == d.mentorId) { viewModel.assignMentor(d.id, if (m.id == d.mentorId) null else m.id) }
                }
            }

            Section("Towarzysz Dao")
            val pair = life.companions.find { it.a == d.id || it.b == d.id }
            if (pair != null) {
                val other = ui.disciples.find { it.id == (if (pair.a == d.id) pair.b else pair.a) }
                SheetBody("Związany z: ${other?.name?.substringBefore(" (")}. Dzieci: ${pair.children}. Co rok w sekcie może urodzić się dziecko.", maxLines = 3)
            } else {
                SheetBody("Związek Dao wymaga lojalności ≥ 60 u obojga.", color = Aaa.TextLow)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    ui.disciples.filter { o -> o.id != d.id && life.companions.none { it.a == o.id || it.b == o.id } }.take(6).forEach { o ->
                        SelectChip(firstName(o), false) { viewModel.bindCompanions(d.id, o.id) }
                    }
                }
            }

            Section("Sztuka Dao")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                DaoArt.entries.forEach { art -> SelectChip(art.polishName, d.art == art) { viewModel.assignArt(d.id, if (d.art == art) null else art) } }
            }
            d.art?.let { SheetBody(it.description, color = Aaa.TextLow, maxLines = 2) }

            Section("Zdrowie")
            val ill = d.illUntilDay >= ui.currentDay
            if (!ill && !d.cursed) SheetBody("Zdrowy.", color = Aaa.Jade)
            if (ill) {
                SheetBody("Gorączka meridianów do dnia ${d.illUntilDay} (Qi ×0,7).", color = Aaa.Cinnabar)
                GhostButton("Lecz · 300 albo Talizman Uzdrowienia", onClick = { viewModel.cure(d.id) }, modifier = Modifier.fillMaxWidth(), height = 36.dp)
            }
            if (d.cursed) {
                SheetBody("Klątwa (Qi ×0,8).", color = Aaa.Violet)
                GhostButton("Oczyść w świątyni", onClick = { viewModel.cleanse(d.id) }, modifier = Modifier.fillMaxWidth(), height = 36.dp)
            }
        }
    }
}
