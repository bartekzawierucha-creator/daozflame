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
