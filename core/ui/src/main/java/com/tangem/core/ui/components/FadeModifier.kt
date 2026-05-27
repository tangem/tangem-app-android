package com.tangem.core.ui.components

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.systemBars
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.res.TangemTheme

@Composable
fun Modifier.edgeFade(
    vararg positions: FadePosition,
    size: Dp,
    isVisible: Boolean = true,
    animationSpec: AnimationSpec<Dp>? = null,
    color: Color = TangemTheme.colors.background.secondary,
    solidStop: Float = 0f,
): Modifier = composed {
    val animatedSize = animationSpec?.let { spec ->
        animateDpAsState(
            targetValue = if (isVisible) size else 0.dp,
            animationSpec = spec,
            label = "Edge fade width",
        )
    }

    drawWithContent {
        drawContent()

        positions.forEach { side ->
            val (start, end) = this.size.getFadeOffsets(side)

            val staticSizePx = if (isVisible) size.toPx() else 0f
            val sizePx = animatedSize?.value?.toPx() ?: staticSizePx
            if (sizePx <= 0f) return@forEach

            val fraction = when (side) {
                FadePosition.LEFT, FadePosition.RIGHT -> sizePx / this.size.width
                FadePosition.BOTTOM, FadePosition.TOP -> sizePx / this.size.height
            }

            drawRect(
                brush = Brush.linearGradient(
                    0f to color,
                    solidStop.coerceIn(minimumValue = 0f, maximumValue = 1f) * fraction to color,
                    fraction to Color.Transparent,
                    start = start,
                    end = end,
                ),
                size = this.size,
            )
        }
    }
}

@Composable
fun Modifier.bottomFade(
    height: Dp = with(LocalDensity.current) { WindowInsets.systemBars.getBottom(this).toDp() } + 96.dp,
    color: Color = TangemTheme.colors.background.secondary,
): Modifier = edgeFade(
    FadePosition.BOTTOM,
    size = height,
    color = color,
)

/**
 * Draws a vertical gradient fade over the top edge of the content.
 *
 * @param height the fade region height
 * @param color the solid color at the top edge that fades to transparent at the bottom of the region
 * @param solidStop fraction (0..1) of [height] kept fully [color] before the fade starts
 */
@Composable
fun Modifier.topFade(
    height: Dp,
    color: Color = TangemTheme.colors.background.secondary,
    solidStop: Float = 0f,
): Modifier = edgeFade(
    FadePosition.TOP,
    size = height,
    color = color,
    solidStop = solidStop,
)

/**
 * Draws a vertical gradient fade over the top edge with custom [colorStops].
 *
 * Stops are defined relative to [height] (0f = top, 1f = bottom of the fade region).
 *
 * Note: the gradient is drawn over the entire content area, so the last color stop's color
 * extends below the fade region down to the bottom. Pass [Color.Transparent] as the last stop
 * (or a color matching the underlying content) to avoid covering the area outside the fade.
 */
@Composable
fun Modifier.topFade(height: Dp, vararg colorStops: Pair<Float, Color>): Modifier = composed {
    drawWithContent {
        drawContent()

        if (this.size.height <= 0f) return@drawWithContent
        val fraction = (height.toPx() / this.size.height).coerceIn(0f, 1f)
        if (fraction <= 0f) return@drawWithContent

        val (start, end) = this.size.getFadeOffsets(FadePosition.TOP)

        drawRect(
            brush = Brush.linearGradient(
                colorStops = colorStops
                    .map { (stop, color) -> stop.coerceIn(0f, 1f) * fraction to color }
                    .toTypedArray(),
                start = start,
                end = end,
            ),
            size = this.size,
        )
    }
}

enum class FadePosition {
    TOP, BOTTOM, LEFT, RIGHT
}

@Stable
private fun Size.getFadeOffsets(position: FadePosition): Pair<Offset, Offset> {
    return when (position) {
        FadePosition.LEFT -> Offset.Zero to Offset(width, 0f)
        FadePosition.RIGHT -> Offset(width, y = 0f) to Offset.Zero
        FadePosition.BOTTOM -> Offset(x = 0f, height) to Offset.Zero
        FadePosition.TOP -> Offset.Zero to Offset(x = 0f, height)
    }
}