package com.tangem.features.foryou.impl.components

import android.content.res.Configuration
import android.graphics.BlurMaskFilter
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.toSize
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.features.foryou.impl.components.state.DonutSegment
import kotlin.math.min

/**
 * Ring (donut) chart drawn behind a center [content] slot.
 *
 * The ring is painted via [Modifier.drawBehind] so the center label stays a normal composable
 * ([content]) laid out on top — no manual text measuring inside the canvas.
 *
 * Paint order (bottom → top), all inside the canvas:
 * 1. The full-circle [trackColor] track (always drawn — it is the empty-state look when [segments] is empty),
 *    with its own inner shadow.
 * 2. Each slice (reverse list order, so slice 0 ends up on top — its round cap tucks over the next one):
 *    the solid stroke, then **its own** white inner shadow (Figma: X0 Y4 Blur8, white 24%) drawn right on
 *    top of it. Per-slice (not one shadow over the whole ring) is what gives each pill its glossy, raised
 *    look and the highlight at the colour seams. No colored glow/halo is used.
 *
 * Empty state: pass an empty [segments] list — only the track + its inner shadow render, and [content] can
 * show the "No data" label.
 *
 * Selection: when [selectedIndex] is non-null, everything except the chosen slice is dimmed with a
 * theme-adaptive overlay ([TangemTheme.colors3.border.inverse.tertiary]) — both the other slices and the
 * track (unfilled remainder) — so only the selected slice stays at full strength. Only one slice can be
 * selected at a time — selection is hoisted (the index is the slice's identity, as there are no segment ids
 * yet). Taps are hit-tested against the ring band only and reported via [onSegmentClick]; the chart is
 * interactive only when [onSegmentClick] is set **and** [segments] is non-empty.
 *
 * @param segments Slices, in priority order (index 0 is painted on top). See [DonutSegment.weight].
 * @param modifier Modifier; should carry the overall size (e.g. `Modifier.size(240.dp)`).
 * @param selectedIndex Index of the currently selected slice, or `null` for no selection (nothing dimmed).
 * @param onSegmentClick Invoked on every tap inside the chart: with the tapped slice index, or with `null`
 *   when the tap missed all slices (the center hole or the unfilled track). Passing `null` for the whole
 *   callback makes the chart non-interactive. Toggling/switching/clearing the selection is the caller's
 *   responsibility — e.g. map a repeat tap or a miss to deselection, and a tap on another slice to a switch.
 * @param strokeWidth Thickness of the ring.
 * @param trackColor Fill of the unfilled remainder of the circle (and the empty-state ring).
 * @param startAngle Angle (degrees) where the first slice starts. `-90f` = 12 o'clock.
 * @param content Centered content (e.g. total value + caption, or the "No data" label).
 */
