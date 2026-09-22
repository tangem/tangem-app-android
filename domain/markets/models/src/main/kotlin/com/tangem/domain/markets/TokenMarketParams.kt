package com.tangem.domain.markets

import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.serialization.SerializedBigDecimal
import kotlinx.serialization.Serializable

@Serializable
data class TokenMarketParams(
    val id: CryptoCurrency.RawID,
    val name: String,
    val symbol: String,
    val tokenQuotes: Quotes,
    val imageUrl: String?,
    /**
     * The token is blocked in the user's region. Travels with the route instead of being refetched: the
     * token details endpoint does not carry the flag, only the list does.
     */
    val isRestrictedForCountry: Boolean = false,
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
        isRestrictedForCountry = isRestrictedForCountry,
        tokenQuotes = TokenMarketParams.Quotes(
            currentPrice = tokenQuotesShort.currentPrice,
            h24Percent = tokenQuotesShort.h24ChangePercent,
            weekPercent = tokenQuotesShort.weekChangePercent,
            monthPercent = tokenQuotesShort.monthChangePercent,
        ),
        imageUrl = imageUrlLarge,
    )
}