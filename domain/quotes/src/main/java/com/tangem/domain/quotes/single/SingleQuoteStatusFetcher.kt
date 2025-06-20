package com.tangem.domain.quotes.single

import com.tangem.domain.core.flow.FlowFetcher
import com.tangem.domain.models.currency.CryptoCurrency

/**
 * Fetcher of quote for specified [CryptoCurrency.RawID]
 *
[REDACTED_AUTHOR]
 */
interface SingleQuoteStatusFetcher : FlowFetcher<SingleQuoteStatusFetcher.Params> {

    /**
     * Params
     *
     * @property rawCurrencyId crypto currency id
     * @property appCurrencyId app currency id, if null then selected app currency will be used
     */
    data class Params(
        val rawCurrencyId: CryptoCurrency.RawID,
        val appCurrencyId: String?,
    )
}