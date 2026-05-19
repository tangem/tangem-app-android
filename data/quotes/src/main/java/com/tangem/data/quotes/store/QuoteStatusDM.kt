package com.tangem.data.quotes.store

import com.squareup.moshi.JsonClass
import com.tangem.datasource.api.tangemTech.models.QuotesResponse

/**
 * Persisted form of [com.tangem.domain.models.quote.QuoteStatus] cache. Carries the fiat currency
 * the quotes are expressed in, so it can be restored on cold start.
 *
 * @property fiatCurrency fiat currency the [quotes] are expressed in; `null` for an empty default cache
 * @property quotes       map of currency id to its quote
 */
@JsonClass(generateAdapter = true)
internal data class QuoteStatusDM(
    val fiatCurrency: FiatCurrency?,
    val quotes: Map<String, QuotesResponse.Quote>,
) {

    @JsonClass(generateAdapter = true)
    internal data class FiatCurrency(
        val code: String,
        val symbol: String,
    )

    companion object {
        val Empty = QuoteStatusDM(fiatCurrency = null, quotes = emptyMap())
    }
}