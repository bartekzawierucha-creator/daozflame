package com.example.ui.game

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.game.WuxiaGameViewModel
import com.example.game.WuxiaUiState
import com.example.game.engine.Gear
import com.example.model.Artifact
import com.example.model.Disciple
import com.example.model.EquipSlot
import com.example.model.MartialSchool
import com.example.ui.aaa.Aaa
import com.example.ui.aaa.AaaType
import com.example.ui.graphics.rememberWuxiaGraphic

/*
 * Etap 11: ekwipunek jak w prawdziwej grze – postać w środku, sześć miejsc dookoła,
 * pod spodem statystyki i komplet, niżej skarbiec jako siatka przedmiotów z ramkami rzadkości.
 * Dotknięcie przedmiotu otwiera kartę z porównaniem (▲/▼) i przyciskiem Załóż / Zdejmij.
 */

// ─── Grafika ─────────────────────────────────────────────────────────────────

private val FEMALE = mapOf(
    MartialSchool.SWORD_IMMORTAL to listOf("sword_buns", "sword_hairpin"),
    MartialSchool.BODY_ASURA to listOf("asura_braid"),
    MartialSchool.ELEMENTAL_TAOIST to listOf("taoist_flame", "taoist_sage"),
    MartialSchool.SHADOW_VIPER to listOf("viper_masked", "viper_pale")
)
private val MALE = mapOf(
    MartialSchool.SWORD_IMMORTAL to listOf("sword_ponytail", "sword_scar"),
    MartialSchool.BODY_ASURA to listOf("asura_grin", "asura_wild", "asura_monk"),
    MartialSchool.ELEMENTAL_TAOIST to listOf("taoist_lightning", "taoist_scholar"),
    MartialSchool.SHADOW_VIPER to listOf("viper_hooded", "viper_elder")
)

/** Portret ucznia: szkoła + płeć, wybór stały dla danego ucznia. */
fun portraitKey(d: Disciple): String {
    val pool = (if (d.gender == "Kobieta") FEMALE else MALE)[d.martialSchool] ?: listOf("sword_ponytail")
    return "portrait_" + pool[Math.floorMod(d.id.hashCode(), pool.size)]
}

/** Ikona artefaktu z grafik gry; null = rysujemy znak miejsca. */
fun gearIconKey(a: Artifact): String? {
    val n = a.name.lowercase()
    fun has(vararg w: String) = w.any { it in n }
    return "gear_" + when {
        has("pierścień", "sygnet") -> if (has("płom", "piekiel", "ogn")) "hellfire_ring" else "storage_ring"
        has("hełm", "kaptur") -> "qilin_helmet"
        has("diadem", "opask", "spink") -> "pearl_diadem"
        has("koron") -> "void_crown"
        has("karwasz", "bransolet", "naramienn") -> "sun_bracers"
        has("rękawic") -> "dragon_gauntlets"
        has("wachlarz") -> "flame_fan"
        has("młot") -> "forge_hammer"
        has("łuk") -> "roc_bow"
        has("bicz") -> "snake_whip"
        has("laska") -> if (has("złot", "mędrc", "małp")) "monkey_staff" else "star_staff"
        has("kieł", "bazylisz", "trucizn") -> "basilisk_fang"
        has("sztylet", "nóż") -> "obsidian_daggers"
        has("włóczni", "halabard") -> "red_spear"
        has("miecz", "ostrz", "szabl") -> "jade_sword"
        has("tarcz") -> "kraken_shield"
        has("płaszcz") -> "void_cloak"
        has("szat") -> "phoenix_robe"
        has("kości", "lisz") && has("zbroj", "pancerz") -> "lich_armor"
        has("pancerz", "zbroj", "kamizel") -> "star_armor"
        has("dzwon") -> "golden_bell"
        has("zwierciadł", "lustr") -> "bagua_mirror"
        has("kocioł") -> "chaos_cauldron"
        has("wisior", "amulet", "nefryt") -> "jade_pendant"
        has("gwiazdozbiór", "konstelac", "perł") -> "constellation"
        has("talizman") -> if (has("ogn")) "talisman_fire" else if (has("lecz")) "talisman_heal" else "talisman_guard"
        else -> return null
    }
}

fun gearRarityColor(rarity: String): Color = when (rarity) {
    "Rzadki" -> Aaa.Jade
    "Niebiański" -> Aaa.Azure
    "Boski" -> Aaa.Ember
    "Nieśmiertelny" -> Aaa.GoldHi
    "Wieczne Dao" -> Aaa.Violet
    else -> Aaa.TextMid
}

private fun frameKey(rarity: String?): String = when (rarity) {
    null -> "frame_carved_dark"
    "Rzadki" -> "frame_talent_a"
    "Niebiański" -> "frame_talent_s"
    "Boski" -> "frame_talent_ss"
    "Nieśmiertelny", "Wieczne Dao" -> "frame_talent_sss"
    else -> "frame_talent_b"
}

