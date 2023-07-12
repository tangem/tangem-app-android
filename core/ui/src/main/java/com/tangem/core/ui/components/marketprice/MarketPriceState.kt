package com.tangem.core.ui.components.marketprice

sealed interface MarketPriceState {

    val currencyName: String

    data class Loading(override val currencyName: String) : MarketPriceState

    data class Content(
        override val currencyName: String,
        val price: String,
        val priceChangeConfig: PriceChangeConfig,
    ) : MarketPriceState
}
