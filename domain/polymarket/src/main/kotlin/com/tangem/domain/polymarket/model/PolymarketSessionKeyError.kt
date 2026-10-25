package com.tangem.domain.polymarket.model

sealed interface PolymarketSessionKeyError : PolymarketError {

    data object KeyMissing : PolymarketSessionKeyError

    data object InvalidDigest : PolymarketSessionKeyError

    data class Failure(val cause: Throwable) : PolymarketSessionKeyError
}