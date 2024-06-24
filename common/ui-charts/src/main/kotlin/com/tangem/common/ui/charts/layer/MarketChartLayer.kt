package com.tangem.common.ui.charts.layer

import android.content.res.Configuration
import androidx.annotation.FloatRange
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
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
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLineSpec
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberSplitLineSpec
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.compose.cartesian.rememberVicoZoomState
import com.patrykandpatrick.vico.compose.common.shader.BrushShader
import com.patrykandpatrick.vico.core.cartesian.HorizontalLayout
import com.patrykandpatrick.vico.core.cartesian.Zoom
import com.patrykandpatrick.vico.core.cartesian.data.AxisValueOverrider
import com.patrykandpatrick.vico.core.cartesian.data.CartesianChartModel
import com.patrykandpatrick.vico.core.cartesian.data.LineCartesianLayerModel
import com.patrykandpatrick.vico.core.cartesian.layer.LineCartesianLayer
import com.patrykandpatrick.vico.core.common.shader.ColorShader
import com.patrykandpatrick.vico.core.common.shader.DynamicShader
import com.tangem.common.ui.charts.preview.MarketChartPreviewDataProvider
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
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
    startDrawingAnimation: MutableState<Boolean>,
    axisValueOverrider: AxisValueOverrider,
    secondColorOnTheRightSide: Boolean,
    @FloatRange(from = 0.0, to = 1.0) markerFraction: Float?,
    canvasHeight: Int,
): LineCartesianLayer {
    var animationFraction: Float? by remember { mutableStateOf(null) }

    LaunchedEffect(startDrawingAnimation.value) {
        animationFraction = null
        if (startDrawingAnimation.value) {
            animate(
                initialValue = 0f,
                targetValue = 1f,
                animationSpec = tween(easing = LinearEasing, durationMillis = 1000),
            ) { start, _ ->
                if (start == 1f) {
                    animationFraction = null
                    startDrawingAnimation.value = false
                } else {
                    animationFraction = start
                }
            }
        }
    }

    return rememberRawMarketChartLayer(
        lineColor = lineColor,
        backgroundLineColor = backgroundLineColor,
        secondLineColor = secondLineColor,
        backgroundSecondLineColor = backgroundSecondLineColor,
        axisValueOverrider = axisValueOverrider,
        secondColorOnTheRightSide = secondColorOnTheRightSide,
        markerFraction = markerFraction,
        animationFraction = animationFraction,
        canvasHeight = canvasHeight,
    )
}

@Suppress("LongParameterList")
@Composable
private fun rememberRawMarketChartLayer(
    lineColor: Color,
    backgroundLineColor: Color,
    secondLineColor: Color,
    backgroundSecondLineColor: Color,
    axisValueOverrider: AxisValueOverrider,
    canvasHeight: Int,
    secondColorOnTheRightSide: Boolean = false,
    @FloatRange(from = 0.0, to = 1.0) markerFraction: Float? = null,
    @FloatRange(from = 0.0, to = 1.0) animationFraction: Float? = null,
): LineCartesianLayer {
    val backgroundColorLineGradient = listOf(backgroundLineColor, Color.Transparent)
    val backgroundSecondLineColorGradient = listOf(backgroundSecondLineColor, Color.Transparent)

    val markerSet = markerFraction != null
    val animationRunning = animationFraction != null && animationFraction != 1f

    val layerColors = when {
        !animationRunning && markerSet && secondColorOnTheRightSide -> {
            LayerColors(
                lineColor = lineColor,
                backLineColor = backgroundColorLineGradient,
                lineColorRight = secondLineColor,
                backLineColorRight = backgroundSecondLineColorGradient,
            )
        }
        !animationRunning && markerSet && !secondColorOnTheRightSide -> {
            LayerColors(
                lineColor = secondLineColor,
                backLineColor = backgroundSecondLineColorGradient,
                lineColorRight = lineColor,
                backLineColorRight = backgroundColorLineGradient,
            )
        }
        animationRunning -> {
            LayerColors(
                lineColor = lineColor,
                backLineColor = backgroundColorLineGradient,
                lineColorRight = Color.Transparent,
                backLineColorRight = listOf(Color.Transparent, Color.Transparent),
            )
        }
        else -> {
            LayerColors(
                lineColor = lineColor,
                backLineColor = backgroundColorLineGradient,
            )
        }
    }

    return rememberLayer(
        fractionValue = animationFraction ?: markerFraction,
        axisValueOverrider = axisValueOverrider,
        layerColors = layerColors,
        canvasHeight = canvasHeight,
    )
}

private data class LayerColors(
    val lineColor: Color,
    val backLineColor: List<Color>,
    val lineColorRight: Color? = null,
    val backLineColorRight: List<Color>? = null,
)

