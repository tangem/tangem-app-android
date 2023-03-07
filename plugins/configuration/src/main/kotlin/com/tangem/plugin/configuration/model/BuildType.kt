package com.tangem.plugin.configuration.model

internal enum class BuildType(
    val id: String,
    val appIdSuffix: String? = null,
    val versionSuffix: String? = null,
    val configFields: List<BuildConfigField>,
) {

    /**
     * Production ready build type for clients
     *
     * Features:
     * - Env: prod
     * - Signing config: release
     * - Proguard
     * */
    Release(
        id = "release",
        configFields = listOf(
            BuildConfigField.Environment(value = "prod"),
            BuildConfigField.TestActionEnabled(isEnabled = false),
            BuildConfigField.LogEnabled(isEnabled = false),
            BuildConfigField.TesterMenuAvailability(isEnabled = false),
        ),
    ),

    /**
     * Build type for developers
     *
     * Features:
     * - Env: dev
     * - Signing config: debug
     * - Debuggable
     * - Logs
     * - Tester menu
     * - Test action
     * - Traffic sniffing
     * */
    Debug(
        id = "debug",
        appIdSuffix = "dev",
        configFields = listOf(
            BuildConfigField.Environment(value = "dev"),
            BuildConfigField.TestActionEnabled(isEnabled = true),
            BuildConfigField.LogEnabled(isEnabled = true),
            BuildConfigField.TesterMenuAvailability(isEnabled = true),
        ),
    ),

    /**
     * Build type for QA and business
     *
     * Features:
     * - Env: prod
     * - Signing config: debug
     * - Tester menu
     * - Test action
     * - Proguard
     * */
    External(
        id = "debug_beta",
        appIdSuffix = "debug",
        versionSuffix = "debug",
        configFields = listOf(
            BuildConfigField.Environment(value = "prod"),
            BuildConfigField.TestActionEnabled(isEnabled = true),
            BuildConfigField.LogEnabled(isEnabled = false),
            BuildConfigField.TesterMenuAvailability(isEnabled = true),
        ),
    ),
}