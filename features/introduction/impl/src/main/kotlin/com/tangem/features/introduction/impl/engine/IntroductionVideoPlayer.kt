package com.tangem.features.introduction.impl.engine

import android.view.SurfaceView
import androidx.annotation.RawRes

internal interface IntroductionVideoPlayer {

    val isMotionEnabled: Boolean

    val isSurfaceAttached: Boolean

    /** Position inside the current pass of the loop: media3 counts per item, so it starts over at every wrap. */
    val positionMs: Long

    /**
     * Frames the decoder has handed back so far, whether shown, dropped or skipped; `null` while no decoder is enabled.
     * A count that stands still while the video is playing means its output is stuck.
     */
    val outputFrameCount: Long?

    fun attachSurface(surfaceView: SurfaceView)

    fun detachSurface(surfaceView: SurfaceView)

    /** Ignored while [isMotionEnabled] is `false`. */
    fun setRunning(isRunning: Boolean)

    fun restart()

    fun release()

    interface Listener {

        /** Fires on every pass of the loop, not only on the first one. */
        fun onFirstFrameRendered()

        /** The size of the decoded frames, in square pixels. */
        fun onVideoSizeChanged(width: Int, height: Int)

        /** Playing means the clock runs and frames are due; paused and buffering are not. */
        fun onPlayingChanged(isPlaying: Boolean)

        fun onError(error: Throwable)
    }

    interface Factory {

        fun create(@RawRes videoRes: Int, listener: Listener): IntroductionVideoPlayer
    }
}