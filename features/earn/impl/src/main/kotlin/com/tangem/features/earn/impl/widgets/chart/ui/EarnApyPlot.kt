package com.tangem.features.earn.impl.widgets.chart.ui

import android.graphics.Typeface
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFontFamilyResolver
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontSynthesis
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.resolveAsTypeface
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.decoration.rememberHorizontalLine
import com.patrykandpatrick.vico.compose.cartesian.layer.grouped
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberColumnCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.compose.cartesian.rememberVicoScrollState
import com.patrykandpatrick.vico.compose.cartesian.rememberVicoZoomState
import com.patrykandpatrick.vico.compose.cartesian.segmented
import com.patrykandpatrick.vico.compose.common.component.rememberLineComponent
import com.patrykandpatrick.vico.compose.common.component.rememberShapeComponent
import com.patrykandpatrick.vico.compose.common.component.rememberTextComponent
import com.patrykandpatrick.vico.compose.common.of
import com.patrykandpatrick.vico.compose.common.shape.dashed
import com.patrykandpatrick.vico.core.cartesian.HorizontalLayout
import com.patrykandpatrick.vico.core.cartesian.Zoom
import com.patrykandpatrick.vico.core.cartesian.data.AxisValueOverrider
import com.patrykandpatrick.vico.core.cartesian.data.CartesianChartModel
import com.patrykandpatrick.vico.core.cartesian.data.ColumnCartesianLayerModel
import com.patrykandpatrick.vico.core.cartesian.decoration.HorizontalLine
import com.patrykandpatrick.vico.core.cartesian.layer.ColumnCartesianLayer
import com.patrykandpatrick.vico.core.cartesian.layer.ColumnCartesianLayer.MergeMode
import com.patrykandpatrick.vico.core.cartesian.marker.CartesianMarker
import com.patrykandpatrick.vico.core.cartesian.marker.CartesianMarkerValueFormatter
import com.patrykandpatrick.vico.core.cartesian.marker.CartesianMarkerVisibilityListener
import com.patrykandpatrick.vico.core.cartesian.marker.DefaultCartesianMarker
import com.patrykandpatrick.vico.core.common.Dimensions
import com.patrykandpatrick.vico.core.common.component.LineComponent
import com.patrykandpatrick.vico.core.common.data.ExtraStore
import com.patrykandpatrick.vico.core.common.shape.Shape
import com.tangem.core.ui.ds2.shimmers.TangemShimmer
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.res.LocalHapticManager
import com.tangem.core.ui.res.TangemTheme
import com.tangem.features.earn.impl.widgets.chart.EarnApyChartTexts
import com.tangem.features.earn.impl.widgets.chart.ui.state.EarnApyAverageUM
import com.tangem.features.earn.impl.widgets.chart.ui.state.EarnApyBarUM
import com.tangem.features.earn.impl.widgets.chart.ui.state.EarnApyChartUM
import com.tangem.features.earn.impl.widgets.chart.ui.state.EarnApyScale
import kotlinx.collections.immutable.ImmutableList
import java.util.Locale
import kotlin.math.roundToInt

private val COLUMN_THICKNESS = 4.dp

private val MIN_COLUMN_SPACING = 1.dp

/** Width of the column the axis labels stand in; the plot starts right after it. */
private val AXIS_LABELS_WIDTH = 40.dp

/** Room above the top grid line for the upper half of the top axis label: a 16dp caption at the default font size. */
private val PLOT_TOP = 8.dp

/** From the top grid line to the bottom of the bars. */
private val PLOT_HEIGHT = 116.dp

/** Part of the chart the bars and the grid occupy; the month labels stand below it. */
private val PLOT_AREA_HEIGHT = PLOT_TOP + PLOT_HEIGHT

private val MONTH_LABELS_GAP = 20.dp

private val MONTH_LABELS_HEIGHT = 16.dp

private val CHART_HEIGHT = PLOT_AREA_HEIGHT + MONTH_LABELS_GAP + MONTH_LABELS_HEIGHT

private val AVERAGE_LABEL_HEIGHT = 32.dp

private val REFERENCE_LINE_THICKNESS = 2.dp

private val GRID_LINE_THICKNESS = 1.dp

