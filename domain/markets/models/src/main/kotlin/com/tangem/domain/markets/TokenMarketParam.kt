package com.tangem.domain.markets

import com.tangem.domain.core.serialization.SerializedBigDecimal
import kotlinx.serialization.Serializable

@Serializable
data class TokenMarketParam(
    val id: String,
    val name: String,
    val symbol: String,
    val tokenQuotes: Quotes,
    val imageUrl: String?,
) {

    @Serializable
    data class Quotes(
        val currentPrice: SerializedBigDecimal,
        val h24Percent: SerializedBigDecimal,
        val weekPercent: SerializedBigDecimal?,
        val monthPercent: SerializedBigDecimal?,
    )
}

fun TokenMarket.toSerializableParam(): TokenMarketParam {
    return TokenMarketParam(
        id = id,
        name = name,
        symbol = symbol,
        tokenQuotes = TokenMarketParam.Quotes(
            currentPrice = tokenQuotesShort.currentPrice,
            h24Percent = tokenQuotesShort.h24ChangePercent,
            weekPercent = tokenQuotesShort.weekChangePercent,
            monthPercent = tokenQuotesShort.monthChangePercent,
        ),
        imageUrl = imageUrlLarge,
    )
}