@Suppress("MagicNumber", "LongParameterList", "LongMethod", "NamedArguments")
@Composable
internal fun DonutChart(
    segments: List<DonutSegment>,
    modifier: Modifier = Modifier,
    selectedIndex: Int? = null,
    onSegmentClick: ((index: Int?) -> Unit)? = null,
    strokeWidth: Dp = 28.dp,
    trackColor: Color = TangemTheme.colors3.border.tertiary,
    startAngle: Float = -90f,
    content: @Composable ColumnScope.() -> Unit,
) {
    val strokePx = with(LocalDensity.current) { strokeWidth.toPx() }
    val dimOverlayColor = TangemTheme.colors3.border.inverse.tertiary

    // Fade the dim in/out in step with the segment tooltip's pop-in (same spring as DonutSegmentTooltip).
    val dimProgress by animateFloatAsState(
        targetValue = if (selectedIndex != null) 1f else 0f,
        animationSpec = spring(dampingRatio = DIM_SPRING_DAMPING, stiffness = DIM_SPRING_STIFFNESS),
        label = "donutDim",
    )

    var highlightedIndex by remember { mutableStateOf<Int?>(null) }
    if (selectedIndex != null) highlightedIndex = selectedIndex

    val latestSelectedIndex by rememberUpdatedState(selectedIndex)
    val latestOnSegmentClick by rememberUpdatedState(onSegmentClick)

    val clickModifier = if (onSegmentClick != null && segments.isNotEmpty()) {
        Modifier.pointerInput(segments, startAngle, strokePx) {
            detectTapGestures { tap ->
                val clickedIndex = segmentIndexAt(tap, size.toSize(), strokePx, segments, startAngle)
                if (latestSelectedIndex != clickedIndex) latestOnSegmentClick?.invoke(clickedIndex)
            }
        }
    } else {
        Modifier
    }

    Box(
        modifier = modifier
            .background(TangemTheme.colors3.bg.secondary)
            .then(clickModifier)
            .drawBehind {
                val arc = arcRect(strokePx)
                // Inner shadow params from Figma: X0 Y4 Blur8 Spread0, white 24%.
                val innerDx = 0f
                val innerDy = 4.dp.toPx()
                val innerBlur = 8.dp.toPx()

                // 1. Track — full circle behind everything, plus its inner shadow.
                drawArc(
                    color = trackColor,
                    startAngle = 0f,
                    sweepAngle = 360f,
                    useCenter = false,
                    topLeft = arc.topLeft,
                    size = arc.size,
                    style = Stroke(width = strokePx, cap = StrokeCap.Round),
                )
                drawInnerShadowArc(arc, 0f, 360f, strokePx, InnerShadowColor, innerBlur, innerDx, innerDy)

                // Dim intensity animates 0f..1f; scale the overlay's own alpha by it so the dim fades.
                val dim = dimProgress.coerceIn(0f, 1f)
                val dimColor = dimOverlayColor.copy(alpha = dimOverlayColor.alpha * dim)

                // Once a selection exists, dim the whole track too, so the unfilled remainder fades
                // along with the non-selected slices instead of staying bright. Drawn before the
                // slices, so each slice (selected included) paints on top at full strength.
                if (dim > 0f) {
                    drawArc(
                        color = dimColor,
                        startAngle = 0f,
                        sweepAngle = 360f,
                        useCenter = false,
                        topLeft = arc.topLeft,
                        size = arc.size,
                        style = Stroke(width = strokePx, cap = StrokeCap.Round),
                    )
                }

                // Precompute each slice's [start, sweep] once.
                val sweeps = segments.map { it.weight.coerceIn(0f, 1f) * 360f }
                val starts = sweeps.runningFold(startAngle) { acc, sweep -> acc + sweep }

                // 2. Slices — reversed so slice 0 sits on top of its neighbor. Each slice gets its own
                //    inner shadow right after its fill, so the glossy highlight follows every pill (and
                //    every colour seam), not just the ring's outer/inner contour.
                for (i in segments.indices.reversed()) {
                    if (sweeps[i] <= 0f) continue
                    drawArc(
                        color = segments[i].color,
                        startAngle = starts[i],
                        sweepAngle = sweeps[i],
                        useCenter = false,
                        topLeft = arc.topLeft,
                        size = arc.size,
                        style = Stroke(width = strokePx, cap = StrokeCap.Round),
                    )
                    drawInnerShadowArc(
                        arc = arc,
                        startAngle = starts[i],
                        sweepAngle = sweeps[i],
                        strokePx = strokePx,
                        shadowColor = InnerShadowColor,
                        blurPx = innerBlur,
                        dx = innerDx,
                        dy = innerDy,
                    )

                    // Dim every non-highlighted slice while a selection is active — the chosen one stays
                    // bright. Uses the animated [dim] so it fades, and [highlightedIndex] (not selectedIndex)
                    // so the right slice stays bright through the fade-out after deselection.
                    if (dim > 0f && i != highlightedIndex) {
                        drawArc(
                            color = dimColor,
                            startAngle = starts[i],
                            sweepAngle = sweeps[i],
                            useCenter = false,
                            topLeft = arc.topLeft,
                            size = arc.size,
                            style = Stroke(width = strokePx, cap = StrokeCap.Round),
                        )
                    }
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 36.dp),
        ) {
            content()
        }
    }
}

/**
 * Maps a tap [tap] to the index of the slice under it, or `null` if the tap is outside the ring band or
 * lands on a gap/track. Uses the same geometry as the drawing pass: the ring is centered, its outer radius
 * is half the min side and its inner radius is `outer - strokePx`. The angular test reuses the
 * `runningFold` start/sweep layout. A small radial tolerance makes the thin ring comfortable to hit.
 */
@Suppress("MagicNumber", "ReturnCount")
private fun segmentIndexAt(
    tap: Offset,
    size: Size,
    strokePx: Float,
    segments: List<DonutSegment>,
    startAngle: Float,
): Int? {
    val cx = size.width / 2f
    val cy = size.height / 2f
    val dx = tap.x - cx
    val dy = tap.y - cy

    val outer = min(size.width, size.height) / 2f
    val inner = outer - strokePx
    val tolerance = strokePx * 0.4f
    val dist = kotlin.math.hypot(dx, dy)
    if (dist < inner - tolerance || dist > outer + tolerance) return null

    // Degrees clockwise from 3 o'clock — same convention as Canvas.drawArc.
    val angle = Math.toDegrees(kotlin.math.atan2(dy, dx).toDouble()).toFloat().mod(360f)

    val sweeps = segments.map { it.weight.coerceIn(0f, 1f) * 360f }
    val starts = sweeps.runningFold(startAngle) { acc, sweep -> acc + sweep }
    for (i in segments.indices) {
        if (sweeps[i] <= 0f) continue
        val relative = (angle - starts[i].mod(360f)).mod(360f)
        if (relative <= sweeps[i]) return i
    }
    return null
}

/** Square arc bounds, centered in this [DrawScope], inset by half the stroke so the ring fits inside. */
private fun DrawScope.arcRect(strokePx: Float): ArcRect {
    val diameter = min(size.width, size.height)
    val side = diameter - strokePx
    val left = (size.width - diameter) / 2f + strokePx / 2f
    val top = (size.height - diameter) / 2f + strokePx / 2f
    return ArcRect(topLeft = Offset(left, top), size = Size(side, side))
}

