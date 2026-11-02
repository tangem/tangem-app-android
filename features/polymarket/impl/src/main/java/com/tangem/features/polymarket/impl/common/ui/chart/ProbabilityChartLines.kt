@file:Suppress("MagicNumber")

package com.tangem.features.polymarket.impl.common.ui.chart

import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.compose.cartesian.rememberVicoScrollState
import com.patrykandpatrick.vico.compose.cartesian.rememberVicoZoomState
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberSplitLine
import com.patrykandpatrick.vico.core.cartesian.HorizontalLayout
import com.patrykandpatrick.vico.core.cartesian.Zoom
import com.patrykandpatrick.vico.core.cartesian.data.AxisValueOverrider
import com.patrykandpatrick.vico.core.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.core.cartesian.data.lineSeries
import com.patrykandpatrick.vico.core.cartesian.layer.LineCartesianLayer
import com.tangem.common.ui.charts.layer.SplitLineShader
import com.tangem.features.polymarket.impl.common.ui.chart.PolymarketProbabilityChart.Series
import com.tangem.features.polymarket.impl.common.ui.chart.PolymarketProbabilityChart.State.Content

@Composable
internal fun ProbabilityChartLines(
    content: Content,
    scale: ProbabilityChartScale,
    selectedTime: Long?,
    onSelectTime: (Long?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val series = remember(content) { content.series.filter { it.points.isNotEmpty() } }
    val colors = series.map { it.color.lineColor() }
    val producer = remember { CartesianChartModelProducer() }
    val currentSeries = rememberUpdatedState(series)
    LaunchedEffect(producer) {
        producer.collectProbabilitySeries(currentSeries)
    }
    val fraction = selectedTime?.let(scale::x) ?: 1f
    val connector = remember { LineCartesianLayer.PointConnector.cubic(curvature = 0f) }
    val lines = colors.map { color ->
        rememberSplitLine(
            shader = SplitLineShader(color.toArgb(), color.copy(alpha = 0.3f).toArgb(), fraction),
            thickness = 1.5.dp,
            backgroundShaderFirst = null,
            backgroundShaderSecond = null,
            xSplitFraction = fraction,
            pointConnector = connector,
        )
    }
    val layer = rememberLineCartesianLayer(
        lineProvider = LineCartesianLayer.LineProvider.series(lines),
        drawingModelInterpolator = remember { ProbabilityDrawingModelInterpolator() },
        axisValueOverrider = remember(scale) {
            AxisValueOverrider.fixed(
                minX = scale.start.toDouble(),
                maxX = scale.end.toDouble(),
                minY = scale.min,
                maxY = scale.max,
            )
        },
    )
    val marker = remember(layer, colors) { ProbabilityMarker(layer, colors) }
    val pulse = rememberChartPulse(content.latestMarker == PolymarketProbabilityChart.LatestMarker.Pulsing)
    val latestPoints = remember(layer, colors, pulse) { LatestProbabilityPoints(layer, colors, pulse) }
    val listener = rememberProbabilityMarkerListener(onSelectTime)
    val isLatestVisible =
        selectedTime == null && content.latestMarker != PolymarketProbabilityChart.LatestMarker.Hidden
    val chart = rememberCartesianChart(
        layer,
        marker = marker,
        markerVisibilityListener = listener,
        horizontalLayout = HorizontalLayout.FullWidth(),
        decorations = if (isLatestVisible) listOf(latestPoints) else emptyList(),
    )
    CartesianChartHost(
        chart = chart,
        modelProducer = producer,
        runInitialAnimation = false,
        modifier = modifier,
        scrollState = rememberVicoScrollState(scrollEnabled = false),
        zoomState = rememberVicoZoomState(initialZoom = Zoom.Content, zoomEnabled = false),
    )
}

internal suspend fun CartesianChartModelProducer.collectProbabilitySeries(series: State<List<Series>>) {
    // Cancelling an in-flight commit can leave this Vico fork's update mutex locked.
    snapshotFlow { series.value }.collect { snapshot ->
        runTransaction {
            lineSeries {
                snapshot.forEach { line ->
                    series(line.points.map { it.timestampMillis }, line.points.map { it.probability })
                }
            }
        }
    }
}