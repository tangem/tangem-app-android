package com.tangem.core.ui.ds2.shtorka

import androidx.compose.animation.core.animate
import androidx.compose.animation.core.calculateTargetValue
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.foundation.gestures.DraggableState
import androidx.compose.foundation.gestures.animateToWithDecay
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.Velocity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.math.sign

/**
 * State holder of [TangemShtorka]. Owns the anchored-draggable machinery, the velocity-projected
 * settling and the rubber-band overdrag.
 *
 * Behavior notes:
 * - On release the target detent is chosen by projecting the fling velocity with a decay curve and
 *   picking the detent closest to the projected stop point, never more than one detent beyond the
 *   one the finger was released next to — so a flick always lands on the neighbouring detent
 *   instead of flying past it.
 * - Settling animations are interruptible: touching the shtorka mid-flight grabs it in place.
 * - The resting detent is saved and restored, so a shtorka that leaves the composition (navigating
 *   to another screen, a configuration change, process death) comes back at the detent it was left
 *   at rather than at the initial one.
 *
 * Create via [rememberTangemShtorkaState].
 *
 * @property detents detents the shtorka can rest at. See [TangemShtorka.Detent].
 */
@Stable
@OptIn(ExperimentalFoundationApi::class)
class TangemShtorkaState internal constructor(
    val detents: List<TangemShtorka.Detent>,
    initialDetent: TangemShtorka.Detent,
    private val scope: CoroutineScope,
) {

    init {
        require(detents.isNotEmpty()) { "TangemShtorka requires at least one detent" }
        require(initialDetent in detents) { "initialDetent must be one of detents" }
    }

    // The settle target is always chosen by [startSettle]'s velocity projection, never by the
    // state itself, so the default target resolution (closest anchor, no velocity) is fine.
    internal val anchoredDraggableState = AnchoredDraggableState(initialValue = initialDetent)

    internal val draggableState = DraggableState { delta -> dispatchDrag(delta) }

    /**
     * Raw finger travel below the collapsed detent, in pixels (never negative — upward overdrag
     * past full is a dead stop). Rendered through [rubberBandOffset] as a resisted translation.
     */
    internal var rawOverdrag: Float by mutableFloatStateOf(0f)

    /** Expand progress of the floating-card → edge-to-edge morph, written during layout. */
    internal var expandProgress: Float by mutableFloatStateOf(0f)

    /** 1 at the collapsed detent, 0 at the next one up; drives the capsule corners. */
    internal var collapseProgress: Float = 0f

    /**
     * Scale the chrome slot renders at, written during layout. Follows the card's width ratio to
     * its [TangemShtorka.Detent.Full] width, floored at the floating-inset scale so the chrome's
     * unscaled measure never overhangs the window (haze would truncate its effects there).
     */
    internal var chromeScale: Float = 1f

    /** Scale of the content slot — equals [chromeScale] except on the collapse leg, where it freezes. */
    internal var contentScale: Float = 1f

    /** Size of the content slot in card coordinates, written during layout; freezes on the collapse leg. */
    internal var contentCardSize: IntSize = IntSize.Zero

    /** 1 at the detent above the collapsed one, 0 at the collapsed detent; fades the content slot. */
    internal var contentAlpha: Float = 1f

    /** Whether a settle animation is running — used to grab the shtorka mid-flight. */
    internal var isSettling: Boolean by mutableStateOf(false)
        private set

    private var settleJob: Job? = null

    var skippedDetents: Set<TangemShtorka.Detent> by mutableStateOf(emptySet())
        private set

    /** The detent the shtorka currently rests at (or rested at before an ongoing gesture). */
    val currentDetent: TangemShtorka.Detent
        get() = anchoredDraggableState.currentValue

    /** The detent the shtorka is moving towards; equals [currentDetent] when settled. */
    val targetDetent: TangemShtorka.Detent
        get() = anchoredDraggableState.targetValue

    /**
     * Current top edge of the shtorka in pixels within its parent. Useful for placing companion
     * controls that should ride the shtorka's top edge.
     *
     * @throws IllegalStateException if called before the first layout pass
     */
    fun requireOffset(): Float = anchoredDraggableState.requireOffset()

    fun updateSkippedDetents(skippedDetents: Set<TangemShtorka.Detent>) {
        if (this.skippedDetents == skippedDetents) return
        this.skippedDetents = skippedDetents

        if (targetDetent in skippedDetents) {
            val position = anchoredDraggableState.anchors.positionOf(targetDetent)
            if (position.isNaN()) return

            animateTo(detent = settleableAbove(position) ?: settleableBelow(position) ?: return)
        }
    }

    /** Animate to [detent], cancelling any ongoing settle. [detent] must be one of [detents]. */
    fun animateTo(detent: TangemShtorka.Detent) {
        require(detent in detents) { "detent must be one of detents" }
        launchSettle {
            anchoredDraggableState.animateToWithDecay(
                targetValue = detent,
                velocity = 0f,
                snapAnimationSpec = ShtorkaSettleSpring,
                decayAnimationSpec = ShtorkaMotionDecay,
            )
        }
    }

    /** Consume a direct drag [delta]: move between anchors, spill the remainder into overdrag. */
    internal fun dispatchDrag(delta: Float) {
        cancelSettle()
        if (rawOverdrag != 0f) {
            val newRaw = rawOverdrag + delta
            if (newRaw.sign == rawOverdrag.sign) {
                rawOverdrag = newRaw
            } else {
                // The gesture unwound the overdrag — route the rest back into the anchors.
                rawOverdrag = 0f
                dispatchToAnchors(newRaw)
            }
        } else {
            dispatchToAnchors(delta)
        }
    }

    /** Consume a nested-scroll [delta] — no overdrag, the inner list shows its own overscroll. */
    internal fun dispatchNestedDrag(delta: Float): Float {
        cancelSettle()
        return anchoredDraggableState.dispatchRawDelta(delta)
    }

    /**
     * Settle after a gesture: project [velocity] with [ShtorkaProjectionDecay], spring to the
     * detent nearest to the projected stop — capped at one detent past the release position — and
     * relax the overdrag back to zero.
     */
    internal fun startSettle(velocity: Float) {
        launchSettle {
            if (rawOverdrag != 0f) {
                launch { relaxOverdrag(velocity) }
            }
            val offset = requireOffset()
            val projected = ShtorkaProjectionDecay.calculateTargetValue(offset, velocity)
            val released = settleTargetFor(offset) ?: return@launchSettle
            val flung = settleTargetFor(projected) ?: released
            anchoredDraggableState.animateToWithDecay(
                targetValue = oneDetentFrom(released = released, towards = flung),
                velocity = velocity,
                snapAnimationSpec = ShtorkaSettleSpring,
                decayAnimationSpec = ShtorkaMotionDecay,
            )
        }
    }

    /**
     * Run [block] as the current settle: cancels the previous one, raises [isSettling] for the
     * whole run (children included — the job completes only after e.g. [relaxOverdrag] finishes)
     * and lowers it on completion unless a newer settle has already taken over.
     */
    private fun cancelSettle() {
        settleJob?.cancel()
        settleJob = null
        isSettling = false
    }

    private fun launchSettle(block: suspend CoroutineScope.() -> Unit) {
        settleJob?.cancel()
        val job = scope.launch(block = block)
        settleJob = job
        isSettling = true
        job.invokeOnCompletion {
            if (settleJob === job) isSettling = false
        }
    }

    @Suppress("MagicNumber")
    private fun dispatchToAnchors(delta: Float) {
        val consumed = anchoredDraggableState.dispatchRawDelta(delta)
        val unconsumed = delta - consumed
        // Only downward overdrag rubber-bands; translating the card up would open a gap
        // between its bottom edge and the screen edge.
        if (unconsumed > 0.5f) {
            rawOverdrag += unconsumed
        }
    }

    private fun oneDetentFrom(released: TangemShtorka.Detent, towards: TangemShtorka.Detent): TangemShtorka.Detent {
        if (released == towards) return towards

        val anchors = anchoredDraggableState.anchors
        val ordered = detents
            .filter { it !in skippedDetents && !anchors.positionOf(it).isNaN() }
            .sortedBy { anchors.positionOf(it) }
        val releasedIndex = ordered.indexOf(released)
        val towardsIndex = ordered.indexOf(towards)
        if (releasedIndex < 0 || towardsIndex < 0) return towards

        return ordered[towardsIndex.coerceIn(releasedIndex - 1, releasedIndex + 1)]
    }

    private fun settleTargetFor(projected: Float): TangemShtorka.Detent? {
        val anchors = anchoredDraggableState.anchors
        val closest = anchors.closestAnchor(projected) ?: return null
        if (closest !in skippedDetents) return closest

        val skippedPosition = anchors.positionOf(closest)
        val settledPosition = anchors.positionOf(anchoredDraggableState.settledValue)
        val isExpanding = if (settledPosition.isNaN() || settledPosition == skippedPosition) {
            projected <= skippedPosition
        } else {
            settledPosition > skippedPosition
        }

        return if (isExpanding) {
            settleableAbove(skippedPosition) ?: settleableBelow(skippedPosition)
        } else {
            settleableBelow(skippedPosition) ?: settleableAbove(skippedPosition)
        }
    }

    private fun settleableAbove(position: Float): TangemShtorka.Detent? {
        val anchors = anchoredDraggableState.anchors

        return detents
            .filter { it !in skippedDetents && anchors.positionOf(it) <= position }
            .maxByOrNull { anchors.positionOf(it) }
    }

    private fun settleableBelow(position: Float): TangemShtorka.Detent? {
        val anchors = anchoredDraggableState.anchors

        return detents
            .filter { it !in skippedDetents && anchors.positionOf(it) >= position }
            .minByOrNull { anchors.positionOf(it) }
    }

    private suspend fun relaxOverdrag(velocity: Float) {
        animate(
            initialValue = rawOverdrag,
            targetValue = 0f,
            initialVelocity = velocity,
            animationSpec = ShtorkaOverdragReleaseSpring,
        ) { value, _ -> rawOverdrag = value }
    }
}

