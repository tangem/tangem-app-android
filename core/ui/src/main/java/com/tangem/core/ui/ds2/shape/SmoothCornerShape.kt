package com.tangem.core.ui.ds2.shape

import androidx.compose.runtime.Immutable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.toRect
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tan

/**
 * Rounded rectangle with Figma "Corner smoothing" (a squircle): each corner's circular arc is
 * shortened and blended into the straight edges with Bézier curves, so the curvature ramps up instead
 * of jumping at the tangent point. Port of the algorithm behind Figma's setting, as published in
 * [figma-squircle](https://github.com/phamfoo/figma-squircle).
 *
 * @param radius Figma "Corner radius". Clamped to half of the shorter side.
 * @param smoothing Figma "Corner smoothing", `0..1` (iOS-like is `0.6`). `0` draws plain circular
 *   corners. Reduced automatically when the shorter side is too small to fit the smoothed corner.
 */
@Immutable
data class SmoothCornerShape(val radius: Dp, val smoothing: Float = 0.6f) : Shape {

    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val budget = size.minDimension / 2
        val radiusPx = min(with(density) { radius.toPx() }, budget)
        if (radiusPx <= 0f) return Outline.Rectangle(size.toRect())

        val params = smoothCornerParams(radius = radiusPx, smoothing = smoothing.coerceIn(0f, 1f), budget = budget)
        return Outline.Generic(smoothCornerPath(size, params))
    }
}

/**
 * Geometry of one corner, in px. Walking clockwise into the top-right corner the outline leaves the
 * edge [p] before the corner, runs a cubic (handles [a], [a] + [b]) to the arc, follows [arcMeasure]
 * degrees of the [radius] circle and mirrors the cubic back onto the next edge.
 */
internal data class SmoothCornerParams(
    val radius: Float,
    val p: Float,
    val a: Float,
    val b: Float,
    val c: Float,
    val d: Float,
    val arcMeasure: Float,
)

@Suppress("MagicNumber")
internal fun smoothCornerParams(radius: Float, smoothing: Float, budget: Float): SmoothCornerParams {
    val fittedSmoothing = min(smoothing, budget / radius - 1)
    val p = min((1 + smoothing) * radius, budget)

    val arcMeasure = 90 * (1 - fittedSmoothing)
    val arcSectionLength = sin(toRadians(arcMeasure / 2)) * radius * sqrt(2f)
    val angleAlpha = (90 - arcMeasure) / 2
    val p3ToP4Distance = radius * tan(toRadians(angleAlpha / 2))
    val angleBeta = toRadians(45 * fittedSmoothing)
    val c = p3ToP4Distance * cos(angleBeta)
    val d = c * tan(angleBeta)
    val b = (p - arcSectionLength - c - d) / 3

    return SmoothCornerParams(radius = radius, p = p, a = 2 * b, b = b, c = c, d = d, arcMeasure = arcMeasure)
}

private fun smoothCornerPath(size: Size, params: SmoothCornerParams): Path = Path().apply {
    val width = size.width
    val height = size.height
    val radius = params.radius
    val p = params.p

    moveTo(width - p, 0f)
    smoothCorner(params, center = Offset(width - radius, radius), quarterTurns = 0)
    lineTo(width, height - p)
    smoothCorner(params, center = Offset(width - radius, height - radius), quarterTurns = 1)
    lineTo(p, height)
    smoothCorner(params, center = Offset(radius, height - radius), quarterTurns = 2)
    lineTo(0f, p)
    smoothCorner(params, center = Offset(radius, radius), quarterTurns = 3)
    close()
}

/** Draws a corner; [quarterTurns] rotates the top-right corner clockwise onto the others. */
@Suppress("MagicNumber")
private fun Path.smoothCorner(params: SmoothCornerParams, center: Offset, quarterTurns: Int) = with(params) {
    relativeCubicTo(Offset(a, 0f), Offset(a + b, 0f), Offset(a + b + c, d), quarterTurns)
    arcTo(
        rect = Rect(center = center, radius = radius),
        startAngleDegrees = quarterTurns * 90f - 45f - arcMeasure / 2,
        sweepAngleDegrees = arcMeasure,
        forceMoveTo = false,
    )
    relativeCubicTo(Offset(d, c), Offset(d, b + c), Offset(d, a + b + c), quarterTurns)
}

private fun Path.relativeCubicTo(control1: Offset, control2: Offset, end: Offset, quarterTurns: Int) {
    val c1 = control1.rotated(quarterTurns)
    val c2 = control2.rotated(quarterTurns)
    val e = end.rotated(quarterTurns)
    relativeCubicTo(c1.x, c1.y, c2.x, c2.y, e.x, e.y)
}

@Suppress("MagicNumber")
private fun Offset.rotated(quarterTurns: Int): Offset = when (quarterTurns % 4) {
    0 -> this
    1 -> Offset(-y, x)
    2 -> Offset(-x, -y)
    else -> Offset(y, -x)
}

private fun toRadians(degrees: Float): Float = Math.toRadians(degrees.toDouble()).toFloat()