package com.tangem.lib.crypto

import com.tangem.lib.crypto.models.txhistory.ProxyTransactionHistoryItem
import com.tangem.lib.crypto.models.txhistory.ProxyTransactionHistoryState

interface TxHistoryManager {

    @Throws(IllegalStateException::class)
    suspend fun checkTxHistoryState(networkId: String, derivationPath: String?): ProxyTransactionHistoryState

    @Throws(IllegalStateException::class)
    suspend fun getTxHistoryItems(
        networkId: String,
        derivationPath: String?,
        page: Int,
        pageSize: Int,
    ): List<ProxyTransactionHistoryItem>
}