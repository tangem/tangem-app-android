package com.tangem.common.ui.charts.layer

import android.content.res.Configuration
import androidx.annotation.FloatRange
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.fullWidth
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberSplitLine
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.compose.cartesian.rememberVicoZoomState
import com.patrykandpatrick.vico.compose.common.shader.toDynamicShader
import com.patrykandpatrick.vico.core.cartesian.HorizontalLayout
import com.patrykandpatrick.vico.core.cartesian.Zoom
import com.patrykandpatrick.vico.core.cartesian.data.AxisValueOverrider
import com.patrykandpatrick.vico.core.cartesian.data.CartesianChartModel
import com.patrykandpatrick.vico.core.cartesian.data.LineCartesianLayerModel
import com.patrykandpatrick.vico.core.cartesian.layer.LineCartesianLayer
import com.patrykandpatrick.vico.core.common.shader.DynamicShader
import com.tangem.common.ui.charts.preview.MarketChartPreviewDataProvider
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import java.math.BigDecimal

/**
 * Creates and remembers a LineCartesianLayer for a chart with specific characteristics.
 *
 * @param lineColor The color of the main line in the chart.
 * @param backgroundLineColor The color of the line's background.
 * @param secondLineColor The color of the line for the second part of the chart.
 * @param backgroundSecondLineColor The color of the line's background for the second part of the chart.
 * @param startDrawingAnimation A mutable state that triggers the start of the drawing animation when set to true.
 * @param axisValueOverrider An AxisValueOverrider that provides custom values for the axis.
 * @param secondColorOnTheRightSide A boolean that determines if the second color should be on the right side of the chart. Default is false.
 * @param markerFraction A float between 0.0 and 1.0 that represents the fraction of the chart where the marker is located. Default is null.
 *
 * @return A LineCartesianLayer that represents a layer in a chart with the specified characteristics.
 */
@Suppress("LongParameterList")
@Composable
internal fun rememberMarketChartLayer(
    lineColor: Color,
    backgroundLineColor: Color,
    secondLineColor: Color,
    backgroundSecondLineColor: Color,
    axisValueOverrider: AxisValueOverrider,
    secondColorOnTheRightSide: Boolean,
    @FloatRange(from = 0.0, to = 1.0) markerFraction: Float?,
    canvasHeight: Int,
): LineCartesianLayer {
    val backgroundColorLineGradient = persistentListOf(backgroundLineColor, Color.Transparent)
    val backgroundSecondLineColorGradient = persistentListOf(backgroundSecondLineColor, Color.Transparent)

    val markerSet = markerFraction != null

    return rememberLayer(
        fractionValue = markerFraction ?: 0f,
        axisValueOverrider = axisValueOverrider,
        canvasHeight = canvasHeight,
        lineColor = if (markerFraction != null) {
            secondLineColor
        } else {
            lineColor
        },
        backLineColor = if (markerSet && !secondColorOnTheRightSide) {
            backgroundSecondLineColorGradient
        } else {
            backgroundColorLineGradient
        },
        lineColorRight = when {
            markerSet && secondColorOnTheRightSide -> secondLineColor
            else -> lineColor
        },
        backLineColorRight = when {
            markerSet && secondColorOnTheRightSide -> backgroundSecondLineColorGradient
            else -> backgroundColorLineGradient
        },
    )
}

@Suppress("LongParameterList")
@Composable
private fun rememberLayer(
    fractionValue: Float,
    axisValueOverrider: AxisValueOverrider,
    lineColor: Color,
    backLineColor: ImmutableList<Color>,
    lineColorRight: Color,
    backLineColorRight: ImmutableList<Color>,
    canvasHeight: Int,
): LineCartesianLayer {
    val endGradientColorPosition = if (canvasHeight != 0) {
        canvasHeight * END_GRADIENT_COLOR_POSITION_PERCENTAGE
    } else {
        Float.POSITIVE_INFINITY
    }

    val alineColor = remember(lineColor) { lineColor.toArgb() }
    val alineColorRight = remember(lineColorRight) { lineColorRight.toArgb() }

    return rememberLineCartesianLayer(
        LineCartesianLayer.LineProvider.series(
            rememberSplitLine(
                shader = DynamicShader.Companion.horizontalGradient(
                    colors = intArrayOf(alineColor, alineColorRight),
                    positions = floatArrayOf(fractionValue, fractionValue),
                ),
                backgroundShaderFirst = Brush.verticalGradient(
                    colors = backLineColor,
                    endY = endGradientColorPosition,
                ).toDynamicShader(),
                backgroundShaderSecond = Brush.verticalGradient(
                    colors = backLineColorRight,
                    endY = endGradientColorPosition,
                ).toDynamicShader(),
                xSplitFraction = fractionValue,
                thickness = 1.dp,
            ),
        ),
        axisValueOverrider = axisValueOverrider,
    )
}

