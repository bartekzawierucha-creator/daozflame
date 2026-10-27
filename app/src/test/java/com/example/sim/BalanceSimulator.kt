package com.example.sim

import com.example.game.engine.GameCommand
import com.example.game.engine.GameContent
import com.example.game.engine.GameEngine
import com.example.game.engine.GameState
import com.example.game.engine.Realms
import com.example.model.CultivationRealm
import com.example.model.FacilityType
import java.io.File

/**
 * Symulator balansu: gra bez UI z botem-graczem. Mierzy, po ilu godzinach realnej gry
 * (1 tick = 1,6 s przy prędkości ×1) sekta osiąga kolejne królestwa i jak rosną zasoby.
 *
 * Uruchamiany przez [BalanceSimulationTest] (CI) albo bezpośrednio z [main].
 */
object BalanceSimulator {

    const val SECONDS_PER_TICK = 1.6

    enum class Profile(val label: String, val actEveryTicks: Int) {
        /** Zagląda rzadko: raz na ~15 min gry przyjmuje zdarzenie i nic więcej nie robi. */
        PASSIVE("pasywny", 560),
        /** Gra uważnie co ~1 min: warzy, rekrutuje, wysyła wyprawy, rozbudowuje. */
        ACTIVE("aktywny", 37),
        /** Zawsze najlepsza decyzja w każdym ticku. */
        OPTIMAL("optymalny", 1)
    }

    data class Snapshot(
        val tick: Long,
        val hours: Double,
        val day: Int,
        val stones: Int,
        val reputation: Int,
        val disciples: Int,
        val topRealm: CultivationRealm,
        val regionsExplored: Int,
        val pills: Int,
        val morale: Int = 0
    )

    data class Run(
        val profile: Profile,
        val seed: Long,
        val snapshots: List<Snapshot>,
        /** Pierwsze osiągnięcie królestwa przez dowolnego ucznia: królestwo → godziny gry. */
        val realmReachedAtHours: Map<CultivationRealm, Double>,
        val bossesDefeated: Int,
        val defeats: Int
    )

    /** Cele tempa z planu wdrożenia (godziny gry gracza aktywnego). Do potwierdzenia przez autora. */
    val PACE_TARGETS_HOURS: Map<CultivationRealm, Double> = mapOf(
        CultivationRealm.CORE_FORMATION to 24.0,
        CultivationRealm.NASCENT_SOUL to 7 * 24.0,
        CultivationRealm.SOUL_TRANSFORMATION to 21 * 24.0,
        CultivationRealm.VOID_TRIBULATION to 42 * 24.0,
        CultivationRealm.MAHAYANA_GREAT_VEHICLE to 90 * 24.0
    )

    fun simulate(profile: Profile, seed: Long, hours: Double, snapshotEveryTicks: Int = 2250): Run {
        val totalTicks = (hours * 3600 / SECONDS_PER_TICK).toLong()
        var s = GameEngine.newGame(seed)
        val reached = linkedMapOf<CultivationRealm, Double>()
        s.disciples.forEach { d -> reached.putIfAbsent(d.realm, 0.0) }
        val snaps = mutableListOf<Snapshot>()
        var bosses = 0
        var defeats = 0

        for (t in 1..totalTicks) {
            s = GameEngine.apply(s, GameCommand.Tick).state
            if (t % profile.actEveryTicks == 0L) {
                val r = act(s, profile)
                s = r.first
                bosses += r.second
                defeats += r.third
            }
            val h = t * SECONDS_PER_TICK / 3600
            s.disciples.forEach { d -> reached.putIfAbsent(d.realm, h) }
            if (t % snapshotEveryTicks == 0L || t == totalTicks) snaps += snapshot(s, t)
        }
        return Run(profile, seed, snaps, reached, bosses, defeats)
    }