private data class ArcRect(val topLeft: Offset, val size: Size)

/**
 * Draws an inset (inner) shadow confined to a single stroked arc — the canvas equivalent of CSS
 * `box-shadow: … inset`. Works on every API level (uses [BlurMaskFilter], not RenderEffect).
 *
 * Technique: in an isolated layer, paint the arc silhouette in [shadowColor], then "punch out" the same
 * arc offset by ([dx], [dy]) and blurred via [PorterDuff.Mode.DST_OUT]. What survives is a blurred band
 * of [shadowColor] hugging the edge opposite the offset — i.e. the inner shadow.
 */
@Suppress("LongParameterList")
private fun DrawScope.drawInnerShadowArc(
    arc: ArcRect,
    startAngle: Float,
    sweepAngle: Float,
    strokePx: Float,
    shadowColor: Color,
    blurPx: Float,
    dx: Float,
    dy: Float,
) {
    if (sweepAngle <= 0f || blurPx <= 0f) return
    drawIntoCanvas { canvas ->
        val native = canvas.nativeCanvas
        val l = arc.topLeft.x
        val t = arc.topLeft.y
        val r = l + arc.size.width
        val b = t + arc.size.height

        val layer = native.saveLayer(null, null)

        // 1. The slice silhouette in the shadow color.
        val basePaint = android.graphics.Paint().apply {
            isAntiAlias = true
            style = android.graphics.Paint.Style.STROKE
            strokeWidth = strokePx
            strokeCap = android.graphics.Paint.Cap.ROUND
            this.color = shadowColor.toArgb()
        }
        native.drawArc(l, t, r, b, startAngle, sweepAngle, false, basePaint)

        // 2. Punch out an offset, blurred copy — leaves the shadow only along the inner edge.
        val cutPaint = android.graphics.Paint().apply {
            isAntiAlias = true
            style = android.graphics.Paint.Style.STROKE
            strokeWidth = strokePx
            strokeCap = android.graphics.Paint.Cap.ROUND
            this.color = android.graphics.Color.BLACK
            xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_OUT)
            maskFilter = BlurMaskFilter(blurPx, BlurMaskFilter.Blur.NORMAL)
        }
        native.drawArc(l + dx, t + dy, r + dx, b + dy, startAngle, sweepAngle, false, cutPaint)

        native.restoreToCount(layer)
    }
}

/** Inner shadow — Figma #FFFFFF at 24% opacity (theme-independent). */
private val InnerShadowColor = Color.White.copy(alpha = 0.24f)

// Selection-dim spring, mirroring DonutSegmentTooltip's pop-in so the dim and the tooltip move together.
private const val DIM_SPRING_DAMPING = 0.82f
private const val DIM_SPRING_STIFFNESS = 1100f

// region Previews

@Suppress("MagicNumber")
@Preview(name = "DonutChart • Dark", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(name = "DonutChart • Light", showBackground = true)
@Composable
private fun PreviewDonutChart() {
    TangemThemePreviewRedesign {
        // Tap a slice to select it (others dim); tap it again to clear. Caller owns the selection.
        var selectedIndex by remember { mutableStateOf<Int?>(null) }
        Box(
            modifier = Modifier
                .background(TangemTheme.colors3.bg.primary)
                .padding(32.dp),
            contentAlignment = Alignment.Center,
        ) {
            DonutChart(
                modifier = Modifier.size(260.dp),
                selectedIndex = selectedIndex,
                onSegmentClick = { index -> selectedIndex = index.takeIf { it != selectedIndex } },
                segments = listOf(
                    DonutSegment(weight = 0.55f, color = TangemTheme.colors3.border.brand),
                    DonutSegment(weight = 0.07f, color = TangemTheme.colors3.border.accent.violet),
                    DonutSegment(weight = 0.06f, color = TangemTheme.colors3.border.accent.red),
                    DonutSegment(weight = 0.05f, color = TangemTheme.colors3.border.accent.green),
                ),
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "$10,000.1333",
                        color = TangemTheme.colors3.text.primary,
                        style = TangemTheme.typography3.heading.medium,
                    )
                    Text(
                        text = "Total value",
                        color = TangemTheme.colors3.text.secondary,
                        style = TangemTheme.typography3.body.medium,
                    )
                }
            }
        }
    }
}

@Preview(name = "DonutChart Empty • Dark", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(name = "DonutChart Empty • Light", showBackground = true)
@Composable
private fun PreviewDonutChartEmpty() {
    TangemThemePreviewRedesign {
        Box(
            modifier = Modifier
                .background(TangemTheme.colors3.bg.primary)
                .padding(32.dp),
            contentAlignment = Alignment.Center,
        ) {
            DonutChart(
                modifier = Modifier.size(260.dp),
                segments = emptyList(),
            ) {
                Text(
                    text = "No data",
                    color = TangemTheme.colors3.text.primary,
                    style = TangemTheme.typography3.heading.medium,
                )
            }
        }
    }
}

// endregion