@Composable
private fun Art(key: String, modifier: Modifier, scale: ContentScale = ContentScale.Fit, alignment: Alignment = Alignment.Center) {
    rememberWuxiaGraphic(key)?.let { Image(it, null, modifier, alignment = alignment, contentScale = scale) }
}

/** Kafel przedmiotu: ramka rzadkości, ikona albo znak miejsca, poziom ulepszenia. */
@Composable
fun GearTile(
    a: Artifact?,
    slot: EquipSlot,
    size: Dp,
    selected: Boolean = false,
    better: Boolean = false,
    onClick: () -> Unit
) {
    val c = a?.let { gearRarityColor(it.rarity) } ?: Aaa.TextLow
    Box(
        Modifier
            .size(size)
            .drawBehind {
                if (a != null) drawCircle(Brush.radialGradient(listOf(c.copy(alpha = 0.35f), Color.Transparent)), radius = this.size.minDimension * 0.62f)
                if (selected) drawCircle(Brush.radialGradient(listOf(Aaa.GoldHi.copy(alpha = 0.5f), Color.Transparent)), radius = this.size.minDimension * 0.8f)
            }
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Box(Modifier.fillMaxSize().padding(size * 0.16f).background(Aaa.Ink0.copy(alpha = 0.75f)))
        if (a != null) {
            val icon = gearIconKey(a)
            if (icon != null) Art(icon, Modifier.fillMaxSize().padding(size * 0.2f))
            else Text(slot.glyph, fontSize = (size.value * 0.36f).sp, color = c)
        } else {
            Text(slot.glyph, fontSize = (size.value * 0.3f).sp, modifier = Modifier.alpha(0.28f))
        }
        Art(frameKey(a?.rarity), Modifier.fillMaxSize())
        if (a != null && a.plus > 0) Text(
            "+${a.plus}", style = AaaType.Numeric.copy(fontSize = 10.sp, color = Aaa.GoldHi, fontWeight = FontWeight.Bold),
            modifier = Modifier.align(Alignment.TopEnd).padding(top = size * 0.1f, end = size * 0.12f)
        )
        if (better) Text("▲", fontSize = 11.sp, color = Aaa.Jade, modifier = Modifier.align(Alignment.BottomEnd).padding(bottom = size * 0.1f, end = size * 0.12f))
    }
}

// ─── Ekran ───────────────────────────────────────────────────────────────────

private sealed interface Pick {
    data class Worn(val slot: EquipSlot) : Pick
    data class Vault(val index: Int) : Pick
}

