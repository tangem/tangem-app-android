package com.tangem.core.media

import android.view.SurfaceView
import androidx.media3.common.Player

/** Binds [player] to the screen's SurfaceView and releases both off the frames of the exit transition. */
internal class VideoSurfaceBinding(
    private val player: Player,
    private val teardownScheduler: VideoTeardownScheduler,
) {

    private var attachedSurfaceView: SurfaceView? = null

    val isAttached: Boolean
        get() = attachedSurfaceView != null

    var isReleased: Boolean = false
        private set

    /** @return `false` once released. */
    fun attach(surfaceView: SurfaceView): Boolean {
        // A screen animating away can still compose a surface, and binding it would cancel the scheduled release.
        if (isReleased) return false
        teardownScheduler.cancel()
        attachedSurfaceView = surfaceView
        player.setVideoSurfaceView(surfaceView)
        return true
    }

    fun detach(surfaceView: SurfaceView) {
        // A replacement may already be attached.
        if (attachedSurfaceView !== surfaceView) return
        attachedSurfaceView = null
        // The unqualified clearVideoSurface() would drop someone else's surface.
        teardownScheduler.schedule { player.clearVideoSurfaceView(surfaceView) }
    }

    fun release() {
        isReleased = true
        attachedSurfaceView = null
        // The release clears the surface too.
        teardownScheduler.cancel()
        teardownScheduler.schedule { player.release() }
    }
}