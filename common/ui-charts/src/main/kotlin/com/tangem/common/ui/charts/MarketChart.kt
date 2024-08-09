package com.tangem.common.ui.charts

import android.content.res.Configuration
import androidx.annotation.FloatRange
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFontFamilyResolver
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontSynthesis
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.resolveAsTypeface
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberAxisGuidelineComponent
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberAxisLabelComponent
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberBottomAxis
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberCustomStartAxis
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.compose.cartesian.rememberVicoScrollState
import com.patrykandpatrick.vico.compose.cartesian.rememberVicoZoomState
import com.patrykandpatrick.vico.compose.common.of
import com.patrykandpatrick.vico.core.cartesian.HorizontalLayout
import com.patrykandpatrick.vico.core.cartesian.Zoom
import com.patrykandpatrick.vico.core.cartesian.axis.AxisPosition
import com.patrykandpatrick.vico.core.cartesian.axis.BaseAxis
import com.patrykandpatrick.vico.core.cartesian.axis.HorizontalAxis
import com.patrykandpatrick.vico.core.cartesian.axis.VerticalAxis
import com.patrykandpatrick.vico.core.cartesian.data.AxisValueOverrider
import com.patrykandpatrick.vico.core.cartesian.data.CartesianValueFormatter
import com.patrykandpatrick.vico.core.cartesian.marker.CartesianMarker
import com.patrykandpatrick.vico.core.cartesian.marker.CartesianMarkerVisibilityListener
import com.patrykandpatrick.vico.core.cartesian.marker.LineCartesianLayerMarkerTarget
import com.patrykandpatrick.vico.core.common.Dimensions
import com.patrykandpatrick.vico.core.common.component.LineComponent
import com.patrykandpatrick.vico.core.common.shape.Shape
import com.tangem.common.ui.charts.layer.TimeItemPlacer
import com.tangem.common.ui.charts.layer.rememberMarketChartLayer
import com.tangem.common.ui.charts.layer.rememberTangemChartMarker
import com.tangem.common.ui.charts.preview.MarketChartPreviewDataProvider
import com.tangem.common.ui.charts.state.*
import com.tangem.core.ui.components.SpacerH16
import com.tangem.core.ui.haptic.TangemHapticEffect
import com.tangem.core.ui.res.LocalHapticManager
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.core.ui.utils.DateTimeFormatters
import com.tangem.core.ui.utils.toTimeFormat
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.math.RoundingMode

private const val GUIDELINES_COUNT = 3

/**
 * MarketChart ui component for representing coin prices.
 *
 * @param modifier The modifier to be applied to the chart.
 * @param state The state of the Market Chart, which includes data and look of the chart.
 * @param splitChartSegmentColor The color of the grayed by marker chart segment.
 * @param backgroundSplitChartSegmentColorAlpha The alpha of the background the [splitChartSegmentColor]
 * @param backgroundColorAlpha The alpha of the background color of the chart.
 */
@Composable
fun MarketChart(
    modifier: Modifier = Modifier,
    state: MarketChartState = rememberMarketChartState(),
    splitChartSegmentColor: Color = TangemTheme.colors.icon.inactive,
    @FloatRange(from = 0.0, to = 1.0) backgroundSplitChartSegmentColorAlpha: Float = 0.24f,
    @FloatRange(from = 0.0, to = 1.0) backgroundColorAlpha: Float = 0.24f,
) {
    var canvasWidth by remember { mutableIntStateOf(0) }
    var chartHeight by remember { mutableIntStateOf(0) }

    val layer = rememberMarketChartLayer(
        lineColor = state.chartColor,
        backgroundLineColor = state.chartColor.copy(alpha = backgroundColorAlpha),
        secondLineColor = splitChartSegmentColor,
        backgroundSecondLineColor = splitChartSegmentColor.copy(alpha = backgroundSplitChartSegmentColorAlpha),
        secondColorOnTheRightSide = state.markerHighlightRightSide.not(),
        markerFraction = state.markerFraction,
        axisValueOverrider = AxisValueOverrider.fixed(),
        canvasHeight = chartHeight,
    )

    val marker = rememberTangemChartMarker(color = state.chartColor)

    val chart = rememberCartesianChart(
        layer,
        startAxis = rememberMarketChartStartAxis(state.yValueFormatter),
        bottomAxis = rememberMarketChartBottomAxis(state.xValueFormatter),
        horizontalLayout = HorizontalLayout.FullWidth(),
        markerVisibilityListener = rememberMarketVisibilityListener(canvasWidth, state),
        marker = marker,
    )

    // we need to calculate what the overall height should be in order to get the correct height of the graph
    val bottomAxisHeight = with(LocalDensity.current) {
        getMarketChartBottomAxisHeight().toPx().toInt()
    }

    CartesianChartHost(
        modifier = modifier
            .onGloballyPositioned {
                canvasWidth = it.size.width
                chartHeight = if (it.size.height != 0) {
                    it.size.height - bottomAxisHeight
                } else {
                    0
                }
            }
            // Sometimes the chart is not drawn correctly (ex. in LazyLayout), so we need to force the redraw
            .drawBehind {
                state.markerFraction
            },
        chart = chart,
        modelProducer = state.modelProducer,
        scrollState = rememberVicoScrollState(scrollEnabled = false),
        zoomState = rememberVicoZoomState(initialZoom = Zoom.Content, zoomEnabled = false),
        animationSpec = null,
    )
}

