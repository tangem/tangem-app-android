package com.tangem.domain.polymarket.model

import java.math.BigDecimal

/**
 * Recent live prices of a coin, oldest first.
 *
 * @property points about the last two minutes, one point a second while the stream is healthy
 */
data class PolymarketCryptoPriceSeries(
    val points: List<PolymarketCryptoPricePoint> = emptyList(),
    val status: Status = Status.WAITING,
) {

    enum class Status {
        WAITING,
        LIVE,

        /** No credentials, a refused subscription or a connection that was given up; [points] are whatever was seen */
        UNAVAILABLE,
    }
}

data class PolymarketCryptoPricePoint(
    val timestampMillis: Long,
    val price: BigDecimal,
)