package com.tangem.core.ui.extensions

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

fun Modifier.softLayerShadow(
    radius: Dp = 8.dp,
    color: Color = Color.Black.copy(alpha = .23f),
    shape: Shape = RectangleShape,
    spread: Dp = 0.dp,
    offset: DpOffset = DpOffset(x = 0.dp, y = 2.dp),
    isAlphaContentClip: Boolean = false,
): Modifier = this.drawWithCache {
    val radiusPx = radius.toPx()
    require(radiusPx > 0.0F)
    val paint = Paint().apply {
        this.color = color

        asFrameworkPaint().apply {
            isDither = true
            isAntiAlias = true

            setShadowLayer(
                radiusPx,
                offset.x.toPx(),
                offset.y.toPx(),
                color.toArgb(),
            )
        }
    }
    val shapeOutline = shape.createOutline(
        size = size,
        layoutDirection = LayoutDirection.Rtl,
        density = this,
    )
    val shapePath = Path().apply {
        addOutline(outline = shapeOutline)
    }

    val drawShadowBlock: DrawScope.() -> Unit = {
        drawIntoCanvas { canvas ->
            canvas.withSave {
                if (spread.value != 0.0F) {
                    canvas.scale(
                        sx = spreadScale(
                            spread = spread.toPx(),
                            size = size.width,
                        ),
                        sy = spreadScale(
                            spread = spread.toPx(),
                            size = size.height,
                        ),
                        pivotX = center.x,
                        pivotY = center.y,
                    )
                }

                canvas.drawOutline(
                    outline = shapeOutline,
                    paint = paint,
                )
            }
        }
    }

    onDrawBehind {
        if (isAlphaContentClip) {
            clipShadowByPath(
                path = shapePath,
                block = drawShadowBlock,
            )
        } else {
            drawShadowBlock()
        }
    }
}

@Suppress("UnnecessaryParentheses")
private fun spreadScale(spread: Float, size: Float): Float = 1.0F + ((spread / size) * 2.0F)

private fun DrawScope.clipShadowByPath(path: Path, block: DrawScope.() -> Unit) {
    clipPath(
        path = path,
        clipOp = ClipOp.Difference,
        block = block,
    )
}