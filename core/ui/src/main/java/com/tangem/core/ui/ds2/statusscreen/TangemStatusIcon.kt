@file:Suppress("MagicNumber")

package com.tangem.core.ui.ds2.statusscreen

import android.content.res.Configuration
import android.graphics.BlurMaskFilter
import android.os.Build
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.PaintingStyle
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.core.ui.res.generated.TangemColorPalette

/**
 * Design-system v2 animated status icon for [TangemStatusScreen]'s asset slot: an outline glyph that
 * draws itself on (stroke trim), pulses in scale and glows, per the "Asset Breakdown" and keyframes in
 * the DS figma. Drawn in code rather than played from a lottie so the tint comes from the theme.
 * Below Android 9 the blur-based glow and highlight passes are skipped (mask filters are ignored by the
 * hardware canvas there) and only the crisp tinted stroke is drawn.
 *
 * [Figma](https://www.figma.com/design/AsnJ5CPHib4Qxw12gszjMS/%F0%9F%92%A0-DS-Components?node-id=9267-11586)
 *
 * The glyph is authored on a 40dp grid and scales to whatever size the [modifier] gives it — size it
 * explicitly (canonically `Modifier.size(40.dp)`).
 *
 * @param kind The glyph to draw — see [TangemStatusIcon.Kind].
 * @param modifier Modifier for the icon; must provide the size.
 * @param tint Stroke and glow color; defaults to the status color matching [kind].
 * @param animated Whether to play the draw-on animation; `false` renders the final frame.
 * @param contentDescription Accessibility label; pass a value when the icon is the only carrier of the
 * status, `null` when the screen already announces it in text.
 */
@Composable
fun TangemStatusIcon(
    kind: TangemStatusIcon.Kind,
    modifier: Modifier = Modifier,
    tint: Color = kind.defaultTint(),
    animated: Boolean = true,
    contentDescription: String? = null,
) {
    val entrance = rememberEntranceProgress(durationMs = ICON_DURATION_MS, animated = animated)
    val glyph = remember(kind) { kind.glyph() }
    val paints = remember(tint) { GlyphPaints(tint = tint) }
    val measure = remember { android.graphics.PathMeasure() }
    val trimmed = remember { android.graphics.Path() }

    Canvas(
        modifier = if (contentDescription != null) {
            modifier.semantics { this.contentDescription = contentDescription }
        } else {
            modifier
        },
    ) {
        scale(scale = entrance.pulseScale()) {
            scale(scale = size.minDimension / GRID, pivot = Offset.Zero) {
                drawGlyph(glyph = glyph, entrance = entrance, paints = paints, measure = measure, trimmed = trimmed)
            }
        }
    }
}

object TangemStatusIcon {

    /** The glyphs shipped by the DS: one per [TangemStatusScreen.Variant] mood except Neutral. */
    enum class Kind { Checkmark, HeartBroken, Warning, Info }
}

@Composable
private fun TangemStatusIcon.Kind.defaultTint(): Color = when (this) {
    TangemStatusIcon.Kind.Checkmark -> TangemTheme.colors3.icon.status.success
    TangemStatusIcon.Kind.HeartBroken -> TangemTheme.colors3.icon.status.error
    TangemStatusIcon.Kind.Warning -> TangemTheme.colors3.icon.status.warning
    TangemStatusIcon.Kind.Info -> TangemTheme.colors3.icon.status.info
}

private fun EntranceProgress.pulseScale(): Float {
    val rise = segment(startMs = 0, endMs = PULSE_PEAK_MS, easing = PulseEase)
    val settle = segment(startMs = PULSE_PEAK_MS, endMs = ICON_DURATION_MS, easing = PulseEase)
    return PULSE_FROM + (PULSE_PEAK - PULSE_FROM) * rise + (1f - PULSE_PEAK) * settle
}

private class Glyph(
    val outline: Path,
    val outlineEase: Easing,
    val mark: Path? = null,
    val markStartMs: Int = 0,
    val markEndMs: Int = 0,
    val markEase: Easing = SettleEase,
    val dotCenter: Offset? = null,
) {
    val outlineContours: List<Float> = contourLengths(outline)
    val markContours: List<Float> = mark?.let(::contourLengths).orEmpty()
}

private fun contourLengths(path: Path): List<Float> {
    val measure = android.graphics.PathMeasure(path.asAndroidPath(), false)
    val lengths = mutableListOf<Float>()
    do {
        lengths += measure.length
    } while (measure.nextContour())
    return lengths
}

