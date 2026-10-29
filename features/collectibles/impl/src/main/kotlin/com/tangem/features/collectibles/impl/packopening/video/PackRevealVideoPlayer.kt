package com.tangem.features.collectibles.impl.packopening.video

import android.view.SurfaceView
import com.tangem.core.media.VideoLoadState
import com.tangem.features.collectibles.impl.packopening.video.source.PackRevealAsset
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow

/** The reveal video and the skip sound. Failures never throw: the animation must not gate the reward. */
internal interface PackRevealVideoPlayer {

    val videoLoadState: StateFlow<VideoLoadState>

    fun attachSurface(surfaceView: SurfaceView)

    fun detachSurface(surfaceView: SurfaceView)

    suspend fun preloadSkipSound(url: String?)

    /** Replaces the previous video until [start]; cancel an unfinished call first. */
    suspend fun preloadVideo(asset: PackRevealAsset?)

    /** Plays the full video; without a ready one it goes straight to the card. */
    fun start()

    /** Plays the skip sound, if it loaded. */
    fun playSkipSound()

    /** Jumps to the burst, mutes the video and plays the skip sound; false once the burst has begun. */
    fun skipToReveal(): Boolean

    /** Whether the screen is in front. */
    fun setRunning(isRunning: Boolean)

    fun release()

    interface Listener {

        /** The full video reached the card, or never will; at most once, only after [start], on the main thread. */
        fun onCardRevealReached()
    }

    interface Factory {

        /** [scope] runs on the main thread. */
        fun create(scope: CoroutineScope, listener: Listener): PackRevealVideoPlayer
    }
}