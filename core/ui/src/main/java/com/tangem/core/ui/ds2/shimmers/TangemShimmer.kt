package com.tangem.core.ui.ds2.shimmers

import android.content.res.Configuration
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import kotlin.math.cos
import kotlin.math.sin

/**
 * Design-system rectangle shimmer placeholder.
 *
 * A rounded rectangle painted with `bg.opaque.secondary`. A tilted band sweeps across it where
 * the base color's alpha is gradually dimmed toward the center of the band and restored at the
 * edges, producing a soft "blade" highlight passing through the placeholder. The alpha profile
 * matches [com.tangem.core.ui.components.text.BladeAnimation].
 *
 * Cycle: 1.5s hold → 0.8s linear sweep → restart.
 *
 * Version 1.0
 * [Figma](https://www.figma.com/design/AsnJ5CPHib4Qxw12gszjMS/%F0%9F%92%A0-DS-Components?node-id=3398-625&p=f&m=dev)
 *
 * Sizing is the caller's responsibility — set width and height via [modifier].
 *
 * @param modifier Modifier applied to the shimmer's root.
 * @param radius Corner radius of the rectangle.
 */
@Composable
fun RectangleShimmer(modifier: Modifier = Modifier, radius: Dp = 6.dp) {
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
 * Text-sized shimmer placeholder. Sizes itself to the bounding box of the [text] measured in the
 * typography preset selected by [style], plus the preset's vertical padding (top + bottom).
 *
 * @param text Text used to determine the shimmer's size. Not drawn.
 * @param style Typography preset — drives both the measurement style and the vertical padding.
 * @param radius Corner radius of the rectangle.
 * @param modifier Modifier applied to the shimmer's root.
 */
@Composable
fun TextShimmer(text: String, style: TextShimmerStyle, radius: Dp, modifier: Modifier = Modifier) {
    val textStyle = style.toTextStyle()
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val (widthDp, heightDp) = remember(text, textStyle, measurer, density) {
        val measured = measurer.measure(text = text, style = textStyle)
        with(density) { measured.size.width.toDp() to measured.size.height.toDp() }
    }

    RectangleShimmer(
        modifier = modifier.size(
            width = widthDp,
            height = heightDp + style.verticalPadding * 2,
        ),
        radius = radius,
    )
}

/**
 * Typography preset for [TextShimmer]. Each preset maps to a [TangemTheme.typography3] style
 * and contributes additional [verticalPadding] applied to both top and bottom — the shimmer
 * block ends up `2 * verticalPadding` taller than the raw measured text.
 */
enum class TextShimmerStyle(val verticalPadding: Dp) {
    DISPLAY(verticalPadding = 4.dp),
    HEADING_MEDIUM(verticalPadding = 2.dp),
    HEADING_SMALL(verticalPadding = 2.dp),
    BODY(verticalPadding = 2.dp),
    SUBHEADING(verticalPadding = 2.dp),
    CAPTION(verticalPadding = 2.dp),
}

@Composable
@ReadOnlyComposable
private fun TextShimmerStyle.toTextStyle(): TextStyle = when (this) {
    TextShimmerStyle.DISPLAY -> TangemTheme.typography3.display.medium
    TextShimmerStyle.HEADING_MEDIUM -> TangemTheme.typography3.heading.medium
    TextShimmerStyle.HEADING_SMALL -> TangemTheme.typography3.heading.small
    TextShimmerStyle.BODY -> TangemTheme.typography3.body.medium
    TextShimmerStyle.SUBHEADING -> TangemTheme.typography3.subheading.medium
    TextShimmerStyle.CAPTION -> TangemTheme.typography3.caption.medium
}

/**
 * Wraps [content] so every [RectangleShimmer] / [TextShimmer] inside reuses a single shimmer
 * animation driver. Without this provider each shimmer creates its own
 * [rememberInfiniteTransition] — that scales poorly in lists and lets sweeps drift out of phase.
 * Safe to nest; safe to omit (each shimmer falls back to its own driver).
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
            RectangleShimmer(
                modifier = Modifier.size(width = 200.dp, height = 24.dp),
                radius = 6.dp,
            )
            RectangleShimmer(
                modifier = Modifier.size(width = 120.dp, height = 16.dp),
                radius = 4.dp,
            )
            TextShimmer(
                text = "Account balance",
                style = TextShimmerStyle.BODY,
                radius = 4.dp,
            )
            TextShimmer(
                text = "$12,345.67",
                style = TextShimmerStyle.HEADING_MEDIUM,
                radius = 6.dp,
            )
        }
    }
}

// endregion