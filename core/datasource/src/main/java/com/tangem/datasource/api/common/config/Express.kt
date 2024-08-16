package com.tangem.datasource.api.common.config

import com.tangem.datasource.BuildConfig
import com.tangem.datasource.config.ConfigManager
import com.tangem.datasource.utils.RequestHeader
import com.tangem.lib.auth.ExpressAuthProvider
import com.tangem.utils.Provider
import com.tangem.utils.version.AppVersionProvider

/**
 * Express [ApiConfig]
 *
 * @property configManager       config manager
 * @property expressAuthProvider express auth provider
 * @property appVersionProvider  app version provider
 */
internal class Express(
    private val configManager: ConfigManager,
    private val expressAuthProvider: ExpressAuthProvider,
    private val appVersionProvider: AppVersionProvider,
) : ApiConfig() {

    override val defaultEnvironment: ApiEnvironment = getInitialEnvironment()

    override val environmentConfigs: List<ApiEnvironmentConfig> = listOf(
        createDevEnvironment(),
        createStageEnvironment(),
        createProdEnvironment(),
    )

    private fun createDevEnvironment(): ApiEnvironmentConfig = ApiEnvironmentConfig(
        environment = ApiEnvironment.DEV,
        baseUrl = "[REDACTED_ENV_URL]",
        headers = createHeaders(isProd = false),
    )

    private fun createStageEnvironment(): ApiEnvironmentConfig = ApiEnvironmentConfig(
        environment = ApiEnvironment.STAGE,
        baseUrl = "[REDACTED_ENV_URL]",
        headers = createHeaders(isProd = false),
    )

    private fun createProdEnvironment(): ApiEnvironmentConfig = ApiEnvironmentConfig(
        environment = ApiEnvironment.PROD,
        baseUrl = "[REDACTED_ENV_URL]",
        headers = createHeaders(isProd = true),
    )

    private fun createHeaders(isProd: Boolean) = buildMap {
        put(key = "api-key", value = Provider { getApiKey(isProd) })
        put(key = "user-id", value = Provider(expressAuthProvider::getUserId))
        put(key = "session-id", value = Provider(expressAuthProvider::getSessionId))
        putAll(from = RequestHeader.AppVersionPlatformHeaders(appVersionProvider).values)
    }

    private fun getApiKey(isProd: Boolean): String {
        return if (isProd) {
            configManager.config.express
        } else {
            configManager.config.devExpress
        }
            ?.apiKey
            ?: error("No express config provided")
    }

    private companion object {

        fun getInitialEnvironment(): ApiEnvironment {
            return when (BuildConfig.BUILD_TYPE) {
                DEBUG_BUILD_TYPE -> ApiEnvironment.DEV
                INTERNAL_BUILD_TYPE,
                MOCKED_BUILD_TYPE,
                -> ApiEnvironment.STAGE
                EXTERNAL_BUILD_TYPE,
                RELEASE_BUILD_TYPE,
                -> ApiEnvironment.PROD
                else -> error("Unknown build type [${BuildConfig.BUILD_TYPE}]")
            }
        }
    }
}