package com.example.game.engine

import com.example.model.Disciple

/**
 * Etap 12: jedna miara siły. Obrażenia z premii (rdzeń, dusza, ścieżka, Prawa, krew, patron, edykt, Drzewo Niebios)
 * działają w każdej walce – nie tylko w ręcznym starciu – a Moc ucznia liczy się zawsze z bieżących statystyk.
 */
object Power {
    /** Mnożnik obrażeń ucznia ze wszystkich premii. */
    fun damageFactor(s: GameState, d: Disciple): Double =
        Modifiers.disciple(Modifiers.sect(s), d).factor(Stat.COMBAT_DAMAGE).coerceAtLeast(0.1)

    /** Atak w walce: atak ucznia × premie obrażeń. */
    fun attack(s: GameState, d: Disciple): Int = (d.attack * damageFactor(s, d)).toInt().coerceAtLeast(1)

    fun combatPower(s: GameState, d: Disciple): Int =
        CultivationRules.combatPower(d.maxHp, attack(s, d), d.defense, d.comprehension, d.karmicLuck)

    /** Odświeża zapisaną Moc wszystkich uczniów (po każdym kroku silnika). */
    fun refresh(s: GameState): GameState {
        if (s.disciples.isEmpty()) return s
        val sect = Modifiers.sect(s)
        var changed = false
        val ds = s.disciples.map { d ->
            val f = Modifiers.disciple(sect, d).factor(Stat.COMBAT_DAMAGE).coerceAtLeast(0.1)
            val cp = CultivationRules.combatPower(d.maxHp, (d.attack * f).toInt().coerceAtLeast(1), d.defense, d.comprehension, d.karmicLuck)
            if (cp != d.combatPower) { changed = true; d.copy(combatPower = cp) } else d
        }
        return if (changed) s.copy(disciples = ds) else s
    }
}
