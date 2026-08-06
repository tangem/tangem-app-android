package com.tangem.core.ui.ds2.animation

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.shrinkVertically
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.IntSize

/**
 * Animation specs shared by the design-system v2 components (`ds2`).
 *
 * Declared once so size, alpha and color animations stay in sync within a component and read the same
 * across components — a chip, a button and the top navigation bar animate with the same feel.
 *
 * The values are stateless and immutable, so they are top-level (no `remember` needed at call sites).
 */
internal object TangemAnimationSpec {

    /** Snappy spring for size changes — expand/shrink and [androidx.compose.animation.SizeTransform]. */
    val Size: FiniteAnimationSpec<IntSize> = spring(stiffness = Spring.StiffnessMediumLow)

    /** Snappy spring for alpha changes — fades and cross-fades. Paired with [Size]. */
    val Alpha: FiniteAnimationSpec<Float> = spring(stiffness = Spring.StiffnessMediumLow)

    /** Snappy spring for color changes, so state-driven recoloring doesn't snap. */
    val Tint: FiniteAnimationSpec<Color> = spring(stiffness = Spring.StiffnessMediumLow)

    /** Bouncy spring for scale changes — press feedback (shrink and spring back) and marks popping in. */
    val Bouncy: FiniteAnimationSpec<Float> = spring(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessMediumLow,
    )

    /** Short tween fading the press overlay in and out instead of toggling it instantly. */
    val PressOverlay: FiniteAnimationSpec<Color> = tween(durationMillis = 100)

    /** Short tween driving a progress-based fill (0 = empty, 1 = filled). */
    val Fill: FiniteAnimationSpec<Float> = tween(durationMillis = 150)
}

/**
 * [EnterTransition] / [ExitTransition] pairs built from [TangemAnimationSpec] and shared by the
 * design-system v2 components (`ds2`).
 */
internal object TangemTransition {

    /**
     * Slot appearing horizontally: fades in while growing in width.
     *
     * [expandHorizontally] defaults to `expandFrom = Alignment.End`, which anchors the slot's end edge
     * and grows it towards the start.
     */
    val SlotEnterHorizontally: EnterTransition =
        fadeIn(animationSpec = TangemAnimationSpec.Alpha) +
            expandHorizontally(animationSpec = TangemAnimationSpec.Size)

    /** Counterpart of [SlotEnterHorizontally]. */
    val SlotExitHorizontally: ExitTransition =
        fadeOut(animationSpec = TangemAnimationSpec.Alpha) +
            shrinkHorizontally(animationSpec = TangemAnimationSpec.Size)

    /** Slot appearing vertically: fades in while growing in height. Use when it pushes siblings down. */
    val SlotEnterVertically: EnterTransition =
        fadeIn(animationSpec = TangemAnimationSpec.Alpha) +
            expandVertically(animationSpec = TangemAnimationSpec.Size)

    /** Counterpart of [SlotEnterVertically]. */
    val SlotExitVertically: ExitTransition =
        fadeOut(animationSpec = TangemAnimationSpec.Alpha) + shrinkVertically(animationSpec = TangemAnimationSpec.Size)

    /** Pure fade in — for content swapped in place, where a size animation would read as a jump. */
    val FadeEnter: EnterTransition = fadeIn(animationSpec = TangemAnimationSpec.Alpha)

    /** Counterpart of [FadeEnter]. */
    val FadeExit: ExitTransition = fadeOut(animationSpec = TangemAnimationSpec.Alpha)
}