package com.example.ui.game

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
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

/** Mały portret ucznia w ramce szkoły z podpisem – do wyboru i list więzi. */
@Composable
fun PortraitChip(d: Disciple, selected: Boolean, caption: String, captionColor: Color = Aaa.TextMid, badge: String? = null, size: Dp = 58.dp, onClick: (() -> Unit)? = null) {
    val c = schoolColor(d.martialSchool)
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(size + 8.dp)) {
        Box(
            Modifier
                .size(size)
                .drawBehind { if (selected) drawCircle(Brush.radialGradient(listOf(Aaa.GoldHi.copy(alpha = 0.55f), Color.Transparent)), radius = this.size.minDimension * 0.75f) }
                .clip(cutCornerTicket())
                .background(if (selected) Aaa.GoldHi else c.copy(alpha = 0.45f))
                .padding(if (selected) 2.dp else 1.dp)
                .clip(cutCornerTicket())
                .background(Aaa.Ink2)
                .let { m -> if (onClick != null) m.clickable(role = Role.Button, onClick = onClick) else m }
        ) {
            ArtImage(portraitKey(d), Modifier.matchParentSize(), alignment = androidx.compose.ui.BiasAlignment(0f, -1f),
                colorFilter = if (selected || onClick == null) null else dimFilter(0.75f, 0.85f))
            if (badge != null) Text(badge, fontSize = 11.sp, modifier = Modifier.align(Alignment.TopEnd).padding(2.dp))
        }
        Text(short(d).substringBefore(" "), style = AaaType.Label.copy(fontSize = 9.5.sp, color = if (selected) Aaa.GoldHi else Aaa.TextHi), maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(caption, style = AaaType.Numeric.copy(fontSize = 9.sp, color = captionColor), maxLines = 1)
    }
}

/** Więzi ucznia z innymi: portrety z rodzajem i siłą więzi. */
@Composable
fun BondsSection(d: Disciple, ui: WuxiaUiState) {
    val mine = ui.bonds.filter { it.has(d.id) }.sortedByDescending { it.value }
    Section("Więzi w sekcie", "${mine.count { it.kind != BondKind.NEUTRAL }}")
    if (mine.isEmpty()) {
        SheetBody("Jeszcze nikogo dobrze nie zna. Wspólna praca i wyprawy zbliżają.", color = Aaa.TextLow, maxLines = 2)
        return
    }
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        mine.forEach { b ->
            val other = ui.disciples.find { it.id == b.other(d.id) } ?: return@forEach
            val k = b.kind
            PortraitChip(other, false, "${k.polishName.substringBefore(" ")} ${if (b.value > 0) "+" else ""}${b.value}", bondColor(k), badge = k.glyph.takeIf { k != BondKind.NEUTRAL })
        }
    }
    RuleHint("Przyjaciele pracują chętniej razem i walczą lepiej w drużynie (60%, przysięgli 75%). Przełom rywala dodaje Qi. Wspólna szkoła i praca zbliżają, dwóch ambitnych lub porywczych się ściera.", maxLines = 4)
}

// ─── Cel tygodnia ────────────────────────────────────────────────────────────

/** Pierścień postępu z procentem w środku. */
@Composable
private fun ProgressRing(fraction: Float, color: Color, size: Dp, center: @Composable () -> Unit) {
    Box(
        Modifier.size(size).drawBehind {
            val w = 6.dp.toPx()
            val r = androidx.compose.ui.geometry.Size(this.size.width - w, this.size.height - w)
            val o = Offset(w / 2, w / 2)
            drawArc(Aaa.Ink4, 0f, 360f, false, o, r, style = androidx.compose.ui.graphics.drawscope.Stroke(w))
            drawArc(color, -90f, 360f * fraction.coerceIn(0f, 1f), false, o, r, style = androidx.compose.ui.graphics.drawscope.Stroke(w, cap = androidx.compose.ui.graphics.StrokeCap.Round))
        },
        contentAlignment = Alignment.Center
    ) { center() }
}

