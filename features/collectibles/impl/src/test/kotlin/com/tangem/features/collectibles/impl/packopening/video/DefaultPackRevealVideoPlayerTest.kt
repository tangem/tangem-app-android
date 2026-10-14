package com.tangem.features.collectibles.impl.packopening.video

import android.view.SurfaceView
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.video.VideoFrameMetadataListener
import com.google.common.truth.Truth.assertThat
import com.tangem.features.collectibles.impl.packopening.video.PackRevealVideoPlayer.VideoLoadState
import com.tangem.features.collectibles.impl.packopening.video.source.PackRevealAsset
import com.tangem.features.collectibles.impl.packopening.video.source.PackRevealMediaSourceFactory
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import io.mockk.verifyOrder
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.io.IOException
import kotlin.time.Duration.Companion.milliseconds

internal class DefaultPackRevealVideoPlayerTest {

    private val videoPlayer: ExoPlayer = mockk(relaxed = true)
    private val soundPlayer: ExoPlayer = mockk(relaxed = true)
    private val mediaSourceFactory: PackRevealMediaSourceFactory = mockk()
    private val listener: PackRevealVideoPlayer.Listener = mockk(relaxed = true)
    private val videoSource: MediaSource = mockk(relaxed = true)
    private val otherVideoSource: MediaSource = mockk(relaxed = true)
    private val soundSource: MediaSource = mockk(relaxed = true)
    private val surfaceView: SurfaceView = mockk(relaxed = true)

    private val asset = PackRevealAsset(videoUrl = VIDEO_URL, cardRevealMs = CARD_REVEAL_MS, skipToMs = SKIP_TO_MS)

    private val otherAsset = asset.copy(videoUrl = OTHER_VIDEO_URL)

    @BeforeEach
    fun resetMocks() {
        clearMocks(videoPlayer, soundPlayer, mediaSourceFactory, listener)
        coEvery { mediaSourceFactory.create(VIDEO_URL) } returns videoSource
        coEvery { mediaSourceFactory.create(OTHER_VIDEO_URL) } returns otherVideoSource
        coEvery { mediaSourceFactory.create(SOUND_URL) } returns soundSource
    }

