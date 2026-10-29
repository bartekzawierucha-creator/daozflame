package com.example.ui.game

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.game.engine.DiscipleRules.has
import com.example.game.engine.Realms
import com.example.model.CultivationPath
import com.example.model.Disciple
import com.example.model.DiscipleRole
import com.example.model.FacilityType
import com.example.model.MajorRealm
import com.example.model.MartialSchool
import com.example.model.MethodGrade
import com.example.model.OpportunityKind
import com.example.model.OriginType
import com.example.model.TalentGrade
import com.example.model.TimeOfDay
import com.example.ui.aaa.Aaa
import com.example.ui.aaa.AaaType
import com.example.ui.graphics.rememberWuxiaGraphic

/*
 * Etap 16: jedna mapa „rzecz w grze → grafika”. Ekrany pytają tutaj o klucz zamiast zgadywać po nazwie.
 * Klucze to pliki w assets/wuxia/. Brak pliku = komponent rysuje zastępczy znak, więc nic się nie wysypie.
 */

// ─── Uczniowie ───────────────────────────────────────────────────────────────

/** Postać ucznia: duża grafika na kartę + portret wycięty z tej samej grafiki (twarz zawsze ta sama). */
private data class Look(val hero: String, val portrait: String)

private fun looks(slug: String, g: String, classic: String? = null): List<Look> =
    (1..3).map { Look("hero_${slug}_$g$it", "portrait_${slug}_$g$it") } +
        listOfNotNull(classic?.let { Look("avatar_$it", "portrait_classic_$it") })

private val FEMALE_POOL = mapOf(
    MartialSchool.SWORD_IMMORTAL to looks("sword", "f"),
    MartialSchool.BODY_ASURA to looks("asura", "f"),
    MartialSchool.ELEMENTAL_TAOIST to looks("taoist", "f"),
    MartialSchool.SHADOW_VIPER to looks("viper", "f", "shadow_viper")
)
private val MALE_POOL = mapOf(
    MartialSchool.SWORD_IMMORTAL to looks("sword", "m", "sword_immortal"),
    MartialSchool.BODY_ASURA to looks("asura", "m", "body_asura"),
    MartialSchool.ELEMENTAL_TAOIST to looks("taoist", "m", "elemental_taoist"),
    MartialSchool.SHADOW_VIPER to looks("viper", "m")
)

private fun female(d: Disciple) = d.gender == "Kobieta"

private fun look(d: Disciple): Look {
    val pool = (if (female(d)) FEMALE_POOL else MALE_POOL)[d.martialSchool] ?: looks("sword", "m", "sword_immortal")
    return pool[Math.floorMod(d.id.hashCode(), pool.size)]
}

/** Portret ucznia – wycinek twarzy z jego dużej grafiki. */
fun portraitKey(d: Disciple): String = look(d).portrait

private fun schoolSlug(s: MartialSchool) = when (s) {
    MartialSchool.SWORD_IMMORTAL -> "sword"
    MartialSchool.BODY_ASURA -> "asura"
    MartialSchool.ELEMENTAL_TAOIST -> "taoist"
    MartialSchool.SHADOW_VIPER -> "viper"
}

/** Pełna postać na karcie ucznia (ta sama osoba co na portrecie). Druga wartość: wycinek bez tła – teraz zawsze false. */
fun heroKey(d: Disciple): Pair<String, Boolean> = look(d).hero to false

fun roleKey(r: DiscipleRole) = "role_" + r.name.lowercase()

// ─── Kultywacja ──────────────────────────────────────────────────────────────

