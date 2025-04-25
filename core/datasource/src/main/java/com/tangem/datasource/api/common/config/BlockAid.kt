package com.tangem.datasource.api.common.config

import com.tangem.datasource.local.config.environment.EnvironmentConfigStorage
import com.tangem.utils.ProviderSuspend

internal class BlockAid(
    private val environmentConfigStorage: EnvironmentConfigStorage,
) : ApiConfig() {

    override val defaultEnvironment: ApiEnvironment = ApiEnvironment.PROD

    override val environmentConfigs = listOf(
        createProdEnvironment(),
    )

    private fun createProdEnvironment(): ApiEnvironmentConfig = ApiEnvironmentConfig(
        environment = ApiEnvironment.PROD,
        baseUrl = "https://api.blockaid.io/v0/",
        headers = buildMap {
            environmentConfigStorage.getConfigSync().blockAidApiKey?.let { apiKey ->
                put("X-API-KEY", ProviderSuspend { apiKey })
            }
            put("accept", ProviderSuspend { "application/json" })
            put("content-type", ProviderSuspend { "application/json" })
        },
    )
}