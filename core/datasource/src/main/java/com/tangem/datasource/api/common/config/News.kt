package com.tangem.datasource.api.common.config

import com.tangem.datasource.BuildConfig
import com.tangem.datasource.api.common.AuthProvider
import com.tangem.datasource.utils.RequestHeader
import com.tangem.utils.Provider

/**
 * News [ApiConfig]
[REDACTED_AUTHOR]
 */
internal class News(
    private val authProvider: AuthProvider,
) : ApiConfig() {

    override val defaultEnvironment: ApiEnvironment = getInitialEnvironment()

    override val environmentConfigs: List<ApiEnvironmentConfig> = listOf(
        createProdEnvironment(),
        createDevEnvironment(),
    )

    private fun getInitialEnvironment(): ApiEnvironment {
        return when (BuildConfig.BUILD_TYPE) {
            MOCKED_BUILD_TYPE,
            DEBUG_BUILD_TYPE,
            -> ApiEnvironment.DEV
            INTERNAL_BUILD_TYPE,
            EXTERNAL_BUILD_TYPE,
            RELEASE_BUILD_TYPE,
            -> ApiEnvironment.PROD
            else -> error("Unknown build type [${BuildConfig.BUILD_TYPE}]")
        }
    }

    private fun createProdEnvironment(): ApiEnvironmentConfig = ApiEnvironmentConfig(
        environment = ApiEnvironment.PROD,
        baseUrl = PROD_BASE_URL,
        headers = createHeaders(ApiEnvironment.PROD),
    )

    private fun createDevEnvironment(): ApiEnvironmentConfig = ApiEnvironmentConfig(
        environment = ApiEnvironment.DEV,
        baseUrl = DEV_BASE_URL,
        headers = createHeaders(ApiEnvironment.DEV),
    )

    private fun createHeaders(environment: ApiEnvironment) = buildMap {
        putAll(
            RequestHeader.TangemApiKeyHeader(
                authProvider = authProvider,
                apiEnvironment = Provider { environment },
            ).values,
        )
    }

    private companion object {

        private const val PROD_BASE_URL = "https://api.tangem.org/"
        private const val DEV_BASE_URL = "[REDACTED_ENV_URL]"
    }
}