package com.tangem.domain.tokens.model

import java.math.BigDecimal

sealed class FeePaidCurrency {
    object Coin : FeePaidCurrency()
    object SameCurrency : FeePaidCurrency()
    data class Token(
        val tokenId: CryptoCurrency.ID,
        val name: String,
        val symbol: String,
        val contractAddress: String,
        val balance: BigDecimal,
    ) : FeePaidCurrency()
}