@Composable
private fun rememberLayer(
    fractionValue: Float?,
    axisValueOverrider: AxisValueOverrider,
    layerColors: LayerColors,
    canvasHeight: Int,
): LineCartesianLayer {
    val endGradientColorPosition = if (canvasHeight != 0) {
        canvasHeight * END_GRADIENT_COLOR_POSITION_PERCENTAGE
    } else {
        Float.POSITIVE_INFINITY
    }

    return rememberLineCartesianLayer(
        listOf(
            if (layerColors.lineColorRight == null || layerColors.backLineColorRight == null || fractionValue == null) {
                rememberLineSpec(
                    shader = remember(layerColors.lineColor) { ColorShader(color = layerColors.lineColor.toArgb()) },
                    backgroundShader = remember(layerColors.backLineColor, endGradientColorPosition) {
                        BrushShader(
                            brush = Brush.verticalGradient(
                                colors = layerColors.backLineColor,
                                endY = endGradientColorPosition,
                            ),
                        )
                    },
                )
            } else {
                rememberSplitLineSpec(
                    shader = remember(layerColors.lineColor, layerColors.lineColorRight, fractionValue) {
                        DynamicShader.Companion.horizontalGradient(
                            colors = intArrayOf(layerColors.lineColor.toArgb(), layerColors.lineColorRight.toArgb()),
                            positions = floatArrayOf(fractionValue, fractionValue),
                        )
                    },
                    backgroundShaderFirst = remember(layerColors.backLineColor, endGradientColorPosition) {
                        BrushShader(
                            brush = Brush.verticalGradient(
                                colors = layerColors.backLineColor,
                                endY = endGradientColorPosition,
                            ),
                        )
                    },
                    backgroundShaderSecond = remember(layerColors.backLineColorRight, endGradientColorPosition) {
                        BrushShader(
                            brush = Brush.verticalGradient(
                                colors = layerColors.backLineColorRight,
                                endY = endGradientColorPosition,
                            ),
                        )
                    },
                    xSplitFraction = fractionValue,
                )
            },
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
                    rememberRawMarketChartLayer(
                        lineColor = lineColor,
                        backgroundLineColor = lineColor.copy(alpha = 0.24f),
                        secondLineColor = Color.Gray,
                        backgroundSecondLineColor = Color.Gray.copy(alpha = 0.24f),
                        secondColorOnTheRightSide = true,
                        axisValueOverrider = AxisValueOverrider.fixed(minY = model.models[0].minY),
                        canvasHeight = 495,
                    ),
                ),
                zoomState = rememberVicoZoomState(initialZoom = Zoom.Content, zoomEnabled = false),
                horizontalLayout = HorizontalLayout.fullWidth(),
                model = model,
            )

            CartesianChartHost(
                modifier = Modifier.fillMaxWidth(),
                chart = rememberCartesianChart(
                    rememberRawMarketChartLayer(
                        lineColor = lineColor,
                        backgroundLineColor = lineColor.copy(alpha = 0.24f),
                        secondLineColor = Color.Gray,
                        backgroundSecondLineColor = Color.Gray.copy(alpha = 0.24f),
                        markerFraction = 0.35f,
                        secondColorOnTheRightSide = true,
                        axisValueOverrider = AxisValueOverrider.fixed(minY = model.models[0].minY),
                        canvasHeight = 495,
                    ),
                ),
                zoomState = rememberVicoZoomState(initialZoom = Zoom.Content, zoomEnabled = false),
                horizontalLayout = HorizontalLayout.fullWidth(),
                model = model,
            )

            CartesianChartHost(
                modifier = Modifier.fillMaxWidth(),
                chart = rememberCartesianChart(
                    rememberRawMarketChartLayer(
                        lineColor = lineColor,
                        backgroundLineColor = lineColor.copy(alpha = 0.24f),
                        secondLineColor = Color.Gray,
                        backgroundSecondLineColor = Color.Gray.copy(alpha = 0.24f),
                        markerFraction = 0.35f,
                        secondColorOnTheRightSide = false,
                        axisValueOverrider = AxisValueOverrider.fixed(minY = model.models[0].minY),
                        canvasHeight = 495,
                    ),
                ),
                zoomState = rememberVicoZoomState(initialZoom = Zoom.Content, zoomEnabled = false),
                horizontalLayout = HorizontalLayout.fullWidth(),
                model = model,
            )

            CartesianChartHost(
                modifier = Modifier.fillMaxWidth(),
                chart = rememberCartesianChart(
                    rememberRawMarketChartLayer(
                        lineColor = lineColor,
                        backgroundLineColor = lineColor.copy(alpha = 0.24f),
                        secondLineColor = lineColor,
                        backgroundSecondLineColor = lineColor.copy(alpha = 0.24f),
                        markerFraction = 0.35f,
                        secondColorOnTheRightSide = true,
                        animationFraction = 0.7f,
                        axisValueOverrider = AxisValueOverrider.fixed(minY = model.models[0].minY),
                        canvasHeight = 495,
                    ),
                ),
                zoomState = rememberVicoZoomState(initialZoom = Zoom.Content, zoomEnabled = false),
                horizontalLayout = HorizontalLayout.fullWidth(),
                model = model,
            )
        }
    }
}

// endregion Preview