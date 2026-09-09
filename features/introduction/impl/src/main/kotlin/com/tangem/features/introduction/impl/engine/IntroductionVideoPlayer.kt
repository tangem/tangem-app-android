package com.tangem.features.introduction.impl.engine

import android.view.SurfaceView
import androidx.annotation.RawRes

internal interface IntroductionVideoPlayer {

    val isMotionEnabled: Boolean

    val isSurfaceAttached: Boolean

    /** Position inside the current pass of the loop: media3 counts per item, so it starts over at every wrap. */
    val positionMs: Long

    fun attachSurface(surfaceView: SurfaceView)

    fun detachSurface(surfaceView: SurfaceView)

    /** Ignored while [isMotionEnabled] is `false`. */
    fun setRunning(isRunning: Boolean)

    fun restart()

    fun release()

    interface Listener {

        /** Fires on every pass of the loop, not only on the first one. */
        fun onFirstFrameRendered()

        fun onError()
    }

    interface Factory {

        fun create(@RawRes videoRes: Int, listener: Listener): IntroductionVideoPlayer
    }
}