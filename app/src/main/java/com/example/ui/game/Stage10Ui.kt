package com.example.ui.game

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.game.WuxiaGameViewModel
import com.example.game.WuxiaUiState
import com.example.model.Disciple
import com.example.model.DiscipleRole
import com.example.ui.aaa.Aaa
import com.example.ui.aaa.AaaType

/*
 * Etap 10: autonomia ucznia. Uczeń sam decyduje o dniu; mistrz widzi jego zamiar, więź i dziennik – i może doradzić.
 */

/** Kolor szansy posłuszeństwa: zielony – posłucha, bursztyn – może, cynober – raczej nie. */
fun obedienceColor(p: Int) = when {
    p >= 65 -> Aaa.Jade
    p >= 40 -> Aaa.Ember
    else -> Aaa.Cinnabar
}

/** Pasek na karcie ucznia: co postanowił, zaufanie, szansa posłuszeństwa. Dotknięcie otwiera [MindPanel]. */
@Composable
fun DiscipleMindStrip(d: Disciple, ui: WuxiaUiState, onOpen: () -> Unit) {
    val p = ui.obedience[d.id] ?: 50
    Column(
        Modifier
            .fillMaxWidth()
            .background(Aaa.Ink0.copy(alpha = 0.55f))
            .border(1.dp, Aaa.Hairline)
            .clickable(role = Role.Button, onClick = onOpen)
            .padding(horizontal = 9.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(
            "„${d.plan ?: "Jeszcze nie postanowił${if (d.gender == "Kobieta") "a" else ""}."}”",
            style = AaaType.Label.copy(fontSize = 10.5.sp, color = Aaa.TextHi, fontStyle = FontStyle.Italic),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Zaufanie ${d.trust}", style = AaaType.Numeric.copy(fontSize = 9.5.sp, color = Aaa.GoldMid))
            Text("Posłucha ~$p%", style = AaaType.Numeric.copy(fontSize = 9.5.sp, color = obedienceColor(p)))
            if (d.holdUntilDay >= ui.currentDay) StateTag("czeka do dnia ${d.holdUntilDay}", Aaa.Azure)
            Text("Dziennik ›", style = AaaType.Label.copy(fontSize = 9.5.sp, color = Aaa.TextLow), modifier = Modifier.weight(1f), maxLines = 1)
        }
    }
}

/** Umysł ucznia: zamiar, więź z mistrzem, rady i dziennik decyzji. */
@Composable
fun MindPanel(d: Disciple, ui: WuxiaUiState, viewModel: WuxiaGameViewModel, onClose: () -> Unit) {
    val she = d.gender == "Kobieta"
    val p = ui.obedience[d.id] ?: 50
    val preferred = ui.preferredRoles[d.id]
    SimplePanel(title = "Umysł · ${d.name.substringBefore(" (")}", onClose = onClose) {
        InfoCard {
            Overline("Co postanowił${if (she) "a" else ""}")
            SheetBody(d.plan ?: "Jeszcze nie postanowił${if (she) "a" else ""}.", color = Aaa.TextHi, maxLines = 3)
            RuleHint("Uczeń sam wybiera zajęcie, przełomy i wyzwania. Twoje polecenia są radami – może je odrzucić.", color = Aaa.TextLow, maxLines = 3)
        }

        Section("Więź z mistrzem", "zaufanie ${d.trust}/100")
        ThinBar(d.trust / 100f, Aaa.Gold)
        KvRow("Ogólna szansa, że posłucha", "$p%", valueColor = obedienceColor(p))
        RuleHint("Zaufanie rośnie, gdy rady okazują się trafne, przy awansie, łasce i leczeniu. Spada po chybionej radzie, karze i naciskaniu kilka razy dziennie.", color = Aaa.TextLow, maxLines = 4)

        MentorSection(d, ui, viewModel)
        BondsSection(d, ui)

        Section("Doradź zajęcie")
        DiscipleRole.entries.forEach { role ->
            val chance = ui.roleAdvice[d.id]?.get(role) ?: p
            val current = d.role == role
            ActionRow(
                title = role.polishName + if (role == preferred) " ★" else "",
                subtitle = if (current) "Tym się teraz zajmuje." else if (role == preferred) "Sam${if (she) "a" else ""} by to wybrał${if (she) "a" else ""}. ${role.bonusDesc}" else role.bonusDesc,
                titleColor = if (current) Aaa.GoldHi else Aaa.TextHi
            ) {
                if (!current) GhostButton("Doradź · $chance%", onClick = { viewModel.assignRole(d.id, role) }, height = 30.dp)
            }
        }
        if (d.committedUntilDay >= ui.currentDay) SheetBody("Trzyma się przyjętej rady do dnia ${d.committedUntilDay}.", color = Aaa.Azure)

        Section("Ryzyko")
        val holding = d.holdUntilDay >= ui.currentDay
        ActionRow(
            title = if (holding) "Obiecał${if (she) "a" else ""} cierpliwość do dnia ${d.holdUntilDay}" else "Wstrzymaj się",
            subtitle = "Bez wielkiego przełomu i wypraw na własną rękę przez 10 dni – np. przed tribulacją."
        ) {
            if (!holding) GhostButton("Doradź", onClick = { viewModel.adviseHold(d.id) }, height = 30.dp)
        }

        Section("Dziennik", "${d.diary.size}")
        if (d.diary.isEmpty()) SheetBody("Jeszcze nic nie zapisał${if (she) "a" else ""}.", color = Aaa.TextLow)
        d.diary.asReversed().forEach { line ->
            Text(line, style = AaaType.Label.copy(fontSize = 10.5.sp, color = Aaa.TextMid), modifier = Modifier.fillMaxWidth())
        }
    }
}
