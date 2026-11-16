package com.tangem.domain.polymarket.model

import java.time.Duration
import java.time.Instant

data class PolymarketMarketLifetime(val start: Instant?, val status: Status) {

    val isClosed: Boolean get() = status is Status.Closed

    fun age(now: Instant): Duration = start?.let { Duration.between(it, now) } ?: UNKNOWN_AGE

    internal fun tradingEnd(now: Instant): Instant = when (status) {
        Status.Open -> now
        is Status.Closed -> minOf(status.closedAt ?: status.scheduledEnd ?: now, now)
    }

    sealed interface Status {
        data object Open : Status
        data class Closed(val closedAt: Instant?, val scheduledEnd: Instant?) : Status
    }

    private companion object {
        val UNKNOWN_AGE: Duration = Duration.ofDays(60)
    }
}