package com.tangem.feature.wallet.presentation.wallet.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.FlingBehavior
import androidx.compose.foundation.gestures.ScrollScope
import androidx.compose.ui.MotionDurationScale
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.withContext
import kotlin.math.abs
import kotlin.math.absoluteValue
import kotlin.math.sign

@ExperimentalFoundationApi
class TangemSnapFlingBehavior(
    private val snapLayoutInfoProvider: SnapLayoutInfoProvider,
    private val lowVelocityAnimationSpec: AnimationSpec<Float>,
    private val highVelocityAnimationSpec: DecayAnimationSpec<Float>,
    private val snapAnimationSpec: AnimationSpec<Float>,
    private val density: Density,
    private val shortSnapVelocityThreshold: Dp = MinFlingVelocityDp,
) : FlingBehavior {

    private val velocityThreshold = with(density) { shortSnapVelocityThreshold.toPx() }
    private var motionScaleDuration = DefaultScrollMotionDurationScale

    override suspend fun ScrollScope.performFling(initialVelocity: Float): Float {
        return performFling(initialVelocity) {}
    }

    /**
     * Perform a snapping fling animation with given velocity and suspend until fling has
     * finished. This will behave the same way as [performFling] except it will report on
     * each remainingOffsetUpdate using the [onSettlingDistanceUpdated] lambda.
     *
     * @param initialVelocity velocity available for fling in the orientation specified in
     * [androidx.compose.foundation.gestures.scrollable] that invoked this method.
     *
     * @param onSettlingDistanceUpdated a lambda that will be called anytime the
     * distance to the settling offset is updated. The settling offset is the final offset where
     * this fling will stop and may change depending on the snapping animation progression.
     *
     * @return remaining velocity after fling operation has ended
     */
    private suspend fun ScrollScope.performFling(
        initialVelocity: Float,
        onSettlingDistanceUpdated: (Float) -> Unit,
    ): Float {
        val (remainingOffset, remainingState) = fling(initialVelocity, onSettlingDistanceUpdated)

        // No remaining offset means we've used everything, no need to propagate velocity. Otherwise
        // we couldn't use everything (probably because we have hit the min/max bounds of the
        // containing layout) we should propagate the offset.
        return if (remainingOffset == 0f) NoVelocity else remainingState.velocity
    }

    private suspend fun ScrollScope.fling(
        initialVelocity: Float,
        onRemainingScrollOffsetUpdate: (Float) -> Unit,
    ): AnimationResult<Float, AnimationVector1D> {
        // If snapping from scroll (short snap) or fling (long snap)
        val result = withContext(motionScaleDuration) {
            if (abs(initialVelocity) <= abs(velocityThreshold)) {
                shortSnap(initialVelocity, onRemainingScrollOffsetUpdate)
            } else {
                longSnap(initialVelocity, onRemainingScrollOffsetUpdate)
            }
        }

        onRemainingScrollOffsetUpdate(0f) // Animation finished or was cancelled
        return result
    }

    private suspend fun ScrollScope.shortSnap(
        velocity: Float,
        onRemainingScrollOffsetUpdate: (Float) -> Unit,
    ): AnimationResult<Float, AnimationVector1D> {
        val closestOffset = with(snapLayoutInfoProvider) {
            density.calculateSnappingOffset(0f)
        }

        var remainingScrollOffset = closestOffset

        val animationState = AnimationState(NoDistance, velocity)
        return animateSnap(
            closestOffset,
            closestOffset,
            animationState,
            snapAnimationSpec,
        ) { delta ->
            remainingScrollOffset -= delta
            onRemainingScrollOffsetUpdate(remainingScrollOffset)
        }
    }

    private suspend fun ScrollScope.longSnap(
        initialVelocity: Float,
        onAnimationStep: (remainingScrollOffset: Float) -> Unit,
    ): AnimationResult<Float, AnimationVector1D> {
        val initialOffset =
            with(snapLayoutInfoProvider) { density.calculateApproachOffset(initialVelocity) }.let {
                abs(it) * sign(initialVelocity) // ensure offset sign is correct
            }
        var remainingScrollOffset = initialOffset

        onAnimationStep(remainingScrollOffset) // First Scroll Offset

        val (remainingOffset, animationState) = runApproach(
            initialOffset,
            initialVelocity,
        ) { delta ->
            remainingScrollOffset -= delta
            onAnimationStep(remainingScrollOffset)
        }

        remainingScrollOffset = remainingOffset

        return animateSnap(
            remainingOffset,
            remainingOffset,
            animationState.copy(value = 0f),
            snapAnimationSpec,
        ) { delta ->
            remainingScrollOffset -= delta
            onAnimationStep(remainingScrollOffset)
        }
    }

    private suspend fun ScrollScope.runApproach(
        initialTargetOffset: Float,
        initialVelocity: Float,
        onAnimationStep: (delta: Float) -> Unit,
    ): AnimationResult<Float, AnimationVector1D> {
        val animation =
            if (isDecayApproachPossible(offset = initialTargetOffset, velocity = initialVelocity)) {
                HighVelocityApproachAnimation(highVelocityAnimationSpec)
            } else {
                LowVelocityApproachAnimation(
                    lowVelocityAnimationSpec,
                    snapLayoutInfoProvider,
                    density,
                )
            }

        return approach(
            initialTargetOffset,
            initialVelocity,
            animation,
            snapLayoutInfoProvider,
            density,
            onAnimationStep,
        )
    }

    /**
     * If we can approach the target and still have velocity left
     */
    private fun isDecayApproachPossible(offset: Float, velocity: Float): Boolean {
        val decayOffset = highVelocityAnimationSpec.calculateTargetValue(NoDistance, velocity)
        val snapStepSize = with(snapLayoutInfoProvider) { density.calculateSnapStepSize() }
        return decayOffset.absoluteValue >= offset.absoluteValue + snapStepSize
    }

    override fun equals(other: Any?): Boolean {
        return if (other is TangemSnapFlingBehavior) {
            other.snapAnimationSpec == this.snapAnimationSpec &&
                other.highVelocityAnimationSpec == this.highVelocityAnimationSpec &&
                other.lowVelocityAnimationSpec == this.lowVelocityAnimationSpec &&
                other.snapLayoutInfoProvider == this.snapLayoutInfoProvider &&
                other.density == this.density &&
                other.shortSnapVelocityThreshold == this.shortSnapVelocityThreshold
        } else {
            false
        }
    }

    override fun hashCode(): Int = 0
        .let { 31 * it + snapAnimationSpec.hashCode() }
        .let { 31 * it + highVelocityAnimationSpec.hashCode() }
        .let { 31 * it + lowVelocityAnimationSpec.hashCode() }
        .let { 31 * it + snapLayoutInfoProvider.hashCode() }
        .let { 31 * it + density.hashCode() }
        .let { 31 * it + shortSnapVelocityThreshold.hashCode() }
}

