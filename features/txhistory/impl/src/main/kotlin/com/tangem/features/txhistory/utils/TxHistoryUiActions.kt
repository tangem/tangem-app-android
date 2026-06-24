package com.tangem.features.txhistory.utils

import com.tangem.domain.txhistory.model.TxHistoryInfo

internal interface TxHistoryUiActions {

    fun openExplorer()
    fun openTxInExplorer(txHash: String)
    fun onTransactionClick(item: TxHistoryInfo)
}