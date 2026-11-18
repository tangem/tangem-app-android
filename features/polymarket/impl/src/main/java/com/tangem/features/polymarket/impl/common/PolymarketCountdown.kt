package com.tangem.features.polymarket.impl.common

import com.tangem.domain.polymarket.PolymarketServerClock
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.util.Locale
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

private const val MILLIS_IN_SECOND = 1_000L
private const val SECONDS_IN_MINUTE = 60L
private const val SECONDS_IN_HOUR = 3_600L

/** Emits the clock's time at once and then on each whole second of that clock, for as long as it is collected. */
internal fun PolymarketServerClock.secondTicks(): Flow<Long> = flow {
    while (true) {
        emit(nowMillis())
        delay(MILLIS_IN_SECOND - nowMillis() % MILLIS_IN_SECOND)
    }
}

/** Time left as `m:ss`, or `h:mm:ss` from an hour up; rounded up to a whole second and never below zero. */
internal fun formatPolymarketCountdown(remaining: Duration): String {
    val totalSeconds = remaining.coerceAtLeast(Duration.ZERO).let { left ->
        if (left == left.inWholeSeconds.seconds) left.inWholeSeconds else left.inWholeSeconds + 1
    }
    val hours = totalSeconds / SECONDS_IN_HOUR
    val minutes = totalSeconds % SECONDS_IN_HOUR / SECONDS_IN_MINUTE
    val seconds = totalSeconds % SECONDS_IN_MINUTE

    return if (hours > 0) {
        String.format(Locale.ROOT, "%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(Locale.ROOT, "%d:%02d", minutes, seconds)
    }
}