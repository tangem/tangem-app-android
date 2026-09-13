package com.tangem.core.ui.ds2.tabnavigation

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.ds2.surface.TangemSurface
import com.tangem.core.ui.res.TangemTheme
import kotlinx.coroutines.launch

@Immutable
internal data class PillBounds(val offset: Dp, val width: Dp)

/**
 * The single pill the whole row shares, drawn behind the tabs and inside the same scrolling content
 * so it travels with them during animation.
 */
@Composable
internal fun SelectionPill(
    bounds: PillBounds,
    selectionKey: Any?,
    variant: TangemTabItem.Variant,
    modifier: Modifier = Modifier,
) {
    // Seeded with the first measured bounds so the pill appears under the initially selected tab
    // instead of flying in from the row's start.
    val offset = remember { Animatable(bounds.offset, Dp.VectorConverter) }
    val width = remember { Animatable(bounds.width, Dp.VectorConverter) }
    val stretch = remember { Animatable(NO_STRETCH) }

    LaunchedEffect(bounds) {
        launch { offset.animateTo(bounds.offset, PillTravelSpec) }
        launch { width.animateTo(bounds.width, PillTravelSpec) }
    }

    // Keyed on the selection rather than the bounds, so a tab that merely re-measures (its counter
    // changed, say) resizes the pill without replaying the stretch.
    var isInitialSelection by remember { mutableStateOf(true) }
    LaunchedEffect(selectionKey) {
        if (isInitialSelection) {
            isInitialSelection = false
            return@LaunchedEffect
        }
        stretch.animateTo(targetValue = NO_STRETCH, animationSpec = PillStretchSpec)
    }

    TangemSurface(
        modifier = modifier
            .offset { IntOffset(x = offset.value.roundToPx(), y = 0) }
            .graphicsLayer { scaleX = stretch.value }
            // Sized in the layout phase rather than with Modifier.size, so the animating width
            // remeasures without recomposing.
            .layout { measurable, _ ->
                val pillWidth = width.value.roundToPx().coerceAtLeast(0)
                val pillHeight = ItemHeight.roundToPx()
                val placeable = measurable.measure(Constraints.fixed(pillWidth, pillHeight))
                layout(pillWidth, pillHeight) { placeable.place(0, 0) }
            },
        color = when (variant) {
            TangemTabItem.Variant.Material -> Color.Transparent
            TangemTabItem.Variant.Transparent -> TangemTheme.colors3.bg.opaque.secondary
        },
        isMaterial = variant == TangemTabItem.Variant.Material,
        shape = CircleShape,
        content = {},
    )
}

@Composable
internal fun AutoScrollToSelected(scrollState: ScrollState, bounds: PillBounds?, margin: Dp) {
    val density = LocalDensity.current
    var isInitialScroll by remember { mutableStateOf(true) }
    // Keyed on the viewport too: the first bounds can land before the row has been measured, and
    // without this the initial scroll-into-view would be skipped and never retried.
    val viewport = scrollState.viewportSize

    LaunchedEffect(bounds, viewport) {
        if (bounds == null || viewport == 0) return@LaunchedEffect

        with(density) {
            val marginPx = margin.roundToPx()
            val start = marginPx + bounds.offset.roundToPx()
            val end = start + bounds.width.roundToPx()
            val target = when {
                start - marginPx < scrollState.value -> start - marginPx
                end + marginPx > scrollState.value + viewport -> end + marginPx - viewport
                else -> return@LaunchedEffect
            }.coerceIn(0, scrollState.maxValue)

            if (isInitialScroll) {
                isInitialScroll = false
                scrollState.scrollTo(target)
            } else {
                scrollState.animateScrollTo(target, PillScrollSpec)
            }
        }
    }
}

internal val ItemSpacing: Dp = 4.dp
internal val ContentHorizontalPadding: Dp = 16.dp

// region Figma: 💠 Tab Navigation → // Animation

private const val NO_STRETCH = 1f
private const val MAX_STRETCH = 1.1f
private const val PILL_TRAVEL_DURATION_MS = 400
private const val PILL_STRETCH_PEAK_MS = 200

private val PillTravelEasing = CubicBezierEasing(a = 0.8f, b = 0f, c = 0.4f, d = 1.2f)
private val PillStretchEasing = CubicBezierEasing(a = 0.5f, b = 0f, c = 0.5f, d = 1f)

/** `PILL MOVEMENT & RESIZE`: 0 → 400ms. The 1.2 control point overshoots the target before settling. */
private val PillTravelSpec: AnimationSpec<Dp> = tween(
    durationMillis = PILL_TRAVEL_DURATION_MS,
    easing = PillTravelEasing,
)

/**
 * The same travel without the overshoot: a scroll that overshoots just clamps at `maxValue` and
 * reads as a stall rather than as a bounce.
 */
private val PillScrollSpec: AnimationSpec<Float> = tween(
    durationMillis = PILL_TRAVEL_DURATION_MS,
    easing = CubicBezierEasing(a = 0.8f, b = 0f, c = 0.4f, d = 1f),
)

/**
 * `PILL SCALE`: axis-X 100% → 110% over 0–200ms, then back to 100% over 200–400ms.
 *
 * The 0ms keyframe is spelled out because Compose synthesizes a missing start keyframe with
 * `LinearEasing`, which would leave the whole first half un-eased.
 */
private val PillStretchSpec: AnimationSpec<Float> = keyframes {
    durationMillis = PILL_TRAVEL_DURATION_MS
    NO_STRETCH at 0 using PillStretchEasing
    MAX_STRETCH at PILL_STRETCH_PEAK_MS using PillStretchEasing
    NO_STRETCH at PILL_TRAVEL_DURATION_MS
}

// endregion