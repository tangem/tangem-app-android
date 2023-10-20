package com.tangem.datasource.local.txhistory

import com.tangem.datasource.local.datastore.core.StringKeyDataStore
import com.tangem.datasource.local.datastore.core.StringKeyDataStoreDecorator
import com.tangem.domain.txhistory.models.PaginationWrapper
import com.tangem.domain.txhistory.models.TxHistoryItem
import com.tangem.utils.extensions.addOrReplace

internal class DefaultTxHistoryItemsStore(
    dataStore: StringKeyDataStore<Set<PaginationWrapper<TxHistoryItem>>>,
) : TxHistoryItemsStore,
    StringKeyDataStoreDecorator<TxHistoryItemsStore.Key, Set<PaginationWrapper<TxHistoryItem>>>(dataStore) {

    override fun provideStringKey(key: TxHistoryItemsStore.Key): String = key.toString()

    override suspend fun getNextPageSyncOrNull(key: TxHistoryItemsStore.Key): Int? {
        val storedValue = getSyncOrNull(key) ?: return null
        val lastWrappedItems = storedValue.maxBy(PaginationWrapper<*>::page)
        val lastPage = lastWrappedItems.page

        return if (lastPage <= lastWrappedItems.totalPages) {
            lastPage
        } else {
            null
        }
    }

    override suspend fun getSyncOrNull(key: TxHistoryItemsStore.Key, page: Int): PaginationWrapper<TxHistoryItem>? {
        val storedValue = getSyncOrNull(key)

        return storedValue?.firstOrNull { it.page == page }
    }

    override suspend fun store(key: TxHistoryItemsStore.Key, value: PaginationWrapper<TxHistoryItem>) {
        val oldValue = getSyncOrNull(key).orEmpty()
        val newValue = oldValue.addOrReplace(value) {
            it.page == value.page
        }

        store(key, newValue)
    }
}