fun realmKey(m: MajorRealm?): String = when (m) {
    null, MajorRealm.BODY_TEMPERING, MajorRealm.QI_CONDENSATION -> "realm_qi_condensation"
    MajorRealm.FOUNDATION -> "realm_foundation"
    MajorRealm.CORE_FORMATION -> "realm_core_formation"
    MajorRealm.NASCENT_SOUL -> "realm_nascent_soul"
    MajorRealm.SOUL_TRANSFORMATION -> "realm_soul_transformation"
    MajorRealm.VOID_TRIBULATION -> "realm_void_tribulation"
    MajorRealm.MAHAYANA -> "realm_mahayana"
    MajorRealm.HEAVENLY_IMMORTAL -> "realm_heavenly_immortal"
    MajorRealm.CHAOS_GOLDEN_IMMORTAL -> "realm_chaos_immortal"
}

fun scrollKey(g: MethodGrade): String = when (g) {
    MethodGrade.YELLOW -> "scroll_yellow"
    MethodGrade.BLACK -> "scroll_black"
    MethodGrade.MYSTIC -> "scroll_mystic"
    MethodGrade.HEAVENLY -> "scroll_heavenly"
    MethodGrade.PRIMORDIAL -> "scroll_primordial"
}

/** Kolor stopnia zwoju – ten sam w całej grze. */
fun gradeColor(g: MethodGrade): Color = when (g) {
    MethodGrade.YELLOW -> Color(0xFFE2C25A)
    MethodGrade.BLACK -> Color(0xFFA9A3B5)
    MethodGrade.MYSTIC -> Color(0xFFB07CF0)
    MethodGrade.HEAVENLY -> Color(0xFFFFD86B)
    MethodGrade.PRIMORDIAL -> Color(0xFF7FE3E0)
}

// ─── Sekta i świat ───────────────────────────────────────────────────────────

fun buildingKey(t: FacilityType): String = when (t) {
    FacilityType.CULTIVATION_PAVILION -> "bld_pavilion"
    FacilityType.ALCHEMY_CHAMBER -> "bld_alchemy"
    FacilityType.DIVINE_FORGE -> "bld_forge"
    FacilityType.MARTIAL_ARCHIVES -> "bld_technique_tower"
    FacilityType.MISSION_HALL -> "bld_expedition"
    FacilityType.DIPLOMACY_HALL -> "bld_diplomacy"
}

fun timeKey(t: TimeOfDay): String = when (t) {
    TimeOfDay.DAWN -> "time_dawn"
    TimeOfDay.DAY -> "time_day"
    TimeOfDay.DUSK -> "time_dusk"
    TimeOfDay.NIGHT, TimeOfDay.FULL_MOON -> "time_night"
}

/** Tło krainy wypraw wg kolejności na mapie (bambus, lód, bagna, wulkan, pustynia, morze). */
private val REGION_BG = listOf("bg_region_bamboo", "bg_region_ice", "bg_region_swamp", "bg_region_volcano", "bg_region_desert", "bg_region_sea")
fun regionBgKey(index: Int): String = REGION_BG[Math.floorMod(index, REGION_BG.size)]

/** Tło krainy wg biomu. */
fun biomeBgKey(b: com.example.model.BiomeType): String = when (b) {
    com.example.model.BiomeType.BAMBOO_MIST -> REGION_BG[0]
    com.example.model.BiomeType.KUNLUN_PEAK, com.example.model.BiomeType.CELESTIAL_ISLES -> REGION_BG[1]
    com.example.model.BiomeType.VENOM_SWAMP, com.example.model.BiomeType.BONE_WASTES -> REGION_BG[2]
    com.example.model.BiomeType.LAVA_CHASM -> REGION_BG[3]
    com.example.model.BiomeType.SPIRIT_DESERT, com.example.model.BiomeType.THUNDER_PLATEAU, com.example.model.BiomeType.STAR_RUINS -> REGION_BG[4]
    com.example.model.BiomeType.DRAGON_ABYSS, com.example.model.BiomeType.SUNKEN_PALACE, com.example.model.BiomeType.PRIMORDIAL_CHAOS -> REGION_BG[5]
}

