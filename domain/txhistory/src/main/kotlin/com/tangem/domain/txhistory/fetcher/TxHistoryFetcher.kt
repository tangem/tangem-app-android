package com.tangem.domain.txhistory.fetcher

import com.tangem.domain.models.account.AccountId
import com.tangem.domain.models.wallet.UserWalletId

interface TxHistoryFetcher<T : TxHistoryFetchTrigger> {
    suspend fun invoke(params: T)
    fun close()
}

interface AppTxHistoryFetcher : TxHistoryFetcher<TxHistoryFetchTrigger>

interface WalletTxHistoryFetcher : TxHistoryFetcher<TxHistoryFetchTrigger> {
    val walletId: UserWalletId
}

interface AccountTxHistoryFetcher : TxHistoryFetcher<TxHistoryFetchTrigger> {
    val accountId: AccountId
    val walletId: UserWalletId get() = accountId.userWalletId
}

interface ExpressTxHistoryFetcher : TxHistoryFetcher<TxHistoryExpressTrigger> {
    val address: String
}