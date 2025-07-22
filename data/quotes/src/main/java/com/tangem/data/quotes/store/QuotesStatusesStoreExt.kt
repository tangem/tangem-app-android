package com.tangem.data.quotes.store

import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.quote.QuoteStatus

/** Set [StatusSource] of quotes statuses as [StatusSource.CACHE] for [currenciesIds] */
internal suspend fun QuotesStatusesStore.setSourceAsCache(currenciesIds: Set<CryptoCurrency.RawID>) {
    updateStatusSource(currenciesIds = currenciesIds, source = StatusSource.CACHE)
}

/**
 * Set [StatusSource] of quotes statuses as [StatusSource.ONLY_CACHE] for [currenciesIds].
 * If the stored status is not found, store a default [QuoteStatus.Empty] status.
 */
internal suspend fun QuotesStatusesStore.setSourceAsOnlyCache(currenciesIds: Set<CryptoCurrency.RawID>) {
    updateStatusSource(
        currenciesIds = currenciesIds,
        source = StatusSource.ONLY_CACHE,
        ifNotFound = ::QuoteStatus,
    )
}