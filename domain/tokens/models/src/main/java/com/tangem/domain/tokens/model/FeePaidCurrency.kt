package com.tangem.domain.tokens.model

import com.tangem.domain.models.currency.CryptoCurrency
import java.math.BigDecimal

sealed class FeePaidCurrency {
    data object Coin : FeePaidCurrency()
    data object SameCurrency : FeePaidCurrency()
    data class FeeResource(val currency: String) : FeePaidCurrency()
    data class Token(
        val tokenId: CryptoCurrency.ID,
        val name: String,
        val symbol: String,
        val contractAddress: String,
        val balance: BigDecimal,
    ) : FeePaidCurrency()
}