package com.tangem.domain.markets

import com.tangem.domain.core.serialization.SerializedBigDecimal
import com.tangem.domain.tokens.model.CryptoCurrency
import kotlinx.serialization.Serializable

@Serializable
data class TokenMarketParams(
    val id: CryptoCurrency.RawID,
    val name: String,
    val symbol: String,
    val tokenQuotes: Quotes,
    val imageUrl: String?,
) {

    @Serializable
    data class Quotes(
        val currentPrice: SerializedBigDecimal,
        val h24Percent: SerializedBigDecimal?,
        val weekPercent: SerializedBigDecimal?,
        val monthPercent: SerializedBigDecimal?,
    )
}

fun TokenMarket.toSerializableParam(): TokenMarketParams {
    return TokenMarketParams(
        id = id,
        name = name,
        symbol = symbol,
        tokenQuotes = TokenMarketParams.Quotes(
            currentPrice = quotes.fiatRate,
            h24Percent = quotes.h24ChangePercent,
            weekPercent = quotes.weekChangePercent,
            monthPercent = quotes.monthChangePercent,
        ),
        imageUrl = imageUrlLarge,
    )
}