private const val DIMMED_COLUMN_ALPHA = 0.3f

private const val COLUMN_ROUNDNESS_PERCENT = 40

@Composable
internal fun EarnApyPlot(state: EarnApyChartUM, modifier: Modifier = Modifier) {
    val plotModifier = modifier
        .fillMaxWidth()
        .height(CHART_HEIGHT)

    when {
        state.isLoading -> TangemShimmer(modifier = plotModifier, radius = 12.dp)
        state.bars.isEmpty() -> EmptyPlot(modifier = plotModifier)
        else -> DataPlot(state = state, modifier = plotModifier)
    }
}

@Composable
private fun EmptyPlot(modifier: Modifier = Modifier) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Text(
            text = EarnApyChartTexts.noData.resolveReference(),
            style = TangemTheme.typography3.caption.medium,
            color = TangemTheme.colors3.text.secondary,
        )
    }
}

@Composable
private fun DataPlot(state: EarnApyChartUM, modifier: Modifier = Modifier) {
    BoxWithConstraints(modifier = modifier) {
        val values = remember(state.bars) { state.bars.map(EarnApyBarUM::value) }
        val model = remember(values) { CartesianChartModel(ColumnCartesianLayerModel.build { series(values) }) }
        val tooltips = rememberTooltips(bars = state.bars)

        val columnsLayer = rememberColumnsLayer(
            selectedBar = state.selectedBar,
            axisMax = state.scale.max * (PLOT_AREA_HEIGHT / PLOT_HEIGHT),
            columnSpacing = columnSpacing(plotWidth = maxWidth, columnCount = values.size),
        )
        val average = state.average
        val referenceLine = average?.let {
            rememberReferenceLine(average = it.value, color = TangemTheme.colors3.icon.primary)
        }

        val chart = rememberCartesianChart(
            columnsLayer,
            decorations = listOfNotNull(referenceLine),
            horizontalLayout = HorizontalLayout.segmented(),
            marker = rememberSelectionMarker(tooltips = tooltips),
            markerVisibilityListener = rememberSelectionListener(onBarSelect = state.onBarSelected),
        )

        PlotGrid(scale = state.scale)
        CartesianChartHost(
            modifier = Modifier
                .padding(start = AXIS_LABELS_WIDTH)
                .height(PLOT_AREA_HEIGHT),
            chart = chart,
            model = model,
            zoomState = rememberVicoZoomState(initialZoom = Zoom.Content, zoomEnabled = false),
            scrollState = rememberVicoScrollState(scrollEnabled = false),
        )
        if (average != null && state.selectedBar == null) {
            AverageLabel(
                average = average,
                modifier = Modifier
                    .offset(
                        x = AXIS_LABELS_WIDTH,
                        y = rememberAverageLabelOffset(average = average, scale = state.scale),
                    )
                    .height(AVERAGE_LABEL_HEIGHT),
            )
        }
        MonthLabelsRow(labels = state.xLabels, modifier = Modifier.align(Alignment.BottomStart))
    }
}

@Composable
private fun rememberTooltips(bars: ImmutableList<EarnApyBarUM>): List<String> {
    val resources = LocalContext.current.resources
    val configuration = LocalConfiguration.current

    return remember(bars, resources, configuration) { bars.map { it.tooltip.resolveReference(resources) } }
}

/**
 * Vico multiplies the column thickness by the zoom factor it needs to fit every point, so a short history would be
 * drawn as blobs the width of a finger. Spacing the columns out so that they fill the plot exactly holds that factor
 * at 1, and the bars stay [COLUMN_THICKNESS] wide whatever the period.
 */
private fun columnSpacing(plotWidth: Dp, columnCount: Int): Dp {
    if (columnCount <= 0) return MIN_COLUMN_SPACING

    val perColumn = (plotWidth - AXIS_LABELS_WIDTH).coerceAtLeast(0.dp) / columnCount

    return (perColumn - COLUMN_THICKNESS).coerceAtLeast(MIN_COLUMN_SPACING)
}

/**
 * The chart has no axes of its own: the plot fills the whole host, so the value range maps onto a height this file
 * knows, and the grid, the axis labels and the average label can be placed against it.
 */
