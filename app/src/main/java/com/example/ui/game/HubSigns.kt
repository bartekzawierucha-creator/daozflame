package com.example.ui.game

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.game.WuxiaUiState
import com.example.model.ContractType
import com.example.ui.aaa.Aaa
import com.example.ui.aaa.AaaType
import kotlin.math.PI
import kotlin.math.sin

/*
 * Znaki nad sektą: pływające ikony przy budynkach, które coś robią po dotknięciu.
 * Zastępują tekstowe alerty tam, gdzie sprawa ma swoje miejsce na mapie (pola, tablica kontraktów, gniazdo, pawilon).
 */

/** Co robi znak: akcja natychmiastowa albo skrót do panelu. */
sealed interface SignAction {
    data object Harvest : SignAction
    data class Open(val shortcut: Shortcut?, val module: GameModule) : SignAction
}

data class HubSign(
    val id: String,
    /** Pozycja w ułamkach grafiki sceny (jak budynki). */
    val fx: Float,
    val fy: Float,
    val glyph: String,
    val caption: String,
    val color: Color,
    val action: SignAction
)

private fun first(name: String) = name.substringBefore(" (").substringBefore(" ")

/** Znaki z gotowego stanu UI – bez logiki gry. Kolejność = ważność (najwyżej 6 naraz). */
fun hubSigns(ui: WuxiaUiState): List<HubSign> = buildList {
    ui.raid?.let { r ->
        val days = (r.arriveDay - ui.currentDay).coerceAtLeast(0)
        add(HubSign("raid", 0.82f, 0.66f, "⚔", if (days == 0) "Najazd!" else "Najazd · ${days} d.", Aaa.Cinnabar, SignAction.Open(Shortcut.RAID, GameModule.CLANS)))
    }
    val ripe = ui.economy.fields.count { it.herb != null && it.readyDay <= ui.currentDay }
    if (ripe > 0) add(HubSign("harvest", 0.70f, 0.63f, "🌿", "Zbierz $ripe", Aaa.Jade, SignAction.Harvest))
    val deliverable = ui.economy.contracts.count { it.type == ContractType.DELIVER && (ui.inventory[it.item] ?: 0) >= it.quantity }
    if (deliverable > 0) add(HubSign("contracts", 0.44f, 0.50f, "📜", "Kontrakt ×$deliverable", Aaa.Gold, SignAction.Open(Shortcut.CONTRACTS, GameModule.PALACE)))
    if (ui.sectLife.eggs.isNotEmpty()) add(HubSign("eggs", 0.27f, 0.19f, "🥚", "Jajo ×${ui.sectLife.eggs.size}", Aaa.GoldHi, SignAction.Open(Shortcut.EGGS, GameModule.BEASTS)))
    ui.secretRealms.firstOrNull { it.open && !it.visited }?.let { add(HubSign("realm", 0.40f, 0.14f, "🌀", shortName(it.name, 2), Aaa.Violet, SignAction.Open(Shortcut.SECRET_REALMS, GameModule.WORLD))) }

    // Myśli uczniów nad Pawilonem Adeptów (autonomia): kto stoi u progu, kto ranny, kto zamknięty w odosobnieniu.
    val thoughts = ui.disciples.mapNotNull { d ->
        when {
            d.realmProgress >= 100 && com.example.game.engine.Realms.atPeak(d) -> Triple(0, "⚡", "${first(d.name)} u progu")
            d.currentHp < d.maxHp / 2 -> Triple(1, "🩹", "${first(d.name)} ranny")
            d.secludedUntilDay >= ui.currentDay -> Triple(2, "🧘", "${first(d.name)} medytuje")
            else -> null
        }
    }.sortedBy { it.first }.take(2)
    thoughts.forEachIndexed { i, (_, glyph, text) ->
        add(HubSign("mind_$i", 0.78f + 0.10f * i, 0.50f + 0.035f * i, glyph, text, Aaa.Azure, SignAction.Open(null, GameModule.ADEPTS)))
    }
}.take(6)

/** Warstwa znaków – rysowana w układzie sceny (te same ułamki co budynki). */
@Composable
fun HubSignLayer(ui: WuxiaUiState, sceneW: Dp, sceneH: Dp, onHarvest: () -> Unit, onOpen: OpenModule) {
    val signs = hubSigns(ui)
    if (signs.isEmpty()) return
    val t = rememberInfiniteTransition(label = "hub_signs")
    val phase by t.animateFloat(0f, 1f, infiniteRepeatable(tween(2600, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "hub_signs_t")
    signs.forEachIndexed { i, sign ->
        val bob = sin((phase + i * 0.37f) * PI.toFloat()) * 5f
        val ref = remember(sign.id) { CenterRef() }
        Box(
            Modifier
                .offset(x = sceneW * sign.fx, y = sceneH * sign.fy)
                .size(0.dp)
        ) {
            Column(
                Modifier
                    .wrapContentSize(align = Alignment.BottomCenter, unbounded = true)
                    .graphicsLayer { translationY = bob * density }
                    .trackCenter(ref)
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, role = Role.Button) {
                        when (val a = sign.action) {
                            SignAction.Harvest -> onHarvest()
                            is SignAction.Open -> { PanelNav.pending = a.shortcut; onOpen(a.module, ref.value) }
                        }
                    }
                    .testTag("hub_sign_${sign.id}"),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Box(
                    Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(Brush.radialGradient(listOf(sign.color.copy(alpha = 0.55f), Aaa.Ink0.copy(alpha = 0.85f))))
                        .border(1.5.dp, sign.color, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(sign.glyph, fontSize = 16.sp)
                }
                Text(
                    sign.caption,
                    style = AaaType.Label.copy(fontSize = 8.5.sp, color = Aaa.TextHi, letterSpacing = 0.sp),
                    maxLines = 1,
                    modifier = Modifier
                        .background(Aaa.Ink0.copy(alpha = 0.78f))
                        .border(1.dp, sign.color.copy(alpha = 0.5f))
                        .padding(horizontal = 5.dp, vertical = 1.dp)
                )
            }
        }
    }
}
