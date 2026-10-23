package com.tangem.core.media

import android.view.SurfaceView
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
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
import io.mockk.verifyOrder
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(UnstableApi::class)
internal class DefaultLoopingVideoPlayerTest {

    private val player: Player = mockk(relaxed = true)
    private val videoItem: MediaItem = MediaItem.Builder().setMediaId("looping_clip").build()
    private val listener: LoopingVideoPlayer.Listener = mockk(relaxed = true)
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
    fun `GIVEN player in use WHEN released THEN listener removed before release`() {
        // Arrange
        val videoPlayer = createPlayer()

        // Act
        videoPlayer.release()

        // Assert
        verifyOrder {
            player.removeListener(any())
            player.release()
        }
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
        val media3Listener = createPlayerHearingMedia3()

        // Act
        media3Listener.onVideoSizeChanged(VideoSize(1080, 1920))
        media3Listener.onIsPlayingChanged(true)

        // Assert
        verify(exactly = 1) { listener.onVideoSizeChanged(width = 1080, height = 1920) }
        verify(exactly = 1) { listener.onPlayingChanged(isPlaying = true) }
    }

    @Test
    fun `GIVEN media3 events WHEN a frame renders and playback fails THEN the listener hears both`() {
        // Arrange
        val media3Listener = createPlayerHearingMedia3()
        val error: PlaybackException = mockk()

        // Act
        media3Listener.onRenderedFirstFrame()
        media3Listener.onPlayerError(error)

        // Assert
        verify(exactly = 1) { listener.onFirstFrameRendered() }
        verify(exactly = 1) { listener.onError(error) }
    }

    @Test
    fun `GIVEN anamorphic frames WHEN their size is reported THEN the listener hears it in square pixels`() {
        // Arrange
        val media3Listener = createPlayerHearingMedia3()

        // Act
        media3Listener.onVideoSizeChanged(VideoSize(1440, 1920, 0.75f))

        // Assert
        verify(exactly = 1) { listener.onVideoSizeChanged(width = 1080, height = 1920) }
    }

    @Test
    fun `GIVEN nothing decoded yet WHEN an empty size is reported THEN the listener is not told`() {
        // Arrange
        val media3Listener = createPlayerHearingMedia3()

        // Act
        media3Listener.onVideoSizeChanged(VideoSize.UNKNOWN)

        // Assert
        verify(exactly = 0) { listener.onVideoSizeChanged(any(), any()) }
    }

    private fun createPlayer(
        isMotionEnabled: Boolean = true,
        teardownScheduler: VideoTeardownScheduler = scheduler,
        decoderCounters: () -> DecoderCounters? = { null },
    ) = DefaultLoopingVideoPlayer(
        player = player,
        decoderCounters = decoderCounters,
        videoItem = videoItem,
        isMotionEnabled = isMotionEnabled,
        listener = listener,
        teardownScheduler = teardownScheduler,
    )

    private fun createPlayerHearingMedia3(): Player.Listener {
        val media3Listener = slot<Player.Listener>()
        every { player.addListener(capture(media3Listener)) } just runs
        createPlayer()
        return media3Listener.captured
    }
}