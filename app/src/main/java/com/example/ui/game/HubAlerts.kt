package com.example.ui.game

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.game.WuxiaUiState
import com.example.ui.aaa.Aaa
import com.example.ui.aaa.AaaType

/** Skróty do konkretnych paneli wewnątrz modułów (alerty huba, odznaki). */
enum class Shortcut(val module: GameModule) {
    RAID(GameModule.CLANS),
    SPIES(GameModule.CLANS),
    EXPEDITION(GameModule.WORLD),
    SECRET_REALMS(GameModule.WORLD),
    TOWER(GameModule.WORLD),
    MARKET(GameModule.PALACE),
    CONTRACTS(GameModule.PALACE),
    VEIN(GameModule.PALACE),
    SECT(GameModule.PALACE),
    SECT_CHRONICLE(GameModule.PALACE),
    META(GameModule.PALACE),
    META_DAILY(GameModule.PALACE),
    JOURNAL(GameModule.PALACE),
    FIELDS(GameModule.ALCHEMY),
    FLAMES(GameModule.ALCHEMY),
    EGGS(GameModule.BEASTS),
    ARENA(GameModule.DAO_NET),
    WEEKLY_GOAL(GameModule.PALACE),
    OPPORTUNITY(GameModule.PALACE)
}

/** Oczekujący skrót: hub ustawia, moduł po otwarciu odbiera i czyści. */
object PanelNav {
    var pending by mutableStateOf<Shortcut?>(null)

    /** Odbiera skrót, jeśli należy do [module]. */
    fun take(module: GameModule): Shortcut? = pending?.takeIf { it.module == module }?.also { pending = null }
}

/** Po otwarciu modułu: wywołuje [handle] ze skrótem, jeśli jakiś czeka. */
@Composable
fun ConsumeShortcut(module: GameModule, handle: (Shortcut) -> Unit) {
    LaunchedEffect(PanelNav.pending) { PanelNav.take(module)?.let(handle) }
}

data class HubAlert(val glyph: String, val text: String, val color: Color, val shortcut: Shortcut, val priority: Int)

/** Najważniejsze sprawy sekty – z gotowego stanu UI, bez logiki gry. */
fun hubAlerts(ui: WuxiaUiState): List<HubAlert> = buildList {
    ui.raid?.let { r ->
        val days = (r.arriveDay - ui.currentDay).coerceAtLeast(0)
        add(HubAlert("⚠", "Najazd: ${shortName(r.sectName, 2)} · ${if (days == 0) "dziś" else "za $days d."}", Aaa.Cinnabar, Shortcut.RAID, 100))
    }
    ui.catastrophe?.let { c ->
        val now = ui.currentDay in c.startDay..c.endDay
        add(HubAlert(if (now) "🌩" else "🔭", if (now) c.type.polishName else "${c.type.polishName} za ${c.startDay - ui.currentDay} d.", Aaa.Ember, Shortcut.SECT_CHRONICLE, if (now) 80 else 70))
    }
    if (ui.expedition != null) add(HubAlert("⛰", "Wyprawa w toku", Aaa.Jade, Shortcut.EXPEDITION, 90))
    ui.secretRealms.firstOrNull { it.open && !it.visited }?.let { add(HubAlert("🌀", "Otwarte: ${shortName(it.name, 2)}", Aaa.Violet, Shortcut.SECRET_REALMS, 60)) }
    ui.opportunity?.let { o -> add(HubAlert(o.kind.glyph, "${o.kind.polishName} · ${(o.untilDay - ui.currentDay).coerceAtLeast(0)} d.", Aaa.Violet, Shortcut.OPPORTUNITY, 65)) }
    ui.weeklyGoal?.let { g ->
        val left = (g.deadline - ui.currentDay).coerceAtLeast(0)
        val pct = (ui.weeklyProgress.coerceAtMost(g.target) * 100 / g.target.coerceAtLeast(1))
        add(HubAlert("挑", "${shortName(g.title, 2)} $pct% · ${left} d.", if (left <= 1) Aaa.Cinnabar else Aaa.Gold, Shortcut.WEEKLY_GOAL, if (left <= 1) 75 else 45))
    }
    if (ui.rebirthAvailable) add(HubAlert("🌅", "Odrodzenie dostępne", Aaa.GoldHi, Shortcut.META, 55))
    val goals = ui.meta.goals
    if (goals.isNotEmpty() && !goals.all { it.done }) add(HubAlert("☯", "Cele dnia ${goals.count { it.done }}/${goals.size}", Aaa.Azure, Shortcut.META_DAILY, 30))
    val deliverable = ui.economy.contracts.count { it.type == com.example.model.ContractType.DELIVER && (ui.inventory[it.item] ?: 0) >= it.quantity }
    if (deliverable > 0) add(HubAlert("📜", "Kontrakty do oddania: $deliverable", Aaa.Gold, Shortcut.CONTRACTS, 40))
    val ripe = ui.economy.fields.count { it.herb != null && it.readyDay <= ui.currentDay }
    if (ripe > 0) add(HubAlert("🌿", "Pola do zbioru: $ripe", Aaa.Jade, Shortcut.FIELDS, 35))
    val sick = ui.disciples.count { it.illUntilDay >= ui.currentDay || it.cursed }
    if (sick > 0) add(HubAlert("🤒", "Chorzy / przeklęci: $sick", Aaa.Ember, Shortcut.SECT, 50))
}.sortedByDescending { it.priority }

/** Kolumna alertów na hubie (najwyżej 4). */
@Composable
fun HubAlertRail(ui: WuxiaUiState, onOpen: OpenModule, modifier: Modifier = Modifier) {
    // Sprawy z miejscem na mapie (pola, kontrakty, królestwa) pokazują pływające znaki – tu zostaje reszta.
    val onMap = setOf(Shortcut.FIELDS, Shortcut.CONTRACTS, Shortcut.SECRET_REALMS, Shortcut.RAID)
    val alerts = hubAlerts(ui).filter { it.shortcut !in onMap }.take(3)
    if (alerts.isEmpty()) return
    Column(modifier, verticalArrangement = Arrangement.spacedBy(4.dp), horizontalAlignment = Alignment.End) {
        alerts.forEach { a ->
            val ref = remember(a.shortcut) { CenterRef() }
            Row(
                Modifier
                    .width(158.dp)
                    .background(Brush.horizontalGradient(listOf(Aaa.Ink0.copy(alpha = 0.78f), a.color.copy(alpha = 0.16f))))
                    .drawBehind { drawRect(a.color, size = Size(2.dp.toPx(), size.height)) }
                    .trackCenter(ref)
                    .clickable(role = Role.Button) {
                        PanelNav.pending = a.shortcut
                        onOpen(a.shortcut.module, ref.value)
                    }
                    .padding(start = 8.dp, top = 5.dp, bottom = 5.dp, end = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(a.glyph, fontSize = 12.sp)
                Text(a.text, style = AaaType.Label.copy(fontSize = 9.5.sp, color = Aaa.TextHi), maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}
