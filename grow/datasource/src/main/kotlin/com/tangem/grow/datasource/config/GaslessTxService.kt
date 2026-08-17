package com.tangem.grow.datasource.config

import com.tangem.core.remote.config.ApiConfig
import com.tangem.core.remote.config.ApiEnvironment
import com.tangem.core.remote.config.ApiEnvironmentConfig
import com.tangem.core.remote.header.RequestHeader
import com.tangem.grow.datasource.BuildConfig
import com.tangem.utils.ProviderSuspend
import com.tangem.utils.info.AppInfoProvider

/**
 * Gasless transactions [ApiConfig]
 */
class GaslessTxService(
    private val growEnvironmentConfig: GrowEnvironmentConfig,
    private val appInfoProvider: AppInfoProvider,
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
        putAll(RequestHeader.AppVersionPlatformHeaders(appInfoProvider).values)
        put(
            key = "Authorization",
            value = ProviderSuspend {
                val apiKey = when (environment) {
                    ApiEnvironment.MOCK,
                    ApiEnvironment.DEV,
                    -> growEnvironmentConfig.gaslessTxApiKeyDev
                    ApiEnvironment.PROD -> growEnvironmentConfig.gaslessTxApiKey
                    else -> error("No gasless tx api config provided for $environment")
                } ?: error("No gasless tx api config provided")
                "Bearer $apiKey"
            },
        )
    }

    companion object {

        const val KEY = "GaslessTxService"
        val ID = ApiConfig.ID(KEY)

        private const val PROD_BASE_URL = "https://gasless.tangem.org/"
        private const val DEV_BASE_URL = "[REDACTED_ENV_URL]"
        private const val MOCK_BASE_URL = "[REDACTED_ENV_URL]"
    }
}