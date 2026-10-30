package com.tangem.domain.txhistory.fetcher

import com.tangem.domain.models.wallet.UserWalletId

interface TxHistoryFetcher<T : TxHistoryFetchTrigger> {
    operator fun invoke(params: T)
    fun close()
}

interface AppTxHistoryFetcher : TxHistoryFetcher<TxHistoryFetchTrigger>

interface WalletTxHistoryFetcher : TxHistoryFetcher<TxHistoryFetchTrigger> {
    val walletId: UserWalletId
}

interface ExpressTxHistoryFetcher : TxHistoryFetcher<TxHistoryExpressTrigger> {
    val walletId: UserWalletId
}

interface JointOperationTxHistoryFetcher : TxHistoryFetcher<TxHistoryJointOperationTrigger> {
    val walletId: UserWalletId
}