package com.tangem.core.media

import android.net.Uri
import androidx.annotation.OptIn
import androidx.media3.common.Format
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.video.VideoFrameMetadataListener
import com.google.common.truth.Truth.assertThat
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.runs
import io.mockk.slot
import io.mockk.unmockkStatic
import io.mockk.verify
import io.mockk.verifyOrder
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@OptIn(UnstableApi::class)
internal class DefaultOneShotVideoPlayerTest {

    private val exoPlayer: ExoPlayer = mockk(relaxed = true)
    private val listener: OneShotVideoPlayer.Listener = mockk(relaxed = true)
    private val format: Format = mockk()
    private val uri: Uri = mockk(relaxed = true)

    private val media3Listener = slot<Player.Listener>()
    private val frameListener = slot<VideoFrameMetadataListener>()
    private val postedToMain = mutableListOf<() -> Unit>()

    @BeforeEach
    fun resetMocks() {
        clearMocks(exoPlayer, listener)
        media3Listener.clear()
        frameListener.clear()
        postedToMain.clear()
        every { exoPlayer.addListener(capture(media3Listener)) } just runs
        every { exoPlayer.setVideoFrameMetadataListener(capture(frameListener)) } just runs
        // `MediaItem.fromUri` parses through the Android stub, which unit tests do not implement.
        mockkStatic(Uri::class)
        every { Uri.parse(any()) } returns uri
    }

    @AfterEach
    fun unmockUri() {
        unmockkStatic(Uri::class)
    }

    @Nested
    inner class Cue {

        @Test
        fun `GIVEN a prepared clip WHEN a frame past the cue is shown THEN the cue reaches the main thread once`() {
            // Arrange
            createPlayer().prepare(url = URL, cueMs = CUE_MS)

            // Act
            presentFrame(atMs = CUE_MS + 40L)
            presentFrame(atMs = CUE_MS + 80L)
            runPostedToMain()

            // Assert
            verify(exactly = 1) { listener.onCueFramePresented() }
        }

        @Test
        fun `GIVEN a prepared clip WHEN a frame before the cue is shown THEN no cue is reported`() {
            // Arrange
            createPlayer().prepare(url = URL, cueMs = CUE_MS)

            // Act
            presentFrame(atMs = CUE_MS - 40L)
            runPostedToMain()

            // Assert
            verify(exactly = 0) { listener.onCueFramePresented() }
        }

        @Test
        fun `GIVEN the cue already shown WHEN the clip is prepared again THEN its cue is reported anew`() {
            // Arrange
            val player = createPlayer()
            player.prepare(url = URL, cueMs = CUE_MS)
            presentFrame(atMs = CUE_MS)

            // Act
            player.prepare(url = URL, cueMs = CUE_MS)
            presentFrame(atMs = CUE_MS)
            runPostedToMain()

            // Assert
            verify(exactly = 2) { listener.onCueFramePresented() }
        }

        @Test
        fun `GIVEN a cue on its way to the main thread WHEN released first THEN it is not reported`() {
            // Arrange
            val player = createPlayer()
            player.prepare(url = URL, cueMs = CUE_MS)
            presentFrame(atMs = CUE_MS)

            // Act
            player.release()
            runPostedToMain()

            // Assert
            verify(exactly = 0) { listener.onCueFramePresented() }
        }

        @Test
        fun `GIVEN frames shown WHEN the count is read THEN every presented frame counts`() {
            // Arrange
            val player = createPlayer()
            player.prepare(url = URL, cueMs = CUE_MS)

            // Act
            repeat(times = 3) { index -> presentFrame(atMs = index * 40L) }

            // Assert
            assertThat(player.presentedFrameCount).isEqualTo(3L)
        }
    }

