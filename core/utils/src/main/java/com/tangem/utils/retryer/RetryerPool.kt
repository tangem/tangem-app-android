package com.tangem.utils.retryer

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch

/**
 * Utility class to manage and launch multiple [Retryer] instances one by one.
 *
 * @param coroutineScope The [CoroutineScope] in which the retryers will be launched.
 *
[REDACTED_AUTHOR]
 */
class RetryerPool(private val coroutineScope: CoroutineScope) {

    private val queue = Channel<Retryer>(Channel.UNLIMITED)

    init {
        coroutineScope.launch {
            for (retryer in queue) {
                retryer.launch()
            }
        }
    }

    /**
     * Adds a [Retryer] to the pool and launches it one by one.
     *
     * @param retryer The [Retryer] instance to be added and launched.
     * @return The current [RetryerPool] instance for chaining.
     */
    operator fun plus(retryer: Retryer): RetryerPool {
        queue.trySend(retryer)
        return this
    }
}