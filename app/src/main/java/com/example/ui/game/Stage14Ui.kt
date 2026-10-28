package com.example.ui.game

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.game.WuxiaGameViewModel
import com.example.game.WuxiaUiState
import com.example.game.engine.Buildings
import com.example.game.engine.Creatures
import com.example.game.engine.Road
import com.example.model.FacilityType
import com.example.ui.aaa.Aaa
import com.example.ui.aaa.AaaType

/*
 * Etap 14: sekta rośnie krok po kroku. Zamknięte działy pokazują, co trzeba zbudować i co to da;
 * Droga Sekty na hubie mówi, co zrobić teraz; przy pierwszym wejściu do działu – krótka karta „po co to jest”.
 */

/** Budynek, który otwiera moduł (null = moduł zawsze otwarty albo zależy od czegoś innego). */
fun moduleFacility(m: GameModule): FacilityType? = when (m) {
    GameModule.ALCHEMY -> FacilityType.ALCHEMY_CHAMBER
    GameModule.FORGE -> FacilityType.DIVINE_FORGE
    GameModule.WORLD -> FacilityType.MISSION_HALL
    GameModule.CLANS, GameModule.DAO_NET -> FacilityType.DIPLOMACY_HALL
    else -> null
}

fun built(ui: WuxiaUiState, t: FacilityType) = (ui.facilities.find { it.type == t }?.level ?: 1) >= 1
fun underConstruction(ui: WuxiaUiState, t: FacilityType) = ui.facilities.find { it.type == t }?.let { it.level == 0 && it.buildDoneDay > 0 } ?: false

/** Bestie otwiera pierwsze jajo. */
fun beastsOpen(ui: WuxiaUiState) = ui.sectBeasts.isNotEmpty() || ui.sectLife.eggs.isNotEmpty() || (ui.inventory[Creatures.EGG] ?: 0) > 0

fun moduleLocked(ui: WuxiaUiState, m: GameModule): Boolean = when {
    m == GameModule.BEASTS -> !beastsOpen(ui)
    else -> moduleFacility(m)?.let { !built(ui, it) } ?: false
}

/** Krótki opis stanu zamkniętego budynku na hubie. */
fun lockedSub(ui: WuxiaUiState, m: GameModule): String {
    if (m == GameModule.BEASTS) return "🔒 Znajdź jajo u strażnika krainy"
    val t = moduleFacility(m) ?: return ""
    val f = ui.facilities.find { it.type == t }
    return if (f != null && f.buildDoneDay > 0) "🏗 W budowie · ${(f.buildDoneDay - ui.currentDay).coerceAtLeast(0)} d." else "🔒 Zbuduj · ${Buildings.PLANS[t]?.stones ?: 0} kam."
}

// ─── Zamknięty dział: budowa ────────────────────────────────────────────────

@Composable
fun BuildPanel(t: FacilityType, ui: WuxiaUiState, viewModel: WuxiaGameViewModel, onClose: () -> Unit) {
    val p = Buildings.PLANS[t] ?: return
    val f = ui.facilities.find { it.type == t }
    SimplePanel(title = t.polishName, onClose = onClose) {
        Box(Modifier.fillMaxWidth().background(Brush.verticalGradient(listOf(Aaa.Gold.copy(alpha = 0.12f), Aaa.Ink2))).border(1.dp, Aaa.Hairline).padding(14.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Overline("Co otwiera")
                Text(p.unlocks, style = AaaType.Body.copy(fontSize = 13.sp, lineHeight = 18.sp, color = Aaa.TextHi))
            }
        }
        when {
            f != null && f.level > 0 -> SheetBody("Budynek stoi. Rozbudowę znajdziesz w Pałacu Sekty.", color = Aaa.Jade)
            f != null && f.buildDoneDay > 0 -> {
                val left = (f.buildDoneDay - ui.currentDay).coerceAtLeast(0)
                KvRow("Budowa", "gotowe za $left dni (~${left * 48 / 60 + 1} min)", valueColor = Aaa.Ember)
                ThinBar(1f - left.toFloat() / p.days.coerceAtLeast(1), Aaa.Ember)
            }
            else -> {
                Section("Koszt")
                KvRow("Kamienie Ducha", "${p.stones} (masz ${ui.spiritStones})", valueColor = if (ui.spiritStones >= p.stones) Aaa.Gold else Aaa.Cinnabar)
                p.material?.let { m ->
                    val have = ui.inventory[m] ?: 0
                    KvRow(m, "${p.count} (masz $have)", valueColor = if (have >= p.count) Aaa.Gold else Aaa.Cinnabar)
                }
                KvRow("Czas budowy", "${p.days} dni gry (~${p.days * 48 / 60 + 1} min)", valueColor = Aaa.TextMid)
                val can = ui.spiritStones >= p.stones && (p.material == null || (ui.inventory[p.material] ?: 0) >= p.count)
                GoldCta(if (can) "Buduj" else "Brakuje zasobów", enabled = can, onClick = { viewModel.build(t) }, modifier = Modifier.fillMaxWidth())
                if (!can) SheetBody("Kamienie przynoszą uczniowie codziennie; minerały – uczniowie w kopalni (Adepci → Doradź: Wydobycie).", color = Aaa.TextLow, maxLines = 3)
            }
        }
    }
}

