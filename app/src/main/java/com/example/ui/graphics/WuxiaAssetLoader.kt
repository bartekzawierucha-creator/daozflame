package com.example.ui.graphics

import android.content.Context
import android.graphics.BitmapFactory
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.MartialSchool
import com.example.ui.pixelart.PixelCard
import com.example.ui.pixelart.SchoolPixelAvatar
import java.util.concurrent.ConcurrentHashMap

/**
 * Bezpieczny menedżer ładowania zewnętrznych grafik Manhwa dostarczonych przez użytkownika.
 * Szuka pliku w:
 * 1. res/drawable/<name>
 * 2. assets/wuxia/<name>.jpg (.png, .webp)
 */
object WuxiaAssetLoader {

    private val cache = ConcurrentHashMap<String, ImageBitmap>()

    fun loadBitmapFromAssetsOrDrawable(context: Context, assetName: String): ImageBitmap? {
        val cleanName = assetName.substringBeforeLast(".")

        cache[cleanName]?.let { return it }

        // 1. Sprawdź zasoby R.drawable
        try {
            val resId = context.resources.getIdentifier(cleanName, "drawable", context.packageName)
            if (resId != 0) {
                val bitmap = BitmapFactory.decodeResource(context.resources, resId)
                if (bitmap != null) {
                    val img = bitmap.asImageBitmap()
                    cache[cleanName] = img
                    return img
                }
            }
        } catch (_: Exception) {}

        // 2. Sprawdź assets/wuxia/
        val extensions = listOf(".jpg", ".png", ".webp", ".jpeg", "")
        for (ext in extensions) {
            val path = "wuxia/$cleanName$ext"
            try {
                context.assets.open(path).use { stream ->
                    val bitmap = BitmapFactory.decodeStream(stream)
                    if (bitmap != null) {
                        val img = bitmap.asImageBitmap()
                        cache[cleanName] = img
                        return img
                    }
                }
            } catch (_: Exception) {}
        }

        return null
    }

    fun getSchoolAvatarKey(school: MartialSchool): String {
        return when (school) {
            MartialSchool.SWORD_IMMORTAL -> "avatar_sword_immortal"
            MartialSchool.BODY_ASURA -> "avatar_body_asura"
            MartialSchool.ELEMENTAL_TAOIST -> "avatar_elemental_taoist"
            MartialSchool.SHADOW_VIPER -> "avatar_shadow_viper"
        }
    }

    fun getBeastAvatarKey(beastName: String): String {
        return when {
            beastName.contains("Qilin", ignoreCase = true) || beastName.contains("Ognist", ignoreCase = true) -> "beast_flame_qilin"
            beastName.contains("Feniks", ignoreCase = true) || beastName.contains("Mroźn", ignoreCase = true) || beastName.contains("Lodow", ignoreCase = true) -> "beast_frost_phoenix"
            beastName.contains("Smok", ignoreCase = true) || beastName.contains("Dragon", ignoreCase = true) || beastName.contains("Piorun", ignoreCase = true) -> "beast_azure_dragon"
            else -> "beast_flame_qilin"
        }
    }
}

@Composable
fun rememberWuxiaGraphic(assetName: String): ImageBitmap? {
    val context = LocalContext.current
    return remember(assetName) {
        WuxiaAssetLoader.loadBitmapFromAssetsOrDrawable(context, assetName)
    }
}

/**
 * Portret Manhwa dla ucznia / mistrza z ramą nefrytowo-złotą i poświatą Qi.
 */
@Composable
fun ManhwaSchoolAvatar(
    school: MartialSchool,
    modifier: Modifier = Modifier,
    size: Dp = 56.dp,
    showAura: Boolean = true
) {
    val assetKey = WuxiaAssetLoader.getSchoolAvatarKey(school)
    val bitmap = rememberWuxiaGraphic(assetKey)

    val schoolColor = when (school) {
        MartialSchool.SWORD_IMMORTAL -> Color(0xFF00E5FF)
        MartialSchool.BODY_ASURA -> Color(0xFFFF5252)
        MartialSchool.ELEMENTAL_TAOIST -> Color(0xFFEBCB7A)
        MartialSchool.SHADOW_VIPER -> Color(0xFFE040FB)
    }

    val infiniteTransition = rememberInfiniteTransition(label = "avatar_aura")
    val auraAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "aura_alpha"
    )

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        if (showAura) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(2.dp)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                schoolColor.copy(alpha = auraAlpha * 0.6f),
                                Color.Transparent
                            )
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )
            )
        }

        if (bitmap != null) {
            Image(
                bitmap = bitmap,
                contentDescription = school.polishName,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(10.dp))
                    .border(
                        width = 1.5.dp,
                        brush = Brush.linearGradient(
                            colors = listOf(
                                Color(0xFFE8C37E),
                                schoolColor,
                                Color(0xFF8B6508)
                            )
                        ),
                        shape = RoundedCornerShape(10.dp)
                    )
            )
        } else {
            // Fallback na Pixel Art
            SchoolPixelAvatar(
                school = school,
                sizeDp = size
            )
        }
    }
}

/**
 * Epicki baner Manhwa z ciemną winietą i złotą kaligrafią
 */
@Composable
fun ManhwaHeroBanner(
    assetName: String,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    height: Dp = 190.dp
) {
    val bitmap = rememberWuxiaGraphic(assetName)

    PixelCard(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .shadow(8.dp, RoundedCornerShape(14.dp)),
        backgroundColor = Color(0xFF0F0B18),
        borderColor = Color(0xFFE8C37E)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (bitmap != null) {
                Image(
                    bitmap = bitmap,
                    contentDescription = title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }

            // Manhwa cinematic dark gradient overlay
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.25f),
                                Color.Black.copy(alpha = 0.5f),
                                Color(0xFF0E0B10).copy(alpha = 0.95f)
                            )
                        )
                    )
            )

            // Dynamic ink wash and auspicious text
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(6.dp, 18.dp)
                            .background(Color(0xFFE8C37E), RoundedCornerShape(2.dp))
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = title,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Black,
                            fontSize = 20.sp,
                            letterSpacing = 0.5.sp
                        ),
                        color = Color(0xFFFFF9E6)
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    text = subtitle,
                    maxLines = 2,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    ),
                    color = Color(0xFFF3DDA6).copy(alpha = 0.9f)
                )
            }
        }
    }
}
