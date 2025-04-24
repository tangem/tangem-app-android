package com.tangem.domain.quotes.multi

import com.tangem.domain.core.flow.FlowFetcher
import com.tangem.domain.tokens.model.CryptoCurrency

/**
 * Fetcher of quotes
 *
[REDACTED_AUTHOR]
 */
interface MultiQuoteFetcher : FlowFetcher<MultiQuoteFetcher.Params> {

    /**
     * Params
     *
     * @property currenciesIds identifiers of currencies
     * @property appCurrencyId app currency id, if null then selected app currency will be used
     */
    data class Params(
        val currenciesIds: Set<CryptoCurrency.RawID>,
        val appCurrencyId: String?,
    )
}