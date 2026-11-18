package com.tangem.data.polymarket

import com.tangem.core.remote.response.ApiResponse
import com.tangem.domain.polymarket.PolymarketServerClock
import com.tangem.utils.time.CurrentTimeProvider
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.atomic.AtomicLong
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs

/**
 * Follows the backend's clock by the `Date` header of its answers, which tells how far the device clock is off.
 *
 * The header counts whole seconds and an answer takes time to arrive, so readings of an unchanged clock differ by
 * up to a second or so. A reading therefore replaces the current one only when it is [NOISE_MILLIS] away or more:
 * following every answer would nudge the clock on each poll and make a countdown skip or repeat a second.
 */
@Singleton
internal class DefaultPolymarketServerClock @Inject constructor(
    private val deviceClock: CurrentTimeProvider,
) : PolymarketServerClock {

    private val offsetMillis = AtomicLong(0)

    override fun nowMillis(): Long = deviceClock.nowMillis() + offsetMillis.get()

    /** An answer without a readable date leaves the clock as it was; a failed answer counts like any other. */
    fun record(response: ApiResponse<*>) {
        val serverMillis = response.headers[DATE_HEADER]?.firstOrNull()?.toEpochMillisOrNull() ?: return
        val measured = serverMillis - deviceClock.nowMillis()

        offsetMillis.updateAndGet { current -> if (abs(measured - current) >= NOISE_MILLIS) measured else current }
    }

    private fun String.toEpochMillisOrNull(): Long? = runCatching {
        ZonedDateTime.parse(this, DateTimeFormatter.RFC_1123_DATE_TIME).toInstant().toEpochMilli()
    }.getOrNull()

    private companion object {
        /** OkHttp hands header names over in lower case. */
        const val DATE_HEADER = "date"
        const val NOISE_MILLIS = 2_000L
    }
}