package com.tangem.domain.markets

sealed class TokenMarketUpdateRequest {

    data class UpdateQuotes(
        val currencyId: String,
    ) : TokenMarketUpdateRequest()

    data class UpdateChart(
        val interval: TokenMarketListConfig.Interval,
        val currency: String,
    ) : TokenMarketUpdateRequest()
}