@Composable
fun getMarketChartBottomAxisHeight(): Dp {
    return with(LocalDensity.current) {
        TangemTheme.typography.caption2.fontSize.toDp() + TangemTheme.dimens.spacing26
    }
}

@Composable
private fun rememberMarketVisibilityListener(
    canvasWidth: Int,
    state: MarketChartState,
): CartesianMarkerVisibilityListener {
    val haptic = LocalHapticManager.current

    return remember(state, canvasWidth) {
        val maxCanvasXFloat = canvasWidth.toFloat().takeIf { it != 0f }

        object : CartesianMarkerVisibilityListener {
            override fun onShown(marker: CartesianMarker, targets: List<CartesianMarker.Target>) {
                val xCanvas = (targets[0] as LineCartesianLayerMarkerTarget).canvasX

                state.markerFraction = maxCanvasXFloat?.let { xCanvas / it }
                state.markerVisibilityListener.onShown(marker, targets)

                haptic.perform(TangemHapticEffect.View.ContextClick)
            }

            override fun onHidden(marker: CartesianMarker) {
                state.markerFraction = null
                state.markerVisibilityListener.onHidden(marker)
            }

            override fun onUpdated(marker: CartesianMarker, targets: List<CartesianMarker.Target>) {
                val xCanvas = (targets[0] as LineCartesianLayerMarkerTarget).canvasX

                state.markerFraction = maxCanvasXFloat?.let { xCanvas / it }
                state.markerVisibilityListener.onUpdated(marker, targets)

                haptic.perform(TangemHapticEffect.View.TextHandleMove)
            }
        }
    }
}

@Composable
private fun rememberMarketChartStartAxis(
    yValueFormatter: CartesianValueFormatter,
): VerticalAxis<AxisPosition.Vertical.Start> {
    val textStyle = TangemTheme.typography.caption2
    val resolver = LocalFontFamilyResolver.current
    val typeface by remember(resolver, textStyle) {
        resolver.resolveAsTypeface(
            fontFamily = textStyle.fontFamily,
            fontWeight = textStyle.fontWeight ?: FontWeight.Normal,
            fontStyle = textStyle.fontStyle ?: FontStyle.Normal,
            fontSynthesis = textStyle.fontSynthesis ?: FontSynthesis.All,
        )
    }

    return rememberCustomStartAxis(
        line = null,
        tick = null,
        guideline = null,
        labelGuideline = rememberChartAxisGuidelineComponent(
            color = TangemTheme.colors.icon.inactive.copy(alpha = 0.12f),
        ),
        label = rememberAxisLabelComponent(
            color = TangemTheme.colors.text.tertiary,
            background = null,
            padding = Dimensions.of(
                start = TangemTheme.dimens.spacing4,
                end = TangemTheme.dimens.spacing4,
            ),
            textSize = TangemTheme.typography.caption2.fontSize,
            typeface = typeface,
        ),
        horizontalLabelPosition = VerticalAxis.HorizontalLabelPosition.Inside,
        verticalLabelPosition = VerticalAxis.VerticalLabelPosition.Center,
        itemPlacer = VerticalAxis.ItemPlacer.count({ GUIDELINES_COUNT }, false),
        valueFormatter = yValueFormatter,
    )
}

