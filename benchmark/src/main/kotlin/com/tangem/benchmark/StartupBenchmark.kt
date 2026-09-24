package com.tangem.benchmark

import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.StartupTimingMetric
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Until
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Measures how long the app takes to start.
 *
 * Reports `timeToInitialDisplay` (first frame — for this app, the splash screen) and, once
 * `reportFullyDrawn()` is called on the first usable screen, `timeToFullDisplay`.
 *
 * `StartupMode.WARM` is not covered: `MainActivity` is `singleInstance`, so a warm launch reuses the
 * existing instance and the measurement never sees a new frame.
 *
 * Run on a physical device, `external` build type:
 * ```
 * ./gradlew :benchmark:connectedExternalAndroidTest
 * ```
 * On a ROM that refuses `cmd package compile` (realme/ColorOS answers `Failed to cpmpile !`), add
 * `-Pandroid.testInstrumentationRunnerArguments.androidx.benchmark.compilation.enabled=false`.
 * Results are printed to the console and written to
 * `benchmark/build/outputs/connected_android_test_additional_output/`.
 */
@RunWith(AndroidJUnit4::class)
class StartupBenchmark {

    @get:Rule
    val benchmarkRule = MacrobenchmarkRule()

    @Test
    fun startupCold() = measureStartup(StartupMode.COLD)

    @Test
    fun startupHot() = measureStartup(StartupMode.HOT)

    private fun measureStartup(startupMode: StartupMode) = benchmarkRule.measureRepeated(
        packageName = TARGET_PACKAGE,
        metrics = listOf(StartupTimingMetric()),
        // DEFAULT compiles the app the way the Play install does, so the numbers match the field
        compilationMode = CompilationMode.DEFAULT,
        startupMode = startupMode,
        iterations = ITERATIONS,
        setupBlock = { pressHome() },
    ) {
        startActivityAndWait()

        // startActivityAndWait() returns on the first frame, which here is still the splash screen.
        // Waiting for the app's own window keeps a start that never reaches the UI from being recorded
        // as a fast one.
        val hasContent = device.wait(Until.hasObject(By.pkg(TARGET_PACKAGE).depth(0)), CONTENT_TIMEOUT_MS)
        check(hasContent) { "$TARGET_PACKAGE did not render its content within $CONTENT_TIMEOUT_MS ms" }
    }

    private companion object {
        const val TARGET_PACKAGE = "com.tangem.wallet.external"
        const val ITERATIONS = 10
        const val CONTENT_TIMEOUT_MS = 20_000L
    }
}