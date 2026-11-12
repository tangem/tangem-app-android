package com.tangem.features.polymarket.impl.details.model.chart

import com.tangem.domain.polymarket.model.PolymarketPriceInterval
import kotlin.time.Duration
import kotlin.time.toKotlinDuration

/** Presentation labels paired with the domain history policy. */
enum class PolymarketChartPeriod(val id: String, val interval: PolymarketPriceInterval) {
    Hour("1h", PolymarketPriceInterval.HOUR),
    SixHours("6h", PolymarketPriceInterval.SIX_HOURS),
    Day("1d", PolymarketPriceInterval.DAY),
    Week("1w", PolymarketPriceInterval.WEEK),
    Month("1m", PolymarketPriceInterval.MONTH),
    Max("max", PolymarketPriceInterval.MAX),
    ;

    val duration: Duration? get() = interval.duration?.toKotlinDuration()
}