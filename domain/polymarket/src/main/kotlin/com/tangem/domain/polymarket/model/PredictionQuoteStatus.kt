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

    /**
     * @param isFullClose the sale covers the whole remaining position. Such a sale is placed even below the
     *  exchange's minimum order size, which is otherwise a dead end: a position smaller than that minimum
     *  could never be closed at all.
     */
    fun isPlaceableForSell(isFullClose: Boolean): Boolean = isPlaceable || isFullClose && this == BELOW_MIN_ORDER_SIZE
}