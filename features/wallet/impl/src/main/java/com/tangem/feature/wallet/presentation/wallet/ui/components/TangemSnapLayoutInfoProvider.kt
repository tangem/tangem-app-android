package com.tangem.feature.wallet.presentation.wallet.ui.components

import androidx.compose.animation.core.DecayAnimationSpec
import androidx.compose.animation.core.calculateTargetValue
import androidx.compose.animation.splineBasedDecay
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.snapping.SnapFlingBehavior
import androidx.compose.foundation.lazy.LazyListLayoutInfo
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.ui.unit.Density
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract
import kotlin.math.abs
import kotlin.math.absoluteValue
import kotlin.math.sign

/**
 * A [SnapLayoutInfoProvider] for LazyLists.
 *
 * @param lazyListState The [LazyListState] with information about the current state of the list
 * @param positionInLayout The desired positioning of the snapped item within the main layout.
 * This position should be considered with regard to the start edge of the item and the placement
 * within the viewport.
 *
 * @return A [SnapLayoutInfoProvider] that can be used with [SnapFlingBehavior]
 */
@Suppress("FunctionNaming")
@ExperimentalFoundationApi
fun TangemSnapLayoutInfoProvider(
    lazyListState: LazyListState,
    positionInLayout: SnapPositionInLayout = SnapPositionInLayout.CenterToCenter,
): SnapLayoutInfoProvider = object : SnapLayoutInfoProvider {

    private val layoutInfo: LazyListLayoutInfo
        get() = lazyListState.layoutInfo

    // Decayed page snapping is the default
    override fun Density.calculateApproachOffset(initialVelocity: Float): Float {
        val decayAnimationSpec: DecayAnimationSpec<Float> = splineBasedDecay(this)
        val offset =
            decayAnimationSpec.calculateTargetValue(NoDistance, initialVelocity).absoluteValue
        val finalDecayOffset = (offset - calculateSnapStepSize()).coerceAtLeast(0f)
        return if (finalDecayOffset == 0f) {
            finalDecayOffset
        } else {
            finalDecayOffset * initialVelocity.sign
        }
    }

    override fun Density.calculateSnappingOffset(currentVelocity: Float): Float {
        var lowerBoundOffset = Float.NEGATIVE_INFINITY
        var upperBoundOffset = Float.POSITIVE_INFINITY

        layoutInfo.visibleItemsInfo.fastForEach { item ->
            val offset =
                calculateDistanceToDesiredSnapPosition(
                    mainAxisViewPortSize = layoutInfo.singleAxisViewportSize,
                    beforeContentPadding = layoutInfo.beforeContentPadding,
                    afterContentPadding = layoutInfo.afterContentPadding,
                    itemSize = item.size,
                    itemOffset = item.offset,
                    itemIndex = item.index,
                    snapPositionInLayout = positionInLayout,
                )

            // Find item that is closest to the center
            if (offset <= 0 && offset > lowerBoundOffset) {
                lowerBoundOffset = offset
            }

            // Find item that is closest to center, but after it
            if (offset >= 0 && offset < upperBoundOffset) {
                upperBoundOffset = offset
            }
        }

        return calculateFinalOffset(currentVelocity, lowerBoundOffset, upperBoundOffset)
    }

    override fun Density.calculateSnapStepSize(): Float = with(layoutInfo) {
        if (visibleItemsInfo.isNotEmpty()) {
            visibleItemsInfo.fastSumBy { it.size } / visibleItemsInfo.size.toFloat()
        } else {
            0f
        }
    }
}

@Suppress("BanInlineOptIn")
@OptIn(ExperimentalContracts::class)
inline fun <T> List<T>.fastSumBy(selector: (T) -> Int): Int {
    contract { callsInPlace(selector) }
    var sum = 0
    fastForEach { element ->
        sum += selector(element)
    }
    return sum
}

internal fun calculateFinalOffset(velocity: Float, lowerBound: Float, upperBound: Float): Float {
    fun Float.isValidDistance(): Boolean {
        return this != Float.POSITIVE_INFINITY && this != Float.NEGATIVE_INFINITY
    }

    val finalDistance = when (sign(velocity)) {
        0f -> {
            if (abs(upperBound) <= abs(lowerBound)) {
                upperBound
            } else {
                lowerBound
            }
        }

        1f -> upperBound
        -1f -> lowerBound
        else -> NoDistance
    }

    return if (finalDistance.isValidDistance()) {
        finalDistance
    } else {
        NoDistance
    }
}

internal val LazyListLayoutInfo.singleAxisViewportSize: Int
    get() = if (orientation == Orientation.Vertical) viewportSize.height else viewportSize.width

@Suppress("BanInlineOptIn")
@OptIn(ExperimentalContracts::class)
inline fun <T> List<T>.fastForEach(action: (T) -> Unit) {
    contract { callsInPlace(action) }
    for (index in indices) {
        val item = get(index)
        action(item)
    }
}

@Suppress("LongParameterList")
@OptIn(ExperimentalFoundationApi::class)
internal fun Density.calculateDistanceToDesiredSnapPosition(
    mainAxisViewPortSize: Int,
    beforeContentPadding: Int,
    afterContentPadding: Int,
    itemSize: Int,
    itemOffset: Int,
    itemIndex: Int,
    snapPositionInLayout: SnapPositionInLayout,
): Float {
    val containerSize = mainAxisViewPortSize - beforeContentPadding - afterContentPadding

    val desiredDistance = with(snapPositionInLayout) {
        position(containerSize, itemSize, itemIndex)
    }.toFloat()

    return itemOffset - desiredDistance
}

@ExperimentalFoundationApi
fun interface SnapPositionInLayout {
    /**
     * Calculates an offset positioning between a container and an element within this container.
     * The offset calculation is the necessary diff that should be applied to the item offset to
     * align the item with a position within the container. As a base line, if we wanted to align
     * the start of the container and the start of the item, we would return 0 in this function.
     */
    fun Density.position(layoutSize: Int, itemSize: Int, itemIndex: Int): Int

    companion object {
        /**
         * Aligns the center of the item with the center of the containing layout.
         */
        val CenterToCenter =
            SnapPositionInLayout { layoutSize, itemSize, _ -> layoutSize / 2 - itemSize / 2 }
    }
}