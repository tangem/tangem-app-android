package com.tangem.features.storiesv2.impl.content

import com.google.common.truth.Truth.assertThat
import com.tangem.features.storiesv2.StoryV2Type
import com.tangem.features.storiesv2.impl.content.remote.RemoteStoryV2ContentSource
import com.tangem.features.storiesv2.impl.content.remote.RemoteStoryV2Load
import com.tangem.test.core.ProvideTestModels
import com.tangem.test.core.TestAppCoroutineScope
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.Called
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class DefaultStoryV2ContentRepositoryTest {

    private val bundledSource: BundledStoryV2ContentSource = mockk()
    private val remoteSource: RemoteStoryV2ContentSource = mockk()
    private val previewContent = StoryV2PreviewContent()
    private val dispatchers = TestingCoroutineDispatcherProvider()

    @BeforeEach
    fun setUp() {
        clearMocks(bundledSource, remoteSource)
        previewContent.composition = null
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class Load {

        @Test
        fun `GIVEN two loads at once WHEN the remote story resolves THEN the remote source is asked once`() = runTest {
            // Arrange
            val repository = createRepository()
            val remoteLoad = CompletableDeferred<RemoteStoryV2Load>()
            coEvery { remoteSource.load(slug = HARDWARE_UPGRADE_SLUG, language = any()) } coAnswers {
                remoteLoad.await()
            }
            val first = async { repository.load(StoryV2Type.HARDWARE_UPGRADE) }
            val second = async { repository.load(StoryV2Type.HARDWARE_UPGRADE) }
            runCurrent()

            // Act
            remoteLoad.complete(RemoteStoryV2Load.Ready(remoteComposition()))
            val actual = awaitAll(first, second)

            // Assert
            val ready = StoryV2ContentLoad.Ready(remoteComposition())
            assertThat(actual).containsExactly(ready, ready)
            coVerify(exactly = 1) { remoteSource.load(slug = HARDWARE_UPGRADE_SLUG, language = any()) }
        }

        @Test
        fun `GIVEN a ready remote story WHEN load again THEN the kept story is returned without asking again`() =
            runTest {
                // Arrange
                val repository = createRepository()
                coEvery { remoteSource.load(slug = any(), language = any()) } returns
                    RemoteStoryV2Load.Ready(remoteComposition())
                repository.load(StoryV2Type.HARDWARE_UPGRADE)

                // Act
                val actual = repository.load(StoryV2Type.HARDWARE_UPGRADE)

                // Assert
                assertThat(actual).isEqualTo(StoryV2ContentLoad.Ready(remoteComposition()))
                coVerify(exactly = 1) { remoteSource.load(slug = any(), language = any()) }
            }

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN a remote load that is not ready WHEN load again THEN the remote source is asked again`(
            model: NotReadyModel,
        ) = runTest {
            // Arrange
            val repository = createRepository()
            coEvery { remoteSource.load(slug = any(), language = any()) } returns model.remoteLoad
            repository.load(StoryV2Type.HARDWARE_UPGRADE)

            // Act
            val actual = repository.load(StoryV2Type.HARDWARE_UPGRADE)

            // Assert
            assertThat(actual).isEqualTo(model.expected)
            coVerify(exactly = 2) { remoteSource.load(slug = any(), language = any()) }
        }

        @Test
        fun `GIVEN a remote story with nothing showable WHEN load THEN it is unavailable`() = runTest {
            // Arrange
            val repository = createRepository()
            val composition = remoteComposition(slides = listOf(slide(id = "broken", durationMs = 0L)))
            coEvery { remoteSource.load(slug = any(), language = any()) } returns RemoteStoryV2Load.Ready(composition)

            // Act
            val actual = repository.load(StoryV2Type.HARDWARE_UPGRADE)

            // Assert
            assertThat(actual).isEqualTo(StoryV2ContentLoad.Unavailable)
        }

        @Test
        fun `GIVEN a preview composition WHEN load THEN it is returned through the validator`() = runTest {
            // Arrange
            val repository = createRepository()
            previewContent.composition = remoteComposition(
                slides = listOf(slide(id = "good"), slide(id = "broken", durationMs = 0L)),
            )

            // Act
            val actual = repository.load(StoryV2Type.HARDWARE_UPGRADE)

            // Assert
            assertThat(actual).isEqualTo(
                StoryV2ContentLoad.Ready(remoteComposition(slides = listOf(slide(id = "good")))),
            )
            verify { bundledSource wasNot Called }
            verify { remoteSource wasNot Called }
        }

        @Test
        fun `GIVEN the onboarding type WHEN load THEN the bundled story is returned`() = runTest {
            // Arrange
            val repository = createRepository()
            val bundled = remoteComposition().copy(id = "onboarding", origin = StoryV2Origin.BUNDLED)
            every { bundledSource.onboarding() } returns bundled

            // Act
            val actual = repository.load(StoryV2Type.ONBOARDING)

            // Assert
            assertThat(actual).isEqualTo(StoryV2ContentLoad.Ready(bundled))
            verify { remoteSource wasNot Called }
        }

        @Test
        fun `GIVEN the shipped onboarding WHEN load THEN it is the validated bundled composition`() = runTest {
            // Arrange
            val shipped = BundledStoryV2ContentSource()
            every { bundledSource.onboarding() } answers { shipped.onboarding() }
            val repository = createRepository()

            // Act
            val actual = repository.load(StoryV2Type.ONBOARDING)

            // Assert
            val expected = requireNotNull(StoryV2CompositionValidator.validate(shipped.onboarding()))
            assertThat(actual).isEqualTo(StoryV2ContentLoad.Ready(expected))
        }

        @Test
        fun `GIVEN the onboarding type WHEN load THEN it resolves without suspending`() = runTest {
            // Arrange
            val repository = createRepository()
            every { bundledSource.onboarding() } returns remoteComposition().copy(origin = StoryV2Origin.BUNDLED)

            // Act
            val load = async(start = CoroutineStart.UNDISPATCHED) { repository.load(StoryV2Type.ONBOARDING) }

            // Assert
            assertThat(load.isCompleted).isTrue()
        }

        @Test
        fun `GIVEN the onboarding loaded once WHEN load again THEN the bundled source is not asked again`() = runTest {
            // Arrange
            val repository = createRepository()
            every { bundledSource.onboarding() } returns remoteComposition().copy(origin = StoryV2Origin.BUNDLED)
            repository.load(StoryV2Type.ONBOARDING)

            // Act
            repository.load(StoryV2Type.ONBOARDING)

            // Assert
            verify(exactly = 1) { bundledSource.onboarding() }
        }

        private fun provideTestModels() = listOf(
            NotReadyModel(remoteLoad = RemoteStoryV2Load.Failed, expected = StoryV2ContentLoad.Failed),
            NotReadyModel(remoteLoad = RemoteStoryV2Load.NotFound, expected = StoryV2ContentLoad.Unavailable),
            NotReadyModel(remoteLoad = RemoteStoryV2Load.AlreadyShown, expected = StoryV2ContentLoad.AlreadyShown),
        )
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class MarkShown {

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN a story type WHEN markShown THEN only a remote story is recorded under its slug`(
            model: MarkShownModel,
        ) = runTest {
            // Arrange
            val repository = createRepository()
            val slugs = mutableListOf<String>()
            coEvery { remoteSource.markShown(capture(slugs)) } returns false

            // Act
            repository.markShown(model.type)

            // Assert
            assertThat(slugs).isEqualTo(model.expectedSlugs)
        }

        @ParameterizedTest
        @MethodSource("provideCacheModels")
        fun `GIVEN a kept remote story WHEN markShown THEN the cache is dropped only for a recorded showing`(
            model: CacheModel,
        ) = runTest {
            // Arrange
            val repository = createRepository()
            coEvery { remoteSource.load(slug = any(), language = any()) } returns
                RemoteStoryV2Load.Ready(remoteComposition())
            coEvery { remoteSource.markShown(any()) } returns model.isRecorded
            repository.load(StoryV2Type.HARDWARE_UPGRADE)

            // Act
            repository.markShown(StoryV2Type.HARDWARE_UPGRADE)
            repository.load(StoryV2Type.HARDWARE_UPGRADE)

            // Assert
            coVerify(exactly = model.expectedRemoteLoads) { remoteSource.load(slug = any(), language = any()) }
        }

        private fun provideCacheModels() = listOf(
            CacheModel(isRecorded = true, expectedRemoteLoads = 2),
            CacheModel(isRecorded = false, expectedRemoteLoads = 1),
        )

        private fun provideTestModels() = listOf(
            MarkShownModel(type = StoryV2Type.HARDWARE_UPGRADE, expectedSlugs = listOf(HARDWARE_UPGRADE_SLUG)),
            MarkShownModel(type = StoryV2Type.ONBOARDING, expectedSlugs = emptyList()),
        )
    }

    @Nested
    inner class ClearRemoteContent {

        @Test
        fun `GIVEN a kept remote story WHEN remote content cleared THEN the next load asks the remote source again`() =
            runTest {
                // Arrange
                val repository = createRepository()
                coEvery { remoteSource.load(slug = any(), language = any()) } returns
                    RemoteStoryV2Load.Ready(remoteComposition())
                coEvery { remoteSource.clear() } just runs
                repository.load(StoryV2Type.HARDWARE_UPGRADE)

                // Act
                repository.clearRemoteContent()
                repository.load(StoryV2Type.HARDWARE_UPGRADE)

                // Assert
                coVerify(exactly = 2) { remoteSource.load(slug = any(), language = any()) }
                coVerify(exactly = 1) { remoteSource.clear() }
            }

        @Test
        fun `GIVEN a load in flight WHEN remote content cleared THEN the load stops and its waiter can retry`() =
            runTest {
                // Arrange
                val repository = createRepository()
                var isRemoteLoadCancelled = false
                coEvery { remoteSource.load(slug = any(), language = any()) } coAnswers {
                    try {
                        awaitCancellation()
                    } finally {
                        isRemoteLoadCancelled = true
                    }
                }
                coEvery { remoteSource.clear() } just runs
                val waiter = async { repository.load(StoryV2Type.HARDWARE_UPGRADE) }
                runCurrent()

                // Act
                repository.clearRemoteContent()

                // Assert
                assertThat(waiter.await()).isEqualTo(StoryV2ContentLoad.Failed)
                assertThat(isRemoteLoadCancelled).isTrue()
            }
    }

    private fun TestScope.createRepository() = DefaultStoryV2ContentRepository(
        bundledSource = bundledSource,
        remoteSource = remoteSource,
        previewContent = previewContent,
        appScope = TestAppCoroutineScope(backgroundScope.coroutineContext),
        dispatchers = dispatchers,
    )

    private fun remoteComposition(slides: List<StoryV2Slide> = listOf(slide(id = "1"))) = StoryV2Composition(
        id = HARDWARE_UPGRADE_SLUG,
        version = 0,
        origin = StoryV2Origin.REMOTE,
        endBehavior = StoryV2EndBehavior.LOOP,
        actions = emptyList(),
        slides = slides,
    )

    private fun slide(id: String, durationMs: Long = 5_000L) = StoryV2Slide(
        id = id,
        title = null,
        subtitle = null,
        asset = StoryV2Asset.Video(
            source = StoryV2MediaRef.LocalFile(path = "/stories_v2/assets/$id.mp4"),
            poster = null,
            durationMs = durationMs,
        ),
    )

    internal data class NotReadyModel(val remoteLoad: RemoteStoryV2Load, val expected: StoryV2ContentLoad)

    internal data class MarkShownModel(val type: StoryV2Type, val expectedSlugs: List<String>)

    internal data class CacheModel(val isRecorded: Boolean, val expectedRemoteLoads: Int)

    private companion object {
        const val HARDWARE_UPGRADE_SLUG = "hardware_upgrade"
    }
}