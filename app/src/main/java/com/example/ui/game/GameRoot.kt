package com.example.ui.game

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.example.game.WuxiaGameViewModel
import com.example.game.WuxiaUiState
import com.example.ui.aaa.Aaa
import com.example.ui.aaa.AaaToastHost
import com.example.ui.audio.WuxiaAudioEngine
import com.example.ui.debug.DebugPanel
import com.example.ui.screens.CombatScreenModal

private val InkEase = CubicBezierEasing(0.7f, 0f, 0.25f, 1f)

/**
 * Korzeń gry: scena sekty + jeden moduł pełnoekranowy naraz.
 * Wejście: kamera wjeżdża w dotknięty punkt, moduł rozlewa się z niego okręgiem.
 * Wyjście: odwrotnie (przycisk ← albo systemowe „wstecz”).
 */
@Composable
fun GameRoot(viewModel: WuxiaGameViewModel) {
    val ui by viewModel.uiState.collectAsState()

    var target by remember { mutableStateOf<GameModule?>(null) }
    var shown by remember { mutableStateOf<GameModule?>(null) }
    var origin by remember { mutableStateOf(Offset(0.5f, 0.5f)) }
    var rootSize by remember { mutableStateOf(IntSize(1, 1)) }
    val reveal = remember { Animatable(0f) }

    LaunchedEffect(target) {
        val m = target
        if (m != null) {
            shown = m
            reveal.snapTo(0f)
            reveal.animateTo(1f, tween(720, easing = InkEase))
        } else if (shown != null) {
            reveal.animateTo(0f, tween(480, easing = FastOutSlowInEasing))
            shown = null
        }
    }

    val open: OpenModule = { m, center ->
        origin = if (center.isSpecified && rootSize.width > 0 && rootSize.height > 0) {
            Offset(
                (center.x / rootSize.width).coerceIn(0f, 1f),
                (center.y / rootSize.height).coerceIn(0f, 1f)
            )
        } else Offset(0.5f, 0.5f)
        WuxiaAudioEngine.playDivineGong()
        target = m
    }
    val back: () -> Unit = { target = null }

    BackHandler(enabled = target != null) { back() }

    Box(
        Modifier
            .fillMaxSize()
            .background(Aaa.Ink0)
            .onSizeChanged { rootSize = it }
    ) {
        val hubVisible by remember { derivedStateOf { shown == null || reveal.value < 0.999f } }
        if (hubVisible) {
            SectHub(
                ui = ui,
                viewModel = viewModel,
                onOpen = open,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        val r = reveal.value
                        val s = 1f + 0.9f * r
                        scaleX = s
                        scaleY = s
                        transformOrigin = TransformOrigin(origin.x, origin.y)
                        alpha = 1f - 0.9f * r
                    }
            )
        }

        val m = shown
        if (m != null) {
            Box(
                Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        clip = true
                        shape = CircleRevealShape(reveal.value, origin)
                    }
            ) {
                ModuleHost(module = m, ui = ui, viewModel = viewModel, onBack = back)
            }
        }

        AaaToastHost(
            message = ui.notificationMessage,
            onDismiss = { viewModel.clearNotification() },
            modifier = Modifier
                .align(Alignment.TopCenter)
                .safeTop()
                .padding(top = 56.dp)
        )

        TutorialBanner(
            tutorial = if (shown == null) ui.tutorial else null,
            onNext = { viewModel.tutorialNext() },
            onSkip = { viewModel.tutorialSkip() },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .safeBottom()
                .padding(bottom = 112.dp)
        )

        DebugPanel(
            viewModel = viewModel,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .safeBottom()
                .padding(start = 8.dp, bottom = 96.dp)
        )

        ui.tribulation?.let { t ->
            TribulationOverlay(t, onDone = { viewModel.dismissTribulation() })
        }

        ui.offlineReport?.let { report ->
            OfflineSummaryDialog(report, onClaim = { viewModel.claimOfflineReport() })
        }

        if (ui.combatState.active) {
            CombatScreenModal(
                combatState = ui.combatState,
                onManualAttack = { viewModel.combatManualAttack() },
                onUsePill = { viewModel.combatUsePill() },
                onUltimate = { viewModel.combatUltimate() },
                onClose = { viewModel.closeCombat() }
            )
        }
    }
}

@Composable
private fun ModuleHost(
    module: GameModule,
    ui: WuxiaUiState,
    viewModel: WuxiaGameViewModel,
    onBack: () -> Unit
) {
    when (module) {
        GameModule.PALACE -> PalaceModule(ui, viewModel, onBack)
        GameModule.ADEPTS -> AdeptsModule(ui, viewModel, onBack)
        GameModule.ALCHEMY -> AlchemyModule(ui, viewModel, onBack)
        GameModule.FORGE -> ForgeModule(ui, viewModel, onBack)
        GameModule.BEASTS -> BeastsModule(ui, viewModel, onBack)
        GameModule.WORLD -> WorldModule(ui, viewModel, onBack)
        GameModule.CLANS -> ClansModule(ui, viewModel, onBack)
        GameModule.DAO_NET -> DaoNetModule(ui, viewModel, onBack)
        GameModule.VAULT -> VaultModule(ui, viewModel, onBack)
        GameModule.OMEN -> OmenModule(ui, viewModel, onBack)
    }
}
