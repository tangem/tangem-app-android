package com.tangem.features.collectibles.impl.packopening.video

import android.view.SurfaceView
import androidx.annotation.OptIn
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import com.tangem.features.collectibles.impl.packopening.video.PackRevealVideoPlayer.VideoLoadState
import com.tangem.features.collectibles.impl.packopening.video.source.PackRevealAsset
import com.tangem.features.collectibles.impl.packopening.video.source.PackRevealMediaSourceFactory
import com.tangem.utils.coroutines.runSuspendCatching
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlin.time.Duration.Companion.milliseconds

@OptIn(UnstableApi::class)
internal class DefaultPackRevealVideoPlayer(
    private val videoPlayer: ExoPlayer,
    private val soundPlayer: ExoPlayer,
    private val mediaSourceFactory: PackRevealMediaSourceFactory,
    private val isMotionEnabled: Boolean,
    private val listener: PackRevealVideoPlayer.Listener,
) : PackRevealVideoPlayer {

    override val videoLoadState: StateFlow<VideoLoadState>
        field = MutableStateFlow(VideoLoadState.Loading)

    private var videoAsset: PackRevealAsset? = null

    private var videoPlaybackState = VideoPlaybackState.Idle

    private var isSoundReady = false

    private val videoListener = object : Player.Listener {

        override fun onPlaybackStateChanged(state: Int) {
            when (state) {
                Player.STATE_READY -> if (videoLoadState.value == VideoLoadState.Loading) {
                    videoLoadState.value = VideoLoadState.Ready
                }
                Player.STATE_ENDED -> reachCardReveal()
            }
        }

        override fun onPlayerError(error: PlaybackException) {
            listener.onError(error)
            videoLoadState.value = VideoLoadState.Failed
            reachCardReveal()
        }
    }

    private val soundListener = object : Player.Listener {

        override fun onPlaybackStateChanged(state: Int) {
            if (state == Player.STATE_READY) isSoundReady = true
        }

        override fun onPlayerError(error: PlaybackException) {
            listener.onError(error)
            isSoundReady = false
        }
    }

    init {
        videoPlayer.addListener(videoListener)
        soundPlayer.addListener(soundListener)
        videoPlayer.setVideoFrameMetadataListener { presentationTimeUs, _, _, _ ->
            val cardRevealMs = videoAsset?.cardRevealMs ?: return@setVideoFrameMetadataListener
            if (presentationTimeUs >= cardRevealMs.milliseconds.inWholeMicroseconds) reachCardReveal()
        }
    }

    override fun attachSurface(surfaceView: SurfaceView) {
        videoPlayer.setVideoSurfaceView(surfaceView)
    }

    override fun detachSurface(surfaceView: SurfaceView) {
        videoPlayer.clearVideoSurfaceView(surfaceView)
    }

    override suspend fun preloadSound(url: String?) {
        if (!isMotionEnabled || url == null) return

        load(url = url, into = soundPlayer, onFailure = {})
    }

    override suspend fun preloadVideo(asset: PackRevealAsset?) {
        if (videoPlaybackState != VideoPlaybackState.Idle) return

        if (asset == null) {
            videoLoadState.value = VideoLoadState.Failed
            return
        }

        videoAsset = asset

        if (!isMotionEnabled) {
            videoLoadState.value = VideoLoadState.Failed
            return
        }

        videoPlayer.stop()
        videoLoadState.value = VideoLoadState.Loading
        load(url = asset.videoUrl, into = videoPlayer, onFailure = { videoLoadState.value = VideoLoadState.Failed })
    }

    override fun start() {
        if (videoPlaybackState != VideoPlaybackState.Idle) return
        videoPlaybackState = VideoPlaybackState.Started

        if (videoLoadState.value != VideoLoadState.Ready) {
            reachCardReveal()
            return
        }

        videoPlayer.play()
    }

    override fun playShortSound() {
        if (!isSoundReady) return

        soundPlayer.seekTo(0L)
        soundPlayer.play()
    }

    override fun skipToReveal(): Boolean {
        val skipToMs = videoAsset?.skipToMs ?: return false
        if (videoPlaybackState != VideoPlaybackState.Started || videoPlayer.currentPosition >= skipToMs) return false

        videoPlayer.seekTo(skipToMs)
        videoPlayer.volume = 0f
        playShortSound()
        return true
    }

    override fun release() {
        videoPlayer.removeListener(videoListener)
        soundPlayer.removeListener(soundListener)
        videoPlayer.release()
        soundPlayer.release()
    }

    private suspend fun load(url: String, into: ExoPlayer, onFailure: () -> Unit) {
        runSuspendCatching { mediaSourceFactory.create(url = url) }
            .fold(
                onSuccess = { source ->
                    into.setMediaSource(source)
                    into.prepare()
                },
                onFailure = { error ->
                    listener.onError(error)
                    onFailure()
                },
            )
    }

    private fun reachCardReveal() {
        if (videoPlaybackState != VideoPlaybackState.Started) return

        videoPlaybackState = VideoPlaybackState.CardRevealReached
        listener.onCardRevealReached()
    }

    private enum class VideoPlaybackState {
        Idle,
        Started,
        CardRevealReached,
    }
}