/** Tło krainy po nazwie (gdy nie znamy indeksu). */
fun regionBgKeyByName(name: String): String {
    val n = name.lowercase()
    return when {
        "bambus" in n || "las" in n -> REGION_BG[0]
        "lod" in n || "kunlun" in n || "śnie" in n || "szczyt" in n -> REGION_BG[1]
        "bagn" in n || "trucizn" in n -> REGION_BG[2]
        "law" in n || "ogn" in n || "wulkan" in n -> REGION_BG[3]
        "pusty" in n || "piask" in n || "burz" in n -> REGION_BG[4]
        "morz" in n || "ocean" in n || "głębin" in n -> REGION_BG[5]
        else -> REGION_BG[Math.floorMod(name.hashCode(), REGION_BG.size)]
    }
}

fun opportunityNpcKey(k: OpportunityKind): String = when (k) {
    OpportunityKind.WANDERING_MASTER -> "npc_wandering_master"
    OpportunityKind.RARE_MERCHANT -> "npc_desert_merchant"
    OpportunityKind.IMPERIAL_EXAM -> "npc_examiner"
    OpportunityKind.HIDDEN_CAVE -> "npc_herb_gatherer"
    OpportunityKind.REFUGEES -> "npc_refugees"
}

fun opportunityBgKey(k: OpportunityKind): String = when (k) {
    OpportunityKind.WANDERING_MASTER -> "bg_region_bamboo"
    OpportunityKind.RARE_MERCHANT -> "bg_region_desert"
    OpportunityKind.IMPERIAL_EXAM -> "bg_sect_sanctuary"
    OpportunityKind.HIDDEN_CAVE -> "bg_region_swamp"
    OpportunityKind.REFUGEES -> "bg_region_volcano"
}

/** Kadr zdarzenia losowego po słowach w tytule: postać, jeśli pasuje, inaczej tło krainy. */
fun eventArtKey(title: String): String {
    val t = title.lowercase()
    fun has(vararg w: String) = w.any { it in t }
    return when {
        has("kupc", "kupiec", "karawan", "handlarz") -> "npc_desert_merchant"
        has("uchodź", "głód", "wiosk") -> "npc_refugees"
        has("cesar", "dwór", "urzędni", "egzamin") -> "npc_examiner"
        has("wędrown", "pustelni", "mistrz") -> "npc_wandering_master"
        has("duch", "przodk", "założyciel") -> "npc_founder_ghost"
        has("bandy", "rozbój", "zbój") -> "foe_bandit"
        has("szpieg", "skrytob", "zamach") -> "foe_spy"
        has("demon serca", "opęta") -> "special_demonic"
        has("demon", "krwi", "kult") -> "foe_demonic"
        has("mnich", "klasztor") -> "special_monk"
        has("sierot", "dzieck", "dziec") -> "special_orphan"
        has("starość", "staroś", "umier", "odchodzi") -> "special_elder"
        has("zdrad") -> "special_traitor"
        has("geniusz", "talent", "cudown") -> "special_prodigy"
        has("rekrut", "nabór", "kandydat") -> "special_recruit_m"
        has("kalek", "okalecz", "rany") -> "special_crippled"
        has("besti", "smok") -> "beast_azure_dragon"
        has("piorun", "burz", "niebios") -> "bg_region_desert"
        has("grobow", "ruin", "wieża") -> "bg_tower"
        has("meteoryt", "gwiazd") -> "bg_region_ice"
        has("ogień", "ogni", "wulkan", "law") -> "bg_region_volcano"
        has("trucizn", "zaraz", "bagn") -> "bg_region_swamp"
        has("morz", "wod", "powód") -> "bg_region_sea"
        else -> REGION_BG[Math.floorMod(title.hashCode(), REGION_BG.size)]
    }
}

