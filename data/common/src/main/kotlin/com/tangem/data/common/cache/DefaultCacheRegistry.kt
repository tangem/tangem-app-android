package com.tangem.data.common.cache

import com.tangem.datasource.local.cache.CacheKeysStore
import com.tangem.datasource.local.cache.model.CacheKey
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import org.joda.time.Duration
import org.joda.time.LocalDateTime
import timber.log.Timber

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
        Timber.d("Invalidate the cache key: $key")
        withContext(NonCancellable) { cacheKeysStore.remove(key) }
    }

    override suspend fun invalidate(keys: Collection<String>) {
        Timber.d("Invalidate cache keys: $keys")
        withContext(NonCancellable) { cacheKeysStore.remove(keys) }
    }

    override suspend fun invalidateAll() {
        Timber.d("Invalidate all cache keys")
        withContext(NonCancellable) { cacheKeysStore.clear() }
    }

    override suspend fun invokeOnExpire(
        key: String,
        skipCache: Boolean,
        expireIn: Duration,
        block: suspend () -> Unit,
    ) {
        val isExpired = isExpired(key) || skipCache
        if (!isExpired) return

        try {
            Timber.d("Invoke the action associated with the cache key: $key")

            cacheKeysStore.store(
                key = CacheKey(
                    id = key,
                    updatedAt = LocalDateTime.now(),
                    expiresIn = expireIn,
                ),
            )

            block()
        } catch (e: Throwable) {
            Timber.e(e, "The action related to the cache key has failed: $key")

            invalidate(key)

            throw e
        }
    }
}
