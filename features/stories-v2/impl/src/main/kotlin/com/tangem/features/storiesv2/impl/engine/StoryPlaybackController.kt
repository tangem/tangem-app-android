package com.tangem.features.storiesv2.impl.engine

import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.features.storiesv2.StoriesV2Result
import com.tangem.features.storiesv2.StoryV2ActionTarget
import com.tangem.features.storiesv2.StoryV2Source
import com.tangem.features.storiesv2.impl.analytics.StoriesV2Events
import com.tangem.features.storiesv2.impl.analytics.StoryV2AdvanceReason
import com.tangem.features.storiesv2.impl.analytics.StoryV2ErrorReason
import com.tangem.features.storiesv2.impl.analytics.StoryV2ErrorStage
import com.tangem.features.storiesv2.impl.analytics.StoryV2ExitReason
import com.tangem.features.storiesv2.impl.analytics.StoryV2FallbackReason
import com.tangem.features.storiesv2.impl.content.StoryV2Action
import com.tangem.features.storiesv2.impl.content.StoryV2Asset
import com.tangem.features.storiesv2.impl.content.StoryV2Composition
import com.tangem.features.storiesv2.impl.content.StoryV2EndBehavior
import com.tangem.features.storiesv2.impl.content.StoryV2Slide
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

/**
 * Everything the story does that is not drawing or decoding: which slide is current, when it ends, what a tap means,
 * when a haptic fires, what analytics sees and how the story finishes.
 *
 * Plain Kotlin with no player and no Compose, so the rules that are easy to get subtly wrong — tap back on the first
 * slide, a loop re-reporting slides it already reported, watched time counting a pause — are directly testable.
 */
/**
 * How long a slide may run without its position moving before it stops waiting for the asset and continues on the
 * poster. Long enough to outlast a rebuffer, short enough that no one sits on a frozen bar.
 */
internal const val STALL_TIMEOUT_MS = 2_000L