/** Przywódca wrogiej sekty – po nazwie / charakterze sekty. */
fun sectLeaderKey(name: String, alignment: String = ""): String {
    val n = (name + " " + alignment).lowercase()
    return when {
        "miecz" in n || "chmur" in n -> "foe_sword_sect"
        "węż" in n || "cień" in n || "cienia" in n || "żmij" in n || "skorpion" in n -> "foe_serpent_sect"
        "żuraw" in n || "fal" in n || "wod" in n || "morz" in n -> "foe_crane_sect"
        "demon" in n || "krwi" in n || "czaszk" in n -> "foe_demonic"
        else -> listOf("foe_sword_sect", "foe_serpent_sect", "foe_crane_sect", "foe_demonic")[Math.floorMod(name.hashCode(), 4)]
    }
}

/** Grafika przedmiotu z magazynu, jeśli istnieje (zwoje, fragmenty, zioła, rudy, pigułki, talizmany, jaja). */
fun itemArtKey(name: String): String? {
    val base = name.substringBefore(" (").substringBefore("×").trim()
    Realms.methodForScroll(base)?.let { return scrollKey(it.grade) }
    if (base.startsWith("Fragment:")) return "ico_fragment"
    if (base == com.example.game.engine.Creatures.EGG) return "st_egg"
    if (base == com.example.game.engine.Market.SPIRIT_PAPER) return "mat_talisman_paper"
    if (base == com.example.game.engine.DiscipleRules.FOOD) return "res_food"
    val n = base.lowercase()
    if (n.startsWith("talizman")) return when {
        "ogn" in n || "piorun" in n -> "talisman_fire"
        "uzdrow" in n -> "talisman_heal"
        else -> "talisman_guard"
    }
    if ("pigułk" in n || "eliksir" in n || "balsam" in n || "nektar" in n) return "pill_" + when {
        "feniks" in n -> "phoenix"
        "serc" in n -> "heart"
        "tribulac" in n || "pioru" in n || "pustki" in n -> "lightning"
        "złot" in n || "dzwon" in n || "słoń" in n -> "sun"
        "chaos" in n || "wniebo" in n -> "chaos"
        "oczyszcz" in n || "odporn" in n || "miazmat" in n || "detoks" in n -> "leaf"
        "dusz" in n || "oświec" in n || "świadom" in n -> "pearl"
        "kośc" in n || "krwi" in n || "asur" in n -> "blood"
        "młodo" in n || "życi" in n || "witaln" in n -> "frost"
        "cien" in n || "cień" in n -> "shadow"
        else -> "harmony"
    }
    return when {
        "skorpion" in n -> "mat_scorpion_root"
        "lotos" in n -> "mat_snow_lotus"
        "żeń-szeń" in n || "żeń szeń" in n -> "mat_dragon_ginseng"
        "zrozumienia" in n || "orchide" in n -> "mat_enlightenment_flower"
        "nefryt" in n -> "mat_spirit_jade"
        "gwiezdne żelazo" in n -> "mat_star_iron"
        "geocentr" in n -> "mat_fire_crystal"
        "złoty piasek" in n -> "mat_gold_crystal"
        "pustki" in n -> "mat_void_ore"
        "perła" in n -> "mat_blue_orb"
        "gromu" in n -> "mat_violet_crystal"
        else -> null
    }
}

// ─── Komponenty ──────────────────────────────────────────────────────────────

/** Ikona z grafiki (bez kadrowania). Gdy pliku brak – rysuje [fallback] (znak / emoji). */
@Composable
fun GameIcon(key: String, size: Dp, modifier: Modifier = Modifier, fallback: String? = null, dim: Boolean = false) {
    val bmp = rememberWuxiaGraphic(key)
    if (bmp != null) {
        Image(bmp, null, modifier.size(size), contentScale = ContentScale.Fit,
            colorFilter = if (dim) dimFilter(0.45f, 0f) else null)
    } else if (fallback != null) {
        Box(modifier.size(size), contentAlignment = Alignment.Center) { Text(fallback, fontSize = (size.value * 0.6f).sp) }
    }
}

