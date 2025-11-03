package com.tangem.features.yield.supply.impl.chart.ui

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.State
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
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.features.yield.supply.impl.chart.entity.YieldSupplyChartUM
import com.tangem.features.yield.supply.impl.chart.entity.YieldSupplyMarketChartDataUM
import kotlinx.collections.immutable.persistentListOf
import java.util.Locale

@Composable
internal fun YieldSupplyChartContent(state: YieldSupplyChartUM, modifier: Modifier = Modifier) {
    val itemModifier = modifier
        .height(105.dp)
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
    val model = rememberChartModel(state.chartData)

    val columnsLayer = rememberColumnsLayer(accentColor = TangemTheme.colors.text.accent)

    val startAxis: VerticalAxis<AxisPosition.Vertical.Start> = rememberStartAxis(
        labelColor = TangemTheme.colors.text.tertiary,
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

    val averageLabelTopPadding by rememberAverageLabelTopPadding(state.chartData)

    Box(modifier = modifier) {
        MonthLabelsRow(labels = state.monthLables, modifier = Modifier.align(Alignment.BottomEnd))
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
            avr = state.chartData.avr.toString(),
        )
    }
}

@Composable
private fun rememberChartModel(data: YieldSupplyMarketChartDataUM): CartesianChartModel {
    return remember(data.y) {
        CartesianChartModel(
            ColumnCartesianLayerModel.build {
                series(data.y)
            },
        )
    }
}

@Suppress("MagicNumber")
@Composable
private fun rememberColumnsLayer(accentColor: Color): ColumnCartesianLayer {
    val column = rememberLineComponent(
        color = accentColor,
        thickness = 6.dp,
        shape = Shape.rounded(40),
    )
    return rememberColumnCartesianLayer(
        columnProvider = ColumnCartesianLayer.ColumnProvider.series(column),
        columnCollectionSpacing = 3.dp,
        mergeMode = { MergeMode.grouped() },
    )
}

@Composable
private fun rememberStartAxis(labelColor: Color, percentFormat: String): VerticalAxis<AxisPosition.Vertical.Start> {
    return rememberCustomStartAxis(
        label = rememberAxisLabelComponent(color = labelColor),
        valueFormatter = CartesianValueFormatter { value, _, _ ->
            val pct = String.format(Locale.getDefault(), percentFormat, value)
            "$pct%"
        },
        guideline = null,
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
private fun rememberAverageLabelTopPadding(data: YieldSupplyMarketChartDataUM): State<Dp> {
    return remember(data.y, data.avr) {
        derivedStateOf {
            val chartHeight = 105.dp
            val labelHeight = 24.dp
            val referenceLineThickness = 2.dp
            val desiredGap = 3.dp
            val opticalNudge = 1.dp
            val extraPaddingAboveLine = 2.dp
            val gapAboveLine = desiredGap + referenceLineThickness / 2 + opticalNudge + extraPaddingAboveLine
            val chartContentBottomInset = 10.dp

            val maxValueInSeries = (data.y.maxOrNull() ?: 0.0).coerceAtLeast(data.avr)
            val averageRatio = if (maxValueInSeries == 0.0) {
                0f
            } else {
                (data.avr / maxValueInSeries).coerceIn(0.0, 1.0).toFloat()
            }
            val computedTopPadding = (chartHeight - chartContentBottomInset) * (1f - averageRatio)
            -labelHeight - gapAboveLine
            if (computedTopPadding < 2.dp) 2.dp else computedTopPadding
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
private fun AverageLabel(avr: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.background(
            color = TangemTheme.colors.icon.primary1,
            shape = TangemTheme.shapes.roundedCornersSmall2,
        ),
    ) {
        Text(
            text = stringResourceSafe(R.string.yield_module_rate_info_sheet_chart_average, avr),
            style = TangemTheme.typography.caption1,
            color = TangemTheme.colors.text.primary2,
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
        )
}
// endregion