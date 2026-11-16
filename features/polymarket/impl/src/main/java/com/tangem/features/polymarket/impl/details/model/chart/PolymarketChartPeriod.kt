package com.tangem.features.polymarket.impl.details.model.chart

import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours

/** Presentation periods. Request fidelity and wire-format intervals belong to the history provider. */
enum class PolymarketChartPeriod(val id: String, val duration: Duration?) {
    Hour("1h", 1.hours),
    SixHours("6h", 6.hours),
    Day("1d", 1.days),
    Week("1w", 7.days),
    Month("1m", 30.days),
    Max("max", null),
}