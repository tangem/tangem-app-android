package com.tangem.domain.tokens.repository

import com.tangem.domain.tokens.model.CryptoCurrency
import com.tangem.domain.tokens.model.Quote
import kotlinx.coroutines.flow.Flow

/**
 * Repository for everything related to the quotes of tokens
 * */
interface QuotesRepository {

    /**
     * Retrieves updates of quotes for a set of specified cryptocurrencies, identified by their unique IDs.
     *
     * Loads remote quotes if they have expired.
     *
     * @param currenciesIds The unique identifiers of the cryptocurrencies for which quotes are to be retrieved.
     * @return A [Flow] emitting a set of quotes corresponding to the specified cryptocurrencies.
     */
    fun getQuotesUpdates(currenciesIds: Set<CryptoCurrency.ID>): Flow<Set<Quote>>

    /**
     * Retrieves quotes for a set of specified cryptocurrencies, identified by their unique IDs.
     *
     * Loads remote quotes if they have expired or if [refresh] is `true`.
     *
     * @param currenciesIds The unique identifiers of the cryptocurrencies for which quotes are to be retrieved.
     * @param refresh A boolean flag indicating whether the data should be refreshed.
     * @return A [Flow] emitting a set of quotes corresponding to the specified cryptocurrencies.
     */
    suspend fun getQuotesSync(currenciesIds: Set<CryptoCurrency.ID>, refresh: Boolean): Set<Quote>

    suspend fun getQuoteSync(currencyId: CryptoCurrency.ID): Quote?

    suspend fun fetchQuotes(currenciesIds: Set<CryptoCurrency.ID>)
}