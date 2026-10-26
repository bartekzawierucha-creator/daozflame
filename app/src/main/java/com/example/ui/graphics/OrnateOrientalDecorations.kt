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

// Ornate Oriental Palette
val GildedGold = Color(0xFFFFD700)
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
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(backgroundColor)
    ) {
        // Custom canvas for corner filigrees & auspicious border
        Canvas(modifier = Modifier.matchParentSize()) {
            val w = size.width
            val h = size.height

            // Dual border lines
            drawRoundRect(
                color = borderColor.copy(alpha = 0.35f),
                topLeft = Offset(1f, 1f),
                size = Size(w - 2f, h - 2f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(8f, 8f),
                style = Stroke(width = 1.5f)
            )
            drawRoundRect(
                color = borderColor.copy(alpha = 0.8f),
                topLeft = Offset(4f, 4f),
                size = Size(w - 8f, h - 8f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(6f, 6f),
                style = Stroke(width = 1f)
            )

            // 4 Traditional Gilded Corners
            drawOrnateCorner(8f, 8f, isRight = false, isBottom = false, color = borderColor)
            drawOrnateCorner(w - 8f, 8f, isRight = true, isBottom = false, color = borderColor)
            drawOrnateCorner(8f, h - 8f, isRight = false, isBottom = true, color = borderColor)
            drawOrnateCorner(w - 8f, h - 8f, isRight = true, isBottom = true, color = borderColor)
        }

        Column(modifier = Modifier.padding(14.dp)) {
            if (headerTitle != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .background(GildedGold, RoundedCornerShape(1.dp))
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = headerTitle.uppercase(),
                        color = TextGold,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(1.dp)
                            .background(
                                Brush.horizontalGradient(
                                    listOf(GildedGold.copy(alpha = 0.7f), Color.Transparent)
                                )
                            )
                    )
                }
            }
            content()
        }
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
    backgroundColor: Color = Color(0xFF161024),
    content: @Composable ColumnScope.() -> Unit
) {
    Column(modifier = modifier) {
        // Gorny walek zwoju (Scroll Header Roller with Jade tips)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(10.dp)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height
                // Glowny drewniano-zloty walek
                drawRoundRect(
                    brush = Brush.horizontalGradient(
                        listOf(
                            Color(0xFF78350F),
                            GildedGold,
                            Color(0xFFFEF08A),
                            GildedGold,
                            Color(0xFF78350F)
                        )
                    ),
                    topLeft = Offset(12f, 2f),
                    size = Size(w - 24f, h - 4f),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(4f, 4f)
                )
                // Lewa i prawa jadeitowa koncowka walka
                drawCircle(color = JadeRoyal, radius = 5f, center = Offset(10f, h * 0.5f))
                drawCircle(color = Color.White.copy(alpha = 0.8f), radius = 2f, center = Offset(9f, h * 0.4f))
                drawCircle(color = JadeRoyal, radius = 5f, center = Offset(w - 10f, h * 0.5f))
                drawCircle(color = Color.White.copy(alpha = 0.8f), radius = 2f, center = Offset(w - 11f, h * 0.4f))
            }
        }

        // Cialo zwoju pergaminowego
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(
                    Brush.verticalGradient(
                        listOf(
                            backgroundColor,
                            Color(0xFF201630),
                            backgroundColor
                        )
                    )
                )
                .border(
                    width = 1.5.dp,
                    brush = Brush.linearGradient(
                        listOf(
                            borderColor,
                            Color(0xFF78350F),
                            borderColor,
                            Color(0xFF451A03),
                            borderColor
                        )
                    ),
                    shape = RoundedCornerShape(8.dp)
                )
        ) {
            // Narożniki kaligraficzne i motywy zwoju
            Canvas(modifier = Modifier.matchParentSize()) {
                val w = size.width
                val h = size.height
                drawOrnateCorner(8f, 8f, isRight = false, isBottom = false, color = borderColor.copy(alpha = 0.85f))
                drawOrnateCorner(w - 8f, 8f, isRight = true, isBottom = false, color = borderColor.copy(alpha = 0.85f))
                drawOrnateCorner(8f, h - 8f, isRight = false, isBottom = true, color = borderColor.copy(alpha = 0.85f))
                drawOrnateCorner(w - 8f, h - 8f, isRight = true, isBottom = true, color = borderColor.copy(alpha = 0.85f))
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp)
            ) {
                if (title != null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (sealChar != null) {
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(CinnabarRed)
                                        .border(1.dp, GildedGold, RoundedCornerShape(4.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = sealChar,
                                        color = Color(0xFFFFE082),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Black
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                            }
                            Text(
                                text = title,
                                color = Color(0xFFFFE8A3),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 0.5.sp
                            )
                        }

                        // Złoty ornament kaligraficzny
                        Box(
                            modifier = Modifier
                                .width(40.dp)
                                .height(2.dp)
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(GildedGold, Color.Transparent)
                                    )
                                )
                        )
                    }
                }

                content()
            }
        }

        // Dolny walek zwoju (Scroll Footer Roller)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(10.dp)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height
                drawRoundRect(
                    brush = Brush.horizontalGradient(
                        listOf(
                            Color(0xFF78350F),
                            GildedGold,
                            Color(0xFFFEF08A),
                            GildedGold,
                            Color(0xFF78350F)
                        )
                    ),
                    topLeft = Offset(12f, 2f),
                    size = Size(w - 24f, h - 4f),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(4f, 4f)
                )
                drawCircle(color = JadeRoyal, radius = 5f, center = Offset(10f, h * 0.5f))
                drawCircle(color = JadeRoyal, radius = 5f, center = Offset(w - 10f, h * 0.5f))
            }
        }
    }
}
