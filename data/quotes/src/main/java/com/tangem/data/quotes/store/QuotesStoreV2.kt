package com.tangem.data.quotes.store

import com.tangem.datasource.api.tangemTech.models.QuotesResponse
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.tokens.model.Quote
import kotlinx.coroutines.flow.Flow

/** Store of [Quote]'es set */
internal interface QuotesStoreV2 {

    /** Get flow of quotes */
    fun get(): Flow<Set<Quote>>

    /** Get all quotes synchronously or null */
    suspend fun getAllSyncOrNull(): Set<Quote>?

    /** Refresh status of [currenciesIds] */
    suspend fun refresh(currenciesIds: Set<CryptoCurrency.RawID>)

    /** Store actual map of currency ids and quotes [values] */
    suspend fun storeActual(values: Map<String, QuotesResponse.Quote>)

    /** Store error for [currenciesIds] */
    suspend fun storeError(currenciesIds: Set<CryptoCurrency.RawID>)
}