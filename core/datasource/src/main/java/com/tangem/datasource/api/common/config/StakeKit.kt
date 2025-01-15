package com.tangem.datasource.api.common.config

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

    override val defaultEnvironment: ApiEnvironment = ApiEnvironment.PROD

    override val environmentConfigs: List<ApiEnvironmentConfig> = listOf(
        createProdEnvironment(),
    )

    private fun createProdEnvironment(): ApiEnvironmentConfig {
        return ApiEnvironmentConfig(
            environment = ApiEnvironment.PROD,
            baseUrl = "https://api.stakek.it/v1/",
            headers = mapOf(
                "X-API-KEY" to ProviderSuspend(stakeKitAuthProvider::getApiKey),
                "accept" to ProviderSuspend { "application/json" },
            ),
        )
    }
}