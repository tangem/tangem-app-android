package com.tangem.features.yield.supply.impl.chart.ui

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberAxisLabelComponent
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberBottomAxis
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberCustomStartAxis
import com.patrykandpatrick.vico.compose.cartesian.decoration.rememberHorizontalLine
import com.patrykandpatrick.vico.compose.cartesian.layer.grouped
import com.patrykandpatrick.vico.compose.cartesian.layer.stacked
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.compose.cartesian.rememberVicoScrollState
import com.patrykandpatrick.vico.compose.cartesian.rememberVicoZoomState
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberColumnCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.segmented
import com.patrykandpatrick.vico.compose.common.component.rememberLineComponent
import com.patrykandpatrick.vico.compose.common.shape.dashed
import com.patrykandpatrick.vico.core.cartesian.HorizontalLayout
import com.patrykandpatrick.vico.core.cartesian.Zoom
import com.patrykandpatrick.vico.core.cartesian.axis.AxisPosition
import com.patrykandpatrick.vico.core.cartesian.axis.HorizontalAxis
import com.patrykandpatrick.vico.core.cartesian.axis.VerticalAxis
import com.patrykandpatrick.vico.core.cartesian.data.CartesianValueFormatter
import com.patrykandpatrick.vico.core.cartesian.data.CartesianChartModel
import com.patrykandpatrick.vico.core.cartesian.data.ColumnCartesianLayerModel
import com.patrykandpatrick.vico.core.cartesian.decoration.HorizontalLine
import com.patrykandpatrick.vico.core.cartesian.layer.ColumnCartesianLayer
import com.patrykandpatrick.vico.core.cartesian.layer.ColumnCartesianLayer.MergeMode
import com.patrykandpatrick.vico.core.common.shape.Shape
import com.tangem.core.ui.R
import com.tangem.core.ui.components.SpacerH12
import com.tangem.core.ui.components.TextShimmer
import com.tangem.core.ui.components.buttons.SecondarySmallButton
import com.tangem.core.ui.components.buttons.SmallButtonConfig
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.features.yield.supply.impl.common.formatter.formatApy
import com.tangem.features.yield.supply.impl.chart.entity.YieldSupplyChartUM
import com.tangem.features.yield.supply.impl.chart.entity.YieldSupplyMarketChartDataUM
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import java.util.Locale

/** Height the chart host is laid out at; the average label and the column gap both measure against it. */
private val CHART_HEIGHT = 105.dp

/** Design keeps the reward cap a separate segment rather than a flush stack. */
private val COLUMN_GAP = 2.dp

/** Bar width in the design, held constant across every period — see [columnSpacing]. */
private val COLUMN_THICKNESS = 4.dp

/** Floor for [columnSpacing]: past it the bars would touch, and a dense period has to stay readable. */
private val MIN_COLUMN_SPACING = 1.dp

@Composable
internal fun YieldSupplyChartContent(state: YieldSupplyChartUM, modifier: Modifier = Modifier) {
    val itemModifier = modifier
        .height(CHART_HEIGHT)
        .fillMaxWidth()
    when (state) {
        is YieldSupplyChartUM.Loading -> YieldSupplyChartLoading(modifier = itemModifier)
        is YieldSupplyChartUM.Error -> {
            Column(
                modifier = itemModifier,
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = stringResourceSafe(R.string.markets_loading_error_title),
                    style = TangemTheme.typography.caption1,
                    color = TangemTheme.colors.text.tertiary,
                )
                SpacerH12()
                SecondarySmallButton(
                    config = SmallButtonConfig(
                        text = resourceReference(R.string.try_to_load_data_again_button_title),
                        onClick = state.onRetry,
                    ),
                )
            }
        }
        is YieldSupplyChartUM.Data -> YieldSupplyChartData(state = state, modifier = itemModifier)
    }
}

