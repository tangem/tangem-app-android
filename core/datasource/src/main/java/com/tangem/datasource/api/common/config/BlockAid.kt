package com.tangem.datasource.api.common.config

import com.tangem.core.remote.config.ApiConfig
import com.tangem.core.remote.config.ApiEnvironment
import com.tangem.core.remote.config.ApiEnvironmentConfig

import com.tangem.datasource.local.config.environment.EnvironmentConfig
import com.tangem.utils.ProviderSuspend

class BlockAid(
    private val environmentConfig: EnvironmentConfig,
) : ApiConfig() {

    override val id: ApiConfig.ID get() = ID

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

    companion object {
        const val KEY = "BlockAid"
        val ID = ApiConfig.ID(KEY)
    }
}