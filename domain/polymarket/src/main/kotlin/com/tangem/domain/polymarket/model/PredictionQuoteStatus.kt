package com.tangem.domain.polymarket.model

enum class PredictionQuoteStatus {
    FULL,
    PARTIAL,
    INSUFFICIENT_LIQUIDITY,
    BELOW_MIN_ORDER_SIZE,
    BELOW_MIN_ORDER_AMOUNT,
    MARKET_CLOSED,
    ;

    val isPlaceable: Boolean get() = this == FULL || this == PARTIAL

    val isPlaceableForSell: Boolean get() = isPlaceable || this == BELOW_MIN_ORDER_SIZE
}