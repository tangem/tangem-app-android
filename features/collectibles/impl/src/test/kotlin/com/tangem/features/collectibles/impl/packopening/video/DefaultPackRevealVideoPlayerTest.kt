package com.tangem.features.collectibles.impl.packopening.video

import com.google.common.truth.Truth.assertThat
import com.tangem.core.media.OneShotVideoController
import com.tangem.core.media.OneShotVideoController.CueReason
import com.tangem.core.media.RemoteClipCache
import com.tangem.core.media.SoundClip
import com.tangem.features.collectibles.impl.packopening.PackOpeningConst
import com.tangem.features.collectibles.impl.packopening.video.source.PackRevealAsset
import com.tangem.test.core.ProvideTestModels
import io.mockk.clearMocks
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

internal class DefaultPackRevealVideoPlayerTest {

    private val video: OneShotVideoController = mockk(relaxed = true)
    private val skipSound: SoundClip = mockk(relaxed = true)
    private val videoFactory: OneShotVideoController.Factory = mockk()
    private val soundFactory: SoundClip.Factory = mockk()
    private val cache: RemoteClipCache = mockk()
    private val listener: PackRevealVideoPlayer.Listener = mockk(relaxed = true)

    private val onCueReached = slot<(CueReason) -> Unit>()

    private val asset = PackRevealAsset(videoUrl = VIDEO_URL, cardRevealMs = CARD_REVEAL_MS, skipToMs = SKIP_TO_MS)

    @BeforeEach
    fun resetMocks() {
        clearMocks(video, skipSound, videoFactory, soundFactory, listener)
        onCueReached.clear()
        every {
            videoFactory.create(any(), cache, any(), PackOpeningConst.VIDEO_STALL_TIMEOUT_MS, capture(onCueReached))
        } returns video
        every { soundFactory.create(cache, any()) } returns skipSound
        every { video.isMotionEnabled } returns true
    }

    @Nested
    inner class PreloadVideo {

        @Test
        fun `GIVEN a video WHEN preloaded THEN it is prepared to reach the card at its reveal`() = runTest {
            // Arrange
            val player = createPlayer()

            // Act
            player.preloadVideo(asset)

            // Assert
            coVerify(exactly = 1) { video.prepare(url = VIDEO_URL, cueMs = CARD_REVEAL_MS) }
        }

        @Test
        fun `GIVEN no video WHEN preloaded THEN the engine is told there is none`() = runTest {
            // Arrange
            val player = createPlayer()

            // Act
            player.preloadVideo(null)

            // Assert
            coVerify(exactly = 1) { video.prepare(url = null, cueMs = any()) }
        }
    }

    @Nested
    inner class PreloadSound {

        @Test
        fun `GIVEN a sound WHEN preloaded THEN it is prepared`() = runTest {
            // Arrange
            val player = createPlayer()

            // Act
            player.preloadSkipSound(SOUND_URL)

            // Assert
            coVerify(exactly = 1) { skipSound.prepare(SOUND_URL) }
        }

        @Test
        fun `GIVEN no sound WHEN preloaded THEN nothing is loaded`() = runTest {
            // Arrange
            val player = createPlayer()

            // Act
            player.preloadSkipSound(null)

            // Assert
            coVerify(exactly = 0) { skipSound.prepare(any()) }
        }

        @Test
        fun `GIVEN animations off system-wide WHEN preloaded THEN nothing is loaded`() = runTest {
            // Arrange
            every { video.isMotionEnabled } returns false
            val player = createPlayer()

            // Act
            player.preloadSkipSound(SOUND_URL)

            // Assert
            coVerify(exactly = 0) { skipSound.prepare(any()) }
        }

        @Test
        fun `GIVEN a sound WHEN played on its own THEN the skip sound plays`() = runTest {
            // Arrange
            val player = createPlayer()

            // Act
            player.playSkipSound()

            // Assert
            verify(exactly = 1) { skipSound.play() }
        }
    }

