package com.tangem.data.quotes.repository

import com.tangem.data.quotes.store.QuotesStatusesStore
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.quote.QuoteStatus
import com.tangem.domain.quotes.QuotesRepository
import timber.log.Timber
import javax.inject.Inject

/**
 * Default implementation of [QuotesRepository]
 *
 * @property quotesStatusesStore quotes statuses store
 *
[REDACTED_AUTHOR]
 */
internal class DefaultQuotesRepository @Inject constructor(
    private val quotesStatusesStore: QuotesStatusesStore,
) : QuotesRepository {

    override suspend fun getMultiQuoteSyncOrNull(currenciesIds: Set<CryptoCurrency.RawID>): Set<QuoteStatus> {
        if (currenciesIds.isEmpty()) {
            Timber.e("currenciesIds are empty")
            return emptySet()
        }

        val storedQuotes = quotesStatusesStore.getAllSyncOrNull()
            ?.filter { it.rawCurrencyId in currenciesIds }

        return currenciesIds.mapTo(hashSetOf()) { currencyId ->
            storedQuotes?.firstOrNull { it.rawCurrencyId == currencyId }
                ?: QuoteStatus(rawCurrencyId = currencyId)
        }
    }
}