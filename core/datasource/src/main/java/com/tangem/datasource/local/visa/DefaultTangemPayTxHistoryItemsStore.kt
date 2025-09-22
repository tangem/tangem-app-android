package com.tangem.datasource.local.visa

import com.tangem.datasource.local.datastore.core.StringKeyDataStore
import com.tangem.datasource.local.datastore.core.StringKeyDataStoreDecorator
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.visa.model.VisaTxHistoryItem

internal class DefaultTangemPayTxHistoryItemsStore(
    dataStore: StringKeyDataStore<Map<Int, List<VisaTxHistoryItem>>>,
) : TangemPayTxHistoryItemsStore,
    StringKeyDataStoreDecorator<UserWalletId, Map<Int, List<VisaTxHistoryItem>>>(dataStore) {
    override fun provideStringKey(key: UserWalletId): String = key.stringValue

    override suspend fun getSyncOrNull(key: UserWalletId, offset: Int): List<VisaTxHistoryItem>? {
        val storedValue = getSyncOrNull(key)
        return storedValue?.get(offset)
    }

    override suspend fun store(key: UserWalletId, offset: Int, value: List<VisaTxHistoryItem>) {
        val oldValue = getSyncOrNull(key).orEmpty()
        val newValue = oldValue.toMutableMap().apply { put(offset, value) }
        store(key, newValue)
    }
}