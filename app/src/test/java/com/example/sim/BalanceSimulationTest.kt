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
        val eras = BalanceSimulator.simulateEras(20261027L, eras = 2)
        val speedup = BalanceSimulator.eraSpeedup(eras)
        assertTrue("przyspieszenie $speedup", speedup != null && speedup in 0.3..0.6)
    }
}