@Suppress("LongParameterList")
@OptIn(ExperimentalFoundationApi::class)
private suspend fun ScrollScope.approach(
    initialTargetOffset: Float,
    initialVelocity: Float,
    animation: ApproachAnimation<Float, AnimationVector1D>,
    snapLayoutInfoProvider: SnapLayoutInfoProvider,
    density: Density,
    onAnimationStep: (delta: Float) -> Unit,
): AnimationResult<Float, AnimationVector1D> {
    val (_, currentAnimationState) = animation.approachAnimation(
        this,
        initialTargetOffset,
        initialVelocity,
        onAnimationStep,
    )

    val remainingOffset = with(snapLayoutInfoProvider) {
        density.calculateSnappingOffset(currentAnimationState.velocity)
    }

    // will snap the remainder
    return AnimationResult(remainingOffset, currentAnimationState)
}

/**
 * Runs a [AnimationSpec] to snap the list into [targetOffset]. Uses [cancelOffset] to stop this
 * animation before it reaches the target.
 *
 * @param targetOffset The final target of this animation
 * @param cancelOffset If we'd like to finish the animation earlier we use this value
 * @param animationState The current animation state for continuation purposes
 * @param snapAnimationSpec The [AnimationSpec] that will drive this animation
 * @param onAnimationStep Called for each new scroll delta emitted by the animation cycle.
 */
@Suppress("MagicNumber")
private suspend fun ScrollScope.animateSnap(
    targetOffset: Float,
    cancelOffset: Float,
    animationState: AnimationState<Float, AnimationVector1D>,
    snapAnimationSpec: AnimationSpec<Float>,
    onAnimationStep: (delta: Float) -> Unit,
): AnimationResult<Float, AnimationVector1D> {
    var consumedUpToNow = 0f
    val initialVelocity = animationState.velocity
    animationState.animateTo(
        targetOffset,
        animationSpec = snapAnimationSpec,
        sequentialAnimation = animationState.velocity != 0f,
    ) {
        val realValue = value.coerceToTarget(cancelOffset)
        val delta = realValue - consumedUpToNow
        val consumed = scrollBy(delta)
        onAnimationStep(consumed)
        // stop when unconsumed or when we reach the desired value
        if (abs(delta - consumed) > 0.5f || realValue != value) {
            cancelAnimation()
        }
        consumedUpToNow += consumed
    }

    // Always course correct velocity so they don't become too large.
    val finalVelocity = animationState.velocity.coerceToTarget(initialVelocity)
    return AnimationResult(
        targetOffset - consumedUpToNow,
        animationState.copy(velocity = finalVelocity),
    )
}

