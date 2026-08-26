package com.tangem.features.introduction.impl.engine

import android.view.SurfaceView
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player

internal class DefaultIntroductionVideoPlayer(
    private val player: Player,
    videoItem: MediaItem,
    override val isMotionEnabled: Boolean,
    private val listener: IntroductionVideoPlayer.Listener,
) : IntroductionVideoPlayer {

    private val playerListener = object : Player.Listener {

        override fun onRenderedFirstFrame() = listener.onFirstFrameRendered()

        override fun onPlayerError(error: PlaybackException) = listener.onError()
    }

    private var isPrepared = false

    private var attachedSurfaceView: SurfaceView? = null

    override val isSurfaceAttached: Boolean
        get() = attachedSurfaceView != null

    init {
        // Copies make the wrap an ordinary transition with the next period already buffered;
        // REPEAT_MODE_ONE re-seeks the single period instead and stalls on slower decoders.
        player.setMediaItems(List(LOOP_COPIES) { videoItem })
        player.repeatMode = Player.REPEAT_MODE_ALL
        player.addListener(playerListener)
    }

    override fun attachSurface(surfaceView: SurfaceView) {
        attachedSurfaceView = surfaceView
        player.setVideoSurfaceView(surfaceView)
        // With no surface set, media3 configures the codec against a placeholder one and reports a first
        // frame that nothing can paint.
        if (!isPrepared) {
            isPrepared = true
            player.prepare()
        }
    }

    override fun detachSurface(surfaceView: SurfaceView) {
        // The unqualified clearVideoSurface() blocks for up to two seconds and can drop someone else's surface.
        player.clearVideoSurfaceView(surfaceView)
        // A replacement can be attached before this one is released, and it keeps the picture.
        if (attachedSurfaceView !== surfaceView) return
        attachedSurfaceView = null
    }

    override fun setRunning(isRunning: Boolean) {
        if (isRunning && isMotionEnabled) player.play() else player.pause()
    }

    override fun release() {
        attachedSurfaceView = null
        player.removeListener(playerListener)
        player.release()
    }

    private companion object {
        const val LOOP_COPIES = 2
    }
}