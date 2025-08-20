package com.tangem.datasource.api.common.config

import com.tangem.datasource.BuildConfig
import com.tangem.lib.auth.StakeKitAuthProvider
import com.tangem.utils.ProviderSuspend

/**
 * StakeKit [ApiConfig]
 *
 * @property stakeKitAuthProvider StakeKit auth provider
 *
[REDACTED_AUTHOR]
 */
internal class StakeKit(
    private val stakeKitAuthProvider: StakeKitAuthProvider,
) : ApiConfig() {

    override val defaultEnvironment: ApiEnvironment = getInitialEnvironment()

    override val environmentConfigs: List<ApiEnvironmentConfig> = listOf(
        createProdEnvironment(),
        createMockEnvironment(),
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

    private fun createProdEnvironment(): ApiEnvironmentConfig {
        return ApiEnvironmentConfig(
            environment = ApiEnvironment.PROD,
            baseUrl = "https://api.stakek.it/v1/",
            headers = createHeaders(),
        )
    }

    private fun createMockEnvironment(): ApiEnvironmentConfig {
        return ApiEnvironmentConfig(
            environment = ApiEnvironment.MOCK,
            baseUrl = "[REDACTED_ENV_URL]",
            headers = createHeaders(),
        )
    }

    private fun createHeaders() = buildMap {
        put(key = "X-API-KEY", value = ProviderSuspend(stakeKitAuthProvider::getApiKey))
        put(key = "accept", value = ProviderSuspend { "application/json" })
    }
}