package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.game.WuxiaGameViewModel
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

enum class WuxiaNavigationTab(val title: String, val testTag: String) {
    SANCTUARY("Sekta", "nav_sanctuary"),
    DISCIPLES("Adeptowie", "nav_disciples"),
    CRAFTING("Rzemiosło", "nav_crafting"),
    WORLD_MAP("Świat", "nav_world_map"),
    DIPLOMACY("Klany", "nav_diplomacy"),
    MULTIPLAYER("Sieć Dao", "nav_multiplayer")
}

@Composable
fun WuxiaMainApp(viewModel: WuxiaGameViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedTab by remember { mutableStateOf(WuxiaNavigationTab.SANCTUARY) }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing),
        topBar = {
            SectHeaderBar(
                sectName = uiState.sectName,
                currentDay = uiState.currentDay,
                timeOfDay = uiState.timeOfDay,
                spiritStones = uiState.spiritStones,
                sectReputation = uiState.sectReputation,
                gameSpeed = uiState.gameSpeedMultiplier,
                onSpeedChange = { viewModel.setGameSpeed(it) }
            )
        },
        bottomBar = {
            Surface(
                color = Color(0xFF0C0716),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    // Top Golden Filigree Border Line
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(2.dp)
                            .background(
                                Brush.horizontalGradient(
                                    listOf(
                                        Color.Transparent,
                                        Color(0xFFFFD700).copy(alpha = 0.4f),
                                        Color(0xFFFFD700),
                                        Color(0xFFFFD700).copy(alpha = 0.4f),
                                        Color.Transparent
                                    )
                                )
                            )
                    )

                    NavigationBar(
                        containerColor = Color(0xFF0C0716),
                        contentColor = Color(0xFFFFD700),
                        tonalElevation = 0.dp
                    ) {
                        WuxiaNavigationTab.values().forEach { tab ->
                            val isSelected = selectedTab == tab
                            val icon = when (tab) {
                                WuxiaNavigationTab.SANCTUARY -> Icons.Filled.Fort
                                WuxiaNavigationTab.DISCIPLES -> Icons.Filled.Groups
                                WuxiaNavigationTab.CRAFTING -> Icons.Filled.Science
                                WuxiaNavigationTab.WORLD_MAP -> Icons.Filled.Explore
                                WuxiaNavigationTab.DIPLOMACY -> Icons.Filled.Handshake
                                WuxiaNavigationTab.MULTIPLAYER -> Icons.Filled.Hub
                            }

                            NavigationBarItem(
                                selected = isSelected,
                                onClick = {
                                    if (selectedTab != tab) {
                                        WuxiaAudioEngine.playDivineGong()
                                        selectedTab = tab
                                    }
                                },
                                icon = {
                                    Box(
                                        modifier = if (isSelected) {
                                            Modifier
                                                .size(36.dp)
                                                .clip(CircleShape)
                                                .background(
                                                    Brush.radialGradient(
                                                        listOf(Color(0xFFFFD700).copy(alpha = 0.35f), Color.Transparent)
                                                    )
                                                )
                                        } else Modifier.size(36.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            icon,
                                            contentDescription = tab.title,
                                            tint = if (isSelected) Color(0xFFFFD700) else Color(0xFF71717A),
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                },
                                label = {
                                    Text(
                                        text = tab.title,
                                        fontSize = 10.sp,
                                        fontWeight = if (isSelected) FontWeight.Black else FontWeight.Medium,
                                        color = if (isSelected) Color(0xFFFFE082) else Color(0xFF71717A)
                                    )
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    indicatorColor = Color(0xFF451A03).copy(alpha = 0.5f),
                                    selectedIconColor = Color(0xFFFFD700),
                                    selectedTextColor = Color(0xFFFFE082),
                                    unselectedIconColor = Color(0xFF71717A),
                                    unselectedTextColor = Color(0xFF71717A)
                                ),
                                modifier = Modifier.testTag(tab.testTag)
                            )
                        }
                    }
                }
            }
        },
        containerColor = Color(0xFF090510)
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (selectedTab) {
                WuxiaNavigationTab.SANCTUARY -> SectSanctuaryScreen(
                    viewModel = viewModel,
                    uiState = uiState,
                    onNavigateToDisciples = { selectedTab = WuxiaNavigationTab.DISCIPLES }
                )
                WuxiaNavigationTab.DISCIPLES -> DisciplesScreen(
                    viewModel = viewModel,
                    uiState = uiState
                )
                WuxiaNavigationTab.CRAFTING -> CraftingScreen(
                    viewModel = viewModel,
                    uiState = uiState
                )
                WuxiaNavigationTab.WORLD_MAP -> WorldMapScreen(
                    viewModel = viewModel,
                    uiState = uiState
                )
                WuxiaNavigationTab.DIPLOMACY -> DiplomacyScreen(
                    viewModel = viewModel,
                    uiState = uiState
                )
                WuxiaNavigationTab.MULTIPLAYER -> MultiplayerScreen(
                    viewModel = viewModel,
                    uiState = uiState
                )
            }

            // Notification message toast
            if (uiState.notificationMessage != null) {
                Surface(
                    color = Color.Black.copy(alpha = 0.88f),
                    shape = RoundedCornerShape(10.dp),
                    border = ButtonDefaults.outlinedButtonBorder,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .clickable { viewModel.clearNotification() }
                        .testTag("notification_toast")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = uiState.notificationMessage ?: "",
                            color = TextGold,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            Icons.Filled.Close,
                            contentDescription = "Zamknij powiadomienie",
                            tint = TextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }

    // Dynamic Random Events Dialog
    uiState.activeRandomEvent?.let { event ->
        RandomEventDialog(
            event = event,
            onChoiceSelected = { index ->
                viewModel.handleRandomEventChoice(index)
            },
            onDismiss = {
                viewModel.dismissRandomEvent()
            }
        )
    }
}
