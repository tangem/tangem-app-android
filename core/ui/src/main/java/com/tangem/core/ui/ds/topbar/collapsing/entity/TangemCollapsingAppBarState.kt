package com.tangem.core.ui.ds.topbar.collapsing.entity

import androidx.compose.animation.core.AnimationState
import androidx.compose.animation.core.animateTo
import androidx.compose.animation.core.tween
import androidx.compose.foundation.MutatePriority
import androidx.compose.foundation.gestures.ScrollScope
import androidx.compose.foundation.gestures.ScrollableState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import com.tangem.core.ui.ds.topbar.collapsing.entity.TangemCollapsingAppBarState.Companion.Saver
import kotlin.math.absoluteValue
import kotlin.math.max
import kotlin.math.min

/**
 * State of the collapsing top app bar.
 * It contains the current height offset, the limits for collapsing and expanding, and the scroll direction.
 *
 * @property initialHeightOffset The initial height offset of the app bar. Default is 0f.
 * @property heightOffsetLimit The height offset limit for full collapse.
 * @property partialHeightLimit The height offset limit for partial collapse. Default is the same as [heightOffsetLimit]
 */
@Stable
class TangemCollapsingAppBarState(
    val initialHeightOffset: Float = 0f,
    val heightOffsetLimit: Float = 0f,
    val partialHeightLimit: Float = heightOffsetLimit,
) : ScrollableState {

    private val _heightOffset = mutableFloatStateOf(initialHeightOffset)
    private var deferredConsumption: Float = 0f

    /**
     * The current height offset of the app bar.
     * This value is updated as the user scrolls, and is constrained between [heightOffsetLimit] and 0f.
     */
    var heightOffset: Float
        get() = _heightOffset.floatValue
        set(newOffset) {
            _heightOffset.floatValue =
                newOffset.coerceIn(minimumValue = heightOffsetLimit, maximumValue = 0f)
        }

    /**
     * The fraction of the app bar that is collapsed, calculated as the ratio of [heightOffset] to [heightOffsetLimit].
     */
    val collapsedFraction: Float
        get() =
            if (heightOffsetLimit != 0f) {
                heightOffset / heightOffsetLimit
            } else {
                0f
            }

    /**
     * The current scroll direction of the app bar, which can be Collapsing, Expanding, or Idle.
     */
    var direction: TopBapScrollDirection = TopBapScrollDirection.Idle

    private val scrollableState = ScrollableState { value ->
        val consume = if (value < 0) {
            max(heightOffsetLimit - heightOffset, value)
        } else {
            min(0f - heightOffset, value)
        }

        val current = consume + deferredConsumption
        val currentInt = current.toInt()

        if (current.absoluteValue > 0) {
            heightOffset += currentInt
            deferredConsumption = current - currentInt
        }

        consume
    }

    override val isScrollInProgress: Boolean
        get() = scrollableState.isScrollInProgress

    /**
     *
     */
    suspend fun collapse() {
        AnimationState(initialValue = heightOffset).animateTo(
            targetValue = heightOffsetLimit + partialHeightLimit,
            animationSpec = tween(),
        ) {
            heightOffset = value
        }
    }

    override suspend fun scroll(scrollPriority: MutatePriority, block: suspend ScrollScope.() -> Unit) =
        scrollableState.scroll(scrollPriority, block)

    override fun dispatchRawDelta(delta: Float) = scrollableState.dispatchRawDelta(delta)

    companion object {
        /** The default [Saver] implementation for [TangemCollapsingAppBarState]. */
        val Saver: Saver<TangemCollapsingAppBarState, *> =
            listSaver(
                save = { state -> listOf(state.heightOffsetLimit, state.heightOffset, state.partialHeightLimit) },
                restore = { state ->
                    TangemCollapsingAppBarState(
                        heightOffsetLimit = state[0],
                        partialHeightLimit = state[2],
                        initialHeightOffset = state[1],
                    )
                },
            )
    }
}

/**
 * Remembers and saves the state of the collapsing top app bar across recompositions and configuration changes.
 */
@Composable
fun rememberTangemCollapsingAppBarState(
    heightOffsetLimit: Float = -Float.MAX_VALUE,
    partialHeightLimit: Float = -Float.MAX_VALUE,
    initialHeightOffset: Float = 0f,
): TangemCollapsingAppBarState {
    return rememberSaveable(saver = Saver) {
        TangemCollapsingAppBarState(
            initialHeightOffset = initialHeightOffset,
            partialHeightLimit = partialHeightLimit,
            heightOffsetLimit = heightOffsetLimit,
        )
    }
}

/**
 * The scroll direction of the top app bar, which can be Collapsing, Expanding, or Idle.
 */
enum class TopBapScrollDirection {
    Collapsing, Expanding, Idle
}