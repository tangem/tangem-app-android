package com.tangem.features.survey.impl.nativesurvey.analytics

import com.google.common.truth.Truth.assertThat
import com.tangem.features.survey.impl.nativesurvey.analytics.NativeSurveyAnalyticsEvent.CloseMethod
import com.tangem.features.survey.impl.nativesurvey.analytics.NativeSurveyAnalyticsEvent.Request
import com.tangem.features.survey.impl.nativesurvey.model.NativeSurvey
import com.tangem.features.survey.impl.nativesurvey.model.SurveyAnswer
import com.tangem.features.survey.impl.nativesurvey.model.SurveyEntry
import com.tangem.features.survey.impl.nativesurvey.model.SurveySource
import com.tangem.features.survey.impl.nativesurvey.repository.SurveyRequestFailure
import com.tangem.test.core.ProvideTestModels
import io.mockk.mockk
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

internal class NativeSurveyAnalyticsEventTest {

    @Nested
    inner class SurveySubmitted {

        @Test
        fun `GIVEN a score and a comment WHEN built THEN id and params match the spec`() {
            // Arrange
            val answers = listOf(
                SurveyAnswer.Score(questionId = 1, value = 9),
                SurveyAnswer.Text(questionId = 2, value = "Nice"),
            )

            // Act
            val event = NativeSurveyAnalyticsEvent.SurveySubmitted(entry(), NativeSurvey.Type.NPS, sentAnswers = answers)

            // Assert
            assertThat(event.id).isEqualTo("[Survey] Survey Submitted")
            assertThat(event.params).isEqualTo(
                mapOf(
                    "Survey Id" to "123456",
                    "Survey Type" to "NPS",
                    "Source" to "Sheet",
                    "Lifecycle Stage" to "funded_30d",
                    "Score" to "9",
                    "Comment Filled" to "Yes",
                ),
            )
        }

        @Test
        fun `GIVEN a score only WHEN built THEN comment is not filled`() {
            // Act
            val event = NativeSurveyAnalyticsEvent.SurveySubmitted(
                entry = entry(),
                surveyType = NativeSurvey.Type.NPS,
                sentAnswers = listOf(SurveyAnswer.Score(questionId = 1, value = 0)),
            )

            // Assert
            assertThat(event.params).isEqualTo(
                mapOf(
                    "Survey Id" to "123456",
                    "Survey Type" to "NPS",
                    "Source" to "Sheet",
                    "Lifecycle Stage" to "funded_30d",
                    "Score" to "0",
                    "Comment Filled" to "No",
                ),
            )
        }

        @Test
        fun `GIVEN no score WHEN built THEN score is not sent`() {
            // Act
            val event = NativeSurveyAnalyticsEvent.SurveySubmitted(
                entry = entry(),
                surveyType = NativeSurvey.Type.NPS,
                sentAnswers = listOf(SurveyAnswer.Text(questionId = 2, value = "Nice")),
            )

            // Assert
            assertThat(event.params).isEqualTo(
                mapOf(
                    "Survey Id" to "123456",
                    "Survey Type" to "NPS",
                    "Source" to "Sheet",
                    "Lifecycle Stage" to "funded_30d",
                    "Comment Filled" to "Yes",
                ),
            )
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class SurveyError {

        @Test
        fun `GIVEN a load failure before the survey is known WHEN built THEN id and params match the spec`() {
            // Act
            val event = NativeSurveyAnalyticsEvent.SurveyError(
                entry = entry(source = SurveySource.LINK, stage = ""),
                surveyType = null,
                request = Request.LOAD,
                failure = SurveyRequestFailure.Http(code = 404),
                isRequestSent = true,
            )

            // Assert
            assertThat(event.id).isEqualTo("[Survey] Survey Error")
            assertThat(event.params).isEqualTo(
                mapOf(
                    "Survey Id" to "123456",
                    "Survey Type" to "",
                    "Source" to "Link",
                    "Lifecycle Stage" to "",
                    "Request" to "Load",
                    "Error Code" to "404",
                    "Request Sent" to "Yes",
                ),
            )
        }

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN a failed request WHEN built THEN request, error code and request sent are mapped`(
            model: ErrorModel,
        ) {
            // Act
            val event = NativeSurveyAnalyticsEvent.SurveyError(
                entry = entry(),
                surveyType = NativeSurvey.Type.NPS,
                request = model.request,
                failure = model.failure,
                isRequestSent = model.isRequestSent,
            )

            // Assert
            assertThat(event.params).isEqualTo(model.expected)
        }

        private fun provideTestModels() = listOf(
            ErrorModel(Request.LOAD, SurveyRequestFailure.Network, true, errorParams("Load", "Network", "Yes")),
            ErrorModel(Request.LOAD, SurveyRequestFailure.Parse, true, errorParams("Load", "Parse", "Yes")),
            ErrorModel(Request.LOAD, SurveyRequestFailure.Unsupported, true, errorParams("Load", "Unsupported", "Yes")),
            ErrorModel(Request.SUBMIT, SurveyRequestFailure.Http(429), false, errorParams("Submit", "429", "No")),
            ErrorModel(Request.SUBMIT, SurveyRequestFailure.Http(503), true, errorParams("Submit", "503", "Yes")),
        )

        private fun errorParams(request: String, errorCode: String, requestSent: String) = mapOf(
            "Survey Id" to "123456",
            "Survey Type" to "NPS",
            "Source" to "Sheet",
            "Lifecycle Stage" to "funded_30d",
            "Request" to request,
            "Error Code" to errorCode,
            "Request Sent" to requestSent,
        )
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class Source {

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN an entry source WHEN event built THEN source param matches the spec`(model: SourceModel) {
            // Act
            val event = NativeSurveyAnalyticsEvent.SurveySubmitted(
                entry = entry(source = model.source),
                surveyType = NativeSurvey.Type.NPS,
                sentAnswers = emptyList(),
            )

            // Assert
            assertThat(event.params).isEqualTo(
                mapOf(
                    "Survey Id" to "123456",
                    "Survey Type" to "NPS",
                    "Source" to model.expected,
                    "Lifecycle Stage" to "funded_30d",
                    "Comment Filled" to "No",
                ),
            )
        }

        private fun provideTestModels() = listOf(
            SourceModel(SurveySource.SHEET, "Sheet"),
            SourceModel(SurveySource.PUSH, "Push"),
            SourceModel(SurveySource.BANNER, "Banner"),
            SourceModel(SurveySource.LINK, "Link"),
        )
    }

    @Nested
    inner class SurveyOpened {

        @Test
        fun `GIVEN a freshly loaded survey WHEN built THEN id and params match the spec`() {
            // Act
            val event = NativeSurveyAnalyticsEvent.SurveyOpened(
                entry = entry(),
                surveyType = NativeSurvey.Type.NPS,
                isFromCache = false,
                isAlreadyPassed = false,
            )

            // Assert
            assertThat(event.id).isEqualTo("[Survey] Survey Opened")
            assertThat(event.params).isEqualTo(
                mapOf(
                    "Survey Id" to "123456",
                    "Survey Type" to "NPS",
                    "Source" to "Sheet",
                    "Lifecycle Stage" to "funded_30d",
                    "From Cache" to "No",
                    "Already Passed" to "No",
                ),
            )
        }

        @Test
        fun `GIVEN an answered survey opened from a link WHEN built THEN its type is empty and it is passed`() {
            // Act
            val event = NativeSurveyAnalyticsEvent.SurveyOpened(
                entry = entry(source = SurveySource.LINK),
                surveyType = null,
                isFromCache = true,
                isAlreadyPassed = true,
            )

            // Assert
            assertThat(event.params).isEqualTo(
                mapOf(
                    "Survey Id" to "123456",
                    "Survey Type" to "",
                    "Source" to "Link",
                    "Lifecycle Stage" to "funded_30d",
                    "From Cache" to "Yes",
                    "Already Passed" to "Yes",
                ),
            )
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class SurveyClosed {

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN a close method WHEN built THEN id and params match the spec`(model: CloseModel) {
            // Act
            val event = NativeSurveyAnalyticsEvent.SurveyClosed(
                entry = entry(),
                surveyType = NativeSurvey.Type.NPS,
                closeMethod = model.closeMethod,
                isScoreSelected = model.isScoreSelected,
            )

            // Assert
            assertThat(event.id).isEqualTo("[Survey] Survey Closed")
            assertThat(event.params).isEqualTo(
                mapOf(
                    "Survey Id" to "123456",
                    "Survey Type" to "NPS",
                    "Source" to "Sheet",
                    "Lifecycle Stage" to "funded_30d",
                    "Close Method" to model.expectedCloseMethod,
                    "Score Selected" to model.expectedScoreSelected,
                ),
            )
        }

        private fun provideTestModels() = listOf(
            CloseModel(CloseMethod.CROSS, true, "X", "Yes"),
            CloseModel(CloseMethod.SYSTEM_BACK, false, "System Back", "No"),
            CloseModel(CloseMethod.LOCK, false, "Lock", "No"),
            CloseModel(CloseMethod.DEEPLINK, true, "Deeplink", "Yes"),
            CloseModel(CloseMethod.ERROR, true, "Error", "Yes"),
        )
    }

    data class CloseModel(
        val closeMethod: CloseMethod,
        val isScoreSelected: Boolean,
        val expectedCloseMethod: String,
        val expectedScoreSelected: String,
    )

    data class ErrorModel(
        val request: Request,
        val failure: SurveyRequestFailure,
        val isRequestSent: Boolean,
        val expected: Map<String, String>,
    )

    data class SourceModel(val source: SurveySource, val expected: String)

    private fun entry(source: SurveySource = SurveySource.SHEET, stage: String = "funded_30d") = SurveyEntry(
        surveyId = 123456,
        source = source,
        stage = stage,
        repeatAfterDays = 180,
        userWallet = mockk(),
    )
}