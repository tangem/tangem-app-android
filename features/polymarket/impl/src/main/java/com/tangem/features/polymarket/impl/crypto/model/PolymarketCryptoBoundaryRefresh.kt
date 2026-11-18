package com.tangem.features.polymarket.impl.crypto.model

import com.tangem.domain.polymarket.model.PolymarketEvent
import com.tangem.domain.polymarket.model.cryptoWinner

/**
 * Tells when the shown window has to be read again around its bounds.
 *
 * The backend fixes the price to beat a moment after the start, and the final price and the winner some time after
 * the end. So the window is read once shortly after a bound and then again while what that bound brings is missing:
 * every five seconds for two minutes, then every twenty until ten minutes have passed. Later only coming back to
 * the screen reads it.
 *
 * @property lastReadMillis when a read of the shown event was last started here, or the event was put on screen
 */
internal data class PolymarketCryptoBoundaryRefresh(private val lastReadMillis: Long) {

    /**
     * The schedule after the read that is due at [nowMillis], or null when none is. A read counts from the moment
     * it is claimed, whatever its answer, so a slow or a failed one is not asked for again on the next tick.
     */
    fun claim(event: PolymarketEvent, nowMillis: Long): PolymarketCryptoBoundaryRefresh? {
        val window = event.cryptoWindow ?: return null
        val startMillis = window.start.toEpochMilli()
        val endMillis = window.end.toEpochMilli()
        val isDue = when {
            nowMillis >= endMillis + REFRESH_DELAY_MILLIS -> isDue(
                boundMillis = endMillis,
                isMissing = window.finalPrice == null || event.cryptoWinner()?.isResolved != true,
                nowMillis = nowMillis,
            )
            nowMillis >= startMillis + REFRESH_DELAY_MILLIS -> isDue(
                boundMillis = startMillis,
                isMissing = window.priceToBeat == null,
                nowMillis = nowMillis,
            )
            else -> false
        }

        return if (isDue) PolymarketCryptoBoundaryRefresh(lastReadMillis = nowMillis) else null
    }

    private fun isDue(boundMillis: Long, isMissing: Boolean, nowMillis: Long): Boolean {
        if (lastReadMillis < boundMillis) return true
        if (!isMissing || nowMillis >= boundMillis + RETRY_PERIOD_MILLIS) return false
        val isFastRetry = nowMillis < boundMillis + FAST_RETRY_PERIOD_MILLIS

        return nowMillis >= lastReadMillis + if (isFastRetry) FAST_RETRY_MILLIS else SLOW_RETRY_MILLIS
    }

    private companion object {
        const val REFRESH_DELAY_MILLIS = 2_000L
        const val FAST_RETRY_MILLIS = 5_000L
        const val SLOW_RETRY_MILLIS = 20_000L
        const val FAST_RETRY_PERIOD_MILLIS = 120_000L
        const val RETRY_PERIOD_MILLIS = 600_000L
    }
}