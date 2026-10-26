package com.example.ui.pixelart

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.MartialSchool

// Authentic 16-bit Retro Pixel Palette
object PixelPalette {
    val Black = Color(0xFF0F0F14)
    val DarkSlate = Color(0xFF1E202B)
    val Slate = Color(0xFF323647)
    val LightSlate = Color(0xFF5A607A)
    
    val JadeDark = Color(0xFF0D5C3A)
    val Jade = Color(0xFF10B981)
    val JadeLight = Color(0xFF6EE7B7)
    
    val GoldDark = Color(0xFF92400E)
    val Gold = Color(0xFFF59E0B)
    val GoldLight = Color(0xFFFDE68A)
    
    val CrimsonDark = Color(0xFF7F1D1D)
    val Crimson = Color(0xFFEF4444)
    val CrimsonLight = Color(0xFFFCA5A5)
    
    val CyanDark = Color(0xFF0E7490)
    val Cyan = Color(0xFF06B6D4)
    val CyanLight = Color(0xFF67E8F9)
    
    val PurpleDark = Color(0xFF581C87)
    val Purple = Color(0xFF9333EA)
    val PurpleLight = Color(0xFFD8B4FE)
    
    val White = Color(0xFFF8FAFC)
    val SkinTone = Color(0xFFFDBA74)
    val HairDark = Color(0xFF18181B)
}

/**
 * 2D Pixel Art Matrix Definitions (16x16)
 * ' ' = transparent
 * 'S' = Skin
 * 'H' = Hair
 * 'C' = Cloth primary
 * 'D' = Cloth dark/secondary
 * 'G' = Gold accent / Weapon
 * 'E' = Eye
 * 'F' = Flame / Magic
 */
object PixelSprites {
    val SwordImmortal16 = listOf(
        "    HHHHHHHH    ",
        "   HHSSSSSSHH   ",
        "  HHSEESSESS H  ",
        "  HHSSSSSSSS H  ",
        "   HHSSSSSHH    ",
        "    GCCCCG      ",
        "   GGCCCCCCG    ",
        "  G GCCCCCG G   ",
        "    DCCCCCD G   ",
        "    DCDDDCD G   ",
        "    DCDDDCD G   ",
        "    CCCCCCC G   ",
        "    DD   DD     ",
        "    DD   DD     ",
        "    HH   HH     ",
        "   HHHH HHHH    "
    )

    val BodyAsura16 = listOf(
        "    HHHHHHHH    ",
        "   HHSSSSSSHH   ",
        "  HHS E SSE H   ",
        "  HHSSSSSSSS H  ",
        "   HHSSGSHH     ",
        "   CCCCCCCCC    ",
        "  GCCCCCCCCCG   ",
        " G GCCCCCCCG G  ",
        " G  DCCCCCD  G  ",
        "    DDDDD     ",
        "   DDDDDDDDD    ",
        "   DDDDDDDDD    ",
        "    DD   DD     ",
        "    DD   DD     ",
        "    HH   HH     ",
        "   HHHH HHHH    "
    )

    val ElementalTaoist16 = listOf(
        "     HHHH       ",
        "    HGGGGH      ",
        "   HHSSSSSHH    ",
        "  HHSEESSES H   ",
        "   HHSSSSSHH    ",
        "    FFFFFF      ",
        "   FCFFFFCF     ",
        "  FFCCCCCCFF    ",
        "   FCCFFCCF     ",
        "   FCCCCCCF     ",
        "   FCCCCCCF     ",
        "   FFFFFFFF     ",
        "    DD   DD     ",
        "    DD   DD     ",
        "    HH   HH     ",
        "   HHHH HHHH    "
    )

    val ShadowViper16 = listOf(
        "   HHHHHHHH     ",
        "  HHDDDDDDHH    ",
        "  HDDEEDDEEDH   ",
        "  HDDDDDDDDDH   ",
        "   HHDDDDDDHH   ",
        "    DCDDCD      ",
        "   DDCDDCDD     ",
        "  G DDCDDCD G   ",
        "  G DDDDDDD G   ",
        "    DCDDCD      ",
        "    DCDDCD      ",
        "    DDDDDD      ",
        "    DD  DD      ",
        "    DD  DD      ",
        "    HH  HH      ",
        "   HHHH HHHH    "
    )

    val DragonBoss16 = listOf(
        "  F        F    ",
        "   F  GG  F     ",
        "    GGGGGG      ",
        "   GGGEGEGG     ",
        "  GGGGGGGGGG    ",
        "   GFFFFFFG     ",
        "    GGGGGG      ",
        "   GGGGGGGG     ",
        "  GGG FF GGG    ",
        " GGGG    GGGG   ",
        " GGGGG  GGGGG   ",
        "  GGGGGGGGGG    ",
        "   G G  G G     ",
        "  GG GG GG GG   ",
        "  FF FF FF FF   ",
        "                "
    )

    val AlchemyCauldron16 = listOf(
        "                ",
        "     GGGGGG     ",
        "    GDDDDDDG    ",
        "   GDDDDDDDDG   ",
        "   GDDFFFFDDG   ",
        "   GDFFFFFFDG   ",
        "   GDFFFFFFDG   ",
        "   GDDFFFFDDG   ",
        "    GDDDDDDG    ",
        "     GGGGGG     ",
        "     F FF F     ",
        "    FFF  FFF    ",
        "   FFFFFFFFFF   ",
        "    GG    GG    ",
        "   GG      GG   ",
        "                "
    )

