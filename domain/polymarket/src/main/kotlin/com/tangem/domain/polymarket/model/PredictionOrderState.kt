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

    data class Rejected(val raw: String) : PredictionOrderStatus

    /** Still open on the book (`LIVE`) or a status this app does not know; the order may yet match. */
    data class Other(val raw: String?) : PredictionOrderStatus
}