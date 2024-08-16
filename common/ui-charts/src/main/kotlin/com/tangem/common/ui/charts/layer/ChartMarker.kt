package com.tangem.common.ui.charts.layer

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.patrykandpatrick.vico.compose.common.component.rememberUnboundedLineComponent
import com.patrykandpatrick.vico.compose.common.component.shapeComponent
import com.patrykandpatrick.vico.compose.common.of
import com.patrykandpatrick.vico.compose.common.shape.dashed
import com.patrykandpatrick.vico.core.cartesian.*
import com.patrykandpatrick.vico.core.cartesian.data.CartesianChartModel
import com.patrykandpatrick.vico.core.cartesian.marker.CartesianMarker
import com.patrykandpatrick.vico.core.cartesian.marker.CartesianMarkerValueFormatter
import com.patrykandpatrick.vico.core.cartesian.marker.DefaultCartesianMarker
import com.patrykandpatrick.vico.core.common.Dimensions
import com.patrykandpatrick.vico.core.common.LayeredComponent
import com.patrykandpatrick.vico.core.common.component.Component
import com.patrykandpatrick.vico.core.common.component.TextComponent
import com.patrykandpatrick.vico.core.common.shape.Shape
import com.tangem.core.ui.res.TangemTheme

/**
 * @param color The color of the indicator and guideline.
 * @param innerCircleColor The color of the inner circle of the indicator.
 *
 * @return A [CartesianMarker] that consists of a dashed guideline and a layered indicator with a shadow effect.
 */
@Composable
internal fun rememberTangemChartMarker(color: Color): CartesianMarker {
    val guideline = rememberUnboundedLineComponent(
        color = color,
        verticalAddDrawSpace = TangemTheme.dimens.spacing24,
        shape = remember { Shape.dashed(Shape.Rectangle, 4.dp, 4.dp) },
    )

    return remember(guideline) {
        val outColor = guideline.color

        object : DefaultCartesianMarker(
            label = TextComponent(textSizeSp = 0f),
            indicator = ::indicator,
            indicatorSizeDp = INDICATOR_SIZE_DP,
            guideline = guideline,
            valueFormatter = object : CartesianMarkerValueFormatter {
                override fun format(
                    context: CartesianDrawContext,
                    targets: List<CartesianMarker.Target>,
                ): CharSequence = ""
            },
        ) {
            override fun updateInsets(
                context: CartesianMeasureContext,
                horizontalDimensions: HorizontalDimensions,
                model: CartesianChartModel,
                insets: Insets,
            ) {
                with(context) {
                    super.updateInsets(context, horizontalDimensions, model, insets)
                    val baseShadowInsetDp =
                        CLIPPING_FREE_SHADOW_RADIUS_MULTIPLIER * LABEL_BACKGROUND_SHADOW_RADIUS_DP
                    val topInset = (baseShadowInsetDp - LABEL_BACKGROUND_SHADOW_DY_DP).pixels
                    val bottomInset = (baseShadowInsetDp + LABEL_BACKGROUND_SHADOW_DY_DP).pixels
                    insets.ensureValuesAtLeast(top = topInset, bottom = bottomInset)
                }
            }

            override fun CartesianDrawContext.drawIndicator(x: Float, y: Float, color: Int, halfIndicatorSize: Float) {
                val indicator = indicator ?: return
                cacheStore
                    .getOrSet(keyNamespace, indicator, outColor) { indicator.invoke(outColor) }
                    .draw(
                        this,
                        x - halfIndicatorSize,
                        y - halfIndicatorSize,
                        x + halfIndicatorSize,
                        y + halfIndicatorSize,
                    )
            }
        }
    }
}

private fun indicator(color: Int): Component {
    val composeColor = Color(color)

    return LayeredComponent(
        rear = shapeComponent(
            color = composeColor.copy(alpha = INDICATOR_REAR_COLOR_ALPHA),
            shape = Shape.Pill,
        ),
        front = LayeredComponent(
            rear = shapeComponent(
                color = composeColor,
                shape = Shape.Pill,
            ),
            front = shapeComponent(
                color = Color.White,
                shape = Shape.Pill,
            ),
            padding = indicatorPadding,
        ),
        padding = indicatorPadding,
    )
}

private val indicatorPadding = Dimensions.of(3.dp)
private const val LABEL_BACKGROUND_SHADOW_RADIUS_DP = 4f
private const val LABEL_BACKGROUND_SHADOW_DY_DP = 2f
private const val CLIPPING_FREE_SHADOW_RADIUS_MULTIPLIER = 1.4f
private const val INDICATOR_SIZE_DP = 16f
private const val INDICATOR_REAR_COLOR_ALPHA = .24f