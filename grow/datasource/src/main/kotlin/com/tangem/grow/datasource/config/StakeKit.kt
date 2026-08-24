package com.tangem.grow.datasource.config

import com.tangem.core.remote.config.ApiConfig
import com.tangem.core.remote.config.ApiEnvironment
import com.tangem.core.remote.config.ApiEnvironmentConfig
import com.tangem.grow.datasource.BuildConfig
import com.tangem.utils.ProviderSuspend

/**
 * StakeKit [ApiConfig]
 */
class StakeKit(
    private val growEnvironmentConfig: GrowEnvironmentConfig,
) : ApiConfig() {

    override val id: ApiConfig.ID get() = ID

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
        put(
            key = "X-API-KEY",
            value = ProviderSuspend {
                growEnvironmentConfig.stakeKitApiKey ?: error("No StakeKit api key provided")
            },
        )
        put(key = "accept", value = ProviderSuspend { "application/json" })
    }

    companion object {
        const val KEY = "StakeKit"
        val ID = ApiConfig.ID(KEY)
    }
}