    @Nested
    inner class PreloadVideo {

        @Test
        fun `GIVEN player created WHEN nothing preloaded THEN nothing is prepared`() {
            // Act
            createPlayer()

            // Assert
            verify(exactly = 0) { videoPlayer.prepare() }
            verify(exactly = 0) { soundPlayer.prepare() }
        }

        @Test
        fun `GIVEN no video WHEN preloaded THEN it is failed without a load`() = runTest {
            // Arrange
            val player = createPlayer()

            // Act
            player.preloadVideo(null)

            // Assert
            assertThat(player.videoLoadState.value).isEqualTo(VideoLoadState.Failed)
            coVerify(exactly = 0) { mediaSourceFactory.create(any()) }
        }

        @Test
        fun `GIVEN a video WHEN preloaded THEN it is prepared but not played`() = runTest {
            // Arrange
            val player = createPlayer()

            // Act
            player.preloadVideo(asset)

            // Assert
            verify(exactly = 1) { videoPlayer.setMediaSource(videoSource) }
            verify(exactly = 1) { videoPlayer.prepare() }
            verify(exactly = 0) { videoPlayer.play() }
        }

        /** A video still preparing would otherwise report ready on behalf of the one replacing it. */
        @Test
        fun `GIVEN a prepared video WHEN another one is preloaded THEN the first is stopped before the other is set`() =
            runTest {
                // Arrange
                val player = createPlayer()
                player.preloadVideo(asset)

                // Act
                player.preloadVideo(otherAsset)

                // Assert
                verifyOrder {
                    videoPlayer.setMediaSource(videoSource)
                    videoPlayer.stop()
                    videoPlayer.setMediaSource(otherVideoSource)
                    videoPlayer.prepare()
                }
            }

        @Test
        fun `GIVEN a ready video WHEN another one is preloaded THEN it is loading until the other reports ready`() =
            runTest {
                // Arrange
                val player = createReadyPlayer()

                // Act
                player.preloadVideo(otherAsset)

                // Assert
                assertThat(player.videoLoadState.value).isEqualTo(VideoLoadState.Loading)
            }

        @Test
        fun `GIVEN started WHEN another video is preloaded THEN the playing one is kept`() = runTest {
            // Arrange
            val player = createReadyPlayer()
            player.start()

            // Act
            player.preloadVideo(otherAsset)

            // Assert
            coVerify(exactly = 0) { mediaSourceFactory.create(OTHER_VIDEO_URL) }
            assertThat(player.videoLoadState.value).isEqualTo(VideoLoadState.Ready)
        }

        @Test
        fun `GIVEN the video reports ready WHEN preloaded THEN the video is ready`() = runTest {
            // Arrange
            val player = createPlayer()
            player.preloadVideo(asset)

            // Act
            videoEvents().onPlaybackStateChanged(Player.STATE_READY)

            // Assert
            assertThat(player.videoLoadState.value).isEqualTo(VideoLoadState.Ready)
        }

        @Test
        fun `GIVEN the video cannot be fetched WHEN preloaded THEN it failed and the error is reported`() = runTest {
            // Arrange
            coEvery { mediaSourceFactory.create(VIDEO_URL) } throws IOException("no network")
            val player = createPlayer()

            // Act
            player.preloadVideo(asset)

            // Assert
            assertThat(player.videoLoadState.value).isEqualTo(VideoLoadState.Failed)
            verify(exactly = 1) { listener.onError(any()) }
        }

        @Test
        fun `GIVEN reduced motion WHEN preloaded THEN nothing is loaded and the video failed`() = runTest {
            // Arrange
            val player = createPlayer(isMotionEnabled = false)

            // Act
            player.preloadVideo(asset)

            // Assert
            assertThat(player.videoLoadState.value).isEqualTo(VideoLoadState.Failed)
            coVerify(exactly = 0) { mediaSourceFactory.create(any()) }
        }

        @Test
        fun `GIVEN not started WHEN playback fails THEN the video failed and no card is announced`() = runTest {
            // Arrange
            val player = createPlayer()
            player.preloadVideo(asset)

            // Act
            videoEvents().onPlayerError(mockk<PlaybackException>(relaxed = true))

            // Assert
            assertThat(player.videoLoadState.value).isEqualTo(VideoLoadState.Failed)
            verify(exactly = 0) { listener.onCardRevealReached() }
        }
    }

    @Nested
    inner class PreloadSound {

        @Test
        fun `GIVEN a sound WHEN preloaded THEN it is prepared but not played`() = runTest {
            // Arrange
            val player = createPlayer()

            // Act
            player.preloadSound(SOUND_URL)

            // Assert
            verify(exactly = 1) { soundPlayer.setMediaSource(soundSource) }
            verify(exactly = 1) { soundPlayer.prepare() }
            verify(exactly = 0) { soundPlayer.play() }
        }

        @Test
        fun `GIVEN no sound WHEN preloaded THEN nothing is loaded`() = runTest {
            // Arrange
            val player = createPlayer()

            // Act
            player.preloadSound(null)

            // Assert
            coVerify(exactly = 0) { mediaSourceFactory.create(any()) }
        }

        @Test
        fun `GIVEN the sound cannot be fetched WHEN preloaded THEN the video is unaffected`() = runTest {
            // Arrange
            coEvery { mediaSourceFactory.create(SOUND_URL) } throws IOException("no network")
            val player = createPlayer()
            player.preloadVideo(asset)

            // Act
            player.preloadSound(SOUND_URL)

            // Assert
            assertThat(player.videoLoadState.value).isEqualTo(VideoLoadState.Loading)
            verify(exactly = 1) { videoPlayer.prepare() }
        }

        @Test
        fun `GIVEN reduced motion WHEN preloaded THEN nothing is loaded`() = runTest {
            // Arrange
            val player = createPlayer(isMotionEnabled = false)

            // Act
            player.preloadSound(SOUND_URL)

            // Assert
            coVerify(exactly = 0) { mediaSourceFactory.create(any()) }
        }
    }

