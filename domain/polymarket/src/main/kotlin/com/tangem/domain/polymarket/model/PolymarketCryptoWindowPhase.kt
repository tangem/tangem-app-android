package com.tangem.domain.polymarket.model

import java.time.Instant

/** Where a crypto window stands at a moment of the backend's clock. */
enum class PolymarketCryptoWindowPhase {
    UPCOMING,
    LIVE,
    ENDED,
    ;

    companion object {

        /** A window is live from its [start] up to, but not at, its [end]. */
        fun at(start: Instant, end: Instant, nowMillis: Long): PolymarketCryptoWindowPhase = when {
            nowMillis < start.toEpochMilli() -> UPCOMING
            nowMillis >= end.toEpochMilli() -> ENDED
            else -> LIVE
        }
    }
}