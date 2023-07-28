package com.tangem.datasource.local.cache

import com.tangem.datasource.local.cache.model.CacheKey
import com.tangem.datasource.local.datastore.core.StringKeyDataStore
import com.tangem.datasource.local.datastore.core.StringKeyDataStoreDecorator

internal class DefaultCacheKeysStore(
    dataStore: StringKeyDataStore<CacheKey>,
) : CacheKeysStore, StringKeyDataStoreDecorator<String, CacheKey>(dataStore) {

    override fun provideStringKey(key: String): String {
        return key
    }

    override suspend fun store(key: CacheKey) {
        store(key.id, key)
    }
}