    @Nested
    inner class Start {

        @Test
        fun `GIVEN a ready video WHEN started THEN it plays and the card waits for the card reveal`() = runTest {
            // Arrange
            val player = createReadyPlayer()

            // Act
            player.start()

            // Assert
            verify(exactly = 1) { videoPlayer.play() }
            verify(exactly = 0) { listener.onCardRevealReached() }
        }

        @Test
        fun `GIVEN no ready video WHEN started THEN the card arrives without playing anything`() = runTest {
            // Arrange
            val player = createPlayer()
            player.preloadVideo(asset)

            // Act
            player.start()

            // Assert
            verify(exactly = 1) { listener.onCardRevealReached() }
            verify(exactly = 0) { videoPlayer.play() }
        }

        @Test
        fun `GIVEN started WHEN started again THEN it plays once`() = runTest {
            // Arrange
            val player = createReadyPlayer()
            player.start()

            // Act
            player.start()

            // Assert
            verify(exactly = 1) { videoPlayer.play() }
        }
    }

    @Nested
    inner class CardReveal {

        @Test
        fun `GIVEN started WHEN a frame past the card reveal is presented THEN the card phase begins`() = runTest {
            // Arrange
            createReadyPlayer().start()

            // Act
            frames().onVideoFrameAboutToBeRendered(CARD_REVEAL_MS.milliseconds.inWholeMicroseconds, 0L, mockk(), null)

            // Assert
            verify(exactly = 1) { listener.onCardRevealReached() }
        }

        @Test
        fun `GIVEN started WHEN a frame before the card reveal is presented THEN the pack keeps playing`() = runTest {
            // Arrange
            createReadyPlayer().start()

            // Act
            frames().onVideoFrameAboutToBeRendered((CARD_REVEAL_MS - 1).milliseconds.inWholeMicroseconds, 0L, mockk(), null)

            // Assert
            verify(exactly = 0) { listener.onCardRevealReached() }
        }

        @Test
        fun `GIVEN not started WHEN a frame past the card reveal is presented THEN no card is announced`() = runTest {
            // Arrange
            createReadyPlayer()

            // Act
            frames().onVideoFrameAboutToBeRendered(CARD_REVEAL_MS.milliseconds.inWholeMicroseconds, 0L, mockk(), null)

            // Assert
            verify(exactly = 0) { listener.onCardRevealReached() }
        }

        /** The backstop: a render whose picture never crosses the card reveal must not leave the reveal on black. */
        @Test
        fun `GIVEN started and no frame crossed the card reveal WHEN playback ends THEN the card arrives anyway`() = runTest {
            // Arrange
            createReadyPlayer().start()

            // Act
            videoEvents().onPlaybackStateChanged(Player.STATE_ENDED)

            // Assert
            verify(exactly = 1) { listener.onCardRevealReached() }
        }

        @Test
        fun `GIVEN the card reveal already fired WHEN playback ends THEN the card is not announced twice`() = runTest {
            // Arrange
            createReadyPlayer().start()
            frames().onVideoFrameAboutToBeRendered(CARD_REVEAL_MS.milliseconds.inWholeMicroseconds, 0L, mockk(), null)

            // Act
            videoEvents().onPlaybackStateChanged(Player.STATE_ENDED)

            // Assert
            verify(exactly = 1) { listener.onCardRevealReached() }
        }

        @Test
        fun `GIVEN started WHEN playback fails THEN it is reported and the card still arrives`() = runTest {
            // Arrange
            createReadyPlayer().start()

            // Act
            videoEvents().onPlayerError(mockk<PlaybackException>(relaxed = true))

            // Assert
            verify(exactly = 1) { listener.onError(any()) }
            verify(exactly = 1) { listener.onCardRevealReached() }
        }
    }

    @Nested
    inner class ShortSound {

        @Test
        fun `GIVEN the sound is ready WHEN played THEN it plays from the start`() = runTest {
            // Arrange
            val player = createReadyPlayer()

            // Act
            player.playShortSound()

            // Assert
            verifyOrder {
                soundPlayer.seekTo(0L)
                soundPlayer.play()
            }
        }

        @Test
        fun `GIVEN the sound is not ready WHEN played THEN nothing plays`() = runTest {
            // Arrange
            val player = createPlayer()
            player.preloadSound(SOUND_URL)

            // Act
            player.playShortSound()

            // Assert
            verify(exactly = 0) { soundPlayer.play() }
        }
    }

