package com.example.ui.aaa

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.model.TimeOfDay

/**
 * DAOZFLAME — system wizualny AAA.
 *
 * Zasada: stonowana, "droga" paleta. Głęboki lakier tuszowy z ciepłym podtonem,
 * szczotkowane antyczne złoto zamiast czystego #FFD700, cynober jako akcent władzy,
 * nefryt dla wartości pozytywnych. Kolor nasycony pojawia się tylko tam, gdzie
 * ma znaczenie (żywioł, rzadkość, alarm).
 */
object Aaa {
    // Tusz / lakier (od najgłębszego)
    val Ink0 = Color(0xFF060508)
    val Ink1 = Color(0xFF0C0A0F)
    val Ink2 = Color(0xFF141017)
    val Ink3 = Color(0xFF1D1720)
    val Ink4 = Color(0xFF2A2129)
    val LacquerRed = Color(0xFF2B0E10)

    // Szczotkowane złoto
    val GoldHi = Color(0xFFFFF0C4)
    val Gold = Color(0xFFE8C37E)
    val GoldMid = Color(0xFFC89A4E)
    val GoldDeep = Color(0xFF8C6429)
    val GoldShadow = Color(0xFF3E2B10)

    // Akcenty
    val Cinnabar = Color(0xFFD2412F)
    val CinnabarDeep = Color(0xFF6E1612)
    val Jade = Color(0xFF4CC9A0)
    val JadeDeep = Color(0xFF0F4A3A)
    val Azure = Color(0xFF6FCDF2)
    val Violet = Color(0xFFB794FF)
    val Ember = Color(0xFFFF9A4A)

    // Tekst
    val Parchment = Color(0xFFF3E6C8)
    val TextHi = Color(0xFFF7EFDD)
    val TextMid = Color(0xFFBDB29C)
    val TextLow = Color(0xFF7F7566)

    val Hairline = Gold.copy(alpha = 0.22f)

    /** Pozioma "szczotka" złota – metaliczny połysk z jasnym środkiem. */
    fun goldSheen(alpha: Float = 1f): Brush = Brush.horizontalGradient(
        listOf(
            GoldDeep.copy(alpha = alpha),
            Gold.copy(alpha = alpha),
            GoldHi.copy(alpha = alpha),
            Gold.copy(alpha = alpha),
            GoldDeep.copy(alpha = alpha)
        )
    )

    /** Pionowy metal – do krawędzi i medalionów. */
    fun goldBevel(): Brush = Brush.verticalGradient(
        listOf(GoldHi, Gold, GoldMid, GoldDeep)
    )

    /** Złota linia zanikająca na końcach. */
    fun goldFade(alpha: Float = 1f): Brush = Brush.horizontalGradient(
        listOf(
            Color.Transparent,
            GoldMid.copy(alpha = 0.6f * alpha),
            Gold.copy(alpha = alpha),
            GoldMid.copy(alpha = 0.6f * alpha),
            Color.Transparent
        )
    )

    fun lacquerFill(tint: Color = Ink3): Brush = Brush.verticalGradient(
        listOf(tint, Ink2, Ink1)
    )

    /** Kolor dominujący fazy dnia – napędza tło, poświaty i cząsteczki. */
    fun phaseAccent(timeOfDay: TimeOfDay): Color = when (timeOfDay) {
        TimeOfDay.DAWN -> Color(0xFFF2A3A0)
        TimeOfDay.DAY -> Gold
        TimeOfDay.DUSK -> Ember
        TimeOfDay.NIGHT -> Azure
        TimeOfDay.FULL_MOON -> Violet
    }
}

/** Typografia: szeryfowe nagłówki (kaligraficzny charakter), czysty sans w treści. */
object AaaType {
    private val glow = Shadow(color = Color(0xCC000000), offset = Offset(0f, 2f), blurRadius = 6f)

    val Display = TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        letterSpacing = 0.6.sp,
        color = Aaa.GoldHi,
        shadow = glow
    )

    val Title = TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.Bold,
        fontSize = 16.sp,
        letterSpacing = 0.4.sp,
        color = Aaa.GoldHi,
        shadow = glow
    )

    val Overline = TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 10.sp,
        letterSpacing = 2.2.sp,
        color = Aaa.GoldMid
    )

    val Label = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 11.sp,
        letterSpacing = 0.6.sp,
        color = Aaa.TextMid
    )

    val Body = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 17.sp,
        color = Aaa.TextHi
    )

    val Numeric = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 15.sp,
        letterSpacing = 0.3.sp,
        fontFeatureSettings = "tnum",
        color = Aaa.TextHi
    )

    val Button = TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.Bold,
        fontSize = 13.sp,
        letterSpacing = 1.4.sp
    )
}
