package com.tangem.domain.polymarket.model

sealed interface PolymarketClobIdentity {

    val address: String
    val credentials: PolymarketApiCredentials

    data class Owner(
        override val address: String,
        override val credentials: PolymarketApiCredentials,
    ) : PolymarketClobIdentity

    data class Session(
        override val address: String,
        override val credentials: PolymarketApiCredentials,
    ) : PolymarketClobIdentity
}