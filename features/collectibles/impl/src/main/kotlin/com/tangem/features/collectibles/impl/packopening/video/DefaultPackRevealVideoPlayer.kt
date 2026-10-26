package com.tangem.features.collectibles.impl.packopening.video

import android.view.SurfaceView
import com.tangem.core.media.OneShotVideoController
import com.tangem.core.media.OneShotVideoController.CueReason
import com.tangem.core.media.RemoteClipCache
import com.tangem.core.media.SoundClip
import com.tangem.core.media.VideoLoadState
import com.tangem.features.collectibles.impl.packopening.PackOpeningConst
import com.tangem.features.collectibles.impl.packopening.video.source.PackRevealAsset
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow

internal class DefaultPackRevealVideoPlayer(
    scope: CoroutineScope,
    cache: RemoteClipCache,
    videoFactory: OneShotVideoController.Factory,
    soundFactory: SoundClip.Factory,
    private val listener: PackRevealVideoPlayer.Listener,
) : PackRevealVideoPlayer {

    private val video = videoFactory.create(
        scope = scope,
        cache = cache,
        diagnosticsSubject = "Pack reveal video",
        stallTimeoutMs = PackOpeningConst.VIDEO_STALL_TIMEOUT_MS,
        onCueReached = ::onCueReached,
    )

    private val skipSound = soundFactory.create(cache = cache, diagnosticsSubject = "Pack skip sound")

    private var videoAsset: PackRevealAsset? = null

    override val videoLoadState: StateFlow<VideoLoadState>
        get() = video.loadState

    override fun attachSurface(surfaceView: SurfaceView) {
        video.attachSurface(surfaceView)
    }

    override fun detachSurface(surfaceView: SurfaceView) {
        video.detachSurface(surfaceView)
    }

    override suspend fun preloadSkipSound(url: String?) {
        if (!video.isMotionEnabled || url == null) return

        skipSound.prepare(url)
    }

    override suspend fun preloadVideo(asset: PackRevealAsset?) {
        videoAsset = asset
        video.prepare(url = asset?.videoUrl, cueMs = asset?.cardRevealMs ?: 0L)
    }

    override fun start() {
        video.start()
    }

    override fun playSkipSound() {
        skipSound.play()
    }

    override fun skipToReveal(): Boolean {
        val skipToMs = videoAsset?.skipToMs ?: return false
        if (!video.skipTo(skipToMs)) return false

        skipSound.play()
        return true
    }

    override fun setRunning(isRunning: Boolean) {
        video.setRunning(isRunning)
        skipSound.setRunning(isRunning)
    }

    override fun release() {
        video.release()
        skipSound.release()
    }

    private fun onCueReached(reason: CueReason) {
        if (reason == CueReason.Stalled) skipSound.play()
        listener.onCardRevealReached()
    }
}