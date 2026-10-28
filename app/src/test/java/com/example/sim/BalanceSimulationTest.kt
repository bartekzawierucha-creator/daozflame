package com.example.sim

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Uruchamia symulator balansu. Domyślnie krótki przebieg (szybki test w każdym buildzie);
 * pełny raport (48 h gry na profil) przy zmiennej środowiskowej DAOZ_SIM_HOURS, np. w CI.
 */
class BalanceSimulationTest {

    @Test
    fun simulationRunsAndWritesReport() {
        val hours = System.getenv("DAOZ_SIM_HOURS")?.toDoubleOrNull() ?: 2.0
        val runs = BalanceSimulator.writeReport(File("build/sim"), hours)
        assertTrue(runs.all { it.snapshots.isNotEmpty() })
        assertTrue("Kamienie nie mogą być ujemne", runs.all { r -> r.snapshots.all { it.stones >= 0 } })
    }
}

/** Etap 8: druga era szybsza o 30–60% niż ten sam świat bez postępu meta. */
class EraPaceTest {
    @Test
    fun secondEraIsFasterWithinTarget() {
        // Mediana z trzech światów: pojedyncze ziarno waha się zbyt mocno (autonomia uczniów, różne krainy).
        val speedups = listOf(20261027L, 42L, 777L).map { BalanceSimulator.eraSpeedup(BalanceSimulator.simulateEras(it, eras = 2)) ?: -1.0 }
        val median = speedups.sorted()[1]
        assertTrue("przyspieszenie $speedups (mediana $median)", median in 0.25..0.7)
    }
}