@Suppress("MagicNumber")
@Composable
private fun YieldSupplyChartLoading(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp),
        ) {
            repeat(4) { index ->
                TextShimmer(
                    modifier = Modifier
                        .size(16.dp),
                    radius = 4.dp,
                    style = TangemTheme.typography.body1,
                )
                Spacer(modifier = Modifier.height(6.dp))
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                repeat(5) {
                    TextShimmer(
                        modifier = Modifier
                            .weight(1f)
                            .height(16.dp),
                        radius = 4.dp,
                        style = TangemTheme.typography.body1,
                    )
                }
            }
        }
    }
}

@Suppress("MagicNumber")
@Composable
private fun YieldSupplyChartData(state: YieldSupplyChartUM.Data, modifier: Modifier = Modifier) {
    BoxWithConstraints(modifier = modifier) {
        val model = rememberChartModel(state.chartData)

        val columnsLayer = rememberColumnsLayer(
            accentColor = TangemTheme.colors.text.accent,
            isStacked = state.chartData.rewardCaps.isNotEmpty(),
            columnSpacing = columnSpacing(plotWidth = maxWidth, columnCount = state.chartData.y.size),
        )

        val startAxis: VerticalAxis<AxisPosition.Vertical.Start> = rememberStartAxis(
            labelColor = TangemTheme.colors.text.tertiary,
            guidelineColor = TangemTheme.colors.stroke.primary,
            percentFormat = state.chartData.percentFormat,
        )

        val bottomAxis: HorizontalAxis<AxisPosition.Horizontal.Bottom> = rememberBottomAxis(
            labelColor = TangemTheme.colors.text.tertiary,
        )

        val referenceLineThickness = 2.dp
        val referenceLine = rememberReferenceLine(
            average = state.chartData.avr,
            color = TangemTheme.colors.icon.primary1,
            thickness = referenceLineThickness,
        )

        val chart = rememberCartesianChart(
            columnsLayer,
            startAxis = startAxis,
            bottomAxis = bottomAxis,
            decorations = listOf(referenceLine),
            horizontalLayout = HorizontalLayout.segmented(),
        )

        val averageLabelTopPadding = rememberAverageLabelTopPadding(state.chartData)

        if (state.chartData.xLabels.isEmpty()) {
            MonthLabelsRow(labels = state.monthLables, modifier = Modifier.align(Alignment.BottomEnd))
        } else {
            ColumnLabelsRow(labels = state.chartData.xLabels, modifier = Modifier.align(Alignment.BottomEnd))
        }
        CartesianChartHost(
            modifier = Modifier.padding(bottom = 4.dp),
            chart = chart,
            model = model,
            zoomState = rememberVicoZoomState(initialZoom = Zoom.Content, zoomEnabled = false),
            scrollState = rememberVicoScrollState(scrollEnabled = false),
        )
        AverageLabel(
            modifier = Modifier
                .padding(start = 36.dp, top = averageLabelTopPadding)
                .height(24.dp),
            avr = state.chartData.avr.toBigDecimal().formatApy(),
        )
    }
}

/**
 * Vico multiplies the column thickness by the zoom factor it needs to fit every point, so seven days of history
 * would be drawn as blobs the width of a finger. Spacing the columns out so that they fill the plot exactly holds
 * that factor at 1, and the bars stay [COLUMN_THICKNESS] wide whatever the period. The 38.dp taken off
 * the width is the room the vertical axis labels occupy.
 */
private fun columnSpacing(plotWidth: Dp, columnCount: Int): Dp {
    if (columnCount <= 0) return MIN_COLUMN_SPACING

    val perColumn = (plotWidth - 38.dp).coerceAtLeast(0.dp) / columnCount

    return (perColumn - COLUMN_THICKNESS).coerceAtLeast(MIN_COLUMN_SPACING)
}

