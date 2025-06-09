package com.tangem.data.quotes.single

import com.tangem.domain.quotes.multi.MultiQuoteFetcher
import com.tangem.domain.quotes.single.SingleQuoteFetcher
import javax.inject.Inject

internal class DefaultSingleQuoteFetcher @Inject constructor(
    private val multiQuoteFetcher: MultiQuoteFetcher,
) : SingleQuoteFetcher {

    override suspend fun invoke(params: SingleQuoteFetcher.Params) = multiQuoteFetcher.invoke(
        MultiQuoteFetcher.Params(
            currenciesIds = setOf(params.rawCurrencyId),
            appCurrencyId = params.appCurrencyId,
        ),
    )
}