    private fun snapshot(s: GameState, t: Long) = Snapshot(
        tick = t,
        hours = t * SECONDS_PER_TICK / 3600,
        day = s.currentDay,
        stones = s.spiritStones,
        reputation = s.sectReputation,
        disciples = s.disciples.size,
        topRealm = s.disciples.maxOf { it.realm },
        regionsExplored = s.regions.count { it.explorationProgress > 0 },
        pills = s.inventory[GameContent.BREAKTHROUGH_PILL] ?: 0,
        morale = s.morale
    )

    private fun apply(s: GameState, c: GameCommand): GameState = GameEngine.apply(s, c).state

    /** Decyzje bota. Zwraca (stan, pokonani bossowie, porażki). */
    private fun act(start: GameState, profile: Profile): Triple<GameState, Int, Int> {
        var s = start
        var wins = 0
        var losses = 0

        // Zdarzenie: wybór najbardziej opłacalnej opcji.
        GameContent.event(s.activeEventId)?.let { ev ->
            val best = ev.choices.indices.maxBy { i -> ev.choices[i].stoneChange + ev.choices[i].repChange * 5 }
            s = apply(s, GameCommand.EventChoice(best))
        }
        // Jedzenie: każdy gracz dokupuje ryż, gdy zostaje na mniej niż 5 dni.
        val food = s.inventory[com.example.game.engine.DiscipleRules.FOOD] ?: 0
        if (s.e2Initialized && food < s.disciples.size * 10) s = apply(s, GameCommand.BuyFood(s.disciples.size * 20))
        if (profile == Profile.PASSIVE) return Triple(s, 0, 0)

        // Awanse i darmowe przyjęcia na egzaminie.
        s.disciples.filter { com.example.game.engine.DiscipleRules.canPromote(s, it) }.forEach { s = apply(s, GameCommand.Promote(it.id)) }
        if (s.e2Initialized && com.example.game.engine.SectCommands.admissionCost(s) == 0) {
            s.availableRecruits.maxByOrNull { it.spiritRoot?.grade?.ordinal ?: 0 }?.let { s = apply(s, GameCommand.Recruit(it.id)) }
        }

        // Rezerwa: 5 dni pensji, utrzymania i jedzenia – rozsądny gracz nie schodzi poniżej.
        val reserve = 5 * (s.disciples.sumOf { com.example.game.engine.DiscipleRules.salary(it) } +
            com.example.game.engine.EdictCatalog.dailyUpkeep(s) + s.disciples.size * com.example.game.engine.DiscipleRules.FOOD_PRICE)
        fun canSpend(amount: Int) = s.spiritStones - amount >= reserve

        // Warzenie pigułek przełomu, gdy są składniki.
        val pillRecipe = GameContent.recipeForPill(GameContent.BREAKTHROUGH_PILL)
        if (pillRecipe != null && (s.inventory[GameContent.BREAKTHROUGH_PILL] ?: 0) < 5 && canSpend(pillRecipe.requiredSpiritStones)) {
            s = apply(s, GameCommand.BrewPill(pillRecipe.id))
        }

        // Rozbudowa Sali Alchemii, gdy stać nas z zapasem.
        s.facilities.find { it.type == FacilityType.ALCHEMY_CHAMBER }?.let { f ->
            if (s.spiritStones > f.upgradeCostStones * 3 && canSpend(f.upgradeCostStones)) s = apply(s, GameCommand.UpgradeFacility(FacilityType.ALCHEMY_CHAMBER))
        }

        // Rekrutacja najlepszego kandydata przy nadwyżce.
        if (s.spiritStones > 2000 && s.disciples.size < 30 && canSpend(com.example.game.engine.SectCommands.admissionCost(s))) {
            s.availableRecruits.maxByOrNull { it.combatPower }?.let { s = apply(s, GameCommand.Recruit(it.id)) }
        }

        // Etap 3: ścieżka, metody ze zwojów, techniki (wąskie gardło Złotego Rdzenia), Prawa, przełomy gracza.
        if (s.e3Initialized) s = cultivate(s)

        // Wyprawa: najsłabsza niezbadana kraina, najsilniejszy zdrowy uczeń, jeśli ma szansę.
        if (s.combat == null) {
            val champ = s.disciples.filter { it.currentHp > it.maxHp / 2 }.maxByOrNull { it.attack }
            val region = s.regions.filter { it.explorationProgress < 100 && it.bossBeast != null }
                .minByOrNull { it.bossBeast!!.hp }
            if (champ != null && region != null && champ.attack * 6 > region.bossBeast!!.defense * 2 + region.bossBeast!!.hp / 20) {
                s = apply(s, GameCommand.StartExpedition(region.id, champ.id))
                var guard = 0
                while (s.combat?.finished == false && guard++ < 100) s = apply(s, GameCommand.CombatStep)
                if (s.combat?.outcome == true) wins++ else losses++
                s = apply(s, GameCommand.CloseCombat)
            }
        }
        return Triple(s, wins, losses)
    }

