package com.tangem.plugin.configuration.model

enum class BuildType(
    val id: String,
    internal val appIdSuffix: String? = null,
    internal val versionSuffix: String? = null,
    internal val obfuscating: Boolean = false,
    internal val configFields: List<BuildConfigField>,
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
            BuildConfigField.ABTestsEnabled(isEnabled = false),
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
            BuildConfigField.TesterMenuAvailability(isEnabled = true),
            BuildConfigField.MockDataSource(isEnabled = true),
            BuildConfigField.ABTestsEnabled(isEnabled = false),
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
            BuildConfigField.ABTestsEnabled(isEnabled = false),
        ),
    ),

    /**
     * Build type for support with possible bugfixes for certain cases
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
            BuildConfigField.ABTestsEnabled(isEnabled = false),
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
            BuildConfigField.ABTestsEnabled(isEnabled = false),
        ),
    ),
    ;

    /** Returns the environment value (dev/prod) for this build type */
    val environment: String
        get() {
            val environmentField = configFields
                .filterIsInstance<BuildConfigField.Environment>()
                .firstOrNull()

            requireNotNull(environmentField) {
                "BuildType '$id' must have a BuildConfigField.Environment in configFields"
            }

            return environmentField.value.removeSurrounding("\"")
        }
}