@file:Suppress("MagicNumber")

package com.tangem.feature.tester.presentation.storybook.page.polymarket.chart

import com.tangem.core.ui.extensions.stringReference
import com.tangem.features.polymarket.impl.common.ui.chart.PolymarketProbabilityChart as Chart
import com.tangem.features.polymarket.impl.common.ui.chart.PolymarketProbabilityChart.SeriesColor
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlin.math.sin

internal enum class ChartSample(val label: String, val count: Int = 3) {
    Binary("2 outcomes", 2),
    Sports("3 outcomes"),
    Multiple("4 outcomes", 4),
    Loading("Loading"),
    Empty("No data"),
    Error("Error → No data"),
    Single("One point"),
    Flat("Flat"),
    Partial("Partial history"),
}

internal val chartPeriods = listOf("Live", "6h", "1d", "Max")
    .map { Chart.Period(it, stringReference(it)) }.toImmutableList()

internal fun chartFixture(sample: ChartSample, period: String, revision: Int): Chart.State {
    if (sample == ChartSample.Loading) return Chart.State.Loading
    if (sample == ChartSample.Empty || sample == ChartSample.Error) return Chart.State.NoData
    val duration = chartDuration(period)
    val colors = when (sample) {
        ChartSample.Binary -> listOf(SeriesColor.Blue, SeriesColor.Red)
        ChartSample.Multiple -> listOf(SeriesColor.Blue, SeriesColor.Green, SeriesColor.Red, SeriesColor.Yellow)
        else -> listOf(SeriesColor.Red, SeriesColor.Blue, SeriesColor.Neutral)
    }
    val names = when (sample) {
        ChartSample.Binary -> listOf("Yes", "No")
        ChartSample.Multiple -> listOf("Candidate A", "Candidate B", "Candidate C", "Candidate D")
        else -> listOf("Otelul Galati", "FC Corvinul 1921 Hunedoara", "Draw")
    }
    val series = List(sample.count) { index ->
        Chart.Series(
            id = "outcome-$index",
            title = names[index],
            color = colors[index],
            points = chartPoints(sample = sample, index = index, duration = duration, revision = revision),
        )
    }
    return Chart.State.Content(
        series = series.toImmutableList(),
        latestMarker = if (period == "Live") Chart.LatestMarker.Pulsing else Chart.LatestMarker.Still,
    )
}

private fun chartDuration(period: String): Long = when (period) {
    "Live" -> 3_600_000L
    "6h" -> 21_600_000L
    "1d" -> 86_400_000L
    else -> 7_776_000_000L
}

private fun chartPoints(sample: ChartSample, index: Int, duration: Long, revision: Int): ImmutableList<Chart.Point> {
    val count = if (sample == ChartSample.Single) 1 else 80 + revision
    val points = List(count) { point ->
        val wave = if (sample == ChartSample.Flat) 0.0 else sin(point * 0.11) * 0.1 + sin(point * 1.7) * 0.02
        val probability = when (sample.count) {
            2 -> if (index == 0) 0.5 + wave else 0.5 - wave
            4 -> listOf(0.4 + wave, 0.3 - wave, 0.2, 0.1)[index]
            else -> listOf(0.45 + wave, 0.35 - wave / 2, 0.2 - wave / 2)[index]
        }
        Chart.Point(timestampMillis = 1_793_613_600_000L + point * duration / 79, probability = probability)
    }
    return (if (sample == ChartSample.Partial && index == 1) points.drop(count / 2) else points).toImmutableList()
}