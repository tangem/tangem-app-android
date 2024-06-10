package com.tangem.common.ui.charts.marker

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.fullWidth
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLineSpec
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.compose.cartesian.rememberVicoZoomState
import com.patrykandpatrick.vico.compose.common.component.rememberLayeredComponent
import com.patrykandpatrick.vico.compose.common.component.rememberShapeComponent
import com.patrykandpatrick.vico.compose.common.component.rememberUnboundedLineComponent
import com.patrykandpatrick.vico.compose.common.of
import com.patrykandpatrick.vico.compose.common.shader.color
import com.patrykandpatrick.vico.compose.common.shape.dashed
import com.patrykandpatrick.vico.core.cartesian.*
import com.patrykandpatrick.vico.core.cartesian.data.AxisValueOverrider
import com.patrykandpatrick.vico.core.cartesian.data.CartesianChartModel
import com.patrykandpatrick.vico.core.cartesian.data.LineCartesianLayerModel
import com.patrykandpatrick.vico.core.cartesian.marker.CartesianMarker
import com.patrykandpatrick.vico.core.cartesian.marker.DefaultCartesianMarker
import com.patrykandpatrick.vico.core.common.Dimensions
import com.patrykandpatrick.vico.core.common.component.TextComponent
import com.patrykandpatrick.vico.core.common.shader.DynamicShader
import com.patrykandpatrick.vico.core.common.shape.Shape
import com.tangem.common.ui.charts.preview.MarketChartPreviewDataProvider
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import java.math.BigDecimal

/**
 * @param color The color of the indicator and guideline.
 * @param innerCircleColor The color of the inner circle of the indicator.
 *
 * @return A [CartesianMarker] that consists of a dashed guideline and a layered indicator with a shadow effect.
 */
@Composable
internal fun rememberTangemChartMarker(color: Color, innerCircleColor: Color): CartesianMarker {
    val indicatorFrontComponent = rememberShapeComponent(
        shape = Shape.Pill,
        color = innerCircleColor,
    )
    val indicatorCenterComponent = rememberShapeComponent(
        shape = Shape.Pill,
        color = color,
    )
    val indicatorRearComponent = rememberShapeComponent(
        shape = Shape.Pill,
        color = if (color == Color.Transparent) {
            Color.Transparent
        } else {
            color.copy(alpha = INDICATOR_REAR_COLOR_ALPHA)
        },
    )
    val indicator = rememberLayeredComponent(
        rear = indicatorRearComponent,
        front = rememberLayeredComponent(
            rear = indicatorCenterComponent,
            front = indicatorFrontComponent,
            padding = indicatorPadding,
        ),
        padding = indicatorPadding,
    )
    val guideline = rememberUnboundedLineComponent(
        color = color,
        verticalAddDrawSpace = TangemTheme.dimens.spacing24,
        shape = remember { Shape.dashed(Shape.Rectangle, 4.dp, 4.dp) },
    )
    return remember(indicator, guideline) {
        object : DefaultCartesianMarker(
            label = TextComponent.build { textSizeSp = 0f },
            indicator = indicator,
            indicatorSizeDp = INDICATOR_SIZE_DP,
            guideline = guideline,
        ) {
            override fun getInsets(
                context: CartesianMeasureContext,
                outInsets: Insets,
                horizontalDimensions: HorizontalDimensions,
            ) {
                with(context) {
                    super.getInsets(context, outInsets, horizontalDimensions)
                    val baseShadowInsetDp =
                        CLIPPING_FREE_SHADOW_RADIUS_MULTIPLIER * LABEL_BACKGROUND_SHADOW_RADIUS_DP
                    outInsets.top += (baseShadowInsetDp - LABEL_BACKGROUND_SHADOW_DY_DP).pixels
                    outInsets.bottom += (baseShadowInsetDp + LABEL_BACKGROUND_SHADOW_DY_DP).pixels
                }
            }
        }
    }
}

private val indicatorPadding = Dimensions.of(3.dp)
private const val LABEL_BACKGROUND_SHADOW_RADIUS_DP = 4f
private const val LABEL_BACKGROUND_SHADOW_DY_DP = 2f
private const val CLIPPING_FREE_SHADOW_RADIUS_MULTIPLIER = 1.4f
private const val INDICATOR_SIZE_DP = 16f
private const val INDICATOR_REAR_COLOR_ALPHA = .24f

// region Preview

@Preview(showBackground = true, widthDp = 360)
@Preview(showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun TangemChartMarkerPreview(
    @PreviewParameter(MarketChartPreviewDataProvider::class) previewData: Pair<List<BigDecimal>, List<BigDecimal>>,
) {
    val marker = rememberTangemChartMarker(Color.Red, Color.White)
    val y = previewData.second.map { it.toFloat() }
    val x = List(y.size) { it.toFloat() }
    val model = CartesianChartModel(LineCartesianLayerModel.build { series(x, y) })

    val centerAprx = (model.models[0].minX + model.models[0].maxX) / 2f
    val center = model.models[0].getXDeltaGcd().let { centerAprx - centerAprx % it }

    TangemThemePreview {
        Box(modifier = Modifier.background(TangemTheme.colors.background.primary)) {
            CartesianChartHost(
                modifier = Modifier.fillMaxWidth(),
                chart = rememberCartesianChart(
                    rememberLineCartesianLayer(
                        listOf(rememberLineSpec(shader = DynamicShader.color(Color.Blue))),
                        axisValueOverrider = AxisValueOverrider.fixed(minY = model.models[0].minY),
                    ),
                    persistentMarkers = mapOf(center to marker),
                ),
                model = model,
                marker = marker,
                zoomState = rememberVicoZoomState(initialZoom = Zoom.Content, zoomEnabled = false),
                horizontalLayout = HorizontalLayout.fullWidth(),
            )
        }
    }
}

// endregion Preview
