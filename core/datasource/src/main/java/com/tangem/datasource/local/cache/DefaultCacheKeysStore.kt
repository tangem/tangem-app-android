package com.tangem.datasource.local.cache

import com.tangem.core.local.datastore.RuntimeSharedMapStore
import com.tangem.datasource.local.cache.model.CacheKey

internal class DefaultCacheKeysStore(
    store: RuntimeSharedMapStore<String, CacheKey>,
) : CacheKeysStore, RuntimeSharedMapStore<String, CacheKey> by store {

    override suspend fun store(key: CacheKey) {
        store(key.id, key)
    }
}