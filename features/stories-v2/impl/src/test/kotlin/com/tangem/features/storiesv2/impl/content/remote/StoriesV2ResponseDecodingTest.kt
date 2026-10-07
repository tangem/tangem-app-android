package com.tangem.features.storiesv2.impl.content.remote

import com.google.common.truth.Truth.assertThat
import com.squareup.moshi.JsonDataException
import com.tangem.test.core.ProvideTestModels
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

/**
 * Documents what the typed `/v2/stories` adapters accept: a type mismatch anywhere fails the whole response, which
 * the network layer reports as a decode failure, before the converter gets a chance to drop only the broken part.
 */
internal class StoriesV2ResponseDecodingTest {

    @Test
    fun `GIVEN the dev response WHEN decode THEN both placements carry the story with numeric slide ids as strings`() {
        // Arrange
        val json = StoryV2Json.resource(path = DEV_RESPONSE_PATH)

        // Act
        val actual = StoryV2Json.decode(json)

        // Assert
        val slideIds = actual.mapValues { (_, stories) ->
            stories.orEmpty().map { story -> story?.slug to story?.slides.orEmpty().map { it?.id } }
        }
        val expectedStory = SLUG to listOf("2", "3", "4", "5")
        assertThat(slideIds).containsExactly(
            MAIN_PLACEMENT,
            listOf(expectedStory),
            SETTINGS_PLACEMENT,
            listOf(expectedStory),
        ).inOrder()
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class Coercions {

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN a slide with a coercible id or duration WHEN decode THEN it is read as the declared type`(
            model: CoercionModel,
        ) {
            // Arrange
            val json = responseJson(slideId = model.slideIdJson, durationMs = model.durationMsJson)

            // Act
            val actual = StoryV2Json.decode(json)

            // Assert
            val expectedSlide = slideDto(
                id = model.expectedId,
                durationMs = model.expectedDurationMs,
                source = sourceDto(),
            )
            assertThat(actual).isEqualTo(mapOf(MAIN_PLACEMENT to listOf(storyDto(slides = listOf(expectedSlide)))))
        }

        private fun provideTestModels() = listOf(
            CoercionModel(slideIdJson = "2", expectedId = "2"),
            CoercionModel(slideIdJson = "\"intro\"", expectedId = "intro"),
            CoercionModel(slideIdJson = "2.5", expectedId = "2.5"),
            CoercionModel(slideIdJson = "1", durationMsJson = "\"5000\"", expectedId = "1"),
        )
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class TypeMismatches {

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN a value of the wrong type WHEN decode THEN the whole response fails`(model: MismatchModel) {
            // Act
            val actual = runCatching { StoryV2Json.decode(model.json) }.exceptionOrNull()

            // Assert
            assertThat(actual).isInstanceOf(JsonDataException::class.java)
        }

        private fun provideTestModels() = listOf(
            MismatchModel(case = "fractional durationMs", json = responseJson(durationMs = "1.5")),
            MismatchModel(case = "non-numeric durationMs", json = responseJson(durationMs = "\"abc\"")),
            MismatchModel(case = "boolean slide id", json = responseJson(slideId = "true")),
            MismatchModel(case = "hapticAtMs not an array", json = responseJson(hapticAtMs = "\"100\"")),
            MismatchModel(case = "non-numeric haptic mark", json = responseJson(hapticAtMs = "[100, \"abc\"]")),
            MismatchModel(case = "fractional haptic mark", json = responseJson(hapticAtMs = "[100, 1.5]")),
            MismatchModel(case = "fractional sizeBytes", json = responseJson(sizeBytes = "10.5")),
            MismatchModel(case = "root not an object", json = "[]"),
        )
    }

    /** One valid story around a single slide; every argument is spliced into the JSON as is. */
    private fun responseJson(
        slideId: String = "1",
        durationMs: String = "$SLIDE_DURATION_MS",
        hapticAtMs: String = "null",
        sizeBytes: String = "$SLIDE_SIZE_BYTES",
    ): String = """
        {
          "$MAIN_PLACEMENT": [
            {
              "slug": "$SLUG",
              "displayPolicy": "always",
              "endBehaviour": "loop",
              "slides": [
                {
                  "id": $slideId,
                  "title": "Title",
                  "subtitle": "Subtitle",
                  "assetType": "video",
                  "contentMode": "contain",
                  "durationMs": $durationMs,
                  "hapticAtMs": $hapticAtMs,
                  "source": {
                    "url": "${slideUrl(id = "1")}",
                    "mimetype": "video/mp4",
                    "codec": "h264",
                    "sizeBytes": $sizeBytes
                  }
                }
              ]
            }
          ]
        }
    """.trimIndent()

    internal data class CoercionModel(
        val slideIdJson: String,
        val expectedId: String,
        val durationMsJson: String = "$SLIDE_DURATION_MS",
        val expectedDurationMs: Long = SLIDE_DURATION_MS,
    )

    internal data class MismatchModel(val case: String, val json: String) {
        override fun toString(): String = case
    }
}