@Suppress("LongMethod")
private fun TangemStatusIcon.Kind.glyph(): Glyph = when (this) {
    TangemStatusIcon.Kind.Checkmark -> Glyph(
        outline = circlePath(),
        outlineEase = CircleDrawOnEase,
        mark = Path().apply {
            moveTo(13f, 20.5f)
            lineTo(17.5f, 25f)
            lineTo(26.5f, 16f)
        },
        markStartMs = 450,
        markEndMs = 600,
        markEase = AccelerateEase,
    )
    TangemStatusIcon.Kind.Info -> Glyph(
        outline = circlePath(),
        outlineEase = CircleDrawOnEase,
        mark = Path().apply {
            moveTo(20f, 27f)
            lineTo(20f, 19.5f)
        },
        markStartMs = 350,
        markEndMs = 550,
        dotCenter = Offset(x = 20f, y = 13f),
    )
    TangemStatusIcon.Kind.Warning -> Glyph(
        outline = Path().apply {
            moveTo(23.464f, 8f)
            lineTo(34.722f, 27.5f)
            cubicTo(x1 = 36.262f, y1 = 30.167f, x2 = 34.338f, y2 = 33.5f, x3 = 31.258f, y3 = 33.5f)
            lineTo(8.742f, 33.5f)
            cubicTo(x1 = 5.662f, y1 = 33.5f, x2 = 3.738f, y2 = 30.167f, x3 = 5.278f, y3 = 27.5f)
            lineTo(16.536f, 8f)
            cubicTo(x1 = 18.076f, y1 = 5.333f, x2 = 21.925f, y2 = 5.333f, x3 = 23.464f, y3 = 8f)
        },
        outlineEase = DrawOnEase,
        mark = Path().apply {
            moveTo(20f, 15f)
            lineTo(20f, 21.5f)
        },
        markStartMs = 350,
        markEndMs = 550,
        dotCenter = Offset(x = 20f, y = 27f),
    )
    TangemStatusIcon.Kind.HeartBroken -> Glyph(
        outline = Path().apply {
            moveTo(19.5f, 8.506f)
            cubicTo(x1 = 18.655f, y1 = 7.777f, x2 = 16.716f, y2 = 6.5f, x3 = 13.5f, y3 = 6.5f)
            cubicTo(x1 = 8f, y1 = 6.5f, x2 = 4.5f, y2 = 12f, x3 = 4.5f, y3 = 16f)
            cubicTo(x1 = 4.5f, y1 = 27.5f, x2 = 20f, y2 = 34f, x3 = 20f, y3 = 34f)
            moveTo(20f, 9f)
            cubicTo(x1 = 20f, y1 = 9f, x2 = 22f, y2 = 6.5f, x3 = 26.5f, y3 = 6.5f)
            cubicTo(x1 = 32f, y1 = 6.5f, x2 = 35.5f, y2 = 12f, x3 = 35.5f, y3 = 16f)
            cubicTo(x1 = 35.5f, y1 = 27.5f, x2 = 20f, y2 = 34f, x3 = 20f, y3 = 34f)
            moveTo(20f, 9f)
            lineTo(22.5f, 14f)
            lineTo(18f, 19.5f)
            lineTo(22.5f, 24.5f)
            cubicTo(x1 = 22.5f, y1 = 24.5f, x2 = 20.263f, y2 = 33f, x3 = 20.263f, y3 = 33f)
        },
        outlineEase = DrawOnEase,
    )
}

private fun circlePath(): Path = Path().apply {
    addOval(oval = Rect(center = Offset(x = 20f, y = 20f), radius = 14.5f))
}

private fun DrawScope.drawGlyph(
    glyph: Glyph,
    entrance: EntranceProgress,
    paints: GlyphPaints,
    measure: android.graphics.PathMeasure,
    trimmed: android.graphics.Path,
) {
    val outlineFraction = entrance.segment(startMs = 0, endMs = ICON_DURATION_MS, easing = glyph.outlineEase)
    val markFraction = if (glyph.mark != null) {
        entrance.segment(startMs = glyph.markStartMs, endMs = glyph.markEndMs, easing = glyph.markEase)
    } else {
        0f
    }
    val dotScale = entrance.segment(startMs = DOT_START_MS, endMs = ICON_DURATION_MS, easing = SettleEase)

    fun drawPass(stroke: Paint, fill: Paint, offsetY: Float) {
        translate(top = offsetY) {
            drawIntoCanvas { canvas ->
                drawTrimmed(
                    canvas = canvas,
                    path = glyph.outline,
                    contours = glyph.outlineContours,
                    fraction = outlineFraction,
                    measure = measure,
                    trimmed = trimmed,
                    paint = stroke,
                )
                if (glyph.mark != null && markFraction > 0f) {
                    drawTrimmed(
                        canvas = canvas,
                        path = glyph.mark,
                        contours = glyph.markContours,
                        fraction = markFraction,
                        measure = measure,
                        trimmed = trimmed,
                        paint = stroke,
                    )
                }
                if (glyph.dotCenter != null && dotScale > 0f) {
                    canvas.drawCircle(center = glyph.dotCenter, radius = DOT_RADIUS * dotScale, paint = fill)
                }
            }
        }
    }

    if (isBlurSupported) {
        drawPass(stroke = paints.glowStroke, fill = paints.glowFill, offsetY = 0f)
    }
    drawPass(stroke = paints.mainStroke, fill = paints.mainFill, offsetY = 0f)
    if (isBlurSupported) {
        drawPass(stroke = paints.highlightStroke, fill = paints.highlightFill, offsetY = HIGHLIGHT_OFFSET_Y)
    }
}

