package com.tangem.features.foryou.impl.ui.components

import android.content.res.Configuration
import androidx.annotation.IntRange
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.components.RectangleShimmer
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import kotlin.math.roundToInt

/**
 * Visual state of the [GradientScaleBar].
 */
internal sealed interface GradientScaleBarState {

    /**
     * Loaded state — renders the horizontal error → info → success gradient track with a circular
     * indicator snapped to [value].
     *
     * @param value current value to point at; coerced into [range].
     * @param range inclusive range of selectable values; its size defines the number of positions
     * (default [DEFAULT_RANGE] = `-5..5`, i.e. 11 positions).
     */
    data class Content(
        @param:IntRange(from = -5, to = 5) val value: Int,
        val range: kotlin.ranges.IntRange = DEFAULT_RANGE,
    ) : GradientScaleBarState

    /** Loading state — renders an animated shimmer placeholder sized to the track. */
    data object Loading : GradientScaleBarState

    /** No-data state — renders a static disabled track. */
    data object NoData : GradientScaleBarState
}

/**
 * Horizontal gradient scale bar with a round indicator snapped to a value on the scale.
 *
 * Renders one of three variants depending on [state]:
 * - [GradientScaleBarState.Content] — the error → info → success gradient track with the circular
 *   indicator snapped to one of the evenly-spaced positions defined by its range (e.g. the default
 *   `-5..5` yields 11 positions). The indicator never overflows the track: its center travels from
 *   the left edge (`range.first`) to the right edge (`range.last`).
 * - [GradientScaleBarState.Loading] — an animated shimmer placeholder.
 * - [GradientScaleBarState.NoData] — a static disabled track.
 *
 * All variants share the same track geometry ([TRACK_HEIGHT], [TRACK_CORNER]) and occupy the same
 * vertical space ([INDICATOR_SIZE]), so switching between states does not shift the layout.
 *
 * @param state visual state to render.
 * @param modifier the [Modifier] to be applied to the component. Width is taken from the incoming
 * constraints (defaults to intrinsic content otherwise) — pass `Modifier.fillMaxWidth()` to stretch.
 */
@Composable
internal fun GradientScaleBar(state: GradientScaleBarState, modifier: Modifier = Modifier) {
    BoxWithConstraints(
        modifier = modifier
            .padding(vertical = 5.dp)
            .height(INDICATOR_SIZE),
    ) {
        when (state) {
            is GradientScaleBarState.Content -> ContentBar(state = state)
            GradientScaleBarState.Loading -> RectangleShimmer(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(TRACK_HEIGHT)
                    .align(Alignment.Center),
                radius = TRACK_CORNER,
            )
            GradientScaleBarState.NoData -> Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(TRACK_HEIGHT)
                    .align(Alignment.Center)
                    .background(TangemTheme.colors3.bg.disabled, RoundedCornerShape(TRACK_CORNER)),
            )
        }
    }
}

@Composable
private fun BoxWithConstraintsScope.ContentBar(state: GradientScaleBarState.Content) {
    val trackBrush = Brush.horizontalGradient(
        colors = listOf(
            TangemTheme.colors3.bg.status.error,
            TangemTheme.colors3.bg.status.info,
            TangemTheme.colors3.bg.status.success,
        ),
    )
    val indicatorColor = TangemTheme.colors3.icon.primary

    // Track — centered vertically, thinner than the indicator.
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(TRACK_HEIGHT)
            .offset(y = (INDICATOR_SIZE - TRACK_HEIGHT) / 2)
            .background(trackBrush, RoundedCornerShape(TRACK_CORNER))
            .clip(RoundedCornerShape(TRACK_CORNER)),
    )

    // Indicator — snapped to one of the positions defined by the range.
    val range = state.range
    val steps = range.last - range.first + 1
    val fraction = if (steps <= 1) {
        0f
    } else {
        (state.value.coerceIn(range) - range.first).toFloat() / (steps - 1)
    }
    Box(
        modifier = Modifier
            .offset {
                val travel = maxWidth.toPx() - INDICATOR_SIZE.toPx()
                IntOffset(x = (fraction * travel).roundToInt(), y = 0)
            }
            .size(INDICATOR_SIZE)
            .dropShadow(
                shape = CircleShape,
                shadow = Shadow(
                    radius = 4.dp, // TODO
                    spread = 0.dp,
                    color = Color.Black.copy(alpha = 0.25f),
                ),
            )
            .clip(CircleShape)
            .background(indicatorColor),
    )
}

private val DEFAULT_RANGE = -5..5
private val INDICATOR_SIZE = 10.dp
private val TRACK_HEIGHT = 6.dp
private val TRACK_CORNER = 10.dp

@Preview(showBackground = true, widthDp = 360)
@Preview(showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun GradientScaleBar_Preview() {
    TangemThemePreviewRedesign {
        Column(
            modifier = Modifier
                .background(TangemTheme.colors3.bg.primary)
                .padding(16.dp),
        ) {
            GradientScaleBar(
                state = GradientScaleBarState.Content(value = -5),
                modifier = Modifier.fillMaxWidth(),
            )
            GradientScaleBar(
                state = GradientScaleBarState.Loading,
                modifier = Modifier.fillMaxWidth(),
            )
            GradientScaleBar(
                state = GradientScaleBarState.NoData,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}