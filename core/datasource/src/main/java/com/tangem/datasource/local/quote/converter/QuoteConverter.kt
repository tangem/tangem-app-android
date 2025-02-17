package com.tangem.datasource.local.quote.converter

import com.tangem.datasource.api.tangemTech.models.QuotesResponse
import com.tangem.domain.tokens.model.CryptoCurrency
import com.tangem.domain.tokens.model.Quote
import com.tangem.utils.converter.Converter
import com.tangem.utils.extensions.orZero

/**
 * Converter from [QuotesResponse.Quote] to [Quote.Value]
 *
 * @property isCached flag that determines whether the quote is a cache
 *
[REDACTED_AUTHOR]
 */
internal class QuoteConverter(private val isCached: Boolean) :
    Converter<Map.Entry<String, QuotesResponse.Quote>, Quote.Value> {

    override fun convert(value: Map.Entry<String, QuotesResponse.Quote>): Quote.Value {
        val (currencyId, quote) = value

        return Quote.Value(
            rawCurrencyId = CryptoCurrency.RawID(currencyId),
            fiatRate = quote.price.orZero(),
            priceChange = quote.priceChange24h.orZero().movePointLeft(2),
            isCached = isCached,
        )
    }
}