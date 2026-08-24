package com.tangem.core.ui.ds2.segmentedcontrol

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.haptic.TangemHapticEffect
import com.tangem.core.ui.res.LocalHapticManager
import com.tangem.core.ui.res.TangemTheme
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.launch

@Immutable
internal data class PillBounds(val offset: Dp, val width: Dp)

/**
 * Offset and width of the selection pill, derived from the measured segment widths. `null` until the
 * selected segment (and everything before it) has been measured.
 */
internal fun resolvePillBounds(
    items: ImmutableList<TangemSegmentedControl.Item>,
    widths: Map<String, Dp>,
): PillBounds? {
    val selectedIndex = items.indexOfFirst { it.isSelected }
    if (selectedIndex < 0) return null

    var offset = 0.dp
    for (i in 0 until selectedIndex) {
        offset += (widths[items[i].id] ?: return null) + ItemSpacing
    }
    return PillBounds(offset = offset, width = widths[items[selectedIndex].id] ?: return null)
}

/**
 * The single pill all segments share, drawn behind them so it can travel between segments instead of
 * crossfading in place.
 */
@Composable
internal fun SelectionPill(bounds: PillBounds, selectionKey: Any?, modifier: Modifier = Modifier) {
    // Seeded with the first measured bounds so the pill appears under the initially selected segment
    // instead of flying in from the control's start.
    val offset = remember { Animatable(bounds.offset, Dp.VectorConverter) }
    val width = remember { Animatable(bounds.width, Dp.VectorConverter) }
    val stretch = remember { Animatable(NO_STRETCH) }

    LaunchedEffect(bounds) {
        launch { offset.animateTo(bounds.offset, PillTravelSpec) }
        launch { width.animateTo(bounds.width, PillTravelSpec) }
    }

    // Keyed on the selection rather than the bounds, so a segment that merely re-measures (the control
    // got resized, say) moves the pill without replaying the stretch.
    var isInitialSelection by remember { mutableStateOf(true) }
    LaunchedEffect(selectionKey) {
        if (isInitialSelection) {
            isInitialSelection = false
            return@LaunchedEffect
        }
        stretch.animateTo(targetValue = NO_STRETCH, animationSpec = PillStretchSpec)
    }

    Box(
        modifier = modifier
            .offset { IntOffset(x = offset.value.roundToPx(), y = 0) }
            .graphicsLayer { scaleX = stretch.value }
            .layout { measurable, _ ->
                val pillWidth = width.value.roundToPx().coerceAtLeast(0)
                val pillHeight = ItemHeight.roundToPx()
                val placeable = measurable.measure(Constraints.fixed(pillWidth, pillHeight))
                layout(pillWidth, pillHeight) { placeable.place(0, 0) }
            }
            .background(color = TangemTheme.colors3.bg.special.control, shape = CircleShape),
    )
}

/**
 * A single segment: label only — the selected background is painted by the shared [SelectionPill].
 */
@Composable
internal fun SegmentedItem(item: TangemSegmentedControl.Item, modifier: Modifier = Modifier) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val isFocused by interactionSource.collectIsFocusedAsState()
    val contentColor = animateContentColor(selected = item.isSelected, isPressed = isPressed)
    val hapticManager = LocalHapticManager.current

    Box(
        modifier = modifier
            .heightIn(min = ItemHeight)
            .widthIn(min = ItemMinWidth)
            .selectable(
                selected = item.isSelected,
                interactionSource = interactionSource,
                indication = null,
                role = Role.RadioButton,
                onClick = {
                    if (!item.isSelected) hapticManager.perform(TangemHapticEffect.View.SegmentTick)
                    item.onClick()
                },
            )
            .focusRing(isFocused = isFocused),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = item.label.resolveReference(),
            color = contentColor,
            style = TangemTheme.typography3.subheading.medium,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = LabelHorizontalPadding),
        )
    }
}

@Composable
private fun Modifier.focusRing(isFocused: Boolean): Modifier {
    if (!isFocused) return this
    return border(
        width = FocusRingWidth,
        color = TangemTheme.colors3.interaction.focusRing.brand,
        shape = CircleShape,
    )
}

@Composable
private fun animateContentColor(selected: Boolean, isPressed: Boolean): Color {
    val colors = TangemTheme.colors3
    val target = when {
        selected && isPressed -> colors.text.secondary
        selected -> colors.text.primary
        isPressed -> colors.text.tertiary
        else -> colors.text.secondary
    }
    val color by animateColorAsState(
        targetValue = target,
        animationSpec = when {
            selected -> ContentActivationSpec
            isPressed -> ContentPressSpec
            else -> ContentReleaseSpec
        },
        label = "segmentedItemContentColor",
    )
    return color
}

internal val ControlHeight: Dp = 40.dp
internal val ContainerPadding: Dp = 2.dp
internal val ItemSpacing: Dp = 4.dp
internal val ItemHeight: Dp = 36.dp
internal val PillRadius: Dp = 999.dp
internal val ShimmerWidth: Dp = 200.dp
private val ItemMinWidth: Dp = 40.dp
private val LabelHorizontalPadding: Dp = 12.dp
private val FocusRingWidth: Dp = 2.dp

// region Figma: 💠 Segmented Control → // Animation: 150ms ease-out, then 200ms ease-out

private const val NO_STRETCH = 1f
private const val MAX_STRETCH = 1.06f
private const val PILL_TRAVEL_DURATION_MS = 150
private const val PILL_STRETCH_PEAK_MS = 75
private const val CONTENT_MORPH_DURATION_MS = 150
private const val CONTENT_ACTIVATION_DELAY_MS = 150
private const val CONTENT_ACTIVATION_DURATION_MS = 200

private val EaseOutEasing = CubicBezierEasing(a = 0f, b = 0f, c = 0.58f, d = 1f)

/** `PILL MOVEMENT & RESIZE`: 0 → 150ms. */
private val PillTravelSpec: AnimationSpec<Dp> = tween(
    durationMillis = PILL_TRAVEL_DURATION_MS,
    easing = EaseOutEasing,
)

/**
 * `PILL SCALE`: axis-X 100% → 106% over 0–75ms, then back to 100% over 75–150ms, so the stretch
 * settles together with the travel.
 *
 * The 0ms keyframe is spelled out because Compose synthesizes a missing start keyframe with
 * `LinearEasing`, which would leave the whole first half un-eased.
 */
private val PillStretchSpec: AnimationSpec<Float> = keyframes {
    durationMillis = PILL_TRAVEL_DURATION_MS
    NO_STRETCH at 0 using EaseOutEasing
    MAX_STRETCH at PILL_STRETCH_PEAK_MS using EaseOutEasing
    NO_STRETCH at PILL_TRAVEL_DURATION_MS
}

/** `SEGMENT PRESS`: 0 → 150ms. */
private val ContentPressSpec: AnimationSpec<Color> = tween(
    durationMillis = CONTENT_MORPH_DURATION_MS,
    easing = EaseOutEasing,
)

/** `SEGMENT PRESS` running backwards after the press ends. */
private val ContentReleaseSpec: AnimationSpec<Color> = ContentPressSpec

/** `SEGMENT ACTIVATION`: 150 → 350ms — the label lights up only once the pill has arrived. */
private val ContentActivationSpec: AnimationSpec<Color> = tween(
    durationMillis = CONTENT_ACTIVATION_DURATION_MS,
    delayMillis = CONTENT_ACTIVATION_DELAY_MS,
    easing = EaseOutEasing,
)

// endregion