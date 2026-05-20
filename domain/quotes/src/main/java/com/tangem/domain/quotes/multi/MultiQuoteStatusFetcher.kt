package com.tangem.domain.quotes.multi

import com.tangem.domain.core.flow.FlowFetcher
import com.tangem.domain.models.currency.CryptoCurrency

/**
 * Fetcher of quotes statuses for specified list of [CryptoCurrency.RawID]
 *
[REDACTED_AUTHOR]
 */
interface MultiQuoteStatusFetcher : FlowFetcher<MultiQuoteStatusFetcher.Params> {

    /**
     * Params
     *
     * @property currenciesIds identifiers of currencies
     */
    data class Params(val currenciesIds: Set<CryptoCurrency.RawID>)
}