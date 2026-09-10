package com.tangem.domain.polymarket.model

sealed interface PolymarketBridgeError : PolymarketError {

    data object Network : PolymarketBridgeError

    data object Unavailable : PolymarketBridgeError

    data class Rejected(val detail: String?) : PolymarketBridgeError
}