private const val END_GRADIENT_COLOR_POSITION_PERCENTAGE = 0.9f

// region Preview

@Suppress("LongMethod")
@Preview(showBackground = true, widthDp = 360)
@Preview(showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun LayerChartPreview(
    @PreviewParameter(MarketChartPreviewDataProvider::class) previewData: Pair<List<BigDecimal>, List<BigDecimal>>,
) {
    val y = previewData.second.map { it.toFloat() }
    val x = List(y.size) { it.toFloat() }
    val model = CartesianChartModel(LineCartesianLayerModel.build { series(x, y) })
    var lineColor by remember {
        mutableStateOf(Color.Blue)
    }

    TangemThemePreview {
        Column(
            modifier = Modifier.background(TangemTheme.colors.background.primary),
            verticalArrangement = Arrangement.spacedBy(48.dp),
        ) {
            CartesianChartHost(
                modifier = Modifier.fillMaxWidth(),
                chart = rememberCartesianChart(
                    rememberMarketChartLayer(
                        lineColor = lineColor,
                        backgroundLineColor = lineColor.copy(alpha = 0.24f),
                        secondLineColor = Color.Gray,
                        backgroundSecondLineColor = Color.Gray.copy(alpha = 0.24f),
                        secondColorOnTheRightSide = true,
                        axisValueOverrider = AxisValueOverrider.fixed(minY = model.models[0].minY),
                        markerFraction = 0.35f,
                        canvasHeight = 495,
                    ),
                    horizontalLayout = HorizontalLayout.fullWidth(),
                ),
                zoomState = rememberVicoZoomState(initialZoom = Zoom.Content, zoomEnabled = false),
                model = model,
            )

            CartesianChartHost(
                modifier = Modifier.fillMaxWidth(),
                chart = rememberCartesianChart(
                    rememberMarketChartLayer(
                        lineColor = lineColor,
                        backgroundLineColor = lineColor.copy(alpha = 0.24f),
                        secondLineColor = Color.Gray,
                        backgroundSecondLineColor = Color.Gray.copy(alpha = 0.24f),
                        markerFraction = 0.35f,
                        secondColorOnTheRightSide = true,
                        axisValueOverrider = AxisValueOverrider.fixed(minY = model.models[0].minY),
                        canvasHeight = 495,
                    ),
                    horizontalLayout = HorizontalLayout.fullWidth(),
                ),
                zoomState = rememberVicoZoomState(initialZoom = Zoom.Content, zoomEnabled = false),
                model = model,
            )

            CartesianChartHost(
                modifier = Modifier.fillMaxWidth(),
                chart = rememberCartesianChart(
                    rememberMarketChartLayer(
                        lineColor = lineColor,
                        backgroundLineColor = lineColor.copy(alpha = 0.24f),
                        secondLineColor = Color.Gray,
                        backgroundSecondLineColor = Color.Gray.copy(alpha = 0.24f),
                        markerFraction = 0.35f,
                        secondColorOnTheRightSide = false,
                        axisValueOverrider = AxisValueOverrider.fixed(minY = model.models[0].minY),
                        canvasHeight = 495,
                    ),
                    horizontalLayout = HorizontalLayout.fullWidth(),
                ),
                zoomState = rememberVicoZoomState(initialZoom = Zoom.Content, zoomEnabled = false),
                model = model,
            )
        }
    }
}

// endregion Preview
