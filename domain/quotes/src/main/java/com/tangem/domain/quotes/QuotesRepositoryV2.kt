package com.tangem.domain.quotes

import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.tokens.model.QuoteStatus

/**
 * Quotes repository
 *
* [REDACTED_AUTHOR]
 */
interface QuotesRepositoryV2 {

    /** Get quotes by [currenciesIds] synchronously or null */
    suspend fun getMultiQuoteSyncOrNull(currenciesIds: Set<CryptoCurrency.RawID>): Set<QuoteStatus>?
}
