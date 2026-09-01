package com.tangem.features.storiesv2.impl.engine

import android.content.Context
import android.net.Uri
import android.view.SurfaceView
import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.RawResourceDataSource
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import com.tangem.features.storiesv2.impl.content.StoryV2Asset
import com.tangem.features.storiesv2.impl.content.StoryV2Composition
import com.tangem.features.storiesv2.impl.content.StoryV2ContentMode
import com.tangem.features.storiesv2.impl.content.StoryV2MediaRef
import java.io.File

/**
 * The video half of the engine: one player, one surface, for the whole story.
 *
 * Consecutive video slides are loaded as a single playlist, so the player moves between them itself — the renderer
 * is never torn down, the surface is never cleared and the next item is already buffered, so there is no black frame
 * to hide. The engine observes that transition rather than driving it: [Listener.onAutoAdvancedTo] reports what the
 * player already did.
 *
 * A slide that is not a video breaks the chain, so slides are grouped into runs of adjacent videos. Moving inside a
 * run is a seek onto a buffered item; moving between runs reloads the playlist.
 */
@OptIn(UnstableApi::class)
internal class StoryVideoPlayer(
    context: Context,
    private val composition: StoryV2Composition,
    private val listener: Listener,
) {

    interface Listener {

        fun onAutoAdvancedTo(slideIndex: Int)

        fun onEnded()

        /** Reported again for every item of a playlist, not just the first. */
        fun onFirstFrameRendered()

        /** Width over height, needed to letterbox a `contain` slide. */
        fun onAspectRatioChanged(aspectRatio: Float)

        fun onError()
    }

    val frames = RenderedFrameFeed()

    private val runs: List<VideoRun> = buildRuns(composition)

    private var currentRunIndex = NO_RUN

    private val player: ExoPlayer = ExoPlayer.Builder(
        context.applicationContext,
        // A device whose primary decoder refuses the file — a profile level above what it advertises, a resolution
        // above what its AVC decoder is tuned for — has others, and trying them is the difference between a story
        // and four still pictures.
        DefaultRenderersFactory(context.applicationContext).setEnableDecoderFallback(true),
    )
        .build()
        .apply {
            repeatMode = Player.REPEAT_MODE_OFF
            // Story videos carry no audio track, and a story is not a reason to duck whatever the viewer is
            // listening to.
            volume = 0f
            setVideoFrameMetadataListener { presentationTimeUs, releaseTimeNs, _, _ ->
                frames.onFrameRendered(presentationTimeUs = presentationTimeUs, releaseTimeNanos = releaseTimeNs)
            }
            addListener(PlayerEvents())
        }

    val positionMs: Long get() = player.currentPosition.coerceAtLeast(0L)

    fun attachSurface(surfaceView: SurfaceView) {
        player.setVideoSurfaceView(surfaceView)
    }

    /**
     * Identity-checked on purpose: the unqualified `clearVideoSurface()` detaches whatever is attached and blocks
     * the caller for up to two seconds, so a released view could take the surface its replacement already claimed.
     */
    fun detachSurface(surfaceView: SurfaceView) {
        player.clearVideoSurfaceView(surfaceView)
    }

    /**
     * Makes [slideIndex] the slide on screen. Called on every slide change, including the ones the player performed
     * itself — those are deliberately a no-op, which is what leaves the transition untouched.
     *
     * @return whether the surface has to be covered by the poster until the next first frame.
     */
    fun playSlide(slideIndex: Int, restart: Boolean): Boolean {
        val runIndex = runs.indexOfFirst { slideIndex in it.slideRange }
        if (runIndex == NO_RUN) {
            player.pause()
            return false
        }

        val run = runs[runIndex]
        val itemIndex = slideIndex - run.firstSlideIndex

        if (runIndex != currentRunIndex) {
            frames.reset()
            player.setMediaItems(run.items, itemIndex, 0L)
            player.prepare()
            // Only once the playlist is loaded: a run recorded as current without its items would send every later
            // slide of it down the seek path, against a timeline that does not have that index.
            currentRunIndex = runIndex
            return true
        }

        // Being parked on the right item is not the same as playing it: an error leaves IDLE, the end of a run
        // leaves ENDED, and in both states `play()` renders nothing further.
        val isOnItem = player.currentMediaItemIndex == itemIndex &&
            player.playbackState != Player.STATE_IDLE &&
            player.playbackState != Player.STATE_ENDED
        if (isOnItem && !restart) {
            // Nothing to cover — unless the hand-over has not caught up, where the surface still holds the previous
            // item's last frame and the poster has to hide it until this item renders its own.
            return player.playbackState != Player.STATE_READY
        }

        frames.reset()
        player.seekTo(itemIndex, 0L)
        // Seeking an IDLE player only moves its position; preparing the playlist is what brings the rest of the
        // run back after one item failed to play.
        if (player.playbackState == Player.STATE_IDLE) player.prepare()
        return true
    }

    fun setRunning(running: Boolean) {
        if (running) player.play() else player.pause()
    }

    /**
     * `cover` is done by the decoder, not by the view: MediaCodec crops to the surface, which avoids resizing a
     * SurfaceView — a resize is one of the few things that still makes one flash black.
     */
    fun setContentMode(contentMode: StoryV2ContentMode) {
        player.videoScalingMode = when (contentMode) {
            StoryV2ContentMode.COVER -> C.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING
            StoryV2ContentMode.CONTAIN -> C.VIDEO_SCALING_MODE_SCALE_TO_FIT
        }
    }

    fun release() {
        player.release()
    }

    private inner class PlayerEvents : Player.Listener {

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            // Seeks and playlist reloads are our own doing; only the player's own transition moves the story on.
            if (reason != Player.MEDIA_ITEM_TRANSITION_REASON_AUTO) return

            val run = runs.getOrNull(currentRunIndex) ?: return
            listener.onAutoAdvancedTo(run.firstSlideIndex + player.currentMediaItemIndex)
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            if (playbackState == Player.STATE_ENDED) listener.onEnded()
        }

        override fun onRenderedFirstFrame() {
            listener.onFirstFrameRendered()
        }

        override fun onVideoSizeChanged(videoSize: VideoSize) {
            if (videoSize.width == 0 || videoSize.height == 0) return

            val ratio = videoSize.width * videoSize.pixelWidthHeightRatio / videoSize.height
            listener.onAspectRatioChanged(ratio)
        }

        override fun onPlayerError(error: PlaybackException) {
            listener.onError()
        }
    }

    private data class VideoRun(val firstSlideIndex: Int, val items: List<MediaItem>) {

        val slideRange: IntRange = firstSlideIndex until firstSlideIndex + items.size
    }

    private companion object {

        const val NO_RUN = -1

        /** Anything that is not a video — an image, a Lottie composition — ends one run and starts the next. */
        fun buildRuns(composition: StoryV2Composition): List<VideoRun> {
            val runs = mutableListOf<VideoRun>()
            val items = mutableListOf<MediaItem>()
            var firstIndex = NO_RUN

            composition.slides.forEachIndexed { index, slide ->
                val source = (slide.asset as? StoryV2Asset.Video)?.source
                if (source == null) {
                    if (items.isNotEmpty()) {
                        runs += VideoRun(firstIndex, items.toList())
                        items.clear()
                    }
                    firstIndex = NO_RUN
                } else {
                    if (items.isEmpty()) firstIndex = index
                    items += MediaItem.fromUri(source.toUri())
                }
            }
            if (items.isNotEmpty()) runs += VideoRun(firstIndex, items.toList())

            return runs
        }

        fun StoryV2MediaRef.toUri(): Uri = when (this) {
            is StoryV2MediaRef.RawResource -> RawResourceDataSource.buildRawResourceUri(id)
            is StoryV2MediaRef.LocalFile -> Uri.fromFile(File(path))
            is StoryV2MediaRef.DrawableResource -> error("A drawable cannot be a video source")
        }
    }
}