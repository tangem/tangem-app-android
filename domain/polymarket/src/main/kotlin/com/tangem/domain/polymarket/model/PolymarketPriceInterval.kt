package com.tangem.domain.polymarket.model

import java.time.Duration
import java.time.Instant

enum class PolymarketPriceInterval(val duration: Duration?) {
    HOUR(Duration.ofHours(1)),
    SIX_HOURS(Duration.ofHours(SIX_HOUR_WINDOW)),
    DAY(Duration.ofDays(1)),
    WEEK(Duration.ofDays(DAYS_IN_WEEK)),
    MONTH(Duration.ofDays(DAYS_IN_MONTH)),
    MAX(null),
    ;

    fun historyQuery(lifetime: PolymarketMarketLifetime, now: Instant): PolymarketPriceHistoryQuery {
        val start = lifetime.start ?: return intervalQuery(
            interval = this,
            fidelityMinutes = if (this == MAX) MAX_FIDELITY_MINUTES else UNKNOWN_AGE_FIDELITY_MINUTES,
        )
        val age = Duration.between(start, lifetime.tradingEnd(now))
        val interval = entries.first { it == this || it.duration?.let { duration -> age < duration } == true }
        val fidelityMinutes = interval.fidelityMinutes(age)
        if (this != MAX) return intervalQuery(interval, fidelityMinutes)

        val closedAt = (lifetime.status as? PolymarketMarketLifetime.Status.Closed)?.closedAt
        val end = closedAt?.takeIf { it > start && Duration.between(start, it) <= BOUNDED_RANGE_LIMIT }
        return PolymarketPriceHistoryQuery(PolymarketPriceHistoryQuery.Window.Range(start, end), fidelityMinutes)
    }

    private fun fidelityMinutes(age: Duration): Int = when (this) {
        HOUR, SIX_HOURS -> MINUTE_FIDELITY
        DAY -> DAY_FIDELITY_MINUTES
        WEEK -> if (age < FINE_WEEK_GRID_LIMIT) SHORT_WEEK_FIDELITY_MINUTES else WEEK_FIDELITY_MINUTES
        MONTH -> if (age < FINE_MONTH_GRID_LIMIT) SHORT_MONTH_FIDELITY_MINUTES else MONTH_FIDELITY_MINUTES
        MAX -> MAX_FIDELITY_MINUTES
    }

    companion object {
        private const val MINUTE_FIDELITY = 1
        private const val DAY_FIDELITY_MINUTES = 5
        private const val SHORT_WEEK_FIDELITY_MINUTES = 15
        private const val WEEK_FIDELITY_MINUTES = 30
        private const val SHORT_MONTH_FIDELITY_MINUTES = 60
        private const val MONTH_FIDELITY_MINUTES = 180
        private const val MAX_FIDELITY_MINUTES = 720
        private const val UNKNOWN_AGE_FIDELITY_MINUTES = 10
        private val BOUNDED_RANGE_LIMIT = Duration.ofDays(14)
        private val FINE_WEEK_GRID_LIMIT = Duration.ofDays(DAYS_IN_WEEK).dividedBy(2)
        private val FINE_MONTH_GRID_LIMIT = Duration.ofDays(15)

        fun available(lifetime: PolymarketMarketLifetime, now: Instant): List<PolymarketPriceInterval> {
            if (lifetime.isClosed) return listOf(MAX)
            val age = lifetime.age(now)
            return when {
                age < DAY.duration -> listOf(HOUR, SIX_HOURS, MAX)
                age < WEEK.duration -> listOf(HOUR, SIX_HOURS, DAY, MAX)
                age < MONTH.duration -> listOf(HOUR, SIX_HOURS, DAY, WEEK, MAX)
                else -> listOf(HOUR, DAY, WEEK, MONTH, MAX)
            }
        }

        private fun intervalQuery(interval: PolymarketPriceInterval, fidelityMinutes: Int) =
            PolymarketPriceHistoryQuery(PolymarketPriceHistoryQuery.Window.Interval(interval), fidelityMinutes)
    }
}

private const val SIX_HOUR_WINDOW = 6L
private const val DAYS_IN_WEEK = 7L
private const val DAYS_IN_MONTH = 30L