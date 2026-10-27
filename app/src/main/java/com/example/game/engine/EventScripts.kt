package com.example.game.engine

import com.example.model.CondType
import com.example.model.Condition
import com.example.model.Effect
import com.example.model.EffectType
import com.example.model.RandomEvent
import com.example.model.RandomEventChoice
import com.example.model.ScheduledEvent
import com.example.model.ScriptChoice
import com.example.model.ScriptEvent
import com.example.model.Season
import com.example.model.TimeOfDay

/**
 * Etap 5, krok 1: silnik skryptów zdarzeń.
 * Ten sam mechanizm obsługuje zdarzenia losowe, łańcuchy, NPC, kontrakty fabularne i tropy płomieni.
 */
object EventScripts {

    const val SEEN_PREFIX = "seen:"
    const val SYSTEM_CHAIN = "system"

    /** Przeniesienie starego zdarzenia do nowego formatu (50 bazowych + katalog). */
    fun fromLegacy(e: RandomEvent): ScriptEvent = ScriptEvent(
        id = e.id,
        title = e.title,
        description = e.description,
        conditions = buildList {
            if (e.minDay > 0) add(Condition(CondType.MIN_DAY, value = e.minDay))
            if (e.minReputation > 0) add(Condition(CondType.MIN_REPUTATION, value = e.minReputation))
            e.requiredTimeOfDay?.let { add(Condition(CondType.TIME_OF_DAY, key = it.name)) }
        },
        choices = e.choices.map { c ->
            ScriptChoice(
                text = c.text,
                outcome = c.outcomeDescription,
                effects = buildList {
                    if (c.stoneChange != 0) add(Effect(EffectType.STONES, value = c.stoneChange))
                    if (c.repChange != 0) add(Effect(EffectType.REPUTATION, value = c.repChange))
                    c.rewardItem?.let { add(Effect(EffectType.ITEM, key = it, value = 2)) }
                }
            )
        }
    )

    /** Widok zdarzenia dla UI i bota: kształt starego [RandomEvent]. */
    fun view(e: ScriptEvent, s: GameState?): RandomEvent = RandomEvent(
        id = e.id,
        title = e.title,
        description = e.description,
        choices = e.choices.map { c ->
            val locked = s != null && !allMet(s, c.conditions)
            RandomEventChoice(
                text = if (locked) "${c.text} (niedostępne)" else c.text,
                outcomeDescription = c.outcome,
                stoneChange = if (locked) -999_999 else c.effects.filter { it.type == EffectType.STONES }.sumOf { it.value },
                repChange = c.effects.filter { it.type == EffectType.REPUTATION }.sumOf { it.value },
                rewardItem = c.effects.firstOrNull { it.type == EffectType.ITEM && it.value > 0 }?.key
            )
        }
    )

    // --- Warunki ---

    fun met(s: GameState, c: Condition): Boolean = when (c.type) {
        CondType.MIN_DAY -> s.currentDay >= c.value
        CondType.MAX_DAY -> s.currentDay <= c.value
        CondType.TIME_OF_DAY -> s.timeOfDay.name == c.key || (c.key == "NIGHT" && s.timeOfDay == TimeOfDay.FULL_MOON)
        CondType.SEASON -> Seasons.of(s.currentDay).name == c.key
        CondType.FLAG_AT_LEAST -> (s.flags[c.key] ?: 0) >= c.value
        CondType.FLAG_BELOW -> (s.flags[c.key] ?: 0) < c.value
        CondType.MIN_REPUTATION -> s.sectReputation >= c.value
        CondType.MIN_KARMA -> s.karma >= c.value
        CondType.MAX_KARMA -> s.karma <= c.value
        CondType.MIN_STONES -> s.spiritStones >= c.value
        CondType.HAS_ITEM -> (s.inventory[c.key] ?: 0) >= c.value.coerceAtLeast(1)
        CondType.REGION_EXPLORED -> (s.regions.find { it.id == c.key }?.explorationProgress ?: 0) >= c.value.coerceAtLeast(1)
    }

    fun allMet(s: GameState, cs: List<Condition>): Boolean = cs.all { met(s, it) }

    fun eligible(s: GameState, e: ScriptEvent): Boolean =
        allMet(s, e.conditions) && !(e.once && (s.flags[SEEN_PREFIX + e.id] ?: 0) > 0)

    // --- Efekty ---

