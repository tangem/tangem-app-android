@file:Suppress("UnnecessaryParentheses")

package com.tangem.core.ui.components.progressbar

import android.content.res.Configuration
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.layout.layout
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.offset
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import kotlin.math.abs

/**
 * Progress indicators express an unspecified wait time or display the length of a process.
 *
 * By default there is no animation between [progress] values. You can use
 *
 * @param progress The progress of this progress indicator, where 0.0 represents no progress and 1.0
 * represents full progress. Values outside of this range are coerced into the range.
 * @param modifier the [Modifier] to be applied to this progress indicator
 * @param color The color of the progress indicator.
 * @param backgroundColor The color of the background behind the indicator, visible when the
 * progress has not reached that area of the overall indicator yet.
 * @param strokeCap stroke cap to use for the ends of this progress indicator
 */
@Composable
fun TangemLinearProgressIndicator(
    progress: () -> Float,
    modifier: Modifier = Modifier,
    color: Color = TangemTheme.colors.text.accent,
    backgroundColor: Color = TangemTheme.colors.background.tertiary,
    strokeCap: StrokeCap = StrokeCap.Round,
) {
    val coercedProgress = { progress().coerceIn(0f, 1f) }
    Canvas(
        modifier
            .increaseSemanticsBounds()
            .semantics(mergeDescendants = true) {
                progressBarRangeInfo = ProgressBarRangeInfo(coercedProgress(), 0f..1f)
            }
            .size(height = 4.dp, width = 24.dp),
    ) {
        val strokeWidth = size.height
        drawLinearIndicatorBackground(backgroundColor, strokeWidth, strokeCap)
        drawLinearIndicator(0f, coercedProgress(), color, strokeWidth, strokeCap)
    }
}

/**
 * Progress indicators loading status with infinite animation
 *
 * @param modifier the [Modifier] to be applied to this progress indicator
 * @param color The color of the progress indicator.
 * @param backgroundColor The color of the background behind the indicator, visible when the
 * progress has not reached that area of the overall indicator yet.
 * @param strokeCap stroke cap to use for the ends of this progress indicator
 */
@Suppress("LongMethod")
@Composable
fun TangemLinearProgressIndicator(
    modifier: Modifier = Modifier,
    color: Color = TangemTheme.colors.text.accent,
    backgroundColor: Color = TangemTheme.colors.background.tertiary,
    strokeCap: StrokeCap = StrokeCap.Round,
) {
    val infiniteTransition = rememberInfiniteTransition()
    val firstLineHead by infiniteTransition.animateFloat(
        0f,
        1f,
        infiniteRepeatable(
            animation = keyframes {
                durationMillis = LINEAR_ANIMATION_DURATION
                0f at FIRST_LINE_HEAD_DELAY using FIRST_LINE_HEAD_EASING
                1f at FIRST_LINE_HEAD_DURATION + FIRST_LINE_HEAD_DELAY
            },
        ),
    )
    val firstLineTail by infiniteTransition.animateFloat(
        0f,
        1f,
        infiniteRepeatable(
            animation = keyframes {
                durationMillis = LINEAR_ANIMATION_DURATION
                0f at FIRST_LINE_TAIL_DELAY using FIRST_LINE_TAIL_EASING
                1f at FIRST_LINE_TAIL_DURATION + FIRST_LINE_TAIL_DELAY
            },
        ),
    )
    val secondLineHead by infiniteTransition.animateFloat(
        0f,
        1f,
        infiniteRepeatable(
            animation = keyframes {
                durationMillis = LINEAR_ANIMATION_DURATION
                0f at SECOND_LINE_HEAD_DELAY using SECOND_LINE_HEAD_EASING
                1f at SECOND_LINE_HEAD_DURATION + SECOND_LINE_HEAD_DELAY
            },
        ),
    )
    val secondLineTail by infiniteTransition.animateFloat(
        0f,
        1f,
        infiniteRepeatable(
            animation = keyframes {
                durationMillis = LINEAR_ANIMATION_DURATION
                0f at SECOND_LINE_TAIL_DELAY using SECOND_LINE_TAIL_EASING
                1f at SECOND_LINE_TAIL_DURATION + SECOND_LINE_TAIL_DELAY
            },
        ),
    )
    Canvas(
        modifier
            .increaseSemanticsBounds()
            .semantics(mergeDescendants = true) {
                progressBarRangeInfo = ProgressBarRangeInfo.Indeterminate
            }
            .size(height = 4.dp, width = 24.dp),
    ) {
        val strokeWidth = size.height
        drawLinearIndicatorBackground(backgroundColor, strokeWidth, strokeCap)
        if (firstLineHead - firstLineTail > 0) {
            drawLinearIndicator(
                firstLineHead,
                firstLineTail,
                color,
                strokeWidth,
                strokeCap,
            )
        }
        if ((secondLineHead - secondLineTail) > 0) {
            drawLinearIndicator(
                secondLineHead,
                secondLineTail,
                color,
                strokeWidth,
                strokeCap,
            )
        }
    }
}