    @Nested
    inner class Skip {

        @Test
        fun `GIVEN playing before the burst WHEN skipped THEN seeks to the burst, mutes it and plays the sound`() =
            runTest {
                // Arrange
                val player = createReadyPlayer()
                player.start()
                every { videoPlayer.currentPosition } returns SKIP_TO_MS - 1

                // Act
                val isSkipped = player.skipToReveal()

                // Assert
                assertThat(isSkipped).isTrue()
                verify(exactly = 1) { videoPlayer.seekTo(SKIP_TO_MS) }
                verify(exactly = 1) { videoPlayer.volume = 0f }
                verify(exactly = 1) { soundPlayer.play() }
            }

        @Test
        fun `GIVEN playback already past the burst WHEN skipped THEN nothing is rewound`() = runTest {
            // Arrange
            val player = createReadyPlayer()
            player.start()
            every { videoPlayer.currentPosition } returns SKIP_TO_MS

            // Act
            val isSkipped = player.skipToReveal()

            // Assert
            assertThat(isSkipped).isFalse()
            verify(exactly = 0) { videoPlayer.seekTo(any<Long>()) }
        }

        @Test
        fun `GIVEN the card reveal already fired WHEN skipped THEN nothing is seeked`() = runTest {
            // Arrange
            val player = createReadyPlayer()
            player.start()
            frames().onVideoFrameAboutToBeRendered(CARD_REVEAL_MS.milliseconds.inWholeMicroseconds, 0L, mockk(), null)
            every { videoPlayer.currentPosition } returns 0L

            // Act
            val isSkipped = player.skipToReveal()

            // Assert
            assertThat(isSkipped).isFalse()
            verify(exactly = 0) { videoPlayer.seekTo(any<Long>()) }
        }

        @Test
        fun `GIVEN playback not started WHEN skipped THEN nothing is seeked`() = runTest {
            // Arrange
            val player = createReadyPlayer()
            every { videoPlayer.currentPosition } returns 0L

            // Act
            val isSkipped = player.skipToReveal()

            // Assert
            assertThat(isSkipped).isFalse()
            verify(exactly = 0) { videoPlayer.seekTo(any<Long>()) }
        }
    }

    @Test
    fun `GIVEN an attached surface WHEN detached THEN only that surface is cleared`() {
        // Arrange
        val player = createPlayer()
        player.attachSurface(surfaceView)

        // Act
        player.detachSurface(surfaceView)

        // Assert
        verify(exactly = 1) { videoPlayer.clearVideoSurfaceView(surfaceView) }
        verify(exactly = 0) { videoPlayer.clearVideoSurface() }
    }

    @Test
    fun `GIVEN a player WHEN released THEN the listeners are removed before the players`() {
        // Arrange
        val player = createPlayer()

        // Act
        player.release()

        // Assert
        verifyOrder {
            videoPlayer.removeListener(any())
            videoPlayer.release()
        }
        verifyOrder {
            soundPlayer.removeListener(any())
            soundPlayer.release()
        }
    }

    /** Preloaded, with both the video and the sound reporting ready. */
    private suspend fun createReadyPlayer(): DefaultPackRevealVideoPlayer {
        val player = createPlayer()
        player.preloadVideo(asset)
        player.preloadSound(SOUND_URL)
        videoEvents().onPlaybackStateChanged(Player.STATE_READY)
        soundEvents().onPlaybackStateChanged(Player.STATE_READY)
        return player
    }

    private fun videoEvents(): Player.Listener =
        slot<Player.Listener>().also { verify { videoPlayer.addListener(capture(it)) } }.captured

    private fun soundEvents(): Player.Listener =
        slot<Player.Listener>().also { verify { soundPlayer.addListener(capture(it)) } }.captured

    private fun frames(): VideoFrameMetadataListener =
        slot<VideoFrameMetadataListener>().also {
            verify { videoPlayer.setVideoFrameMetadataListener(capture(it)) }
        }.captured

    private fun createPlayer(isMotionEnabled: Boolean = true) = DefaultPackRevealVideoPlayer(
        videoPlayer = videoPlayer,
        soundPlayer = soundPlayer,
        mediaSourceFactory = mediaSourceFactory,
        isMotionEnabled = isMotionEnabled,
        listener = listener,
    )

    private companion object {

        const val VIDEO_URL = "https://cdn/reveal.mp4"
        const val OTHER_VIDEO_URL = "https://cdn/reveal-dark.mp4"
        const val SOUND_URL = "https://cdn/short.m4a"
        const val CARD_REVEAL_MS = 7_000L
        const val SKIP_TO_MS = 6_000L
    }
}