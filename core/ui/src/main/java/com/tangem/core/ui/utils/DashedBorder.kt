package com.tangem.core.ui.utils

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Add a dashed border around a Composable component.
 *
 * @param color       color
 * @param shape       shape
 * @param strokeWidth stroke width
 * @param dashLength  length of each dash
 * @param gapLength   length of the gap between each dash
 * @param cap         stroke cap
 */
fun Modifier.dashedBorder(
    color: Color,
    shape: Shape,
    dashLength: Dp,
    gapLength: Dp,
    strokeWidth: Dp = 1.dp,
    cap: StrokeCap = StrokeCap.Round,
) = dashedBorder(
    brush = SolidColor(color),
    shape = shape,
    strokeWidth = strokeWidth,
    dashLength = dashLength,
    gapLength = gapLength,
    cap = cap,
)

/**
 * Add a dashed border around a Composable component.
 *
 * @param brush       brush
 * @param shape       shape
 * @param strokeWidth stroke width
 * @param dashLength  length of each dash
 * @param gapLength   length of the gap between each dash
 * @param cap         stroke cap
 */
fun Modifier.dashedBorder(
    brush: Brush,
    shape: Shape,
    strokeWidth: Dp = 2.dp,
    dashLength: Dp = 4.dp,
    gapLength: Dp = 4.dp,
    cap: StrokeCap = StrokeCap.Round,
) = this.drawWithContent {
    val outline = shape.createOutline(size = size, layoutDirection = layoutDirection, density = this)

    val dashedStroke = Stroke(
        cap = cap,
        width = strokeWidth.toPx(),
        pathEffect = PathEffect.dashPathEffect(
            intervals = floatArrayOf(dashLength.toPx(), gapLength.toPx()),
        ),
    )

    drawContent()

    drawOutline(outline = outline, style = dashedStroke, brush = brush)
}