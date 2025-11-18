package com.tangem.datasource.api.common.config

import com.tangem.datasource.BuildConfig
import com.tangem.datasource.api.common.AuthProvider
import com.tangem.datasource.local.config.environment.EnvironmentConfigStorage
import com.tangem.datasource.utils.RequestHeader
import com.tangem.utils.ProviderSuspend
import com.tangem.utils.info.AppInfoProvider
import com.tangem.utils.version.AppVersionProvider

/** YieldSupply [ApiConfig] */
internal class YieldSupply(
    private val environmentConfigStorage: EnvironmentConfigStorage,
    private val appVersionProvider: AppVersionProvider,
    private val authProvider: AuthProvider,
    private val appInfoProvider: AppInfoProvider,
) : ApiConfig() {

    override val defaultEnvironment: ApiEnvironment = getInitialEnvironment()

    override val environmentConfigs = listOf(
        createDevEnvironment(),
        createStageEnvironment(),
        createMockedEnvironment(),
        createProdEnvironment(),
    )

    private fun getInitialEnvironment(): ApiEnvironment {
        return when (BuildConfig.BUILD_TYPE) {
            MOCKED_BUILD_TYPE,
            -> ApiEnvironment.MOCK
            DEBUG_BUILD_TYPE,
            INTERNAL_BUILD_TYPE,
            EXTERNAL_BUILD_TYPE,
            RELEASE_BUILD_TYPE,
            -> ApiEnvironment.PROD
            else -> error("Unknown build type [${BuildConfig.BUILD_TYPE}]")
        }
    }

    private fun createDevEnvironment(): ApiEnvironmentConfig = ApiEnvironmentConfig(
        environment = ApiEnvironment.DEV,
        baseUrl = "[REDACTED_ENV_URL]",
        headers = createHeaders(ApiEnvironment.DEV),
    )

    private fun createStageEnvironment(): ApiEnvironmentConfig = ApiEnvironmentConfig(
        environment = ApiEnvironment.STAGE,
        baseUrl = "[REDACTED_ENV_URL]",
        headers = createHeaders(ApiEnvironment.STAGE),
    )

    private fun createMockedEnvironment(): ApiEnvironmentConfig = ApiEnvironmentConfig(
        environment = ApiEnvironment.MOCK,
        baseUrl = "[REDACTED_ENV_URL]",
        headers = createHeaders(ApiEnvironment.MOCK),
    )

    private fun createProdEnvironment(): ApiEnvironmentConfig = ApiEnvironmentConfig(
        environment = ApiEnvironment.PROD,
        baseUrl = "https://yield.tangem.org/",
        headers = createHeaders(ApiEnvironment.PROD),
    )

    private fun createHeaders(apiEnvironment: ApiEnvironment) = buildMap {
        put(key = "api-key", value = ProviderSuspend {
            getApiKey(apiEnvironment)
        })
        putAll(from = RequestHeader.AppVersionPlatformHeaders(appVersionProvider, appInfoProvider).values)
        putAll(from = RequestHeader.AuthenticationHeader(authProvider).values)
    }

    private fun getApiKey(apiEnvironment: ApiEnvironment): String {
        return when (apiEnvironment) {
            ApiEnvironment.MOCK,
            ApiEnvironment.DEV,
            ApiEnvironment.DEV_2,
            ApiEnvironment.DEV_3,
            ApiEnvironment.STAGE,
            -> environmentConfigStorage.getConfigSync().yieldModuleApiKeyDev
            ApiEnvironment.PROD -> environmentConfigStorage.getConfigSync().yieldModuleApiKey
        } ?: error("No tangem tech api config provided")
    }
}