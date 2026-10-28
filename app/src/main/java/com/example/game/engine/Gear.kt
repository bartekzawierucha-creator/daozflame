package com.example.game.engine

import com.example.model.Artifact
import com.example.model.Disciple
import com.example.model.EquipSlot

/**
 * Etap 11: ekwipunek ucznia – sześć miejsc wokół postaci: broń ([Disciple.equippedArtifact]), głowa, szata,
 * dłonie, pierścień i relikwia ([Disciple.treasure]). Premie artefaktów są wliczone w atak i obronę ucznia;
 * zdjęty przedmiot wraca do skarbca. Komplet: 3 przedmioty +[SET_HALF]%, wszystkie 6 +[SET_FULL]% premii przedmiotów.
 */
object Gear {
    const val SET_HALF_PIECES = 3
    const val SET_HALF = 5
    const val SET_FULL = 12

    private val WEAPON_WORDS = listOf("miecz", "sztylet", "włóczni", "łuk", "lask", "bicz", "wachlarz", "kieł", "młot", "ostrz", "szabl", "topór", "kosa", "halabard")
    private val HEAD_WORDS = listOf("hełm", "diadem", "koron", "kaptur", "opask", "spink")
    private val HANDS_WORDS = listOf("karwasz", "rękawic", "naramienn", "bransolet")
    private val ARMOR_WORDS = listOf("pancerz", "zbroj", "szat", "płaszcz", "tarcz", "kamizel", "pas ")
    private val RING_WORDS = listOf("pierścień", "sygnet")
    private val RELIC_WORDS = listOf("dzwon", "zwierciadł", "kocioł", "wisior", "gwiazdozbiór", "lustr", "amulet", "perł", "talizman", "pieczęć", "tykw")

    fun slotOf(a: Artifact): EquipSlot {
        val n = a.name.lowercase()
        return when {
            RING_WORDS.any { it in n } -> EquipSlot.RING
            HEAD_WORDS.any { it in n } -> EquipSlot.HEAD
            HANDS_WORDS.any { it in n } -> EquipSlot.HANDS
            WEAPON_WORDS.any { it in n } -> EquipSlot.WEAPON
            ARMOR_WORDS.any { it in n } -> EquipSlot.ARMOR
            RELIC_WORDS.any { it in n } -> EquipSlot.RELIC
            a.attackBonus >= a.defenseBonus -> EquipSlot.WEAPON
            else -> EquipSlot.ARMOR
        }
    }

    fun inSlot(d: Disciple, slot: EquipSlot): Artifact? = when (slot) {
        EquipSlot.WEAPON -> d.equippedArtifact
        EquipSlot.HEAD -> d.head
        EquipSlot.ARMOR -> d.armor
        EquipSlot.HANDS -> d.hands
        EquipSlot.RING -> d.ring
        EquipSlot.RELIC -> d.treasure
    }

    fun items(d: Disciple): List<Artifact> = EquipSlot.entries.mapNotNull { inSlot(d, it) }

    fun fullSet(d: Disciple) = items(d).size == EquipSlot.entries.size

    /** Premia kompletu w % dla danej liczby przedmiotów. */
    fun setPercent(pieces: Int): Int = when {
        pieces >= EquipSlot.entries.size -> SET_FULL
        pieces >= SET_HALF_PIECES -> SET_HALF
        else -> 0
    }

    /** Łączna premia przedmiotów (z kompletem) – to, co siedzi w ataku i obronie ucznia. */
    fun bonus(d: Disciple): Pair<Int, Int> {
        val it = items(d)
        val pct = 100 + setPercent(it.size)
        return it.sumOf { a -> a.attackBonus } * pct / 100 to it.sumOf { a -> a.defenseBonus } * pct / 100
    }

