package com.tangem.data.tokens.utils

import com.tangem.datasource.local.quote.model.StoredQuote
import com.tangem.domain.tokens.model.CryptoCurrency
import com.tangem.domain.tokens.model.Quote
import com.tangem.utils.converter.Converter
import java.math.BigDecimal

typealias QuotesConverterValue = Pair<Set<CryptoCurrency.RawID>, Set<StoredQuote>>

internal class QuotesConverter : Converter<QuotesConverterValue, Set<Quote>> {

    override fun convert(value: QuotesConverterValue): Set<Quote> {
        val (setOfCurrencyId, setOfStoredQuote) = value
        return setOfCurrencyId.mapTo(hashSetOf()) { id ->
            setOfStoredQuote.find { id.value == it.rawCurrencyId }
                ?.let(::convertExistStoredQuote)
                ?: Quote.Empty(id)
        }
    }

    private fun convertExistStoredQuote(storedQuote: StoredQuote): Quote {
        val (rawCurrencyId, responseQuote) = storedQuote

        return Quote.Value(
            rawCurrencyId = CryptoCurrency.RawID(rawCurrencyId),
            fiatRate = responseQuote.price ?: BigDecimal.ZERO,
            priceChange = (responseQuote.priceChange24h ?: BigDecimal.ZERO).movePointLeft(2),
        )
    }
}