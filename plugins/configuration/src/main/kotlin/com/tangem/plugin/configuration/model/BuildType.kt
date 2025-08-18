package com.tangem.plugin.configuration.model

internal enum class BuildType(
    val id: String,
    val appIdSuffix: String? = null,
    val versionSuffix: String? = null,
    val obfuscating: Boolean = false,
    val configFields: List<BuildConfigField>,
) {

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
        appIdSuffix = "debug",
        configFields = listOf(
            BuildConfigField.Environment(value = "dev"),
            BuildConfigField.LogEnabled(isEnabled = true),
            BuildConfigField.TesterMenuAvailability(isEnabled = true),
            BuildConfigField.MockDataSource(isEnabled = false),
        ),
    ),

    /**
     * Build type for QA and business
     *
     * Features:
     * - Env: dev
     * - Signing config: debug
     * - Logs
     * - Enabled mocked datasource
     * */
    Mocked(
        id = "mocked",
        appIdSuffix = "mocked",
        versionSuffix = "mocked",
        configFields = listOf(
            BuildConfigField.Environment(value = "dev"),
            BuildConfigField.LogEnabled(isEnabled = true),
            BuildConfigField.TesterMenuAvailability(isEnabled = false),
            BuildConfigField.MockDataSource(isEnabled = true),
        ),
    ),

    /**
     * Build type for QA
     *
     * Features:
     * - Env: dev
     * - Signing config: debug
     * - Logs
     * - Tester menu
     * - Test action
     * - Traffic sniffing
     * */
    Internal(
        id = "internal",
        appIdSuffix = "internal",
        versionSuffix = "internal",
        configFields = listOf(
            BuildConfigField.Environment(value = "prod"),
            BuildConfigField.LogEnabled(isEnabled = true),
            BuildConfigField.TesterMenuAvailability(isEnabled = true),
            BuildConfigField.MockDataSource(isEnabled = false),
        ),
    ),

    /**
     * Build type for QA and business
     *
     * Features:
     * - Env: prod
     * - Signing config: debug
     * - Proguard
     * */
    External(
        id = "external",
        appIdSuffix = "external",
        versionSuffix = "external",
        configFields = listOf(
            BuildConfigField.Environment(value = "prod"),
            BuildConfigField.LogEnabled(isEnabled = false),
            BuildConfigField.TesterMenuAvailability(isEnabled = false),
            BuildConfigField.MockDataSource(isEnabled = false),
        ),
    ),

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
            BuildConfigField.LogEnabled(isEnabled = false),
            BuildConfigField.TesterMenuAvailability(isEnabled = false),
            BuildConfigField.MockDataSource(isEnabled = false),
        ),
    ),
}