@Composable
fun GearPanel(d: Disciple, ui: WuxiaUiState, viewModel: WuxiaGameViewModel, onClose: () -> Unit) {
    var pick by remember(d.id) { mutableStateOf<Pick?>(null) }
    var filter by remember(d.id) { mutableStateOf<EquipSlot?>(null) }
    val school = schoolColor(d.martialSchool)
    // Zaznaczenie po zmianie stanu (np. po założeniu) może wskazywać już nieistniejący indeks.
    val vaultPick = (pick as? Pick.Vault)?.index?.let { i -> ui.vaultArtifacts.getOrNull(i)?.let { i to it } }
    val wornPick = (pick as? Pick.Worn)?.slot?.let { s -> Gear.inSlot(d, s)?.let { s to it } }

    Box(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Aaa.Ink2, Aaa.Ink0)))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {}
    ) {
        Column(Modifier.fillMaxSize().safeTop()) {
            // Nagłówek
            Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Overline("Ekwipunek", color = school)
                    Text(d.name.substringBefore(" ("), fontFamily = BrushFont, fontSize = 26.sp, color = Aaa.GoldHi, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Box(
                    Modifier.size(36.dp).clip(CircleShape).background(Aaa.Ink0.copy(alpha = 0.6f))
                        .clickable(role = Role.Button, onClick = onClose).testTag("gear_close"),
                    contentAlignment = Alignment.Center
                ) { Text("✕", color = Aaa.Gold, fontSize = 16.sp) }
            }

            Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                PaperDoll(d, ui, school, selected = (pick as? Pick.Worn)?.slot) { slot ->
                    pick = if (Gear.inSlot(d, slot) != null) Pick.Worn(slot) else null
                    filter = slot
                }
                StatsStrip(d)
                Spacer(Modifier.height(8.dp))
                // Karta wybranego przedmiotu
                when {
                    wornPick != null -> ItemCard(d, wornPick.second, worn = true,
                        onAction = { viewModel.unequip(d.id, wornPick.first); pick = null })
                    vaultPick != null -> ItemCard(d, vaultPick.second, worn = false,
                        onAction = { viewModel.equip(d.id, vaultPick.first); pick = Pick.Worn(Gear.slotOf(vaultPick.second)) })
                }
                VaultGrid(d, ui, filter, onFilter = { filter = it }, selectedIndex = vaultPick?.first) { pick = Pick.Vault(it) }
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

/** Postać w środku, po trzy miejsca z każdej strony; pod postacią komplet. */
@Composable
private fun PaperDoll(d: Disciple, ui: WuxiaUiState, school: Color, selected: EquipSlot?, onSlot: (EquipSlot) -> Unit) {
    val spin = rememberInfiniteTransition(label = "seal")
    val angle by spin.animateFloat(0f, 360f, infiniteRepeatable(tween(90_000, easing = LinearEasing), RepeatMode.Restart), label = "seal_angle")
    val left = listOf(EquipSlot.WEAPON, EquipSlot.HANDS, EquipSlot.RING)
    val right = listOf(EquipSlot.HEAD, EquipSlot.ARMOR, EquipSlot.RELIC)
    Box(Modifier.fillMaxWidth().height(330.dp)) {
        // Tło: poświata szkoły i obracająca się pieczęć.
        Box(Modifier.matchParentSize().drawBehind {
            drawCircle(Brush.radialGradient(listOf(school.copy(alpha = 0.28f), Color.Transparent), center = Offset(size.width / 2, size.height * 0.45f), radius = size.width * 0.42f))
        })
        Art("orn_seal_ring", Modifier.align(Alignment.Center).offset(y = (-14).dp).size(250.dp).rotate(angle).alpha(0.45f))
        Art(portraitKey(d), Modifier.align(Alignment.BottomCenter).padding(bottom = 30.dp).width(190.dp).fillMaxHeight(0.92f), alignment = Alignment.BottomCenter)
        // Wtopienie dołu portretu w tło.
        Box(Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(90.dp).background(Brush.verticalGradient(listOf(Color.Transparent, Aaa.Ink1))))
        Column(Modifier.align(Alignment.CenterStart).padding(start = 10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            left.forEach { s -> SlotWithLabel(d, s, selected == s) { onSlot(s) } }
        }
        Column(Modifier.align(Alignment.CenterEnd).padding(end = 10.dp), verticalArrangement = Arrangement.spacedBy(10.dp), horizontalAlignment = Alignment.End) {
            right.forEach { s -> SlotWithLabel(d, s, selected == s) { onSlot(s) } }
        }
        // Królestwo i komplet pod postacią.
        Column(Modifier.align(Alignment.BottomCenter).padding(bottom = 4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(com.example.game.engine.Realms.label(d), style = AaaType.Label.copy(fontSize = 11.sp, color = Aaa.TextHi), maxLines = 1)
            SetPips(d)
        }
    }
}

@Composable
private fun SlotWithLabel(d: Disciple, slot: EquipSlot, selected: Boolean, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        GearTile(Gear.inSlot(d, slot), slot, 64.dp, selected = selected, onClick = onClick)
        Text(slot.polishName, style = AaaType.Overline.copy(fontSize = 8.5.sp, color = if (Gear.inSlot(d, slot) != null) Aaa.GoldMid else Aaa.TextLow), modifier = Modifier.padding(top = 1.dp))
    }
}

/** Sześć kropek kompletu i bieżąca premia. */
@Composable
private fun SetPips(d: Disciple) {
    val n = Gear.items(d).size
    val pct = Gear.setPercent(n)
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        repeat(EquipSlot.entries.size) { i ->
            Box(Modifier.size(7.dp).clip(CircleShape).background(if (i < n) Aaa.Gold else Aaa.Ink4))
        }
        Text(
            if (pct > 0) "  komplet +$pct%" else "  komplet od ${Gear.SET_HALF_PIECES}",
            style = AaaType.Label.copy(fontSize = 9.5.sp, color = if (pct > 0) Aaa.GoldHi else Aaa.TextLow)
        )
    }
}

@Composable
private fun StatsStrip(d: Disciple) {
    val (ga, gd) = Gear.bonus(d)
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 14.dp).background(Aaa.Ink3.copy(alpha = 0.8f)).border(1.dp, Aaa.Hairline).padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        Stat("Atak", "${d.attack}", if (ga > 0) "+$ga z ekw." else null)
        Stat("Obrona", "${d.defense}", if (gd > 0) "+$gd z ekw." else null)
        Stat("HP", "${d.maxHp}", if (d.scars > 0) "blizny ${d.scars}" else null)
        Stat("Moc", "${d.combatPower}", null)
    }
}

