package com.tangem.features.markets.details.api

import com.tangem.domain.core.serialization.SerializedBigDecimal
import com.tangem.domain.markets.TokenMarket
import kotlinx.serialization.Serializable

@Serializable
data class TokenMarketSerializable(
    val id: String,
    val name: String,
    val symbol: String,
    val marketCap: SerializedBigDecimal?,
    val tokenQuotes: Quotes,
    val imageUrl: String,
) {

    @Serializable
    data class Quotes(
        val currentPrice: SerializedBigDecimal,
        val h24Percent: SerializedBigDecimal,
        val weekPercent: SerializedBigDecimal,
        val monthPercent: SerializedBigDecimal,
    )
}

fun TokenMarket.toSerializable(): TokenMarketSerializable {
    return TokenMarketSerializable(
        id = id,
        name = name,
        symbol = symbol,
        marketCap = marketCap,
        tokenQuotes = TokenMarketSerializable.Quotes(
            currentPrice = tokenQuotes.currentPrice,
            h24Percent = tokenQuotes.h24Percent(),
            weekPercent = tokenQuotes.weekPercent(),
            monthPercent = tokenQuotes.monthPercent(),
        ),
        imageUrl = imageUrlLarge,
    )
}