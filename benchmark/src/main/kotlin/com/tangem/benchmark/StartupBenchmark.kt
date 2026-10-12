package com.tangem.benchmark

import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.StartupTimingMetric
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Measures how long the app takes to start.
 *
 * Reports `timeToInitialDisplay`. The splash screen holds back the first frame until the app is ready
 * (`setKeepOnScreenCondition` in `MainActivity`), so this already covers the whole wait behind the splash.
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
    }

    private companion object {
        const val TARGET_PACKAGE = "com.tangem.wallet.external"
        const val ITERATIONS = 10
    }
}