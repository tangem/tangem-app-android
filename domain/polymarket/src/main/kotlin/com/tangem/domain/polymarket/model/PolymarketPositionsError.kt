package com.tangem.domain.polymarket.model

/** A failed portfolio read must not be presented as an empty holding. */
sealed interface PolymarketPositionsError : PolymarketError {

    data object Network : PolymarketPositionsError

    data class Unknown(val httpCode: Int?, val detail: String?) : PolymarketPositionsError
}