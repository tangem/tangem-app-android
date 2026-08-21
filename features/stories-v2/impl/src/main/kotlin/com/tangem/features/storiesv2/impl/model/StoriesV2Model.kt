package com.tangem.features.storiesv2.impl.model

import android.content.Context
import android.provider.Settings
import android.view.SurfaceView
import androidx.compose.runtime.LongState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableLongStateOf
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.ui.haptic.TangemHapticEffect
import com.tangem.core.ui.haptic.VibratorHapticManager
import com.tangem.features.storiesv2.StoriesV2Component
import com.tangem.features.storiesv2.StoriesV2Result
import com.tangem.features.storiesv2.impl.analytics.StoriesV2Events
import com.tangem.features.storiesv2.impl.analytics.StoryV2ErrorReason
import com.tangem.features.storiesv2.impl.analytics.StoryV2ErrorStage
import com.tangem.features.storiesv2.impl.content.StoryV2Action
import com.tangem.features.storiesv2.impl.content.StoryV2Asset
import com.tangem.features.storiesv2.impl.content.StoryV2Composition
import com.tangem.features.storiesv2.impl.content.StoryV2ContentRepository
import com.tangem.features.storiesv2.impl.content.StoryV2Slide
import com.tangem.features.storiesv2.impl.engine.MAX_FRAME_STEP_NANOS
import com.tangem.features.storiesv2.impl.engine.NANOS_PER_MS
import com.tangem.features.storiesv2.impl.engine.SlideClock
import com.tangem.features.storiesv2.impl.engine.SlideRenderMode
import com.tangem.features.storiesv2.impl.engine.StoryPlaybackController
import com.tangem.features.storiesv2.impl.engine.StoryPlaybackState
import com.tangem.features.storiesv2.impl.engine.StoryVideoPlayer
import com.tangem.features.storiesv2.impl.engine.VideoSlideClock
import com.tangem.features.storiesv2.impl.engine.WallSlideClock
import com.tangem.features.storiesv2.impl.ui.StoryPlayerHandle
import com.tangem.features.storiesv2.impl.ui.state.StoriesV2UM
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Wires the three halves of the player together: the rules ([StoryPlaybackController]), the pixels
 * ([StoryVideoPlayer]) and the timeline ([SlideClock]).
 *
 * The important part is what it does *not* do on an automatic slide change: the reaction is a no-op on the player,
 * only the clock and the texts are swapped. A seek, a re-prepare or a poster here would put a visible seam into a
 * story whose slides were authored to run into each other frame for frame.
 */
