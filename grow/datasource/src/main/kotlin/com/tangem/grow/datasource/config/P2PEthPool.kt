package com.tangem.grow.datasource.config

import com.tangem.core.remote.config.ApiConfig
import com.tangem.core.remote.config.ApiEnvironment
import com.tangem.core.remote.config.ApiEnvironmentConfig
import com.tangem.domain.staking.model.ethpool.P2PEthPoolStakingConfig
import com.tangem.grow.datasource.BuildConfig
import com.tangem.utils.ProviderSuspend

/**
 * P2P.org Ethereum Pooled Staking API configuration
 */
class P2PEthPool(
    private val growEnvironmentConfig: GrowEnvironmentConfig,
) : ApiConfig() {

    override val id: ApiConfig.ID get() = ID

    override val defaultEnvironment: ApiEnvironment = getInitialEnvironment()

    override val environmentConfigs: List<ApiEnvironmentConfig> = listOf(
        createProdEnvironment(),
        createTestEnvironment(),
        createMockEnvironment(),
    )

    private fun getInitialEnvironment(): ApiEnvironment {
        return when (BuildConfig.BUILD_TYPE) {
            MOCKED_BUILD_TYPE -> ApiEnvironment.MOCK
            else -> if (P2PEthPoolStakingConfig.USE_TESTNET) ApiEnvironment.DEV else ApiEnvironment.PROD
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
        put(
            key = "Authorization",
            value = ProviderSuspend {
                val keys = growEnvironmentConfig.p2pApiKey ?: error("No P2P api keys provided")
                val apiKey = if (P2PEthPoolStakingConfig.USE_TESTNET) keys.hoodi else keys.mainnet
                "Bearer $apiKey"
            },
        )
        put(key = "accept", value = ProviderSuspend { "application/json" })
        put(key = "Content-Type", value = ProviderSuspend { "application/json" })
    }

    companion object {
        const val KEY = "P2PEthPool"
        val ID = ApiConfig.ID(KEY)
    }
}