@Composable
private fun rememberColumnsLayer(selectedBar: Int?, axisMax: Double, columnSpacing: Dp): ColumnCartesianLayer {
    val accent = TangemTheme.colors3.text.accent.blue
    val normal = rememberLineComponent(
        color = accent,
        thickness = COLUMN_THICKNESS,
        shape = Shape.rounded(COLUMN_ROUNDNESS_PERCENT),
    )
    val dimmed = rememberLineComponent(
        color = accent.copy(alpha = DIMMED_COLUMN_ALPHA),
        thickness = COLUMN_THICKNESS,
        shape = Shape.rounded(COLUMN_ROUNDNESS_PERCENT),
    )
    val selectedBarState = rememberUpdatedState(selectedBar)
    val provider = remember(normal, dimmed) { SelectionColumnProvider(selectedBarState, normal, dimmed) }

    return rememberColumnCartesianLayer(
        columnProvider = provider,
        columnCollectionSpacing = columnSpacing,
        mergeMode = { MergeMode.grouped() },
        axisValueOverrider = remember(axisMax) { AxisValueOverrider.fixed(minY = 0.0, maxY = axisMax) },
    )
}

/**
 * Dims every column but the selected one while a finger is down on the chart. The selection is read from a [State]
 * while the chart draws, so a new selection repaints the chart instead of rebuilding it: a chart rebuilt after the
 * finger is lifted is not repainted, and the dimming would stay on screen.
 */
private class SelectionColumnProvider(
    private val selectedBar: State<Int?>,
    private val normal: LineComponent,
    private val dimmed: LineComponent,
) : ColumnCartesianLayer.ColumnProvider {

    override fun getColumn(
        entry: ColumnCartesianLayerModel.Entry,
        seriesIndex: Int,
        extraStore: ExtraStore,
    ): LineComponent {
        val selected = selectedBar.value

        return if (selected != null && entry.x.toInt() != selected) dimmed else normal
    }

    override fun getWidestSeriesColumn(seriesIndex: Int, extraStore: ExtraStore): LineComponent = normal
}

/** Vico draws its text on a canvas, so it does not pick up the design-system font by itself. */
@Composable
private fun rememberCaptionTypeface(): Typeface {
    val textStyle = TangemTheme.typography3.caption.medium
    val resolver = LocalFontFamilyResolver.current
    val typeface by remember(resolver, textStyle) {
        resolver.resolveAsTypeface(
            fontFamily = textStyle.fontFamily,
            fontWeight = textStyle.fontWeight ?: FontWeight.Normal,
            fontStyle = textStyle.fontStyle ?: FontStyle.Normal,
            fontSynthesis = textStyle.fontSynthesis ?: FontSynthesis.All,
        )
    }

    return typeface
}

@Composable
private fun rememberSelectionMarker(tooltips: List<String>): CartesianMarker {
    val label = rememberTextComponent(
        color = TangemTheme.colors3.text.primary,
        textSize = TangemTheme.typography3.caption.medium.fontSize,
        typeface = rememberCaptionTypeface(),
        padding = Dimensions.of(horizontal = 8.dp, vertical = 4.dp),
        background = rememberShapeComponent(
            color = TangemTheme.colors3.bg.tertiary,
            shape = Shape.rounded(COLUMN_ROUNDNESS_PERCENT),
        ),
    )

    return remember(label, tooltips) {
        DefaultCartesianMarker(
            label = label,
            valueFormatter = CartesianMarkerValueFormatter { _, targets ->
                tooltips.getOrElse(targets.barIndex()) { "" }
            },
            labelPosition = DefaultCartesianMarker.LabelPosition.AroundPoint,
            indicator = null,
            guideline = null,
        )
    }
}

/**
 * The callback is read through [rememberUpdatedState]: every selection republishes the state with a new lambda, and a
 * listener rebuilt on each of them would restart the chart while the finger is still down.
 */
@Composable
private fun rememberSelectionListener(onBarSelect: (Int?) -> Unit): CartesianMarkerVisibilityListener {
    val currentOnBarSelect by rememberUpdatedState(onBarSelect)
    val haptic = LocalHapticManager.current

    return remember(haptic) {
        BarSelectionListener(haptic = haptic, onBarSelect = { bar -> currentOnBarSelect(bar) })
    }
}

