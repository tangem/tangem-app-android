package com.tangem.datasource.local.txhistory

import com.tangem.datasource.local.datastore.core.StringKeyDataStore
import com.tangem.datasource.local.datastore.core.StringKeyDataStoreDecorator
import com.tangem.domain.models.network.TxInfo
import com.tangem.domain.txhistory.models.Page
import com.tangem.domain.txhistory.models.PaginationWrapper
import com.tangem.utils.extensions.addOrReplace

internal class DefaultTxHistoryItemsStore(
    dataStore: StringKeyDataStore<Set<PaginationWrapper<TxInfo>>>,
) : TxHistoryItemsStore,
    StringKeyDataStoreDecorator<TxHistoryItemsStore.Key, Set<PaginationWrapper<TxInfo>>>(dataStore) {

    override fun provideStringKey(key: TxHistoryItemsStore.Key): String = key.toString()

    override suspend fun getSyncOrNull(key: TxHistoryItemsStore.Key, page: Page): PaginationWrapper<TxInfo>? {
        val storedValue = getSyncOrNull(key)

        return storedValue?.firstOrNull { it.currentPage == page }
    }

    override suspend fun store(key: TxHistoryItemsStore.Key, value: PaginationWrapper<TxInfo>) {
        val oldValue = getSyncOrNull(key).orEmpty()
        val newValue = oldValue.addOrReplace(value) {
            it.currentPage == value.currentPage
        }

        store(key, newValue)
    }
}