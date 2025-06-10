package com.tangem.datasource.local.quote.converter

import com.tangem.datasource.api.tangemTech.models.QuotesResponse
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.quote.QuoteStatus
import com.tangem.utils.converter.Converter
import com.tangem.utils.extensions.orZero

/**
 * Converter from [QuotesResponse.Quote] to [QuoteStatus]
 *
 * @property source status source
 *
[REDACTED_AUTHOR]
 */
class QuoteStatusConverter(
    private val source: StatusSource,
) : Converter<Map.Entry<String, QuotesResponse.Quote>, QuoteStatus> {

    /**
     * Secondary constructor
     *
     * @param isCached flag that determines whether the quote is a cache
     */
    constructor(isCached: Boolean) : this(
        source = if (isCached) StatusSource.CACHE else StatusSource.ACTUAL,
    )

    override fun convert(value: Map.Entry<String, QuotesResponse.Quote>): QuoteStatus {
        val (currencyId, quote) = value

        return QuoteStatus(
            rawCurrencyId = CryptoCurrency.RawID(currencyId),
            value = QuoteStatus.Data(
                source = source,
                fiatRate = quote.price.orZero(),
                priceChange = quote.priceChange24h.orZero().movePointLeft(2),
            ),
        )
    }
}