/** Zamknięty moduł: zamiast działu pokazuje, jak go otworzyć. */
@Composable
fun LockedModule(m: GameModule, ui: WuxiaUiState, viewModel: WuxiaGameViewModel, onBack: () -> Unit) {
    val t = moduleFacility(m)
    if (t != null) {
        BuildPanel(t, ui, viewModel, onBack)
        return
    }
    SimplePanel(title = m.title, onClose = onBack) {
        Text("🥚", fontSize = 48.sp)
        Text("Gniazdo jest puste", fontFamily = BrushFont, color = Aaa.GoldHi, fontSize = 26.sp)
        SheetBody("Bestie nie przychodzą same. Strażnicy krain pilnują swoich gniazd – pokonaj pierwszego strażnika na wyprawie (Świat), a w legowisku znajdziesz jajo. Wysiadywane wykluje się po ok. 20 minutach.", color = Aaa.TextHi, maxLines = 6)
        SheetBody("Bestia daje premię Qi całej sekcie, a później może nosić ucznia jako wierzchowiec.", color = Aaa.TextMid, maxLines = 3)
    }
}

/** Lista budowy w Pałacu. */
@Composable
fun BuildListPanel(ui: WuxiaUiState, viewModel: WuxiaGameViewModel, onClose: () -> Unit) {
    SimplePanel(title = "Budowa sekty", onClose = onClose) {
        RuleHint("Każdy budynek otwiera nowy dział gry. Budowa trwa kilka minut – w tym czasie sekta żyje dalej.", maxLines = 2)
        Buildings.PLANS.forEach { (t, p) ->
            val f = ui.facilities.find { it.type == t }
            val state = when {
                f == null || f.level > 0 -> "✓ Stoi"
                f.buildDoneDay > 0 -> "🏗 ${(f.buildDoneDay - ui.currentDay).coerceAtLeast(0)} d."
                else -> null
            }
            InfoCard(accent = if (state == null) Aaa.Gold else null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(t.polishName, style = AaaType.Label.copy(fontSize = 12.5.sp, color = Aaa.TextHi, fontWeight = FontWeight.Bold), modifier = Modifier.weight(1f))
                    if (state != null) Text(state, style = AaaType.Label.copy(fontSize = 10.sp, color = if (state.startsWith("✓")) Aaa.Jade else Aaa.Ember))
                }
                SheetBody(p.unlocks, color = Aaa.TextMid, maxLines = 3)
                if (state == null) {
                    val can = ui.spiritStones >= p.stones && (p.material == null || (ui.inventory[p.material] ?: 0) >= p.count)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("${p.stones} kam." + (p.material?.let { " · ${p.count}× $it" } ?: "") + " · ${p.days} dni",
                            style = AaaType.Label.copy(fontSize = 10.sp, color = Aaa.TextLow), modifier = Modifier.weight(1f), maxLines = 2)
                        GhostButton("Buduj", enabled = can, onClick = { viewModel.build(t) }, height = 30.dp)
                    }
                }
            }
        }
    }
}

// ─── Droga Sekty ────────────────────────────────────────────────────────────

