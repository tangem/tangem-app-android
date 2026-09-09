package com.tangem.features.foryou.impl.components

import android.content.res.Configuration
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.features.foryou.impl.R
import com.tangem.features.foryou.impl.components.state.DonutSegmentColor
import com.tangem.features.foryou.impl.components.state.DonutSegmentUM
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import java.math.BigDecimal
import kotlin.math.*

/**
 * Ring (donut) chart drawn behind a center [content] slot.

 * @param segments Slices, in priority order (index 0 is painted on top). See [DonutSegmentUM.weight].
 * @param modifier Modifier; should carry the overall size (e.g. `Modifier.size(240.dp)`).
 * @param selectedIndex Index of the currently selected slice, or `null` for no selection (nothing dimmed).
 * @param onSegmentClick Invoked when a tap lands on a slice **other than** the selected one, or misses all
 *   slices (the center hole or the unfilled track) while something is selected — i.e. only when the tap can
 *   change the selection. A repeat tap on the selected slice is not reported here; use [onTap] to observe
 *   every tap. Toggling/switching/clearing the selection is the caller's responsibility — e.g. map a miss to
 *   deselection, and a tap on another slice to a switch.
 * @param onTap Invoked on every tap inside the chart, without the de-duplication [onSegmentClick] applies —
 *   repeat taps on the selected slice and taps that miss the ring included. Fires on press, in step with
 *   [onSegmentClick].
 * @param strokeWidth Thickness of the ring.
 * @param trackColor Fill of the unfilled remainder of the circle (and the empty-state ring). Translucent,
 *   so it is not painted at all when the slices close the ring.
 * @param startAngle Angle (degrees) where the first slice starts. `-90f` = 12 o'clock.
 * @param content Centered content (e.g. total value + caption, or the "No data" label).
 */
