package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.game.WuxiaGameViewModel
import com.example.ui.aaa.AaaAtmosphere
import com.example.ui.aaa.AaaDock
import com.example.ui.aaa.AaaDockItem
import com.example.ui.aaa.AaaToastHost
import com.example.ui.audio.WuxiaAudioEngine
import com.example.ui.components.SectHeaderBar
import com.example.ui.dialogs.RandomEventDialog
import com.example.ui.screens.*
import com.example.ui.theme.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            WuxiaSectTheme {
                WuxiaMainApp()
            }
        }
    }
}

enum class WuxiaNavigationTab(val title: String, val testTag: String, val icon: ImageVector) {
    SANCTUARY("Sekta", "nav_sanctuary", Icons.Filled.Fort),
    DISCIPLES("Adeptowie", "nav_disciples", Icons.Filled.Groups),
    CRAFTING("Rzemiosło", "nav_crafting", Icons.Filled.Science),
    WORLD_MAP("Świat", "nav_world_map", Icons.Filled.Explore),
    DIPLOMACY("Klany", "nav_diplomacy", Icons.Filled.Handshake),
    MULTIPLAYER("Sieć Dao", "nav_multiplayer", Icons.Filled.Hub)
}

/**
 * Warstwy kadru (od spodu):
 *  1. Żywe tło atmosferyczne (niebo fazy dnia, góry tuszowe, mgła, iskry Qi)
 *  2. HUD sekty  →  ekran z przejściem kinowym  →  dok nawigacji
 *  3. Edykty (toasty) i zdarzenia losowe
 */
@Composable
fun WuxiaMainApp(viewModel: WuxiaGameViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedTab by remember { mutableStateOf(WuxiaNavigationTab.SANCTUARY) }
    var isMuted by remember { mutableStateOf(WuxiaAudioEngine.isMuted) }

    val readyBreakthroughCount = uiState.disciples.count { it.realmProgress >= 100 }
    val tabs = WuxiaNavigationTab.values()
    val dockItems = tabs.map { tab ->
        AaaDockItem(
            title = tab.title,
            icon = tab.icon,
            testTag = tab.testTag,
            badge = when (tab) {
                WuxiaNavigationTab.DISCIPLES -> if (readyBreakthroughCount > 0) "$readyBreakthroughCount" else null
                else -> null
            }
        )
    }

    Box(Modifier.fillMaxSize()) {
        AaaAtmosphere(
            timeOfDay = uiState.timeOfDay,
            modifier = Modifier.fillMaxSize()
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.displayCutout.only(WindowInsetsSides.Horizontal))
        ) {
            SectHeaderBar(
                sectName = uiState.sectName,
                currentDay = uiState.currentDay,
                timeOfDay = uiState.timeOfDay,
                spiritStones = uiState.spiritStones,
                sectReputation = uiState.sectReputation,
                gameSpeed = uiState.gameSpeedMultiplier,
                isMuted = isMuted,
                onToggleMute = {
                    WuxiaAudioEngine.isMuted = !WuxiaAudioEngine.isMuted
                    isMuted = WuxiaAudioEngine.isMuted
                    if (!isMuted) WuxiaAudioEngine.playDivineGong()
                },
                onSpeedChange = { viewModel.setGameSpeed(it) }
            )

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                AnimatedContent(
                    targetState = selectedTab,
                    transitionSpec = {
                        val forward = targetState.ordinal > initialState.ordinal
                        (fadeIn(tween(320, delayMillis = 60)) +
                            scaleIn(tween(380), initialScale = 0.965f) +
                            slideInHorizontally(tween(380)) { w -> if (forward) w / 14 else -w / 14 }) togetherWith
                            fadeOut(tween(160))
                    },
                    label = "screen_transition",
                    modifier = Modifier.fillMaxSize()
                ) { tab ->
                    when (tab) {
                        WuxiaNavigationTab.SANCTUARY -> SectSanctuaryScreen(
                            viewModel = viewModel,
                            uiState = uiState,
                            onNavigateToDisciples = { selectedTab = WuxiaNavigationTab.DISCIPLES }
                        )
                        WuxiaNavigationTab.DISCIPLES -> DisciplesScreen(viewModel = viewModel, uiState = uiState)
                        WuxiaNavigationTab.CRAFTING -> CraftingScreen(viewModel = viewModel, uiState = uiState)
                        WuxiaNavigationTab.WORLD_MAP -> WorldMapScreen(viewModel = viewModel, uiState = uiState)
                        WuxiaNavigationTab.DIPLOMACY -> DiplomacyScreen(viewModel = viewModel, uiState = uiState)
                        WuxiaNavigationTab.MULTIPLAYER -> MultiplayerScreen(viewModel = viewModel, uiState = uiState)
                    }
                }

                AaaToastHost(
                    message = uiState.notificationMessage,
                    onDismiss = { viewModel.clearNotification() },
                    modifier = Modifier.align(Alignment.TopCenter)
                )
            }

            AaaDock(
                items = dockItems,
                selectedIndex = selectedTab.ordinal,
                onSelect = { index ->
                    val tab = tabs[index]
                    if (selectedTab != tab) {
                        WuxiaAudioEngine.playDivineGong()
                        selectedTab = tab
                    }
                }
            )
        }
    }

    uiState.activeRandomEvent?.let { event ->
        RandomEventDialog(
            event = event,
            onChoiceSelected = { index -> viewModel.handleRandomEventChoice(index) },
            onDismiss = { viewModel.dismissRandomEvent() }
        )
    }
}
