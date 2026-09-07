package com.tangem.domain.polymarket.model

import java.math.BigDecimal

/**
 * A placed order as the book reports it. Sizes are in shares; [price] is the order's own price, which the
 * flow treats as the execution price of whatever matched.
 */
data class PredictionOrderState(
    val status: PredictionOrderStatus,
    val price: BigDecimal?,
    val originalSize: BigDecimal?,
    val sizeMatched: BigDecimal?,
)

sealed interface PredictionOrderStatus {

    /** Matched in full or in part; the fill is [PredictionOrderState.sizeMatched]. */
    data object Matched : PredictionOrderStatus

    /** Anything the book reports other than a match, kept as it came for the log. */
    data class Other(val raw: String?) : PredictionOrderStatus
}