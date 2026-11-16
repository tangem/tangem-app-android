package com.tangem.domain.polymarket.model

sealed interface PolymarketPriceHistoryError : PolymarketError {
    data object Network : PolymarketPriceHistoryError
    data object InvalidToken : PolymarketPriceHistoryError
    data class Unknown(val httpCode: Int?, val detail: String?) : PolymarketPriceHistoryError
}