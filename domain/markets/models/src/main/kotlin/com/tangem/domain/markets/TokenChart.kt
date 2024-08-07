package com.tangem.domain.markets

import java.math.BigDecimal

data class TokenChart(
    val interval: PriceChangeInterval,
    val priceY: List<BigDecimal>,
    val timeStamp: List<Long>,
) {
    init {
        require(priceY.size == timeStamp.size)
    }
}
