package com.tangem.domain.polymarket.model

/**
 * Direction of a prediction order.
 *
 * [BUY] acquires outcome shares for collateral, [SELL] returns them. The CLOB prices the two sides from
 * opposite ends of the book, so the side is part of every quote request, not a presentation detail.
 */
enum class PredictionOrderSide {
    BUY,
    SELL,
}