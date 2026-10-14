package com.tangem.features.collectibles.impl.packopening.video

import android.view.SurfaceView
import com.tangem.features.collectibles.impl.packopening.video.source.PackRevealAsset
import kotlinx.coroutines.flow.StateFlow

/**
 * The media half of the reveal: the full video, played once, and the short sound for a skip or a fallback.
 * Failures never throw: the animation must not gate the reward.
 */
internal interface PackRevealVideoPlayer {

    /** Whether the full video is loaded and prepared. */
    val videoLoadState: StateFlow<VideoLoadState>

    /** Renders the video into [surfaceView]. */
    fun attachSurface(surfaceView: SurfaceView)

    /** Stops rendering into [surfaceView]. */
    fun detachSurface(surfaceView: SurfaceView)

    /** Downloads and prepares the short sound. */
    suspend fun preloadSound(url: String?)

    /** Downloads and prepares [asset], replacing the previous video until [start]; cancel an unfinished call first. */
    suspend fun preloadVideo(asset: PackRevealAsset?)

    /** Plays the full video; without a ready one it goes straight to the card. */
    fun start()

    /** Plays the short sound, if it loaded. */
    fun playShortSound()

    /** Jumps to the burst, mutes the video and plays the short sound; false once the burst has begun. */
    fun skipToReveal(): Boolean

    /** Releases both players. */
    fun release()

    enum class VideoLoadState { Loading, Ready, Failed }

    interface Listener {

        /** The full video reached the card, or failed while playing; at most once, only after [start]. */
        fun onCardRevealReached()

        /** A load or playback failure, for logging. */
        fun onError(error: Throwable)
    }

    interface Factory {

        /** Creates a player reporting to [listener]. */
        fun create(listener: Listener): PackRevealVideoPlayer
    }
}