@Composable
private fun rememberChartModel(data: YieldSupplyMarketChartDataUM): CartesianChartModel {
    return remember(data.y, data.rewardCaps) {
        CartesianChartModel(
            ColumnCartesianLayerModel.build {
                if (data.rewardCaps.isEmpty()) {
                    series(data.y)
                } else {
                    // The gap is carved out of the base rather than stacked on top, so the column still ends at the
                    // APY the backend reported and keeps meeting the average line.
                    val gap = data.gapSeries()
                    series(data.y.mapIndexed { index, base -> (base - gap[index]).coerceAtLeast(0.0) })
                    series(gap)
                    series(data.rewardCaps)
                }
            },
        )
    }
}

/**
 * A transparent spacer series, as tall in data units as [COLUMN_GAP] is on screen, so the cap reads as its own
 * segment. It is sized off the tallest column, which is what the vertical axis scales to, and the base series gives
 * the room up rather than the column growing by it.
 */
private fun YieldSupplyMarketChartDataUM.gapSeries(): List<Double> {
    val top = y.zip(rewardCaps) { base, cap -> base + cap }.maxOrNull() ?: return emptyList()
    // Every column gives up the same room, so the gap can be no deeper than the shallowest base: taking more than
    // a base holds would leave that column standing on the gap and top out above the APY the backend reported.
    val gap = (top * (COLUMN_GAP.value / CHART_HEIGHT.value)).coerceAtMost(y.minOrNull() ?: 0.0)

    return List(y.size) { gap }
}

@Suppress("MagicNumber")
@Composable
private fun rememberColumnsLayer(accentColor: Color, isStacked: Boolean, columnSpacing: Dp): ColumnCartesianLayer {
    val column = rememberLineComponent(
        color = accentColor,
        thickness = COLUMN_THICKNESS,
        shape = Shape.rounded(40),
    )
    val gapColumn = rememberLineComponent(
        color = Color.Transparent,
        thickness = COLUMN_THICKNESS,
    )
    val rewardsColumn = rememberLineComponent(
        color = accentColor.copy(alpha = 0.3f),
        thickness = COLUMN_THICKNESS,
        shape = Shape.rounded(40),
    )
    val columns = if (isStacked) listOf(column, gapColumn, rewardsColumn) else listOf(column)

    return rememberColumnCartesianLayer(
        columnProvider = ColumnCartesianLayer.ColumnProvider.series(columns),
        columnCollectionSpacing = columnSpacing,
        mergeMode = { if (isStacked) MergeMode.stacked() else MergeMode.grouped() },
    )
}

@Composable
private fun rememberStartAxis(
    labelColor: Color,
    guidelineColor: Color,
    percentFormat: String,
): VerticalAxis<AxisPosition.Vertical.Start> {
    return rememberCustomStartAxis(
        label = rememberAxisLabelComponent(color = labelColor),
        valueFormatter = CartesianValueFormatter { value, _, _ ->
            val pct = String.format(Locale.getDefault(), percentFormat, value)
            "$pct%"
        },
        guideline = rememberLineComponent(color = guidelineColor, thickness = 1.dp),
        tick = null,
        line = null,
    )
}

@Composable
private fun rememberBottomAxis(labelColor: Color): HorizontalAxis<AxisPosition.Horizontal.Bottom> {
    return rememberBottomAxis(
        label = rememberAxisLabelComponent(color = labelColor),
        guideline = null,
        tick = null,
        line = null,
        valueFormatter = CartesianValueFormatter { _, _, _ ->
            ""
        },
    )
}

@Composable
private fun rememberReferenceLine(average: Double, color: Color, thickness: Dp): HorizontalLine {
    return rememberHorizontalLine(
        y = { average },
        line = rememberLineComponent(
            color = color,
            thickness = thickness,
            shape = Shape.dashed(Shape.Pill, 4.dp, 2.dp),
        ),
    )
}

