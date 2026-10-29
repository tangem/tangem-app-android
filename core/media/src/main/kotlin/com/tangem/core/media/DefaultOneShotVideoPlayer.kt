package com.tangem.core.media

import android.view.SurfaceView
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import kotlin.time.Duration.Companion.milliseconds

/** @param postToMain frame callbacks arrive on the playback thread */
@OptIn(UnstableApi::class)
internal class DefaultOneShotVideoPlayer(
    private val player: ExoPlayer,
    private val listener: OneShotVideoPlayer.Listener,
    teardownScheduler: VideoTeardownScheduler,
    private val postToMain: (() -> Unit) -> Unit,
) : OneShotVideoPlayer {

    private val playerListener = object : Player.Listener {

        override fun onPlaybackStateChanged(playbackState: Int) {
            when (playbackState) {
                Player.STATE_READY -> listener.onReady()
                Player.STATE_ENDED -> listener.onEnded()
            }
        }

        override fun onPlayerError(error: PlaybackException) = listener.onError(error)
    }

    private val surfaceBinding = VideoSurfaceBinding(player = player, teardownScheduler = teardownScheduler)

    private val presentedFrames = AtomicLong(0L)
    private val isCueFramePresented = AtomicBoolean(false)

    @Volatile
    private var cueUs = Long.MAX_VALUE

    override val positionMs: Long
        get() = player.currentPosition.coerceAtLeast(0L)

    override val presentedFrameCount: Long
        get() = presentedFrames.get()

    init {
        player.addListener(playerListener)
        // Only frames released to the surface get here, so a frozen picture never reaches the cue.
        player.setVideoFrameMetadataListener { presentationTimeUs, _, _, _ ->
            presentedFrames.incrementAndGet()
            if (presentationTimeUs >= cueUs && isCueFramePresented.compareAndSet(false, true)) {
                postToMain { if (!surfaceBinding.isReleased) listener.onCueFramePresented() }
            }
        }
    }

    override fun attachSurface(surfaceView: SurfaceView) {
        surfaceBinding.attach(surfaceView)
    }

    override fun detachSurface(surfaceView: SurfaceView) {
        surfaceBinding.detach(surfaceView)
    }

    override fun stop() {
        if (surfaceBinding.isReleased) return
        player.stop()
    }

    override fun prepare(url: String, cueMs: Long) {
        if (surfaceBinding.isReleased) return
        cueUs = cueMs.milliseconds.inWholeMicroseconds
        isCueFramePresented.set(false)
        player.setMediaItem(MediaItem.fromUri(url))
        player.prepare()
    }

    override fun play() {
        if (surfaceBinding.isReleased) return
        player.play()
    }

    override fun pause() {
        if (surfaceBinding.isReleased) return
        player.pause()
    }

    override fun seekTo(positionMs: Long) {
        if (surfaceBinding.isReleased) return
        player.seekTo(positionMs)
    }

    override fun mute() {
        if (surfaceBinding.isReleased) return
        player.volume = 0f
    }

    override fun release() {
        if (surfaceBinding.isReleased) return
        player.removeListener(playerListener)
        // Silent at once: the release itself waits for the exit transition.
        player.pause()
        surfaceBinding.release()
    }
}