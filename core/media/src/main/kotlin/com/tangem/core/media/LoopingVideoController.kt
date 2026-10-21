package com.tangem.core.media

import android.view.SurfaceView
import androidx.annotation.RawRes
import com.tangem.utils.logging.TangemLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

/** Plays a raw-resource clip as a muted looping background; a failed video never runs again. */
class LoopingVideoController internal constructor(
    scope: CoroutineScope,
    private val frameSize: VideoFrameSize,
    private val diagnosticsSubject: String,
    private val onFailed: (error: Throwable, hasRenderedFrame: Boolean) -> Unit,
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
    private var isRunRequested = false

    /** Starts over at every pass of the loop. */
    val positionMs: Long
        get() = player.positionMs

    /** Whether the screen is resumed and nothing covers the video. */
    fun setRunning(isRunning: Boolean) {
        isRunRequested = isRunning
        applyRunning()
    }

    fun restart() {
        player.restart()
    }

    fun release() {
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

    private fun applyRunning() {
        val isRunning = isRunRequested && !state.value.isFailed
        player.setRunning(isRunning = isRunning)
        state.update { it.copy(isRunning = isRunning) }
    }

    private fun replaceSurface() {
        updateSurface { it.copy(key = it.key + 1, isFirstFrameShown = false) }
    }

    private fun updateFirstFrameShown() {
        if (!hasRenderedFrame || !player.isSurfaceAttached) return
        updateSurface { it.copy(isFirstFrameShown = true) }
    }

    private fun fail(error: Throwable) {
        val stage = if (hasRenderedFrame) "after" else "before"
        TangemLogger.e("$diagnosticsSubject failed $stage its first frame", error)
        onFailed(error, hasRenderedFrame)
        // The poster stays: a SurfaceView without a frame is a hole through the window.
        state.update { it.copy(isFailed = true) }
        // The watchdog giving up, unlike a player error, does not stop the player.
        applyRunning()
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
    }
}