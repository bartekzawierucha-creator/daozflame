package com.example.ui.pixelart

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.BiomeType
import com.example.model.WorldRegion

/**
 * 2D Retro Pixel Art World Map Grid View
 */
@Composable
fun PixelWorldTilemap(
    regions: List<WorldRegion>,
    selectedRegionId: String?,
    onSelectRegion: (WorldRegion) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(PixelPalette.Black)
            .border(2.dp, PixelPalette.Gold)
            .padding(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "🗺️ 2D PIXEL MAPA ŚWIATA DAO",
                color = PixelPalette.GoldLight,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp
            )
            Text(
                text = "16-BIT RETRO GRID",
                color = PixelPalette.JadeLight,
                fontFamily = FontFamily.Monospace,
                fontSize = 9.sp
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Grid of 6 Biome Regions in 2x3 Pixel Tiles
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            regions.chunked(2).forEach { rowRegions ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    rowRegions.forEach { region ->
                        val isSelected = region.id == selectedRegionId
                        PixelBiomeTile(
                            region = region,
                            isSelected = isSelected,
                            onClick = { onSelectRegion(region) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PixelBiomeTile(
    region: WorldRegion,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val (primaryBg, accentTile) = when (region.biome) {
        BiomeType.BAMBOO_MIST -> Pair(Color(0xFF0F3D24), Color(0xFF22C55E))
        BiomeType.KUNLUN_PEAK -> Pair(Color(0xFF162D4A), Color(0xFF93C5FD))
        BiomeType.LAVA_CHASM -> Pair(Color(0xFF450A0A), Color(0xFFF97316))
        BiomeType.VENOM_SWAMP -> Pair(Color(0xFF2E1065), Color(0xFFA855F7))
        BiomeType.SPIRIT_DESERT -> Pair(Color(0xFF451A03), Color(0xFFFBBF24))
        BiomeType.DRAGON_ABYSS -> Pair(Color(0xFF090D16), Color(0xFF38BDF8))
        BiomeType.THUNDER_PLATEAU -> Pair(Color(0xFF1E1B4B), Color(0xFF93C5FD))
        BiomeType.SUNKEN_PALACE -> Pair(Color(0xFF0C2A3D), Color(0xFF22D3EE))
        BiomeType.BONE_WASTES -> Pair(Color(0xFF292524), Color(0xFFE7E5E4))
        BiomeType.CELESTIAL_ISLES -> Pair(Color(0xFF1E3A5F), Color(0xFFFDE68A))
        BiomeType.STAR_RUINS -> Pair(Color(0xFF0F0A2E), Color(0xFFC4B5FD))
        BiomeType.PRIMORDIAL_CHAOS -> Pair(Color(0xFF1A0B14), Color(0xFFF472B6))
    }

    Box(
        modifier = modifier
            .background(PixelPalette.Black)
            .border(2.dp, if (isSelected) PixelPalette.GoldLight else PixelPalette.Slate)
            .background(primaryBg)
            .clickable(onClick = onClick)
            .padding(8.dp)
            .testTag("pixel_tile_${region.id}")
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = region.biome.emoji,
                    fontSize = 18.sp
                )
                Text(
                    text = "LV.${region.dangerLevel}",
                    color = PixelPalette.GoldLight,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = region.name,
                color = PixelPalette.White,
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )

            Text(
                text = "${region.explorationProgress}% ODKRYTO",
                color = accentTile,
                fontFamily = FontFamily.Monospace,
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

/**
 * 2D Pixel Battle Stage with Cultivator vs Boss
 */
@Composable
fun PixelBattleStage(
    discipleHp: Int,
    discipleMaxHp: Int,
    enemyHp: Int,
    enemyMaxHp: Int,
    enemyName: String,
    discipleName: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(130.dp)
            .background(PixelPalette.Black)
            .border(2.dp, PixelPalette.Gold)
            .padding(8.dp)
    ) {
        // Floor line
        Canvas(modifier = Modifier.fillMaxSize()) {
            val groundY = size.height - 20f
            drawLine(
                color = PixelPalette.Slate,
                start = Offset(0f, groundY),
                end = Offset(size.width, groundY),
                strokeWidth = 3f
            )

            // Dotted retro ground tiles
            var x = 0f
            while (x < size.width) {
                drawRect(
                    color = PixelPalette.JadeDark,
                    topLeft = Offset(x, groundY + 4f),
                    size = Size(8f, 4f)
                )
                x += 16f
            }
        }

        // Disciple Sprite on Left
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 16.dp, bottom = 12.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                PixelSpriteView(
                    spriteMatrix = PixelSprites.SwordImmortal16,
                    primaryColor = PixelPalette.Cyan,
                    secondaryColor = PixelPalette.White,
                    pixelSizeDp = 3.5.dp
                )
                Text(
                    text = discipleName.take(8),
                    color = PixelPalette.CyanLight,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Versus Battle Text Center
        Text(
            text = "VS",
            color = PixelPalette.Crimson,
            fontFamily = FontFamily.Monospace,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.Center)
        )

        // Boss Sprite on Right
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = 12.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                PixelSpriteView(
                    spriteMatrix = PixelSprites.DragonBoss16,
                    primaryColor = PixelPalette.Crimson,
                    secondaryColor = PixelPalette.Gold,
                    accentColor = PixelPalette.CrimsonLight,
                    pixelSizeDp = 4.dp
                )
                Text(
                    text = enemyName.take(8),
                    color = PixelPalette.CrimsonLight,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