/**
 * Grid lines on every labelled value but the lowest, which is the bottom of the bars. The labels stand at the left
 * edge of the chart, each centred on its line by its measured height, so a larger system font keeps them on the lines.
 */
@Composable
private fun PlotGrid(scale: EarnApyScale, modifier: Modifier = Modifier) {
    val intervals = (scale.max / scale.step).roundToInt()
    val labels = remember(scale) {
        (intervals downTo 0).map { interval -> gridLabel(value = interval * scale.step, decimals = scale.decimals) }
    }
    val lineColor = TangemTheme.colors3.border.secondary

    Box(modifier = modifier.fillMaxWidth()) {
        Canvas(
            modifier = Modifier
                .padding(start = AXIS_LABELS_WIDTH)
                .fillMaxWidth()
                .height(PLOT_AREA_HEIGHT),
        ) {
            for (interval in 1..intervals) {
                val y = PLOT_TOP.toPx() + PLOT_HEIGHT.toPx() * (1f - interval / intervals.toFloat())

                drawLine(
                    color = lineColor,
                    start = Offset(x = 0f, y = y),
                    end = Offset(x = size.width, y = y),
                    strokeWidth = GRID_LINE_THICKNESS.toPx(),
                )
            }
        }
        Layout(
            content = {
                labels.forEach { label ->
                    Text(
                        text = label,
                        style = TangemTheme.typography3.caption.medium,
                        color = TangemTheme.colors3.text.secondary,
                        maxLines = 1,
                    )
                }
            },
            modifier = Modifier.height(PLOT_AREA_HEIGHT),
        ) { measurables, constraints ->
            val placeables = measurables.map { it.measure(Constraints()) }
            val plotTop = PLOT_TOP.roundToPx()
            val plotHeight = PLOT_HEIGHT.roundToPx()

            layout(width = placeables.maxOfOrNull { it.width } ?: 0, height = constraints.maxHeight) {
                placeables.forEachIndexed { index, placeable ->
                    val lineY = plotTop + plotHeight * index / intervals

                    placeable.placeRelative(x = 0, y = lineY - placeable.height / 2)
                }
            }
        }
    }
}

private fun gridLabel(value: Double, decimals: Int): String =
    String.format(Locale.getDefault(), "%.${decimals}f", value) + "%"

@Composable
private fun rememberReferenceLine(average: Double, color: Color): HorizontalLine {
    return rememberHorizontalLine(
        y = { average },
        line = rememberLineComponent(
            color = color,
            thickness = REFERENCE_LINE_THICKNESS,
            shape = Shape.dashed(Shape.Pill, 4.dp, 2.dp),
        ),
    )
}

/**
 * The label is centred on the dashed line, which runs on from under it; the label is opaque and hides its start. An
 * average at either end of the scale puts the label partly outside the plot, where there is room for it.
 */
@Composable
private fun rememberAverageLabelOffset(average: EarnApyAverageUM, scale: EarnApyScale): Dp {
    return remember(scale, average) {
        val averageRatio = (average.value / scale.max).coerceIn(0.0, 1.0).toFloat()
        val lineY = PLOT_TOP + PLOT_HEIGHT * (1f - averageRatio)

        lineY - AVERAGE_LABEL_HEIGHT / 2
    }
}

/** Every month gets a cell of the same width and is centred in it, however many months the period spans. */
@Composable
private fun MonthLabelsRow(labels: ImmutableList<String>, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = AXIS_LABELS_WIDTH),
    ) {
        labels.forEach { label ->
            Text(
                text = label,
                style = TangemTheme.typography3.caption.medium,
                color = TangemTheme.colors3.text.secondary,
                textAlign = TextAlign.Center,
                maxLines = 1,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun AverageLabel(average: EarnApyAverageUM, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .background(color = TangemTheme.colors3.bg.tertiary, shape = CircleShape)
            .border(width = 1.dp, color = TangemTheme.colors3.border.secondary, shape = CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = average.text.resolveReference(),
            style = TangemTheme.typography3.caption.medium,
            color = TangemTheme.colors3.text.primary,
            modifier = Modifier.padding(horizontal = 10.dp),
        )
    }
}