/** Liczba z ikoną – zasób, statystyka, koszt. */
@Composable
fun IconValue(key: String, value: String, color: Color = Aaa.TextHi, size: Dp = 18.dp, fallback: String? = null) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        GameIcon(key, size, fallback = fallback)
        Text(value, style = AaaType.Label.copy(fontSize = 12.sp, color = color, fontWeight = FontWeight.Bold))
    }
}

/** Pieczęć królestwa z rombami etapów (wypełnione = osiągnięte). */
@Composable
fun RealmSeal(realm: MajorRealm?, stage: Int, stages: Int, size: Dp = 44.dp, showName: Boolean = true) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        GameIcon(realmKey(realm), size)
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            if (showName) Text(realm?.polishName ?: "Śmiertelnik", style = AaaType.Label.copy(fontSize = 13.sp, color = Aaa.TextHi, fontWeight = FontWeight.Bold))
            StagePips(stage, stages)
        }
    }
}

@Composable
fun StagePips(filled: Int, total: Int, color: Color = Aaa.Gold, pip: Dp = 6.dp) {
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
        repeat(total.coerceIn(1, 12)) { i ->
            androidx.compose.foundation.Canvas(Modifier.size(pip)) {
                val p = androidx.compose.ui.graphics.Path().apply {
                    moveTo(size.width / 2, 0f); lineTo(size.width, size.height / 2); lineTo(size.width / 2, size.height); lineTo(0f, size.height / 2); close()
                }
                if (i < filled) drawPath(p, color) else drawPath(p, color.copy(alpha = 0.8f), style = Stroke(1.dp.toPx()))
            }
        }
    }
}

/** Jeden kawałek pierścienia szans: ile punktów procentowych i jakim kolorem. */
data class OddsPart(val label: String, val points: Int, val color: Color, val icon: String? = null)

/** Pierścień szansy: kolorowe łuki = źródła premii, w środku łączna szansa. */
@Composable
fun OddsRing(total: Int, parts: List<OddsPart>, size: Dp = 132.dp, caption: String = "szansa") {
    Box(Modifier.size(size), contentAlignment = Alignment.Center) {
        androidx.compose.foundation.Canvas(Modifier.size(size)) {
            val w = this.size.width * 0.085f
            val inset = w / 2
            val arcSize = androidx.compose.ui.geometry.Size(this.size.width - w, this.size.height - w)
            val tl = androidx.compose.ui.geometry.Offset(inset, inset)
            drawArc(Aaa.Ink2, 0f, 360f, false, tl, arcSize, style = Stroke(w))
            var start = -90f
            parts.filter { it.points > 0 }.forEach { p ->
                val sweep = 3.6f * p.points.coerceAtMost(100)
                drawArc(p.color, start, (sweep - 1.5f).coerceAtLeast(0.5f), false, tl, arcSize, style = Stroke(w, cap = StrokeCap.Butt))
                start += sweep
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("$total%", fontFamily = BrushFont, fontSize = (size.value * 0.24f).sp, color = Aaa.GoldHi)
            Text(caption, style = AaaType.Label.copy(fontSize = 9.sp, color = Aaa.TextMid))
        }
    }
}

/** Legenda pierścienia: pasek koloru, ikona, nazwa, wartość. */
@Composable
fun OddsLegend(parts: List<OddsPart>) {
    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        parts.forEach { p ->
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                Box(Modifier.width(3.dp).height(16.dp).background(p.color))
                if (p.icon != null) GameIcon(p.icon, 16.dp)
                Text(p.label, style = AaaType.Label.copy(fontSize = 11.sp, color = Aaa.TextHi), modifier = Modifier.weight(1f), maxLines = 1)
                Text((if (p.points >= 0) "+" else "") + "${p.points}%", style = AaaType.Label.copy(fontSize = 11.sp, color = if (p.points >= 0) p.color else Aaa.Cinnabar, fontWeight = FontWeight.Bold))
            }
        }
    }
}

