package com.tangem.domain.quotes.multi

import com.tangem.domain.core.flow.FlowFetcher
import com.tangem.domain.tokens.model.CryptoCurrency

/**
 * Fetcher of quotes
 *
[REDACTED_AUTHOR]
 */
interface MultiQuoteFetcher : FlowFetcher<MultiQuoteFetcher.Params> {

    data class Params(val currenciesIds: Set<CryptoCurrency.RawID>)
}