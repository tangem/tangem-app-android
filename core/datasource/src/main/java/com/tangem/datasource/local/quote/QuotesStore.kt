package com.tangem.datasource.local.quote

import com.tangem.datasource.api.tangemTech.models.QuotesResponse
import com.tangem.domain.tokens.model.CryptoCurrency
import com.tangem.domain.tokens.model.Quote
import kotlinx.coroutines.flow.Flow

/** Quotes store */
interface QuotesStore {

    /** Get flow of quotes for [currenciesIds] */
    fun get(currenciesIds: Set<CryptoCurrency.RawID>): Flow<Set<Quote>>

    /** Get quotes for [currenciesIds] synchronously */
    suspend fun getSync(currenciesIds: Set<CryptoCurrency.RawID>): Set<Quote>

    /** Store [response] from remote */
    suspend fun store(response: QuotesResponse)
}