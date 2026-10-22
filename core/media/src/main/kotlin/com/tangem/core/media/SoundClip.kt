package com.tangem.core.media

import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import com.tangem.utils.logging.TangemLogger

/** A short remote sound mixed over other audio; failures are only logged. */
class SoundClip internal constructor(
    private val player: Player,
    private val cache: RemoteClipCache,
    private val diagnosticsSubject: String,
) {

    private var isReady = false
    private var isPausedInBackground = false

    private val playerListener = object : Player.Listener {

        override fun onPlaybackStateChanged(playbackState: Int) {
            if (playbackState == Player.STATE_READY) isReady = true
        }

        override fun onPlayerError(error: PlaybackException) {
            TangemLogger.e("$diagnosticsSubject failed", error)
            isReady = false
        }
    }

    init {
        player.addListener(playerListener)
    }

    /** Never throws but to cancel. */
    suspend fun prepare(url: String) {
        if (!cache.ensureCached(url = url, diagnosticsSubject = diagnosticsSubject)) return

        player.setMediaItem(MediaItem.fromUri(url))
        player.prepare()
    }

    /** Ignored until the sound is ready, and after it failed. */
    fun play() {
        if (!isReady) return

        player.seekTo(0L)
        player.play()
    }

    /** Whether the screen is in front; a sound cut off by the background carries on from where it was. */
    fun setRunning(isRunning: Boolean) {
        if (isRunning) {
            if (isPausedInBackground) player.play()
            isPausedInBackground = false
        } else if (player.isPlaying) {
            player.pause()
            isPausedInBackground = true
        }
    }

    fun release() {
        player.removeListener(playerListener)
        player.release()
    }

    interface Factory {

        fun create(cache: RemoteClipCache, diagnosticsSubject: String): SoundClip
    }
}