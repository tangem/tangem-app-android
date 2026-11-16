package com.tangem.features.polymarket.impl.details.model.chart

import com.tangem.features.polymarket.impl.common.ui.chart.PolymarketProbabilityChart
import com.tangem.features.polymarket.impl.common.ui.chart.PolymarketProbabilityChart.Point
import com.tangem.features.polymarket.impl.common.ui.chart.PolymarketProbabilityChart.Series
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.toImmutableList
import kotlin.time.Duration

/**
 * Presentation input from a history provider: UTC milliseconds, sorted unique timestamps and probabilities in 0…1.
 * A missing key is a failed series; a present empty list is a successful empty history. No wire conversion happens here.
 */
data class PolymarketChartHistory(
    val points: ImmutableMap<PolymarketChartOutcome.Key, ImmutableList<Point>>,
    val fidelity: Duration,
) {
    init {
        require(fidelity.inWholeMilliseconds > 0 && fidelity.isFinite())
    }
}

internal fun PolymarketChartContent.series(history: PolymarketChartHistory): ImmutableList<Series> {
    val series = outcomes.map { outcome ->
        Series(
            id = outcome.key.seriesId,
            title = outcome.title,
            color = outcome.color,
            points = history.points[outcome.key].orEmpty().toImmutableList(),
        )
    }
    if (!isComplementaryPair) return series.toImmutableList()
    return series.mapIndexed { index, own ->
        val other = series[series.lastIndex - index]
        val first = own.points.firstOrNull()?.timestampMillis
        val last = own.points.lastOrNull()?.timestampMillis
        val before = other.points.filter { first == null || it.timestampMillis < first }.map { it.complemented() }
        val after = other.points.filter { last != null && it.timestampMillis > last }.map { it.complemented() }
        own.copy(points = (before + own.points + after).toImmutableList())
    }.toImmutableList()
}

internal val PolymarketChartOutcome.Key.seriesId: String
    get() = "$marketId:${version.name}:$assetId"

private fun Point.complemented(): Point = copy(probability = 1.0 - probability)

internal fun PolymarketChartContent.withLivePoints(
    series: ImmutableList<Series>,
    livePoints: Map<PolymarketChartOutcome.Key, List<Point>>,
    period: PolymarketChartPeriod,
    nowMillis: Long,
): ImmutableList<Series> = series.mapIndexed { index, line ->
    val lastTimestamp = line.points.lastOrNull()?.timestampMillis ?: Long.MIN_VALUE
    val tail = livePoints[outcomes[index].key].orEmpty()
        .filter { it.timestampMillis > lastTimestamp }
    val combined = line.points + tail
    val cutoff = period.duration?.let { nowMillis - it.inWholeMilliseconds }
    val visible = if (cutoff == null) combined else combined.filter { it.timestampMillis >= cutoff }
    line.copy(
        title = outcomes[index].title,
        color = outcomes[index].color,
        points = if (visible.size >= 2) visible.toImmutableList() else line.points,
    )
}.toImmutableList()

internal fun ImmutableList<Series>.asChartState(
    marker: PolymarketProbabilityChart.LatestMarker,
): PolymarketProbabilityChart.State = if (any { it.points.size > 1 }) {
    PolymarketProbabilityChart.State.Content(this, marker)
} else {
    PolymarketProbabilityChart.State.NoData
}