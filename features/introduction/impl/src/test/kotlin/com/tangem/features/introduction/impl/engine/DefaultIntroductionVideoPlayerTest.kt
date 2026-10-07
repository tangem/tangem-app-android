package com.tangem.features.introduction.impl.engine

import android.view.SurfaceView
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DecoderCounters
import com.google.common.truth.Truth.assertThat
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(UnstableApi::class)
internal class DefaultIntroductionVideoPlayerTest {

    private val player: Player = mockk(relaxed = true)
    private val videoItem: MediaItem = MediaItem.Builder().setMediaId("introduction_background").build()
    private val listener: IntroductionVideoPlayer.Listener = mockk(relaxed = true)
    private val surfaceView: SurfaceView = mockk(relaxed = true)
    private val otherSurfaceView: SurfaceView = mockk(relaxed = true)
    private val scheduler = FakeTeardownScheduler()

    @BeforeEach
    fun resetMocks() {
        clearMocks(player, listener)
    }

    @Test
    fun `GIVEN player created WHEN no surface attached yet THEN playback is not prepared`() {
        // Act
        createPlayer()

        // Assert
        verify(exactly = 0) { player.prepare() }
    }

    @Test
    fun `GIVEN player created WHEN surface attached twice THEN prepared once`() {
        // Arrange
        val videoPlayer = createPlayer()

        // Act
        videoPlayer.attachSurface(surfaceView)
        videoPlayer.attachSurface(otherSurfaceView)

        // Assert
        verify(exactly = 1) { player.prepare() }
        verify(exactly = 1) { player.setVideoSurfaceView(surfaceView) }
        verify(exactly = 1) { player.setVideoSurfaceView(otherSurfaceView) }
    }

    @Test
    fun `GIVEN player created WHEN looping configured THEN whole playlist repeats instead of a single item`() {
        // Arrange
        val items = slot<List<MediaItem>>()

        // Act
        createPlayer()

        // Assert
        verify { player.setMediaItems(capture(items)) }
        assertThat(items.captured.size).isGreaterThan(1)
        verify { player.repeatMode = Player.REPEAT_MODE_ALL }
    }

    @Test
    fun `GIVEN motion enabled WHEN running requested THEN playback starts`() {
        // Arrange
        val videoPlayer = createPlayer(isMotionEnabled = true)

        // Act
        videoPlayer.setRunning(isRunning = true)

        // Assert
        verify(exactly = 1) { player.play() }
    }

    @Test
    fun `GIVEN reduced motion WHEN running requested THEN playback stays paused on the first frame`() {
        // Arrange
        val videoPlayer = createPlayer(isMotionEnabled = false)

        // Act
        videoPlayer.setRunning(isRunning = true)

        // Assert
        verify(exactly = 0) { player.play() }
        verify(exactly = 1) { player.pause() }
    }

    @Test
    fun `GIVEN motion enabled WHEN running revoked THEN playback pauses`() {
        // Arrange
        val videoPlayer = createPlayer()

        // Act
        videoPlayer.setRunning(isRunning = false)

        // Assert
        verify(exactly = 1) { player.pause() }
        verify(exactly = 0) { player.play() }
    }

    @Test
    fun `GIVEN attached surface WHEN detached THEN only that surface is cleared`() {
        // Arrange
        val videoPlayer = createPlayer()
        videoPlayer.attachSurface(surfaceView)

        // Act
        videoPlayer.detachSurface(surfaceView)

        // Assert
        verify(exactly = 1) { player.clearVideoSurfaceView(surfaceView) }
        verify(exactly = 0) { player.clearVideoSurface() }
    }

    @Test
    fun `GIVEN player in use WHEN released THEN listener removed before release`() {
        // Arrange
        val videoPlayer = createPlayer()

        // Act
        videoPlayer.release()

        // Assert
        verify(exactly = 1) { player.removeListener(any()) }
        verify(exactly = 1) { player.release() }
    }

    @Test
    fun `GIVEN a released player WHEN a late surface is attached THEN the scheduled release still runs`() {
        // Arrange
        val pending = FakeTeardownScheduler(runsImmediately = false)
        val videoPlayer = createPlayer(teardownScheduler = pending)
        videoPlayer.release()

        // Act
        videoPlayer.attachSurface(surfaceView)
        videoPlayer.setRunning(isRunning = true)
        pending.scheduled?.invoke()

        // Assert
        verify(exactly = 0) { player.setVideoSurfaceView(any()) }
        verify(exactly = 0) { player.play() }
        verify(exactly = 1) { player.release() }
    }

    @Test
    fun `GIVEN a fresh player WHEN nothing is attached THEN no surface is reported`() {
        // Act
        val videoPlayer = createPlayer()

        // Assert
        assertThat(videoPlayer.isSurfaceAttached).isFalse()
    }

    @Test
    fun `GIVEN an attached surface WHEN the same one is detached THEN no surface is reported`() {
        // Arrange
        val videoPlayer = createPlayer()
        videoPlayer.attachSurface(surfaceView)
        assertThat(videoPlayer.isSurfaceAttached).isTrue()

        // Act
        videoPlayer.detachSurface(surfaceView)

        // Assert
        assertThat(videoPlayer.isSurfaceAttached).isFalse()
    }