@Suppress("LongParameterList")
private fun drawTrimmed(
    canvas: androidx.compose.ui.graphics.Canvas,
    path: Path,
    contours: List<Float>,
    fraction: Float,
    measure: android.graphics.PathMeasure,
    trimmed: android.graphics.Path,
    paint: Paint,
) {
    if (fraction >= 1f) {
        canvas.drawPath(path = path, paint = paint)
        return
    }
    var budget = contours.sum() * fraction
    measure.setPath(path.asAndroidPath(), false)
    for (length in contours) {
        if (budget <= 0f) break
        trimmed.reset()
        measure.getSegment(0f, minOf(budget, length), trimmed, true)
        canvas.nativeCanvas.drawPath(trimmed, paint.asFrameworkPaint())
        budget -= length
        measure.nextContour()
    }
}

private class GlyphPaints(tint: Color) {
    private val glowColor = tint.copy(alpha = tint.alpha * GLOW_ALPHA)

    val glowStroke = strokePaint(color = glowColor, blurRadius = GLOW_BLUR)
    val glowFill = fillPaint(color = glowColor, blurRadius = GLOW_BLUR)
    val mainStroke = strokePaint(color = tint, blurRadius = 0f)
    val mainFill = fillPaint(color = tint, blurRadius = 0f)
    val highlightStroke = strokePaint(color = HighlightColor, blurRadius = HIGHLIGHT_BLUR)
    val highlightFill = fillPaint(color = HighlightColor, blurRadius = HIGHLIGHT_BLUR)
}

private fun strokePaint(color: Color, blurRadius: Float): Paint = Paint().apply {
    this.color = color
    style = PaintingStyle.Stroke
    strokeWidth = STROKE_WIDTH
    strokeCap = StrokeCap.Round
    strokeJoin = StrokeJoin.Round
    if (blurRadius > 0f) {
        asFrameworkPaint().maskFilter = BlurMaskFilter(blurRadius, BlurMaskFilter.Blur.NORMAL)
    }
}

private fun fillPaint(color: Color, blurRadius: Float): Paint = Paint().apply {
    this.color = color
    style = PaintingStyle.Fill
    if (blurRadius > 0f) {
        asFrameworkPaint().maskFilter = BlurMaskFilter(blurRadius, BlurMaskFilter.Blur.NORMAL)
    }
}

private const val GRID = 40f
private const val STROKE_WIDTH = 3f
private const val DOT_RADIUS = 2f
private const val ICON_DURATION_MS = 600
private const val PULSE_PEAK_MS = 400
private const val DOT_START_MS = 500
private const val PULSE_FROM = 0.6f
private const val PULSE_PEAK = 1.2f
private const val GLOW_BLUR = 16f
private const val GLOW_ALPHA = 0.6f
private const val HIGHLIGHT_BLUR = 2f
private const val HIGHLIGHT_OFFSET_Y = 2f

private val CircleDrawOnEase = CubicBezierEasing(a = 0.8f, b = 0f, c = 0.8f, d = 1f)
private val HighlightColor = TangemColorPalette.Opaque.BaseWhite.`25`
private val isBlurSupported = Build.VERSION.SDK_INT >= Build.VERSION_CODES.P

@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun TangemStatusIconPreview() {
    TangemThemePreviewRedesign {
        Row(
            modifier = Modifier
                .background(TangemTheme.colors3.bg.primary)
                .padding(all = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(space = 16.dp),
        ) {
            TangemStatusIcon.Kind.entries.forEach { kind ->
                TangemStatusIcon(
                    kind = kind,
                    modifier = Modifier.size(40.dp),
                    animated = false,
                )
            }
        }
    }
}