@file:Suppress("MagicNumber")

package com.tangem.core.ui.ds2.expandable

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.Transition
import androidx.compose.animation.core.animateDp
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp

/**
 * Motion of an expandable row between its collapsed and expanded states, transcribed from the
 * `Animation Breakdown (Expanding)` / `(Collapsing)` sections of the account row's Figma page. The
 * expandable row's own page carries no breakdown because it inherits this one.
 *
 * Figma describes every element as an offset into the gesture plus a duration, which maps 1:1 onto
 * `tween(durationMillis, delayMillis, easing)`. The whole choreography therefore runs off a single
 * [Transition], one `transitionSpec` per element, so the pieces stay in phase no matter how often
 * the state is toggled mid-flight.
 */
@Immutable
internal data class ExpansionMotion(
    /** 0 = fully collapsed, 1 = fully expanded. Drives height, avatar size, title scale and positions. */
    val expansion: Float,
    val subtitleAlpha: Float,
    /** Top line of the collapsed end column — the account row's balance, the expandable row's value. */
    val endTopAlpha: Float,
    /** Line under it — the account row's price change, the expandable row's caption. */
    val endBottomAlpha: Float,
    /** The compact copies that join the title while expanded. */
    val compactAlpha: Float,
    val dotAlpha: Float,
    /**
     * Horizontal drift of the compact line. Figma animates its position alongside its opacity, so it
     * slides out as it dissolves and slides in as it appears instead of popping at its final spot.
     */
    val compactOffset: Dp,
    val arrowAlpha: Float,
    val slotAlpha: Float,
    /**
     * `true` once the transition has come to rest. The title is drawn as scaled `body.medium` while
     * the row is moving and swapped to real `caption.medium` here, which is the "getting text style
     * caption/medium at final moment" note in Figma — scaled text stays crisp only at rest.
     */
    val isSettled: Boolean,
)

private val StandardEasing = CubicBezierEasing(a = 0.6f, b = 0f, c = 0.4f, d = 1f)
private val AcceleratedEasing = CubicBezierEasing(a = 0.7f, b = 0f, c = 0.3f, d = 1f)
private val PressEasing = CubicBezierEasing(a = 0.5f, b = 0f, c = 0.5f, d = 1f)

/** Opacity every heading element drops to while the row is held down. */
internal const val ROW_PRESSED_ALPHA = 0.6f

/** Time the heading takes to reach [ROW_PRESSED_ALPHA], and the floor on how long it stays there. */
private const val PRESS_DURATION_MS = 150

private val RowPressSpec: FiniteAnimationSpec<Float> =
    tween(durationMillis = PRESS_DURATION_MS, easing = PressEasing)

/** Title scale at the expanded end — `caption.medium` (12sp) over `body.medium` (16sp). */
internal const val ROW_TITLE_EXPANDED_SCALE = 0.75f

/**
 * Opacity the heading elements are drawn at while the row is held down.
 *
 * A tap is usually shorter than the ramp, so releasing the moment the finger lifts leaves the fade
 * interrupted part-way down — around 0.87 for a 60ms tap, close enough to 1 that the press reads as
 * no feedback at all. Collecting sequentially and suspending on [Animatable.animateTo] makes the
 * release wait for the press to finish, so every tap reaches [ROW_PRESSED_ALPHA] before it comes
 * back. Expressing the hold in animation time rather than wall-clock also keeps it correct when the
 * system animator duration scale is not 1.
 */
@Composable
internal fun rememberRowPressAlpha(interactionSource: InteractionSource): State<Float> {
    val alpha = remember { Animatable(initialValue = 1f) }
    LaunchedEffect(interactionSource) {
        interactionSource.interactions.collect { interaction ->
            when (interaction) {
                is PressInteraction.Press ->
                    alpha.animateTo(targetValue = ROW_PRESSED_ALPHA, animationSpec = RowPressSpec)
                is PressInteraction.Release, is PressInteraction.Cancel ->
                    alpha.animateTo(targetValue = 1f, animationSpec = RowPressSpec)
            }
        }
    }
    return alpha.asState()
}

/**
 * Whether the title is currently real `caption.medium` rather than scaled-down `body.medium`. The
 * layout has to agree with the title composable on this, or the dot and the compact line are placed
 * against a width the title no longer has.
 */
