package com.tangem.common.test.data.quote

import com.tangem.datasource.api.tangemTech.models.QuotesResponse
import com.tangem.datasource.local.quote.converter.QuoteStatusConverter
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.quote.QuoteStatus

fun QuotesResponse.Quote.toDomain(rawCurrencyId: String, source: StatusSource = StatusSource.ACTUAL): QuoteStatus {
    return QuoteStatusConverter(source = source).convert(value = mapOf(rawCurrencyId to this).entries.first())
}

fun Pair<String, QuotesResponse.Quote>.toDomain(source: StatusSource = StatusSource.ACTUAL): QuoteStatus {
    return QuoteStatusConverter(source = source).convert(value = mapOf(this).entries.first())
}