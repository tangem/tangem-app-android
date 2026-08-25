package com.tangem.domain.polymarket.model

/**
 * How much of a requested order the book can actually fill, as reported by the quote endpoint.
 *
 * Only [FULL] and [PARTIAL] can be placed, and no figure may be read from the rest: the exchange zeroes them
 * for [INSUFFICIENT_LIQUIDITY] and [MARKET_CLOSED], while a status this build does not know is mapped to
 * [MARKET_CLOSED] carrying whatever numbers it arrived with. [BELOW_MIN_ORDER_SIZE] is the exception the
 * contract makes — it blocks the order but keeps the figures, so the user can see what was attempted.
 */
enum class PredictionQuoteStatus {
    FULL,
    PARTIAL,
    INSUFFICIENT_LIQUIDITY,
    BELOW_MIN_ORDER_SIZE,
    MARKET_CLOSED,
    ;

    /** Whether an order may be built from a quote in this state at all. */
    val isPlaceable: Boolean get() = this == FULL || this == PARTIAL
}