@Suppress("MagicNumber", "LongParameterList")
@Composable
internal fun DonutChart(
    segments: ImmutableList<DonutSegmentUM>,
    modifier: Modifier = Modifier,
    selectedIndex: Int? = null,
    onSegmentClick: ((index: Int?) -> Unit)? = null,
    onTap: (() -> Unit)? = null,
    strokeWidth: Dp = 28.dp,
    trackColor: Color = TangemTheme.colors3.border.tertiary,
    startAngle: Float = -90f,
    content: @Composable ColumnScope.() -> Unit,
) {
    val strokePx = with(LocalDensity.current) { strokeWidth.toPx() }
    val dimOverlayColor = TangemTheme.colors3.border.inverse.tertiary

    // Resolve each slice's themed colour once, here in composition (the palette reads TangemTheme, which
    // isn't available inside the drawBehind DrawScope). Order is preserved 1:1 with [segments] so slice i
    // keeps the colour its producer assigned by rank — the draw pass below indexes by i, not by paint order.
    val segmentColors = segments.map { it.color.getColor() }

    // Fade the dim in/out in step with the segment tooltip's pop-in (same spring as DonutSegmentTooltip).
    val dimProgress by animateFloatAsState(
        targetValue = if (selectedIndex != null) 1f else 0f,
        animationSpec = spring(dampingRatio = DIM_SPRING_DAMPING, stiffness = DIM_SPRING_STIFFNESS),
        label = "donutDim",
    )

    val capPath = remember { Path() }

    var highlightedIndex by remember { mutableStateOf<Int?>(null) }
    if (selectedIndex != null) highlightedIndex = selectedIndex

    val latestSelectedIndex by rememberUpdatedState(selectedIndex)
    val latestOnSegmentClick by rememberUpdatedState(onSegmentClick)
    val latestOnTap by rememberUpdatedState(onTap)

    val clickModifier = if ((onSegmentClick != null || onTap != null) && segments.isNotEmpty()) {
        Modifier.pointerInput(segments, startAngle, strokePx) {
            detectTapGestures(
                onPress = { tap ->
                    latestOnTap?.invoke()
                    val clickedIndex = segmentIndexAt(
                        tap = tap,
                        size = size.toSize(),
                        strokePx = strokePx,
                        segments = segments,
                        startAngle = startAngle,
                    )
                    if (latestSelectedIndex != clickedIndex) latestOnSegmentClick?.invoke(clickedIndex)
                },
            )
        }
    } else {
        Modifier
    }

    Box(
        modifier = modifier
            .then(clickModifier)
            .drawBehind {
                drawDonut(
                    segments = segments,
                    segmentColors = segmentColors,
                    capPath = capPath,
                    highlightedIndex = highlightedIndex,
                    dimProgress = dimProgress,
                    dimOverlayColor = dimOverlayColor,
                    strokePx = strokePx,
                    trackColor = trackColor,
                    startAngle = startAngle,
                )
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
 * Paints the ring: the track where the slices don't reach, then every slice and, while a selection is
 * active, its dim. Split out of [DonutChart] so the composable stays about state and gestures — the paint
 * order is documented there.
 *
 * @param highlightedIndex the slice kept at full strength; it survives the fade-out after a deselection,
 *   unlike `selectedIndex`.
 * @param dimProgress `0f..1f` dim intensity, scaling [dimOverlayColor]'s own alpha.
 */
@Suppress("MagicNumber", "LongParameterList", "LongMethod")
private fun DrawScope.drawDonut(
    segments: List<DonutSegmentUM>,
    segmentColors: List<Color>,
    capPath: Path,
    highlightedIndex: Int?,
    dimProgress: Float,
    dimOverlayColor: Color,
    strokePx: Float,
    trackColor: Color,
    startAngle: Float,
) {
    val arc = arcRect(strokePx)

    // Precompute each slice's [start, sweep] once. Sweeps are the *visual* angles: every
    // non-zero slice is floored to a minimum share (see [visualSweepAngles]) so tiny holdings
    // stay visible
    val capDeg = capPaddingDeg(strokePx, arc.size.width)
    val sweeps = visualSweepAngles(weights = segments.map { it.weight.toFloat() }, capDeg = capDeg)
    val starts = sweeps.runningFold(startAngle) { acc, sweep -> acc + sweep }
    val paintOrder = slicePaintOrder(sweeps)

    // 1. Track — the circle behind everything. Skipped once the slices close the ring
    val isClosed = isRingClosed(sweeps)
    if (!isClosed) {
        drawArc(
            color = trackColor,
            startAngle = 0f,
            sweepAngle = 360f,
            useCenter = false,
            topLeft = arc.topLeft,
            size = arc.size,
            style = Stroke(width = strokePx, cap = StrokeCap.Round),
        )
    }

    // Dim intensity animates 0f..1f; scale the overlay's own alpha by it so the dim fades.
    val dim = dimProgress.coerceIn(0f, 1f)
    val dimColor = dimOverlayColor.copy(alpha = dimOverlayColor.alpha * dim)

    // Once a selection exists, dim the whole track too, so the unfilled remainder fades
    // along with the non-selected slices instead of staying bright. Drawn before the
    // slices, so each slice (selected included) paints on top at full strength.
    if (dim > 0f && !isClosed) {
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

    // 2. Slice bodies — butt-capped, so each covers exactly its own sweep and overlaps nothing.
    val bodyStroke = Stroke(width = strokePx, cap = StrokeCap.Butt)
    for (i in paintOrder) {
        drawArc(
            color = segmentColors[i],
            startAngle = starts[i],
            sweepAngle = sweeps[i],
            useCenter = false,
            topLeft = arc.topLeft,
            size = arc.size,
            style = bodyStroke,
        )
        if (dim > 0f && i != highlightedIndex) {
            drawArc(
                color = dimColor,
                startAngle = starts[i],
                sweepAngle = sweeps[i],
                useCenter = false,
                topLeft = arc.topLeft,
                size = arc.size,
                style = bodyStroke,
            )
        }
    }

    // 3. End caps — after every body, so the last slice's cap lands on slice 0 and the wrap needs no special case.
    val capRadius = strokePx / 2f
    for (i in paintOrder) {
        val capAngle = starts[i] + sweeps[i]
        drawCapHalf(
            arc = arc,
            path = capPath,
            angleDeg = capAngle,
            radiusPx = capRadius,
            color = segmentColors[i],
            forward = true,
        )
        if (dim > 0f && i != highlightedIndex) {
            drawCapHalf(
                arc = arc,
                path = capPath,
                angleDeg = capAngle,
                radiusPx = capRadius,
                color = dimColor,
                forward = true,
            )
        }
    }

    // 4. The ring's free start, when the slices leave a remainder: a backward cap so the tail is rounded
    //    against the track rather than cut off square.
    freeStartSliceIndex(sweeps)?.let { index ->
        drawCapHalf(
            arc = arc,
            path = capPath,
            angleDeg = starts[index],
            radiusPx = capRadius,
            color = segmentColors[index],
            forward = false,
        )
        if (dim > 0f && index != highlightedIndex) {
            drawCapHalf(
                arc = arc,
                path = capPath,
                angleDeg = starts[index],
                radiusPx = capRadius,
                color = dimColor,
                forward = false,
            )
        }
    }
}

/**
 * One round cap: a half-disc of radius [radiusPx] centred on the ring centreline at [angleDeg], its flat
 * side on the radial line there and its bulge running along the arc — [forward] over the slice's successor
 * for an end cap, backward for the start cap of a free end.
 *
 * The centreline point sits at [angleDeg] from the ring's centre, so the half-disc's flat diameter lies at
 * that same angle and sweeping `180°` from it passes through `angleDeg + 90°` — the arc's forward tangent.
 */
@Suppress("LongParameterList")
private fun DrawScope.drawCapHalf(
    arc: ArcRect,
    path: Path,
    angleDeg: Float,
    radiusPx: Float,
    color: Color,
    forward: Boolean,
) {
    path.reset()
    path.addArc(
        oval = Rect(center = arc.centerlinePointAt(angleDeg), radius = radiusPx),
        startAngleDegrees = if (forward) angleDeg else angleDeg + HALF_TURN_DEG,
        sweepAngleDegrees = HALF_TURN_DEG,
    )
    path.close()
    drawPath(path = path, color = color)
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
    segments: List<DonutSegmentUM>,
    startAngle: Float,
): Int? {
    val cx = size.width / 2f
    val cy = size.height / 2f
    val dx = tap.x - cx
    val dy = tap.y - cy

    val outer = min(size.width, size.height) / 2f
    val inner = outer - strokePx
    val tolerance = strokePx * 0.4f
    val dist = hypot(dx, dy)
    if (dist < inner - tolerance || dist > outer + tolerance) return null

    // Degrees clockwise from 3 o'clock — same convention as Canvas.drawArc.
    val angle = Math.toDegrees(atan2(dy, dx).toDouble()).toFloat().mod(360f)

    // Same cap compensation as the draw pass — centerline diameter is `min(size) - strokePx` (see
    // [arcRect]) — so hit-testing matches the drawn geometry exactly.
    val arcDiameter = min(size.width, size.height) - strokePx
    val capDeg = capPaddingDeg(strokePx, arcDiameter)
    val sweeps = visualSweepAngles(weights = segments.map { it.weight.toFloat() }, capDeg = capDeg)
    val starts = sweeps.runningFold(startAngle) { acc, sweep -> acc + sweep }

    // Angles only — a round cap is not modelled here, so at every seam the ~half-cap the neighbour's cap
    // paints over belongs to the slice whose sweep covers it. That mismatch is uniform across all seams,
    // the wrap included.
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

/** Point on the ring centreline at [angleDeg] — where a slice's round cap is centred. */
private fun ArcRect.centerlinePointAt(angleDeg: Float): Offset {
    val radius = size.width / 2f
    val rad = Math.toRadians(angleDeg.toDouble())
    return Offset(
        x = topLeft.x + radius + radius * cos(rad).toFloat(),
        y = topLeft.y + radius + radius * sin(rad).toFloat(),
    )
}

private data class ArcRect(val topLeft: Offset, val size: Size)

/** Sweep of a round cap's half-disc, and the rotation from a forward-facing cap to a backward-facing one. */
private const val HALF_TURN_DEG = 180f

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
                segments = persistentListOf(
                    DonutSegmentUM(
                        weight = BigDecimal(0.55),
                        color = DonutSegmentColor.Blue,
                        title = stringReference("Ethereum"),
                        fiatValue = stringReference("$5,720.22"),
                    ),
                    DonutSegmentUM(
                        weight = BigDecimal(0.07),
                        color = DonutSegmentColor.Violet,
                        title = stringReference("Solana"),
                        fiatValue = stringReference("$728.30"),
                    ),
                    DonutSegmentUM(
                        weight = BigDecimal(0.06),
                        color = DonutSegmentColor.Red,
                        title = stringReference("Polkadot"),
                        fiatValue = stringReference("$624.26"),
                    ),
                    DonutSegmentUM(
                        weight = BigDecimal(0.05),
                        color = DonutSegmentColor.Green,
                        title = stringReference("Tether"),
                        fiatValue = stringReference("$520.18"),
                    ),
                    // Closes the ring at the exact complement, so the remainder is a selectable slice
                    // rather than bare track — see [DonutChart]'s "Closing the ring".
                    DonutSegmentUM(
                        weight = BigDecimal(0.27),
                        color = DonutSegmentColor.Grey,
                        title = stringReference("Other"),
                        fiatValue = stringReference("$2,407.17"),
                    ),
                ),
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "$10,000.1333",
                        color = TangemTheme.colors3.text.primary,
                        style = TangemTheme.typography3.heading.medium,
                    )
                    Text(
                        text = stringResourceSafe(R.string.market_chart_bubble_total_value),
                        color = TangemTheme.colors3.text.secondary,
                        style = TangemTheme.typography3.body.medium,
                    )
                }
            }
        }
    }
}

