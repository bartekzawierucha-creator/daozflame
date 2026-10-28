package com.example.ui.game

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.game.WuxiaGameViewModel
import com.example.game.WuxiaUiState
import com.example.game.engine.Gear
import com.example.game.engine.Mentor
import com.example.game.engine.Opportunities
import com.example.model.BondKind
import com.example.model.Disciple
import com.example.model.EquipSlot
import com.example.model.StewardPolicy
import com.example.ui.aaa.Aaa
import com.example.ui.aaa.AaaType

/*
 * Etap 11: ekwipunek, narzędzia mentora, więzi, cel tygodnia, okazje i zarządca.
 */

private fun short(d: Disciple) = d.name.substringBefore(" (")

fun bondColor(k: BondKind) = when (k) {
    BondKind.SWORN -> Aaa.GoldHi
    BondKind.FRIEND -> Aaa.Jade
    BondKind.NEUTRAL -> Aaa.TextLow
    BondKind.RIVAL -> Aaa.Ember
    BondKind.ENEMY -> Aaa.Cinnabar
}

// ─── Ekwipunek ───────────────────────────────────────────────────────────────

/** Trzy miejsca ekwipunku i skarbiec z porównaniem: ile ataku i obrony da zmiana. */
@Composable
fun GearPanel(d: Disciple, ui: WuxiaUiState, viewModel: WuxiaGameViewModel, onClose: () -> Unit) {
    SimplePanel(title = "Ekwipunek · ${short(d)}", onClose = onClose) {
        val set = Gear.fullSet(d)
        InfoCard(accent = if (set) Aaa.GoldHi else null) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                KvRow("Atak", "${d.attack}", Modifier.weight(1f))
                KvRow("Obrona", "${d.defense}", Modifier.weight(1f))
            }
            SheetBody(
                if (set) "Pełny komplet: +${Gear.SET_BONUS_PERCENT}% premii wszystkich przedmiotów."
                else "Załóż broń, szatę i skarb – pełny komplet daje +${Gear.SET_BONUS_PERCENT}% premii.",
                color = if (set) Aaa.GoldHi else Aaa.TextLow, maxLines = 2
            )
            if (d.scars > 0) SheetBody("Blizny: ${d.scars} (+${d.scars * com.example.game.engine.Legacy.SCAR_DEFENSE} obrony z hartu).", color = Aaa.Ember, maxLines = 1)
        }
        EquipSlot.entries.forEach { slot ->
            val a = Gear.inSlot(d, slot)
            Section("${slot.glyph} ${slot.polishName}")
            if (a == null) SheetBody("Puste miejsce.", color = Aaa.TextLow, maxLines = 1)
            else ActionRow(
                title = a.name + if (a.plus > 0) " +${a.plus}" else "",
                subtitle = "${a.quality.polishName} · atak +${a.attackBonus} · obrona +${a.defenseBonus}. ${a.specialEffect}"
            ) { GhostButton("Zdejmij", onClick = { viewModel.unequip(d.id, slot) }, height = 30.dp) }
        }
        Section("Skarbiec sekty", "${ui.vaultArtifacts.size}")
        if (ui.vaultArtifacts.isEmpty()) SheetBody("Skarbiec jest pusty. Artefakty wykuwa Boska Kuźnia, przynoszą okazje i groty.", color = Aaa.TextLow)
        ui.vaultArtifacts.withIndex().sortedByDescending { it.value.attackBonus + it.value.defenseBonus }.forEach { (i, a) ->
            val slot = Gear.slotOf(a)
            val cur = Gear.inSlot(d, slot)
            val dAtk = a.attackBonus - (cur?.attackBonus ?: 0)
            val dDef = a.defenseBonus - (cur?.defenseBonus ?: 0)
            fun sign(v: Int) = if (v >= 0) "+$v" else "$v"
            ActionRow(
                title = "${slot.glyph} ${a.name}" + if (a.plus > 0) " +${a.plus}" else "",
                subtitle = "${a.quality.polishName} · atak ${sign(dAtk)} · obrona ${sign(dDef)}" + (cur?.let { " (zamiast: ${it.name})" } ?: ""),
                titleColor = if (dAtk + dDef > 0) Aaa.TextHi else Aaa.TextMid
            ) { GhostButton("Załóż", onClick = { viewModel.equip(d.id, i) }, height = 30.dp) }
        }
    }
}

// ─── Mentor i więzi (w panelu Umysłu) ───────────────────────────────────────