    @Test
    fun `GIVEN a replacement surface WHEN the previous one is detached THEN the replacement stays bound`() {
        // Arrange
        val videoPlayer = createPlayer()
        videoPlayer.attachSurface(surfaceView)
        videoPlayer.attachSurface(otherSurfaceView)

        // Act
        videoPlayer.detachSurface(surfaceView)

        // Assert
        assertThat(videoPlayer.isSurfaceAttached).isTrue()
    }

    @Test
    fun `GIVEN an attached surface WHEN the player is released THEN no surface is reported`() {
        // Arrange
        val videoPlayer = createPlayer()
        videoPlayer.attachSurface(surfaceView)

        // Act
        videoPlayer.release()

        // Assert
        assertThat(videoPlayer.isSurfaceAttached).isFalse()
    }

    @Test
    fun `GIVEN a surface is detached WHEN the teardown is still pending THEN the surface is kept until it runs`() {
        // Arrange
        val pending = FakeTeardownScheduler(runsImmediately = false)
        val videoPlayer = createPlayer(teardownScheduler = pending)
        videoPlayer.attachSurface(surfaceView)

        // Act
        videoPlayer.detachSurface(surfaceView)

        // Assert
        verify(exactly = 0) { player.clearVideoSurfaceView(any()) }
        pending.scheduled?.invoke()
        verify(exactly = 1) { player.clearVideoSurfaceView(surfaceView) }
    }

    @Test
    fun `GIVEN a pending teardown WHEN a surface is attached again THEN the teardown is dropped`() {
        // Arrange
        val pending = FakeTeardownScheduler(runsImmediately = false)
        val videoPlayer = createPlayer(teardownScheduler = pending)
        videoPlayer.attachSurface(surfaceView)
        videoPlayer.detachSurface(surfaceView)

        // Act
        videoPlayer.attachSurface(otherSurfaceView)

        // Assert
        assertThat(pending.scheduled).isNull()
        verify(exactly = 0) { player.clearVideoSurfaceView(any()) }
    }

    @Test
    fun `GIVEN playback under way WHEN the position is read THEN it is the player position within the pass`() {
        // Arrange
        every { player.currentPosition } returns 4_200L
        val videoPlayer = createPlayer()

        // Act
        val positionMs = videoPlayer.positionMs

        // Assert
        assertThat(positionMs).isEqualTo(4_200L)
    }

    @Test
    fun `GIVEN playback under way WHEN restarted THEN the current pass rewinds to its first frame`() {
        // Arrange
        val videoPlayer = createPlayer()

        // Act
        videoPlayer.restart()

        // Assert
        verify(exactly = 1) { player.seekTo(0L) }
    }

    @Test
    fun `GIVEN a running decoder WHEN the output count is read THEN shown, skipped and dropped frames all count`() {
        // Arrange
        val counters = DecoderCounters().apply {
            renderedOutputBufferCount = 40
            skippedOutputBufferCount = 3
            droppedBufferCount = 2
        }
        val videoPlayer = createPlayer(decoderCounters = { counters })

        // Act
        val count = videoPlayer.outputFrameCount

        // Assert
        assertThat(count).isEqualTo(45L)
    }

    @Test
    fun `GIVEN no decoder yet WHEN the output count is read THEN it is unknown`() {
        // Arrange
        val videoPlayer = createPlayer(decoderCounters = { null })

        // Act
        val count = videoPlayer.outputFrameCount

        // Assert
        assertThat(count).isNull()
    }

    @Test
    fun `GIVEN media3 events WHEN the frame size and the playing state change THEN the listener hears both`() {
        // Arrange
        val playerListener = slot<Player.Listener>()
        every { player.addListener(capture(playerListener)) } just runs
        createPlayer()

        // Act
        playerListener.captured.onVideoSizeChanged(VideoSize(1080, 1920))
        playerListener.captured.onIsPlayingChanged(true)

        // Assert
        verify(exactly = 1) { listener.onVideoSizeChanged(width = 1080, height = 1920) }
        verify(exactly = 1) { listener.onPlayingChanged(isPlaying = true) }
    }

    private fun createPlayer(
        isMotionEnabled: Boolean = true,
        teardownScheduler: VideoTeardownScheduler = scheduler,
        decoderCounters: () -> DecoderCounters? = { null },
    ) = DefaultIntroductionVideoPlayer(
        player = player,
        decoderCounters = decoderCounters,
        videoItem = videoItem,
        isMotionEnabled = isMotionEnabled,
        listener = listener,
        teardownScheduler = teardownScheduler,
    )

    /** Stands in for the main thread the teardown is handed to; runs it on the spot unless asked to hold it. */
    private class FakeTeardownScheduler(private val runsImmediately: Boolean = true) : VideoTeardownScheduler {

        var scheduled: (() -> Unit)? = null
            private set

        var cancelCount = 0
            private set

        override fun schedule(action: () -> Unit) {
            scheduled = action
            if (runsImmediately) action()
        }

        override fun cancel() {
            scheduled = null
            cancelCount++
        }
    }
}