@Suppress("MagicNumber")
@Preview(name = "DonutChart No Other • Dark", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(name = "DonutChart No Other • Light", showBackground = true)
@Composable
private fun PreviewDonutChartWithoutOther() {
    TangemThemePreviewRedesign {
        // What the app renders when nothing was collapsed into "Other": the ring is closed by the last
        // *coloured* slice, which is lifted on top so its cap laps over slice 0 at 12 o'clock. That slice
        // is also below the minimum share here, so it doubles as the check that a lifted slice gets the
        // plain floor — it should read as a small round pill, not a wedge.
        Box(
            modifier = Modifier
                .background(TangemTheme.colors3.bg.primary)
                .padding(32.dp),
            contentAlignment = Alignment.Center,
        ) {
            DonutChart(
                modifier = Modifier.size(260.dp),
                segments = persistentListOf(
                    DonutSegmentUM(
                        weight = BigDecimal("0.55"),
                        color = DonutSegmentColor.Blue,
                        title = stringReference("Ethereum"),
                        fiatValue = stringReference("$5,500.00"),
                    ),
                    DonutSegmentUM(
                        weight = BigDecimal("0.20"),
                        color = DonutSegmentColor.Violet,
                        title = stringReference("Solana"),
                        fiatValue = stringReference("$2,000.00"),
                    ),
                    DonutSegmentUM(
                        weight = BigDecimal("0.245"),
                        color = DonutSegmentColor.Red,
                        title = stringReference("Polkadot"),
                        fiatValue = stringReference("$2,450.00"),
                    ),
                    DonutSegmentUM(
                        weight = BigDecimal("0.005"),
                        color = DonutSegmentColor.Green,
                        title = stringReference("Tether"),
                        fiatValue = stringReference("$50.00"),
                    ),
                ),
            ) {
                Text(
                    text = "$10,000.00",
                    color = TangemTheme.colors3.text.primary,
                    style = TangemTheme.typography3.heading.medium,
                )
            }
        }
    }
}

@Suppress("MagicNumber")
@Preview(name = "DonutChart Sub-cap • Dark", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(name = "DonutChart Sub-cap • Light", showBackground = true)
@Composable
private fun PreviewDonutChartSubCapSlices() {
    TangemThemePreviewRedesign {
        // Two holdings below the minimum share, at the production size and stroke — one mid-ring, one the
        // trailing grey "Other". Both must render at that same share and so look identical in width: no
        // slice is widened to make room for a cap, because a cap only ever extends forward over the slice
        // after it.
        Box(
            modifier = Modifier
                .background(TangemTheme.colors3.bg.secondary)
                .padding(32.dp),
            contentAlignment = Alignment.Center,
        ) {
            DonutChart(
                modifier = Modifier.size(200.dp),
                strokeWidth = 28.dp,
                segments = persistentListOf(
                    DonutSegmentUM(
                        weight = BigDecimal("0.58"),
                        color = DonutSegmentColor.Blue,
                        title = stringReference("Ethereum"),
                        fiatValue = stringReference("$5,800.00"),
                    ),
                    DonutSegmentUM(
                        weight = BigDecimal("0.01"),
                        color = DonutSegmentColor.Green,
                        title = stringReference("Tether"),
                        fiatValue = stringReference("$100.00"),
                    ),
                    DonutSegmentUM(
                        weight = BigDecimal("0.40"),
                        color = DonutSegmentColor.Violet,
                        title = stringReference("Solana"),
                        fiatValue = stringReference("$4,000.00"),
                    ),
                    DonutSegmentUM(
                        weight = BigDecimal("0.01"),
                        color = DonutSegmentColor.Grey,
                        title = stringReference("Other"),
                        fiatValue = stringReference("$100.00"),
                    ),
                ),
            ) {
                Text(
                    text = "$10,000.00",
                    color = TangemTheme.colors3.text.primary,
                    style = TangemTheme.typography3.body.medium,
                )
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
                segments = persistentListOf(),
            ) {
                Text(
                    text = stringResourceSafe(R.string.market_chart_bubble_no_data),
                    color = TangemTheme.colors3.text.primary,
                    style = TangemTheme.typography3.heading.medium,
                )
            }
        }
    }
}

// endregion