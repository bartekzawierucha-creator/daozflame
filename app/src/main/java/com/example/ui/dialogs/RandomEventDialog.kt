package com.example.ui.dialogs

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.RandomEvent
import com.example.ui.audio.WuxiaAudioEngine
import com.example.ui.graphics.WuxiaAssetLoader
import com.example.ui.graphics.rememberWuxiaGraphic
import com.example.ui.theme.*

/**
 * Moduł VI: Zdarzenia Losowe w Stylu Kadrów Komiksowych (Manhwa Story Panels)
 * - Dramatyczny kadr komiksu z ilustracją wędrownego mistrza / bestii
 * - Dymek dialogowy w stylu webtoona
 * - Wybory moralne: Ścieżka Prawego Dao vs Ścieżka Bezwzględnego Kultywatora
 */
@Composable
fun RandomEventDialog(
    event: RandomEvent,
    onChoiceSelected: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val eventAssetKey = when {
        event.title.contains("Demoniczn", ignoreCase = true) || event.title.contains("Krwi", ignoreCase = true) -> "avatar_shadow_viper"
        event.title.contains("Besti", ignoreCase = true) || event.title.contains("Smok", ignoreCase = true) -> "beast_azure_dragon"
        event.title.contains("Mistrz", ignoreCase = true) || event.title.contains("Starożytn", ignoreCase = true) -> "avatar_elemental_taoist"
        else -> "avatar_sword_immortal"
    }
    val eventHeroBitmap = rememberWuxiaGraphic(eventAssetKey)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = null,
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .border(2.dp, Color(0xFFFFD700), RoundedCornerShape(16.dp))
                    .background(Color(0xFF090611))
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // 1. Manhwa Comic Strip Header
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .border(1.dp, Color(0xFFFFD700).copy(alpha = 0.6f), RoundedCornerShape(10.dp))
                ) {
                    if (eventHeroBitmap != null) {
                        Image(
                            bitmap = eventHeroBitmap,
                            contentDescription = event.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    // Manhwa Dark Ink Vignette
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    listOf(
                                        Color.Black.copy(alpha = 0.2f),
                                        Color(0xFF0D0814).copy(alpha = 0.85f)
                                    )
                                )
                            )
                    )

                    // Auspicious Title Tag
                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp, 16.dp)
                                .background(Color(0xFFFFD700), RoundedCornerShape(2.dp))
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = event.title,
                            color = Color(0xFFFFE082),
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Black,
                            fontSize = 13.sp
                        )
                    }
                }

                // 2. Comic Speech Bubble
                Surface(
                    color = Color(0xFF1E1B4B),
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF6366F1)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(text = "💬", fontSize = 14.sp)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = event.description,
                            color = Color(0xFFE2E8F0),
                            fontSize = 11.sp,
                            lineHeight = 16.sp
                        )
                    }
                }

                Text(
                    text = "⚖️ Wybierz Ścieżkę Przeznaczenia Sekty:",
                    color = Color(0xFFFFE082),
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )

                // 3. Moral Choice Buttons
                event.choices.forEachIndexed { index, choice ->
                    val isRuthless = choice.text.contains("Zabierz", ignoreCase = true) ||
                            choice.text.contains("Siłą", ignoreCase = true) ||
                            choice.text.contains("Zniszcz", ignoreCase = true) ||
                            choice.text.contains("Atak", ignoreCase = true)

                    val containerColor = if (isRuthless) Color(0xFF7F1D1D) else Color(0xFF065F46)
                    val borderColor = if (isRuthless) Color(0xFFFF5252) else Color(0xFF34D399)

                    Button(
                        onClick = {
                            if (isRuthless) {
                                WuxiaAudioEngine.playSwordClash()
                            } else {
                                WuxiaAudioEngine.playDivineGong()
                            }
                            onChoiceSelected(index)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = containerColor),
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, borderColor),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("event_choice_$index"),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = (if (isRuthless) "🩸 " else "✨ ") + choice.text,
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (isRuthless) "Ścieżka Siły" else "Prawdziwe Dao",
                                color = Color(0xFFFFE082),
                                fontSize = 9.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Zignoruj Omen", color = Color(0xFF94A3B8), fontSize = 11.sp)
            }
        },
        containerColor = Color.Transparent
    )
}
