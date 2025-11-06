package com.tangem.utils.retryer

import kotlinx.coroutines.delay
import kotlin.math.pow
import kotlin.random.Random

/**
 * Utility class to retry a suspending block of code a specified number of times with exponential backoff and jitter.
 *
 * @param attempt The number of attempts to retry the block.
 * @param block The suspending block of code to be executed. It should return true if successful, false otherwise.
 *
[REDACTED_AUTHOR]
 */
class Retryer(
    private val attempt: Int,
    private val block: suspend () -> Boolean,
) {

    init {
        require(attempt > 0) { "Retryer.attempt should be greater than 0" }
    }

    /**
     * Launches the retry mechanism, executing the block up to the specified number of attempts.
     * If the block returns true, the retrying stops.
     */
    suspend fun launch() {
        repeat(attempt) {
            val timeInMillis = calculateDelay(iteration = it)
            delay(timeInMillis)

            val result = block()

            if (result) return
        }
    }

    /**
     * Calculates the delay before the next retry attempt using exponential backoff with jitter.
     * delay = BASE * 2^iteration +- JITTER
     *
     * @param iteration The current iteration number (0-based).
     * @return The calculated delay in milliseconds.
     */
    private fun calculateDelay(iteration: Int): Long {
        return (BASE_DELAY * 2.0.pow(iteration.toDouble()) + Random.nextLong(-JITTER, JITTER)).toLong()
    }

    private companion object {

        const val BASE_DELAY = 1000L // 1 second
        const val JITTER = 500L // 0.5 second
    }
}