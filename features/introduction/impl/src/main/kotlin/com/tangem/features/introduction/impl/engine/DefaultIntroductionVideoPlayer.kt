package com.tangem.features.introduction.impl.engine

import android.view.SurfaceView
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DecoderCounters
import kotlin.math.roundToInt

@OptIn(UnstableApi::class)
internal class DefaultIntroductionVideoPlayer(
    private val player: Player,
    private val decoderCounters: () -> DecoderCounters?,
    videoItem: MediaItem,
    override val isMotionEnabled: Boolean,
    private val listener: IntroductionVideoPlayer.Listener,
    private val teardownScheduler: VideoTeardownScheduler,
) : IntroductionVideoPlayer {

    private val playerListener = object : Player.Listener {

        override fun onRenderedFirstFrame() = listener.onFirstFrameRendered()

        override fun onVideoSizeChanged(videoSize: VideoSize) {
            if (videoSize.width == 0 || videoSize.height == 0) return
            listener.onVideoSizeChanged(
                width = (videoSize.width * videoSize.pixelWidthHeightRatio).roundToInt(),
                height = videoSize.height,
            )
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) = listener.onPlayingChanged(isPlaying)

        override fun onPlayerError(error: PlaybackException) = listener.onError(error)
    }

    private var isPrepared = false

    private var isReleased = false

    private var attachedSurfaceView: SurfaceView? = null

    override val isSurfaceAttached: Boolean
        get() = attachedSurfaceView != null

    override val positionMs: Long
        get() = player.currentPosition.coerceAtLeast(0L)

    override val outputFrameCount: Long?
        get() = decoderCounters()?.run {
            // The playback thread writes these; reading from another thread has to go through this first.
            ensureUpdated()
            renderedOutputBufferCount.toLong() + skippedOutputBufferCount + droppedBufferCount
        }

    init {
        // Copies make the wrap an ordinary transition with the next period already buffered;
        // REPEAT_MODE_ONE re-seeks the single period instead and stalls on slower decoders.
        player.setMediaItems(List(LOOP_COPIES) { videoItem })
        player.repeatMode = Player.REPEAT_MODE_ALL
        player.addListener(playerListener)
    }

    override fun attachSurface(surfaceView: SurfaceView) {
        // The screen can still compose a new surface while it animates away from a destroyed model, and binding
        // it would cancel the release that is already scheduled.
        if (isReleased) return
        teardownScheduler.cancel()
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
        // A replacement can be attached before this one is released, and it keeps the picture.
        if (attachedSurfaceView !== surfaceView) return
        attachedSurfaceView = null
        // The unqualified clearVideoSurface() would drop someone else's surface.
        teardownScheduler.schedule { player.clearVideoSurfaceView(surfaceView) }
    }

    override fun setRunning(isRunning: Boolean) {
        if (isReleased) return
        if (isRunning && isMotionEnabled) player.play() else player.pause()
    }

    override fun restart() {
        if (isReleased) return
        player.seekTo(0L)
    }

    override fun release() {
        isReleased = true
        attachedSurfaceView = null
        player.removeListener(playerListener)
        // Releasing takes the surface with it, so a pending detach has nothing left to do.
        teardownScheduler.cancel()
        teardownScheduler.schedule { player.release() }
    }

    private companion object {
        const val LOOP_COPIES = 2
    }
}