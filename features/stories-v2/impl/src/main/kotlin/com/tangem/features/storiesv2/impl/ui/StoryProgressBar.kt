@file:Suppress("MagicNumber")

package com.tangem.features.storiesv2.impl.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.res.TangemTheme

/**
 * The segment strip above a story.
 *
 * [position] is a lambda on purpose: invoked inside the draw scope, it only invalidates drawing. As a parameter it
 * would recompose this composable — and whatever Compose recomposes with it — sixty times a second.
 */
@Composable
internal fun StoryProgressBar(slideCount: Int, slideIndex: Int, position: () -> Float, modifier: Modifier = Modifier) {
    val fillColor = TangemTheme.colors3.text.staticDark.primary
    val trackColor = fillColor.copy(alpha = TRACK_ALPHA)

    Canvas(modifier = modifier.fillMaxWidth().height(SEGMENT_HEIGHT)) {
        if (slideCount <= 0) return@Canvas

        val gap = SEGMENT_GAP.toPx()
        val available = size.width - gap * (slideCount - 1)
        // The design pins a segment to a fixed width and centres the group; a story long enough to overflow shares
        // the width instead, which is the only way the strip stays on one line.
        val segment = minOf(SEGMENT_WIDTH.toPx(), available / slideCount)
        val totalWidth = segment * slideCount + gap * (slideCount - 1)
        val startX = (size.width - totalWidth) / 2f
        val radius = CornerRadius(size.height / 2f)

        repeat(slideCount) { index ->
            val left = startX + index * (segment + gap)
            drawRoundRect(
                color = trackColor,
                topLeft = Offset(left, 0f),
                size = Size(segment, size.height),
                cornerRadius = radius,
            )

            val fill = when {
                index < slideIndex -> 1f
                index > slideIndex -> 0f
                else -> position().coerceIn(0f, 1f)
            }
            if (fill <= 0f) return@repeat

            // Grows from zero width. A minimum of one segment height looks tidier at the start but holds the same
            // value for the first sixth of every slide, which is the stillness this player exists to avoid.
            drawRoundRect(
                color = fillColor,
                topLeft = Offset(left, 0f),
                size = Size(segment * fill, size.height),
                cornerRadius = radius,
            )
        }
    }
}

private val SEGMENT_HEIGHT: Dp = 6.dp
private val SEGMENT_WIDTH: Dp = 32.dp
private val SEGMENT_GAP: Dp = 4.dp
private const val TRACK_ALPHA = 0.15f