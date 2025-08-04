package com.tangem.datasource.api.common.config

import com.tangem.datasource.BuildConfig
import com.tangem.datasource.local.config.environment.EnvironmentConfigStorage
import com.tangem.datasource.utils.RequestHeader
import com.tangem.lib.auth.ExpressAuthProvider
import com.tangem.utils.ProviderSuspend
import com.tangem.utils.info.AppInfoProvider
import com.tangem.utils.version.AppVersionProvider

/**
 * Express [ApiConfig]
 *
 * @property environmentConfigStorage  environment config storage
 * @property expressAuthProvider       express auth provider
 * @property appVersionProvider        app version provider
 * @property appInfoProvider           app info provider
 */
internal class Express(
    private val environmentConfigStorage: EnvironmentConfigStorage,
    private val expressAuthProvider: ExpressAuthProvider,
    private val appVersionProvider: AppVersionProvider,
    private val appInfoProvider: AppInfoProvider,
) : ApiConfig() {

    override val defaultEnvironment: ApiEnvironment = getInitialEnvironment()

    override val environmentConfigs: List<ApiEnvironmentConfig> = listOf(
        createDevEnvironment(),
        createDev2Environment(),
        createStageEnvironment(),
        createMockedEnvironment(),
        createProdEnvironment(),
    )

    private fun getInitialEnvironment(): ApiEnvironment {
        return when (BuildConfig.BUILD_TYPE) {
            DEBUG_BUILD_TYPE,
            -> ApiEnvironment.DEV
            INTERNAL_BUILD_TYPE,
            -> ApiEnvironment.STAGE
            MOCKED_BUILD_TYPE,
            -> ApiEnvironment.MOCK
            EXTERNAL_BUILD_TYPE,
            RELEASE_BUILD_TYPE,
            -> ApiEnvironment.PROD
            else -> error("Unknown build type [${BuildConfig.BUILD_TYPE}]")
        }
    }

    private fun createDevEnvironment(): ApiEnvironmentConfig = ApiEnvironmentConfig(
        environment = ApiEnvironment.DEV,
        baseUrl = "[REDACTED_ENV_URL]",
        headers = createHeaders(isProd = false),
    )

    private fun createDev2Environment(): ApiEnvironmentConfig = ApiEnvironmentConfig(
        environment = ApiEnvironment.DEV_2,
        baseUrl = "[REDACTED_ENV_URL]",
        headers = createHeaders(isProd = false),
    )

    private fun createStageEnvironment(): ApiEnvironmentConfig = ApiEnvironmentConfig(
        environment = ApiEnvironment.STAGE,
        baseUrl = "[REDACTED_ENV_URL]",
        headers = createHeaders(isProd = false),
    )

    private fun createMockedEnvironment(): ApiEnvironmentConfig = ApiEnvironmentConfig(
        environment = ApiEnvironment.MOCK,
        baseUrl = "[REDACTED_ENV_URL]",
        headers = createHeaders(isProd = false),
    )

    private fun createProdEnvironment(): ApiEnvironmentConfig = ApiEnvironmentConfig(
        environment = ApiEnvironment.PROD,
        baseUrl = "[REDACTED_ENV_URL]",
        headers = createHeaders(isProd = true),
    )

    private fun createHeaders(isProd: Boolean) = buildMap {
        put(key = "api-key", value = ProviderSuspend { getApiKey(isProd) })
        put(key = "session-id", value = ProviderSuspend(expressAuthProvider::getSessionId))
        putAll(from = RequestHeader.AppVersionPlatformHeaders(appVersionProvider, appInfoProvider).values)
    }

    private fun getApiKey(isProd: Boolean): String {
        return if (isProd) {
            environmentConfigStorage.getConfigSync().express
        } else {
            environmentConfigStorage.getConfigSync().devExpress
        }
            ?.apiKey
            ?: error("No express config provided")
    }
}