package com.tangem.core.ui.ds2.shimmers

import android.content.res.Configuration
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import kotlin.math.cos
import kotlin.math.sin

/**
 * Design-system v2 shimmer placeholder — a rounded rectangle with a sweeping highlight.
 *
 * [Figma](https://www.figma.com/design/AsnJ5CPHib4Qxw12gszjMS/%F0%9F%92%A0-DS-Components?node-id=3398-625&p=f&m=dev)
 *
 * For a placeholder sized after a typography line, use the [TangemShimmer] text overload instead.
 *
 * @param modifier Modifier applied to the shimmer's root. Set the width and height here.
 * @param radius Corner radius of the rectangle.
 */
@Composable
fun TangemShimmer(modifier: Modifier = Modifier, radius: Dp = TangemShimmer.DefaultRadius) {
    val baseColor = TangemTheme.colors3.bg.opaque.secondary
    val progress = LocalTangemShimmerProgress.current ?: rememberShimmerProgressInstance()
    val colorStops = remember(baseColor) { buildColorStops(baseColor) }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(radius))
            .drawWithCache {
                // Stable per layout — recomputed only when size or density changes.
                val shimmerWidthPx = SHIMMER_WIDTH.toPx()
                val coverage = size.width * SHIMMER_DX + size.height * SHIMMER_DY
                val travel = coverage + shimmerWidthPx
                val halfWidth = shimmerWidthPx / 2f
                onDrawBehind {
                    val center = -halfWidth + progress.value * travel
                    drawRect(
                        brush = Brush.linearGradient(
                            colorStops = colorStops,
                            start = Offset(
                                x = (center - halfWidth) * SHIMMER_DX,
                                y = (center - halfWidth) * SHIMMER_DY,
                            ),
                            end = Offset(
                                x = (center + halfWidth) * SHIMMER_DX,
                                y = (center + halfWidth) * SHIMMER_DY,
                            ),
                        ),
                    )
                }
            },
    )
}

/**
 * Text-line shimmer placeholder, sized and styled after the typography line described by [style].
 *
 * [Figma](https://www.figma.com/design/AsnJ5CPHib4Qxw12gszjMS/%F0%9F%92%A0-DS-Components?node-id=3398-625&p=f&m=dev)
 *
 * @param style A [TangemTheme.typography3] style (e.g. `TangemTheme.typography3.body.medium`) the
 * placeholder is sized after. Unrecognized styles fall back to `body.medium`.
 * @param modifier Modifier applied to the shimmer's root.
 * @param textAlign Horizontal position of the block within the parent width.
 */
@Composable
fun TangemShimmer(style: TextStyle, modifier: Modifier = Modifier, textAlign: TextAlign = TextAlign.Start) {
    val preset = TangemShimmer.TextPreset.forStyle(style)
    val lineHeightDp = with(LocalDensity.current) { style.lineHeight.toDp() }
    val alignment = when (textAlign) {
        TextAlign.Center -> Alignment.Center
        TextAlign.End -> Alignment.CenterEnd
        else -> Alignment.CenterStart
    }

    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = alignment,
    ) {
        TangemShimmer(
            modifier = Modifier
                .fillMaxWidth(preset.widthFraction)
                .height(lineHeightDp)
                .padding(vertical = preset.verticalPadding),
            radius = preset.radius,
        )
    }
}

/** Public API namespace for [TangemShimmer]. */
object TangemShimmer {

    /** Default corner radius of the rectangle shimmer. */
    val DefaultRadius: Dp = 6.dp

    /** Per-typography sizing for the [TangemShimmer] text overload. */
    internal enum class TextPreset(val widthFraction: Float, val verticalPadding: Dp, val radius: Dp) {
        Display(widthFraction = 0.5f, verticalPadding = 4.dp, radius = 12.dp),
        HeadingMedium(widthFraction = 0.7f, verticalPadding = 2.dp, radius = 8.dp),
        HeadingSmall(widthFraction = 0.6f, verticalPadding = 2.dp, radius = 16.dp),
        Body(widthFraction = 0.5f, verticalPadding = 2.dp, radius = 16.dp),
        Subheading(widthFraction = 0.4f, verticalPadding = 2.dp, radius = 16.dp),
        Caption(widthFraction = 0.3f, verticalPadding = 2.dp, radius = 16.dp),
        ;

        companion object {
            @Composable
            @ReadOnlyComposable
            fun forStyle(style: TextStyle): TextPreset {
                val typography = TangemTheme.typography3
                return when (style) {
                    typography.display.medium -> Display
                    typography.heading.medium -> HeadingMedium
                    typography.heading.small -> HeadingSmall
                    typography.subheading.medium -> Subheading
                    typography.caption.medium -> Caption
                    else -> Body
                }
            }
        }
    }
}

/**
 * Wraps [content] so every [TangemShimmer] inside shares a single, in-phase animation driver —
 * use it around lists of shimmers. Safe to nest; safe to omit.
 */
@Composable
fun ProvideTangemShimmer(content: @Composable () -> Unit) {
    CompositionLocalProvider(
        LocalTangemShimmerProgress provides rememberShimmerProgressInstance(),
        content = content,
    )
}

private val LocalTangemShimmerProgress = compositionLocalOf<State<Float>?> { null }

@Composable
private fun rememberShimmerProgressInstance(): State<Float> {
    val transition = rememberInfiniteTransition(label = "TangemShimmer")
    return transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = SHIMMER_DURATION_MS,
                delayMillis = SHIMMER_DELAY_MS,
                easing = LinearEasing,
            ),
            repeatMode = RepeatMode.Restart,
        ),
        label = "TangemShimmerProgress",
    )
}

private fun buildColorStops(baseColor: Color): Array<Pair<Float, Color>> = SHIMMER_ALPHA_STOPS
    .map { (position, factor) -> position to baseColor.copy(alpha = baseColor.alpha * factor) }
    .toTypedArray()

private val SHIMMER_WIDTH: Dp = 400.dp
private const val SHIMMER_DURATION_MS = 800
private const val SHIMMER_DELAY_MS = 1_500
private const val SHIMMER_ROTATION_DEG = 15.0
private val SHIMMER_DX = cos(Math.toRadians(SHIMMER_ROTATION_DEG)).toFloat()
private val SHIMMER_DY = sin(Math.toRadians(SHIMMER_ROTATION_DEG)).toFloat()

/** Alpha profile borrowed from BladeAnimation — a wide, gradual dim through the band's center. */
private val SHIMMER_ALPHA_STOPS: List<Pair<Float, Float>> = listOf(
    0f to 1f,
    0.15f to 0.75f,
    0.35f to 0.45f,
    0.5f to 0.3f,
    0.65f to 0.45f,
    0.85f to 0.75f,
    1f to 1f,
)

// region Previews

@Preview(name = "Light", showBackground = true)
@Preview(name = "Dark", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Composable
private fun TangemShimmerPreview() {
    TangemThemePreviewRedesign {
        Column(
            modifier = Modifier
                .background(TangemTheme.colors3.bg.primary)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            TangemShimmer(
                modifier = Modifier.size(width = 200.dp, height = 24.dp),
                radius = 6.dp,
            )
            TangemShimmer(
                modifier = Modifier.size(width = 120.dp, height = 16.dp),
                radius = 4.dp,
            )
            TangemShimmer(style = TangemTheme.typography3.body.medium)
            TangemShimmer(style = TangemTheme.typography3.heading.medium)
        }
    }
}

// endregion