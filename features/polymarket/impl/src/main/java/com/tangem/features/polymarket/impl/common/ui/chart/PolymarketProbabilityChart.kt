@file:Suppress("MagicNumber")

package com.tangem.features.polymarket.impl.common.ui.chart

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.ds2.segmentedcontrol.TangemSegmentedControl
import com.tangem.core.ui.ds2.shimmers.TangemShimmer
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.res.TangemTheme
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList

/**
 * Probability histories with a wrapping legend, adaptive percentage axis and a shared time marker.
 * [Figma](https://www.figma.com/design/dNvLAk1K7ZQ35oWN2aoAAY/Polymarket?node-id=11668-17461)
 *
 * Touching a line reads all available series at that time; releasing restores their latest values.
 * Updates animate between snapshots; while inspecting, incoming snapshots wait until release.
 * Changing the period or the set of series starts a fresh chart without an entrance animation.
 * Histories with fewer than two distinct timestamps use the empty state. Errors are mapped to
 * [PolymarketProbabilityChart.State.NoData] by the caller until an error design is available.
 *
 * @param periods Available periods, with localized labels. Empty hides the period control.
 * @param selectedPeriodId Caller-owned selection; changing it also clears the time marker.
 * @param onPeriodChange Requests another period without changing the supplied histories.
 * @param formatTime Localized date/time readout appropriate to the selected period.
 * @param legendLineCount Number of legend rows in the loading skeleton; loaded content wraps to its actual height.
 */
