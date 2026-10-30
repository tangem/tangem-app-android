package com.tangem.core.media

import android.view.SurfaceView

internal interface LoopingVideoPlayer {

    val isMotionEnabled: Boolean

    val isSurfaceAttached: Boolean

    /** Starts over at every pass of the loop. */
    val positionMs: Long

    /** Decoder output so far, shown or not; `null` without a decoder. Stands still while the output is stuck. */
    val outputFrameCount: Long?

    fun attachSurface(surfaceView: SurfaceView)

    fun detachSurface(surfaceView: SurfaceView)

    /** Keeps the clip paused while [isMotionEnabled] is `false`. */
    fun setRunning(isRunning: Boolean)

    fun restart()

    fun release()

    interface Listener {

        /** Fires on every pass of the loop. */
        fun onFirstFrameRendered()

        /** In square pixels. */
        fun onVideoSizeChanged(width: Int, height: Int)

        fun onPlayingChanged(isPlaying: Boolean)

        fun onError(error: Throwable)
    }
}