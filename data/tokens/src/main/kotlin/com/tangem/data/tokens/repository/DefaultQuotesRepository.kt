package com.tangem.data.tokens.repository

import com.tangem.datasource.quotes.QuotesDataSource
import com.tangem.domain.tokens.model.CryptoCurrency
import com.tangem.domain.tokens.model.Quote
import com.tangem.domain.tokens.repository.QuotesRepository
import kotlinx.coroutines.flow.Flow

internal class DefaultQuotesRepository(
    private val quotesDataSource: QuotesDataSource,
) : QuotesRepository {
    override fun getQuotesUpdates(currenciesIds: Set<CryptoCurrency.RawID>, refresh: Boolean): Flow<Set<Quote>> {
        return quotesDataSource.getQuotesUpdates(currenciesIds, refresh)
    }

    override suspend fun getQuotesSync(currenciesIds: Set<CryptoCurrency.RawID>, refresh: Boolean): Set<Quote> {
        return quotesDataSource.getQuotesSync(currenciesIds, refresh)
    }

    override suspend fun getQuoteSync(currencyId: CryptoCurrency.RawID): Quote? {
        return quotesDataSource.getQuoteSync(currencyId)
    }

    override suspend fun fetchQuotes(currenciesIds: Set<CryptoCurrency.RawID>) {
        quotesDataSource.fetchQuotes(currenciesIds)
    }
}