internal val ExpansionMotion.isTitleCaption: Boolean
    get() = isSettled && expansion == 1f

/** Scale the title placeable is drawn at: 1 once it is real caption text, interpolated until then. */
internal val ExpansionMotion.titleScale: Float
    get() = if (isTitleCaption) {
        1f
    } else {
        lerp(start = 1f, stop = ROW_TITLE_EXPANDED_SCALE, fraction = expansion)
    }

@Composable
internal fun rememberExpansionMotion(isExpanded: Boolean): ExpansionMotion {
    val transition = updateTransition(targetState = isExpanded, label = "RowExpansion")

    val expansion by transition.animateFloat(
        label = "expansion",
        transitionSpec = { tween(durationMillis = 300, delayMillis = 150, easing = StandardEasing) },
    ) { expanded -> if (expanded) 1f else 0f }

    return ExpansionMotion(
        expansion = expansion,
        subtitleAlpha = transition.animateExpansionAlpha(
            label = "subtitleAlpha",
            expandedValue = 0f,
            expandSpec = tween(durationMillis = 100, delayMillis = 200, easing = StandardEasing),
            collapseSpec = tween(durationMillis = 100, delayMillis = 300, easing = StandardEasing),
        ),
        endTopAlpha = transition.animateExpansionAlpha(
            label = "endTopAlpha",
            expandedValue = 0f,
            expandSpec = tween(durationMillis = 100, delayMillis = 150, easing = StandardEasing),
            collapseSpec = tween(durationMillis = 180, delayMillis = 270, easing = StandardEasing),
        ),
        endBottomAlpha = transition.animateExpansionAlpha(
            label = "endBottomAlpha",
            expandedValue = 0f,
            expandSpec = tween(durationMillis = 100, delayMillis = 150, easing = AcceleratedEasing),
            collapseSpec = tween(durationMillis = 180, delayMillis = 270, easing = StandardEasing),
        ),
        compactAlpha = transition.animateExpansionAlpha(
            label = "compactAlpha",
            expandedValue = 1f,
            expandSpec = tween(durationMillis = 120, delayMillis = 270, easing = StandardEasing),
            collapseSpec = tween(durationMillis = 50, delayMillis = 270, easing = AcceleratedEasing),
        ),
        compactOffset = transition.animateCompactOffset(),
        dotAlpha = transition.animateExpansionAlpha(
            label = "dotAlpha",
            expandedValue = 1f,
            expandSpec = tween(durationMillis = 120, delayMillis = 270, easing = StandardEasing),
            collapseSpec = tween(durationMillis = 120, delayMillis = 150, easing = StandardEasing),
        ),
        arrowAlpha = transition.animateExpansionAlpha(
            label = "arrowAlpha",
            expandedValue = 1f,
            expandSpec = tween(durationMillis = 150, delayMillis = 270, easing = StandardEasing),
            collapseSpec = tween(durationMillis = 180, delayMillis = 150, easing = StandardEasing),
        ),
        slotAlpha = transition.animateExpansionAlpha(
            label = "slotAlpha",
            expandedValue = 1f,
            expandSpec = tween(durationMillis = 340, delayMillis = 210, easing = StandardEasing),
            collapseSpec = tween(durationMillis = 180, delayMillis = 150, easing = StandardEasing),
        ),
        isSettled = transition.currentState == transition.targetState,
    )
}

@Composable
private fun Transition<Boolean>.animateCompactOffset(): Dp {
    val offset by animateDp(
        label = "compactOffset",
        transitionSpec = { tween(durationMillis = 180, delayMillis = 270, easing = AcceleratedEasing) },
    ) { expanded -> if (expanded) 0.dp else 26.dp }
    return offset
}

/**
 * One element's opacity leg of the choreography: [expandedValue] at the expanded end, its complement
 * at the collapsed one, with a separate spec per direction — Figma gives expanding and collapsing
 * different offsets and durations for nearly every element.
 */
@Composable
private fun Transition<Boolean>.animateExpansionAlpha(
    label: String,
    expandedValue: Float,
    expandSpec: FiniteAnimationSpec<Float>,
    collapseSpec: FiniteAnimationSpec<Float>,
): Float {
    val alpha by animateFloat(
        label = label,
        transitionSpec = { if (targetState) expandSpec else collapseSpec },
    ) { expanded -> if (expanded) expandedValue else 1f - expandedValue }
    return alpha
}