package com.tangem.core.media

import android.net.Uri
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.runs
import io.mockk.slot
import io.mockk.unmockkStatic
import io.mockk.verify
import io.mockk.verifyOrder
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class SoundClipTest {

    private val player: Player = mockk(relaxed = true)
    private val cache: RemoteClipCache = mockk(relaxed = true)
    private val media3Listener = slot<Player.Listener>()

    @BeforeEach
    fun resetMocks() {
        clearMocks(player, cache)
        media3Listener.clear()
        every { player.addListener(capture(media3Listener)) } just runs
        every { cache.isFullyCached(any()) } returns false
        // `MediaItem.fromUri` parses through the Android stub, which unit tests do not implement.
        mockkStatic(Uri::class)
        every { Uri.parse(any()) } returns mockk(relaxed = true)
    }

    @AfterEach
    fun unmockUri() {
        unmockkStatic(Uri::class)
    }

    @Test
    fun `GIVEN a sound not cached yet WHEN prepared THEN it is downloaded before the player prepares it`() = runTest {
        // Arrange
        val clip = createClip()

        // Act
        clip.prepare(URL)

        // Assert
        coVerifyOrder {
            cache.download(URL)
            player.setMediaItem(any())
            player.prepare()
        }
    }

    @Test
    fun `GIVEN the download fails WHEN prepared THEN nothing is prepared`() = runTest {
        // Arrange
        coEvery { cache.download(URL) } throws IllegalStateException("offline")
        val clip = createClip()

        // Act
        clip.prepare(URL)

        // Assert
        verify(exactly = 0) { player.prepare() }
    }

    @Test
    fun `GIVEN a prepared sound WHEN played THEN it plays from its start`() = runTest {
        // Arrange
        val clip = createClip()
        clip.prepare(URL)
        media3Listener.captured.onPlaybackStateChanged(Player.STATE_READY)

        // Act
        clip.play()

        // Assert
        verifyOrder {
            player.seekTo(0L)
            player.play()
        }
    }

    @Test
    fun `GIVEN a sound not ready WHEN played THEN nothing plays`() {
        // Arrange
        val clip = createClip()

        // Act
        clip.play()

        // Assert
        verify(exactly = 0) { player.play() }
    }

    @Test
    fun `GIVEN a sound that failed after loading WHEN played THEN nothing plays`() = runTest {
        // Arrange
        val clip = createClip()
        clip.prepare(URL)
        media3Listener.captured.onPlaybackStateChanged(Player.STATE_READY)
        media3Listener.captured.onPlayerError(mockk<PlaybackException>(relaxed = true))

        // Act
        clip.play()

        // Assert
        verify(exactly = 0) { player.play() }
    }

    @Test
    fun `GIVEN a cached sound WHEN prepared THEN nothing is downloaded`() = runTest {
        // Arrange
        every { cache.isFullyCached(URL) } returns true
        val clip = createClip()

        // Act
        clip.prepare(URL)

        // Assert
        coVerify(exactly = 0) { cache.download(any()) }
        verify(exactly = 1) { player.prepare() }
    }

    @Test
    fun `GIVEN a sound WHEN released THEN its listener is removed before the player goes`() {
        // Arrange
        val clip = createClip()

        // Act
        clip.release()

        // Assert
        verifyOrder {
            player.removeListener(media3Listener.captured)
            player.release()
        }
    }

    private fun createClip() = SoundClip(player = player, cache = cache, diagnosticsSubject = "Test sound")

    private companion object {
        const val URL = "https://cdn.test/skip.mp3"
    }
}