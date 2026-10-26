package com.tangem.core.media

import android.view.SurfaceView

internal interface OneShotVideoPlayer {

    val positionMs: Long

    /** Safe to read from any thread. */
    val presentedFrameCount: Long

    fun attachSurface(surfaceView: SurfaceView)

    fun detachSurface(surfaceView: SurfaceView)

    fun stop()

    /** [url] must be downloaded already. */
    fun prepare(url: String, cueMs: Long)

    fun play()

    fun pause()

    fun seekTo(positionMs: Long)

    fun mute()

    fun release()

    interface Listener {

        fun onReady()

        /** A frame at or past the cue is on screen; once per [prepare], on the main thread. */
        fun onCueFramePresented()

        fun onEnded()

        fun onError(error: Throwable)
    }
}