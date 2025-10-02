package com.tangem.features.yield.supply.impl.chart.ui

import android.content.res.Configuration
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberAxisLabelComponent
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberBottomAxis
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberCustomStartAxis
import com.patrykandpatrick.vico.compose.cartesian.decoration.rememberHorizontalLine
import com.patrykandpatrick.vico.compose.cartesian.fullWidth
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.compose.cartesian.rememberVicoScrollState
import com.patrykandpatrick.vico.compose.cartesian.rememberVicoZoomState
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberColumnCartesianLayer
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
import com.tangem.features.yield.supply.impl.chart.entity.YieldSupplyMarketChartData

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

        CircularProgressIndicator(
            modifier = Modifier
                .size(TangemTheme.dimens.size16)
                .align(Alignment.Center),
            color = TangemTheme.colors.text.tertiary,
        )
    }
}

@Composable
private fun YieldSupplyChartData(state: YieldSupplyChartUM.Data, modifier: Modifier = Modifier) {
    val yValues = state.chartData.y
    val model = CartesianChartModel(ColumnCartesianLayerModel.build { series(yValues) })

    val layer = rememberColumnCartesianLayer()

    val startAxis: VerticalAxis<AxisPosition.Vertical.Start> = rememberCustomStartAxis(
        label = rememberAxisLabelComponent(color = TangemTheme.colors.text.tertiary),
        valueFormatter = CartesianValueFormatter { value, _, _ ->
            val pct = value.toInt()
            "$pct%"
        },
    )

    val bottomAxis: HorizontalAxis<AxisPosition.Horizontal.Bottom> = rememberBottomAxis(
        label = rememberAxisLabelComponent(color = TangemTheme.colors.text.tertiary),
        guideline = null,
        tick = null,
        line = null,
    )

    val average = yValues.map { it.toDouble() }.average()

    val referenceLine = rememberHorizontalLine(
        y = { average },
        line = rememberLineComponent(
            color = TangemTheme.colors.text.tertiary,
            thickness = TangemTheme.dimens.size1,
            shape = Shape.dashed(Shape.Rectangle, TangemTheme.dimens.size4, TangemTheme.dimens.size4),
        ),
    )

    val chart = rememberCartesianChart(
        layer,
        startAxis = startAxis,
        bottomAxis = bottomAxis,
        decorations = listOf(referenceLine),
        horizontalLayout = HorizontalLayout.fullWidth(),
    )

    CartesianChartHost(
        modifier = modifier,
        chart = chart,
        model = model,
        zoomState = rememberVicoZoomState(initialZoom = Zoom.Content, zoomEnabled = false),
        scrollState = rememberVicoScrollState(scrollEnabled = false),
    )
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
            YieldSupplyChartUM.Data(chartData = YieldSupplyMarketChartData.mock()),
        )
}
// endregion