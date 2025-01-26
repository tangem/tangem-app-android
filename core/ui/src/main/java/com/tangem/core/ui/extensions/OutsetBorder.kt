package com.tangem.core.ui.extensions

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.isSimple
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp

/**
 * Modifier that adds an outset border to the content.
 * @see [androidx.compose.foundation.border]
 *
 * @param width The width of the border.
 * @param color The color of the border.
 * @param shape The shape of the border.
 */
fun Modifier.outsetBorder(width: Dp, color: Color, shape: Shape) = drawWithCache {
    val outline = shape.createOutline(size, layoutDirection, this)
    val cornerRadius = when {
        outline is Outline.Rounded && outline.roundRect.isSimple -> outline.roundRect.topLeftCornerRadius
        else -> CornerRadius.Zero
    }

    onDrawWithContent {
        drawContent()
        drawRoundRect(
            color = color,
            topLeft = Offset(-width.toPx() / 2, -width.toPx() / 2),
            size = size.copy(
                width = size.width + width.toPx(),
                height = size.height + width.toPx(),
            ),
            cornerRadius = cornerRadius,
            style = Stroke(width = width.toPx()),
        )
    }
}