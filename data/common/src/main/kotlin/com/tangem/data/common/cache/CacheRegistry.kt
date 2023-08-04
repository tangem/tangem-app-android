package com.tangem.data.common.cache

import org.joda.time.Duration

/**
 * Represents a registry for managing cache.
 */
interface CacheRegistry {

    /**
     * Checks whether the cache key is expired.
     *
     * @param key cache key.
     * @return `true` if the cache key is expired, `false` otherwise.
     */
    suspend fun isExpired(key: String): Boolean

    /**
     * Invalidates the cache key in registry.
     *
     * If the key doesn't exist, or it's already invalidated, this method doesn't have any effect.
     *
     * @param key cache key.
     */
    suspend fun invalidate(key: String)

    /**
     * Invalidates all cache keys in the registry.
     *
     * After the call, the registry doesn't contain any valid keys.
     */
    suspend fun invalidateAll()

    /**
     * Defines a callback to be invoked when the cache key expires.
     *
     * @param key cache key.
     * @param skipCache if `true`, the callback will be invoked regardless of whether the key has expired or not.
     * @param expireIn the duration after which the cache key is considered expired.
     * @param block the block of code to be executed when the cache key expires.
     */
    suspend fun invokeOnExpire(
        key: String,
        skipCache: Boolean,
        expireIn: Duration = Duration.standardMinutes(DEFAULT_CACHE_KEY_EXPIRE_IN_MINUTES),
        block: suspend () -> Unit,
    )

    private companion object {
        const val DEFAULT_CACHE_KEY_EXPIRE_IN_MINUTES = 5L
    }
}