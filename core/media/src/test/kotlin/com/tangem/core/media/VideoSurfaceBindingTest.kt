package com.tangem.core.media

import android.view.SurfaceView
import androidx.media3.common.Player
import com.google.common.truth.Truth.assertThat
import io.mockk.clearMocks
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

internal class VideoSurfaceBindingTest {

    private val player: Player = mockk(relaxed = true)
    private val surfaceView: SurfaceView = mockk(relaxed = true)
    private val otherSurfaceView: SurfaceView = mockk(relaxed = true)

    @BeforeEach
    fun resetMocks() {
        clearMocks(player)
    }

    @Nested
    inner class Attach {

        @Test
        fun `GIVEN a fresh binding WHEN nothing is attached THEN no surface is reported`() {
            // Act
            val binding = createBinding()

            // Assert
            assertThat(binding.isAttached).isFalse()
        }

        @Test
        fun `GIVEN a fresh binding WHEN a surface is attached THEN the player renders into it`() {
            // Arrange
            val binding = createBinding()

            // Act
            val isBound = binding.attach(surfaceView)

            // Assert
            assertThat(isBound).isTrue()
            assertThat(binding.isAttached).isTrue()
            verify(exactly = 1) { player.setVideoSurfaceView(surfaceView) }
        }

        @Test
        fun `GIVEN a pending teardown WHEN a surface is attached again THEN the teardown is dropped`() {
            // Arrange
            val scheduler = FakeTeardownScheduler(runsImmediately = false)
            val binding = createBinding(scheduler)
            binding.attach(surfaceView)
            binding.detach(surfaceView)

            // Act
            binding.attach(otherSurfaceView)

            // Assert
            assertThat(scheduler.scheduled).isNull()
            verify(exactly = 0) { player.clearVideoSurfaceView(any()) }
        }

        @Test
        fun `GIVEN a released binding WHEN a late surface is attached THEN it stays unbound and the release runs`() {
            // Arrange
            val scheduler = FakeTeardownScheduler(runsImmediately = false)
            val binding = createBinding(scheduler)
            binding.release()

            // Act
            val isBound = binding.attach(surfaceView)
            scheduler.scheduled?.invoke()

            // Assert
            assertThat(isBound).isFalse()
            assertThat(binding.isAttached).isFalse()
            verify(exactly = 0) { player.setVideoSurfaceView(any()) }
            verify(exactly = 1) { player.release() }
        }
    }

    @Nested
    inner class Detach {

        @Test
        fun `GIVEN an attached surface WHEN it is detached THEN only that surface is cleared`() {
            // Arrange
            val binding = createBinding()
            binding.attach(surfaceView)

            // Act
            binding.detach(surfaceView)

            // Assert
            assertThat(binding.isAttached).isFalse()
            verify(exactly = 1) { player.clearVideoSurfaceView(surfaceView) }
            verify(exactly = 0) { player.clearVideoSurface() }
        }

        @Test
        fun `GIVEN a replacement surface WHEN the previous one is detached THEN the replacement stays bound`() {
            // Arrange
            val binding = createBinding()
            binding.attach(surfaceView)
            binding.attach(otherSurfaceView)

            // Act
            binding.detach(surfaceView)

            // Assert
            assertThat(binding.isAttached).isTrue()
            verify(exactly = 0) { player.clearVideoSurfaceView(any()) }
        }

        @Test
        fun `GIVEN a surface is detached WHEN the teardown is still pending THEN the surface is kept until it runs`() {
            // Arrange
            val scheduler = FakeTeardownScheduler(runsImmediately = false)
            val binding = createBinding(scheduler)
            binding.attach(surfaceView)

            // Act
            binding.detach(surfaceView)

            // Assert
            verify(exactly = 0) { player.clearVideoSurfaceView(any()) }
            assertThat(scheduler.scheduled).isNotNull()
        }
    }

    @Nested
    inner class Release {

        @Test
        fun `GIVEN an attached surface WHEN released THEN no surface is reported and the player is released`() {
            // Arrange
            val binding = createBinding()
            binding.attach(surfaceView)

            // Act
            binding.release()

            // Assert
            assertThat(binding.isAttached).isFalse()
            assertThat(binding.isReleased).isTrue()
            verify(exactly = 1) { player.release() }
        }

        @Test
        fun `GIVEN a pending detach WHEN released THEN only the release runs`() {
            // Arrange
            val scheduler = FakeTeardownScheduler(runsImmediately = false)
            val binding = createBinding(scheduler)
            binding.attach(surfaceView)
            binding.detach(surfaceView)

            // Act
            binding.release()
            scheduler.scheduled?.invoke()

            // Assert
            verify(exactly = 0) { player.clearVideoSurfaceView(any()) }
            verify(exactly = 1) { player.release() }
        }
    }

    private fun createBinding(scheduler: VideoTeardownScheduler = FakeTeardownScheduler()) = VideoSurfaceBinding(
        player = player,
        teardownScheduler = scheduler,
    )
}