@Composable
private fun rememberAverageLabelTopPadding(data: YieldSupplyMarketChartDataUM): Dp {
    return remember(data.y, data.rewardCaps, data.avr) {
        val chartHeight = CHART_HEIGHT
        val labelHeight = 24.dp
        val referenceLineThickness = 2.dp
        val desiredGap = 3.dp
        val opticalNudge = 1.dp
        val extraPaddingAboveLine = 2.dp
        val gapAboveLine = desiredGap + referenceLineThickness / 2 + opticalNudge + extraPaddingAboveLine
        val chartContentBottomInset = 10.dp

        // The axis scales to the tallest column, and a column is its base plus the reward stacked on it.
        // Measuring against the bases alone would place the label above a line drawn against the real top.
        val tallestColumn = if (data.rewardCaps.isEmpty()) {
            data.y.maxOrNull()
        } else {
            data.y.zip(data.rewardCaps) { base, cap -> base + cap }.maxOrNull()
        }
        val maxValueInSeries = (tallestColumn ?: 0.0).coerceAtLeast(data.avr)
        val averageRatio = if (maxValueInSeries == 0.0) {
            0f
        } else {
            (data.avr / maxValueInSeries).coerceIn(0.0, 1.0).toFloat()
        }
        val computedTopPadding = (chartHeight - chartContentBottomInset) * (1f - averageRatio) -
            labelHeight - gapAboveLine
        if (computedTopPadding < 2.dp) 2.dp else computedTopPadding
    }
}

/** One cell per column so a label sits under the bar it belongs to; blank entries keep their slot. */
@Composable
private fun ColumnLabelsRow(labels: ImmutableList<String>, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .padding(start = 38.dp, top = 4.dp)
            .fillMaxWidth(),
    ) {
        labels.forEach { label ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 16.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = label,
                    modifier = Modifier.wrapContentWidth(unbounded = true),
                    style = TangemTheme.typography.caption2,
                    color = TangemTheme.colors.text.tertiary,
                )
            }
        }
    }
}

@Composable
private fun MonthLabelsRow(labels: kotlinx.collections.immutable.ImmutableList<String>, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .padding(start = 38.dp, top = 4.dp)
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        repeat(labels.size) { index ->
            Text(
                text = labels[index],
                modifier = Modifier
                    .weight(1f)
                    .height(16.dp),
                style = TangemTheme.typography.caption2,
                color = TangemTheme.colors.text.tertiary,
            )
        }
    }
}

@Composable
private fun AverageLabel(avr: TextReference, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.background(
            color = TangemTheme.colors.button.secondary,
            shape = TangemTheme.shapes.roundedCornersSmall2,
        ),
    ) {
        Text(
            text = stringResourceSafe(R.string.yield_module_rate_info_sheet_chart_average, avr.resolveReference()),
            style = TangemTheme.typography.caption1,
            color = TangemTheme.colors.text.primary1,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
        )
    }
}

// region Preview
@Composable
@Preview(showBackground = true, widthDp = 360)
@Preview(showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
private fun YieldSupplyChartContent_Preview(
    @PreviewParameter(YieldSupplyChartPreviewProvider::class) state: YieldSupplyChartUM,
) {
    TangemThemePreview {
        YieldSupplyChartContent(state = state)
    }
}

private class YieldSupplyChartPreviewProvider : PreviewParameterProvider<YieldSupplyChartUM> {
    override val values: Sequence<YieldSupplyChartUM>
        get() = sequenceOf(
            YieldSupplyChartUM.Loading,
            YieldSupplyChartUM.Error(onRetry = {}),
            YieldSupplyChartUM.Data(
                chartData = YieldSupplyMarketChartDataUM.mock(),
                monthLables = persistentListOf("Jul", "Sep", "Nov", "Feb", "May"),
            ),
            YieldSupplyChartUM.Data(
                chartData = YieldSupplyMarketChartDataUM.mockIncentivised(),
                monthLables = persistentListOf("Jul", "Sep", "Nov", "Feb", "May"),
            ),
        )
}
// endregion