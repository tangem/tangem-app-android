package com.tangem.core.ui.components.marketprice

import androidx.compose.runtime.Immutable

@Immutable
sealed interface MarketPriceBlockState {

    val currencySymbol: String

    data class Error(override val currencySymbol: String) : MarketPriceBlockState

    data class Loading(override val currencySymbol: String) : MarketPriceBlockState

    data class Content(
        override val currencySymbol: String,
        val price: String,
        val priceChangeConfig: PriceChangeState.Content,
    ) : MarketPriceBlockState
}