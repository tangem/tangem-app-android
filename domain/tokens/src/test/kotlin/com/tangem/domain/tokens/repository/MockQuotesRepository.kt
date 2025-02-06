package com.tangem.domain.tokens.repository

import arrow.core.Either
import arrow.core.getOrElse
import com.tangem.domain.core.error.DataError
import com.tangem.domain.tokens.model.CryptoCurrency
import com.tangem.domain.tokens.model.Quote
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

internal class MockQuotesRepository(
    private val quotes: Flow<Either<DataError, Set<Quote>>>,
) : QuotesRepository {

    override fun getQuotesUpdates(currenciesIds: Set<CryptoCurrency.RawID>): Flow<Set<Quote>> {
        return quotes.map { it.getOrElse { e -> throw e } }
    }

    override suspend fun fetchQuotes(currenciesIds: Set<CryptoCurrency.RawID>, refresh: Boolean) {
        /* no-op */
    }

    override fun getQuotesUpdatesLegacy(currenciesIds: Set<CryptoCurrency.RawID>, refresh: Boolean): Flow<Set<Quote>> {
        return quotes.map { it.getOrElse { e -> throw e } }
    }

    override suspend fun getQuotesSync(currenciesIds: Set<CryptoCurrency.RawID>, refresh: Boolean): Set<Quote> {
        return getQuotesUpdatesLegacy(currenciesIds).first()
    }

    override suspend fun getQuoteSync(currencyId: CryptoCurrency.RawID): Quote {
        return quotes.map { it.getOrElse { e -> throw e } }.first()
            .first { it.rawCurrencyId == currencyId }
    }
}