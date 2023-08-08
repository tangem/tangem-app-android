package com.tangem.domain.walletmanager.utils

import com.tangem.blockchain.common.txhistory.TransactionHistoryState
import com.tangem.domain.txhistory.models.TxHistoryState
import com.tangem.utils.converter.Converter
import com.tangem.blockchain.common.txhistory.TransactionHistoryState as SdkTransactionHistoryState

internal class SdkTransactionHistoryStateConverter : Converter<SdkTransactionHistoryState, TxHistoryState> {

    override fun convert(value: TransactionHistoryState): TxHistoryState = when (value) {
        TransactionHistoryState.Success.Empty -> TxHistoryState.Success.Empty
        is TransactionHistoryState.Success.HasTransactions -> TxHistoryState.Success.HasTransactions(value.txCount)
        is TransactionHistoryState.Failed.FetchError -> TxHistoryState.Failed.FetchError(value.exception)
        TransactionHistoryState.NotImplemented -> TxHistoryState.NotImplemented
    }
}
