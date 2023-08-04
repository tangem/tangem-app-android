package com.tangem.datasource.local.cache

import com.tangem.datasource.local.cache.model.CacheKey

interface CacheKeysStore {

    suspend fun getSyncOrNull(key: String): CacheKey?

    suspend fun store(key: CacheKey)

    suspend fun remove(key: String)

    suspend fun clear()
}
