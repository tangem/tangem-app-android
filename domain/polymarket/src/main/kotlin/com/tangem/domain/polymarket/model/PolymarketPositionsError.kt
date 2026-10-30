package com.tangem.domain.polymarket.model

/** A failed portfolio read must not be presented as an empty holding. */
sealed interface PolymarketPositionsError : PolymarketError {

    data object Network : PolymarketPositionsError

    /** The list refused the cursor it was continued with; the only way on is a fresh first page. */
    data object StaleCursor : PolymarketPositionsError

    data class Unknown(val httpCode: Int?, val detail: String?) : PolymarketPositionsError
}