package com.tangem.feature.swap

import com.google.common.truth.Truth.assertThat
import com.tangem.datasource.api.surveysparrow.SurveySparrowApi
import com.tangem.datasource.api.surveysparrow.models.CreateSurveySparrowResponseBody
import com.tangem.datasource.api.surveysparrow.models.SurveySparrowGetAnswerDto
import com.tangem.datasource.api.surveysparrow.models.SurveySparrowResponseDto
import com.tangem.datasource.api.surveysparrow.models.SurveySparrowResponsesDto
import com.tangem.datasource.local.config.environment.models.SurveySparrowSwapRatingConfig
import com.tangem.feature.swap.domain.models.domain.SwapFeedbackParams
import io.mockk.Runs
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class SurveySparrowSwapFeedbackRemoteSourceTest {

    private val api: SurveySparrowApi = mockk()

    private val remoteSource = SurveySparrowSwapFeedbackRemoteSource(api = api, config = CONFIG)

    @BeforeEach
    fun resetMocks() {
        clearMocks(api)
    }

    private fun responsesWithRating(rating: Int) = SurveySparrowResponsesDto(
        data = listOf(
            SurveySparrowResponseDto(
                answers = listOf(SurveySparrowGetAnswerDto(questionId = CONFIG.ratingQuestionId, answer = rating)),
            ),
        ),
    )

    private fun submitParams(rating: Int = 5) = SwapFeedbackParams(
        userWalletIdHash = USER_WALLET_ID_HASH,
        providerName = PROVIDER_NAME,
        txUrl = TX_EXTERNAL_URL,
        txExternalId = TX_EXTERNAL_ID,
        rating = rating,
        feedback = "feedback",
    )

    @Test
    fun `GIVEN survey has rating answer WHEN getRating THEN rating returned`() = runTest {
        // Arrange
        coEvery { api.getResponses(any(), any(), any()) } returns responsesWithRating(rating = 4)

        // Act
        val result = remoteSource.getRating(TX_EXTERNAL_ID)

        // Assert
        assertThat(result.getOrNull()).isEqualTo(4)
    }

    @Test
    fun `GIVEN survey has no responses WHEN getRating THEN null returned`() = runTest {
        // Arrange
        coEvery { api.getResponses(any(), any(), any()) } returns SurveySparrowResponsesDto(data = emptyList())

        // Act
        val result = remoteSource.getRating(TX_EXTERNAL_ID)

        // Assert
        assertThat(result.isRight()).isTrue()
        assertThat(result.getOrNull()).isNull()
    }

    @Test
    fun `GIVEN api throws WHEN getRating THEN error returned`() = runTest {
        // Arrange
        coEvery { api.getResponses(any(), any(), any()) } throws RuntimeException("error")

        // Act
        val result = remoteSource.getRating(TX_EXTERNAL_ID)

        // Assert
        assertThat(result.isLeft()).isTrue()
    }

    @Test
    fun `GIVEN deal data WHEN submitFeedback THEN POST carries deal data`() = runTest {
        // Arrange
        val bodySlot = slot<CreateSurveySparrowResponseBody>()
        coEvery { api.createResponse(capture(bodySlot)) } just Runs

        // Act
        val result = remoteSource.submitFeedback(submitParams(rating = 5))

        // Assert
        assertThat(result.isRight()).isTrue()
        with(bodySlot.captured) {
            assertThat(surveyId).isEqualTo(CONFIG.surveyId)
            assertThat(answers.map { it.questionId to it.answer }).containsExactly(
                CONFIG.ratingQuestionId to "5",
                CONFIG.feedbackQuestionId to "feedback",
            )
            assertThat(variables).containsEntry("tx_external_id", TX_EXTERNAL_ID)
            assertThat(variables).containsEntry("user_wallet_id", USER_WALLET_ID_HASH)
        }
    }

    @Test
    fun `GIVEN api throws WHEN submitFeedback THEN error returned`() = runTest {
        // Arrange
        coEvery { api.createResponse(any()) } throws RuntimeException("error")

        // Act
        val result = remoteSource.submitFeedback(submitParams())

        // Assert
        assertThat(result.isLeft()).isTrue()
    }

    private companion object {
        val CONFIG = SurveySparrowSwapRatingConfig(
            surveyId = 100L,
            ratingQuestionId = 42L,
            feedbackQuestionId = 43L,
        )
        const val TX_EXTERNAL_ID = "tx-external-id"
        const val PROVIDER_NAME = "ChangeNow"
        const val TX_EXTERNAL_URL = "https://provider.example/tx"
        const val USER_WALLET_ID_HASH = "wallet-id-hash"
    }
}