package com.tangem.core.media

import android.view.SurfaceView

/** In square pixels. */
data class VideoFrameSize(val width: Int, val height: Int) {

    val aspectRatio: Float
        get() = width.toFloat() / height
}

data class VideoSurfaceState(
    val frameSize: VideoFrameSize,
    /** A new value asks for a new surface: a stuck output only recovers on a fresh one ([REDACTED_TASK_KEY]). */
    val key: Int,
    val isFirstFrameShown: Boolean,
    val onAvailable: (SurfaceView) -> Unit,
    /** A replacement surface can arrive before the previous one is released. */
    val onRelease: (SurfaceView) -> Unit,
)

data class LoopingVideoState(
    val surface: VideoSurfaceState,
    val isRunning: Boolean,
    /** `false` while animations are off system-wide: the clip stays on its first frame. */
    val isMotionEnabled: Boolean,
    /** Stays stopped for good. */
    val isFailed: Boolean,
)