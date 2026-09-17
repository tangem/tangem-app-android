package com.tangem.domain.polymarket.model

/** Shared classification for position cards and local filters. */
enum class PolymarketPositionStatus { ACTIVE, WON, LOST, DISPUTED, PENDING, SPLIT, CLOSED, UNKNOWN }

/** Market resolution, independent of the held outcome, scheduled end date and claimability. */
enum class PolymarketResolutionStatus {
    OPEN,
    PENDING_RESOLUTION,
    PROPOSED,
    DISPUTED,
    RESOLVED,
    UNKNOWN,
}

/** Settlement outcome, not the sign of the user's profit or loss. */
enum class PolymarketPositionResult {
    WON,
    LOST,
    SPLIT,
    UNKNOWN,
}

/** How the holding ended. CLOSED means sold out; the other known results mean claimed. */
enum class PolymarketClosedPositionStatus {
    CLOSED,
    WON,
    LOST,
    SPLIT,
    UNKNOWN,
}