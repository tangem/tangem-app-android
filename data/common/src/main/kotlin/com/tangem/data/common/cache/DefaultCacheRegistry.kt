package com.tangem.data.common.cache

import com.tangem.datasource.local.cache.CacheKeysStore
import com.tangem.datasource.local.cache.model.CacheKey
import org.joda.time.Duration
import org.joda.time.LocalDateTime

internal class DefaultCacheRegistry(
    private val cacheKeysStore: CacheKeysStore,
) : CacheRegistry {

    override suspend fun isExpired(key: String): Boolean {
        val cacheKey = cacheKeysStore.getSyncOrNull(key) ?: return true

        return cacheKey.updatedAt
            .plus(cacheKey.expiresIn)
            .isBefore(LocalDateTime.now())
    }

    override suspend fun invalidate(key: String) {
        cacheKeysStore.remove(key)
    }

    override suspend fun invalidateAll() {
        cacheKeysStore.clear()
    }

    override suspend fun invokeOnExpire(
        key: String,
        skipCache: Boolean,
        expireIn: Duration,
        block: suspend () -> Unit,
    ) {
        val isExpired = isExpired(key) || skipCache
        if (!isExpired) return

        cacheKeysStore.store(
            key = CacheKey(
                id = key,
                updatedAt = LocalDateTime.now(),
                expiresIn = expireIn,
            ),
        )

        try {
            block()
        } catch (e: Throwable) {
            invalidate(key)
            throw e
        }
    }
}
