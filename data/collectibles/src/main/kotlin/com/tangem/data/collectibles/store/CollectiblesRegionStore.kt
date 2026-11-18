package com.tangem.data.collectibles.store

import com.tangem.core.local.datastore.RuntimeSharedStore

internal class CollectiblesRegionStore(
    private val store: RuntimeSharedStore<Boolean>,
) {

    suspend fun isBlockedOrNull(): Boolean? = store.getSyncOrNull()

    suspend fun store(isBlocked: Boolean) {
        store.store(isBlocked)
    }
}