/** Osobiste działania mistrza: ograniczone skupieniem, więc trzeba wybrać, komu poświęcić dzień. */
@Composable
fun MentorSection(d: Disciple, ui: WuxiaUiState, viewModel: WuxiaGameViewModel) {
    var choosing by remember(d.id) { mutableStateOf(false) }
    Section("Czas mistrza", "skupienie ${ui.masterFocus}/${Mentor.MAX_FOCUS}")
    RuleHint("Mistrz ma ${Mentor.MAX_FOCUS} osobiste działania na dzień. Pochwała budzi zazdrość rywali i zazdrosnych. Złamana obietnica niszczy zaufanie.", maxLines = 3)
    val tired = ui.masterFocus <= 0
    val meditated = d.meditatedWithMasterDay == ui.currentDay
    ActionRow("Wspólna medytacja", if (meditated) "Już dziś medytowaliście razem." else "Qi dla ucznia i +${Mentor.MEDITATION_TRUST} zaufania.") {
        GhostButton("Medytuj", enabled = !tired && !meditated, onClick = { viewModel.meditateWith(d.id) }, height = 30.dp)
    }
    ActionRow("Pochwała przy sekcie", "+${Mentor.PRAISE_LOYALTY} lojalności, +${Mentor.PRAISE_TRUST} zaufania. Rywale zapamiętają.") {
        GhostButton("Pochwal", enabled = !tired, onClick = { viewModel.praise(d.id) }, height = 30.dp)
    }
    val p = d.promise
    val target = Mentor.nextMajor(d)
    if (p != null) {
        InfoCard(accent = Aaa.Azure) {
            Overline("Obietnica")
            SheetBody("${p.rewardName} za ${com.example.model.MajorRealm.entries[p.targetMajor].polishName} – do dnia ${p.untilDay}. Nie sprzedawaj nagrody!", color = Aaa.TextHi, maxLines = 3)
        }
    } else if (target != null) {
        ActionRow("Obietnica nagrody", "Nagroda za ${target.polishName} w ${Mentor.PROMISE_DAYS} dni. Motywuje i buduje zaufanie.") {
            GhostButton(if (choosing) "Anuluj" else "Obiecaj", enabled = !tired, onClick = { choosing = !choosing }, height = 30.dp)
        }
        if (choosing) {
            val pills = ui.inventory.filter { (k, v) -> v > 0 && k.startsWith("Pigułka") }.keys.sorted().take(6)
            pills.forEach { item ->
                ActionRow("💊 $item", "W magazynie: ${ui.inventory[item]}") {
                    GhostButton("Wybierz", onClick = { choosing = false; viewModel.makePromise(d.id, item) }, height = 28.dp)
                }
            }
            ui.vaultArtifacts.withIndex().take(6).forEach { (i, a) ->
                ActionRow("${Gear.slotOf(a).glyph} ${a.name}", "${a.quality.polishName} · atak +${a.attackBonus} · obrona +${a.defenseBonus}") {
                    GhostButton("Wybierz", onClick = { choosing = false; viewModel.makePromise(d.id, "art:$i") }, height = 28.dp)
                }
            }
            if (pills.isEmpty() && ui.vaultArtifacts.isEmpty()) SheetBody("Nie masz czego obiecać – zdobądź pigułki albo artefakty.", color = Aaa.TextLow)
        }
    }
    if (tired) SheetBody("Mistrz jest dziś wyczerpany. Skupienie wróci jutro.", color = Aaa.TextLow, maxLines = 1)
}

/** Więzi ucznia z innymi: przyjaźnie, rywalizacje, braterstwo krwi. */
@Composable
fun BondsSection(d: Disciple, ui: WuxiaUiState) {
    val mine = ui.bonds.filter { it.has(d.id) }.sortedByDescending { kotlin.math.abs(it.value) }
    Section("Więzi w sekcie", "${mine.count { it.kind != BondKind.NEUTRAL }}")
    RuleHint("Wspólna praca i ta sama szkoła zbliżają, dwóch ambitnych lub porywczych się ściera. Przyjaciele chętniej pracują razem i lepiej walczą w drużynie. Przełom rywala dodaje Qi.", maxLines = 4)
    if (mine.isEmpty()) SheetBody("Jeszcze nikogo dobrze nie zna.", color = Aaa.TextLow, maxLines = 1)
    mine.take(6).forEach { b ->
        val other = ui.disciples.find { it.id == b.other(d.id) } ?: return@forEach
        val k = b.kind
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(k.glyph, fontSize = 12.sp)
            Text(short(other), style = AaaType.Label.copy(fontSize = 11.sp, color = Aaa.TextHi), modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(k.polishName, style = AaaType.Label.copy(fontSize = 10.sp, color = bondColor(k)), maxLines = 1)
            Text(if (b.value > 0) "+${b.value}" else "${b.value}", style = AaaType.Numeric.copy(fontSize = 10.sp, color = bondColor(k)), modifier = Modifier.width(32.dp))
        }
    }
}