internal fun Modifier.increaseSemanticsBounds(): Modifier {
    val padding = 10.dp
    return this
        .layout { measurable, constraints ->
            val paddingPx = padding.roundToPx()
            // We need to add vertical padding to the semantics bounds in other to meet
            // screenreader green box minimum size, but we also want to
            // preserve a visual appearance and layout size below that minimum
            // in order to maintain backwards compatibility. This custom
            // layout effectively implements "negative padding".
            val newConstraint = constraints.offset(0, paddingPx * 2)
            val placeable = measurable.measure(newConstraint)

            // But when actually placing the placeable, create the layout without additional
            // space. Place the placeable where it would've been without any extra padding.
            val height = placeable.height - paddingPx * 2
            val width = placeable.width
            layout(width, height) {
                placeable.place(0, -paddingPx)
            }
        }
        .semantics(mergeDescendants = true) {}
        .padding(vertical = padding)
}

private fun DrawScope.drawLinearIndicator(
    startFraction: Float,
    endFraction: Float,
    color: Color,
    strokeWidth: Float,
    strokeCap: StrokeCap,
) {
    val width = size.width
    val height = size.height
    // Start drawing from the vertical center of the stroke
    val yOffset = height / 2

    val isLtr = layoutDirection == LayoutDirection.Ltr
    val barStart = (if (isLtr) startFraction else 1f - endFraction) * width
    val barEnd = (if (isLtr) endFraction else 1f - startFraction) * width

    // if there isn't enough space to draw the stroke caps, fall back to StrokeCap.Butt
    if (strokeCap == StrokeCap.Butt || height > width) {
        // Progress line
        drawLine(color, Offset(barStart, yOffset), Offset(barEnd, yOffset), strokeWidth)
    } else {
        // need to adjust barStart and barEnd for the stroke caps
        val strokeCapOffset = strokeWidth / 2
        val coerceRange = strokeCapOffset..(width - strokeCapOffset)
        val adjustedBarStart = barStart.coerceIn(coerceRange)
        val adjustedBarEnd = barEnd.coerceIn(coerceRange)

        if (abs(endFraction - startFraction) > 0) {
            // Progress line
            drawLine(
                color,
                Offset(adjustedBarStart, yOffset),
                Offset(adjustedBarEnd, yOffset),
                strokeWidth,
                strokeCap,
            )
        }
    }
}

private fun DrawScope.drawLinearIndicatorBackground(color: Color, strokeWidth: Float, strokeCap: StrokeCap) =
    drawLinearIndicator(0f, 1f, color, strokeWidth, strokeCap)

// Indeterminate linear indicator transition specs
// Total duration for one cycle
private const val LINEAR_ANIMATION_DURATION = 1800

// Duration of the head and tail animations for both lines
private const val FIRST_LINE_HEAD_DURATION = 750
private const val FIRST_LINE_TAIL_DURATION = 850
private const val SECOND_LINE_HEAD_DURATION = 567
private const val SECOND_LINE_TAIL_DURATION = 533

// Delay before the start of the head and tail animations for both lines
private const val FIRST_LINE_HEAD_DELAY = 0
private const val FIRST_LINE_TAIL_DELAY = 333
private const val SECOND_LINE_HEAD_DELAY = 1000
private const val SECOND_LINE_TAIL_DELAY = 1267

private val FIRST_LINE_HEAD_EASING = CubicBezierEasing(0.2f, 0f, 0.8f, 1f)
private val FIRST_LINE_TAIL_EASING = CubicBezierEasing(0.4f, 0f, 1f, 1f)
private val SECOND_LINE_HEAD_EASING = CubicBezierEasing(0f, 0f, 0.65f, 1f)
private val SECOND_LINE_TAIL_EASING = CubicBezierEasing(0.1f, 0f, 0.45f, 1f)

// region Preview
@Preview(showBackground = true, widthDp = 360)
@Preview(showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun LinearProgressIndicator_Preview() {
    TangemThemePreview {
        Column(
            modifier = Modifier
                .background(TangemTheme.colors.background.secondary)
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            TangemLinearProgressIndicator(
                progress = { 0.6f },
                modifier = Modifier.fillMaxWidth(),
                color = TangemTheme.colors.icon.primary1,
                backgroundColor = TangemTheme.colors.background.tertiary,
            )
            TangemLinearProgressIndicator(
                modifier = Modifier.fillMaxWidth(),
                color = TangemTheme.colors.icon.primary1,
                backgroundColor = TangemTheme.colors.background.tertiary,
            )
        }
    }
}
// endregion