package com.tangem.data.tokens.utils

import com.tangem.datasource.local.quote.model.StoredQuote
import com.tangem.domain.tokens.model.Quote
import com.tangem.utils.converter.Converter
import java.math.BigDecimal

internal class QuotesConverter : Converter<StoredQuote, Quote> {

    override fun convert(value: StoredQuote): Quote {
        val (rawCurrencyId, responseQuote) = value

        return Quote(
            rawCurrencyId = rawCurrencyId,
            fiatRate = responseQuote.price ?: BigDecimal.ZERO,
            priceChange = (responseQuote.priceChange24h ?: BigDecimal.ZERO).movePointLeft(2),
        )
    }
}
