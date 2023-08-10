package com.tangem.domain.tokens.repository

import com.tangem.domain.tokens.models.CryptoCurrency
import com.tangem.domain.tokens.models.Quote
import kotlinx.coroutines.flow.Flow

/**
 * Repository for everything related to the quotes of tokens
 * */
interface QuotesRepository {

    /**
     * Retrieves the quotes for a set of specified cryptocurrencies, identified by their unique IDs.
     *
     * @param currenciesIds The unique identifiers of the cryptocurrencies for which quotes are to be retrieved.
     * @param refresh A boolean flag indicating whether the data should be refreshed.
     * @return A [Flow] emitting a set of quotes corresponding to the specified cryptocurrencies.
     */
    fun getQuotes(currenciesIds: Set<CryptoCurrency.ID>, refresh: Boolean): Flow<Set<Quote>>
}
