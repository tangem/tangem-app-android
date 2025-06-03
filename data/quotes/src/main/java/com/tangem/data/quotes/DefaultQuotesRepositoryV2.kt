package com.tangem.data.quotes

import com.tangem.data.quotes.store.QuotesStatusesStore
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.quotes.QuotesRepositoryV2
import com.tangem.domain.tokens.model.QuoteStatus
import javax.inject.Inject

/**
 * Default implementation of [QuotesRepositoryV2]
 *
 * @property quotesStore quotes store
 *
[REDACTED_AUTHOR]
 */
internal class DefaultQuotesRepositoryV2 @Inject constructor(
    private val quotesStore: QuotesStatusesStore,
) : QuotesRepositoryV2 {

    override suspend fun getMultiQuoteSyncOrNull(currenciesIds: Set<CryptoCurrency.RawID>): Set<QuoteStatus>? {
        return quotesStore.getAllSyncOrNull()?.mapTo(hashSetOf()) {
            it.takeIf { it.rawCurrencyId in currenciesIds }
                ?: QuoteStatus(rawCurrencyId = it.rawCurrencyId)
        }
    }
}