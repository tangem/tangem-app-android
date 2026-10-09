package com.tangem.core.socket

import kotlin.math.pow
import kotlin.random.Random
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

fun interface Backoff {

    /** @param attempt 0 for the first retry after a connection ends */
    fun delayFor(attempt: Int): Duration

    companion object {

        fun exponential(
            min: Duration = 1.seconds,
            max: Duration = 30.seconds,
            jitter: Double = 0.5,
            random: Random = Random.Default,
        ): Backoff {
            require(min.isPositive()) { "Backoff min must be positive" }
            require(max >= min) { "Backoff max must not be below min" }
            require(jitter >= 0 && jitter < 1) { "Backoff jitter must be in [0, 1)" }
            return ExponentialBackoff(min = min, max = max, jitter = jitter, random = random)
        }
    }
}

private class ExponentialBackoff(
    private val min: Duration,
    private val max: Duration,
    private val jitter: Double,
    private val random: Random,
) : Backoff {

    override fun delayFor(attempt: Int): Duration {
        val exponent = attempt.coerceIn(0, MAX_EXPONENT)
        val base = (min * 2.0.pow(exponent)).coerceAtMost(max)
        val factor = if (jitter > 0) 1 + random.nextDouble(-jitter, jitter) else 1.0
        return (base * factor).coerceAtMost(max)
    }

    private companion object {
        const val MAX_EXPONENT = 30
    }
}