@file:Suppress("MagicNumber")

package com.tangem.core.ui.ds2.glowring

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.Dp
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min

/**
 * Token-driven measurements shared by both renderers, mirroring the Figma component anatomy:
 * a crisp [coreWidth] core line plus two wider, blurred glow bands ([ringWidth] stroked, blurred by
 * [blurMid] and [blurBottom]).
 */
internal data class GlowRingMetrics(
    val coreWidth: Dp, // crisp core stroke (top layer)
    val ringWidth: Dp, // glow band stroke (mid + bottom layers)
    val blurMid: Dp, // mid glow blur radius
    val blurBottom: Dp, // widest glow blur radius
)

/**
 * Tier 1 — works on every API level, no blur or shader. Approximates the blurred glow by stacking the
 * same breathing angular-gradient ring several times: progressively wider + fainter bands under a crisp
 * core. Everything is clipped to the rounded box, so only the inner half of each band shows → inner glow.
 */
@Composable
internal fun LayeredStrokesGlowRing(
    angle: Float,
    stops: List<Pair<Float, Color>>,
    cornerRadius: Dp,
    metrics: GlowRingMetrics,
    modifier: Modifier = Modifier,
) {
    Box(modifier.clip(RoundedCornerShape(cornerRadius))) {
        Canvas(Modifier.fillMaxSize()) {
            val r = cornerRadius.toPx()
            // widest & faintest first, crisp core last
            drawBreathingRing(
                stops = stops,
                angleDeg = angle,
                cornerRadiusPx = r,
                strokePx = (metrics.ringWidth + metrics.blurBottom).toPx(),
                alpha = 0.06f,
            )
            drawBreathingRing(
                stops = stops,
                angleDeg = angle,
                cornerRadiusPx = r,
                strokePx = (metrics.ringWidth + metrics.blurMid).toPx(),
                alpha = 0.12f,
            )
            drawBreathingRing(
                stops = stops,
                angleDeg = angle,
                cornerRadiusPx = r,
                strokePx = metrics.ringWidth.toPx(),
                alpha = 0.30f,
            )
            drawBreathingRing(
                stops = stops,
                angleDeg = angle,
                cornerRadiusPx = r,
                strokePx = metrics.coreWidth.toPx(),
                alpha = 1.0f,
            )
        }
    }
}

/**
 * Tier 2 — Android 12+ (API 31). Reproduces the Figma anatomy directly: three stacked breathing
 * angular-gradient rings with real blur (bottom widest, mid, top crisp). Each layer bleeds with
 * [BlurredEdgeTreatment.Unbounded]; the surrounding [clip] to the rounded box keeps only the inner
 * bloom, producing the inner glow.
 */
@Composable
internal fun BlurGlowRing(
    angle: Float,
    stops: List<Pair<Float, Color>>,
    cornerRadius: Dp,
    metrics: GlowRingMetrics,
    modifier: Modifier = Modifier,
) {
    Box(modifier.clip(RoundedCornerShape(cornerRadius))) {
        // bottom — widest halo
        BreathingRing(
            angle = angle,
            stops = stops,
            cornerRadius = cornerRadius,
            strokeWidth = metrics.ringWidth,
            modifier = Modifier
                .fillMaxSize()
                .blur(radius = metrics.blurBottom, edgeTreatment = BlurredEdgeTreatment.Unbounded),
        )
        // mid
        BreathingRing(
            angle = angle,
            stops = stops,
            cornerRadius = cornerRadius,
            strokeWidth = metrics.ringWidth,
            modifier = Modifier
                .fillMaxSize()
                .blur(radius = metrics.blurMid, edgeTreatment = BlurredEdgeTreatment.Unbounded),
        )
        // top — crisp core
        BreathingRing(
            angle = angle,
            stops = stops,
            cornerRadius = cornerRadius,
            strokeWidth = metrics.coreWidth,
            modifier = Modifier.fillMaxSize(),
        )
    }
}

@Composable
private fun BreathingRing(
    angle: Float,
    stops: List<Pair<Float, Color>>,
    cornerRadius: Dp,
    strokeWidth: Dp,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier) {
        drawBreathingRing(
            stops = stops,
            angleDeg = angle,
            cornerRadiusPx = cornerRadius.toPx(),
            strokePx = strokeWidth.toPx(),
            alpha = 1f,
        )
    }
}

/**
 * Draws one angular-gradient ring band clipped to the rounded-rect stroke outline. The gradient is a
 * sweep whose colour seam is rotated by [angleDeg] (via [rotatedStops]) and whose vertical squish
 * breathes between W/2 and W/8 over the rotation (`rxM = mid + amp·cos(2φ)`), reproducing the morphing
 * arcs of the reference rig.
 */
private fun DrawScope.drawBreathingRing(
    stops: List<Pair<Float, Color>>,
    angleDeg: Float,
    cornerRadiusPx: Float,
    strokePx: Float,
    alpha: Float,
) {
    val w = size.width
    val h = size.height
    if (w <= 0f || h <= 0f) return
    val center = Offset(w / 2f, h / 2f)

    // Breathing horizontal radius of the gradient ellipse → vertical squish of the angle sampling.
    val maxRx = w / 2f
    val minRx = w / 8f
    val mid = (maxRx + minRx) / 2f
    val amp = max((maxRx - minRx) / 2f, 0f)
    val phaseRad = Math.toRadians(angleDeg.toDouble()).toFloat()
    val rxM = mid + amp * cos(2f * phaseRad)
    val scaleY = h / 2f / max(rxM, 1f)

    val r = min(cornerRadiusPx, min(w, h) / 2f)
    val o = strokePx / 2f
    val ring = Path().apply {
        fillType = PathFillType.EvenOdd
        addRoundRect(
            RoundRect(rect = Rect(Offset(-o, -o), Size(w + 2f * o, h + 2f * o)), cornerRadius = CornerRadius(r + o)),
        )
        addRoundRect(
            RoundRect(
                rect = Rect(Offset(o, o), Size(w - 2f * o, h - 2f * o)),
                cornerRadius = CornerRadius(max(r - o, 0f)),
            ),
        )
    }

    val brush = Brush.sweepGradient(colorStops = rotatedStops(stops, angleDeg), center = center)
    val big = max(w, h) * 4f
    clipPath(ring) {
        withTransform({ scale(scaleX = 1f, scaleY = scaleY, pivot = center) }) {
            drawRect(
                brush = brush,
                topLeft = Offset(center.x - big / 2f, center.y - big / 2f),
                size = Size(big, big),
                alpha = alpha,
            )
        }
    }
}

/**
 * Compose's [Brush.sweepGradient] has no start-angle parameter, so the colour seam is rotated by
 * shifting every stop position by `deg/360` (wrapping around the loop) and re-anchoring boundary stops
 * at 0 and 1 with the interpolated wrap colour. Mirrors `rotatedStops` from the reference rig.
 */
private fun rotatedStops(base: List<Pair<Float, Color>>, deg: Float): Array<Pair<Float, Color>> {
    val d = (deg / 360f % 1f + 1f) % 1f
    val uniq = base.dropLast(1) // drop the duplicate wrap stop at 1.0
    val shifted = uniq
        .map { (p, c) -> ((p + d) % 1f + 1f) % 1f to c }
        .sortedBy { it.first }
    val first = shifted.first()
    val last = shifted.last()
    val span = first.first + 1f - last.first
    val wrapFraction = if (span > 1e-6f) (1f - last.first) / span else 0f
    val wrapColor = lerp(last.second, first.second, wrapFraction)
    return (listOf(0f to wrapColor) + shifted + listOf(1f to wrapColor)).toTypedArray()
}