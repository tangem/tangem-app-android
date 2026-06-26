package com.tangem.core.ui.ds2.for_you_temp

import android.content.res.Configuration
import android.graphics.BlurMaskFilter
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.core.ui.res.generated.TangemColorPalette

/**
 * Canvas-based take on the glow divider: the line background and every blurred color blob are painted by
 * hand inside a single `Box(Modifier.drawBehind { … })`.
 *
 * How it differs from [GlowDotsDivider] (which layers child `Box`es with [androidx.compose.ui.draw.blur]):
 * here there are no child composables at all. Inside [androidx.compose.ui.draw.drawBehind] we clip to the
 * capsule path, fill the base color, then draw each [GlowDot] as a circle whose native [Paint] carries a
 * [BlurMaskFilter] — the canvas equivalent of a Gaussian layer blur. The clip means a blob drawn past the
 * line's bounds only paints its color onto the visible capsule.
 *
 * @param modifier Modifier for positioning. Lays out to [lineWidth]×[lineHeight].
 * @param lineWidth Width of the capsule.
 * @param lineHeight Height of the capsule.
 * @param lineColor Solid base fill of the line.
 * @param dots Color blobs; [GlowDot.offset] is the blob center relative to the line's top-center,
 *   [GlowDot.size] its diameter, [GlowDot.blur] the mask-blur radius.
 */
data class CanvasGlowDot(
    val color: Color,
    val offset: DpOffset,
    val height: Dp,
    val blur: Dp = 8.dp,
)

@Suppress("MagicNumber", "LongParameterList")
@Composable
fun CanvasGradientDivider(modifier: Modifier = Modifier, lineWidth: Dp = 2.dp) {
    val shape = RoundedCornerShape(size = 100.dp)

    // Gentle "breathing" glow: pulse the drop-shadow alpha between MIN and MAX. Designer hasn't
    // provided timing yet, so 1600ms per direction reads as a calm, non-distracting pulse.
    val infiniteTransition = rememberInfiniteTransition(label = "GlowDividerShadow")
    val shadowAlpha by infiniteTransition.animateFloat(
        initialValue = GlowMinAlpha,
        targetValue = GlowMaxAlpha,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "GlowDividerShadowAlpha",
    )

    Box(
        modifier = modifier
            .width(lineWidth)
            .drawBehind {
                val resolvedDots = canvasDefaultDots(size.height.toDp())
                val cornerPx = size.width / 2f
                val capsule = Path().apply {
                    addRoundRect(
                        RoundRect(
                            left = 0f,
                            top = 0f,
                            right = size.width,
                            bottom = size.height,
                            cornerRadius = CornerRadius(cornerPx, cornerPx),
                        ),
                    )
                }

                clipPath(capsule) {
                    // Base background.
                    drawRect(color = LineColor)

                    // Blurred color blobs, drawn with a native BlurMaskFilter paint.
                    drawIntoCanvas { canvas ->
                        resolvedDots.forEach { dot ->
                            val blurPx = dot.blur.toPx()
                            val paint = Paint().apply {
                                color = dot.color
                                if (blurPx > 0f) {
                                    asFrameworkPaint().maskFilter =
                                        BlurMaskFilter(blurPx, BlurMaskFilter.Blur.NORMAL)
                                }
                            }

                            val ovalWidth = DotWidth.toPx()
                            val ovalHeight = dot.height.toPx()
                            val centerX = size.width / 2f + dot.offset.x.toPx()
                            val centerY = dot.offset.y.toPx() + ovalHeight / 2f
                            canvas.drawOval(
                                left = centerX - ovalWidth / 2f,
                                top = centerY - ovalHeight / 2f,
                                right = centerX + ovalWidth / 2f,
                                bottom = centerY + ovalHeight / 2f,
                                paint = paint,
                            )
                        }
                    }
                }
            }
            .dropShadow(
                shape = shape,
                shadow = Shadow(
                    radius = 12.dp,
                    spread = 0.dp,
                    color = TangemTheme.colors3.bg.brand.copy(alpha = shadowAlpha),
                ),
            )
            .border(width = 0.5.dp, color = TangemTheme.colors3.border.secondary, shape = shape),
    )
}

/** Glow pulse bounds for the animated drop shadow alpha. */
private const val GlowMinAlpha = 0.3f
private const val GlowMaxAlpha = 0.65f

/** Line fill — Figma "Background color" #0000F9 at 56% opacity (alpha 0x8F). */
private val LineColor = Color(0x8F0000F9)

/** Fixed blob width (the oval's horizontal diameter). */
private val DotWidth = 12.dp

/**
 * Placeholder snake-scatter of the four Figma "selection colors", sized proportionally to [lineHeight]
 * so the glow scales with the divider's length. Tune to match Figma.
 */
@Suppress("MagicNumber")
private fun canvasDefaultDots(lineHeight: Dp): List<CanvasGlowDot> {
    val step = lineHeight / 5
    return listOf(
        CanvasGlowDot(TangemColorPalette.Violet.`40`, DpOffset(x = 0.5.dp, y = (-3).dp), height = step), // purple
        CanvasGlowDot(TangemColorPalette.Green.`40`, DpOffset(x = 4.dp, y = step * 1.7f), height = step), // green
        CanvasGlowDot(TangemColorPalette.Blue.`40`, DpOffset(x = (-2.5).dp, y = step * 2.2f), height = step), // blue
        CanvasGlowDot(TangemColorPalette.Orange.`40`, DpOffset(x = (-3.5).dp, y = step * 3), height = step + step / 2),
        CanvasGlowDot(TangemColorPalette.Violet.`40`, DpOffset(x = 0.5.dp, y = step * 4 + 4.dp), height = step),
    )
}

// region Previews

@Preview(name = "Canvas glow • Light", showBackground = true)
@Preview(name = "Canvas glow • Dark", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PreviewCanvasGradientDivider() {
    TangemThemePreviewRedesign {
        Box(
            modifier = Modifier
                .background(TangemTheme.colors3.bg.primary)
                .padding(64.dp),
            contentAlignment = Alignment.Center,
        ) {
            // Height comes from the parent — here a fixed 46.dp.
            CanvasGradientDivider(modifier = Modifier.height(46.dp))
        }
    }
}

@Preview(name = "Length variants • Dark", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PreviewCanvasLengthVariants() {
    TangemThemePreviewRedesign {
        Row(
            modifier = Modifier
                .background(TangemTheme.colors3.bg.primary)
                .padding(64.dp),
            horizontalArrangement = Arrangement.spacedBy(48.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CanvasGradientDivider(modifier = Modifier.height(20.dp))
            CanvasGradientDivider(modifier = Modifier.height(46.dp))
            CanvasGradientDivider(modifier = Modifier.height(80.dp))
            CanvasGradientDivider(modifier = Modifier.height(140.dp))
            CanvasGradientDivider(modifier = Modifier.height(220.dp))
        }
    }
}

// endregion