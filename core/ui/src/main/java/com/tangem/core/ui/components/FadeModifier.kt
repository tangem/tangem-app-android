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
): Modifier = composed {
    require(value = size > 0.dp) {
        "Size must be greater than '0'"
    }
    val animatedSize = animationSpec?.let {
        animateDpAsState(
            targetValue = if (isVisible) size else 0.dp,
            animationSpec = it,
            label = "Edge fade width",
        )
    }

    drawWithContent {
        drawContent()

        positions.forEach { side ->
            val (start, end) = this.size.getFadeOffsets(side)

            val staticSizePx = if (isVisible) size.toPx() else 0f
            val sizePx = animatedSize?.value?.toPx() ?: staticSizePx

            val fraction = when (side) {
                FadePosition.LEFT, FadePosition.RIGHT -> sizePx / this.size.width
                FadePosition.BOTTOM, FadePosition.TOP -> sizePx / this.size.height
            }

            drawRect(
                brush = Brush.linearGradient(
                    0f to color,
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