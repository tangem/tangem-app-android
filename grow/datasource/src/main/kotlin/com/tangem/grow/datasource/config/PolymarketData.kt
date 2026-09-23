package com.tangem.grow.datasource.config

import com.tangem.core.remote.config.ApiConfig
import com.tangem.core.remote.config.ApiEnvironment
import com.tangem.core.remote.config.ApiEnvironmentConfig

class PolymarketData : ApiConfig() {

    override val id: ApiConfig.ID get() = ID

    override val defaultEnvironment: ApiEnvironment = ApiEnvironment.PROD

    override val environmentConfigs = listOf(
        ApiEnvironmentConfig(
            environment = ApiEnvironment.PROD,
            baseUrl = "https://data-api.polymarket.com/",
        ),
    )

    companion object {
        const val KEY = "PolymarketData"
        val ID = ApiConfig.ID(KEY)
    }
}