@Suppress("TooManyFunctions")
internal class StoryPlaybackController(
    private val composition: StoryV2Composition,
    private val source: StoryV2Source,
    private val analytics: AnalyticsEventHandler,
    private val onHaptic: () -> Unit,
    private val onResult: (StoriesV2Result) -> Unit,
) {

    val state: StateFlow<StoryPlaybackState>
        field = MutableStateFlow(
            StoryPlaybackState(
                slideIndex = 0,
                playToken = 0,
                isManualEntry = false,
                isPlayerDrivenEntry = false,
                isHeld = false,
                isDragging = false,
                isInForeground = true,
                isFinished = false,
                renderMode = SlideRenderMode.ASSET,
            ),
        )

    val currentSlide: StoryV2Slide get() = composition.slides[state.value.slideIndex]

    private val viewedSlideIds = mutableSetOf<String>()
    private val fallbackSlideIds = mutableSetOf<String>()

    private var isReducedMotion = false
    private var loopsWatched = 0
    private var isStoryCompleted = false
    private var isOpened = false

    private var slideWatchedMs = 0L
    private var slidePositionMs = 0L
    private var slideAtEndForMs = 0L
    private var isRepeatAutoView = false
    private var firedHapticCount = 0
    private var slideFallbackReason: StoryV2FallbackReason? = null

    /**
     * Not a failure: animated assets are shown as their poster, the viewer keeps the full story, and the fallback
     * is reported under its own reason so it stays out of the technical guardrail.
     */
    fun setReducedMotion(enabled: Boolean) {
        isReducedMotion = enabled
        if (enabled) degradeToPoster(StoryV2FallbackReason.REDUCE_MOTION)
    }

    /** Safe to call more than once. */
    fun onOpened() {
        if (isOpened) return
        isOpened = true

        analytics.send(
            StoriesV2Events.StoryOpened(
                storyId = composition.id,
                storyVersion = composition.version,
                slidesTotal = composition.slides.size,
                contentOrigin = composition.origin.analyticsValue,
                source = source.analyticsValue,
            ),
        )
        enterSlide(index = 0, isManual = false)
    }

    /**
     * @param elapsedMs    wall time since the previous frame, already zeroed while held or backgrounded, so watched
     *                     time never counts a paused story.
     * @param stalledForMs how long the position has stood still, as reported by the slide's clock.
     */
    fun onProgress(positionMs: Long, elapsedMs: Long, stalledForMs: Long = 0L) {
        if (state.value.isFinished) return

        slidePositionMs = positionMs
        slideWatchedMs += elapsedMs

        val isRunning = state.value.isRunning
        // Taken before the watchdog can hand the timeline over to the wall clock: within one frame the slide must
        // either wait for the player or time itself out, never both.
        val isPlayerOwned = isDrivenByPlayer()

        if (isRunning) {
            fireHapticsUpTo(positionMs)
            updateStall(stalledForMs = stalledForMs)
        }

        val durationMs = currentSlide.asset.durationMs
        if (positionMs < durationMs) {
            slideAtEndForMs = 0L
            return
        }

        if (isPlayerOwned) {
            // A video slide normally ends because the player moved on; this is only the guard for a renderer that
            // reaches the end and never reports it.
            slideAtEndForMs += elapsedMs
            if (slideAtEndForMs > PLAYER_ADVANCE_GRACE_MS) advance(StoryV2AdvanceReason.AUTO)
        } else {
            advance(StoryV2AdvanceReason.AUTO)
        }
    }

    fun onPlayerAdvancedTo(slideIndex: Int) {
        if (state.value.isFinished) return
        if (slideIndex != state.value.slideIndex + 1) return

        leaveSlide(StoryV2AdvanceReason.AUTO)
        enterSlide(index = slideIndex, isManual = false, isPlayerDriven = true)
    }

    /** Only meaningful when the last item of the playlist belongs to the current slide. */
    fun onPlayerEnded() {
        if (state.value.isFinished || !isDrivenByPlayer()) return
        advance(StoryV2AdvanceReason.AUTO)
    }

    /** The story continues on the poster rather than showing an error: an empty screen is never acceptable. */
    fun onPlaybackFailed() {
        if (state.value.isFinished) return

        reportAssetFailure(StoryV2ErrorReason.DECODE_FAILURE)
        degradeToPoster(StoryV2FallbackReason.PLAYBACK_FAILED)
    }

    fun onTapForward() {
        if (state.value.isFinished) return
        advance(StoryV2AdvanceReason.TAP_FORWARD)
    }

    /**
     * On the first slide there is no previous one, so it restarts — including in a loop, where jumping to the last
     * slide would read as going forward.
     */
    fun onTapBack() {
        if (state.value.isFinished) return

        leaveSlide(StoryV2AdvanceReason.TAP_BACK)
        enterSlide(index = (state.value.slideIndex - 1).coerceAtLeast(0), isManual = true)
    }

    fun setHeld(held: Boolean) {
        state.update { it.copy(isHeld = held) }
    }

    /**
     * Tracked apart from [setHeld] because the two gestures overlap: a hold that turns into a drag would otherwise
     * have one detector clear what the other set.
     */
    fun setDragging(dragging: Boolean) {
        state.update { it.copy(isDragging = dragging) }
    }

    fun setInForeground(inForeground: Boolean) {
        state.update { it.copy(isInForeground = inForeground) }
    }

    fun onCloseClicked() = finish(StoryV2ExitReason.CLOSE_BUTTON, StoriesV2Result.Dismissed)

    fun onSwipedDown() = finish(StoryV2ExitReason.SWIPE_DOWN, StoriesV2Result.Dismissed)

    fun onActionClicked(action: StoryV2Action) {
        if (state.value.isFinished) return

        val result = when (val target = action.target) {
            is StoryV2Action.Target.Screen -> StoriesV2Result.ActionInvoked(
                actionId = action.id,
                target = StoryV2ActionTarget.Screen(key = target.key),
            )
            is StoryV2Action.Target.Web -> StoriesV2Result.ActionInvoked(
                actionId = action.id,
                target = StoryV2ActionTarget.Web(url = target.url),
            )
            is StoryV2Action.Target.Deeplink -> StoriesV2Result.ActionInvoked(
                actionId = action.id,
                target = StoryV2ActionTarget.Deeplink(uri = target.uri),
            )
            StoryV2Action.Target.Close -> StoriesV2Result.Dismissed
        }
        finish(reason = StoryV2ExitReason.ACTION, result = result, action = action)
    }

    private fun advance(reason: StoryV2AdvanceReason) {
        val next = state.value.slideIndex + 1

        when {
            next < composition.slides.size -> {
                leaveSlide(reason)
                enterSlide(index = next, isManual = reason == StoryV2AdvanceReason.TAP_FORWARD)
            }
            composition.endBehavior == StoryV2EndBehavior.LOOP -> {
                isStoryCompleted = true
                loopsWatched++
                leaveSlide(if (reason == StoryV2AdvanceReason.AUTO) StoryV2AdvanceReason.LOOP_RESTART else reason)
                enterSlide(index = 0, isManual = reason == StoryV2AdvanceReason.TAP_FORWARD)
            }
            else -> {
                isStoryCompleted = true
                finish(StoryV2ExitReason.COMPLETED, StoriesV2Result.Completed, advanceReason = reason)
            }
        }
    }

    private fun enterSlide(index: Int, isManual: Boolean, isPlayerDriven: Boolean = false) {
        val slide = composition.slides[index]
        val isFirstView = viewedSlideIds.add(slide.id)

        // An automatic loop round replays slides already reported: repeating their haptics and Slide Viewed would
        // double-count a story nobody interacted with. A manual return is a genuine new view.
        isRepeatAutoView = !isManual && !isFirstView

        slideWatchedMs = 0L
        slidePositionMs = 0L
        slideAtEndForMs = 0L
        firedHapticCount = 0
        slideFallbackReason = if (isReducedMotion && slide.asset !is StoryV2Asset.Image) {
            StoryV2FallbackReason.REDUCE_MOTION
        } else {
            null
        }
        if (slideFallbackReason != null) fallbackSlideIds.add(slide.id)

        state.update { previous ->
            previous.copy(
                slideIndex = index,
                playToken = previous.playToken + 1,
                isManualEntry = isManual,
                isPlayerDrivenEntry = isPlayerDriven,
                renderMode = if (slideFallbackReason == null) SlideRenderMode.ASSET else SlideRenderMode.POSTER,
            )
        }
    }

    private fun leaveSlide(reason: StoryV2AdvanceReason, isStoryEnding: Boolean = false) {
        // A repeat of an automatic round is not a new view, but leaving it by hand or ending the story on it is:
        // deciding this by how the slide was entered loses the watched time of whatever slide the viewer quit on.
        val isAutomaticLeave = reason == StoryV2AdvanceReason.AUTO || reason == StoryV2AdvanceReason.LOOP_RESTART
        if (isRepeatAutoView && isAutomaticLeave && !isStoryEnding) return

        val slide = currentSlide
        analytics.send(
            StoriesV2Events.SlideViewed(
                storyId = composition.id,
                storyVersion = composition.version,
                slideId = slide.id,
                slideIndex = state.value.slideIndex + 1,
                assetType = slide.asset.analyticsType,
                assetDurationMs = slide.asset.durationMs,
                watchedMs = slideWatchedMs,
                isSlideCompleted = slidePositionMs >= slide.asset.durationMs,
                advanceReason = reason.analyticsValue,
                fallbackReason = slideFallbackReason?.analyticsValue,
                source = source.analyticsValue,
            ),
        )
    }

    private fun finish(
        reason: StoryV2ExitReason,
        result: StoriesV2Result,
        action: StoryV2Action? = null,
        advanceReason: StoryV2AdvanceReason = StoryV2AdvanceReason.STORY_CLOSED,
    ) {
        if (state.value.isFinished) return

        leaveSlide(reason = advanceReason, isStoryEnding = true)
        state.update { it.copy(isFinished = true) }

        analytics.send(
            StoriesV2Events.StoryClosed(
                storyId = composition.id,
                storyVersion = composition.version,
                slidesTotal = composition.slides.size,
                watched = viewedSlideIds.size,
                loopsWatched = loopsWatched,
                isStoryCompleted = isStoryCompleted,
                exitReason = reason.analyticsValue,
                actionId = action?.id,
                actionTargetType = action?.target?.analyticsType,
                actionDestination = action?.target?.analyticsDestination,
                fallbackSlides = fallbackSlideIds.size,
                source = source.analyticsValue,
            ),
        )
        onResult(result)
    }

    private fun degradeToPoster(reason: StoryV2FallbackReason) {
        if (currentSlide.asset is StoryV2Asset.Image) return
        // The first reason is the true one. A slide already on its poster because the viewer turned animations off
        // must not be re-labelled a decode failure by whatever the silenced decoder reports afterwards.
        if (state.value.renderMode == SlideRenderMode.POSTER) return

        slideFallbackReason = reason
        fallbackSlideIds.add(currentSlide.id)
        state.update { it.copy(renderMode = SlideRenderMode.POSTER) }
    }

    private fun fireHapticsUpTo(positionMs: Long) {
        val markers = currentSlide.hapticAtMs
        if (isRepeatAutoView || firedHapticCount >= markers.size) return

        while (firedHapticCount < markers.size && markers[firedHapticCount] <= positionMs) {
            firedHapticCount++
            onHaptic()
        }
    }

    /**
     * A stall that no error reports would otherwise freeze the story with no way out but a tap. The slide falls
     * back to its poster and the composition times it from there, from the position it had reached.
     */
    private fun updateStall(stalledForMs: Long) {
        if (stalledForMs <= STALL_TIMEOUT_MS) return
        if (state.value.renderMode == SlideRenderMode.POSTER) return

        reportAssetFailure(StoryV2ErrorReason.TIMEOUT)
        degradeToPoster(StoryV2FallbackReason.ASSET_NOT_READY)
    }

    /** Reported once per slide per story: a looping story would otherwise re-report the same broken file forever. */
    private fun reportAssetFailure(reason: StoryV2ErrorReason) {
        if (currentSlide.id in fallbackSlideIds) return

        analytics.send(
            StoriesV2Events.StoryErrorOccurred(
                storyId = composition.id,
                slideId = currentSlide.id,
                errorStage = StoryV2ErrorStage.ASSET_PLAYBACK.analyticsValue,
                errorReason = reason.analyticsValue,
                source = source.analyticsValue,
            ),
        )
    }

    private fun isDrivenByPlayer(): Boolean {
        return state.value.renderMode == SlideRenderMode.ASSET && currentSlide.asset is StoryV2Asset.Video
    }

    private companion object {
        const val PLAYER_ADVANCE_GRACE_MS = 250L
    }
}