/**
 * Kadr ze sceną i postacią (okazja, aukcja, zarządca, cel tygodnia): tło przyciemnione,
 * postać po prawej, tytuł pędzlem po lewej na dole.
 */
@Composable
fun NpcScene(bgKey: String, npcKey: String?, overline: String, title: String, modifier: Modifier = Modifier, height: Dp = 200.dp,
             badge: (@Composable () -> Unit)? = null, overlineColor: Color = Aaa.Violet) {
    Box(modifier.fillMaxWidth().height(height).clip(cutCornerTicket())) {
        ArtImage(bgKey, Modifier.matchParentSize(), colorFilter = dimFilter(0.5f, 0.8f))
        if (npcKey != null) {
            ArtImage(npcKey, Modifier.align(Alignment.BottomEnd).fillMaxHeight().fillMaxWidth(0.46f), alignment = Alignment.TopCenter)
            Box(Modifier.align(Alignment.BottomEnd).fillMaxHeight().fillMaxWidth(0.46f)
                .background(Brush.horizontalGradient(listOf(Aaa.Ink0.copy(alpha = 0.85f), Color.Transparent, Color.Transparent))))
        }
        Box(Modifier.matchParentSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color.Transparent, Aaa.Ink0.copy(alpha = 0.92f)))))
        if (badge != null) Box(Modifier.align(Alignment.TopEnd).padding(10.dp)) { badge() }
        Column(Modifier.align(Alignment.BottomStart).padding(14.dp).fillMaxWidth(0.62f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Overline(overline, color = overlineColor)
            Text(title, fontFamily = BrushFont, color = Aaa.GoldHi, fontSize = 28.sp, lineHeight = 30.sp, maxLines = 2)
        }
    }
}

/** Filtr szarości dla budynków, na które nie stać sekty. */
val GrayFilter: ColorFilter get() = dimFilter(0.5f, 0f)

/** Rząd statystyk z ikonami: ikona, liczba, krótki podpis. Dotknięcie → [onTap] z kluczem statystyki. */
@Composable
fun IconStatRow(items: List<Triple<String, String, String>>, modifier: Modifier = Modifier, onTap: ((String) -> Unit)? = null) {
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        items.forEach { (key, value, label) ->
            Column(
                Modifier.weight(1f)
                    .background(Aaa.Ink2)
                    .then(if (onTap != null) Modifier.clickable { onTap(key) } else Modifier)
                    .padding(vertical = 5.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(1.dp)
            ) {
                GameIcon(key, 20.dp)
                Text(value, style = AaaType.Numeric.copy(fontSize = 13.sp), maxLines = 1)
                Text(label, style = AaaType.Label.copy(fontSize = 8.sp, color = Aaa.TextLow), maxLines = 1)
            }
        }
    }
}

/** Zakładka z ikoną (panel ucznia): ikona nad podpisem, opcjonalna plakietka liczby. */
@Composable
fun IconTab(key: String, label: String, modifier: Modifier = Modifier, badge: String? = null, onClick: () -> Unit) {
    Column(
        modifier.clip(cutCornerTicket()).background(Aaa.Ink2)
            .border(1.dp, Aaa.Hairline, cutCornerTicket())
            .clickable(role = androidx.compose.ui.semantics.Role.Button, onClick = onClick)
            .padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Box {
            GameIcon(key, 24.dp)
            if (badge != null) Text(badge, style = AaaType.Label.copy(fontSize = 8.sp, color = Aaa.Ink0, fontWeight = FontWeight.Bold),
                modifier = Modifier.align(Alignment.TopEnd).offset(x = 8.dp, y = (-4).dp).background(Aaa.Gold).padding(horizontal = 3.dp))
        }
        Text(label, style = AaaType.Label.copy(fontSize = 10.sp, color = Aaa.TextHi), maxLines = 1)
    }
}

