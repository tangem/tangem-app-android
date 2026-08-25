package com.tangem.domain.polymarket.model

/**
 * How much of a requested order the book can actually fill, as reported by the quote endpoint.
 *
 * [PARTIAL] and [INSUFFICIENT_LIQUIDITY] are not failures: the order still goes through, it just fills for
 * less than asked. Only [BELOW_MIN_ORDER_SIZE] and [MARKET_CLOSED] make it unplaceable.
 */
enum class PredictionQuoteStatus {
    FULL,
    PARTIAL,
    INSUFFICIENT_LIQUIDITY,
    BELOW_MIN_ORDER_SIZE,
    MARKET_CLOSED,
}