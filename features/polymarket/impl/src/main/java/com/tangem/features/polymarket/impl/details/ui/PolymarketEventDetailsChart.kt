package com.tangem.features.polymarket.impl.details.ui

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.utils.DateTimeFormatters
import com.tangem.features.polymarket.impl.R
import com.tangem.features.polymarket.impl.common.ui.chart.PolymarketProbabilityChart
import com.tangem.features.polymarket.impl.details.model.chart.PolymarketChartPeriod
import com.tangem.features.polymarket.impl.details.model.chart.PolymarketEventChartSession
import kotlinx.collections.immutable.toImmutableList

@Composable
internal fun PolymarketEventDetailsChart(
    snapshot: PolymarketEventChartSession.Snapshot,
    onPeriodChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val formatter = when (snapshot.selectedPeriod) {
        PolymarketChartPeriod.Hour, PolymarketChartPeriod.SixHours -> DateTimeFormatters.timeFormatter
        PolymarketChartPeriod.Day -> DateTimeFormatters.localFullDate
        else -> DateTimeFormatters.dateMMMdYYYY
    }
    PolymarketProbabilityChart(
        state = snapshot.chart,
        periods = snapshot.content.periods.map {
            PolymarketProbabilityChart.Period(it.id, resourceReference(it.title(snapshot.content.isLive)))
        }.toImmutableList(),
        selectedPeriodId = snapshot.selectedPeriod.id,
        onPeriodChange = onPeriodChange,
        formatTime = formatter::print,
        emptyTitle = resourceReference(R.string.prediction_chart_empty_title),
        emptyDescription = resourceReference(R.string.prediction_chart_empty_description),
        modifier = modifier,
        legendLineCount = if (snapshot.content.outcomes.size <= SINGLE_LINE_OUTCOMES) 1 else 2,
    )
}

@StringRes
private fun PolymarketChartPeriod.title(isLive: Boolean): Int = when (this) {
    PolymarketChartPeriod.Hour -> if (isLive) R.string.prediction_event_live else R.string.prediction_chart_period_1h
    PolymarketChartPeriod.SixHours -> R.string.prediction_chart_period_6h
    PolymarketChartPeriod.Day -> R.string.prediction_chart_period_1d
    PolymarketChartPeriod.Week -> R.string.prediction_chart_period_1w
    PolymarketChartPeriod.Month -> R.string.prediction_chart_period_1m
    PolymarketChartPeriod.Max -> R.string.prediction_chart_period_max
}

private const val SINGLE_LINE_OUTCOMES = 2