    @Nested
    inner class Skip {

        @Test
        fun `GIVEN the engine accepts the jump WHEN skipped THEN the skip sound plays`() = runTest {
            // Arrange
            every { video.skipTo(SKIP_TO_MS) } returns true
            val player = createPlayer()
            player.preloadVideo(asset)

            // Act
            val isSkipped = player.skipToReveal()

            // Assert
            assertThat(isSkipped).isTrue()
            verify(exactly = 1) { skipSound.play() }
        }

        @Test
        fun `GIVEN the engine refuses the jump WHEN skipped THEN nothing plays`() = runTest {
            // Arrange
            every { video.skipTo(SKIP_TO_MS) } returns false
            val player = createPlayer()
            player.preloadVideo(asset)

            // Act
            val isSkipped = player.skipToReveal()

            // Assert
            assertThat(isSkipped).isFalse()
            verify(exactly = 0) { skipSound.play() }
        }

        @Test
        fun `GIVEN no video preloaded WHEN skipped THEN the engine is not asked`() = runTest {
            // Arrange
            val player = createPlayer()

            // Act
            val isSkipped = player.skipToReveal()

            // Assert
            assertThat(isSkipped).isFalse()
            verify(exactly = 0) { video.skipTo(any()) }
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class CardReveal {

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN a cue reason WHEN it arrives THEN the card begins and only a stall plays the skip sound`(
            model: CardRevealModel,
        ) = runTest {
            // Arrange
            createPlayer()

            // Act
            onCueReached.captured(model.reason)

            // Assert
            verify(exactly = 1) { listener.onCardRevealReached() }
            verify(exactly = if (model.isSkipSoundPlayed) 1 else 0) { skipSound.play() }
        }

        private fun provideTestModels() = listOf(
            CardRevealModel(reason = CueReason.Shown, isSkipSoundPlayed = false),
            CardRevealModel(reason = CueReason.Ended, isSkipSoundPlayed = false),
            CardRevealModel(reason = CueReason.Failed, isSkipSoundPlayed = false),
            CardRevealModel(reason = CueReason.Stalled, isSkipSoundPlayed = true),
            CardRevealModel(reason = CueReason.NotReady, isSkipSoundPlayed = false),
        )
    }

    @Nested
    inner class Playback {

        @Test
        fun `GIVEN a player WHEN started THEN the video starts`() = runTest {
            // Arrange
            val player = createPlayer()

            // Act
            player.start()

            // Assert
            verify(exactly = 1) { video.start() }
        }

        @ParameterizedTest
        @ValueSource(booleans = [true, false])
        fun `GIVEN a player WHEN the screen comes and goes THEN the video and the sound are told`(isRunning: Boolean) =
            runTest {
                // Arrange
                val player = createPlayer()

                // Act
                player.setRunning(isRunning)

                // Assert
                verify(exactly = 1) { video.setRunning(isRunning) }
                verify(exactly = 1) { skipSound.setRunning(isRunning) }
            }

        @Test
        fun `GIVEN a player WHEN released THEN the video and the skip sound are released`() = runTest {
            // Arrange
            val player = createPlayer()

            // Act
            player.release()

            // Assert
            verify(exactly = 1) { video.release() }
            verify(exactly = 1) { skipSound.release() }
        }
    }

    private fun TestScope.createPlayer() = DefaultPackRevealVideoPlayer(
        scope = backgroundScope,
        cache = cache,
        videoFactory = videoFactory,
        soundFactory = soundFactory,
        listener = listener,
    )

    internal data class CardRevealModel(val reason: CueReason, val isSkipSoundPlayed: Boolean)

    private companion object {
        const val VIDEO_URL = "https://cdn.test/reveal-light.mp4"
        const val SOUND_URL = "https://cdn.test/skip.mp3"
        const val CARD_REVEAL_MS = 7_000L
        const val SKIP_TO_MS = 6_100L
    }
}