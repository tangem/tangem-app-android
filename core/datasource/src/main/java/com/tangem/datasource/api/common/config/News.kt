package com.tangem.datasource.api.common.config

import com.tangem.core.remote.config.ApiConfig
import com.tangem.core.remote.config.ApiEnvironment
import com.tangem.core.remote.config.ApiEnvironmentConfig

import com.tangem.datasource.BuildConfig
import com.tangem.datasource.api.common.AuthProvider
import com.tangem.datasource.utils.RequestHeader
import com.tangem.utils.Provider
import com.tangem.utils.info.AppInfoProvider

/**
 * News [ApiConfig]
[REDACTED_AUTHOR]
 */
class News(
    private val appInfoProvider: AppInfoProvider,
    private val authProvider: AuthProvider,
) : ApiConfig() {

    override val id: ApiConfig.ID get() = ID

    override val defaultEnvironment: ApiEnvironment = getInitialEnvironment()

    override val environmentConfigs: List<ApiEnvironmentConfig> = listOf(
        createProdEnvironment(),
        createDevEnvironment(),
        createMockedEnvironment(),
    )

    private fun getInitialEnvironment(): ApiEnvironment {
        return when (BuildConfig.BUILD_TYPE) {
            MOCKED_BUILD_TYPE,
            -> ApiEnvironment.MOCK
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

    private fun createMockedEnvironment(): ApiEnvironmentConfig = ApiEnvironmentConfig(
        environment = ApiEnvironment.MOCK,
        baseUrl = MOCK_BASE_URL,
        headers = createHeaders(ApiEnvironment.MOCK),
    )

    private fun createHeaders(environment: ApiEnvironment) = buildMap {
        putAll(
            RequestHeader.TangemApiKeyHeader(
                authProvider = authProvider,
                apiEnvironment = Provider { environment },
            ).values,
        )
        putAll(from = RequestHeader.AppVersionPlatformHeaders(appInfoProvider).values)
    }

    companion object {

        const val KEY = "News"
        val ID = ApiConfig.ID(KEY)

        private const val PROD_BASE_URL = "https://api.tangem.org/"
        private const val DEV_BASE_URL = "[REDACTED_ENV_URL]"
        private const val MOCK_BASE_URL = "[REDACTED_ENV_URL]"
    }
}