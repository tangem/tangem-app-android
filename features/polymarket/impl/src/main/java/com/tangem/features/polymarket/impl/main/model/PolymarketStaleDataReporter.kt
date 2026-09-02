package com.tangem.features.polymarket.impl.main.model

import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.TimeMark
import kotlin.time.TimeSource

/**
 * Tells the user that the feed may be showing stale data, then keeps quiet for [cooldown].
 *
 * One per screen, not per category: every category polls on its own, and without a shared gate a stretch without
 * network would raise the message again on every tab the user tries.
 */
internal class PolymarketStaleDataReporter(
    private val onReport: () -> Unit,
    private val timeSource: TimeSource = TimeSource.Monotonic,
    private val cooldown: Duration = REPORT_COOLDOWN,
) {

    private var lastReport: TimeMark? = null

    fun report() {
        val lastReport = lastReport
        if (lastReport != null && lastReport.elapsedNow() < cooldown) return

        this.lastReport = timeSource.markNow()
        onReport()
    }

    private companion object {
        val REPORT_COOLDOWN = 5.minutes
    }
}