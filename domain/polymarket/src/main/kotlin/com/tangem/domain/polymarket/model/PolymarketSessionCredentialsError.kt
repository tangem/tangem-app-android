package com.tangem.domain.polymarket.model

sealed interface PolymarketSessionCredentialsError : PolymarketError {

    data class Key(val cause: PolymarketSessionKeyError) : PolymarketSessionCredentialsError

    data class Auth(val cause: PolymarketAuthError) : PolymarketSessionCredentialsError
}