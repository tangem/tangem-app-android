package com.tangem.domain.polymarket.model

sealed interface PredictionOrderStateError : PolymarketError {

    /** No connection: the read never reached the book. */
    data object Network : PredictionOrderStateError

    /** The read reached the book but no answer came back in time; the order itself is unaffected. */
    data object Timeout : PredictionOrderStateError

    /** HTTP 404 — the book does not know the id. */
    data object NotFound : PredictionOrderStateError

    /** HTTP 401 — the L2 credentials are refused. */
    data class Unauthorized(val detail: String?) : PredictionOrderStateError

    data class Unknown(val httpCode: Int?, val detail: String?) : PredictionOrderStateError
}