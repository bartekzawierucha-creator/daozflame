package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.Composable
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.game.WuxiaGameViewModel
import com.example.ui.game.GameRoot
import com.example.ui.theme.WuxiaSectTheme

class MainActivity : ComponentActivity() {
    /** Ten sam egzemplarz co `viewModel()` w Compose (wspólny ViewModelStore aktywności). */
    private val gameViewModel: WuxiaGameViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        enterImmersiveMode()
        setContent {
            WuxiaSectTheme {
                WuxiaMainApp(gameViewModel)
            }
        }
    }

    /** Zapis przy każdym wyjściu z gry (przełączenie aplikacji, blokada ekranu). */
    override fun onStop() {
        super.onStop()
        gameViewModel.saveNow()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) enterImmersiveMode()
    }

    /** Pełny ekran jak w grach: paski systemu chowają się, wysuwa je przeciągnięcie od krawędzi. */
    private fun enterImmersiveMode() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowCompat.getInsetsController(window, window.decorView).apply {
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            hide(WindowInsetsCompat.Type.systemBars())
        }
    }
}

/**
 * Gra: scena sekty + moduły pełnoekranowe (patrz [GameRoot]).
 * Dawne ekrany zakładkowe są dostępne w modułach jako „Księga”.
 */
@Composable
fun WuxiaMainApp(viewModel: WuxiaGameViewModel = viewModel()) {
    GameRoot(viewModel)
}
