package com.example.ui.graphics

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import com.example.ui.aaa.AaaPanelHeader
import com.example.ui.aaa.aaaFrame

// Ornate Oriental Palette
val GildedGold = Color(0xFFE8C37E)
val ImperialGoldDark = Color(0xFFB8860B)
val LacquerDark = Color(0xFF140D14)
val JadeRoyal = Color(0xFF00A86B)
val CinnabarRed = Color(0xFFE23D28)
val EtherealBlue = Color(0xFF00E5FF)

/**
 * Draws traditional Chinese Auspicious Clouds (Xiangyun 祥云) and Gilded Corner filigrees
 */
fun DrawScope.drawAuspiciousCloud(cx: Float, cy: Float, scale: Float = 1f, color: Color = GildedGold.copy(alpha = 0.5f)) {
    val r = 12f * scale
    // Cloud swirls with connected bezier arcs
    val path = Path().apply {
        moveTo(cx - 24f * scale, cy + 6f * scale)
        quadraticBezierTo(cx - 30f * scale, cy - 6f * scale, cx - 14f * scale, cy - 10f * scale)
        quadraticBezierTo(cx - 8f * scale, cy - 22f * scale, cx + 8f * scale, cy - 16f * scale)
        quadraticBezierTo(cx + 26f * scale, cy - 18f * scale, cx + 24f * scale, cy + 2f * scale)
        quadraticBezierTo(cx + 34f * scale, cy + 12f * scale, cx + 16f * scale, cy + 14f * scale)
        quadraticBezierTo(cx, cy + 16f * scale, cx - 24f * scale, cy + 6f * scale)
        close()
    }
    drawPath(path, color = color, style = Stroke(width = 1.8f * scale))
}

fun DrawScope.drawOrnateCorner(x: Float, y: Float, isRight: Boolean, isBottom: Boolean, color: Color = GildedGold) {
    val dirX = if (isRight) -1f else 1f
    val dirY = if (isBottom) -1f else 1f
    val len = 22f

    // Outer corner lines
    drawLine(color = color, start = Offset(x, y), end = Offset(x + dirX * len, y), strokeWidth = 2.5f)
    drawLine(color = color, start = Offset(x, y), end = Offset(x, y + dirY * len), strokeWidth = 2.5f)

    // Inner stepped corner accent
    val step = 5f
    val inX = x + dirX * step
    val inY = y + dirY * step
    drawLine(color = color.copy(alpha = 0.7f), start = Offset(inX, inY), end = Offset(inX + dirX * (len - step), inY), strokeWidth = 1.2f)
    drawLine(color = color.copy(alpha = 0.7f), start = Offset(inX, inY), end = Offset(inX, inY + dirY * (len - step)), strokeWidth = 1.2f)

    // Corner diamond stud
    drawCircle(color = color, radius = 2f, center = Offset(x + dirX * 3f, y + dirY * 3f))
}

/**
 * AAA Quality Gilded Oriental Card Frame with traditional corner filigrees and lacquer finish
 */
@Composable
fun OrnateOrientalCard(
    modifier: Modifier = Modifier,
    headerTitle: String? = null,
    borderColor: Color = GildedGold,
    backgroundColor: Color = Color(0xFF101722),
    content: @Composable ColumnScope.() -> Unit
) {
    val accent = com.example.ui.components.aaaAccentFor(borderColor)
    Column(
        modifier = modifier
            .aaaFrame(accent = accent)
            .padding(14.dp)
    ) {
        if (headerTitle != null) {
            AaaPanelHeader(title = headerTitle, accent = accent)
            Spacer(Modifier.height(10.dp))
        }
        content()
    }
}

/**
 * Talisman Seal Insignia
 */
@Composable
fun TalismanSealBadge(
    symbolText: String,
    labelText: String,
    sealColor: Color = CinnabarRed,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
            .border(1.dp, sealColor.copy(alpha = 0.6f), RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Red stamp seal
        Box(
            modifier = Modifier
                .size(20.dp)
                .background(sealColor, RoundedCornerShape(3.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = symbolText,
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = labelText,
            color = TextGold,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

/**
 * Stylizowany pergaminowy zwoj z kaligraficznym obramowaniem,
 * walkami ze zlota i jadeitu oraz cesarska pieczecia lakowa.
 */
@Composable
fun ParchmentScrollFrame(
    modifier: Modifier = Modifier,
    title: String? = null,
    sealChar: String? = "道",
    borderColor: Color = GildedGold,
    backgroundColor: Color = Color(0xFF17121A),
    content: @Composable ColumnScope.() -> Unit
) {
    val accent = com.example.ui.components.aaaAccentFor(borderColor)
    Column(modifier = modifier) {
        ScrollRoller()
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 5.dp)
                .aaaFrame(
                    accent = accent,
                    fill = Brush.verticalGradient(
                        listOf(
                            com.example.ui.aaa.Aaa.LacquerRed.copy(alpha = 0.55f),
                            com.example.ui.aaa.Aaa.Ink2,
                            com.example.ui.aaa.Aaa.Ink1
                        )
                    ),
                    chamfer = 6.dp,
                    elevation = 4.dp
                )
                .padding(14.dp)
        ) {
            if (title != null) {
                AaaPanelHeader(title = title, seal = sealChar, accent = accent)
                Spacer(Modifier.height(10.dp))
            }
            content()
        }
        ScrollRoller()
    }
}

/** Wałek zwoju: szczotkowany złoty pręt z nefrytowymi główkami. */
@Composable
private fun ScrollRoller() {
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(12.dp)
    ) {
        val w = size.width
        val h = size.height
        val capR = h * 0.5f
        drawRoundRect(
            brush = Brush.verticalGradient(
                listOf(
                    com.example.ui.aaa.Aaa.GoldHi,
                    com.example.ui.aaa.Aaa.Gold,
                    com.example.ui.aaa.Aaa.GoldDeep,
                    com.example.ui.aaa.Aaa.GoldShadow
                ),
                startY = h * 0.2f,
                endY = h * 0.8f
            ),
            topLeft = Offset(capR * 1.6f, h * 0.2f),
            size = Size(w - capR * 3.2f, h * 0.6f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(h * 0.3f, h * 0.3f)
        )
        listOf(capR, w - capR).forEach { x ->
            drawCircle(
                brush = Brush.radialGradient(
                    listOf(Color(0xFF7FE0C0), JadeRoyal, Color(0xFF06352A)),
                    center = Offset(x - capR * 0.3f, h * 0.35f),
                    radius = capR * 1.2f
                ),
                radius = capR,
                center = Offset(x, h / 2f)
            )
            drawCircle(com.example.ui.aaa.Aaa.GoldDeep, radius = capR, center = Offset(x, h / 2f), style = Stroke(width = 1f))
        }
    }
}
