package com.tangem.features.polymarket.impl.main.model

import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.TimeMark
import kotlin.time.TimeSource

/** One per screen: a shared cooldown keeps a stretch without network from repeating the message on every tab. */
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