    private fun cultivate(start: GameState): GameState {
        var s = start
        for (d0 in start.disciples) {
            var d = s.disciple(d0.id) ?: continue
            val major = d.majorRealm ?: continue
            if (d.path == null && major.ordinal >= com.example.model.MajorRealm.FOUNDATION.ordinal) {
                s = apply(s, GameCommand.ChoosePath(d.id, com.example.model.CultivationPath.ORTHODOX))
            }
            d = s.disciple(d.id) ?: continue
            s.inventory.filter { it.value > 0 && it.key.startsWith("Zwój: ") }.keys.sorted().forEach { scroll ->
                s = apply(s, GameCommand.LearnMethod(d.id, scroll))
            }
            d = s.disciple(d.id) ?: continue
            GameContent.skills
                .filter { it.martialSchool == d.martialSchool && it.id !in d.unlockedSkillIds && it.tier <= d.rank.maxTechniqueTier }
                .minByOrNull { it.costDaoPoints }
                ?.let { s = apply(s, GameCommand.UnlockSkill(d.id, it.id)) }
            d = s.disciple(d.id) ?: continue
            if (d.laws.isEmpty() && (d.majorRealm?.ordinal ?: 0) >= com.example.model.MajorRealm.NASCENT_SOUL.ordinal) {
                s = apply(s, GameCommand.ChooseLaw(d.id, com.example.model.DaoLaw.SWORD))
            }
            d = s.disciple(d.id) ?: continue
            if (com.example.game.engine.CultivationSystem.ready(d) && Realms.atPeak(d) &&
                com.example.game.engine.CultivationSystem.bottleneck(s, d) == null && d.currentHp > d.maxHp * 0.8) {
                s = apply(s, GameCommand.AttemptBreakthrough(d.id, usePill = true))
            }
        }
        return s
    }

    // --- Raport ---

    fun csv(run: Run): String = buildString {
        appendLine("hours,day,stones,reputation,disciples,top_realm,regions_explored,breakthrough_pills")
        run.snapshots.forEach {
            appendLine("%.2f,%d,%d,%d,%d,%s,%d,%d".format(java.util.Locale.ROOT, it.hours, it.day, it.stones, it.reputation, it.disciples, it.topRealm.name, it.regionsExplored, it.pills))
        }
    }

    /** Czas gry czytelnie: minuty poniżej godziny, godziny poniżej doby, dni powyżej. */
    fun fmt(hours: Double): String = when {
        hours < 1.0 -> "%.0f min".format(java.util.Locale.ROOT, hours * 60)
        hours < 48.0 -> "%.1f h".format(java.util.Locale.ROOT, hours)
        else -> "%.1f dni".format(java.util.Locale.ROOT, hours / 24)
    }

