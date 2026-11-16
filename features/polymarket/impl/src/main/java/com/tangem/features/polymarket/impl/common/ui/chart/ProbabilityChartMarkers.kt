@file:Suppress("MagicNumber")

package com.tangem.features.polymarket.impl.common.ui.chart

import android.graphics.Paint
import androidx.compose.runtime.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.patrykandpatrick.vico.core.cartesian.CartesianDrawContext
import com.patrykandpatrick.vico.core.cartesian.data.LineCartesianLayerModel
import com.patrykandpatrick.vico.core.cartesian.decoration.Decoration
import com.patrykandpatrick.vico.core.cartesian.layer.LineCartesianLayer
import com.patrykandpatrick.vico.core.cartesian.marker.CartesianMarker
import com.patrykandpatrick.vico.core.cartesian.marker.CartesianMarkerVisibilityListener
import com.patrykandpatrick.vico.core.cartesian.marker.LineCartesianLayerMarkerTarget
import com.tangem.core.ui.haptic.TangemHapticEffect
import com.tangem.core.ui.res.LocalHapticManager
import kotlin.math.abs

internal class LatestProbabilityPoints(
    private val layer: LineCartesianLayer,
    private val colors: List<Color>,
    private val pulse: State<Float>,
) : Decoration {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    override fun drawOverLayers(context: CartesianDrawContext) {
        val fraction = pulse.value
        context.lineSeries().forEachIndexed { index, series ->
            val center = layer.position(series.last()) ?: return@forEachIndexed
            if (fraction > 0f) {
                paint.color = colors[index].copy(alpha = (1f - fraction) * 0.5f).toArgb()
                context.canvas.drawCircle(center.x, center.y, with(context) { 9f.pixels } * fraction, paint)
            }
            paint.color = colors[index].toArgb()
            context.canvas.drawCircle(center.x, center.y, with(context) { 3f.pixels }, paint)
        }
    }
}

internal class ProbabilityMarker(
    private val layer: LineCartesianLayer,
    private val colors: List<Color>,
) : CartesianMarker {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    override fun draw(context: CartesianDrawContext, targets: List<CartesianMarker.Target>) {
        val target = targets.firstOrNull() ?: return
        context.lineSeries().forEachIndexed { index, series ->
            val position = layer.positionAt(series, target.x) ?: return@forEachIndexed
            paint.color = colors[index].toArgb()
            context.canvas.drawCircle(target.canvasX, position.y, with(context) { 3f.pixels }, paint)
        }
    }
}

private fun CartesianDrawContext.lineSeries(): List<List<LineCartesianLayerModel.Entry>> {
    val model = chartValues.model.models.firstOrNull() as? LineCartesianLayerModel
    return model?.series.orEmpty()
}

/** Uses Vico's drawn coordinates, including the current difference-animation frame. */
private fun LineCartesianLayer.position(entry: LineCartesianLayerModel.Entry): Offset? {
    val target = markerTargets[entry.x]?.firstOrNull() as? LineCartesianLayerMarkerTarget ?: return null
    val point = target.points.firstOrNull { it.entry === entry } ?: return null
    return Offset(target.canvasX, point.canvasY)
}

private fun LineCartesianLayer.positionAt(series: List<LineCartesianLayerModel.Entry>, x: Double): Offset? {
    if (x < series.first().x || x > series.last().x) return null
    val index = series.binarySearchBy(x) { it.x }
    if (index >= 0) return position(series[index])
    val lower = series[-index - 2]
    val upper = series[-index - 1]
    val start = position(lower) ?: return null
    val end = position(upper) ?: return null
    return start + (end - start) * ((x - lower.x) / (upper.x - lower.x)).toFloat()
}

@Composable
internal fun rememberProbabilityMarkerListener(onSelectTime: (Long?) -> Unit): CartesianMarkerVisibilityListener {
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