/** Baner na hubie: co zrobić teraz i po co. Dotknięcie prowadzi na miejsce. */
@Composable
fun RoadBanner(ui: WuxiaUiState, onGo: (String) -> Unit, modifier: Modifier = Modifier) {
    if (!ui.tutorialDone) return
    val step = Road.STEPS.getOrNull(ui.roadStep) ?: return
    Row(
        modifier
            .widthIn(max = 250.dp)
            .background(Brush.horizontalGradient(listOf(Aaa.Ink0.copy(alpha = 0.88f), Aaa.Jade.copy(alpha = 0.18f))))
            .border(1.dp, Aaa.Jade.copy(alpha = 0.6f))
            .clickable(role = Role.Button) { onGo(step.target) }
            .padding(horizontal = 10.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("🧭", fontSize = 18.sp)
        Column(Modifier.weight(1f, fill = false)) {
            Text("DROGA SEKTY ${ui.roadStep + 1}/${Road.STEPS.size}", style = AaaType.Overline.copy(fontSize = 8.5.sp, color = Aaa.Jade))
            Text(step.title, style = AaaType.Label.copy(fontSize = 12.sp, color = Aaa.TextHi, fontWeight = FontWeight.Bold), maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(step.why, style = AaaType.Label.copy(fontSize = 9.5.sp, color = Aaa.TextMid), maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text("Nagroda: ${step.rewardStones} kam." + (step.rewardItem?.let { " · ${it.second}× ${it.first}" } ?: "") + "  ›",
                style = AaaType.Label.copy(fontSize = 9.sp, color = Aaa.GoldHi), maxLines = 1)
        }
    }
}

// ─── Karta „po co to jest” przy pierwszym wejściu ───────────────────────────

private val EXPLAIN: Map<GameModule, Pair<String, List<String>>> = mapOf(
    GameModule.ADEPTS to ("Uczniowie żyją sami" to listOf(
        "Każdy uczeń sam wybiera zajęcie i decyduje o przełomach – ty doradzasz (szansa, że posłucha, rośnie z zaufaniem).",
        "Qi rośnie w medytacji; przy 100% etapu uczeń przechodzi dalej. Wielki przełom (nowe królestwo) bywa ryzykowny.",
        "Ekwipunek, Kultywacja (ścieżka, metoda) i Mistrz i więzi – przyciski na karcie ucznia.")),
    GameModule.PALACE to ("Zarząd sekty" to listOf(
        "Nabór uczniów, edykty, budowa i rozbudowa pawilonów.",
        "Kafle u góry: gospodarka (żyła, kontrakty, targ), cel tygodnia, okazje, Dom Aukcyjny, zarządca.",
        "Bilans dnia w Domu Aukcyjnym pokazuje, czy sekta zarabia na siebie.")),
    GameModule.ALCHEMY to ("Kocioł i pola" to listOf(
        "Pigułka Przełomu: +25–60 pkt szansy wielkiego przełomu (zależnie od jakości).",
        "Zioła z pól i wypraw; płomień w kotle podnosi jakość pigułek.",
        "Za dużo pigułek = toksyczność ucznia – blokuje Narodziny Duszy, dopóki nie spadnie.")),
    GameModule.WORLD to ("Świat poza bramą" to listOf(
        "Wyprawa: wybierasz ścieżki, na końcu strażnik krainy z łupami (artefakty, zwoje, jajo bestii).",
        "Żywioł krainy kontra korzeń ucznia: ▲ = przewaga, ▼ = kara. Towarzysz pomaga i widzi dalej.",
        "Wieża Prób: piętro za piętrem; co 5 pigułka, co 10 artefakt.")),
    GameModule.FORGE to ("Boska Kuźnia" to listOf(
        "Artefakty z minerałów: broń, szaty, skarby – zakładasz je w Ekwipunku ucznia.",
        "Ulepszanie +1…+10; od +7 budzi się duch artefaktu.",
        "Temperatura paleniska (płomień) decyduje, co da się wykuć.")),
    GameModule.BEASTS to ("Gniazdo Bestii" to listOf(
        "Wysiaduj jaja – po ok. 20 min wykluwa się młoda bestia.",
        "Aktywna bestia daje premię Qi całej sekcie; karm ją, by ewoluowała.",
        "Bestia może nosić ucznia jako wierzchowiec.")),
    GameModule.CLANS to ("Inne sekty" to listOf(
        "Relacje, sojusze (premia Qi), handel, szpiedzy i wojny.",
        "Wrogie sekty najeżdżają – obsadź trzy bramy najsilniejszymi uczniami.")),
    GameModule.DAO_NET to ("Sieć Dao" to listOf(
        "Pojedynki z cieniami sekt innych graczy i ranking sezonu.")),
    GameModule.VAULT to ("Magazyn" to listOf(
        "Wszystkie zasoby sekty: zioła, minerały, pigułki, zwoje i artefakty."))
)

/** Pierwsze wejście do działu: krótka karta z trzema zdaniami i „Rozumiem”. */
@Composable
fun FirstVisitCard(m: GameModule, ui: WuxiaUiState, viewModel: WuxiaGameViewModel) {
    if (!ui.tutorialDone || m.name in ui.seenModules) return
    val (title, lines) = EXPLAIN[m] ?: return
    Box(
        Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.6f))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {},
        contentAlignment = Alignment.Center
    ) {
        Column(
            Modifier.padding(24.dp).background(Aaa.Ink2).border(1.dp, Aaa.Gold.copy(alpha = 0.6f)).padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Overline(m.title)
            Text(title, fontFamily = BrushFont, color = Aaa.GoldHi, fontSize = 28.sp)
            lines.forEach { l ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(Modifier.padding(top = 6.dp).size(6.dp).background(Aaa.Gold))
                    Text(l, style = AaaType.Body.copy(fontSize = 12.5.sp, lineHeight = 17.sp, color = Aaa.TextHi))
                }
            }
            GoldCta("Rozumiem", onClick = { viewModel.seeModule(m.name) }, modifier = Modifier.fillMaxWidth())
        }
    }
}
