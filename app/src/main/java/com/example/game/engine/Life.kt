package com.example.game.engine

import com.example.game.content.StoryCatalog
import com.example.model.AncestorRecord
import com.example.model.Artifact
import com.example.model.Companionship
import com.example.model.CultivationPath
import com.example.model.CultivationRealm
import com.example.model.Disciple
import com.example.model.MajorRealm
import com.example.model.ScheduledEvent

/** Etap 7: reżyser głównego wątku i Wybraniec Losu. */
object Story {
    const val START_DAY = 10

    fun act(s: GameState): Int = s.flags[StoryCatalog.ACT] ?: 0
    fun ending(s: GameState): Int = s.flags[StoryCatalog.ENDING] ?: 0

    private fun schedule(s: GameState, id: String): GameState =
        if (s.scheduledEvents.any { it.eventId == id } || s.activeEventId == id) s
        else s.copy(scheduledEvents = s.scheduledEvents + ScheduledEvent(s.currentDay, id))

    /** Najlepszy kandydat na Wybrańca: najwyższy talent, potem moc. */
    fun pickChosen(s: GameState): Disciple? = s.disciples.maxWithOrNull(compareBy<Disciple>({ it.talentGrade.ordinal }, { it.combatPower }))

    fun daily(s0: GameState): Pair<GameState, List<String>> {
        if (!s0.e7Initialized) return s0 to emptyList()
        var s = s0
        val notes = mutableListOf<String>()
        // Wybraniec: zawsze jest następca – fabuła nie może utknąć po śmierci ucznia.
        if (s.disciple(s.chosenId) == null && s.disciples.isNotEmpty() && s.currentDay >= START_DAY) {
            val had = s.chosenId != null
            val next = pickChosen(s)
            s = schedule(s.copy(chosenId = next?.id), if (had) "story_new_chosen" else "story_chosen")
            next?.let { notes += "✋ Znamię Losu pojawia się na dłoni: ${it.name}." }
        }
        if (act(s) == 0 && s.currentDay >= START_DAY) {
            s = schedule(s.copy(flags = s.flags + (StoryCatalog.ACT to 1)), "story_1_1")
        }
        // Akt ustawiają zdarzenia otwierające akty (flaga story_act); kronika odnotowuje zmianę.
        val noted = s.flags["story_act_noted"] ?: 0
        if (act(s) > noted) {
            s = WorldAI.note(s.copy(flags = s.flags + ("story_act_noted" to act(s))),
                if (act(s) >= 5) "Opowieść o Demonie pod Górą dobiegła końca." else "Rozpoczyna się akt ${act(s)} opowieści o Demonie pod Górą.")
        }
        return s to notes
    }

    /** Premie: Wybraniec (+10% Qi) i trwały skutek zakończenia. */
    fun modifiers(s: GameState, d: Disciple?): List<Modifier> = buildList {
        if (d != null && d.id == s.chosenId) add(Modifier("Wybraniec Losu", Stat.QI_GAIN, ModOp.PERCENT, 10.0))
        if (d == null) when (ending(s)) {
            1 -> add(Modifier("Pieczęć Światła", Stat.REPUTATION_GAIN, ModOp.PERCENT, 25.0))
            2 -> add(Modifier("Pakt z Demonem", Stat.COMBAT_DAMAGE, ModOp.PERCENT, 25.0))
            3 -> add(Modifier("Odnowiona Pieczęć", Stat.QI_GAIN, ModOp.PERCENT, 10.0))
        }
    }
}

/** Etap 7: wiek, śmierć, Sala Przodków, mentorzy, towarzysze Dao i dzieci. */
object Lifecycle {
    const val YEAR_DAYS = 120
    const val ROOT_INHERITANCE = 60
    const val MAX_STUDENTS = 2
    const val ANCESTOR_CAP = 15

    fun lifespan(d: Disciple): Int {
        val base = d.majorRealm?.lifespanYears ?: (60 + d.realm.ordinal * 5)
        return if (d.path == CultivationPath.BODY) (base * 0.8).toInt() else base
    }

