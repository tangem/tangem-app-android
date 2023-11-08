package com.tangem.datasource.local.cache

import com.tangem.datasource.local.cache.model.CacheKey
import com.tangem.datasource.local.datastore.core.StringKeyDataStore

internal class DefaultCacheKeysStore(
    dataStore: StringKeyDataStore<CacheKey>,
) : CacheKeysStore, StringKeyDataStore<CacheKey> by dataStore {

    override suspend fun store(key: CacheKey) {
        store(key.id, key)
    }
}