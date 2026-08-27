package com.tangem.features.introduction.impl.engine

import android.view.SurfaceView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import com.google.common.truth.Truth.assertThat
import io.mockk.clearMocks
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class DefaultIntroductionVideoPlayerTest {

    private val player: Player = mockk(relaxed = true)
    private val videoItem: MediaItem = MediaItem.Builder().setMediaId("introduction_background").build()
    private val listener: IntroductionVideoPlayer.Listener = mockk(relaxed = true)
    private val surfaceView: SurfaceView = mockk(relaxed = true)
    private val otherSurfaceView: SurfaceView = mockk(relaxed = true)

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

    private fun createPlayer(isMotionEnabled: Boolean = true) = DefaultIntroductionVideoPlayer(
        player = player,
        videoItem = videoItem,
        isMotionEnabled = isMotionEnabled,
        listener = listener,
    )
}