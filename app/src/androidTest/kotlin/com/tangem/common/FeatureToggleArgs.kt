package com.tangem.common

import androidx.test.platform.app.InstrumentationRegistry

/**
 * Reads feature-toggle overrides passed to the UI-test run as an instrumentation argument.
 *
 * The value is set by the CI workflow (`run_ui_tests.yml` → Marathonfile `instrumentationArgs.featureToggles`),
 * which in turn is driven by the `feature_toggles` GitHub Actions input / Allure TestOps launch parameter.
 *
 * Format: `RAW_NAME=true,OTHER_RAW_NAME=false`. Keys must be exact [com.tangem.core.configtoggle.FeatureToggles]
 * `rawName` values. An absent/blank argument yields an empty map — i.e. the run behaves exactly as before
 * (backward compatible).
 *
 * This lives in `androidTest` only, so the mechanism is unavailable in release builds by construction.
 */
internal object FeatureToggleArgs {

    const val ARG = "featureToggles"

    /** Raw, unparsed argument value (used for reporting the configuration to Allure). */
    fun rawArg(): String? = InstrumentationRegistry.getArguments().getString(ARG)

    /** Parsed overrides from the instrumentation argument. */
    fun fromInstrumentation(): Map<String, Boolean> = parse(rawArg())

    /** Parses a `name=bool,name=bool` string into a map. Malformed entries are ignored. */
    fun parse(raw: String?): Map<String, Boolean> {
        if (raw.isNullOrBlank()) return emptyMap()

        return raw.split(",")
            .mapNotNull { entry ->
                val parts = entry.split("=", limit = 2)
                if (parts.size != 2) return@mapNotNull null

                val name = parts[0].trim()
                if (name.isEmpty()) return@mapNotNull null

                val rawValue = parts[1].trim()
                val enabled = when {
                    // equals(ignoreCase = true) is locale-independent, unlike lowercase()
                    rawValue.equals("true", ignoreCase = true) -> true
                    rawValue.equals("false", ignoreCase = true) -> false
                    else -> return@mapNotNull null
                }
                name to enabled
            }
            .toMap()
    }
}