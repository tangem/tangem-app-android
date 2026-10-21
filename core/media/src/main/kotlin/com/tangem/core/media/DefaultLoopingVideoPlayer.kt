package com.tangem.core.media

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
internal class DefaultLoopingVideoPlayer(
    private val player: Player,
    private val decoderCounters: () -> DecoderCounters?,
    videoItem: MediaItem,
    override val isMotionEnabled: Boolean,
    private val listener: LoopingVideoPlayer.Listener,
    teardownScheduler: VideoTeardownScheduler,
) : LoopingVideoPlayer {

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

    private val surfaceBinding = VideoSurfaceBinding(player = player, teardownScheduler = teardownScheduler)

    private var isPrepared = false

    override val isSurfaceAttached: Boolean
        get() = surfaceBinding.isAttached

    override val positionMs: Long
        get() = player.currentPosition.coerceAtLeast(0L)

    override val outputFrameCount: Long?
        get() = decoderCounters()?.run {
            // Written on the playback thread.
            ensureUpdated()
            renderedOutputBufferCount.toLong() + skippedOutputBufferCount + droppedBufferCount
        }

    init {
        // With copies the wrap is a buffered transition; REPEAT_MODE_ONE re-seeks and stalls slower decoders.
        player.setMediaItems(List(LOOP_COPIES) { videoItem })
        player.repeatMode = Player.REPEAT_MODE_ALL
        player.addListener(playerListener)
    }

    override fun attachSurface(surfaceView: SurfaceView) {
        if (!surfaceBinding.attach(surfaceView)) return
        // Prepared without a surface, media3 reports a first frame that nothing paints.
        if (!isPrepared) {
            isPrepared = true
            player.prepare()
        }
    }

    override fun detachSurface(surfaceView: SurfaceView) {
        surfaceBinding.detach(surfaceView)
    }

    override fun setRunning(isRunning: Boolean) {
        if (surfaceBinding.isReleased) return
        if (isRunning && isMotionEnabled) player.play() else player.pause()
    }

    override fun restart() {
        if (surfaceBinding.isReleased) return
        player.seekTo(0L)
    }

    override fun release() {
        player.removeListener(playerListener)
        surfaceBinding.release()
    }

    private companion object {
        const val LOOP_COPIES = 2
    }
}