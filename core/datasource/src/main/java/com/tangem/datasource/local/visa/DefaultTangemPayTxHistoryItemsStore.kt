package com.tangem.datasource.local.visa

import com.tangem.datasource.local.datastore.core.StringKeyDataStore
import com.tangem.datasource.local.datastore.core.StringKeyDataStoreDecorator
import com.tangem.domain.visa.model.TangemPayTxHistoryItem

internal class DefaultTangemPayTxHistoryItemsStore(
    dataStore: StringKeyDataStore<Map<String, List<TangemPayTxHistoryItem>>>,
) : TangemPayTxHistoryItemsStore,
    StringKeyDataStoreDecorator<String, Map<String, List<TangemPayTxHistoryItem>>>(dataStore) {
    override fun provideStringKey(key: String): String = key

    override suspend fun getSyncOrNull(key: String, cursor: String): List<TangemPayTxHistoryItem>? {
        val storedValue = getSyncOrNull(key)
        return storedValue?.get(cursor)
    }

    override suspend fun store(key: String, cursor: String, value: List<TangemPayTxHistoryItem>) {
        val oldValue = getSyncOrNull(key).orEmpty()
        val newValue = oldValue.toMutableMap().apply { put(cursor, value) }
        store(key, newValue)
    }
}