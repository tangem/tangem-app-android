package com.tangem.domain.polymarket.model

/**
 * Lifecycle status of a [PolymarketEvent] or a [PolymarketMarket] (BFF `status`).
 */
enum class PolymarketStatus {

    /** Open for trading */
    ACTIVE,

    /** Trading is over, the outcome is resolved */
    CLOSED,

    /** Closed and moved out of the feed */
    ARCHIVED,

    /** Reported by the BFF, but not known to the app */
    UNKNOWN,
}