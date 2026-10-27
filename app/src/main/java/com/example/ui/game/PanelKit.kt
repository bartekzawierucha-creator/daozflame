package com.example.ui.game

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.aaa.Aaa
import com.example.ui.aaa.AaaType

/*
 * Wspólne klocki paneli nowych systemów (Etapy 4–9). Jeden wygląd zamiast prywatnych kopii w każdym pliku:
 * panel z zakładkami, karta, chip wyboru, sekcja, kafel akcji, przycisk z potwierdzeniem.
 */

/** Panel „Księgi” z przewijanymi zakładkami i przewijaną treścią. */
@Composable
fun TabbedPanel(
    title: String,
    tabs: List<String>,
    selected: Int,
    onSelect: (Int) -> Unit,
    onClose: () -> Unit,
    header: (@Composable ColumnScope.() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    LegacyPanel(title = title, onClose = onClose) {
        Column(Modifier.fillMaxSize()) {
            if (header != null) Column(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 6.dp), verticalArrangement = Arrangement.spacedBy(4.dp), content = header)
            if (tabs.size > 1) {
                TextTabs(tabs, selected, onSelect, Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 14.dp, vertical = 6.dp))
            }
            Column(
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                content = content
            )
        }
    }
}

/** Panel bez zakładek. */
@Composable
fun SimplePanel(title: String, onClose: () -> Unit, content: @Composable ColumnScope.() -> Unit) =
    TabbedPanel(title, emptyList(), 0, {}, onClose, content = content)

/** Karta z delikatną ramką; [accent] podświetla stan (zagrożenie, gotowość, wybór). */
@Composable
fun InfoCard(accent: Color? = null, modifier: Modifier = Modifier, onClick: (() -> Unit)? = null, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier
            .fillMaxWidth()
            .background(Brush.horizontalGradient(listOf((accent ?: Aaa.Ink3).copy(alpha = if (accent != null) 0.16f else 0.8f), Aaa.Ink3.copy(alpha = 0.8f))))
            .border(1.dp, accent?.copy(alpha = 0.7f) ?: Aaa.Hairline)
            .let { if (onClick != null) it.clickable(role = Role.Button, onClick = onClick) else it }
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        content = content
    )
}

/** Chip wyboru (opcje, uczniowie, zioła). */
@Composable
fun SelectChip(text: String, selected: Boolean, accent: Color = Aaa.Gold, enabled: Boolean = true, onClick: () -> Unit) {
    Text(
        text,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        style = AaaType.Label.copy(fontSize = 10.5.sp, color = when {
            selected -> Aaa.Ink0
            enabled -> Aaa.TextHi
            else -> Aaa.TextLow
        }),
        modifier = Modifier
            .background(if (selected) accent else Aaa.Ink3)
            .border(1.dp, if (selected) accent else Aaa.Hairline)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = 9.dp, vertical = 6.dp)
    )
}

/** Nagłówek sekcji z opcjonalną wartością po prawej. */
@Composable
fun Section(title: String, trailing: String? = null, color: Color = Aaa.GoldMid) {
    Row(Modifier.fillMaxWidth().padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Overline(title, color = color, modifier = Modifier.weight(1f))
        if (trailing != null) Text(trailing, style = AaaType.Numeric.copy(fontSize = 10.sp, color = Aaa.TextMid))
    }
    Box(Modifier.fillMaxWidth().height(1.dp).background(Aaa.goldFade()))
}

/** Wiersz: nazwa + opis po lewej, akcja po prawej. */
@Composable
fun ActionRow(title: String, subtitle: String? = null, titleColor: Color = Aaa.TextHi, action: (@Composable () -> Unit)? = null) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Column(Modifier.weight(1f)) {
            Text(title, style = AaaType.Label.copy(fontSize = 11.5.sp, color = titleColor), maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (subtitle != null) Text(subtitle, style = AaaType.Label.copy(fontSize = 9.5.sp, color = Aaa.TextLow), maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
        action?.invoke()
    }
}

/** Kafel akcji (zarząd sekty, skróty): znak, tytuł, podtytuł, odznaka. */
@Composable
fun ActionTile(glyph: String, title: String, subtitle: String, modifier: Modifier = Modifier, badge: String? = null, accent: Color = Aaa.Gold, onClick: () -> Unit) {
    Box(modifier) {
        Column(
            Modifier
                .fillMaxWidth()
                .background(Brush.verticalGradient(listOf(accent.copy(alpha = 0.18f), Aaa.Ink1.copy(alpha = 0.9f))))
                .border(1.dp, accent.copy(alpha = 0.45f))
                .clickable(role = Role.Button, onClick = onClick)
                .padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(glyph, fontFamily = BrushFont, fontSize = 20.sp, color = accent)
            Text(title, style = AaaType.Label.copy(fontSize = 10.5.sp, color = Aaa.TextHi), maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(subtitle, style = AaaType.Label.copy(fontSize = 8.5.sp, color = Aaa.TextLow), maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        if (badge != null) Box(Modifier.align(Alignment.TopEnd).offset(x = 3.dp, y = (-4).dp)) { RedDot(badge) }
    }
}

/** Przycisk nieodwracalnej akcji: kolejne naciśnięcia przechodzą przez [steps], ostatnie wykonuje akcję. */
@Composable
fun ConfirmCta(steps: List<String>, modifier: Modifier = Modifier, enabled: Boolean = true, resetKey: Any? = null, onConfirmed: () -> Unit) {
    var step by remember(resetKey) { mutableIntStateOf(0) }
    GoldCta(
        steps[step.coerceAtMost(steps.lastIndex)],
        enabled = enabled,
        danger = step > 0,
        onClick = { if (step < steps.lastIndex) step++ else { step = 0; onConfirmed() } },
        modifier = modifier
    )
}

/** Mały znacznik stanu (chory, Wybraniec, nosiciel płomienia…). */
@Composable
fun StateTag(text: String, color: Color) {
    Text(
        text,
        style = AaaType.Label.copy(fontSize = 9.sp, color = color),
        maxLines = 1,
        modifier = Modifier
            .background(color.copy(alpha = 0.14f))
            .border(1.dp, color.copy(alpha = 0.5f))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    )
}

/** Mała ikona-kropka statusu w wierszu. */
@Composable
fun Dot(color: Color) = Box(Modifier.size(7.dp).background(color))
