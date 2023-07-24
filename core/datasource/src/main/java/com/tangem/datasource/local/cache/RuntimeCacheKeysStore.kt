package com.tangem.datasource.local.cache

import com.tangem.datasource.local.cache.model.CacheKey
import com.tangem.datasource.local.datastore.RuntimeDataStore

internal class RuntimeCacheKeysStore : CacheKeysStore {

    private val store = RuntimeDataStore(keyProvider = CacheKey::id)

    override fun get(id: String): CacheKey? {
        return store.getSync { it.id == id }.firstOrNull()
    }

    override fun addOrReplace(key: CacheKey) {
        store.addOrReplace(key)
    }

    override fun remove(id: String) {
        store.remove { it.id == id }
    }

    override fun clear() {
        store.clear()
    }
}