/**
 * Create and remember a [TangemShtorkaState], restoring the detent it was last left at.
 *
 * @param detents detents the shtorka can rest at; must be non-empty
 * @param initialDetent the detent the shtorka starts at when there is nothing to restore; must be
 *   one of [detents]
 */
@Composable
fun rememberTangemShtorkaState(
    detents: List<TangemShtorka.Detent>,
    initialDetent: TangemShtorka.Detent = detents.first(),
    skippedDetents: Set<TangemShtorka.Detent> = emptySet(),
): TangemShtorkaState {
    val scope = rememberCoroutineScope()
    val saver = remember(detents, initialDetent, skippedDetents, scope) {
        Saver<TangemShtorkaState, Int>(
            save = { it.detents.indexOf(it.currentDetent) },
            restore = { index ->
                TangemShtorkaState(
                    detents = detents,
                    initialDetent = detents.getOrNull(index)?.takeIf { it !in skippedDetents } ?: initialDetent,
                    scope = scope,
                )
            },
        )
    }
    val state = rememberSaveable(detents, scope, saver = saver) {
        TangemShtorkaState(detents = detents, initialDetent = initialDetent, scope = scope)
    }

    SideEffect { state.updateSkippedDetents(skippedDetents) }

    return state
}

/**
 * Nested-scroll connection implementing the scroll handoff: dragging up on the shtorka's
 * scrollable content first raises the shtorka to its top detent and only then scrolls the content;
 * pulling down from the top of the content lowers the shtorka.
 */
@OptIn(ExperimentalFoundationApi::class)
internal fun TangemShtorkaState.shtorkaNestedScrollConnection(): NestedScrollConnection {
    return object : NestedScrollConnection {

        override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
            val delta = available.y
            return if (delta < 0 && source == NestedScrollSource.UserInput) {
                Offset(x = 0f, y = dispatchNestedDrag(delta))
            } else {
                Offset.Zero
            }
        }

        override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
            return if (source == NestedScrollSource.UserInput) {
                Offset(x = 0f, y = dispatchNestedDrag(available.y))
            } else {
                Offset.Zero
            }
        }

        override suspend fun onPreFling(available: Velocity): Velocity {
            val toFling = available.y
            val minPosition = anchoredDraggableState.anchors.minPosition()
            if (toFling == 0f || requireOffset() <= minPosition) return Velocity.Zero

            startSettle(toFling)

            return Velocity(x = 0f, y = toFling)
        }

        override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
            if (!isSettling) {
                startSettle(available.y)
            }
            return available
        }
    }
}