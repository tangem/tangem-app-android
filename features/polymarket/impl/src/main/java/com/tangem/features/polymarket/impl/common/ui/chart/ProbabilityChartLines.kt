@file:Suppress("MagicNumber")

package com.tangem.features.polymarket.impl.common.ui.chart

import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.compose.cartesian.rememberVicoScrollState
import com.patrykandpatrick.vico.compose.cartesian.rememberVicoZoomState
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberSplitLine
import com.patrykandpatrick.vico.core.cartesian.CartesianDrawContext
import com.patrykandpatrick.vico.core.cartesian.HorizontalLayout
import com.patrykandpatrick.vico.core.cartesian.Zoom
import com.patrykandpatrick.vico.core.cartesian.data.AxisValueOverrider
import com.patrykandpatrick.vico.core.cartesian.data.CartesianChartModel
import com.patrykandpatrick.vico.core.cartesian.data.LineCartesianLayerModel
import com.patrykandpatrick.vico.core.cartesian.layer.LineCartesianLayer
import com.patrykandpatrick.vico.core.cartesian.marker.CartesianMarker
import com.patrykandpatrick.vico.core.cartesian.marker.CartesianMarkerVisibilityListener
import com.tangem.common.ui.charts.layer.SplitLineShader
import com.tangem.core.ui.haptic.TangemHapticEffect
import com.tangem.core.ui.res.LocalHapticManager
import com.tangem.features.polymarket.impl.common.ui.chart.PolymarketProbabilityChart.Series
import com.tangem.features.polymarket.impl.common.ui.chart.PolymarketProbabilityChart.State.Content
import kotlin.math.abs

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
    val model = remember(series) {
        CartesianChartModel(
            LineCartesianLayerModel.build {
                series.forEach { line ->
                    series(line.points.map { it.timestampMillis }, line.points.map { it.probability })
                }
            },
        )
    }
    val fraction = selectedTime?.let(scale::x) ?: 1f
    val lines = colors.map { color ->
        rememberSplitLine(
            shader = SplitLineShader(color.toArgb(), color.copy(alpha = 0.3f).toArgb(), fraction),
            thickness = 1.5.dp,
            backgroundShaderFirst = null,
            backgroundShaderSecond = null,
            xSplitFraction = fraction,
        )
    }
    val layer = rememberLineCartesianLayer(
        lineProvider = LineCartesianLayer.LineProvider.series(lines),
        axisValueOverrider = remember(scale) {
            AxisValueOverrider.fixed(
                minX = scale.start.toDouble(),
                maxX = scale.end.toDouble(),
                minY = scale.min,
                maxY = scale.max,
            )
        },
    )
    val marker = remember(series, colors, scale) {
        ProbabilityMarker(series, colors, scale)
    }
    val listener = rememberProbabilityMarkerListener(onSelectTime)
    val chart = rememberCartesianChart(
        layer,
        marker = marker,
        markerVisibilityListener = listener,
        horizontalLayout = HorizontalLayout.FullWidth(),
    )
    Box(modifier) {
        CartesianChartHost(
            chart = chart,
            model = model,
            modifier = Modifier.matchParentSize(),
            scrollState = rememberVicoScrollState(scrollEnabled = false),
            zoomState = rememberVicoZoomState(initialZoom = Zoom.Content, zoomEnabled = false),
        )
        if (selectedTime == null && content.latestMarker != PolymarketProbabilityChart.LatestMarker.Hidden) {
            LatestProbabilityPoints(
                series = series,
                colors = colors,
                scale = scale,
                isPulsing = content.latestMarker == PolymarketProbabilityChart.LatestMarker.Pulsing,
                modifier = Modifier.matchParentSize(),
            )
        }
    }
}

@Composable
private fun LatestProbabilityPoints(
    series: List<Series>,
    colors: List<Color>,
    scale: ProbabilityChartScale,
    isPulsing: Boolean,
    modifier: Modifier = Modifier,
) {
    val pulse = rememberChartPulse(isPulsing)
    Canvas(modifier) {
        series.forEachIndexed { index, line ->
            val last = line.points.last()
            val center = Offset(scale.x(last.timestampMillis) * size.width, scale.y(last.probability) * size.height)
            if (pulse.value > 0f) {
                drawCircle(colors[index].copy(alpha = (1f - pulse.value) * 0.5f), 9.dp.toPx() * pulse.value, center)
            }
            drawCircle(colors[index], 3.dp.toPx(), center)
        }
    }
}

private class ProbabilityMarker(
    private val series: List<Series>,
    private val colors: List<Color>,
    private val scale: ProbabilityChartScale,
) : CartesianMarker {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    override fun draw(context: CartesianDrawContext, targets: List<CartesianMarker.Target>) {
        val target = targets.firstOrNull() ?: return
        series.forEachIndexed { index, line ->
            val value = line.points.probabilityAt(target.x.toLong()) ?: return@forEachIndexed
            paint.color = colors[index].toArgb()
            val y = context.layerBounds.top + scale.y(value) * context.layerBounds.height()
            context.canvas.drawCircle(target.canvasX, y, with(context) { 3f.pixels }, paint)
        }
    }
}

@Composable
private fun rememberProbabilityMarkerListener(onSelectTime: (Long?) -> Unit): CartesianMarkerVisibilityListener {
    val onSelect by rememberUpdatedState(onSelectTime)
    val haptic = LocalHapticManager.current
    val spacing = with(LocalDensity.current) { 8.dp.toPx() }
    return remember(haptic, spacing) {
        object : CartesianMarkerVisibilityListener {
            private var lastHapticX: Float? = null

            override fun onShown(marker: CartesianMarker, targets: List<CartesianMarker.Target>) {
                onUpdated(marker, targets)
            }

            override fun onUpdated(marker: CartesianMarker, targets: List<CartesianMarker.Target>) {
                val target = targets.firstOrNull() ?: return
                onSelect(target.x.toLong())
                if (lastHapticX == null || abs(target.canvasX - requireNotNull(lastHapticX)) >= spacing) {
                    lastHapticX = target.canvasX
                    haptic.perform(TangemHapticEffect.View.TextHandleMove)
                }
            }

            override fun onHidden(marker: CartesianMarker) {
                lastHapticX = null
                onSelect(null)
            }
        }
    }
}