    fun applyEffects(s: GameState, effects: List<Effect>): GameState {
        var st = s
        for (e in effects) st = when (e.type) {
            EffectType.STONES -> st.copy(spiritStones = (st.spiritStones + e.value).coerceAtLeast(0))
            EffectType.REPUTATION -> st.copy(
                sectReputation = st.sectReputation +
                    if (e.value > 0) (e.value * Modifiers.sect(st).factor(Stat.REPUTATION_GAIN)).toInt() else e.value
            )
            EffectType.KARMA -> st.copy(karma = st.karma + e.value)
            EffectType.MORALE -> st.copy(morale = (st.morale + e.value).coerceIn(0, 100))
            EffectType.ITEM -> st.copy(inventory = st.inventory + (e.key to ((st.inventory[e.key] ?: 0) + e.value).coerceAtLeast(0)))
            EffectType.SET_FLAG -> st.copy(flags = st.flags + (e.key to e.value))
            EffectType.ADD_FLAG -> st.copy(flags = st.flags + (e.key to (st.flags[e.key] ?: 0) + e.value))
            EffectType.SCHEDULE -> st.copy(scheduledEvents = st.scheduledEvents + ScheduledEvent(st.currentDay + e.value.coerceAtLeast(0), e.key))
            EffectType.RIVAL_RELATION -> st.copy(rivalSects = st.rivalSects.map {
                if (it.id == e.key) it.copy(relations = (it.relations + e.value).coerceIn(-100, 100)) else it
            })
            EffectType.STONES_PERCENT -> {
                val delta = (st.spiritStones * e.value / 100).coerceIn(-5_000, 5_000)
                st.copy(spiritStones = (st.spiritStones + delta).coerceAtLeast(0))
            }
            EffectType.INJURE_CHAMPION -> st.disciples.maxByOrNull { it.combatPower }?.let { champ ->
                st.copy(disciples = st.disciples.map {
                    if (it.id == champ.id) it.copy(currentHp = (it.currentHp - it.maxHp * e.value / 100).coerceAtLeast(10)) else it
                })
            } ?: st
        }
        return st
    }

    /** Aktywacja zdarzenia: oznacza „widziane” dla jednorazowych i zapamiętuje w ostatnich. */
    fun activate(s: GameState, e: ScriptEvent): GameState = s.copy(
        activeEventId = e.id,
        // Pamięć powtórek dotyczy losowania – zdarzenia planowane jej nie zajmują.
        recentEventIds = if (e.random) (s.recentEventIds + e.id).takeLast(RandomEventPhase.MEMORY) else s.recentEventIds,
        flags = if (e.once) s.flags + (SEEN_PREFIX + e.id to 1) else s.flags
    )

    fun choose(s: GameState, index: Int): StepResult {
        val event = GameContent.script(s.activeEventId) ?: return ok(s.copy(activeEventId = null))
        val choice = event.choices.getOrNull(index) ?: return ok(s)
        if (!allMet(s, choice.conditions)) return fail(s, "Ta odpowiedź jest teraz niedostępna.")
        return ok(applyEffects(s.copy(activeEventId = null), choice.effects), choice.outcome)
    }

    /** Zaplanowane zdarzenia mają pierwszeństwo przed losowymi. Zwraca nowy stan albo null, gdy nic nie wypadło. */
    fun dueScheduled(s: GameState): GameState? {
        if (s.activeEventId != null) return null
        val due = s.scheduledEvents.filter { it.day <= s.currentDay }
        if (due.isEmpty()) return null
        val first = due.minBy { it.day }
        val rest = s.scheduledEvents - first
        val e = GameContent.script(first.eventId)
        // Warunki niespełnione w dniu terminu: ogniwo przepada (łańcuch się urywa).
        if (e == null || !eligible(s, e)) return s.copy(scheduledEvents = rest)
        return activate(s.copy(scheduledEvents = rest), e)
    }

    // --- Walidator treści ---

    /** Zwraca listę błędów: brakujące cele SCHEDULE, łańcuchy bez wejścia, pętle bez wyjścia. */
    fun validate(events: List<ScriptEvent>): List<String> = buildList {
        val byId = events.associateBy { it.id }
        if (byId.size != events.size) add("Zduplikowane id zdarzeń: " + events.groupBy { it.id }.filter { it.value.size > 1 }.keys)
        val edges = events.associate { e ->
            e.id to e.choices.flatMap { c -> c.effects.filter { it.type == EffectType.SCHEDULE }.map { it.key } }.toSet()
        }
        edges.forEach { (from, tos) -> tos.filter { it !in byId }.forEach { add("$from planuje nieistniejące zdarzenie $it") } }
        val targets = edges.values.flatten().toSet()
        // Zdarzenia łańcucha „system” planuje kod (Imperium, katastrofy) – nie potrzebują poprzednika.
        events.filter { !it.random && it.id !in targets && it.chain != SYSTEM_CHAIN }.forEach { add("${it.id} nie wypada losowo i nikt go nie planuje") }
        events.forEach { e -> if (e.choices.isEmpty()) add("${e.id} nie ma żadnego wyboru") }
        // Pętla bez wyjścia: z węzła nie da się dojść do żadnego wyboru, który nie planuje dalej.
        val canEnd = mutableSetOf<String>()
        var changed = true
        events.filter { e -> e.choices.any { c -> c.effects.none { it.type == EffectType.SCHEDULE } } }.forEach { canEnd += it.id }
        while (changed) {
            changed = false
            for ((id, tos) in edges) if (id !in canEnd && tos.any { it in canEnd }) { canEnd += id; changed = true }
        }
        events.filter { it.id !in canEnd }.forEach { add("${it.id} należy do pętli bez wyjścia") }
    }
}
