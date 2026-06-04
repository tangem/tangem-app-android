package com.tangem.data.txhistory.fetcher

import com.tangem.data.txhistory.fetcher.TxHistoryFetcherUtils.Companion.cancelScope
import com.tangem.domain.models.account.AccountId
import com.tangem.domain.txhistory.fetcher.ExpressTxHistoryFetcher
import com.tangem.domain.txhistory.fetcher.TxHistoryExpressTrigger
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

internal class DefaultExpressTxHistoryFetcher @AssistedInject constructor(
    @Assisted override val address: String,
    @Assisted private val accountId: AccountId,
    private val utils: TxHistoryFetcherUtils,
) : ExpressTxHistoryFetcher, TxHistoryFetcherUtils by utils {

    override suspend fun invoke(params: TxHistoryExpressTrigger) {
        utils.sendTrigger(params)
        accountId
    }

    override fun close() {
        cancelScope()
    }

    @AssistedFactory
    internal interface Factory {
        fun create(address: String, accountId: AccountId): DefaultExpressTxHistoryFetcher
    }
}