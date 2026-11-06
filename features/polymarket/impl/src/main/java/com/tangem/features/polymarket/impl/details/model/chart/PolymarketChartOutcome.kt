package com.tangem.features.polymarket.impl.details.model.chart

import com.tangem.domain.polymarket.model.PolymarketMarketVersion
import com.tangem.features.polymarket.impl.common.ui.chart.PolymarketProbabilityChart.SeriesColor
import java.math.BigDecimal

data class PolymarketChartOutcome(
    val key: Key,
    val title: String,
    val probability: BigDecimal?,
    val color: SeriesColor,
) {
    /** A token from another market or protocol version must never reuse this outcome's history. */
    data class Key(val marketId: String, val assetId: String, val version: PolymarketMarketVersion)
}