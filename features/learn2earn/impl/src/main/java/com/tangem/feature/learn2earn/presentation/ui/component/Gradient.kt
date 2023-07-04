package com.tangem.feature.learn2earn.presentation.ui.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp

/**
* [REDACTED_AUTHOR]
 */
@Composable
internal fun GradientCircle(
    size: Dp,
    offsetX: Dp,
    offsetY: Dp,
    startColor: Color,
    endColor: Color,
    radius: Dp = size / 2,
) {
    Canvas(
        modifier = Modifier
            .size(size)
            .offset(x = offsetX, y = offsetY),
        onDraw = {
            drawCircle(
                radius = radius.toPx(),
                brush = Brush.radialGradient(
                    radius = radius.toPx(),
                    colors = listOf(startColor, endColor),
                ),
            )
        },
    )
}
