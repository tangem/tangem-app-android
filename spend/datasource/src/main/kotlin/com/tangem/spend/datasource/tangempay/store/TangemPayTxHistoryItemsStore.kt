package com.tangem.spend.datasource.tangempay.store

import com.tangem.domain.tangempay.model.TangemPayTxHistoryItem

interface TangemPayTxHistoryItemsStore {

    suspend fun getSyncOrNull(key: String, cursor: String): List<TangemPayTxHistoryItem>?

    suspend fun remove(key: String)

    suspend fun remove(keys: List<String>)

    suspend fun store(key: String, cursor: String, value: List<TangemPayTxHistoryItem>)
}