@Composable
private fun RewardTile(glyph: @Composable () -> Unit, value: String, label: String, color: Color, modifier: Modifier) {
    Column(
        modifier.background(Aaa.Ink3.copy(alpha = 0.85f)).border(1.dp, color.copy(alpha = 0.4f)).padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Box(Modifier.size(30.dp), contentAlignment = Alignment.Center) { glyph() }
        Text(value, style = AaaType.Numeric.copy(fontSize = 13.sp, color = color, fontWeight = FontWeight.Bold), maxLines = 1)
        Text(label, style = AaaType.Label.copy(fontSize = 9.sp, color = Aaa.TextLow), maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
fun WeeklyGoalPanel(ui: WuxiaUiState, onClose: () -> Unit) {
    SimplePanel(title = "Cel tygodnia", onClose = onClose) {
        val g = ui.weeklyGoal
        if (g == null) {
            InfoCard { SheetBody("Świat milczy – nowe wyzwanie zostanie ogłoszone wkrótce.", color = Aaa.TextMid) }
        } else {
            val left = (g.deadline - ui.currentDay).coerceAtLeast(0)
            val frac = ui.weeklyProgress.toFloat() / g.target.coerceAtLeast(1)
            val done = frac >= 1f
            val art = when (g.kind) {
                com.example.model.GoalKind.TOURNAMENT, com.example.model.GoalKind.WINS, com.example.model.GoalKind.TOWER -> "bg_combat_arena"
                com.example.model.GoalKind.BREAKTHROUGHS -> "bg_alchemy_hall"
                else -> "bg_sect_sanctuary"
            }
            // Kadr wyzwania: grafika, tytuł pędzlem, pierścień postępu i pieczęć terminu.
            Box(Modifier.fillMaxWidth().height(190.dp).clip(cutCornerTicket())) {
                ArtImage(art, Modifier.matchParentSize(), colorFilter = dimFilter(0.42f, 0.75f))
                Box(Modifier.matchParentSize().background(Brush.horizontalGradient(listOf(Aaa.Ink0.copy(alpha = 0.92f), Aaa.Ink0.copy(alpha = 0.2f)))))
                Column(Modifier.align(Alignment.CenterStart).padding(start = 14.dp, end = 120.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Overline("Wyzwanie tygodnia", color = Aaa.Cinnabar)
                    Text(g.title, fontFamily = BrushFont, color = Aaa.GoldHi, fontSize = 28.sp, lineHeight = 30.sp)
                    Text(g.description, style = AaaType.Body.copy(fontSize = 11.sp, lineHeight = 15.sp, color = Aaa.TextHi), maxLines = 4)
                }
                Column(Modifier.align(Alignment.CenterEnd).padding(end = 14.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    ProgressRing(frac, if (done) Aaa.Jade else Aaa.Gold, 84.dp) {
                        Text("${(frac * 100).toInt().coerceAtMost(100)}%", style = AaaType.Numeric.copy(fontSize = 18.sp, color = Aaa.TextHi, fontWeight = FontWeight.Bold))
                    }
                    Text(
                        if (done) "GOTOWE" else if (left == 0) "DZIŚ" else "$left DNI",
                        style = AaaType.Overline.copy(fontSize = 11.sp, color = if (done) Aaa.Jade else if (left <= 1) Aaa.Cinnabar else Aaa.GoldHi, letterSpacing = 2.sp),
                        modifier = Modifier.background(Aaa.Ink0.copy(alpha = 0.7f)).border(1.dp, if (left <= 1 && !done) Aaa.Cinnabar else Aaa.Hairline).padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }
            val shown = if (g.kind == com.example.model.GoalKind.TOURNAMENT) "${ui.weeklyProgress} / ${g.target} mocy trzech najlepszych" else "${ui.weeklyProgress.coerceAtMost(g.target)} / ${g.target}"
            KvRow("Postęp", shown, valueColor = if (done) Aaa.Jade else Aaa.Gold)
            ThinBar(frac, if (done) Aaa.Jade else Aaa.Gold, height = 7.dp)
            Section("Nagroda", "rozdana o świcie po spełnieniu")
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                RewardTile({ Text("💎", fontSize = 20.sp) }, "+${g.rewardStones}", "kamienie", Aaa.Azure, Modifier.weight(1f))
                RewardTile({ Text("☯", fontSize = 20.sp, color = Aaa.GoldHi) }, "+${g.rewardReputation}", "reputacja", Aaa.GoldHi, Modifier.weight(1f))
                g.rewardItem?.let { RewardTile({ ItemIcon(it, 28.dp) }, "+1", it, Aaa.Jade, Modifier.weight(1f)) }
                RewardTile({ Text("🏮", fontSize = 20.sp) }, "+5", "morale", Aaa.Ember, Modifier.weight(1f))
            }
            InfoCard(accent = Aaa.Cinnabar) {
                Overline("Cena porażki", color = Aaa.Cinnabar)
                SheetBody("Morale −${com.example.game.engine.SectGoals.FAIL_MORALE} · reputacja −${g.rewardReputation / 2}", color = Aaa.TextHi, maxLines = 1)
            }
        }
        Section("Kronika wyzwań", "${ui.goalsWon} wygranych · ${ui.goalsLost} przegranych")
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            repeat(ui.goalsWon.coerceAtMost(12)) { Text("🏆", fontSize = 14.sp) }
            repeat(ui.goalsLost.coerceAtMost(6)) { Text("🥀", fontSize = 14.sp) }
            if (ui.goalsWon + ui.goalsLost == 0) SheetBody("Pierwsze wyzwanie przed sektą.", color = Aaa.TextLow, maxLines = 1)
        }
        RuleHint("Co tydzień gry świat stawia sekcie jedno wyzwanie. Każde zwycięstwo podnosi poprzeczkę i nagrody.", maxLines = 2)
    }
}

// ─── Okazje ──────────────────────────────────────────────────────────────────

private fun opportunityArt(k: com.example.model.OpportunityKind) = when (k) {
    com.example.model.OpportunityKind.WANDERING_MASTER -> "avatar_sword_immortal"
    com.example.model.OpportunityKind.RARE_MERCHANT -> "bg_forge_hall"
    com.example.model.OpportunityKind.IMPERIAL_EXAM -> "bg_sect_sanctuary"
    com.example.model.OpportunityKind.HIDDEN_CAVE -> "bg_combat_arena"
    com.example.model.OpportunityKind.REFUGEES -> "bg_sect_sanctuary"
}

@Composable
fun OpportunityPanel(ui: WuxiaUiState, viewModel: WuxiaGameViewModel, onClose: () -> Unit) {
    val o = ui.opportunity
    SimplePanel(title = o?.let { Opportunities.title(it) } ?: "Okazje", onClose = onClose) {
        if (o == null) {
            InfoCard { SheetBody("Żadna okazja nie czeka. Wędrowcy, kupcy i wieści przychodzą co kilkanaście dni.", color = Aaa.TextMid) }
            return@SimplePanel
        }
        val left = (o.untilDay - ui.currentDay).coerceAtLeast(0)
        // Kadr okazji jak scena z manhwy.
        Box(Modifier.fillMaxWidth().height(210.dp).clip(cutCornerTicket())) {
            ArtImage(opportunityArt(o.kind), Modifier.matchParentSize(), alignment = androidx.compose.ui.BiasAlignment(0f, -0.5f), colorFilter = dimFilter(0.55f, 0.8f))
            Box(Modifier.matchParentSize().background(Brush.verticalGradient(listOf(Color.Transparent, Aaa.Ink0.copy(alpha = 0.95f)))))
            Text(
                if (left == 0) "OSTATNI DZIEŃ" else "JESZCZE $left D.",
                style = AaaType.Overline.copy(fontSize = 10.sp, color = if (left == 0) Aaa.Cinnabar else Aaa.GoldHi, letterSpacing = 2.sp),
                modifier = Modifier.align(Alignment.TopEnd).padding(10.dp).background(Aaa.Ink0.copy(alpha = 0.75f)).border(1.dp, Aaa.Hairline).padding(horizontal = 8.dp, vertical = 3.dp)
            )
            Column(Modifier.align(Alignment.BottomStart).padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Overline("${o.kind.glyph} Okazja", color = Aaa.Violet)
                Text(Opportunities.title(o), fontFamily = BrushFont, color = Aaa.GoldHi, fontSize = 30.sp)
            }
        }
        Text(Opportunities.description(o), style = AaaType.Body.copy(fontSize = 12.sp, lineHeight = 17.sp, color = Aaa.TextHi))
        o.artifact?.let { a ->
            Row(Modifier.fillMaxWidth().background(Aaa.Ink3.copy(alpha = 0.85f)).border(1.dp, gearRarityColor(a.rarity).copy(alpha = 0.5f)).padding(8.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                GearTile(a, com.example.game.engine.Gear.slotOf(a), 64.dp) {}
                Column(Modifier.weight(1f)) {
                    Text(a.name, style = AaaType.Label.copy(fontSize = 12.sp, color = gearRarityColor(a.rarity), fontWeight = FontWeight.Bold), maxLines = 2)
                    Text("${a.rarity} · ${a.quality.polishName} · atak +${a.attackBonus} · obrona +${a.defenseBonus}", style = AaaType.Label.copy(fontSize = 10.sp, color = Aaa.TextMid))
                    if (a.specialEffect.isNotBlank()) Text(a.specialEffect, style = AaaType.Label.copy(fontSize = 10.sp, color = Aaa.TextHi), maxLines = 2)
                }
            }
        }
        if (o.cost > 0) KvRow("Koszt", "${o.cost} kamieni (masz ${ui.spiritStones})", valueColor = if (ui.spiritStones >= o.cost) Aaa.Gold else Aaa.Cinnabar)
        var pick by remember(o) { mutableStateOf<String?>(null) }
        val needs = Opportunities.needsDisciple(o.kind)
        if (needs) {
            val chanceOf: (Disciple) -> Int? = when (o.kind) {
                com.example.model.OpportunityKind.IMPERIAL_EXAM -> { d -> Opportunities.examChance(d) }
                com.example.model.OpportunityKind.HIDDEN_CAVE -> { d -> Opportunities.caveChance(d) }
                else -> { _ -> null }
            }
            Section("Kogo wysłać?", if (o.kind == com.example.model.OpportunityKind.WANDERING_MASTER) "zyskuje każdy" else "szansa powodzenia")
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ui.disciples.filter { it.currentHp >= it.maxHp / 3 }.sortedByDescending { chanceOf(it) ?: it.combatPower }.forEach { d ->
                    val p = chanceOf(d)
                    PortraitChip(d, d.id == pick, p?.let { "$it%" } ?: "${d.combatPower}",
                        captionColor = p?.let { if (it >= 65) Aaa.Jade else if (it >= 40) Aaa.Ember else Aaa.Cinnabar } ?: Aaa.TextMid) { pick = d.id }
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            GhostButton("Odpraw", onClick = { viewModel.declineOpportunity(); onClose() }, modifier = Modifier.weight(1f))
            GoldCta(if (needs && pick == null) "Wybierz ucznia" else "Przyjmij", enabled = (!needs || pick != null) && ui.spiritStones >= o.cost,
                onClick = { viewModel.acceptOpportunity(pick); onClose() }, modifier = Modifier.weight(1.6f))
        }
    }
}

// ─── Zarządca ────────────────────────────────────────────────────────────────

@Composable
private fun PolicyCard(title: String, subtitle: String, on: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Column(
        modifier
            .background(if (on) Aaa.Jade.copy(alpha = 0.14f) else Aaa.Ink3.copy(alpha = 0.85f))
            .border(1.dp, if (on) Aaa.Jade else Aaa.Hairline)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(title, style = AaaType.Label.copy(fontSize = 11.5.sp, color = Aaa.TextHi), modifier = Modifier.weight(1f), maxLines = 1)
            Text(if (on) "●" else "○", color = if (on) Aaa.Jade else Aaa.TextLow, fontSize = 13.sp)
        }
        Text(subtitle, style = AaaType.Label.copy(fontSize = 9.5.sp, color = Aaa.TextMid), maxLines = 2)
    }
}

@Composable
fun StewardPanel(ui: WuxiaUiState, viewModel: WuxiaGameViewModel, onClose: () -> Unit) {
    val p = ui.steward
    fun set(x: StewardPolicy) = viewModel.setSteward(x)
    val who = ui.disciples.find { it.id == p.stewardId }
    SimplePanel(title = "Zarządca skarbca", onClose = onClose) {
        // Biurko zarządcy: kto prowadzi księgi i co robi codziennie.
        Row(
            Modifier.fillMaxWidth().background(Brush.horizontalGradient(listOf(Aaa.Jade.copy(alpha = 0.14f), Aaa.Ink3.copy(alpha = 0.9f)))).border(1.dp, Aaa.Hairline).padding(10.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (who != null) PortraitChip(who, true, "zarządca", Aaa.Jade, size = 70.dp)
            else Box(Modifier.size(70.dp).clip(cutCornerTicket()).background(Aaa.Ink2), contentAlignment = Alignment.Center) { Text("管", fontFamily = BrushFont, fontSize = 34.sp, color = Aaa.TextLow) }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(who?.let { short(it) } ?: "Księgi leżą odłogiem", fontFamily = BrushFont, fontSize = 22.sp, color = Aaa.GoldHi, maxLines = 1)
                val duties = buildList {
                    if (p.keepAbove > 0) add("sprzedaje nadwyżki ponad ${p.keepAbove}")
                    if (p.autoContracts) add("oddaje kontrakty")
                    if (p.autoFood) add("pilnuje spiżarni")
                }
                SheetBody(if (who == null) "Wybierz ucznia poniżej." else if (duties.isEmpty()) "Czeka na twoje polecenia." else "Codziennie: " + duties.joinToString(", ") + ".", color = Aaa.TextHi, maxLines = 3)
                who?.let { w ->
                    when {
                        w.traitIds.contains("charismatic") -> StateTag("✦ Charyzmatyczny: ceny +5%", Aaa.Jade)
                        w.traitIds.contains("greedy") && w.loyalty < 50 -> StateTag("⚠ Chciwy: podbiera 5%", Aaa.Cinnabar)
                        else -> Unit
                    }
                }
            }
        }
        Section("Kto prowadzi księgi?", "✦ charyzma · ⚠ chciwość")
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ui.disciples.sortedByDescending { if (it.traitIds.contains("charismatic")) 1 else 0 }.forEach { d ->
                val tag = when {
                    d.traitIds.contains("charismatic") -> "✦"
                    d.traitIds.contains("greedy") -> "⚠"
                    else -> null
                }
                PortraitChip(d, d.id == p.stewardId, "lojalność ${d.loyalty}", badge = tag) { set(p.copy(stewardId = if (d.id == p.stewardId) null else d.id)) }
            }
        }
        Section("Nadwyżki ziół i minerałów")
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf(0, 20, 50, 100).forEach { n ->
                PolicyCard(if (n == 0) "Trzymaj" else "> $n", if (n == 0) "nic nie sprzedaje" else "sprzedaj resztę", p.keepAbove == n, Modifier.weight(1f)) { set(p.copy(keepAbove = n)) }
            }
        }
        Section("Obowiązki")
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            PolicyCard("Kontrakty dostaw", "Oddaje, gdy magazyn ma towar.", p.autoContracts, Modifier.weight(1f)) { set(p.copy(autoContracts = !p.autoContracts)) }
            PolicyCard("Pełna spiżarnia", "Dokupuje ryż na 5 dni.", p.autoFood, Modifier.weight(1f)) { set(p.copy(autoFood = !p.autoFood)) }
        }
        RuleHint("Przedmiotów obiecanych uczniom zarządca nie rusza. Najwyżej 3 sprzedaże dziennie, od największej nadwyżki.", maxLines = 2)
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
