package com.tangem.core.ui.components.marketprice

import androidx.compose.runtime.Immutable

@Immutable
sealed interface MarketPriceBlockState {

    val currencyName: String

    data class Loading(override val currencyName: String) : MarketPriceBlockState

    data class Content(
        override val currencyName: String,
        val price: String,
        val priceChangeConfig: PriceChangeConfig,
    ) : MarketPriceBlockState
}