    private fun place(d: Disciple, slot: EquipSlot, a: Artifact?): Disciple = when (slot) {
        EquipSlot.WEAPON -> d.copy(equippedArtifact = a)
        EquipSlot.HEAD -> d.copy(head = a)
        EquipSlot.ARMOR -> d.copy(armor = a)
        EquipSlot.HANDS -> d.copy(hands = a)
        EquipSlot.RING -> d.copy(ring = a)
        EquipSlot.RELIC -> d.copy(treasure = a)
    }

    /** Zmiana ekwipunku z przeliczeniem premii (stara premia odjęta, nowa dodana). */
    private fun reequip(d: Disciple, slot: EquipSlot, a: Artifact?): Disciple {
        val (oa, od) = bonus(d)
        val x = place(d, slot, a)
        val (na, nd) = bonus(x)
        return x.copy(attack = (d.attack - oa + na).coerceAtLeast(1), defense = (d.defense - od + nd).coerceAtLeast(0))
    }

    /** Podgląd: (atak, obrona) ucznia po założeniu [a] – do porównania w interfejsie. */
    fun preview(d: Disciple, a: Artifact): Pair<Int, Int> = reequip(d, slotOf(a), a).let { it.attack to it.defense }

    /** Załóż artefakt ze skarbca; poprzedni z tego miejsca wraca do skarbca. */
    fun equip(s: GameState, discipleId: String, vaultIndex: Int): StepResult {
        val d = s.disciple(discipleId) ?: return ok(s)
        val a = s.vaultArtifacts.getOrNull(vaultIndex) ?: return ok(s)
        val slot = slotOf(a)
        val prev = inSlot(d, slot)
        val vault = s.vaultArtifacts.filterIndexed { i, _ -> i != vaultIndex } + listOfNotNull(prev)
        val after = reequip(d, slot, a)
        val before = setPercent(items(d).size)
        val now = setPercent(items(after).size)
        return ok(
            s.copy(vaultArtifacts = vault, disciples = s.disciples.map { if (it.id == d.id) after else it }),
            "${Autonomy.short(d)}: ${slot.polishName.lowercase()} – ${a.name}." + (prev?.let { " ${it.name} wraca do skarbca." } ?: "") +
                if (now > before) " Komplet ${items(after).size}/${EquipSlot.entries.size}: +$now% premii przedmiotów!" else ""
        )
    }

    fun unequip(s: GameState, discipleId: String, slot: EquipSlot): StepResult {
        val d = s.disciple(discipleId) ?: return ok(s)
        val a = inSlot(d, slot) ?: return ok(s)
        val after = reequip(d, slot, null)
        return ok(s.copy(vaultArtifacts = s.vaultArtifacts + a, disciples = s.disciples.map { if (it.id == d.id) after else it }),
            "${a.name} wraca do skarbca.")
    }

    /** Stara komenda (artefakt po wartości): szuka go w skarbcu; gdy go tam nie ma – zakłada jak dawniej. */
    fun equipLegacy(s: GameState, discipleId: String, artifact: Artifact): StepResult {
        val i = s.vaultArtifacts.indexOf(artifact)
        if (i >= 0) return equip(s, discipleId, i)
        val d = s.disciple(discipleId) ?: return ok(s)
        val after = reequip(d, slotOf(artifact), artifact)
        return ok(s.copy(disciples = s.disciples.map { if (it.id == d.id) after else it }), "Wyposażono artefakt: ${artifact.name}!")
    }

    /** Migracja: przedmioty założone na niewłaściwe miejsce (stare zapisy) trafiają na swoje, jeśli jest wolne. */
    fun normalize(d: Disciple): Disciple {
        var x = d
        for (slot in listOf(EquipSlot.WEAPON, EquipSlot.ARMOR, EquipSlot.RELIC)) {
            val a = inSlot(x, slot) ?: continue
            val right = slotOf(a)
            if (right != slot && inSlot(x, right) == null) x = place(place(x, slot, null), right, a)
        }
        return x
    }

    fun misplaced(d: Disciple): Boolean = EquipSlot.entries.any { s -> inSlot(d, s)?.let { slotOf(it) != s } == true }
}
