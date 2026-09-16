package com.tangem.domain.polymarket.model

sealed interface PredictionPriceError : PolymarketError {

    /** Transport failure (no connection / timeout) — poll on and keep the last known value. */
    data object Network : PredictionPriceError

    /** HTTP 404 — no orderbook exists for the token: the market is closed. Terminal for polling. */
    data object MarketClosed : PredictionPriceError

    /** HTTP 400 — the token id itself is refused. Terminal for polling. */
    data object InvalidAsset : PredictionPriceError

    data object EmptyBook : PredictionPriceError

    /** Anything else (5xx included) — callers treat it like [Network]. */
    data class Unknown(val httpCode: Int?, val detail: String?) : PredictionPriceError
}