package com.tangem.domain.tokens.model

import com.tangem.domain.models.currency.CryptoCurrency
import java.math.BigDecimal

data class Amount(
    val currencySymbol: String,
    val value: BigDecimal? = null,
    val decimals: Int,
    val type: AmountType = AmountType.CoinType,
)

sealed class AmountType {
    object CoinType : AmountType()
    object ReserveType : AmountType()
    data class TokenType(val token: CryptoCurrency.Token) : AmountType()
    data class FiatType(val code: String) : AmountType()
}

/** Converts `BigDecimal` [cryptoCurrency] to [Amount] */
fun BigDecimal.convertToAmount(cryptoCurrency: CryptoCurrency) = Amount(
    currencySymbol = cryptoCurrency.symbol,
    value = this,
    decimals = cryptoCurrency.decimals,
    type = when (cryptoCurrency) {
        is CryptoCurrency.Coin -> AmountType.CoinType
        is CryptoCurrency.Token -> AmountType.TokenType(
            token = cryptoCurrency,
        )
    },
)