@Composable
@Suppress("LongParameterList")
fun PolymarketProbabilityChart(
    state: PolymarketProbabilityChart.State,
    periods: ImmutableList<PolymarketProbabilityChart.Period>,
    selectedPeriodId: String?,
    onPeriodChange: (String) -> Unit,
    formatTime: (Long) -> String,
    emptyTitle: TextReference,
    emptyDescription: TextReference,
    modifier: Modifier = Modifier,
    legendLineCount: Int = 2,
) {
    val incoming = state as? PolymarketProbabilityChart.State.Content
    val seriesIds = incoming?.series?.map { it.id to it.points.isEmpty() }
    var selectedTime by remember(selectedPeriodId, seriesIds) { mutableStateOf<Long?>(null) }
    var heldContent by remember(
        selectedPeriodId,
        seriesIds,
    ) { mutableStateOf<PolymarketProbabilityChart.State.Content?>(null) }
    val content = heldContent ?: incoming
    val scale = remember(content) { content?.let { ProbabilityChartScale.from(it.series) } }
    val isLoading = state == PolymarketProbabilityChart.State.Loading
    Column(modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        ProbabilityChartLegend(
            content = content?.takeIf { scale != null },
            isLoading = isLoading,
            lineCount = legendLineCount,
            selectedTime = selectedTime,
        )
        key(selectedPeriodId, seriesIds) {
            ProbabilityPlot(
                content = content?.takeIf { scale != null },
                scale = scale ?: ProbabilityChartScale.Placeholder,
                isLoading = isLoading,
                selectedTime = selectedTime,
                onSelectTime = { time ->
                    heldContent = if (time == null) null else content
                    selectedTime = time
                },
                formatTime = formatTime,
                emptyTitle = emptyTitle,
                emptyDescription = emptyDescription,
            )
        }
        if (periods.isNotEmpty()) {
            TangemSegmentedControl(
                items = periods.map { period ->
                    TangemSegmentedControl.Item(
                        id = period.id,
                        label = period.title,
                        isSelected = period.id == selectedPeriodId,
                        onClick = { if (period.id != selectedPeriodId) onPeriodChange(period.id) },
                    )
                }.toImmutableList(),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

/** Presentation inputs; outcome selection, history loading and error → [State.NoData] belong to the caller. */
object PolymarketProbabilityChart {

    @Immutable
    sealed interface State {
        data object Loading : State
        data object NoData : State

        @Immutable
        data class Content(
            val series: ImmutableList<Series>,
            val latestMarker: LatestMarker = LatestMarker.Still,
        ) : State {
            init {
                require(series.map { it.id }.distinct().size == series.size)
            }
        }
    }

    /** Stable IDs and colors are supplied by the outcome mapper, independently of current probability. */
    @Immutable
    data class Series(
        val id: String,
        val title: String,
        val color: SeriesColor,
        val points: ImmutableList<Point>,
    ) {
        init {
            require(points.zipWithNext().all { (left, right) -> left.timestampMillis < right.timestampMillis })
        }
    }

    /** UTC milliseconds and a finite probability in 0…1. History must already be sorted and deduplicated. */
    @Immutable
    data class Point(val timestampMillis: Long, val probability: Double) {
        init {
            require(probability in 0.0..1.0)
        }
    }

    @Immutable
    data class Period(val id: String, val title: TextReference)

    enum class SeriesColor { Blue, Red, Green, Yellow, Neutral }

    enum class LatestMarker { Hidden, Still, Pulsing }
}

@Composable
@Suppress("LongParameterList", "LongMethod")
private fun ProbabilityPlot(
    content: PolymarketProbabilityChart.State.Content?,
    scale: ProbabilityChartScale,
    isLoading: Boolean,
    selectedTime: Long?,
    onSelectTime: (Long?) -> Unit,
    formatTime: (Long) -> String,
    emptyTitle: TextReference,
    emptyDescription: TextReference,
) {
    val style = TangemTheme.typography3.caption.medium.copy(color = TangemTheme.colors3.text.secondary)
    val measurer = rememberTextMeasurer()
    val labels = remember(scale, style, measurer) { scale.ticks.map { measurer.measure(it.probabilityText(), style) } }
    val density = LocalDensity.current
    val labelHeight = with(density) { labels.maxOf { it.size.height }.toDp() }
    val axisWidth = with(density) { labels.maxOf { it.size.width }.toDp() }
    val height = maxOf(124.dp, labelHeight * 6)
    val gridColor = TangemTheme.colors3.border.primary
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().heightIn(min = height).height(IntrinsicSize.Min),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Box(Modifier.weight(1f).fillMaxHeight(), contentAlignment = Alignment.Center) {
                Canvas(Modifier.matchParentSize().padding(vertical = labelHeight / 2).clearAndSetSemantics {}) {
                    scale.ticks.forEach { tick ->
                        val y = scale.y(tick) * size.height
                        drawLine(
                            color = gridColor,
                            start = Offset(0f, y),
                            end = Offset(size.width, y),
                            strokeWidth = 1.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(3.dp.toPx(), 3.dp.toPx())),
                        )
                    }
                }
                when {
                    isLoading -> TangemShimmer(Modifier.matchParentSize(), radius = 24.dp)
                    content != null -> {
                        ProbabilityChartLines(
                            content = content,
                            scale = scale,
                            selectedTime = selectedTime,
                            onSelectTime = onSelectTime,
                            modifier = Modifier.matchParentSize().padding(vertical = labelHeight / 2),
                        )
                    }
                    else -> ProbabilityChartEmpty(emptyTitle, emptyDescription)
                }
            }
            if (isLoading) {
                Column(Modifier.width(axisWidth).fillMaxHeight(), verticalArrangement = Arrangement.SpaceBetween) {
                    repeat(4) { TangemShimmer(Modifier.fillMaxWidth().height(labelHeight), radius = 24.dp) }
                }
            } else {
                Canvas(Modifier.width(axisWidth).fillMaxHeight().clearAndSetSemantics {}) {
                    labels.forEachIndexed { index, label ->
                        val y = scale.y(scale.ticks[index]) * (size.height - labelHeight.toPx())
                        drawText(label, topLeft = Offset(0f, y))
                    }
                }
            }
        }
        ProbabilityChartDates(
            scale = scale.takeIf { content != null },
            selectedTime = selectedTime,
            formatTime = formatTime,
            isLoading = isLoading,
            modifier = Modifier.fillMaxWidth().padding(end = axisWidth + 8.dp),
        )
    }
}