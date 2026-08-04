package com.tangem.datasource.api.common.config

import com.tangem.core.remote.config.ApiConfig
import com.tangem.core.remote.config.ApiEnvironment
import com.tangem.core.remote.config.ApiEnvironmentConfig

class PolymarketClob : ApiConfig() {

    override val id: ApiConfig.ID get() = ID

    override val defaultEnvironment: ApiEnvironment = ApiEnvironment.PROD

    override val environmentConfigs = listOf(
        ApiEnvironmentConfig(
            environment = ApiEnvironment.PROD,
            baseUrl = "https://clob.polymarket.com/",
        ),
    )

    companion object {
        const val KEY = "PolymarketClob"
        val ID = ApiConfig.ID(KEY)
    }
}