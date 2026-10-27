package com.example.ui.game

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.game.TutorialUi
import com.example.game.engine.OfflineProgress
import com.example.ui.aaa.Aaa
import com.example.ui.aaa.aaaFrame
import com.example.ui.aaa.formatGameNumber

/** Zwój „Gdy cię nie było” – podsumowanie postępu offline. */
@Composable
fun OfflineSummaryDialog(report: OfflineProgress.Report, onClaim: () -> Unit) {
    val hours = report.elapsedMs / 3_600_000
    val minutes = (report.elapsedMs / 60_000) % 60
    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.72f))
            .clickable(remember { MutableInteractionSource() }, indication = null) {},
        contentAlignment = Alignment.Center
    ) {
        Column(
            Modifier
                .padding(24.dp)
                .widthIn(max = 420.dp)
                .fillMaxWidth()
                .aaaFrame(fill = Brush.verticalGradient(listOf(Aaa.Ink3, Aaa.Ink1)))
                .padding(horizontal = 22.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Overline("Gdy cię nie było")
            Spacer(Modifier.height(6.dp))
            Text(
                "${hours} h ${minutes} min",
                fontFamily = BrushFont,
                color = Aaa.GoldHi,
                fontSize = 34.sp
            )
            if (report.capped) {
                Text(
                    "Liczy się najwyżej ${OfflineProgress.MAX_HOURS} h nieobecności.",
                    color = Aaa.TextLow,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center
                )
            }
            Spacer(Modifier.height(14.dp))
            SummaryLine("Kamienie Ducha", "+${formatGameNumber(report.stones)}", Aaa.Gold)
            if (report.upkeep > 0) SummaryLine("Utrzymanie edyktów", "−${formatGameNumber(report.upkeep)}", Aaa.Cinnabar)
            SummaryLine("Minęło dni gry", formatGameNumber(report.days), Aaa.TextHi)
            if (report.qiGained.isNotEmpty()) {
                SummaryLine("Uczniowie medytowali", "${report.qiGained.size}", Aaa.Jade)
            }
            if (report.foodShortage > 0) SummaryLine("Zabrakło ryżu", "${report.foodShortage} racji", Aaa.Cinnabar)
            if (report.readyForBreakthrough > 0) {
                SummaryLine("Gotowi do przełomu", "${report.readyForBreakthrough}", Aaa.Jade)
            }
            report.items.entries.sortedByDescending { it.value }.take(6).forEach { (name, count) ->
                SummaryLine(name, "+${formatGameNumber(count)}", Aaa.TextMid)
            }
            Spacer(Modifier.height(18.dp))
            GoldCta("Odbierz", onClick = onClaim, modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun SummaryLine(label: String, value: String, color: Color) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = Aaa.TextMid, fontSize = 14.sp, modifier = Modifier.weight(1f, fill = false))
        Text(value, color = color, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
    }
}

/** Pasek Ducha Założyciela – samouczek. */
@Composable
fun TutorialBanner(tutorial: TutorialUi?, onNext: () -> Unit, onSkip: () -> Unit, modifier: Modifier = Modifier) {
    AnimatedVisibility(
        visible = tutorial != null,
        enter = fadeIn() + slideInVertically { it / 2 },
        exit = fadeOut() + slideOutVertically { it / 2 },
        modifier = modifier
    ) {
        val t = tutorial ?: return@AnimatedVisibility
        Column(
            Modifier
                .padding(horizontal = 16.dp)
                .widthIn(max = 460.dp)
                .fillMaxWidth()
                .aaaFrame(accent = Aaa.Jade, fill = Brush.verticalGradient(listOf(Aaa.Ink3.copy(alpha = 0.96f), Aaa.Ink1.copy(alpha = 0.96f))))
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Overline("Duch Założyciela · ${t.index + 1}/${t.total}", color = Aaa.Jade)
                Spacer(Modifier.weight(1f))
                Text(
                    "Pomiń",
                    color = Aaa.TextLow,
                    fontSize = 12.sp,
                    modifier = Modifier
                        .clickable(onClick = onSkip)
                        .padding(4.dp)
                )
            }
            Text(t.title, fontFamily = BrushFont, color = Aaa.GoldHi, fontSize = 22.sp)
            Spacer(Modifier.height(2.dp))
            Text(t.text, color = Aaa.TextHi, fontSize = 14.sp, lineHeight = 19.sp)
            Spacer(Modifier.height(10.dp))
            if (t.waitingForAction) {
                Text("Wykonaj zadanie, a Duch przemówi dalej…", color = Aaa.TextLow, fontSize = 12.sp)
            } else {
                GhostButton(if (t.index + 1 >= t.total) "Zakończ" else "Dalej", onClick = onNext, height = 38.dp)
            }
        }
    }
}
