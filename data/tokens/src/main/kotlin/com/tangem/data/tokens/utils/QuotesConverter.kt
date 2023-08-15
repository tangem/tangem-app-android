package com.tangem.data.tokens.utils

import com.tangem.datasource.local.quote.model.StoredQuote
import com.tangem.domain.tokens.models.Quote
import com.tangem.utils.converter.Converter

internal class QuotesConverter : Converter<StoredQuote, Quote> {

    override fun convert(value: StoredQuote): Quote {
        val (rawCurrencyId, responseQuote) = value

        return Quote(
            rawCurrencyId = rawCurrencyId,
            fiatRate = responseQuote.price,
            priceChange = responseQuote.priceChange.movePointLeft(2),
        )
    }
}