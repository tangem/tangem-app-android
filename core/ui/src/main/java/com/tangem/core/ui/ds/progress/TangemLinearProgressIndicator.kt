package com.tangem.core.ui.ds.progress

import android.content.res.Configuration
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.components.progressbar.increaseSemanticsBounds
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import kotlin.math.abs

/**
 * Progress indicator with a dot that reflects the current progress position.
 * The dot is drawn on top of the background line. Dot diameter equals the track height.
 * At progress 0 the dot's left edge aligns with the track's left edge;
 * at progress 1 the dot's right edge aligns with the track's right edge.
 *
 * @param progress The progress of this indicator, where 0.0 represents no progress and 1.0
 * represents full progress. Values outside of this range are coerced into the range.
 * @param modifier the [Modifier] to be applied to this progress indicator
 * @param dotColor The color of the progress dot.
 * @param backgroundColor The color of the background track.
 * @param strokeCap stroke cap to use for the ends of the background track
 */
@Composable
fun TangemLinearProgressIndicatorWithDot(
    progress: () -> Float,
    dotColor: Color,
    backgroundColor: Color,
    modifier: Modifier = Modifier,
    strokeCap: StrokeCap = StrokeCap.Round,
) {
    val coercedProgress = { progress().coerceIn(0f, 1f) }
    Canvas(
        modifier
            .increaseSemanticsBounds()
            .semantics(mergeDescendants = true) {
                progressBarRangeInfo = ProgressBarRangeInfo(coercedProgress(), 0f..1f)
            },
    ) {
        val strokeWidth = size.height
        drawLinearIndicatorBackground(backgroundColor, strokeWidth, strokeCap)
        drawLinearProgressDot(
            progress = coercedProgress(),
            color = dotColor,
            dotDiameter = strokeWidth,
        )
    }
}

private fun DrawScope.drawLinearIndicatorBackground(color: Color, strokeWidth: Float, strokeCap: StrokeCap) =
    drawLinearIndicator(
        startFraction = 0f,
        endFraction = 1f,
        color = color,
        strokeWidth = strokeWidth,
        strokeCap = strokeCap,
    )

private fun DrawScope.drawLinearProgressDot(progress: Float, color: Color, dotDiameter: Float) {
    val width = size.width
    val radius = dotDiameter / 2
    val yOffset = size.height / 2

    val isLtr = layoutDirection == LayoutDirection.Ltr
    val centerX = if (isLtr) {
        radius + progress * (width - dotDiameter)
    } else {
        width - radius - progress * (width - dotDiameter)
    }

    drawCircle(
        color = color,
        radius = radius,
        center = Offset(centerX, yOffset),
    )
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
        drawLine(
            color = color,
            start = Offset(barStart, yOffset),
            end = Offset(barEnd, yOffset),
            strokeWidth = strokeWidth,
        )
    } else {
        // need to adjust barStart and barEnd for the stroke caps
        val strokeCapOffset = strokeWidth / 2
        val coerceRange = strokeCapOffset..width - strokeCapOffset
        val adjustedBarStart = barStart.coerceIn(coerceRange)
        val adjustedBarEnd = barEnd.coerceIn(coerceRange)

        if (abs(endFraction - startFraction) > 0) {
            // Progress line
            drawLine(
                color = color,
                start = Offset(adjustedBarStart, yOffset),
                end = Offset(adjustedBarEnd, yOffset),
                strokeWidth = strokeWidth,
                cap = strokeCap,
            )
        }
    }
}

// region Preview
@Preview(showBackground = true, widthDp = 360)
@Preview(showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun LinearProgressIndicator_Preview() {
    TangemThemePreviewRedesign {
        Column(
            modifier = Modifier
                .background(TangemTheme.colors.background.secondary)
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            TangemLinearProgressIndicatorWithDot(
                progress = { 1f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp),
                dotColor = TangemTheme.colors2.fill.status.accent,
                backgroundColor = TangemTheme.colors2.graphic.neutral.primaryInvertedConstant,
            )
        }
    }
}
// endregion