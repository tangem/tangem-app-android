package com.tangem.datasource.api.common.config

import com.tangem.datasource.BuildConfig
import com.tangem.lib.auth.P2PEthPoolAuthProvider
import com.tangem.utils.ProviderSuspend

/**
 * P2P.org Ethereum Pooled Staking API configuration
 */
internal class P2PEthPool(
    private val p2pAuthProvider: P2PEthPoolAuthProvider,
) : ApiConfig() {

    override val defaultEnvironment: ApiEnvironment = getInitialEnvironment()

    override val environmentConfigs: List<ApiEnvironmentConfig> = listOf(
        createProdEnvironment(),
        createTestEnvironment(),
        createMockEnvironment(),
    )

    private fun getInitialEnvironment(): ApiEnvironment {
        return when (BuildConfig.BUILD_TYPE) {
            MOCKED_BUILD_TYPE -> ApiEnvironment.MOCK
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
            baseUrl = "https://api.p2p.org/",
            headers = createHeaders(),
        )
    }

    private fun createTestEnvironment(): ApiEnvironmentConfig {
        return ApiEnvironmentConfig(
            environment = ApiEnvironment.DEV,
            baseUrl = "https://api-test.p2p.org/",
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
        put(key = "Authorization", value = ProviderSuspend { "Bearer ${p2pAuthProvider.getApiKey()}" })
        put(key = "accept", value = ProviderSuspend { "application/json" })
        put(key = "Content-Type", value = ProviderSuspend { "application/json" })
    }
}