package com.tangem.data.txhistory.fetcher

import com.tangem.data.txhistory.fetcher.TxHistoryFetcherUtils.Companion.cancelScope
import com.tangem.domain.models.account.AccountId
import com.tangem.domain.txhistory.fetcher.AccountTxHistoryFetcher
import com.tangem.domain.txhistory.fetcher.TxHistoryFetchTrigger
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

internal class DefaultAccountTxHistoryFetcher @AssistedInject constructor(
    @Assisted override val accountId: AccountId,
    private val utils: TxHistoryFetcherUtils,
) : AccountTxHistoryFetcher, TxHistoryFetcherUtils by utils {

    override operator fun invoke(params: TxHistoryFetchTrigger) {
        sendTrigger(params)
    }

    override fun close() {
        cancelScope()
    }

    @AssistedFactory
    internal interface Factory {
        fun create(accountId: AccountId): DefaultAccountTxHistoryFetcher
    }
}