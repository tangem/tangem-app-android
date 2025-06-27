package com.tangem.data.quotes.store

import com.tangem.datasource.api.tangemTech.models.QuotesResponse
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.quote.QuoteStatus
import kotlinx.coroutines.flow.Flow

/** Store of [QuoteStatus]'es set */
internal interface QuotesStatusesStore {

    /** Get flow of quotes */
    fun get(): Flow<Set<QuoteStatus>>

    /** Get all quotes synchronously or null */
    suspend fun getAllSyncOrNull(): Set<QuoteStatus>?

    /**
     * Update [source] of [QuoteStatus] by [currencyId].
     * If the status is not found, create a new one by [ifNotFound].
     */
    suspend fun updateStatusSource(
        currencyId: CryptoCurrency.RawID,
        source: StatusSource,
        ifNotFound: (CryptoCurrency.RawID) -> QuoteStatus? = { null },
    )

    /**
     * Update [source] of [QuoteStatus]es by [currenciesIds].
     * If the status is not found, create a new one by [ifNotFound].
     */
    suspend fun updateStatusSource(
        currenciesIds: Set<CryptoCurrency.RawID>,
        source: StatusSource,
        ifNotFound: (CryptoCurrency.RawID) -> QuoteStatus? = { null },
    )

    /**
     * Store quotes statuses
     *
     * @param values map of currency ids and quotes
     *
     * See complex methods in `QuotesStatusesStoreExt`.
     */
    suspend fun store(values: Map<String, QuotesResponse.Quote>)
}