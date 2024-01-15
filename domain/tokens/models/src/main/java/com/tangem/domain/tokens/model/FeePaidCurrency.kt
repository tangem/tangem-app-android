package com.tangem.domain.tokens.model

sealed class FeePaidCurrency {
    object Coin : FeePaidCurrency()
    object SameCurrency : FeePaidCurrency()
    data class Token(
        val name: String,
        val symbol: String,
        val contractAddress: String,
        val decimals: Int,
        val id: String? = null,
    ) : FeePaidCurrency()
}
