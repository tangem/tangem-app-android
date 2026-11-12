package com.tangem.domain.polymarket.model

import java.math.BigDecimal

data class PolymarketLiveQuote(
    val assetId: String,
    val bestBid: BigDecimal? = null,
    val bestAsk: BigDecimal? = null,
    val lastTradePrice: BigDecimal? = null,
    val updatedAtMillis: Long? = null,
    val status: Status = Status.WAITING,
) {

    /** Only LIVE overrides REST. A LIVE quote with a null ask is an empty book, not a fallback request. */
    enum class Status {
        WAITING,
        LIVE,
        STALE,
    }
}