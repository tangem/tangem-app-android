package com.tangem.core.ui.ds.topbar.collapsing

import androidx.compose.animation.core.*
import androidx.compose.animation.rememberSplineBasedDecay
import androidx.compose.foundation.gestures.FlingBehavior
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.ScrollScope
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.ds.topbar.collapsing.entity.TangemCollapsingAppBarState
import com.tangem.core.ui.ds.topbar.collapsing.entity.TopBapScrollDirection
import com.tangem.core.ui.ds.topbar.collapsing.entity.rememberTangemCollapsingAppBarState
import com.tangem.core.ui.utils.toPx
import kotlin.math.abs
import kotlin.math.absoluteValue

/**
 * A scroll behavior for a collapsing top app bar that collapses when scrolling up and expands when scrolling down.
 * When the user stops scrolling, the app bar will settle to either fully collapsed or fully expanded state
 * based on the current collapsed fraction and scroll direction.
 *
 * @param expandedHeight The height of the app bar when it is fully expanded.
 * @param partialCollapsedHeight The height of the app bar when it is partially collapsed.
 * @param snapAnimationSpec The animation spec for snapping the app bar to the collapsed or expanded state when the
 *                          user stops scrolling. If null, no snapping will occur.
 * @param flingAnimationSpec The decay animation spec for the fling behavior when the user flings the app bar.
 *                           If null, no fling behavior will occur.
 */
@Composable
fun rememberTangemExitUntilCollapsedScrollBehavior(
    expandedHeight: Dp = -Int.MAX_VALUE.dp,
    partialCollapsedHeight: Dp = expandedHeight,
    snapAnimationSpec: AnimationSpec<Float>? = spring(),
    flingAnimationSpec: DecayAnimationSpec<Float>? = rememberSplineBasedDecay(),
): TangemCollapsingAppBarBehavior {
    val topBarState = rememberTangemCollapsingAppBarState(
        heightOffsetLimit = -expandedHeight.toPx(),
        partialHeightLimit = partialCollapsedHeight.toPx(),
    )
    return exitUntilCollapsedScrollBehavior(
        state = topBarState,
        snapAnimationSpec = snapAnimationSpec,
        flingAnimationSpec = flingAnimationSpec,
    )
}

/**
 * A scroll behavior for a collapsing top app bar that collapses when scrolling up and expands when scrolling down.
 * When the user stops scrolling, the app bar will settle to either fully collapsed or fully expanded state
 * based on the current collapsed fraction and scroll direction.
 *
 * @param state The state of the collapsing app bar, which controls the height offset and scroll behavior.
 * @param snapAnimationSpec The animation spec for snapping the app bar to the collapsed or expanded state when the
 *                          user stops scrolling. If null, no snapping will occur.
 * @param flingAnimationSpec The decay animation spec for the fling behavior when the user flings the app bar.
 *                           If null, no fling behavior will occur.
 */
@Composable
private fun exitUntilCollapsedScrollBehavior(
    state: TangemCollapsingAppBarState = rememberTangemCollapsingAppBarState(),
    snapAnimationSpec: AnimationSpec<Float>? = spring(),
    flingAnimationSpec: DecayAnimationSpec<Float>? = rememberSplineBasedDecay(),
): TangemCollapsingAppBarBehavior {
    val nestedScrollConnection = remember(state) {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                val dy = available.y

                val consume = if (dy < 0) {
                    state.direction = TopBapScrollDirection.Collapsing
                    state.dispatchRawDelta(dy)
                } else {
                    0f
                }

                return Offset(0f, consume)
            }

            override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                val dy = available.y

                val consume = if (dy > 0) {
                    state.direction = TopBapScrollDirection.Expanding
                    state.dispatchRawDelta(dy)
                } else {
                    state.direction = TopBapScrollDirection.Collapsing
                    0f
                }

                return Offset(0f, consume)
            }

            override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                val superConsumed = super.onPostFling(consumed, available)
                return superConsumed + settleAppBar(
                    state = state,
                    velocity = available.y,
                    flingAnimationSpec = flingAnimationSpec,
                    snapAnimationSpec = snapAnimationSpec,
                )
            }
        }
    }

    return remember(state, nestedScrollConnection, snapAnimationSpec, flingAnimationSpec) {
        TangemCollapsingAppBarBehavior(
            state = state,
            snapAnimationSpec = snapAnimationSpec,
            flingAnimationSpec = flingAnimationSpec,
            nestedScrollConnection = nestedScrollConnection,
        )
    }
}

