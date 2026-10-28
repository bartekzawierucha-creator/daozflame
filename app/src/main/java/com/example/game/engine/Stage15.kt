package com.example.game.engine

import com.example.model.FacilityType
import com.example.model.MethodGrade

/** Etap 15: ujścia dla zbędnych zwojów i fragmentów. */
object Scrolls {
    /** Punkty Dao za przestudiowanie zwoju w Wieży Technik. */
    fun daoValue(g: MethodGrade): Int = when (g) {
        MethodGrade.YELLOW -> 3
        MethodGrade.BLACK -> 8
        MethodGrade.MYSTIC -> 20
        MethodGrade.HEAVENLY -> 40
        MethodGrade.PRIMORDIAL -> 80
    }

    /** Zbędny zwój → punkty Dao dla ucznia (wymaga Wieży Technik). */
    fun study(s: GameState, discipleId: String, scroll: String): StepResult {
        val d = s.disciple(discipleId) ?: return ok(s)
        val m = Realms.methodForScroll(scroll) ?: return ok(s)
        if (!Buildings.has(s, FacilityType.MARTIAL_ARCHIVES)) return fail(s, "Studiowanie zwojów wymaga Wieży Technik.")
        val have = s.inventory[scroll] ?: 0
        if (have < 1) return fail(s, "Brak zwoju w magazynie.")
        val pts = daoValue(m.grade)
        return ok(
            s.copy(inventory = s.inventory + (scroll to have - 1), disciples = s.disciples.map { if (it.id == d.id) it.copy(daoPoints = it.daoPoints + pts) else it }),
            "📚 ${Autonomy.short(d)} studiuje „${m.name}” w Wieży Technik: +$pts pkt Dao."
        )
    }

    /** Pięć fragmentów tej samej metody składa się w pełny zwój (gdy szyfr już niepotrzebny albo metoda obca). */
    fun assemble(s: GameState, methodId: String): StepResult {
        val item = Breath.fragmentItem(methodId)
        val have = s.inventory[item] ?: 0
        if (have < Breath.FRAGMENTS) return fail(s, "Potrzeba ${Breath.FRAGMENTS} fragmentów (masz $have).")
        val m = Realms.method(methodId)
        return ok(s.copy(inventory = s.inventory + (item to have - Breath.FRAGMENTS) + (m.scrollItem to (s.inventory[m.scrollItem] ?: 0) + 1)),
            "📜 Z fragmentów złożono zwój: ${m.name}.")
    }
}