@Composable
private fun rememberMarketChartBottomAxis(
    xValueFormatter: CartesianValueFormatter,
): HorizontalAxis<AxisPosition.Horizontal.Bottom> {
    val textStyle = TangemTheme.typography.caption2

    val resolver = LocalFontFamilyResolver.current

    val typeface by remember(resolver, textStyle) {
        resolver.resolveAsTypeface(
            fontFamily = textStyle.fontFamily,
            fontWeight = textStyle.fontWeight ?: FontWeight.Normal,
            fontStyle = textStyle.fontStyle ?: FontStyle.Normal,
            fontSynthesis = textStyle.fontSynthesis ?: FontSynthesis.All,
        )
    }

    return rememberBottomAxis(
        label = rememberAxisLabelComponent(
            color = TangemTheme.colors.text.tertiary,
            textSize = TangemTheme.typography.caption2.fontSize,
            padding = Dimensions.of(top = TangemTheme.dimens.spacing26),
            typeface = typeface,
        ),
        tick = null,
        line = null,
        guideline = null,
        sizeConstraint = BaseAxis.SizeConstraint.Auto(),
        itemPlacer = remember { TimeItemPlacer() },
        valueFormatter = xValueFormatter,
    )
}

@Composable
private fun rememberChartAxisGuidelineComponent(color: Color): LineComponent {
    return rememberAxisGuidelineComponent(
        color = color,
        shape = Shape.Rectangle,
        margins = Dimensions(
            startDp = TangemTheme.dimens.spacing4.value,
            endDp = TangemTheme.dimens.spacing4.value,
            topDp = 0f,
            bottomDp = 0f,
        ),
        thickness = TangemTheme.dimens.size2,
    )
}

// region Preview

@Suppress("LongMethod")
@Preview(showBackground = true, widthDp = 360)
@Preview(showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun MarketChartPreview(
    @PreviewParameter(MarketChartPreviewDataProvider::class) previewData: Pair<List<BigDecimal>, List<BigDecimal>>,
) {
    val y = previewData.second
    val x = previewData.first

    val dataProducer = remember {
        MarketChartDataProducer.build {
            chartLook = MarketChartLook(
                type = MarketChartLook.Type.Growing,
                markerHighlightRightSide = true,
            )
        }
    }

    LaunchedEffect(key1 = Unit) {
        dataProducer.runTransactionSuspend {
            chartData = MarketChartData.Data(
                x = x.toImmutableList(),
                y = y.toImmutableList(),
            )
            updateLook {
                it.copy(
                    xAxisFormatter = { value ->
                        value.toLong().toTimeFormat(DateTimeFormatters.dateMMMdd)
                    },
                    yAxisFormatter = { value ->
                        value.setScale(3, RoundingMode.HALF_UP).toPlainString()
                    },
                )
            }
        }
    }
    var markerPoint by remember {
        mutableStateOf(Pair<BigDecimal?, BigDecimal?>(null, null))
    }

    val coroutineScope = rememberCoroutineScope()
    val look by dataProducer.lookState.collectAsState()

    TangemThemePreview {
        val growingColor = TangemTheme.colors.icon.accent
        val fallingColor = TangemTheme.colors.icon.warning

        val chartState = rememberMarketChartState(
            dataProducer = dataProducer,
            onMarkerShown = { x, y ->
                markerPoint = Pair(x, y)
            },
            colorMapper = {
                when (it) {
                    MarketChartLook.Type.Growing -> growingColor
                    MarketChartLook.Type.Falling -> fallingColor
                }
            },
        )

        Column(
            modifier = Modifier
                .background(TangemTheme.colors.background.primary)
                .fillMaxWidth(),
        ) {
            Text(text = "Point: ${markerPoint.first}, ${markerPoint.second}")

            MarketChart(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(TangemTheme.colors.background.tertiary)
                    .height(173.dp),
                state = chartState,
                splitChartSegmentColor = TangemTheme.colors.icon.inactive,
                backgroundSplitChartSegmentColorAlpha = 0.24f,
                backgroundColorAlpha = 0.24f,
            )
            SpacerH16()

            Button(
                onClick = {
                    dataProducer.runTransaction {
                        updateLook {
                            it.copy(markerHighlightRightSide = !it.markerHighlightRightSide)
                        }
                    }
                },
            ) {
                Text(
                    text = "Change marker highlight side",
                )
            }
            Button(
                onClick = {
                    coroutineScope.launch {
                        dataProducer.runTransactionSuspend {
                            updateData {
                                MarketChartData.Data(
                                    x = it.x,
                                    y = it.y.reversed().toImmutableList(),
                                )
                            }
                        }
                    }
                },
            ) {
                Text("Change Data")
            }

            Button(
                onClick = {
                    dataProducer.runTransaction {
                        updateLook {
                            it.copy(
                                type = if (it.type == MarketChartLook.Type.Growing) {
                                    MarketChartLook.Type.Falling
                                } else {
                                    MarketChartLook.Type.Growing
                                },
                            )
                        }
                    }
                },
            ) {
                Text("Change color type")
            }
        }
    }
}

// endregion Preview