    val DivineForge16 = listOf(
        "                ",
        "      GGGG      ",
        "      GGGG      ",
        "       DD       ",
        "       DD       ",
        "   DDDDDDDDDD   ",
        "  DDDDDDDDDDDD  ",
        "  DDFFFFFFFFDD  ",
        "  DDFFFFFFFFDD  ",
        "   DDDDDDDDDD   ",
        "     DDDDDD     ",
        "     DDDDDD     ",
        "    DDDDDDDD    ",
        "   DDDDDDDDDD   ",
        "  DDDDDDDDDDDD  ",
        "                "
    )
}

/**
 * Renders a crisp 16x16 Pixel Sprite on Canvas
 */
@Composable
fun PixelSpriteView(
    spriteMatrix: List<String>,
    primaryColor: Color = PixelPalette.Jade,
    secondaryColor: Color = PixelPalette.Gold,
    accentColor: Color = PixelPalette.Crimson,
    pixelSizeDp: Dp = 3.dp,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier) {
        Canvas(
            modifier = Modifier.size(pixelSizeDp * 16, pixelSizeDp * 16)
        ) {
            val pixelWidth = size.width / 16f
            val pixelHeight = size.height / 16f

            for (row in spriteMatrix.indices) {
                val line = spriteMatrix[row]
                for (col in line.indices) {
                    val char = line[col]
                    val color = when (char) {
                        'S' -> PixelPalette.SkinTone
                        'H' -> PixelPalette.HairDark
                        'C' -> primaryColor
                        'D' -> secondaryColor
                        'G' -> PixelPalette.Gold
                        'E' -> PixelPalette.CyanLight
                        'F' -> accentColor
                        else -> null
                    }

                    if (color != null) {
                        drawRect(
                            color = color,
                            topLeft = Offset(col * pixelWidth, row * pixelHeight),
                            size = Size(pixelWidth, pixelHeight)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Pixelated School Avatar Icon
 */
@Composable
fun SchoolPixelAvatar(
    school: MartialSchool,
    sizeDp: Dp = 48.dp,
    modifier: Modifier = Modifier
) {
    val matrix = when (school) {
        MartialSchool.SWORD_IMMORTAL -> PixelSprites.SwordImmortal16
        MartialSchool.BODY_ASURA -> PixelSprites.BodyAsura16
        MartialSchool.ELEMENTAL_TAOIST -> PixelSprites.ElementalTaoist16
        MartialSchool.SHADOW_VIPER -> PixelSprites.ShadowViper16
    }

    val primary = when (school) {
        MartialSchool.SWORD_IMMORTAL -> PixelPalette.Cyan
        MartialSchool.BODY_ASURA -> PixelPalette.Gold
        MartialSchool.ELEMENTAL_TAOIST -> PixelPalette.Purple
        MartialSchool.SHADOW_VIPER -> PixelPalette.Jade
    }

    val secondary = when (school) {
        MartialSchool.SWORD_IMMORTAL -> PixelPalette.White
        MartialSchool.BODY_ASURA -> PixelPalette.Crimson
        MartialSchool.ELEMENTAL_TAOIST -> PixelPalette.CyanLight
        MartialSchool.SHADOW_VIPER -> PixelPalette.DarkSlate
    }

    Box(
        modifier = modifier
            .size(sizeDp)
            .background(PixelPalette.Black)
            .border(2.dp, PixelPalette.GoldDark),
        contentAlignment = Alignment.Center
    ) {
        PixelSpriteView(
            spriteMatrix = matrix,
            primaryColor = primary,
            secondaryColor = secondary,
            pixelSizeDp = (sizeDp / 18)
        )
    }
}

/**
 * Retro Pixel Card with 16-bit double stepped border
 */
@Composable
fun PixelCard(
    modifier: Modifier = Modifier,
    borderColor: Color = PixelPalette.Gold,
    backgroundColor: Color = PixelPalette.DarkSlate,
    content: @Composable ColumnScope.() -> Unit
) {
    Box(
        modifier = modifier
            .background(PixelPalette.Black)
            .padding(2.dp)
            .border(2.dp, borderColor)
            .background(backgroundColor)
            .padding(10.dp)
    ) {
        Column(content = content)
    }
}

/**
 * Retro Segmented Pixel Health / Energy Bar
 */
@Composable
fun PixelSegmentedBar(
    current: Int,
    max: Int,
    color: Color = PixelPalette.Crimson,
    segments: Int = 12,
    label: String = "HP",
    modifier: Modifier = Modifier
) {
    val filledCount = ((current.toFloat() / max.coerceAtLeast(1).toFloat()) * segments).toInt().coerceIn(0, segments)

    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "$label: $current/$max",
                color = PixelPalette.GoldLight,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(modifier = Modifier.height(2.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(10.dp)
                .background(PixelPalette.Black)
                .border(1.dp, PixelPalette.Slate)
                .padding(1.dp),
            horizontalArrangement = Arrangement.spacedBy(1.dp)
        ) {
            for (i in 0 until segments) {
                val isFilled = i < filledCount
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .background(if (isFilled) color else PixelPalette.DarkSlate)
                )
            }
        }
    }
}

/**
 * Retro Pixel Button with 3D bevel click effect
 */
@Composable
fun PixelButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    containerColor: Color = PixelPalette.JadeDark,
    contentColor: Color = PixelPalette.GoldLight,
    enabled: Boolean = true
) {
    Box(
        modifier = modifier
            .background(if (enabled) PixelPalette.Black else PixelPalette.Slate)
            .padding(2.dp)
            .border(2.dp, if (enabled) PixelPalette.Gold else PixelPalette.DarkSlate)
            .background(if (enabled) containerColor else PixelPalette.DarkSlate)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = if (enabled) contentColor else PixelPalette.LightSlate,
            fontFamily = FontFamily.Monospace,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