// ─── Cel tygodnia ────────────────────────────────────────────────────────────

@Composable
fun WeeklyGoalPanel(ui: WuxiaUiState, onClose: () -> Unit) {
    SimplePanel(title = "Cel tygodnia", onClose = onClose) {
        val g = ui.weeklyGoal
        if (g == null) {
            SheetBody("Świat milczy – nowe wyzwanie zostanie ogłoszone wkrótce.", color = Aaa.TextLow)
        } else {
            val left = (g.deadline - ui.currentDay).coerceAtLeast(0)
            Text(g.title, fontFamily = BrushFont, color = Aaa.GoldHi, fontSize = 26.sp)
            SheetBody(g.description, color = Aaa.TextHi, maxLines = 4)
            val shown = if (g.kind == com.example.model.GoalKind.TOURNAMENT) "${ui.weeklyProgress} / ${g.target} mocy" else "${ui.weeklyProgress.coerceAtMost(g.target)} / ${g.target}"
            KvRow("Postęp", shown, valueColor = if (ui.weeklyProgress >= g.target) Aaa.Jade else Aaa.Gold)
            ThinBar(ui.weeklyProgress.toFloat() / g.target.coerceAtLeast(1), Aaa.Gold)
            KvRow("Termin", if (left == 0) "dziś" else "za $left d. (dzień ${g.deadline})", valueColor = if (left <= 1) Aaa.Cinnabar else Aaa.TextMid)
            Section("Nagroda")
            SheetBody("+${g.rewardStones} kamieni · +${g.rewardReputation} reputacji" + (g.rewardItem?.let { " · $it" } ?: "") + " · morale +5", color = Aaa.Jade)
            Section("Cena porażki")
            SheetBody("Morale −${com.example.game.engine.SectGoals.FAIL_MORALE} · reputacja −${g.rewardReputation / 2}", color = Aaa.Cinnabar)
        }
        Section("Kronika wyzwań")
        KvRow("Wygrane / przegrane", "${ui.goalsWon} / ${ui.goalsLost}", valueColor = Aaa.TextMid)
        RuleHint("Co tydzień gry świat stawia sekcie jedno wyzwanie. Każde kolejne zwycięstwo podnosi poprzeczkę i nagrody.", maxLines = 3)
    }
}