@Stable
@ModelScoped
@Suppress("LongParameterList", "TooManyFunctions")
internal class StoriesV2Model @Inject constructor(
    paramsContainer: ParamsContainer,
    override val dispatchers: CoroutineDispatcherProvider,
    @ApplicationContext private val context: Context,
    private val contentRepository: StoryV2ContentRepository,
    private val analytics: AnalyticsEventHandler,
    private val hapticManager: VibratorHapticManager,
) : Model(), StoryPlayerHandle, StoryVideoPlayer.Listener {

    private val params = paramsContainer.require<StoriesV2Component.Params>()

    val uiState: StateFlow<StoriesV2UM?>
        field = MutableStateFlow<StoriesV2UM?>(null)

    override val slidePositionMs: LongState
        field = mutableLongStateOf(0L)

    /** Only needed to letterbox a `contain` video; `cover` is cropped by the decoder. */
    val videoAspectRatio: StateFlow<Float?>
        field = MutableStateFlow<Float?>(null)

    private var composition: StoryV2Composition? = null
    private var controller: StoryPlaybackController? = null
    private var videoPlayer: StoryVideoPlayer? = null

    override val hasVideo: Boolean get() = videoPlayer != null

    private var clock: SlideClock = WallSlideClock(durationMs = 0L)
    private var previousFrameNanos = NO_TIME
    private var elapsedRemainderNanos = 0L

    private var appliedPlayToken = NO_TOKEN
    private var appliedRenderMode = SlideRenderMode.ASSET

    init {
        modelScope.launch { open() }
    }

    override fun onDestroy() {
        videoPlayer?.release()
        videoPlayer = null
        super.onDestroy()
    }

    fun setInForeground(inForeground: Boolean) {
        controller?.setInForeground(inForeground)
    }

    /** System back leaves the story the same way the close button does. */
    fun onBackClick() {
        controller?.onCloseClicked()
    }

    override fun attachSurface(surfaceView: SurfaceView) {
        videoPlayer?.attachSurface(surfaceView)
    }

    override fun detachSurface(surfaceView: SurfaceView) {
        videoPlayer?.detachSurface(surfaceView)
    }

    override fun onFrame(frameTimeNanos: Long) {
        val controller = controller ?: return
        val state = controller.state.value
        if (state.isFinished) return

        val isRunning = state.isRunning
        val elapsedMs = elapsedMs(frameTimeNanos = frameTimeNanos, isRunning = isRunning)
        val positionMs = clock.onFrame(frameTimeNanos = frameTimeNanos, isRunning = isRunning)
        slidePositionMs.longValue = positionMs

        controller.onProgress(positionMs = positionMs, elapsedMs = elapsedMs, stalledForMs = clock.stalledForMs)
    }

    override fun onAutoAdvancedTo(slideIndex: Int) {
        controller?.onPlayerAdvancedTo(slideIndex)
    }

    override fun onEnded() {
        controller?.onPlayerEnded()
    }

    override fun onFirstFrameRendered() {
        uiState.update { it?.copy(slide = it.slide.copy(isAssetReady = true)) }
    }

    override fun onAspectRatioChanged(aspectRatio: Float) {
        videoAspectRatio.value = aspectRatio
    }

    override fun onError() {
        controller?.onPlaybackFailed()
    }

    private suspend fun open() {
        val composition = contentRepository.getComposition(params.type)
        if (composition == null) {
            analytics.send(
                StoriesV2Events.StoryErrorOccurred(
                    storyId = null,
                    slideId = null,
                    errorStage = StoryV2ErrorStage.CONTENT_VALIDATION.analyticsValue,
                    errorReason = StoryV2ErrorReason.NOT_FOUND.analyticsValue,
                    source = params.source.analyticsValue,
                ),
            )
            params.onResult(StoriesV2Result.Unavailable(reason = StoriesV2Result.Unavailable.Reason.NO_CONTENT))
            return
        }
        this.composition = composition

        val controller = StoryPlaybackController(
            composition = composition,
            source = params.source,
            analytics = analytics,
            onHaptic = { hapticManager.performOneTime(TangemHapticEffect.OneTime.HeavyClick) },
            onResult = params.onResult,
        )
        this.controller = controller

        if (composition.slides.any { it.asset is StoryV2Asset.Video }) {
            videoPlayer = StoryVideoPlayer(context = context, composition = composition, listener = this)
        }

        controller.setReducedMotion(enabled = isReducedMotionEnabled())
        controller.onOpened()

        // Collected only after the first slide is entered, so the player starts that slide rather than starting
        // the pre-open state and correcting itself.
        controller.state
            .onEach(::applyState)
            .launchIn(modelScope)
    }

    private fun applyState(state: StoryPlaybackState) {
        val composition = composition ?: return
        val slide = composition.slides[state.slideIndex]

        var isAssetReady = uiState.value?.slide?.isAssetReady == true

        if (state.playToken != appliedPlayToken) {
            isAssetReady = enterSlide(state = state, slide = slide)
        } else if (state.renderMode != appliedRenderMode) {
            degradeSlide(slide = slide)
            isAssetReady = true
        }

        videoPlayer?.setRunning(running = state.isRunning && isPlayerDriven(state = state, slide = slide))

        uiState.value = buildUiState(
            composition = composition,
            state = state,
            slide = slide,
            isAssetReady = isAssetReady,
        )
    }

    /** @return whether the asset already has a frame on screen, i.e. whether the poster can be skipped. */
    private fun enterSlide(state: StoryPlaybackState, slide: StoryV2Slide): Boolean {
        appliedPlayToken = state.playToken
        appliedRenderMode = state.renderMode

        val isPlayerDriven = isPlayerDriven(state = state, slide = slide)
        val isInterrupted = if (isPlayerDriven) {
            videoPlayer?.setContentMode(slide.asset.contentMode)
            // Everything except the player's own hand-over restarts the asset — including a loop back onto the very
            // item the player is parked on, where resuming would show a frozen last frame.
            videoPlayer?.playSlide(slideIndex = state.slideIndex, restart = !state.isPlayerDrivenEntry) == true
        } else {
            videoPlayer?.setRunning(running = false)
            false
        }

        clock = createClock(slide = slide, isPlayerDriven = isPlayerDriven)
        clock.reset()
        previousFrameNanos = NO_TIME
        elapsedRemainderNanos = 0L
        slidePositionMs.longValue = 0L

        return !isPlayerDriven || !isInterrupted
    }

    /** Only the timeline changes hands, from the decoder to the wall clock: a restart would look like a rewind. */
    private fun degradeSlide(slide: StoryV2Slide) {
        appliedRenderMode = SlideRenderMode.POSTER
        videoPlayer?.setRunning(running = false)
        clock = WallSlideClock(durationMs = slide.asset.durationMs, startPositionMs = slidePositionMs.longValue)
        previousFrameNanos = NO_TIME
        elapsedRemainderNanos = 0L
    }

    private fun createClock(slide: StoryV2Slide, isPlayerDriven: Boolean): SlideClock {
        val videoPlayer = videoPlayer
        return if (isPlayerDriven && videoPlayer != null) {
            VideoSlideClock(
                durationMs = slide.asset.durationMs,
                frames = videoPlayer.frames,
                playerPositionMs = videoPlayer::positionMs,
            )
        } else {
            WallSlideClock(durationMs = slide.asset.durationMs)
        }
    }

    private fun isPlayerDriven(state: StoryPlaybackState, slide: StoryV2Slide): Boolean {
        return state.renderMode == SlideRenderMode.ASSET && slide.asset is StoryV2Asset.Video
    }

    private fun buildUiState(
        composition: StoryV2Composition,
        state: StoryPlaybackState,
        slide: StoryV2Slide,
        isAssetReady: Boolean,
    ): StoriesV2UM {
        val controller = controller
        return StoriesV2UM(
            slideCount = composition.slides.size,
            slideIndex = state.slideIndex,
            playToken = state.playToken,
            slide = StoriesV2UM.SlideUM(
                id = slide.id,
                title = slide.title,
                subtitle = slide.subtitle,
                poster = slide.asset.poster,
                durationMs = slide.asset.durationMs,
                content = slide.asset.toContent(renderMode = state.renderMode),
                contentMode = slide.asset.contentMode,
                isAssetReady = isAssetReady,
            ),
            actions = composition.actionsOf(slide)
                .map { action ->
                    StoriesV2UM.ActionUM(
                        id = action.id,
                        label = action.label,
                        isPrimary = action.style == StoryV2Action.Style.PRIMARY,
                        iconRes = action.icon?.id,
                        onClick = { controller?.onActionClicked(action) },
                    )
                }
                .toImmutableList(),
            isPaused = !state.isRunning,
            onTapForward = { this.controller?.onTapForward() },
            onTapBack = { this.controller?.onTapBack() },
            onHoldChange = { held -> this.controller?.setHeld(held) },
            onDragChange = { dragging -> this.controller?.setDragging(dragging) },
            onCloseClick = { this.controller?.onCloseClicked() },
            onSwipeDown = { this.controller?.onSwipedDown() },
        )
    }

    /**
     * The sub-millisecond remainder is carried over: a frame is 16.667 ms at 60 Hz, so dropping the fraction every
     * frame would under-report watched time by 4%.
     */
    private fun elapsedMs(frameTimeNanos: Long, isRunning: Boolean): Long {
        val previous = previousFrameNanos
        previousFrameNanos = frameTimeNanos

        if (!isRunning || previous == NO_TIME) return 0L

        // Dropped, not clamped, like the position and the stall budget: clamping would credit watched time with a
        // quarter of a second on every return from background.
        val deltaNanos = frameTimeNanos - previous
        if (deltaNanos !in 0..MAX_FRAME_STEP_NANOS) return 0L

        val totalNanos = deltaNanos + elapsedRemainderNanos
        elapsedRemainderNanos = totalNanos % NANOS_PER_MS

        return totalNanos / NANOS_PER_MS
    }

    /**
     * Animations turned off system-wide is the closest thing Android offers to Reduce Motion, and the app already
     * reads it this way elsewhere. A viewer preference, not a failure: the story plays on posters.
     */
    private fun isReducedMotionEnabled(): Boolean {
        val scale = Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f)
        return scale == 0f
    }

    private companion object {
        const val NO_TIME = Long.MIN_VALUE
        const val NO_TOKEN = -1
    }
}

private fun StoryV2Asset.toContent(renderMode: SlideRenderMode): StoriesV2UM.SlideUM.Content = when {
    renderMode == SlideRenderMode.POSTER -> StoriesV2UM.SlideUM.Content.Still
    this is StoryV2Asset.Video -> StoriesV2UM.SlideUM.Content.Video
    this is StoryV2Asset.VectorAnimation -> StoriesV2UM.SlideUM.Content.VectorAnimation(source = source)
    else -> StoriesV2UM.SlideUM.Content.Still
}