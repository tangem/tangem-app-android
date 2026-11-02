package com.tangem.domain.polymarket.model

import java.time.Instant

data class PolymarketPriceHistoryQuery(val window: Window, val fidelityMinutes: Int) {

    init {
        require(fidelityMinutes > 0)
    }

    sealed interface Window {
        data class Interval(val interval: PolymarketPriceInterval) : Window

        data class Range(val start: Instant, val end: Instant? = null) : Window {
            init {
                require(end == null || end > start)
            }
        }
    }
}