package com.tangem.domain.polymarket.model

/** Why a prediction event could not be served. */
sealed interface PolymarketEventError : PolymarketError {

    /** The event is unknown to the BFF — resolved and pruned, or a stale link (HTTP 404). */
    data object NotFound : PolymarketEventError

    /** Transport failure (no connection / timeout). */
    data object Network : PolymarketEventError

    /** Anything else. */
    data class Unknown(val httpCode: Int?, val detail: String?) : PolymarketEventError
}