@Composable
private fun Stat(label: String, value: String, note: String?) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label.uppercase(), style = AaaType.Overline.copy(fontSize = 8.5.sp, color = Aaa.TextLow))
        Text(value, style = AaaType.Numeric.copy(fontSize = 15.sp, color = Aaa.TextHi, fontWeight = FontWeight.Bold))
        Text(note ?: " ", style = AaaType.Label.copy(fontSize = 8.5.sp, color = Aaa.Jade))
    }
}

/** Karta przedmiotu z porównaniem: ile zmieni się atak i obrona. */
@Composable
private fun ItemCard(d: Disciple, a: Artifact, worn: Boolean, onAction: () -> Unit) {
    val c = gearRarityColor(a.rarity)
    val slot = Gear.slotOf(a)
    val current = Gear.inSlot(d, slot)
    val (na, nd) = if (worn) d.attack to d.defense else Gear.preview(d, a)
    Row(
        Modifier
            .padding(horizontal = 14.dp)
            .fillMaxWidth()
            .background(Brush.horizontalGradient(listOf(c.copy(alpha = 0.16f), Aaa.Ink3.copy(alpha = 0.9f))))
            .border(1.dp, c.copy(alpha = 0.6f))
            .padding(10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        GearTile(a, slot, 76.dp) {}
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(a.name + if (a.plus > 0) " +${a.plus}" else "", style = AaaType.Label.copy(fontSize = 13.sp, color = c, fontWeight = FontWeight.Bold), maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text("${slot.glyph} ${slot.polishName} · ${a.rarity} · ${a.quality.polishName}", style = AaaType.Label.copy(fontSize = 9.5.sp, color = Aaa.TextMid), maxLines = 1)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Delta("Atak", d.attack, na, worn)
                Delta("Obrona", d.defense, nd, worn)
            }
            if (a.specialEffect.isNotBlank()) Text(a.specialEffect, style = AaaType.Label.copy(fontSize = 10.sp, color = Aaa.TextHi), maxLines = 2, overflow = TextOverflow.Ellipsis)
            if (!worn && current != null) Text("Zastąpi: ${current.name}", style = AaaType.Label.copy(fontSize = 9.sp, color = Aaa.TextLow), maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        GhostButton(if (worn) "Zdejmij" else "Załóż", onClick = onAction, height = 36.dp, danger = worn)
    }
}

@Composable
private fun Delta(label: String, before: Int, after: Int, worn: Boolean) {
    val diff = after - before
    val col = when { worn || diff == 0 -> Aaa.TextMid; diff > 0 -> Aaa.Jade; else -> Aaa.Cinnabar }
    Text(
        if (worn) "$label $before" else "$label $before → $after " + if (diff > 0) "▲" else if (diff < 0) "▼" else "",
        style = AaaType.Numeric.copy(fontSize = 10.5.sp, color = col)
    )
}

/** Skarbiec jako siatka: filtr miejsc, ramki rzadkości, ▲ przy przedmiotach lepszych od założonych. */
@Composable
private fun VaultGrid(d: Disciple, ui: WuxiaUiState, filter: EquipSlot?, onFilter: (EquipSlot?) -> Unit, selectedIndex: Int?, onPick: (Int) -> Unit) {
    val all = ui.vaultArtifacts.withIndex().toList()
    val shown = all.filter { filter == null || Gear.slotOf(it.value) == filter }
        .sortedWith(compareBy<IndexedValue<Artifact>> { Gear.slotOf(it.value).ordinal }.thenByDescending { it.value.attackBonus + it.value.defenseBonus })
    Column(Modifier.padding(horizontal = 14.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Section("Skarbiec sekty", "${all.size}")
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            SelectChip("Wszystko ${all.size}", filter == null, Aaa.Gold) { onFilter(null) }
            EquipSlot.entries.forEach { s ->
                val n = all.count { Gear.slotOf(it.value) == s }
                SelectChip("${s.glyph} $n", filter == s, Aaa.Gold, enabled = n > 0) { onFilter(s) }
            }
        }
        if (shown.isEmpty()) {
            SheetBody(
                if (all.isEmpty()) "Skarbiec jest pusty. Artefakty wykuwa Boska Kuźnia, przynoszą je kupcy, groty i bossowie."
                else "Brak przedmiotów na to miejsce.", color = Aaa.TextLow
            )
        }
        val cols = 5
        shown.chunked(cols).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                row.forEach { (i, a) ->
                    val cur = Gear.inSlot(d, Gear.slotOf(a))
                    val better = (a.attackBonus + a.defenseBonus) > ((cur?.attackBonus ?: 0) + (cur?.defenseBonus ?: 0))
                    Box(Modifier.weight(1f).aspectRatio(1f), contentAlignment = Alignment.Center) {
                        GearTile(a, Gear.slotOf(a), 64.dp, selected = i == selectedIndex, better = better) { onPick(i) }
                    }
                }
                repeat(cols - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}
