@file:Suppress("MagicNumber")

package com.tangem.core.ui.ds2.glowring

import android.os.Build
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.core.ui.res.generated.TangemColors3

/**
 * Design-system v2 (DS3) **Glow Ring** — an animated angular-gradient halo that runs around a
 * rounded-rect outline, like lights chasing along the border.
 *
 * [Figma](https://www.figma.com/design/AsnJ5CPHib4Qxw12gszjMS/%F0%9F%92%A0-DS-Components?node-id=4933-126&m=dev)
 *
 * @param modifier Modifier for the whole component; also defines its size when there is no [content].
 * @param variant Color theme of the gradient — see [TangemGlowRing.Variant].
 * @param cornerRadius Corner radius of the ring; should match the radius of the wrapped surface.
 * @param animated When `false`, the ring is rendered static (no rotation).
 * @param quality Rendering strategy; defaults to [TangemGlowRing.Quality.Auto] (device-appropriate).
 * Force [TangemGlowRing.Quality.LayeredStrokes] to preview the pre-Android-12 fallback on any device.
 * @param contentDescription Accessibility label; pass a value when the ring conveys state (e.g. error),
 * leave `null` when it is purely decorative.
 * @param content Optional content drawn inside/over the ring.
 */
@Composable
fun TangemGlowRing(
    modifier: Modifier = Modifier,
    variant: TangemGlowRing.Variant = TangemGlowRing.Variant.Magic,
    cornerRadius: Dp = 24.dp,
    animated: Boolean = true,
    quality: TangemGlowRing.Quality = TangemGlowRing.Quality.Auto,
    contentDescription: String? = null,
    content: @Composable BoxScope.() -> Unit = {},
) {
    val resolved = remember(quality) { resolveQuality(quality) }
    val stops = rememberGlowRingStops(variant, animated)
    val metrics = remember {
        GlowRingMetrics(coreWidth = 2.dp, ringWidth = 4.dp, blurMid = 8.dp, blurBottom = 16.dp)
    }

    val angle = if (animated) {
        val transition = rememberInfiniteTransition(label = "glowRing")
        val rotation by transition.animateFloat(
            initialValue = GLOW_RING_START_ANGLE,
            targetValue = 270f,
            animationSpec = infiniteRepeatable(
                animation = tween(
                    durationMillis = 24_000,
                    easing = CubicBezierEasing(a = 0.1f, b = 0f, c = 0.9f, d = 1f),
                ),
                repeatMode = RepeatMode.Restart,
            ),
            label = "angle",
        )
        rotation
    } else {
        GLOW_RING_START_ANGLE
    }

    Box(
        modifier = if (contentDescription != null) {
            modifier.semantics { this.contentDescription = contentDescription }
        } else {
            modifier
        },
    ) {
        val ringModifier = Modifier.matchParentSize()
        when (resolved) {
            ResolvedGlowRingQuality.Blur -> BlurGlowRing(
                angle = angle,
                stops = stops,
                cornerRadius = cornerRadius,
                metrics = metrics,
                modifier = ringModifier,
            )
            ResolvedGlowRingQuality.LayeredStrokes -> LayeredStrokesGlowRing(
                angle = angle,
                stops = stops,
                cornerRadius = cornerRadius,
                metrics = metrics,
                modifier = ringModifier,
            )
        }
        content()
    }
}

/** Sweep start angle, also reused as the static angle when [TangemGlowRing] is not animated (Figma: -90°). */
private const val GLOW_RING_START_ANGLE = -90f

/**
 * Resolves the gradient stops for [variant] from the DS3 `colors3.glow` tokens. The
 * [TangemGlowRing.Variant.Magic] variant continuously ping-pongs between gradient A (`glow.magic`) and
 * gradient B (`glow.magicBlend`) while [animated] is `true`; every other variant has a single static
 * gradient.
 */
@Composable
private fun rememberGlowRingStops(variant: TangemGlowRing.Variant, animated: Boolean): List<Pair<Float, Color>> {
    val glow = TangemTheme.colors3.glow
    if (variant != TangemGlowRing.Variant.Magic || !animated) {
        return variant.stops(glow)
    }
    val morphTransition = rememberInfiniteTransition(label = "glowRingMorph")
    val mix by morphTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            // 6s A→B half-period; Reverse makes a 12s ping-pong (Figma morphDur = 12s).
            animation = tween(durationMillis = 6_000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "morphMix",
    )
    return morphedMagicStops(glow.magic.steps(), glow.magicBlend.steps(), mix)
}

/** Public API surface of [TangemGlowRing]. */
object TangemGlowRing {

    /** Color theme of the glow ring gradient. */
    enum class Variant {
        /**
         * Multi-color "magic" gradient that continuously auto-morphs (ping-pongs) between separated
         * orange / blue / purple arcs and a continuous fully-saturated blend.
         */
        Magic,

        /** Green success glow. */
        Success,

        /** Red error glow. */
        Error,

        /** Orange/amber warning glow. */
        Warning,

        /** Blue informational glow. */
        Info,
    }

    /**
     * Rendering strategy for the glow.
     *
     * [Auto] picks the best renderer for the current device — a real Gaussian blur on Android 12+
     * (API 31) and a layered-stroke approximation on older versions. The explicit values force one
     * renderer regardless of API level; they exist mainly for previews / Storybook so the
     * pre-Android-12 fallback can be inspected on a modern device. Product code should use [Auto].
     */
    enum class Quality {
        /** Auto-detect the renderer from the device API level (recommended). */
        Auto,

