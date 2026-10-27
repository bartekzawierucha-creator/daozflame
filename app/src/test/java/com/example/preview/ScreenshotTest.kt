package com.example.preview

import android.app.Application
import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.core.app.ApplicationProvider
import com.example.game.TribulationUi
import com.example.game.TutorialUi
import com.example.game.WuxiaGameViewModel
import com.example.game.WuxiaUiState
import com.example.game.engine.GameState
import com.example.game.engine.OfflineProgress
import com.example.ui.aaa.Aaa
import com.example.ui.game.*
import com.example.ui.theme.WuxiaSectTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/**
 * Podgląd wszystkich ekranów: renderuje każdy moduł i panel na JVM (Robolectric, natywna grafika)
 * ze stanu [PreviewWorld] i zapisuje PNG do app/build/previews. CI publikuje je na gałęzi `previews`.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36], qualifiers = "w400dp-h866dp-xhdpi")
class ScreenshotTest {

    @get:Rule
    val compose = createAndroidComposeRule<androidx.activity.ComponentActivity>()

    private val out = File("build/previews").apply { mkdirs() }
    private val index = StringBuilder()

    private var screen by mutableStateOf<(@Composable () -> Unit)?>(null)
    /** Klucz ekranu: nowy ekran = świeży stan (zakładki, rozwinięcia), bez przecieków z poprzedniego. */
    private var screenKey by mutableStateOf("")

    private val errors = StringBuilder()

    private fun record(where: String, t: Throwable) {
        errors.append("== ").append(where).append('\n').append(t.stackTraceToString().lines().take(40).joinToString("\n")).append("\n\n")
    }

    /** Zrzut z powiększoną czcionką systemową (jak na telefonach z większym tekstem). */
    private fun largeShot(name: String, title: String, content: @Composable () -> Unit) = shot(name, "$title · czcionka ×1,3") {
        val d = androidx.compose.ui.platform.LocalDensity.current
        androidx.compose.runtime.CompositionLocalProvider(
            androidx.compose.ui.platform.LocalDensity provides androidx.compose.ui.unit.Density(d.density, 1.3f)
        ) { content() }
    }

    private fun shot(name: String, title: String, content: @Composable () -> Unit) {
        try {
            screenKey = name
            screen = content
            androidx.compose.runtime.snapshots.Snapshot.sendApplyNotifications()
            compose.mainClock.advanceTimeBy(1_500)
            // Rysujemy widok wprost do bitmapy: captureToImage czeka na klatkę, której zatrzymany zegar nie wyrysuje.
            compose.runOnUiThread {
                val v = compose.activity.findViewById<android.view.ViewGroup>(android.R.id.content)
                val bmp = Bitmap.createBitmap(v.width, v.height, Bitmap.Config.ARGB_8888)
                v.draw(android.graphics.Canvas(bmp))
                File(out, "$name.png").outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
            }
            index.append(name).append('\t').append(title).append('\n')
        } catch (t: Throwable) {
            record(name, t)
            screen = null
        }
    }

    /** Błędy pojedynczych ekranów nie przerywają podglądu: trafiają do errors.txt obok zrzutów. */
    @Test
    fun allScreens() {
        try {
            renderAll()
        } catch (t: Throwable) {
            record("setup", t)
        } finally {
            File(out, "index.tsv").writeText(index.toString())
            File(out, "errors.txt").writeText(errors.toString())
        }
    }

    private fun renderAll() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val vm = WuxiaGameViewModel(app)
        val busy: GameState = PreviewWorld.build()
        val calm: GameState = busy.copy(activeEventId = null)
        fun ui(s: GameState, selected: String? = null): WuxiaUiState {
            vm.previewState(s, selected ?: s.chosenId)
            return vm.uiState.value
        }

        compose.mainClock.autoAdvance = false
        compose.setContent {
            WuxiaSectTheme {
                Box(Modifier.fillMaxSize().background(Aaa.Ink0)) { androidx.compose.runtime.key(screenKey) { screen?.invoke() } }
            }
        }

        val u = ui(calm)
        val chosen = u.disciples.first { it.id == calm.chosenId }
        val sect = u.rivalSects.first { it.id == "sect_shadow" }
        val noop: () -> Unit = {}

        // ── Hub i moduły ────────────────────────────────────────────────────
        shot("01_hub", "Hub sekty – alerty, odznaki") { SectHub(u, vm, onOpen = { _, _ -> }) }
        shot("02_palace", "Pałac – kafle zarządu") { PalaceModule(u, vm, noop) }
        shot("03_adepts", "Adepci – karta ucznia ze znacznikami") { AdeptsModule(u, vm, noop) }
        shot("04_alchemy", "Alchemia") { AlchemyModule(u, vm, noop) }
        shot("05_forge", "Kuźnia") { ForgeModule(u, vm, noop) }
        shot("06_beasts", "Bestie – etap ewolucji, jeździec, jaja") { BeastsModule(u, vm, noop) }
        shot("07_world", "Świat – mapa krain, mgła") { WorldModule(u, vm, noop) }
        shot("08_clans", "Klany – dyplomacja") { ClansModule(u, vm, noop) }
        shot("09_daonet", "Sieć Dao") { DaoNetModule(u, vm, noop) }
        shot("10_vault", "Magazyn") { VaultModule(u, vm, noop) }
        val ub = ui(busy)
        shot("11_omen", "Omen – zdarzenie fabuły (akt II)") { OmenModule(ub, vm, noop) }
        ui(calm)

        // ── Etap 1–3 ────────────────────────────────────────────────────────
        shot("12_tutorial", "Samouczek") {
            Box(Modifier.fillMaxSize()) { TutorialBanner(TutorialUi(2, 10, "Pierwszy przełom", "Uczeń z pełnym Qi może przełamać barierę etapu.", true), {}, {}) }
        }
        shot("13_offline", "Gdy cię nie było") {
            OfflineSummaryDialog(OfflineProgress.Report(7_200_000, 7_200_000, 4_500, 3_120, 400, 174, mapOf("Li Long" to 18, "Yue Qing" to 12), 2, mapOf("Trawa Wiecznej Wiosny" to 14)), noop)
        }
        shot("14_exam", "Egzamin wstępny") { ExamPanel(u, vm, noop) }
        shot("15_cultivation", "Kultywacja ucznia") { CultivationPanel(chosen, u, vm, noop) }
        shot("16_tribulation", "Tribulacja") { TribulationOverlay(TribulationUi(1, chosen.name, 6, listOf(120, 180, 90, 260, 140, 210), chosen.maxHp, true, false, false), noop) }

        // ── Etap 4: gospodarka ──────────────────────────────────────────────
        EconomyTab.entries.forEachIndexed { i, t ->
            shot("2${i}_economy_${t.name.lowercase()}", "Gospodarka – ${t.label}") { EconomyPanel(u, vm, t, noop) }
        }

        // ── Etap 5: świat i walka ───────────────────────────────────────────
        shot("30_expedition_run", "Wyprawa w toku – mapa węzłów") { ExpeditionPanel(null, u, vm, noop) }
        val noExp = ui(calm.copy(expedition = null))
        shot("31_expedition_plan", "Planowanie wyprawy") { ExpeditionPanel(noExp.regions.first(), noExp, vm, noop) }
        ui(calm)
        shot("32_tower", "Wieża Prób") { TowerPanel(u, vm, noop) }
        shot("33_secret_realms", "Tajne królestwa") { SecretRealmsPanel(u, vm, noop) }
        SectsTab.entries.forEachIndexed { i, t ->
            shot("3${4 + i}_sects_${t.name.lowercase()}", "Sekty świata – ${t.label}") { SectsPanel(u, vm, sect, t, noop) }
        }
        shot("38_talismans_in_combat", "Talizmany w walce") { Box(Modifier.fillMaxSize()) { CombatTalismanBar(u, vm) } }

        // ── Etap 6: tajemnice ───────────────────────────────────────────────
        (0..3).forEach { i ->
            shot("4${i}_dao_${listOf("meridians", "breath", "contemplation", "tools")[i]}", "Dao ucznia – ${listOf("Meridiany", "Oddech", "Kontemplacja", "Narzędzia")[i]}") {
                SecretsPanel(chosen, u, vm, i, noop)
            }
        }
        shot("44_journal", "Księga Tajemnic") { JournalPanel(u, 0, noop) }

        // ── Etap 7: sekta i życie ───────────────────────────────────────────
        SectTab.entries.forEachIndexed { i, t ->
            shot("5${i}_sect_${t.name.lowercase()}", "Sekta – ${t.label}") { SectPanel(u, vm, t, noop) }
        }
        shot("55_life", "Życie ucznia") { LifePanel(chosen, u, vm, noop) }
        shot("56_mind", "Umysł ucznia – zamiar, więź, rady, dziennik") { MindPanel(chosen, u, vm, noop) }

        // ── Etap 8: meta ────────────────────────────────────────────────────
        MetaTab.entries.forEachIndexed { i, t ->
            shot("6${i}_meta_${t.name.lowercase()}", "Niebiosa – ${t.label}") { MetaPanel(u, vm, t, noop) }
        }

        // ── Etap 9: Sieć Dao ────────────────────────────────────────────────
        shot("70_arena", "Arena cieni") { ShadowArenaPanel(u, vm, noop) }

        // ── Duża czcionka systemowa ─────────────────────────────────────────
        largeShot("80_large_hub", "Hub") { SectHub(u, vm, onOpen = { _, _ -> }) }
        largeShot("81_large_disciples", "Sala Adeptów (Księga)") {
            LegacyPanel("Sala Adeptów", onClose = noop) { com.example.ui.screens.DisciplesScreen(viewModel = vm, uiState = u) }
        }
        largeShot("82_large_adepts", "Adepci") { AdeptsModule(u, vm, noop) }
        largeShot("83_large_mind", "Umysł ucznia") { MindPanel(chosen, u, vm, noop) }
        largeShot("84_large_market", "Gospodarka – Targ") { EconomyPanel(u, vm, EconomyTab.MARKET, noop) }
    }
}
