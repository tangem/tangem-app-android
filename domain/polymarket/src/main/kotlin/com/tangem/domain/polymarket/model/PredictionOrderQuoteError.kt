package com.tangem.domain.polymarket.model

/** Why an order could not be quoted. */
sealed interface PredictionOrderQuoteError : PolymarketError {

    /** Transport failure (no connection / timeout). */
    data object Network : PredictionOrderQuoteError

    /** Anything else. */
    data class Unknown(val httpCode: Int?, val detail: String?) : PredictionOrderQuoteError
}