        /** Force the Android 12+ real-blur renderer. */
        Blur,

        /** Force the pre-Android-12 layered-stroke fallback. */
        LayeredStrokes,
    }
}

/**
 * Resolves [quality] to a concrete renderer. [TangemGlowRing.Quality.Auto] picks a real blur on
 * Android 12+ (API 31) and falls back to stacked translucent strokes on older versions; the explicit
 * values force their renderer regardless of API level.
 */
private fun resolveQuality(quality: TangemGlowRing.Quality): ResolvedGlowRingQuality = when (quality) {
    TangemGlowRing.Quality.Blur -> ResolvedGlowRingQuality.Blur
    TangemGlowRing.Quality.LayeredStrokes -> ResolvedGlowRingQuality.LayeredStrokes
    TangemGlowRing.Quality.Auto -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        ResolvedGlowRingQuality.Blur
    } else {
        ResolvedGlowRingQuality.LayeredStrokes
    }
}

/** Concrete rendering strategy chosen by [resolveQuality]. */
private enum class ResolvedGlowRingQuality { Blur, LayeredStrokes }

/**
 * Builds the angular-gradient stops for [this] variant from its DS3 `colors3.glow` token group. Every
 * variant token exposes the same 10 [steps] — solid arcs at steps 1/4/7, a faint arc at 9 and transparent
 * gaps elsewhere — which [glowStops] lays out as evenly-spaced, seamlessly-looping stops.
 */
private fun TangemGlowRing.Variant.stops(glow: TangemColors3.Glow): List<Pair<Float, Color>> = glowStops(
    when (this) {
        TangemGlowRing.Variant.Magic -> glow.magic.steps()
        TangemGlowRing.Variant.Success -> glow.success.steps()
        TangemGlowRing.Variant.Error -> glow.error.steps()
        TangemGlowRing.Variant.Warning -> glow.warning.steps()
        TangemGlowRing.Variant.Info -> glow.info.steps()
    },
)

/**
 * Blends the Magic gradients A ([magic] = `glow.magic`) and B ([magicBlend] = `glow.magicBlend`) at
 * [mix] (`0` = A, `1` = B). Both token groups share the same stop positions, so the morph is a direct
 * per-step color lerp. Mirrors the reference rig's auto-morph (ping-pong) between gradient A and B.
 */
private fun morphedMagicStops(magic: List<Color>, magicBlend: List<Color>, mix: Float): List<Pair<Float, Color>> {
    val m = mix.coerceIn(0f, 1f)
    return glowStops(List(magic.size) { lerp(magic[it], magicBlend[it], m) })
}

/**
 * Lays the glow [steps] out as an angular gradient: evenly spaced from `0`, with step 1 repeated at `1.0`
 * so the rotation loops seamlessly. Transparent steps create the gaps between the glowing arcs.
 */
private fun glowStops(steps: List<Color>): List<Pair<Float, Color>> {
    val count = steps.size
    return steps.mapIndexed { index, color -> index.toFloat() / count to color } + (1f to steps.first())
}

private fun TangemColors3.Glow.Magic.steps(): List<Color> =
    listOf(step1, step2, step3, step4, step5, step6, step7, step8, step9, step10)

private fun TangemColors3.Glow.MagicBlend.steps(): List<Color> =
    listOf(step1, step2, step3, step4, step5, step6, step7, step8, step9, step10)

private fun TangemColors3.Glow.Success.steps(): List<Color> =
    listOf(step1, step2, step3, step4, step5, step6, step7, step8, step9, step10)

private fun TangemColors3.Glow.Error.steps(): List<Color> =
    listOf(step1, step2, step3, step4, step5, step6, step7, step8, step9, step10)

private fun TangemColors3.Glow.Warning.steps(): List<Color> =
    listOf(step1, step2, step3, step4, step5, step6, step7, step8, step9, step10)

private fun TangemColors3.Glow.Info.steps(): List<Color> =
    listOf(step1, step2, step3, step4, step5, step6, step7, step8, step9, step10)

@Preview(name = "Light", showBackground = true)
@Preview(name = "Dark", uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Composable
private fun TangemGlowRingPreview() {
    TangemThemePreviewRedesign {
        Column(
            modifier = Modifier
                .background(TangemTheme.colors3.bg.primary)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                TangemGlowRing(
                    modifier = Modifier.size(120.dp, 72.dp),
                    variant = TangemGlowRing.Variant.Magic,
                )
                TangemGlowRing(
                    modifier = Modifier.size(120.dp, 72.dp),
                    variant = TangemGlowRing.Variant.Success,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                TangemGlowRing(
                    modifier = Modifier.size(120.dp, 72.dp),
                    variant = TangemGlowRing.Variant.Error,
                )
                TangemGlowRing(
                    modifier = Modifier.size(120.dp, 72.dp),
                    variant = TangemGlowRing.Variant.Warning,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                TangemGlowRing(
                    modifier = Modifier.size(120.dp, 72.dp),
                    variant = TangemGlowRing.Variant.Info,
                )
            }
        }
    }
}