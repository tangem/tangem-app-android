package com.tangem.grow.datasource.config

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
        const val MARKET_SOCKET_URL = "wss://ws-subscriptions-clob.polymarket.com/ws/market"
        const val LIVE_DATA_SOCKET_URL = "wss://ws-live-v2.polymarket.com/ws"
        val ID = ApiConfig.ID(KEY)
    }
}