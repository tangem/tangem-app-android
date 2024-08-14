package com.tangem.datasource.api.common.config

typealias ApiConfigs = Set<@JvmSuppressWildcards ApiConfig>

/**
 * Api config
 *
 * @see <a href="https://www.notion.so/tangem/API-eacb264e7daf420a88b419a8a26f5b26?pvs=4">API configuration</a>
 *
 * @author Andrew Khokhlov on 07/08/2024
 */
sealed class ApiConfig {

    /** Default environment */
    abstract val defaultEnvironment: ApiEnvironment

    /** Available environments with base url */
    abstract val environments: Map<ApiEnvironment, String>

    /** Unique id */
    val id: ID = initializeId()

    enum class ID {
        Express,
        TangemTech,
    }

    private fun initializeId(): ID {
        return when (this) {
            is Express -> ID.Express
            is TangemTech -> ID.TangemTech
        }
    }

    companion object {
        internal const val DEBUG_BUILD_TYPE = "debug"
        internal const val INTERNAL_BUILD_TYPE = "internal"
        internal const val MOCKED_BUILD_TYPE = "mocked"
        internal const val EXTERNAL_BUILD_TYPE = "external"
        internal const val RELEASE_BUILD_TYPE = "release"
    }
}