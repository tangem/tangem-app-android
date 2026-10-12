package com.tangem.features.storiesv2.impl.content.remote

import com.google.common.truth.Truth.assertThat
import com.tangem.datasource.api.tangemTech.models.storiesv2.StoriesV2Response
import com.tangem.test.core.datastore.createAppDataStoreFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

internal class StoryV2LocalStoreTest {

    private val response: StoriesV2Response = mapOf(
        MAIN_PLACEMENT to listOf(
            storyDto(
                language = "en",
                priority = 100,
                displayPolicy = "once",
                slides = listOf(
                    slideDto(id = "1", hapticAtMs = listOf(100, 130)),
                    slideDto(id = "2", title = null, source = sourceDto(url = slideUrl(id = "2"), codec = null)),
                ),
                actions = listOf(actionDto(), actionDto(id = "later", style = null, deeplink = null)),
            ),
        ),
        SETTINGS_PLACEMENT to listOf(storyDto()),
    )

    private lateinit var dataStoreScope: CoroutineScope
    private lateinit var store: StoryV2LocalStore

    /** A fresh file per test: the store is a real DataStore, so its state lives on disk, not in the instance. */
    @BeforeEach
    fun setUp(@TempDir tempDir: File) {
        dataStoreScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        store = StoryV2LocalStore(
            dataStoreFactory = createAppDataStoreFactory(filesDir = tempDir, appScope = dataStoreScope),
            moshi = StoryV2Json.moshi,
        )
    }

    @AfterEach
    fun tearDown() {
        dataStoreScope.cancel()
    }

    @Nested
    inner class SavedResponse {

        @Test
        fun `GIVEN nothing saved WHEN savedResponse THEN returns null`() = runTest {
            // Act
            val actual = store.savedResponse(language = "en", slugs = listOf(SLUG))

            // Assert
            assertThat(actual).isNull()
        }

        @Test
        fun `GIVEN a saved response WHEN savedResponse for the same question THEN returns an equal response`() =
            runTest {
                // Arrange
                store.saveResponse(language = "en", slugs = listOf(SLUG), response = response)

                // Act
                val actual = store.savedResponse(language = "en", slugs = listOf(SLUG))

                // Assert
                assertThat(actual).isEqualTo(response)
            }

        @Test
        fun `GIVEN a saved response WHEN savedResponse in another language THEN returns null`() = runTest {
            // Arrange
            store.saveResponse(language = "en", slugs = listOf(SLUG), response = response)

            // Act
            val actual = store.savedResponse(language = "de", slugs = listOf(SLUG))

            // Assert
            assertThat(actual).isNull()
        }

        @Test
        fun `GIVEN a saved response WHEN savedResponse for other slugs THEN returns null`() = runTest {
            // Arrange
            store.saveResponse(language = "en", slugs = listOf(SLUG), response = response)

            // Act
            val actual = store.savedResponse(language = "en", slugs = listOf(SLUG, "other"))

            // Assert
            assertThat(actual).isNull()
        }

        @Test
        fun `GIVEN two saved responses WHEN savedResponse THEN only the last question is answered`() = runTest {
            // Arrange
            val latest = mapOf(MAIN_PLACEMENT to listOf(storyDto(endBehaviour = "finish")))
            store.saveResponse(language = "en", slugs = listOf(SLUG), response = response)
            store.saveResponse(language = "de", slugs = listOf(SLUG), response = latest)

            // Act
            val actual = listOf("en", "de").map { language ->
                store.savedResponse(language = language, slugs = listOf(SLUG))
            }

            // Assert
            assertThat(actual).containsExactly(null, latest).inOrder()
        }
    }

    @Nested
    inner class ShownSlugs {

        @Test
        fun `GIVEN nothing shown WHEN shownSlugs THEN returns empty`() = runTest {
            // Act
            val actual = store.shownSlugs()

            // Assert
            assertThat(actual).isEmpty()
        }

        @Test
        fun `GIVEN slugs marked shown WHEN shownSlugs THEN contains each of them once`() = runTest {
            // Arrange
            store.markShown(SLUG)
            store.markShown("other")
            store.markShown(SLUG)

            // Act
            val actual = store.shownSlugs()

            // Assert
            assertThat(actual).containsExactly(SLUG, "other")
        }

        @Test
        fun `GIVEN a saved response WHEN markShown THEN the saved response is kept`() = runTest {
            // Arrange
            store.saveResponse(language = "en", slugs = listOf(SLUG), response = response)

            // Act
            store.markShown(SLUG)

            // Assert
            assertThat(store.savedResponse(language = "en", slugs = listOf(SLUG))).isEqualTo(response)
        }
    }

    @Nested
    inner class Clear {

        @Test
        fun `GIVEN a saved response and shown slugs WHEN clear THEN nothing is left`() = runTest {
            // Arrange
            store.saveResponse(language = "en", slugs = listOf(SLUG), response = response)
            store.markShown(SLUG)

            // Act
            store.clear()

            // Assert
            assertThat(store.shownSlugs()).isEmpty()
            assertThat(store.savedResponse(language = "en", slugs = listOf(SLUG))).isNull()
        }
    }
}