package com.tangem.domain.markets

import java.math.BigDecimal

data class TokenQuotes(
    val currentPrice: BigDecimal,
    private val priceChanges: Map<PriceChangeInterval, BigDecimal>,
) {
    init {
        require(priceChanges.containsKey(PriceChangeInterval.H24))
        require(priceChanges.containsKey(PriceChangeInterval.WEEK))
        require(priceChanges.containsKey(PriceChangeInterval.MONTH))
    }

    fun h24Percent() = priceChanges[PriceChangeInterval.H24]!!
    fun weekPercent() = priceChanges[PriceChangeInterval.WEEK]!!
    fun monthPercent() = priceChanges[PriceChangeInterval.MONTH]!!
}