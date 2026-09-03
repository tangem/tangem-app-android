package com.tangem.data.common.cache

import com.tangem.data.common.cache.model.CacheKey

internal interface CacheKeysStore {

    suspend fun getSyncOrNull(key: String): CacheKey?

    suspend fun store(key: CacheKey)

    suspend fun remove(key: String)

    suspend fun remove(keys: Collection<String>)

    suspend fun clear()
}