package com.tangem.domain.markets

import java.math.BigDecimal

data class TokenQuotes(
    val currentPrice: BigDecimal,
    val priceChanges: Map<PriceChangeInterval, BigDecimal>,
)