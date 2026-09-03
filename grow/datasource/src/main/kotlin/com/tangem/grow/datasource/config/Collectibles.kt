package com.tangem.grow.datasource.config

import com.tangem.core.remote.config.ApiConfig
import com.tangem.core.remote.config.ApiEnvironment
import com.tangem.core.remote.config.ApiEnvironmentConfig
import com.tangem.core.remote.header.RequestHeader
import com.tangem.grow.datasource.BuildConfig
import com.tangem.grow.datasource.collectibles.CollectiblesAuthProvider
import com.tangem.utils.ProviderSuspend
import com.tangem.utils.info.AppInfoProvider

/**
 * Collectibles (Collector Crypt) [ApiConfig].
 *
 * Carries no api key: the endpoints are session-protected per method via
 * `com.tangem.lib.auth.api.RequiresSessionAuth`.
 */
class Collectibles(
    private val collectiblesAuthProvider: CollectiblesAuthProvider,
    private val appInfoProvider: AppInfoProvider,
) : ApiConfig() {

    override val id: ID get() = ID

    override val defaultEnvironment: ApiEnvironment = getInitialEnvironment()

    override val environmentConfigs = listOf(
        createDevEnvironment(),
        createMockedEnvironment(),
        createProdEnvironment(),
    )

    private fun getInitialEnvironment(): ApiEnvironment {
        return when (BuildConfig.BUILD_TYPE) {
            MOCKED_BUILD_TYPE,
            -> ApiEnvironment.MOCK
            DEBUG_BUILD_TYPE,
            INTERNAL_BUILD_TYPE,
            -> ApiEnvironment.DEV
            EXTERNAL_BUILD_TYPE,
            RELEASE_BUILD_TYPE,
            -> ApiEnvironment.PROD
            else -> error("Unknown build type [${BuildConfig.BUILD_TYPE}]")
        }
    }

    private fun createDevEnvironment(): ApiEnvironmentConfig = ApiEnvironmentConfig(
        environment = ApiEnvironment.DEV,
        baseUrl = "[REDACTED_ENV_URL]",
        headers = createHeaders(),
    )

    private fun createMockedEnvironment(): ApiEnvironmentConfig = ApiEnvironmentConfig(
        environment = ApiEnvironment.MOCK,
        baseUrl = "[REDACTED_ENV_URL]",
        headers = createHeaders(),
    )

    private fun createProdEnvironment(): ApiEnvironmentConfig = ApiEnvironmentConfig(
        environment = ApiEnvironment.PROD,
        baseUrl = "[REDACTED_ENV_URL]", // TODO Collectibles replace with actual prod env
        headers = createHeaders(),
    )

    private fun createHeaders() = buildMap {
        put(key = "session-id", value = ProviderSuspend(collectiblesAuthProvider::getSessionId))
        putAll(from = RequestHeader.AppVersionPlatformHeaders(appInfoProvider).values)
    }

    companion object {
        const val KEY = "Collectibles"
        val ID = ID(KEY)
    }
}