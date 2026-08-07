package com.tangem.core.ui.extensions

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp

/**
 * Draws a soft drop shadow behind the content.
 *
 * @param radius Blur size as authored in the design tool — a Figma / CSS `box-shadow` blur, i.e.
 *   twice the Gaussian sigma. It is converted to the framework's blur radius internally. Values
 *   too small to produce a visible blur render nothing.
 * @param color Shadow color. Only its alpha and hue matter; the shape itself is never filled.
 * @param shape Shape the shadow is cast from.
 * @param spread Grows (positive) or shrinks (negative) the shadow relative to [shape].
 * @param offset Shadow displacement.
 * @param isAlphaContentClip Cuts [shape]'s own area out of the shadow. Enable it when the content
 *   drawn on top is translucent, otherwise the blur shows through it.
 */
fun Modifier.softLayerShadow(
    radius: Dp = 8.dp,
    color: Color = Color.Black.copy(alpha = .23f),
    shape: Shape = RectangleShape,
    spread: Dp = 0.dp,
    offset: DpOffset = DpOffset(x = 0.dp, y = 2.dp),
    isAlphaContentClip: Boolean = false,
): Modifier = this.drawWithCache {
    val radiusPx = designBlurToShadowRadiusPx(radius.toPx())
    if (radiusPx <= 0f) return@drawWithCache onDrawBehind {}

    val paint = Paint().apply {
        this.color = color.copy(alpha = 0f)

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
        layoutDirection = layoutDirection,
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

/**
 * Converts a design-tool blur value to the blur radius expected by `Paint.setShadowLayer`.
 *
 * A Figma / CSS `box-shadow` blur of `B` describes a Gaussian with `sigma = B / 2`, whereas the
 * framework derives `sigma = 0.57735 * radius + 0.5`. Passing `B` straight through renders the
 * shadow ~16% wider than designed.
 */
private fun designBlurToShadowRadiusPx(blurPx: Float): Float = (blurPx / 2f - BLUR_SIGMA_INTERCEPT) / BLUR_SIGMA_SLOPE

private const val BLUR_SIGMA_SLOPE = 0.57735f
private const val BLUR_SIGMA_INTERCEPT = 0.5f

private fun DrawScope.clipShadowByPath(path: Path, block: DrawScope.() -> Unit) {
    clipPath(
        path = path,
        clipOp = ClipOp.Difference,
        block = block,
    )
}