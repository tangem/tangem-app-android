package com.tangem.common.test.data.quote

import com.tangem.datasource.api.tangemTech.models.QuotesResponse
import com.tangem.datasource.local.quote.converter.QuoteConverter
import com.tangem.domain.models.StatusSource
import com.tangem.domain.tokens.model.Quote

fun QuotesResponse.Quote.toDomain(rawCurrencyId: String, source: StatusSource = StatusSource.ACTUAL): Quote {
    return QuoteConverter(source = source).convert(value = mapOf(rawCurrencyId to this).entries.first())
}

fun Pair<String, QuotesResponse.Quote>.toDomain(source: StatusSource = StatusSource.ACTUAL): Quote {
    return QuoteConverter(source = source).convert(value = mapOf(this).entries.first())
}