package com.example.game.engine

import com.example.model.Artifact
import com.example.model.Disciple
import com.example.model.EquipSlot

/**
 * Etap 11: ekwipunek ucznia – trzy miejsca: broń ([Disciple.equippedArtifact]), szata i skarb.
 * Premie artefaktu są wliczone w atak i obronę ucznia (jak dotąd); zdjęty przedmiot wraca do skarbca.
 * Pełny komplet tej samej lub wyższej jakości daje premię zestawu.
 */
object Gear {
    /** Premia zestawu: +10% ataku i obrony z przedmiotów, gdy wszystkie trzy miejsca są zajęte. */
    const val SET_BONUS_PERCENT = 10

    private val WEAPON_WORDS = listOf("miecz", "sztylet", "włóczni", "łuk", "lask", "bicz", "wachlarz", "rękawic", "kieł", "młot", "ostrz", "szabl", "topór", "kosa")
    private val ARMOR_WORDS = listOf("pancerz", "zbroj", "szat", "płaszcz", "karwasz", "tarcz", "hełm", "kaptur", "pas ")
    private val TREASURE_WORDS = listOf("pierścień", "dzwon", "zwierciadł", "kocioł", "koron", "wisior", "diadem", "gwiazdozbiór", "lustr", "amulet", "perł", "talizman")

    fun slotOf(a: Artifact): EquipSlot {
        val n = a.name.lowercase()
        return when {
            WEAPON_WORDS.any { it in n } -> EquipSlot.WEAPON
            ARMOR_WORDS.any { it in n } -> EquipSlot.ARMOR
            TREASURE_WORDS.any { it in n } -> EquipSlot.TREASURE
            a.attackBonus >= a.defenseBonus -> EquipSlot.WEAPON
            else -> EquipSlot.ARMOR
        }
    }

    fun inSlot(d: Disciple, slot: EquipSlot): Artifact? = when (slot) {
        EquipSlot.WEAPON -> d.equippedArtifact
        EquipSlot.ARMOR -> d.armor
        EquipSlot.TREASURE -> d.treasure
    }

    fun items(d: Disciple): List<Artifact> = listOfNotNull(d.equippedArtifact, d.armor, d.treasure)

    fun fullSet(d: Disciple) = d.equippedArtifact != null && d.armor != null && d.treasure != null

    /** Łączna premia przedmiotów (z zestawem) – to, co siedzi w ataku i obronie ucznia. */
    fun bonus(d: Disciple): Pair<Int, Int> {
        val it = items(d)
        val atk = it.sumOf { a -> a.attackBonus }
        val def = it.sumOf { a -> a.defenseBonus }
        return if (fullSet(d)) atk * (100 + SET_BONUS_PERCENT) / 100 to def * (100 + SET_BONUS_PERCENT) / 100 else atk to def
    }

    private fun place(d: Disciple, slot: EquipSlot, a: Artifact?): Disciple = when (slot) {
        EquipSlot.WEAPON -> d.copy(equippedArtifact = a)
        EquipSlot.ARMOR -> d.copy(armor = a)
        EquipSlot.TREASURE -> d.copy(treasure = a)
    }

    /** Zmiana ekwipunku z przeliczeniem premii (stara premia odjęta, nowa dodana). */
    private fun reequip(d: Disciple, slot: EquipSlot, a: Artifact?): Disciple {
        val (oa, od) = bonus(d)
        val x = place(d, slot, a)
        val (na, nd) = bonus(x)
        return x.copy(attack = (d.attack - oa + na).coerceAtLeast(1), defense = (d.defense - od + nd).coerceAtLeast(0))
    }

    /** Załóż artefakt ze skarbca; poprzedni z tego miejsca wraca do skarbca. */
    fun equip(s: GameState, discipleId: String, vaultIndex: Int): StepResult {
        val d = s.disciple(discipleId) ?: return ok(s)
        val a = s.vaultArtifacts.getOrNull(vaultIndex) ?: return ok(s)
        val slot = slotOf(a)
        val prev = inSlot(d, slot)
        val vault = s.vaultArtifacts.filterIndexed { i, _ -> i != vaultIndex } + listOfNotNull(prev)
        val after = reequip(d, slot, a)
        val set = !fullSet(d) && fullSet(after)
        return ok(
            s.copy(vaultArtifacts = vault, disciples = s.disciples.map { if (it.id == d.id) after else it }),
            "${Autonomy.short(d)}: ${slot.polishName.lowercase()} – ${a.name}." + (prev?.let { " ${it.name} wraca do skarbca." } ?: "") +
                if (set) " Pełny komplet: +$SET_BONUS_PERCENT% premii przedmiotów!" else ""
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

    /** Migracja: broń założona przed Etapem 11, która jest szatą lub skarbem, trafia na właściwe miejsce. */
    fun normalize(d: Disciple): Disciple {
        val a = d.equippedArtifact ?: return d
        return when (slotOf(a)) {
            EquipSlot.WEAPON -> d
            EquipSlot.ARMOR -> if (d.armor == null) d.copy(equippedArtifact = null, armor = a) else d
            EquipSlot.TREASURE -> if (d.treasure == null) d.copy(equippedArtifact = null, treasure = a) else d
        }
    }
}
