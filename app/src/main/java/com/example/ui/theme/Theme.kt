package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val WuxiaColorScheme = darkColorScheme(
    primary = JadeSpiritPrimary,
    onPrimary = Color.Black,
    primaryContainer = JadeSpiritDark,
    onPrimaryContainer = TextGold,
    secondary = DaoGold,
    onSecondary = Color.Black,
    secondaryContainer = DaoGoldDark,
    onSecondaryContainer = Color.White,
    tertiary = CrimsonFlame,
    onTertiary = Color.White,
    background = InkDarkBg,
    onBackground = TextPrimary,
    surface = InkSurface,
    onSurface = TextPrimary,
    surfaceVariant = InkSurfaceVariant,
    onSurfaceVariant = TextSecondary,
    outline = InkCardBorder,
    error = VermillionRed,
    onError = Color.White
)

@Composable
fun WuxiaSectTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    // Wuxia Sect Master is best experienced with rich atmospheric ink and jade aesthetic
    MaterialTheme(
        colorScheme = WuxiaColorScheme,
        typography = Typography,
        content = content
    )
}
