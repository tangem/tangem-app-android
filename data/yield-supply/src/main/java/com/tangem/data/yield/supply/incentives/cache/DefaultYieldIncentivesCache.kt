package com.tangem.data.yield.supply.incentives.cache

import arrow.core.Either
import arrow.core.right
import com.tangem.domain.yield.supply.models.incentives.YieldIncentivesError
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.time.Duration

internal class DefaultYieldIncentivesCache(
    private val currentTimeMillis: () -> Long = System::currentTimeMillis,
) : YieldIncentivesCache {

    private val guard = Mutex()
    private val entries = mutableMapOf<String, Entry>()
    private val keyLocks = mutableMapOf<String, Mutex>()

    override suspend fun <T : Any> getOrFetch(
        key: String,
        window: Duration,
        fetch: suspend () -> Either<YieldIncentivesError, T>,
    ): Either<YieldIncentivesError, T> {
        peek<T>(key = key, window = window)?.let { return it.right() }

        val keyLock = guard.withLock { keyLocks.getOrPut(key) { Mutex() } }

        return keyLock.withLock {
            peek<T>(key = key, window = window)?.let { return@withLock it.right() }

            fetch().onRight { value ->
                guard.withLock { entries[key] = Entry(value = value, storedAtMillis = currentTimeMillis()) }
            }
        }
    }

    override suspend fun invalidate(key: String) {
        guard.withLock { entries.remove(key) }
    }

    private suspend fun <T : Any> peek(key: String, window: Duration): T? = guard.withLock {
        val entry = entries[key] ?: return@withLock null
        if (currentTimeMillis() - entry.storedAtMillis >= window.inWholeMilliseconds) {
            entries.remove(key)
            return@withLock null
        }
        @Suppress("UNCHECKED_CAST")
        entry.value as T
    }

    private class Entry(val value: Any, val storedAtMillis: Long)
}