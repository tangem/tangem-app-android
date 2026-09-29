package com.tangem.features.storiesv2.impl.content.remote

import com.google.common.truth.Truth.assertThat
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.analytics.models.AnalyticsEvent
import com.tangem.core.remote.response.ApiResponse
import com.tangem.core.remote.response.ApiResponseError
import com.tangem.core.ui.extensions.stringReference
import com.squareup.moshi.JsonDataException
import com.tangem.datasource.api.tangemTech.TangemTechApi
import com.tangem.datasource.api.tangemTech.models.storiesv2.StoriesV2Response
import com.tangem.features.storiesv2.impl.analytics.StoriesV2Events
import com.tangem.features.storiesv2.impl.analytics.StoryV2ErrorReason
import com.tangem.features.storiesv2.impl.content.StoryV2Asset
import com.tangem.features.storiesv2.impl.content.StoryV2Composition
import com.tangem.features.storiesv2.impl.content.StoryV2ContentMode
import com.tangem.features.storiesv2.impl.content.StoryV2EndBehavior
import com.tangem.features.storiesv2.impl.content.StoryV2MediaRef
import com.tangem.features.storiesv2.impl.content.StoryV2Origin
import com.tangem.features.storiesv2.impl.content.StoryV2Slide
import com.tangem.test.core.ProvideTestModels
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import java.io.File

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class RemoteStoryV2ContentSourceTest {

    private val tangemTechApi: TangemTechApi = mockk()
    private val localStore: StoryV2LocalStore = mockk(relaxUnitFun = true)
    private val assetStore: StoryV2AssetStore = mockk(relaxUnitFun = true)
    private val analytics: AnalyticsEventHandler = mockk(relaxed = true)

    private lateinit var source: RemoteStoryV2ContentSource

    private val body: StoriesV2Response = mapOf(
        MAIN_PLACEMENT to listOf(
            storyDto(
                slides = listOf(
                    slideDto(id = "1", subtitle = "", hapticAtMs = listOf(100)),
                    slideDto(id = "2", assetType = "lottie"),
                    slideDto(id = "3", assetType = "image", contentMode = "cover"),
                ),
                actions = listOf(actionDto()),
            ),
        ),
    )

    @BeforeEach
    fun setUp() {
        clearMocks(tangemTechApi, localStore, assetStore, analytics)
        coEvery { localStore.savedResponse(language = any(), slugs = any()) } returns null
        coEvery { localStore.shownSlugs() } returns emptySet()
        coEvery { assetStore.resolve(file = any(), keep = any()) } coAnswers {
            StoryV2AssetResult.Ready(fileOf(firstArg<RemoteStoryV2File>().url))
        }
        source = RemoteStoryV2ContentSource(
            tangemTechApi = tangemTechApi,
            localStore = localStore,
            assetStore = assetStore,
            analytics = analytics,
        )
    }

    @Nested
    inner class Delivery {

        @Test
        fun `GIVEN a successful response WHEN load THEN the story is ready on local files without posters`() = runTest {
            // Arrange
            givenResponse(body)

            // Act
            val actual = source.load(slug = SLUG, language = "en")

            // Assert
            assertThat(actual).isEqualTo(RemoteStoryV2Load.Ready(expectedComposition()))
        }

        @Test
        fun `GIVEN a successful response WHEN load THEN the response is saved with its question`() = runTest {
            // Arrange
            givenResponse(body)

            // Act
            source.load(slug = SLUG, language = "en")

            // Assert
            coVerify(exactly = 1) { localStore.saveResponse(language = "en", slugs = listOf(SLUG), response = body) }
        }

        @Test
        fun `GIVEN a successful response WHEN load THEN only files of the returned stories are retained`() = runTest {
            // Arrange
            givenResponse(body)

            // Act
            source.load(slug = SLUG, language = "en")

            // Assert
            coVerify(exactly = 1) { assetStore.retainOnly(setOf(slideUrl("1"), slideUrl("2"), slideUrl("3"))) }
        }

        @Test
        fun `GIVEN a response without the slug WHEN load THEN the story is not found`() = runTest {
            // Arrange
            givenResponse(mapOf(MAIN_PLACEMENT to emptyList()))

            // Act
            val actual = source.load(slug = SLUG, language = "en")

            // Assert
            assertThat(actual).isEqualTo(RemoteStoryV2Load.NotFound)
        }

        @Test
        fun `GIVEN a response that breaks the contract WHEN load THEN it is not saved and the load fails`() = runTest {
            // Arrange
            givenResponse(mapOf(MAIN_PLACEMENT to listOf(storyDto(slug = "other"))))

            // Act
            val actual = source.load(slug = SLUG, language = "en")

            // Assert
            assertThat(actual).isEqualTo(RemoteStoryV2Load.Failed)
            coVerify(exactly = 0) { localStore.saveResponse(language = any(), slugs = any(), response = any()) }
            assertThat(sentEvents()).containsExactly(
                errorEvent(stage = "content_validation", reason = StoryV2ErrorReason.UNKNOWN_SCHEMA),
            )
        }

        @Test
        fun `GIVEN the first slide file fails WHEN load THEN the load fails`() = runTest {
            // Arrange
            givenResponse(body)
            givenFileFails(url = slideUrl("1"))

            // Act
            val actual = source.load(slug = SLUG, language = "en")

            // Assert
            assertThat(actual).isEqualTo(RemoteStoryV2Load.Failed)
            assertThat(sentEvents()).containsExactly(
                errorEvent(
                    stage = "asset_prefetch",
                    reason = StoryV2ErrorReason.HTTP_FAILURE,
                    storyId = SLUG,
                    slideId = "1",
                    httpCode = 404,
                ),
            )
        }

        @Test
        fun `GIVEN a later slide file fails WHEN load THEN the story opens without that slide`() = runTest {
            // Arrange
            givenResponse(body)
            givenFileFails(url = slideUrl("2"))

            // Act
            val actual = source.load(slug = SLUG, language = "en")

            // Assert
            val expected = expectedComposition(slides = listOf(videoSlide(), imageSlide()))
            assertThat(actual).isEqualTo(RemoteStoryV2Load.Ready(expected))
        }
    }

    @Nested
    inner class Catalog {

        @Test
        fun `GIVEN a catalog loaded for a language WHEN load again in it THEN the api is not asked again`() = runTest {
            // Arrange
            givenResponse(body)
            source.load(slug = SLUG, language = "en")

            // Act
            val actual = source.load(slug = SLUG, language = "en")

            // Assert
            assertThat(actual).isEqualTo(RemoteStoryV2Load.Ready(expectedComposition()))
            coVerify(exactly = 1) { tangemTechApi.getStoriesV2(slugs = any(), language = any()) }
        }

        @Test
        fun `GIVEN a catalog loaded for a language WHEN load in another THEN the api is asked for it`() = runTest {
            // Arrange
            givenResponse(body)
            source.load(slug = SLUG, language = "en")

            // Act
            source.load(slug = SLUG, language = "de")

            // Assert
            coVerify(exactly = 1) { tangemTechApi.getStoriesV2(slugs = listOf(SLUG), language = "en") }
            coVerify(exactly = 1) { tangemTechApi.getStoriesV2(slugs = listOf(SLUG), language = "de") }
        }

        @Test
        fun `GIVEN a network error and a saved answer to the same question WHEN load THEN it is ready from it`() =
            runTest {
                // Arrange
                givenRequestFails(ApiResponseError.NetworkException())
                givenSaved(language = "en", slugs = listOf(SLUG), response = body)

                // Act
                val actual = source.load(slug = SLUG, language = "en")

                // Assert
                assertThat(actual).isEqualTo(RemoteStoryV2Load.Ready(expectedComposition()))
            }

        @Test
        fun `GIVEN an undecodable response and a saved answer WHEN load THEN it is ready from the saved answer`() =
            runTest {
                // Arrange
                givenRequestFails(ApiResponseError.UnknownException(JsonDataException("Expected a long but was 1.5")))
                givenSaved(language = "en", slugs = listOf(SLUG), response = body)

                // Act
                val actual = source.load(slug = SLUG, language = "en")

                // Assert
                assertThat(actual).isEqualTo(RemoteStoryV2Load.Ready(expectedComposition()))
                assertThat(sentEvents()).containsExactly(
                    errorEvent(stage = "request", reason = StoryV2ErrorReason.DECODE_FAILURE),
                )
            }

        @Test
        fun `GIVEN a network error and a saved answer to another language WHEN load THEN the load fails`() = runTest {
            // Arrange
            givenRequestFails(ApiResponseError.NetworkException())
            givenSaved(language = "de", slugs = listOf(SLUG), response = body)

            // Act
            val actual = source.load(slug = SLUG, language = "en")

            // Assert
            assertThat(actual).isEqualTo(RemoteStoryV2Load.Failed)
        }

        @Test
        fun `GIVEN a network error and a saved answer to other slugs WHEN load THEN the load fails`() = runTest {
            // Arrange
            givenRequestFails(ApiResponseError.NetworkException())
            givenSaved(language = "en", slugs = listOf(SLUG, "other"), response = body)

            // Act
            val actual = source.load(slug = SLUG, language = "en")

            // Assert
            assertThat(actual).isEqualTo(RemoteStoryV2Load.Failed)
        }

        @Test
        fun `GIVEN a load served from the saved answer WHEN load again THEN the api is asked again`() = runTest {
            // Arrange
            givenRequestFails(ApiResponseError.NetworkException())
            givenSaved(language = "en", slugs = listOf(SLUG), response = body)
            source.load(slug = SLUG, language = "en")

            // Act
            source.load(slug = SLUG, language = "en")

            // Assert
            coVerify(exactly = 2) { tangemTechApi.getStoriesV2(slugs = listOf(SLUG), language = "en") }
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class RequestErrors {

        @Test
        fun `GIVEN a network error and no saved answer WHEN load THEN it fails and the request error is reported`() =
            runTest {
                // Arrange
                givenRequestFails(ApiResponseError.NetworkException())

                // Act
                val actual = source.load(slug = SLUG, language = "en")

                // Assert
                assertThat(actual).isEqualTo(RemoteStoryV2Load.Failed)
                val sent = sentEvents().map { event -> event.event to event.params }
                assertThat(sent).containsExactly(
                    "Story Error Occurred" to mapOf("Error Stage" to "request", "Error Reason" to "network"),
                )
            }

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN a failed request WHEN load THEN the reported reason matches the failure`(model: RequestErrorModel) =
            runTest {
                // Arrange
                givenRequestFails(model.cause)

                // Act
                source.load(slug = SLUG, language = "en")

                // Assert
                assertThat(sentEvents()).containsExactly(model.expected)
            }

        private fun provideTestModels() = listOf(
            RequestErrorModel(
                cause = ApiResponseError.TimeoutException(),
                expected = errorEvent(stage = "request", reason = StoryV2ErrorReason.TIMEOUT),
            ),
            RequestErrorModel(
                cause = httpError(ApiResponseError.HttpException.Code.NOT_FOUND),
                expected = errorEvent(stage = "request", reason = StoryV2ErrorReason.NOT_FOUND, httpCode = 404),
            ),
            RequestErrorModel(
                cause = httpError(ApiResponseError.HttpException.Code.SERVICE_UNAVAILABLE),
                expected = errorEvent(stage = "request", reason = StoryV2ErrorReason.HTTP_FAILURE, httpCode = 503),
            ),
            RequestErrorModel(
                cause = ApiResponseError.UnknownException(JsonDataException("Expected a long but was 1.5")),
                expected = errorEvent(stage = "request", reason = StoryV2ErrorReason.DECODE_FAILURE),
            ),
        )

        private fun httpError(code: ApiResponseError.HttpException.Code) = ApiResponseError.HttpException(
            code = code,
            message = null,
            errorBody = null,
        )
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class OncePolicy {

        @Test
        fun `GIVEN a once story already shown WHEN load THEN it is reported as shown and not downloaded`() = runTest {
            // Arrange
            givenResponse(mapOf(MAIN_PLACEMENT to listOf(storyDto(displayPolicy = "once"))))
            coEvery { localStore.shownSlugs() } returns setOf(SLUG)

            // Act
            val actual = source.load(slug = SLUG, language = "en")

            // Assert
            assertThat(actual).isEqualTo(RemoteStoryV2Load.AlreadyShown)
            coVerify(exactly = 0) { assetStore.resolve(file = any(), keep = any()) }
        }

        @Test
        fun `GIVEN a once story not shown yet WHEN load THEN it is ready`() = runTest {
            // Arrange
            givenResponse(mapOf(MAIN_PLACEMENT to listOf(storyDto(displayPolicy = "once"))))

            // Act
            val actual = source.load(slug = SLUG, language = "en")

            // Assert
            assertThat(actual).isInstanceOf(RemoteStoryV2Load.Ready::class.java)
        }

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN a loaded story WHEN markShown THEN only a once story is recorded`(model: MarkShownModel) = runTest {
            // Arrange
            givenResponse(mapOf(MAIN_PLACEMENT to listOf(storyDto(displayPolicy = model.displayPolicy))))
            source.load(slug = SLUG, language = "en")

            // Act
            source.markShown(SLUG)

            // Assert
            coVerify(exactly = model.expectedRecords) { localStore.markShown(SLUG) }
        }

        @Test
        fun `GIVEN a once story loaded from the saved answer WHEN markShown THEN the showing is recorded`() = runTest {
            // Arrange
            val body = mapOf(MAIN_PLACEMENT to listOf(storyDto(displayPolicy = "once")))
            givenRequestFails(ApiResponseError.NetworkException())
            givenSaved(language = "en", slugs = listOf(SLUG), response = body)
            source.load(slug = SLUG, language = "en")

            // Act
            val actual = source.markShown(SLUG)

            // Assert
            assertThat(actual).isTrue()
            coVerify(exactly = 1) { localStore.markShown(SLUG) }
        }

        private fun provideTestModels() = listOf(
            MarkShownModel(displayPolicy = "once", expectedRecords = 1),
            MarkShownModel(displayPolicy = "always", expectedRecords = 0),
        )
    }

    private fun givenResponse(body: StoriesV2Response) {
        coEvery { tangemTechApi.getStoriesV2(slugs = any(), language = any()) } returns ApiResponse.Success(body)
    }

    private fun givenRequestFails(cause: ApiResponseError) {
        @Suppress("UNCHECKED_CAST")
        val error = ApiResponse.Error(cause = cause) as ApiResponse<StoriesV2Response>
        coEvery { tangemTechApi.getStoriesV2(slugs = any(), language = any()) } returns error
    }

    private fun givenSaved(language: String, slugs: List<String>, response: StoriesV2Response) {
        coEvery { localStore.savedResponse(language = language, slugs = slugs) } returns response
    }

    private fun givenFileFails(url: String) {
        coEvery { assetStore.resolve(file = match { it.url == url }, keep = any()) } returns
            StoryV2AssetResult.Failed(reason = StoryV2ErrorReason.HTTP_FAILURE, httpCode = 404)
    }

    private fun sentEvents(): List<AnalyticsEvent> {
        val events = mutableListOf<AnalyticsEvent>()
        verify { analytics.send(capture(events)) }
        return events
    }

    private fun expectedComposition(slides: List<StoryV2Slide> = listOf(videoSlide(), lottieSlide(), imageSlide())) =
        StoryV2Composition(
            id = SLUG,
            version = 0,
            origin = StoryV2Origin.REMOTE,
            endBehavior = StoryV2EndBehavior.LOOP,
            actions = listOf(storyAction()),
            slides = slides,
        )

    private fun videoSlide() = StoryV2Slide(
        id = "1",
        title = stringReference("Title"),
        subtitle = null,
        asset = StoryV2Asset.Video(
            source = localFileOf(slideUrl("1")),
            poster = null,
            durationMs = SLIDE_DURATION_MS,
            contentMode = StoryV2ContentMode.CONTAIN,
        ),
        haptics = listOf(tick(atMs = 100L)),
    )

    private fun lottieSlide() = StoryV2Slide(
        id = "2",
        title = stringReference("Title"),
        subtitle = stringReference("Subtitle"),
        asset = StoryV2Asset.VectorAnimation(
            source = localFileOf(slideUrl("2")),
            poster = null,
            durationMs = SLIDE_DURATION_MS,
            contentMode = StoryV2ContentMode.CONTAIN,
        ),
    )

    private fun imageSlide() = StoryV2Slide(
        id = "3",
        title = stringReference("Title"),
        subtitle = stringReference("Subtitle"),
        asset = StoryV2Asset.Image(
            source = localFileOf(slideUrl("3")),
            durationMs = SLIDE_DURATION_MS,
            contentMode = StoryV2ContentMode.COVER,
        ),
    )

    private fun fileOf(url: String) = File(ASSETS_DIR, url.substringAfterLast('/'))

    private fun localFileOf(url: String) = StoryV2MediaRef.LocalFile(path = fileOf(url).path)

    private fun errorEvent(
        stage: String,
        reason: StoryV2ErrorReason,
        storyId: String? = null,
        slideId: String? = null,
        httpCode: Int? = null,
    ) = StoriesV2Events.StoryErrorOccurred(
        storyId = storyId,
        slideId = slideId,
        errorStage = stage,
        errorReason = reason.analyticsValue,
        source = null,
        httpCode = httpCode,
    )

    internal data class RequestErrorModel(val cause: ApiResponseError, val expected: AnalyticsEvent)

    internal data class MarkShownModel(val displayPolicy: String, val expectedRecords: Int)

    private companion object {
        const val ASSETS_DIR = "/stories_v2/assets"
    }
}