package com.tangem.data.common.cache

import com.tangem.datasource.local.cache.CacheKeysStore
import com.tangem.datasource.local.cache.model.CacheKey
import com.tangem.utils.logging.TangemLogger
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.joda.time.Duration
import org.joda.time.LocalDateTime
import java.util.concurrent.ConcurrentHashMap

internal class DefaultCacheRegistry(
    private val cacheKeysStore: CacheKeysStore,
) : CacheRegistry {

    private val mutex = Mutex()
    private val mutexes = ConcurrentHashMap<String, Mutex>()

    override suspend fun isExpired(key: String): Boolean {
        cacheKeysStore.getSyncOrNull(key) ?: return true

        // disabled cache expiration by time, update always using refresh flag
        return false
    }

    override suspend fun invalidate(key: String) {
        TangemLogger.d("Invalidate the cache key: $key")
        withContext(NonCancellable) { cacheKeysStore.remove(key) }
    }

    override suspend fun invalidate(keys: Collection<String>) {
        TangemLogger.d("Invalidate cache keys: $keys")
        withContext(NonCancellable) { cacheKeysStore.remove(keys) }
    }

    override suspend fun invalidateAll() {
        TangemLogger.d("Invalidate all cache keys")
        withContext(NonCancellable) { cacheKeysStore.clear() }
    }

    override suspend fun invokeOnExpire(
        key: String,
        skipCache: Boolean,
        expireIn: Duration,
        block: suspend () -> Unit,
    ) {
        // use a separate mutexForKey for each key to avoid multiple calls block() to the same key
        // also used mutex to safe create mutexForKey, otherwise it can lead to multiple calls for the same key
        val mutexForKey = mutex.withLock {
            mutexes.getOrPut(key) { Mutex() }
        }
        mutexForKey.withLock {
            val isExpired = isExpired(key) || skipCache
            if (!isExpired) {
                return
            }

            try {
                TangemLogger.d("Invoke the action associated with the cache key: $key")

                cacheKeysStore.store(
                    key = CacheKey(
                        id = key,
                        updatedAt = LocalDateTime.now(),
                        expiresIn = expireIn,
                    ),
                )

                block()
            } catch (e: Throwable) {
                TangemLogger.e("The action related to the cache key has failed: $key", e)

                invalidate(key)

                throw e
            }
        }
    }
}