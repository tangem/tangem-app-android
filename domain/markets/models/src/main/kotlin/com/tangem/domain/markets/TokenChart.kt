package com.tangem.domain.markets

import java.math.BigDecimal

data class TokenChart(
    val interval: PriceChangeInterval,
    val priceY: List<BigDecimal>,
    val timeStamps: List<Long>,
) {
    init {
        require(priceY.size == timeStamps.size)

        // temporary fix for the issue with generic collection serialization adapter
        timeStamps.forEach {
            it.inc()
        }
        priceY.forEach {
            it.inc()
        }
    }
}
