package com.example.ui.game

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.game.WuxiaGameViewModel
import com.example.game.WuxiaUiState
import com.example.game.engine.CloudSave
import com.example.ui.aaa.Aaa
import com.example.ui.aaa.AaaType

/** Arena cieni (asynchroniczne PvP) i zapis w chmurze. */
@Composable
fun ShadowArenaPanel(ui: WuxiaUiState, viewModel: WuxiaGameViewModel, onClose: () -> Unit) {
    val a = ui.arena
    LaunchedEffect(Unit) { viewModel.refreshArena() }
    SimplePanel(title = "Arena cieni · ranking ${a.rating}", onClose = onClose) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            SheetBody(
                if (a.online) "Twoi trzej najsilniejsi uczniowie walczą jako cień z cieniami innych sekt. Serwer odtwarza każdą walkę z ziarna."
                else "Sieć Dao jest offline – walczysz z cieniami sekt-rywali. Ranking liczy się lokalnie.",
                maxLines = 3
            )
            KvRow("Bilans", "${a.wins} zwycięstw · ${a.losses} porażek")
            a.error?.let { SheetBody(it, color = Aaa.Cinnabar, maxLines = 3) }
            GhostButton(if (a.busy) "Szukam rywali…" else "Odśwież rywali", enabled = !a.busy, onClick = { viewModel.refreshArena() }, modifier = Modifier.fillMaxWidth(), height = 36.dp)

            Section("Rywale")
            a.opponents.forEach { t ->
                Column(
                    Modifier
                        .fillMaxWidth()
                        .background(Aaa.Ink3.copy(alpha = 0.75f))
                        .border(1.dp, Aaa.Hairline)
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(t.sectName, style = AaaType.Label.copy(fontSize = 11.5.sp, color = Aaa.TextHi), modifier = Modifier.weight(1f), maxLines = 1)
                        Text("${t.rating}", style = AaaType.Numeric.copy(fontSize = 11.sp, color = Aaa.Gold))
                    }
                    Text(t.fighters.joinToString(" · ") { "${it.name} (${it.attack}/${it.defense})" }, style = AaaType.Label.copy(fontSize = 9.5.sp, color = Aaa.TextLow), maxLines = 2)
                    GoldCta("Wyzwij cień", onClick = { viewModel.fightShadow(t) }, modifier = Modifier.fillMaxWidth())
                }
            }

            if (a.ranking.isNotEmpty()) {
                Section("Ranking sezonu")
                a.ranking.take(20).forEach { r -> KvRow("${r.place}. ${r.sect_name}", "${r.rating}", valueColor = if (r.id == a.myShadowId) Aaa.GoldHi else Aaa.TextMid) }
            }

            Section("Zapis w chmurze")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                GhostButton("Wyślij zapis", enabled = a.online, onClick = { viewModel.uploadCloud() }, modifier = Modifier.weight(1f), height = 36.dp)
                GhostButton("Porównaj z chmurą", enabled = a.online, onClick = { viewModel.checkCloud() }, modifier = Modifier.weight(1f), height = 36.dp)
            }
            val local = a.cloudLocal
            val remote = a.cloudRemote
            if (local != null && remote != null) {
                SheetBody("Dwa zapisy – wybierz, który zostaje:", color = Aaa.TextHi)
                KvRow("To urządzenie", "era ${local.era} · dzień ${local.day} · ${local.topRealm}")
                KvRow("Chmura", "era ${remote.era} · dzień ${remote.day} · ${remote.topRealm}")
                when (a.cloudSuggestion) {
                    CloudSave.Choice.REMOTE -> SheetBody("Zapis w chmurze jest dalej w grze.", color = Aaa.Jade)
                    CloudSave.Choice.LOCAL -> SheetBody("Zapis na urządzeniu jest dalej w grze.", color = Aaa.Jade)
                    else -> SheetBody("Oba zapisy są w tym samym miejscu gry.", color = Aaa.TextLow)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    GhostButton("Zostaw ten", onClick = { viewModel.keepLocalSave() }, modifier = Modifier.weight(1f), height = 36.dp)
                    GoldCta("Wczytaj z chmury", danger = true, onClick = { viewModel.useCloudSave() }, modifier = Modifier.weight(1f))
                }
            }
        }
    }
}