/**
 * @property playToken           bumped on every slide entry, including a restart of the same slide, so the renderer
 *                               knows to start over when the index alone did not change
 * @property isManualEntry       the viewer opened this slide themselves, which makes it a new view
 * @property isPlayerDrivenEntry the only entry the renderer must not touch. Every other one restarts the asset,
 *                               including a loop back onto the very item the player is parked on
 */
internal data class StoryPlaybackState(
    val slideIndex: Int,
    val playToken: Int,
    val isManualEntry: Boolean,
    val isPlayerDrivenEntry: Boolean,
    val isHeld: Boolean,
    val isDragging: Boolean,
    val isInForeground: Boolean,
    val isFinished: Boolean,
    val renderMode: SlideRenderMode,
) {

    val isRunning: Boolean get() = !isHeld && !isDragging && isInForeground && !isFinished
}

internal enum class SlideRenderMode {

    ASSET,

    /** Timed by the composition rather than by the asset. */
    POSTER,
}

private val StoryV2Asset.analyticsType: String
    get() = when (this) {
        is StoryV2Asset.Video -> "video"
        is StoryV2Asset.Image -> "image"
        is StoryV2Asset.VectorAnimation -> "lottie"
    }

private val StoryV2Action.Target.analyticsType: String
    get() = when (this) {
        is StoryV2Action.Target.Screen -> "screen"
        is StoryV2Action.Target.Web -> "web"
        is StoryV2Action.Target.Deeplink -> "deeplink"
        StoryV2Action.Target.Close -> "close"
    }

/** Web destinations are reported as the host alone: the path and the query can carry campaign data. */
private val StoryV2Action.Target.analyticsDestination: String?
    get() = when (this) {
        is StoryV2Action.Target.Screen -> key
        is StoryV2Action.Target.Web -> url.substringAfter("://").substringBefore('/').substringBefore('?')
        is StoryV2Action.Target.Deeplink -> uri
        StoryV2Action.Target.Close -> null
    }