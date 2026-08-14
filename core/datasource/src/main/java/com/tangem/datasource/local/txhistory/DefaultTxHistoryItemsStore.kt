package com.tangem.datasource.local.txhistory

import com.tangem.core.local.datastore.RuntimeSharedMapStore
import com.tangem.domain.models.network.TxInfo
import com.tangem.domain.txhistory.models.Page
import com.tangem.domain.txhistory.models.PaginationWrapper
import com.tangem.utils.extensions.addOrReplace

internal class DefaultTxHistoryItemsStore(
    private val store: RuntimeSharedMapStore<TxHistoryItemsStore.Key, Set<PaginationWrapper<TxInfo>>>,
) : TxHistoryItemsStore {

    override suspend fun getSyncOrNull(key: TxHistoryItemsStore.Key, page: Page): PaginationWrapper<TxInfo>? {
        val storedValue = store.getSyncOrNull(key)

        return storedValue?.firstOrNull { it.currentPage == page }
    }

    override suspend fun remove(key: TxHistoryItemsStore.Key) = store.remove(key)

    override suspend fun store(key: TxHistoryItemsStore.Key, value: PaginationWrapper<TxInfo>) {
        store.update(key = key, default = emptySet()) { stored ->
            stored.addOrReplace(value) { it.currentPage == value.currentPage }
        }
    }
}