    /** Migracja: obecni uczniowie dostają młody wiek, żeby stary zapis nikogo nie zabił przy pierwszym wczytaniu. */
    fun normalize(s: GameState): GameState {
        if (s.e7Initialized) return s
        return s.copy(
            e7Initialized = true,
            disciples = s.disciples.mapIndexed { i, d ->
                if (d.ageYears in 1 until lifespan(d) - 10) d else d.copy(ageYears = 16 + (i * 3) % 10)
            }
        )
    }

    fun daily(s0: GameState, ctx: EngineContext): Pair<GameState, List<String>> {
        if (!s0.e7Initialized) return s0 to emptyList()
        var s = s0
        val notes = mutableListOf<String>()
        if (s.currentDay % YEAR_DAYS == 0) {
            s = s.copy(disciples = s.disciples.map { it.copy(ageYears = it.ageYears + 1) })
            s.disciples.filter { lifespan(it) - it.ageYears == 3 }.forEach { notes += "⏳ ${it.name} czuje, że jego czas się kończy (zostały 3 lata). Przełom przedłuży życie." }
            s.disciples.filter { it.ageYears >= lifespan(it) }.forEach { d ->
                s = die(s, d, "starość")
                notes += "🕯 ${d.name} odchodzi ze starości. Jego tablica staje w Sali Przodków."
            }
        }
        // Dzieci towarzyszy Dao: co rok jedno dziecko-kandydat.
        s.companions.forEach { c ->
            val years = (s.currentDay - c.sinceDay) / YEAR_DAYS
            if (years > c.children && s.disciple(c.a) != null && s.disciple(c.b) != null) {
                val (st, msg) = child(s, c, ctx)
                s = st
                notes += msg
            }
        }
        return s to notes
    }

    /** Śmierć ucznia: wpis w Sali Przodków i sprzątanie odwołań. */
    fun die(s: GameState, d: Disciple, cause: String): GameState {
        val record = AncestorRecord(d.name, d.majorRealm?.polishName ?: d.realm.polishName, d.majorRealm?.ordinal ?: 0, s.currentDay, cause)
        return WorldAI.note(
            s.copy(
                disciples = s.disciples.filter { it.id != d.id }.map { if (it.mentorId == d.id) it.copy(mentorId = null) else it },
                ancestors = (s.ancestors + record).takeLast(100),
                gates = s.gates.filterValues { it != d.id },
                researcherId = s.researcherId.takeIf { it != d.id },
                companions = s.companions.filter { it.a != d.id && it.b != d.id }
            ),
            "${d.name} umiera (${cause})."
        )
    }

    fun ancestorBonus(s: GameState): Double =
        s.ancestors.count { it.majorOrdinal >= MajorRealm.FOUNDATION.ordinal }.coerceAtMost(ANCESTOR_CAP).toDouble()

    fun assignMentor(s: GameState, studentId: String, mentorId: String?): StepResult {
        val student = s.disciple(studentId) ?: return ok(s)
        if (mentorId == null) return ok(s.copy(disciples = s.disciples.map { if (it.id == studentId) it.copy(mentorId = null) else it }), "${student.name} uczy się sam.")
        val mentor = s.disciple(mentorId) ?: return ok(s)
        if (mentor.id == student.id) return ok(s)
        if ((mentor.majorRealm?.ordinal ?: mentor.realm.ordinal) <= (student.majorRealm?.ordinal ?: student.realm.ordinal) &&
            !(mentor.majorRealm == student.majorRealm && mentor.stage > student.stage)) return fail(s, "Mistrz musi stać wyżej niż uczeń.")
        if (s.disciples.count { it.mentorId == mentor.id } >= MAX_STUDENTS) return fail(s, "${mentor.name} ma już $MAX_STUDENTS uczniów.")
        return ok(
            s.copy(disciples = s.disciples.map {
                when (it.id) {
                    student.id -> it.copy(mentorId = mentor.id)
                    mentor.id -> it.copy(loyalty = (it.loyalty + 5).coerceAtMost(100))
                    else -> it
                }
            }),
            "${mentor.name} przyjmuje ${student.name} na ucznia."
        )
    }