    @Test
    fun `GIVEN media3 events WHEN the clip gets ready, ends and fails THEN the listener hears each`() {
        // Arrange
        createPlayer()
        val error: PlaybackException = mockk()

        // Act
        media3Listener.captured.onPlaybackStateChanged(Player.STATE_READY)
        media3Listener.captured.onPlaybackStateChanged(Player.STATE_ENDED)
        media3Listener.captured.onPlayerError(error)

        // Assert
        verifyOrder {
            listener.onReady()
            listener.onEnded()
            listener.onError(error)
        }
    }

    @Test
    fun `GIVEN a downloaded clip WHEN prepared THEN the player loads it from its url`() {
        // Arrange
        val item = slot<MediaItem>()
        every { exoPlayer.setMediaItem(capture(item)) } just runs
        val player = createPlayer()

        // Act
        player.prepare(url = URL, cueMs = CUE_MS)

        // Assert
        verify(exactly = 1) { Uri.parse(URL) }
        assertThat(item.captured.localConfiguration?.uri).isSameInstanceAs(uri)
        verify(exactly = 1) { exoPlayer.prepare() }
    }

    @Nested
    inner class Release {

        @Test
        fun `GIVEN a playing clip WHEN released THEN its sound stops at once and media3 waits for the teardown`() {
            // Arrange
            val player = createPlayer(teardownScheduler = FakeTeardownScheduler(runsImmediately = false))

            // Act
            player.release()

            // Assert
            verifyOrder {
                exoPlayer.removeListener(media3Listener.captured)
                exoPlayer.pause()
            }
            verify(exactly = 0) { exoPlayer.release() }
        }

        @Test
        fun `GIVEN a released player WHEN the teardown runs THEN media3 is released`() {
            // Arrange
            val scheduler = FakeTeardownScheduler(runsImmediately = false)
            createPlayer(teardownScheduler = scheduler).release()

            // Act
            scheduler.scheduled?.invoke()

            // Assert
            verify(exactly = 1) { exoPlayer.release() }
        }

        @Test
        fun `GIVEN a released player WHEN released again THEN nothing more reaches media3`() {
            // Arrange
            val player = createPlayer(teardownScheduler = FakeTeardownScheduler(runsImmediately = false))
            player.release()

            // Act
            player.release()

            // Assert
            verify(exactly = 1) { exoPlayer.removeListener(any()) }
            verify(exactly = 1) { exoPlayer.pause() }
        }

        @Test
        fun `GIVEN a released player WHEN a download finishes or it is played THEN nothing reaches media3`() {
            // Arrange
            val player = createPlayer(teardownScheduler = FakeTeardownScheduler(runsImmediately = false))
            player.release()

            // Act
            player.stop()
            player.prepare(url = URL, cueMs = CUE_MS)
            player.play()
            player.seekTo(positionMs = 6_100L)
            player.mute()

            // Assert
            verify(exactly = 0) { exoPlayer.stop() }
            verify(exactly = 0) { exoPlayer.setMediaItem(any()) }
            verify(exactly = 0) { exoPlayer.prepare() }
            verify(exactly = 0) { exoPlayer.play() }
            verify(exactly = 0) { exoPlayer.seekTo(any()) }
            verify(exactly = 0) { exoPlayer.volume = any() }
        }
    }

    private fun presentFrame(atMs: Long) {
        frameListener.captured.onVideoFrameAboutToBeRendered(atMs * MICROS_PER_MILLI, 0L, format, null)
    }

    private fun runPostedToMain() {
        postedToMain.toList().forEach { action -> action() }
        postedToMain.clear()
    }

    private fun createPlayer(teardownScheduler: VideoTeardownScheduler = FakeTeardownScheduler()) =
        DefaultOneShotVideoPlayer(
            player = exoPlayer,
            listener = listener,
            teardownScheduler = teardownScheduler,
            postToMain = { action -> postedToMain += action },
        )

    private companion object {
        const val URL = "https://cdn.test/reveal.mp4"
        const val CUE_MS = 7_000L
        const val MICROS_PER_MILLI = 1_000L
    }
}