private class HighVelocityApproachAnimation(
    private val decayAnimationSpec: DecayAnimationSpec<Float>,
) : ApproachAnimation<Float, AnimationVector1D> {
    override suspend fun approachAnimation(
        scope: ScrollScope,
        offset: Float,
        velocity: Float,
        onAnimationStep: (delta: Float) -> Unit,
    ): AnimationResult<Float, AnimationVector1D> {
        val animationState = AnimationState(initialValue = 0f, initialVelocity = velocity)
        return with(scope) {
            animateDecay(offset, animationState, decayAnimationSpec, onAnimationStep)
        }
    }
}

private class LowVelocityApproachAnimation @OptIn(ExperimentalFoundationApi::class) constructor(
    private val lowVelocityAnimationSpec: AnimationSpec<Float>,
    private val layoutInfoProvider: SnapLayoutInfoProvider,
    private val density: Density,
) : ApproachAnimation<Float, AnimationVector1D> {
    @OptIn(ExperimentalFoundationApi::class)
    override suspend fun approachAnimation(
        scope: ScrollScope,
        offset: Float,
        velocity: Float,
        onAnimationStep: (delta: Float) -> Unit,
    ): AnimationResult<Float, AnimationVector1D> {
        val animationState = AnimationState(initialValue = 0f, initialVelocity = velocity)
        val targetOffset =
            (abs(offset) + with(layoutInfoProvider) { density.calculateSnapStepSize() }) * sign(
                velocity,
            )
        return with(scope) {
            animateSnap(
                targetOffset = targetOffset,
                cancelOffset = offset,
                animationState = animationState,
                snapAnimationSpec = lowVelocityAnimationSpec,
                onAnimationStep = onAnimationStep,
            )
        }
    }
}

@Suppress("MagicNumber")
private suspend fun ScrollScope.animateDecay(
    targetOffset: Float,
    animationState: AnimationState<Float, AnimationVector1D>,
    decayAnimationSpec: DecayAnimationSpec<Float>,
    onAnimationStep: (delta: Float) -> Unit,
): AnimationResult<Float, AnimationVector1D> {
    var previousValue = 0f

    fun AnimationScope<Float, AnimationVector1D>.consumeDelta(delta: Float) {
        val consumed = scrollBy(delta)
        onAnimationStep(consumed)
        if (abs(delta - consumed) > 0.5f) cancelAnimation()
    }

    animationState.animateDecay(
        animationSpec = decayAnimationSpec,
        sequentialAnimation = animationState.velocity != 0f,
    ) {
        previousValue = if (abs(value) >= abs(targetOffset)) {
            val finalValue = value.coerceToTarget(targetOffset)
            val finalDelta = finalValue - previousValue
            consumeDelta(finalDelta)
            cancelAnimation()
            finalValue
        } else {
            val delta = value - previousValue
            consumeDelta(delta)
            value
        }
    }

    return AnimationResult(
        targetOffset - previousValue,
        animationState,
    )
}

private interface ApproachAnimation<T, V : AnimationVector> {
    suspend fun approachAnimation(
        scope: ScrollScope,
        offset: T,
        velocity: T,
        onAnimationStep: (delta: T) -> Unit,
    ): AnimationResult<T, V>
}

private fun Float.coerceToTarget(target: Float): Float {
    if (target == 0f) return 0f
    return if (target > 0) coerceAtMost(target) else coerceAtLeast(target)
}

private class AnimationResult<T, V : AnimationVector>(
    val remainingOffset: T,
    val currentAnimationState: AnimationState<T, V>,
) {
    operator fun component1(): T = remainingOffset
    operator fun component2(): AnimationState<T, V> = currentAnimationState
}

@Suppress("TopLevelPropertyNaming")
private const val DefaultScrollMotionDurationScaleFactor = 1f

@Suppress("TopLevelPropertyNaming")
val DefaultScrollMotionDurationScale = object : MotionDurationScale {
    override val scaleFactor: Float
        get() = DefaultScrollMotionDurationScaleFactor
}

@Suppress("TopLevelPropertyNaming")
internal val MinFlingVelocityDp = 400.dp

@Suppress("TopLevelPropertyNaming")
internal const val NoDistance = 0f

@Suppress("TopLevelPropertyNaming")
internal const val NoVelocity = 0f

@ExperimentalFoundationApi
interface SnapLayoutInfoProvider {
    /**
     * The minimum offset that snapping will use to animate.(e.g. an item size)
     */
    fun Density.calculateSnapStepSize(): Float

    /**
     * Calculate the distance to navigate before settling into the next snapping bound.
     *
     * @param initialVelocity The current fling movement velocity. You can use this tho calculate a
     * velocity based offset.
     */
    fun Density.calculateApproachOffset(initialVelocity: Float): Float

    /**
     * Given a target placement in a layout, the snapping offset is the next snapping position
     * this layout can be placed in. If this is a short snapping, [currentVelocity] is guaranteed
     * to be 0.If it is a long snapping, this method  will be called
     * after [calculateApproachOffset].
     *
     * @param currentVelocity The current fling movement velocity. This may change throughout the
     * fling animation.
     */
    fun Density.calculateSnappingOffset(currentVelocity: Float): Float
}