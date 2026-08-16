package com.tangem.core.remote.config

typealias ApiConfigs = Map<String, @JvmSuppressWildcards ApiConfig>

/**
 * Api config
 *
 * @see <a href="https://www.notion.so/tangem/API-eacb264e7daf420a88b419a8a26f5b26?pvs=4">API configuration</a>
 *
[REDACTED_AUTHOR]
 */
// Base type for configs declared in other modules; keep it a class, not an interface.
@Suppress("UnnecessaryAbstractClass")
abstract class ApiConfig {

    /** Default environment */
    abstract val defaultEnvironment: ApiEnvironment

    /** Available environments */
    abstract val environmentConfigs: List<ApiEnvironmentConfig>

    /** Unique id */
    abstract val id: ID

    /**
     * Whether this API's requests may be written to network logs. Defaults to `true`; override to `false`
     * when the requests carry a secret (e.g. an `apiKey` query param) that must not be logged.
     */
    open val isLoggable: Boolean get() = true

    /**
     * Type-safe config identifier, backed by a stable string [name] that is also the DI map key.
     * Each config declares its own key and id next to itself (e.g. `Express.KEY` / `Express.ID`).
     */
    @JvmInline
    value class ID(val name: String)

    companion object {
        const val DEBUG_BUILD_TYPE = "debug"
        const val INTERNAL_BUILD_TYPE = "internal"
        const val MOCKED_BUILD_TYPE = "mocked"
        const val EXTERNAL_BUILD_TYPE = "external"
        const val RELEASE_BUILD_TYPE = "release"
    }
}