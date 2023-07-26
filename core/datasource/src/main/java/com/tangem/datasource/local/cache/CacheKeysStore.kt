package com.tangem.datasource.local.cache

import com.tangem.datasource.local.cache.model.CacheKey

interface CacheKeysStore {

    fun getOrNull(id: String): CacheKey?

    fun addOrReplace(key: CacheKey)

    fun remove(id: String)

    fun clear()
}