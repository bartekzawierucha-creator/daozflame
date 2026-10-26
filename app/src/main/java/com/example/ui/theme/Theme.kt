package com.example.ui.theme

import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.ui.aaa.Aaa

/**
 * Motyw M3 strojony pod system AAA: komponenty Material, których ekrany używają
 * bezpośrednio (Button, Card, AlertDialog, TabRow…), dziedziczą lakier, złoto
 * i ścięte narożniki – spójnie z ramami Aaa.
 */
private val WuxiaColorScheme = darkColorScheme(
    primary = Aaa.GoldMid,
    onPrimary = Aaa.Ink0,
    primaryContainer = Aaa.GoldShadow,
    onPrimaryContainer = Aaa.GoldHi,
    secondary = Aaa.Jade,
    onSecondary = Aaa.Ink0,
    secondaryContainer = Aaa.JadeDeep,
    onSecondaryContainer = Aaa.TextHi,
    tertiary = Aaa.Cinnabar,
    onTertiary = Aaa.GoldHi,
    tertiaryContainer = Aaa.CinnabarDeep,
    onTertiaryContainer = Aaa.GoldHi,
    background = Aaa.Ink1,
    onBackground = Aaa.TextHi,
    surface = Aaa.Ink2,
    onSurface = Aaa.TextHi,
    surfaceVariant = Aaa.Ink3,
    onSurfaceVariant = Aaa.TextMid,
    surfaceTint = Aaa.Gold,
    inverseSurface = Aaa.Parchment,
    inverseOnSurface = Aaa.Ink1,
    outline = Aaa.GoldDeep,
    outlineVariant = Aaa.GoldShadow,
    error = Aaa.Cinnabar,
    onError = Aaa.GoldHi,
    scrim = Color.Black,
    surfaceBright = Aaa.Ink4,
    surfaceDim = Aaa.Ink1,
    surfaceContainerLowest = Aaa.Ink0,
    surfaceContainerLow = Aaa.Ink1,
    surfaceContainer = Aaa.Ink2,
    surfaceContainerHigh = Aaa.Ink3,
    surfaceContainerHighest = Aaa.Ink4
)

private val WuxiaShapes = Shapes(
    extraSmall = CutCornerShape(3.dp),
    small = CutCornerShape(5.dp),
    medium = CutCornerShape(8.dp),
    large = CutCornerShape(10.dp),
    extraLarge = CutCornerShape(14.dp)
)

@Composable
fun WuxiaSectTheme(
    @Suppress("UNUSED_PARAMETER") darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = WuxiaColorScheme,
        typography = Typography,
        shapes = WuxiaShapes,
        content = content
    )
}
