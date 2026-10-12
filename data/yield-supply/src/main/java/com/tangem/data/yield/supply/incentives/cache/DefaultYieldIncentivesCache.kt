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
    private val generations = mutableMapOf<String, Long>()

    override suspend fun <T : Any> getOrFetch(
        key: String,
        window: Duration,
        fetch: suspend () -> Either<YieldIncentivesError, T>,
    ): Either<YieldIncentivesError, T> {
        peek<T>(key = key, window = window)?.let { return it.right() }

        val keyLock = guard.withLock { keyLocks.getOrPut(key) { Mutex() } }

        return keyLock.withLock {
            peek<T>(key = key, window = window)?.let { return@withLock it.right() }

            val generation = guard.withLock { generations.getOrDefault(key, 0L) }
            fetch().onRight { value ->
                guard.withLock {
                    // A fetch that left before an invalidation answers for the state it discarded, so it is not kept.
                    if (generations.getOrDefault(key, 0L) == generation) {
                        entries[key] = Entry(value = value, storedAtMillis = currentTimeMillis())
                    }
                }
            }
        }
    }

    override suspend fun invalidate(key: String) {
        guard.withLock {
            entries.remove(key)
            generations[key] = generations.getOrDefault(key, 0L) + 1
            // The fetch in flight keeps its own lock; a read after the invalidation starts a fresh one instead of
            // queueing behind a request that already answers for the past.
            keyLocks.remove(key)
        }
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