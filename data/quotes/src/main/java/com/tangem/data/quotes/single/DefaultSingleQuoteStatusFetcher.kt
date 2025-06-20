package com.tangem.data.quotes.single

import com.tangem.domain.quotes.multi.MultiQuoteStatusFetcher
import com.tangem.domain.quotes.single.SingleQuoteStatusFetcher
import javax.inject.Inject

/**
 * Default implementation of [SingleQuoteStatusFetcher]
 *
 * @property multiQuoteStatusFetcher fetcher of quotes statuses
 */
internal class DefaultSingleQuoteStatusFetcher @Inject constructor(
    private val multiQuoteStatusFetcher: MultiQuoteStatusFetcher,
) : SingleQuoteStatusFetcher {

    override suspend fun invoke(params: SingleQuoteStatusFetcher.Params) = multiQuoteStatusFetcher.invoke(
        MultiQuoteStatusFetcher.Params(
            currenciesIds = setOf(params.rawCurrencyId),
            appCurrencyId = params.appCurrencyId,
        ),
    )
}