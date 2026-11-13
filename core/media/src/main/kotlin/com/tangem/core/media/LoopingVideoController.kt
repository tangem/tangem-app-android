package com.tangem.core.media

import android.view.SurfaceView
import androidx.annotation.RawRes
import com.tangem.utils.coroutines.runSuspendCatching
import com.tangem.utils.logging.TangemLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Plays a raw-resource clip, or a clip whose source [loadSource] makes available, as a muted looping background;
 * a failed video, or a failed load, never runs again.
 */
class LoopingVideoController internal constructor(
    scope: CoroutineScope,
    private val frameSize: VideoFrameSize,
    private val diagnosticsSubject: String,
    private val onFailed: (error: Throwable, hasRenderedFrame: Boolean) -> Unit,
    loadSource: (suspend () -> Unit)? = null,
    createPlayer: (LoopingVideoPlayer.Listener) -> LoopingVideoPlayer,
) {

    private val player = createPlayer(PlayerListener())

    val state: StateFlow<LoopingVideoState>
        field = MutableStateFlow(
            LoopingVideoState(
                surface = VideoSurfaceState(
                    frameSize = frameSize,
                    key = 0,
                    isFirstFrameShown = false,
                    onAvailable = ::attachSurface,
                    onRelease = ::detachSurface,
                ),
                isRunning = false,
                isMotionEnabled = player.isMotionEnabled,
                isFailed = false,
            ),
        )

    private val outputWatchdog = VideoOutputWatchdog(
        scope = scope,
        videoPlayer = player,
        subject = diagnosticsSubject,
        onReplaceSurface = ::replaceSurface,
        onGiveUp = ::fail,
    )

    private var hasRenderedFrame = false

    // Declared last: on an immediate dispatcher a failing load reaches fail() before the constructor returns.
    private val loadJob: Job? = loadSource?.let { load ->
        scope.launch {
            runSuspendCatching { load() }
                // A cancelled loader may still return or throw something other than a CancellationException.
                .also { ensureActive() }
                .onSuccess { player.markSourceReady() }
                .onFailure(::fail)
        }
    }

    /** Starts over at every pass of the loop. */
    val positionMs: Long
        get() = player.positionMs

    /** Whether the screen is resumed and nothing covers the video. */
    fun setRunning(isRunning: Boolean) {
        if (state.value.isFailed) return
        player.setRunning(isRunning = isRunning)
        state.update { it.copy(isRunning = isRunning) }
    }

    fun restart() {
        player.restart()
    }

    fun release() {
        loadJob?.cancel()
        // A released player never reports the stop.
        outputWatchdog.onPlayingChanged(isPlaying = false)
        player.release()
    }

    private fun attachSurface(surfaceView: SurfaceView) {
        player.attachSurface(surfaceView)
    }

    private fun detachSurface(surfaceView: SurfaceView) {
        player.detachSurface(surfaceView)
        if (player.isSurfaceAttached) return
        updateSurface { it.copy(isFirstFrameShown = false) }
    }

    private fun replaceSurface() {
        updateSurface { it.copy(key = it.key + 1, isFirstFrameShown = false) }
    }

    private fun updateFirstFrameShown() {
        if (!hasRenderedFrame || !player.isSurfaceAttached) return
        updateSurface { it.copy(isFirstFrameShown = true) }
    }

    private fun fail(error: Throwable) {
        // The player and the watchdog can both give up on the same video.
        if (state.value.isFailed) return
        val stage = if (hasRenderedFrame) "after" else "before"
        TangemLogger.e("$diagnosticsSubject failed $stage its first frame", error)
        onFailed(error, hasRenderedFrame)
        // The watchdog giving up, unlike a player error, does not stop the player.
        player.setRunning(isRunning = false)
        // The poster stays: a SurfaceView without a frame is a hole through the window.
        state.update { it.copy(isRunning = false, isFailed = true) }
    }

    private inline fun updateSurface(transform: (VideoSurfaceState) -> VideoSurfaceState) {
        state.update { it.copy(surface = transform(it.surface)) }
    }

    private inner class PlayerListener : LoopingVideoPlayer.Listener {

        override fun onFirstFrameRendered() {
            hasRenderedFrame = true
            updateFirstFrameShown()
        }

        /** Only logged: a resized surface would race the first frame, and cropping fits the file into [frameSize]. */
        override fun onVideoSizeChanged(width: Int, height: Int) {
            if (width == frameSize.width && height == frameSize.height) return
            val declaredSize = "${frameSize.width}x${frameSize.height}"
            TangemLogger.e("$diagnosticsSubject is ${width}x$height, but $declaredSize is declared for it")
        }

        override fun onPlayingChanged(isPlaying: Boolean) = outputWatchdog.onPlayingChanged(isPlaying)

        override fun onError(error: Throwable) = fail(error)
    }

    interface Factory {

        /**
         * @param frameSize          declared rather than read from the file: resizing the surface under a playing
         *                           decoder can get its output stuck
         * @param diagnosticsSubject what the log entries are about, e.g. "Welcome video"
         * @param onFailed           given up for good; `hasRenderedFrame` tells a playback failure from a load one
         */
        fun create(
            scope: CoroutineScope,
            @RawRes videoRes: Int,
            frameSize: VideoFrameSize,
            diagnosticsSubject: String,
            onFailed: (error: Throwable, hasRenderedFrame: Boolean) -> Unit = { _, _ -> },
        ): LoopingVideoController

        /**
         * Like [create], for a clip at [url].
         *
         * @param cache    downloads [url] in full before the clip is prepared; a failed download fails the clip
         *                 as a load failure
         * @param onFailed a load that fails at once calls it before [createRemote] returns, so it must not touch
         *                 the returned controller
         */
        fun createRemote(
            scope: CoroutineScope,
            url: String,
            cache: RemoteClipCache,
            frameSize: VideoFrameSize,
            diagnosticsSubject: String,
            onFailed: (error: Throwable, hasRenderedFrame: Boolean) -> Unit = { _, _ -> },
        ): LoopingVideoController
    }
}