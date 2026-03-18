package com.tangem.datasource.api.common.config

import com.tangem.datasource.local.config.environment.EnvironmentConfig
import com.tangem.utils.ProviderSuspend

internal class BlockAid(
    private val environmentConfig: EnvironmentConfig,
) : ApiConfig() {

    override val defaultEnvironment: ApiEnvironment = ApiEnvironment.PROD

    override val environmentConfigs = listOf(
        createProdEnvironment(),
    )

    private fun createProdEnvironment(): ApiEnvironmentConfig = ApiEnvironmentConfig(
        environment = ApiEnvironment.PROD,
        baseUrl = "https://api.blockaid.io/v0/",
        headers = buildMap {
            put(
                key = "X-API-KEY",
                value = ProviderSuspend {
                    requireNotNull(environmentConfig.blockAidApiKey)
                },
            )
            put("accept", ProviderSuspend { "application/json" })
            put("content-type", ProviderSuspend { "application/json" })
        },
    )
}