@Composable
fun Modifier.snapToExitUntilCollapsed(behavior: TangemCollapsingAppBarBehavior): Modifier {
    return nestedScroll(behavior.nestedScrollConnection)
        .scrollable(
            orientation = Orientation.Vertical,
            state = behavior.state,
            flingBehavior = remember(behavior) {
                object : FlingBehavior {
                    override suspend fun ScrollScope.performFling(initialVelocity: Float): Float {
                        val consumed = settleAppBar(
                            state = behavior.state,
                            velocity = initialVelocity,
                            flingAnimationSpec = behavior.flingAnimationSpec,
                            snapAnimationSpec = behavior.snapAnimationSpec,
                        )
                        return initialVelocity - consumed.y
                    }
                }
            },
        )
}

/**
 * Settles the app bar to either fully collapsed or fully expanded state
 * based on the current collapsed fraction and scroll direction.
 */
@Suppress("MagicNumber", "CyclomaticComplexMethod")
private suspend fun settleAppBar(
    state: TangemCollapsingAppBarState,
    velocity: Float,
    flingAnimationSpec: DecayAnimationSpec<Float>?,
    snapAnimationSpec: AnimationSpec<Float>?,
    snapCollapseThreshold: Float = 0.3f,
    snapExpandThreshold: Float = 0.7f,
): Velocity {
    val partialLimit = state.heightOffsetLimit + state.partialHeightLimit
    var remainingVelocity = velocity

    // Check if the app bar is completely collapsed/expanded. If so, no need to settle the app bar,
    // and just return Zero Velocity.
    // Note that we don't check for 0f due to float precision with the collapsedFraction
    // calculation.
    if (state.collapsedFraction < 0.01f || state.collapsedFraction == 1f) {
        return Velocity.Zero
    }

    // Fling
    if (flingAnimationSpec != null && velocity.absoluteValue > 1f) {
        var lastValue = 0f
        AnimationState(
            initialValue = 0f,
            initialVelocity = velocity,
        ).animateDecay(flingAnimationSpec) {
            val delta = value - lastValue
            val initialHeightOffset = state.heightOffset

            val availableDelta = partialLimit - initialHeightOffset

            state.heightOffset = if (delta < 0f && initialHeightOffset > partialLimit) {
                (initialHeightOffset + delta).coerceAtLeast(partialLimit)
            } else {
                initialHeightOffset + delta
            }

            val consumed = abs(initialHeightOffset - state.heightOffset)
            lastValue = value
            remainingVelocity = this.velocity
            // avoid rounding errors and stop if anything is unconsumed
            if (abs(maxOf(delta, availableDelta) - consumed) > 0.5f) this.cancelAnimation()
        }
    }
    // Snap
    if (snapAnimationSpec != null && state.heightOffset > partialLimit && state.heightOffset < 0f) {
        AnimationState(initialValue = state.heightOffset).animateTo(
            when (state.direction) {
                TopBapScrollDirection.Collapsing -> if (state.collapsedFraction > snapCollapseThreshold) {
                    partialLimit
                } else {
                    0f
                }
                TopBapScrollDirection.Expanding -> if (state.collapsedFraction < snapExpandThreshold) {
                    0f
                } else {
                    partialLimit
                }
                TopBapScrollDirection.Idle -> 0f
            },
            animationSpec = snapAnimationSpec,
        ) {
            state.heightOffset = value
        }
    }
    return Velocity(0f, remainingVelocity)
}