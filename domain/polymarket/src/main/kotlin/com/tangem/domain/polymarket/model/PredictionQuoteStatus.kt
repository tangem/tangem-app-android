package com.tangem.domain.polymarket.model

enum class PredictionQuoteStatus {
    FULL,
    PARTIAL,
    INSUFFICIENT_LIQUIDITY,
    BELOW_MIN_ORDER_SIZE,
    MARKET_CLOSED,
    ;

    val isPlaceable: Boolean get() = this == FULL || this == PARTIAL
}