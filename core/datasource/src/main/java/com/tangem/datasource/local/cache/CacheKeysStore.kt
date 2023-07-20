package com.tangem.datasource.local.cache

import com.tangem.datasource.local.cache.model.CacheKey

interface CacheKeysStore {

    fun get(id: String): CacheKey?

    fun addOrReplace(key: CacheKey)

    fun remove(id: String)

    fun clear()
}