    fun report(runs: List<Run>, hours: Double): String = buildString {
        appendLine("# Raport balansu DaozFlame")
        appendLine()
        appendLine("Symulacja ${"%.0f".format(hours)} h gry na profil (1 tick = $SECONDS_PER_TICK s).")
        appendLine()
        appendLine("## Pierwsze osiągnięcie królestwa (czas gry)")
        appendLine()
        appendLine("| Królestwo | " + runs.joinToString(" | ") { it.profile.label } + " | Cel (aktywny) | Odchylenie aktywnego |")
        appendLine("| --- | " + runs.joinToString(" | ") { "---" } + " | --- | --- |")
        CultivationRealm.entries.forEach { realm ->
            val cells = runs.map { r -> r.realmReachedAtHours[realm]?.let { if (it == 0.0) "start" else fmt(it) } ?: "—" }
            val target = PACE_TARGETS_HOURS[realm]
            val active = runs.find { it.profile == Profile.ACTIVE }?.realmReachedAtHours?.get(realm)
            val deviation = when {
                target == null -> ""
                active == null -> "⚠ nie osiągnięto"
                else -> {
                    val d = (active - target) / target * 100
                    "%+.0f%%".format(java.util.Locale.ROOT, d) + if (kotlin.math.abs(d) > 30) " ⚠" else ""
                }
            }
            appendLine("| ${realm.polishName} | ${cells.joinToString(" | ")} | ${target?.let { fmt(it) } ?: ""} | $deviation |")
        }
        appendLine()
        appendLine("## Stan końcowy")
        appendLine()
        appendLine("| Profil | Dzień gry | Kamienie | Reputacja | Uczniowie (min) | Morale (min) | Krainy | Bossowie | Porażki |")
        appendLine("| --- | --- | --- | --- | --- | --- | --- | --- | --- |")
        runs.forEach { r ->
            val last = r.snapshots.last()
            val minD = r.snapshots.minOf { it.disciples }
            val minM = r.snapshots.minOf { it.morale }
            appendLine("| ${r.profile.label} | ${last.day} | ${last.stones} | ${last.reputation} | ${last.disciples} ($minD) | ${last.morale} ($minM) | ${last.regionsExplored} | ${r.bossesDefeated} | ${r.defeats} |")
        }
        appendLine()
        appendLine("## Ostrzeżenia")
        appendLine()
        val warnings = warnings(runs)
        if (warnings.isEmpty()) appendLine("Brak.") else warnings.forEach { appendLine("- $it") }
    }

    /** Automatyczne sygnały problemów balansu. */
    fun warnings(runs: List<Run>): List<String> = buildList {
        runs.forEach { r ->
            val first = r.snapshots.first().stones.toDouble()
            val last = r.snapshots.last().stones.toDouble()
            val mid = r.snapshots[r.snapshots.size / 2].stones.toDouble()
            if (last > 1_000_000) add("${r.profile.label}: ponad milion kamieni – sprawdź nieskończone źródła.")
            if (mid > 0 && last / mid > 50) add("${r.profile.label}: kamienie rosną wykładniczo (×${"%.0f".format(last / mid)} w drugiej połowie).")
            if (last < first && r.profile != Profile.PASSIVE) add("${r.profile.label}: kamienie spadły względem startu – możliwa spirala kosztów.")
            if (r.snapshots.minOf { it.disciples } < r.snapshots.first().disciples / 2) add("${r.profile.label}: sekta straciła ponad połowę uczniów – sprawdź lojalność i pensje.")
            if (r.snapshots.minOf { it.morale } < 20) add("${r.profile.label}: morale spadło poniżej 20.")
            if (r.realmReachedAtHours.keys.maxOrNull() == CultivationRealm.entries.last() && r.snapshots.last().hours < 24) {
                add("${r.profile.label}: szczyt drabiny kultywacji w mniej niż dobę gry.")
            }
        }
    }

    fun writeReport(dir: File, hours: Double, seed: Long = 20261027L): List<Run> {
        dir.mkdirs()
        val runs = Profile.entries.map { simulate(it, seed, hours) }
        runs.forEach { File(dir, "sim_${it.profile.name.lowercase()}.csv").writeText(csv(it)) }
        File(dir, "raport.md").writeText(report(runs, hours))
        return runs
    }

    @JvmStatic
    fun main(args: Array<String>) {
        val hours = args.firstOrNull()?.toDoubleOrNull() ?: 48.0
        val dir = File(args.getOrNull(1) ?: "build/sim")
        writeReport(dir, hours)
        println(File(dir, "raport.md").readText())
    }
}