    fun bind(s: GameState, a: String, b: String): StepResult {
        val x = s.disciple(a) ?: return ok(s)
        val y = s.disciple(b) ?: return ok(s)
        if (x.id == y.id) return ok(s)
        if (s.companions.any { it.a in setOf(a, b) || it.b in setOf(a, b) }) return fail(s, "Któreś z nich ma już towarzysza Dao.")
        if (x.loyalty < 60 || y.loyalty < 60) return fail(s, "Związek Dao wymaga lojalności ≥ 60 u obojga.")
        return ok(s.copy(companions = s.companions + Companionship(a, b, s.currentDay)), "${x.name} i ${y.name} zostają towarzyszami Dao.")
    }

    private fun child(s: GameState, c: Companionship, ctx: EngineContext): Pair<GameState, String> {
        val rng = ctx.rng("child:${c.a}:${c.children}")
        val parents = listOf(s.disciple(c.a)!!, s.disciple(c.b)!!)
        val parent = parents.random(rng)
        var kid = com.example.game.WuxiaGenerator.generateDisciple(rng, CultivationRealm.QI_CONDENSATION_1, parent.bloodline)
        kid = GameEngine.prepareCandidate(kid, rng, s)
        if (rng.nextInt(100) < ROOT_INHERITANCE && parent.spiritRoot != null) kid = kid.copy(spiritRoot = parent.spiritRoot)
        kid = kid.copy(name = "${parent.name.substringBefore(" (").substringBefore(" ")} Młodszy (Dziecko Sekty)", ageYears = 12, ancientClan = parent.ancientClan)
        val st = s.copy(
            availableRecruits = s.availableRecruits + kid,
            companions = s.companions.map { if (it == c) it.copy(children = it.children + 1) else it }
        )
        return WorldAI.note(st, "Rodzi się dziecko sekty.") to "👶 Towarzysze Dao ${parents[0].name.substringBefore(" (")} i ${parents[1].name.substringBefore(" (")} mają dziecko – czeka wśród kandydatów."
    }

    fun modifiers(s: GameState, d: Disciple): List<Modifier> = buildList {
        val mentor = s.disciple(d.mentorId)
        if (mentor != null) add(Modifier("Mistrz: ${mentor.name.substringBefore(" (")}", Stat.QI_GAIN, ModOp.PERCENT, 15.0))
    }
}

/** Etap 7: tożsamość sekty – ścieżka karmy, patron i świątynia, skarby sekty. */
object Identity {
    const val KARMA_THRESHOLD = 50
    const val TREASURE_SLOTS = 5
    const val PATRON_COST = 1_000
    const val PATRON_COOLDOWN = 30
    const val RAID_TREASURE_LOSS = 30

    data class Patron(val id: String, val name: String, val description: String, val stat: Stat, val percent: Double)

    val PATRONS = listOf(
        Patron("p_jade", "Nefrytowy Cesarz", "Ład i sława: zysk reputacji.", Stat.REPUTATION_GAIN, 10.0),
        Patron("p_moon", "Bogini Księżyca", "Spokój nocy: przyrost Qi.", Stat.QI_GAIN, 8.0),
        Patron("p_war", "Pan Wojny", "Odwaga i stal: obrażenia w walce.", Stat.COMBAT_DAMAGE, 10.0),
        Patron("p_harvest", "Bogini Żniw", "Hojna ziemia: zbiory.", Stat.GATHER_YIELD, 15.0),
        Patron("p_dragon", "Smok Wschodu", "Bogactwo rzek: dochód kamieni.", Stat.STONE_INCOME, 10.0)
    )

    fun karmaPath(s: GameState): String = when {
        s.karma >= KARMA_THRESHOLD -> "Sekta Prawa"
        s.karma <= -KARMA_THRESHOLD -> "Sekta Demoniczna"
        else -> "Sekta Równowagi"
    }

    fun choosePatron(s: GameState, id: String): StepResult {
        val p = PATRONS.find { it.id == id } ?: return ok(s)
        if (s.patronId == id) return ok(s)
        if (s.patronId != null && s.currentDay - s.patronChangedDay < PATRON_COOLDOWN) return fail(s, "Zmiana patrona możliwa za ${PATRON_COOLDOWN - (s.currentDay - s.patronChangedDay)} dni.")
        if (s.patronId != null && s.spiritStones < PATRON_COST) return fail(s, "Nowe poświęcenie świątyni kosztuje $PATRON_COST kamieni.")
        val cost = if (s.patronId != null) PATRON_COST else 0
        return ok(s.copy(patronId = id, patronChangedDay = s.currentDay, templeLevel = s.templeLevel.coerceAtLeast(1), spiritStones = s.spiritStones - cost),
            "Świątynia sekty zostaje poświęcona: ${p.name}.")
    }

    fun templeCost(level: Int) = 2_000 * (level + 1)

    fun upgradeTemple(s: GameState): StepResult {
        if (s.patronId == null) return fail(s, "Najpierw wybierz patrona.")
        if (s.templeLevel >= 5) return fail(s, "Świątynia osiągnęła pełnię.")
        val cost = templeCost(s.templeLevel)
        if (s.spiritStones < cost) return fail(s, "Rozbudowa świątyni kosztuje $cost kamieni.")
        return ok(s.copy(spiritStones = s.spiritStones - cost, templeLevel = s.templeLevel + 1), "Świątynia rośnie – poziom ${s.templeLevel + 1}.")
    }

    fun treasureQi(a: Artifact): Double = when (a.rarity) {
        "Wieczne Dao" -> 8.0
        "Nieśmiertelny" -> 6.0
        "Boski" -> 4.0
        else -> 2.0
    }

    fun placeTreasure(s: GameState, vaultIndex: Int): StepResult {
        val a = s.vaultArtifacts.getOrNull(vaultIndex) ?: return ok(s)
        if (s.treasury.size >= TREASURE_SLOTS) return fail(s, "Skarbiec sekty ma tylko $TREASURE_SLOTS miejsc.")
        return ok(s.copy(vaultArtifacts = s.vaultArtifacts.filterIndexed { i, _ -> i != vaultIndex }, treasury = s.treasury + a),
            "${a.name} staje w skarbcu sekty jako jej skarb.")
    }

    fun removeTreasure(s: GameState, index: Int): StepResult {
        val a = s.treasury.getOrNull(index) ?: return ok(s)
        return ok(s.copy(treasury = s.treasury.filterIndexed { i, _ -> i != index }, vaultArtifacts = s.vaultArtifacts + a), "${a.name} wraca do magazynu.")
    }

    /** Przy przełamanych bramach rabusie mogą wynieść skarb. */
    fun raidLoss(s: GameState, ctx: EngineContext): Pair<GameState, String?> {
        if (s.treasury.isEmpty()) return s to null
        val rng = ctx.rng("treasure-loss")
        if (rng.nextInt(100) >= RAID_TREASURE_LOSS) return s to null
        val i = rng.nextInt(s.treasury.size)
        val lost = s.treasury[i]
        return s.copy(treasury = s.treasury.filterIndexed { j, _ -> j != i }) to "Rabusie wynieśli skarb sekty: ${lost.name}!"
    }

    fun modifiers(s: GameState): List<Modifier> = buildList {
        when {
            s.karma >= KARMA_THRESHOLD -> add(Modifier("Sekta Prawa", Stat.REPUTATION_GAIN, ModOp.PERCENT, 20.0))
            s.karma <= -KARMA_THRESHOLD -> {
                add(Modifier("Sekta Demoniczna", Stat.COMBAT_DAMAGE, ModOp.PERCENT, 15.0))
                add(Modifier("Sekta Demoniczna", Stat.REPUTATION_GAIN, ModOp.PERCENT, -10.0))
            }
        }
        PATRONS.find { it.id == s.patronId }?.let { p ->
            add(Modifier("Patron: ${p.name} (świątynia ${s.templeLevel})", p.stat, ModOp.PERCENT, p.percent * (1 + 0.25 * (s.templeLevel - 1).coerceAtLeast(0))))
        }
        val treasures = s.treasury.sumOf { treasureQi(it) }
        if (treasures > 0) add(Modifier("Skarby sekty (${s.treasury.size})", Stat.QI_GAIN, ModOp.PERCENT, treasures))
        val anc = Lifecycle.ancestorBonus(s)
        if (anc > 0) add(Modifier("Sala Przodków", Stat.QI_GAIN, ModOp.PERCENT, anc))
        addAll(Story.modifiers(s, null))
    }
}
