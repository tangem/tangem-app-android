package com.tangem.datasource.local.visa

import com.tangem.domain.visa.model.TangemPayTxHistoryItem

interface TangemPayTxHistoryItemsStore {

    suspend fun getSyncOrNull(key: String, cursor: String): List<TangemPayTxHistoryItem>?

    suspend fun remove(key: String)

    suspend fun store(key: String, cursor: String, value: List<TangemPayTxHistoryItem>)
}