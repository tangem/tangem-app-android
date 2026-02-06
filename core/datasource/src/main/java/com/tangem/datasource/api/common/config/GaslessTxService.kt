package com.tangem.datasource.api.common.config

import com.tangem.datasource.BuildConfig
import com.tangem.datasource.api.common.AuthProvider
import com.tangem.datasource.utils.RequestHeader
import com.tangem.utils.Provider
import com.tangem.utils.ProviderSuspend
import com.tangem.utils.info.AppInfoProvider
import com.tangem.utils.version.AppVersionProvider

/**
 * Gasless transactions [ApiConfig]
 */
internal class GaslessTxService(
    private val authProvider: AuthProvider,
    private val appVersionProvider: AppVersionProvider,
    private val appInfoProvider: AppInfoProvider,
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
        putAll(RequestHeader.AppVersionPlatformHeaders(appVersionProvider, appInfoProvider).values)
        put(
            key = "Authorization",
            value = ProviderSuspend {
                "Bearer ${authProvider.getGaslessServiceApiKey(Provider { environment }).invoke()}"
            },
        )
    }

    private companion object {
        private const val PROD_BASE_URL = "https://gasless.tangem.org/"
        private const val DEV_BASE_URL = "[REDACTED_ENV_URL]"
    }
}