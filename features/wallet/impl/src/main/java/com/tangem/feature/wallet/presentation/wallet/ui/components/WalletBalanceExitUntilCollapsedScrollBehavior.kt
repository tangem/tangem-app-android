package com.tangem.feature.wallet.presentation.wallet.ui.components

import androidx.compose.animation.core.*
import androidx.compose.animation.rememberSplineBasedDecay
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.unit.Velocity
import com.tangem.feature.wallet.presentation.wallet.ui.components.WalletBalanceScrollState.Companion.Saver
import timber.log.Timber
import kotlin.math.abs
import kotlin.math.absoluteValue

enum class ScrollDirection {
    Collapsing, Expanding, Idle
}

@Composable
fun rememberWalletBalanceState(
    heightOffsetLimit: Float = -Float.MAX_VALUE,
    partialHeightLimit: Float = -Float.MAX_VALUE,
    initialHeightOffset: Float = 0f,
): WalletBalanceScrollState {
    return rememberSaveable(saver = Saver) {
        WalletBalanceScrollState(
            initialHeightOffset = initialHeightOffset,
            partialHeightLimit = partialHeightLimit,
            heightOffsetLimit = heightOffsetLimit,
        )
    }
}

@Stable
class WalletBalanceScrollState(
    val initialHeightOffset: Float = 0f,
    val partialHeightLimit: Float,
    val heightOffsetLimit: Float = 0f,
) {
    var heightOffset: Float
        get() = _heightOffset.floatValue
        set(newOffset) {
            _heightOffset.floatValue =
                newOffset.coerceIn(minimumValue = heightOffsetLimit, maximumValue = 0f)
        }

    val collapsedFraction: Float
        get() =
            if (heightOffsetLimit != 0f) {
                heightOffset / heightOffsetLimit
            } else {
                0f
            }

    var direction: ScrollDirection = ScrollDirection.Idle

    companion object {
        /** The default [Saver] implementation for [TopAppBarState]. */
        val Saver: Saver<WalletBalanceScrollState, *> =
            listSaver(
                save = { listOf(it.heightOffsetLimit, it.heightOffset, it.partialHeightLimit) },
                restore = {
                    WalletBalanceScrollState(
                        heightOffsetLimit = it[0],
                        partialHeightLimit = it[2],
                        initialHeightOffset = it[1],
                    )
                },
            )
    }

    private var _heightOffset = mutableFloatStateOf(initialHeightOffset)
}

@Stable
data class WalletBalanceScrollBehavior(
    val state: WalletBalanceScrollState,
    val snapAnimationSpec: AnimationSpec<Float>?,
    val flingAnimationSpec: DecayAnimationSpec<Float>?,
    val nestedScrollConnection: NestedScrollConnection,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun customExitUntilCollapsedScrollBehavior(
    state: WalletBalanceScrollState = rememberWalletBalanceState(),
    snapAnimationSpec: AnimationSpec<Float>? = spring(),
    flingAnimationSpec: DecayAnimationSpec<Float>? = rememberSplineBasedDecay(),
): WalletBalanceScrollBehavior {
    val nestedScrollConnection = remember(state) {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (available.y > 0f) {
                    state.direction = ScrollDirection.Expanding
                    return Offset.Zero
                }

                val oldOffset = state.heightOffset

                state.direction = ScrollDirection.Collapsing
                state.heightOffset = (state.heightOffset + available.y).coerceIn(state.heightOffsetLimit, 0f)

                return if (oldOffset != state.heightOffset) {
                    // We're in the middle of top app bar collapse or expand.
                    // Consume only the scroll on the Y axis.
                    available.copy(x = 0f)
                } else {
                    Offset.Zero
                }
            }

            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource,
            ): Offset {
                if (available.y < 0f || consumed.y < 0f) {
                    // When scrolling up, just update the state's height offset.
                    val oldHeightOffset = state.heightOffset
                    state.heightOffset += consumed.y
                    return Offset(0f, state.heightOffset - oldHeightOffset)
                }

                if (available.y > 0f) {
                    // Adjust the height offset in case the consumed delta Y is less than what was
                    // recorded as available delta Y in the pre-scroll.
                    val oldHeightOffset = state.heightOffset
                    state.heightOffset += available.y
                    return Offset(0f, state.heightOffset - oldHeightOffset)
                }
                return Offset.Zero
            }

            override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                val consumed = super.onPostFling(consumed, available)
                return consumed + settleAppBar(
                    state = state,
                    velocity = available.y,
                    flingAnimationSpec = flingAnimationSpec,
                    snapAnimationSpec = snapAnimationSpec,
                )
            }
        }
    }

    return remember(state, nestedScrollConnection, snapAnimationSpec, flingAnimationSpec) {
        WalletBalanceScrollBehavior(
            state = state,
            snapAnimationSpec = snapAnimationSpec,
            flingAnimationSpec = flingAnimationSpec,
            nestedScrollConnection = nestedScrollConnection,
        )
    }
}

suspend fun settleAppBar(
    state: WalletBalanceScrollState,
    velocity: Float,
    flingAnimationSpec: DecayAnimationSpec<Float>?,
    snapAnimationSpec: AnimationSpec<Float>?,
): Velocity {
    val partialLimit = state.heightOffsetLimit + state.partialHeightLimit
    var remainingVelocity = velocity

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

            Timber.d("TOPBAR FLING b: delta=$delta, heightOffset=${state.heightOffset}")
            state.heightOffset = if (delta < 0f && initialHeightOffset > partialLimit) {
                (initialHeightOffset + delta).coerceAtLeast(partialLimit)
            } else {
                initialHeightOffset + delta
            }
            Timber.d("TOPBAR FLING a: delta=$delta, heightOffset=${state.heightOffset}")

            val consumed = abs(initialHeightOffset - state.heightOffset)
            lastValue = value
            remainingVelocity = this.velocity
            // avoid rounding errors and stop if anything is unconsumed
            if (abs(maxOf(delta, availableDelta) - consumed) > 0.5f) this.cancelAnimation()
        }
    }
    // Snap
    if (snapAnimationSpec != null) {
        if (state.heightOffset < 0f && state.heightOffset > partialLimit) {
            AnimationState(initialValue = state.heightOffset).animateTo(
                when (state.direction) {
                    ScrollDirection.Collapsing -> partialLimit
                    ScrollDirection.Expanding -> 0f
                    ScrollDirection.Idle -> 0f
                },
                animationSpec = snapAnimationSpec,
            ) {
                state.heightOffset = value
            }
        }
    }
    return Velocity(0f, remainingVelocity)
}