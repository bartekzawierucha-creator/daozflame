package com.example.ui.debug

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.game.WuxiaGameViewModel
import com.example.game.engine.GameCommand
import com.example.game.engine.GameContent

/**
 * Panel deweloperski – widoczny tylko w buildzie debug. Przycisk „DBG” w rogu ekranu.
 * Nie używać do niczego, co ma trafić do graczy.
 */
@Composable
fun DebugPanel(viewModel: WuxiaGameViewModel, modifier: Modifier = Modifier) {
    if (!com.example.BuildConfig.DEBUG) return
    var open by remember { mutableStateOf(false) }
    var confirmNewGame by remember { mutableStateOf(false) }
    val clipboard = LocalClipboardManager.current

    Box(modifier) {
        if (!open) {
            Chip("DBG") { open = true }
            return@Box
        }
        Column(
            Modifier
                .width(260.dp)
                .background(Color(0xEE101010), RoundedCornerShape(10.dp))
                .padding(12.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text("Panel debug", color = Color.White, fontSize = 14.sp)
            Text(viewModel.debugInfo(), color = Color(0xFFAAAAAA), fontSize = 11.sp)

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Chip("×1") { viewModel.setGameSpeed(1) }
                Chip("×10") { viewModel.setGameSpeed(10) }
                Chip("×100") { viewModel.setGameSpeed(100) }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Chip("+100 cykli") { viewModel.debugSkipTicks(100) }
                Chip("+1 dzień gry") { viewModel.debugSkipTicks(4) }
            }
            Chip("+10 000 kamieni") { viewModel.debug(GameCommand.DebugAddStones(10_000)) }
            Chip("+10 pigułek przełomu") { viewModel.debug(GameCommand.DebugAddItem(GameContent.BREAKTHROUGH_PILL, 10)) }
            Chip("Qi uczniów = 100%") { viewModel.debug(GameCommand.DebugFillQi) }
            // Etap 6: tylko w buildzie debug (cały panel jest pokazywany wyłącznie przy BuildConfig.DEBUG).
            Chip("Pokaż tajemnice") { viewModel.debug(GameCommand.DebugRevealSecrets) }
            Chip("Wymuś zdarzenie") { viewModel.debug(GameCommand.DebugForceEvent) }
            Chip("Zapisz teraz") { viewModel.saveNow() }
            Chip("Kopiuj zapis do schowka") {
                viewModel.exportSave()?.let { clipboard.setText(AnnotatedString(it)) }
            }
            Chip("Wczytaj zapis ze schowka") {
                clipboard.getText()?.text?.let { viewModel.importSave(it) }
            }
            if (!confirmNewGame) {
                Chip("Nowa gra…") { confirmNewGame = true }
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Chip("Tak, usuń zapis", danger = true) {
                        confirmNewGame = false
                        viewModel.startNewGame()
                    }
                    Chip("Anuluj") { confirmNewGame = false }
                }
            }
            viewModel.lastCrash?.let { crash ->
                Text("Ostatnia awaria:", color = Color(0xFFFF8A73), fontSize = 12.sp)
                Text(crash.take(1200), color = Color(0xFFAAAAAA), fontSize = 9.sp)
                Chip("Kopiuj raport awarii") { clipboard.setText(AnnotatedString(crash)) }
            }
            Chip("Zamknij") { open = false }
        }
    }
}

@Composable
private fun Chip(label: String, danger: Boolean = false, onClick: () -> Unit) {
    Text(
        label,
        color = Color.White,
        fontSize = 12.sp,
        modifier = Modifier
            .background(if (danger) Color(0xFF8B1E1E) else Color(0xFF2E2E2E), RoundedCornerShape(6.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp)
    )
}
