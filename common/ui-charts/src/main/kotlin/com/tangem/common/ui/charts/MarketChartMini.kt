package com.tangem.common.ui.charts

import android.content.res.Configuration
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.patrykandpatrick.vico.compose.cartesian.*
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLine
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLineCartesianLayer
import com.patrykandpatrick.vico.compose.common.shader.toDynamicShader
import com.patrykandpatrick.vico.core.cartesian.HorizontalLayout
import com.patrykandpatrick.vico.core.cartesian.Zoom
import com.patrykandpatrick.vico.core.cartesian.data.CartesianChartModel
import com.patrykandpatrick.vico.core.cartesian.data.LineCartesianLayerModel
import com.patrykandpatrick.vico.core.cartesian.layer.LineCartesianLayer
import com.patrykandpatrick.vico.core.common.shader.ColorShader
import com.tangem.common.ui.charts.state.MarketChartLook
import com.tangem.common.ui.charts.state.MarketChartRawData
import com.tangem.core.ui.components.SpacerH16
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import kotlinx.collections.immutable.toImmutableList
import kotlin.random.Random

@Composable
fun MarketChartMini(
    rawData: MarketChartRawData,
    modifier: Modifier = Modifier,
    type: MarketChartLook.Type = MarketChartLook.Type.Growing,
    growingColor: Color = TangemTheme.colors.icon.accent,
    fallingColor: Color = TangemTheme.colors.icon.warning,
    neutralColor: Color = TangemTheme.colors.icon.informative,
) {
    val model = remember(rawData) {
        CartesianChartModel(LineCartesianLayerModel.build { series(rawData.y) })
    }

    val lineColor = when (type) {
        MarketChartLook.Type.Growing -> growingColor
        MarketChartLook.Type.Falling -> fallingColor
        MarketChartLook.Type.Neutral -> neutralColor
    }

    val lineSpec = rememberLine(
        shader = ColorShader(lineColor.toArgb()),
        thickness = 1.dp,
        backgroundShader = Brush.verticalGradient(
            colors = listOf(lineColor.copy(alpha = 0.22f), Color.Transparent),
        ).toDynamicShader(),
    )

    val layer = rememberLineCartesianLayer(LineCartesianLayer.LineProvider.series(lineSpec))
    val chart = rememberCartesianChart(
        layer,
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

@Preview(showBackground = true, widthDp = 360)
@Preview(showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun Preview() {
    val data = MarketChartRawData(
        x = List(20) { Random.nextFloat().toDouble() }.toImmutableList(),
        y = List(20) { Random.nextFloat().toDouble() }.toImmutableList(),
    )

    TangemThemePreview {
        Column {
            MarketChartMini(rawData = data, type = MarketChartLook.Type.Growing)
            SpacerH16()
            MarketChartMini(rawData = data, type = MarketChartLook.Type.Falling)
            SpacerH16()
            MarketChartMini(rawData = data, type = MarketChartLook.Type.Neutral)
        }
    }
}

@Preview(showBackground = true, widthDp = 360)
@Preview(showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PreviewColumn() {
    val data = MarketChartRawData(
        x = List(20) { Random.nextFloat().toDouble() }.toImmutableList(),
        y = List(20) { Random.nextFloat().toDouble() }.toImmutableList(),
    )

    TangemThemePreview {
        LazyColumn {
            items(100) {
                MarketChartMini(
                    rawData = data,
                    type = if (it % 3 == 0) MarketChartLook.Type.Growing else MarketChartLook.Type.Falling,
                )
            }
        }
    }
}

// endregion Preview