// ─── Okazje ──────────────────────────────────────────────────────────────────

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun OpportunityPanel(ui: WuxiaUiState, viewModel: WuxiaGameViewModel, onClose: () -> Unit) {
    val o = ui.opportunity
    SimplePanel(title = o?.let { "${it.kind.glyph} ${Opportunities.title(it)}" } ?: "Okazje", onClose = onClose) {
        if (o == null) {
            SheetBody("Żadna okazja nie czeka. Wędrowcy, kupcy i wieści przychodzą co kilkanaście dni.", color = Aaa.TextLow)
            return@SimplePanel
        }
        val left = (o.untilDay - ui.currentDay).coerceAtLeast(0)
        SheetBody(Opportunities.description(o), color = Aaa.TextHi, maxLines = 5)
        KvRow("Czas", if (left == 0) "ostatni dzień" else "jeszcze $left d.", valueColor = if (left == 0) Aaa.Cinnabar else Aaa.TextMid)
        if (o.cost > 0) KvRow("Koszt", "${o.cost} kamieni", valueColor = if (ui.spiritStones >= o.cost) Aaa.Gold else Aaa.Cinnabar)
        var pick by remember(o) { mutableStateOf<String?>(null) }
        val needs = Opportunities.needsDisciple(o.kind)
        if (needs) {
            Section("Kogo wysłać?")
            val hint = when (o.kind) {
                com.example.model.OpportunityKind.IMPERIAL_EXAM -> "Liczy się zrozumienie Dao."
                com.example.model.OpportunityKind.HIDDEN_CAVE -> "Liczy się moc – słabszy może wrócić ranny."
                else -> "Najwięcej zyska ten, kto ma najwięcej przed sobą."
            }
            SheetBody(hint, color = Aaa.TextLow, maxLines = 1)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                ui.disciples.filter { it.currentHp >= it.maxHp / 3 }.sortedByDescending {
                    if (o.kind == com.example.model.OpportunityKind.IMPERIAL_EXAM) it.comprehension else it.combatPower
                }.take(10).forEach { d ->
                    val stat = if (o.kind == com.example.model.OpportunityKind.IMPERIAL_EXAM) "zr. ${d.comprehension}" else "${d.combatPower}"
                    SelectChip("${short(d).substringBefore(" ")} · $stat", d.id == pick, Aaa.Gold) { pick = d.id }
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            GhostButton("Odpraw", onClick = { viewModel.declineOpportunity(); onClose() }, modifier = Modifier.weight(1f))
            GoldCta("Przyjmij", enabled = (!needs || pick != null) && ui.spiritStones >= o.cost,
                onClick = { viewModel.acceptOpportunity(pick); onClose() }, modifier = Modifier.weight(1.6f))
        }
    }
}

// ─── Zarządca ────────────────────────────────────────────────────────────────

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun StewardPanel(ui: WuxiaUiState, viewModel: WuxiaGameViewModel, onClose: () -> Unit) {
    val p = ui.steward
    fun set(x: StewardPolicy) = viewModel.setSteward(x)
    SimplePanel(title = "Zarządca skarbca", onClose = onClose) {
        RuleHint("Zarządca codziennie handluje według twojej polityki: sprzedaje nadwyżki ziół i minerałów, oddaje kontrakty, dokupuje ryż. Charyzmatyczny targuje się o 5% lepiej, chciwy przy niskiej lojalności podbiera. Obiecanych przedmiotów nie rusza.", maxLines = 5)
        Section("Zarządca")
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            SelectChip("Brak", p.stewardId == null, Aaa.TextMid) { set(p.copy(stewardId = null)) }
            ui.disciples.take(14).forEach { d ->
                val tag = when {
                    d.traitIds.contains("charismatic") -> " ✦"
                    d.traitIds.contains("greedy") -> " ⚠"
                    else -> ""
                }
                SelectChip("${short(d).substringBefore(" ")}$tag", d.id == p.stewardId, Aaa.Gold) { set(p.copy(stewardId = d.id)) }
            }
        }
        Section("Nadwyżki", if (p.keepAbove == 0) "nie sprzedawaj" else "trzymaj ${p.keepAbove}")
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf(0, 20, 50, 100).forEach { n ->
                SelectChip(if (n == 0) "Nie" else "> $n", p.keepAbove == n, Aaa.Gold) { set(p.copy(keepAbove = n)) }
            }
        }
        Section("Obowiązki")
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            SelectChip("Kontrakty dostaw", p.autoContracts, Aaa.Jade) { set(p.copy(autoContracts = !p.autoContracts)) }
            SelectChip("Pełna spiżarnia", p.autoFood, Aaa.Jade) { set(p.copy(autoFood = !p.autoFood)) }
        }
        if (p.stewardId == null) SheetBody("Bez zarządcy polityka czeka – wybierz ucznia.", color = Aaa.Ember, maxLines = 1)
    }
}

/** Kafle Etapu 11 w Pałacu: cel tygodnia, okazja, zarządca. */
@Composable
fun Stage11Tiles(ui: WuxiaUiState, onOpen: (String) -> Unit) {
    val g = ui.weeklyGoal
    val o = ui.opportunity
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        ActionTile("挑", "Cel tygodnia",
            g?.let { "${(ui.weeklyProgress.coerceAtMost(it.target) * 100 / it.target.coerceAtLeast(1))}% · ${(it.deadline - ui.currentDay).coerceAtLeast(0)} d." } ?: "wkrótce",
            Modifier.weight(1f), accent = Aaa.Cinnabar, badge = g?.takeIf { it.deadline - ui.currentDay <= 1 }?.let { "!" }) { onOpen("goal") }
        ActionTile("机", "Okazja", o?.let { Opportunities.title(it) } ?: "brak", Modifier.weight(1f), accent = Aaa.Violet,
            badge = o?.let { "!" }) { onOpen("opportunity") }
        ActionTile("管", "Zarządca", ui.disciples.find { it.id == ui.steward.stewardId }?.let { short(it).substringBefore(" ") } ?: "brak",
            Modifier.weight(1f), accent = Aaa.Jade) { onOpen("steward") }
    }
}
