package com.tangem.common.test.data.quote

import com.tangem.datasource.api.tangemTech.models.QuotesResponse
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.FiatCurrency
import com.tangem.domain.models.quote.QuoteStatus
import com.tangem.utils.extensions.orZero

fun QuotesResponse.Quote.toDomain(
    rawCurrencyId: String,
    source: StatusSource = StatusSource.ACTUAL,
    fiatCurrency: FiatCurrency = FiatCurrency.Default,
): QuoteStatus {
    return QuoteStatus(
        rawCurrencyId = CryptoCurrency.RawID(rawCurrencyId),
        value = QuoteStatus.Data(
            source = source,
            fiatCurrency = fiatCurrency,
            fiatRate = price.orZero(),
            priceChange = priceChange24h.orZero().movePointLeft(2),
            fiatRateUSD = priceUsd.orZero(),
        ),
    )
}

fun Pair<String, QuotesResponse.Quote>.toDomain(
    source: StatusSource = StatusSource.ACTUAL,
    fiatCurrency: FiatCurrency = FiatCurrency.Default,
): QuoteStatus {
    return second.toDomain(rawCurrencyId = first, source = source, fiatCurrency = fiatCurrency)
}