package com.tangem.data.marketing

import arrow.core.Either
import com.google.common.truth.Truth.assertThat
import com.tangem.core.remote.response.ApiResponse
import com.tangem.core.remote.response.ApiResponseError
import com.tangem.core.remote.response.ApiResponseError.HttpException.Code
import com.tangem.datasource.api.survey.models.SurveyAnswerDto
import com.tangem.datasource.api.survey.models.SurveyCompletionBody
import com.tangem.datasource.api.survey.models.SurveyCompletionResponse
import com.tangem.datasource.api.survey.models.SurveyCompletionStatusResponse
import com.tangem.datasource.api.tangemTech.TangemTechApi
import com.tangem.domain.marketing.models.SurveyAnswer
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.test.core.ProvideTestModels
import com.tangem.test.core.assertEitherRight
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

internal class DefaultDemandSurveyRepositoryTest {

    private val tangemTechApi: TangemTechApi = mockk()
    private val repository = DefaultDemandSurveyRepository(tangemTechApi = tangemTechApi)

    private val userWalletId = UserWalletId("0000000000000000000000000000000000000000000000000000000000000001")

    @BeforeEach
    fun reset() = clearMocks(tangemTechApi)

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class Submit {

        private val answers = listOf(
            SurveyAnswer(questionKey = "ticket_size", answerKey = "100k_500k"),
            SurveyAnswer(questionKey = "frequency", answerKey = "1_5_per_month"),
        )

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN success status WHEN submit THEN returns Right and sends demand survey body`(status: String) =
            runTest {
                // Arrange
                coEvery { tangemTechApi.submitSurveyCompletion(any(), any()) } returns
                    ApiResponse.Success(SurveyCompletionResponse(status = status))

                // Act
                val actual = repository.submit(userWalletId, answers)

                // Assert
                assertEitherRight(actual)
                val expectedBody = SurveyCompletionBody(
                    surveyId = "demand",
                    answers = listOf(
                        SurveyAnswerDto(question = "ticket_size", answer = "100k_500k"),
                        SurveyAnswerDto(question = "frequency", answer = "1_5_per_month"),
                    ),
                )
                coVerify(exactly = 1) {
                    tangemTechApi.submitSurveyCompletion(walletId = userWalletId.stringValue, body = expectedBody)
                }
            }

        @ParameterizedTest
        @MethodSource("provideErrors")
        fun `GIVEN api error WHEN submit THEN returns Left with cause`(cause: ApiResponseError) = runTest {
            // Arrange
            coEvery { tangemTechApi.submitSurveyCompletion(any(), any()) } returns errorResponse(cause)

            // Act
            val actual = repository.submit(userWalletId, answers)

            // Assert
            assertThat(actual).isEqualTo(Either.Left(cause))
        }

        @Suppress("UnusedPrivateMember")
        private fun provideTestModels() = listOf("created", "already_exists")

        @Suppress("UnusedPrivateMember")
        private fun provideErrors() = listOf(
            ApiResponseError.NetworkException(),
            httpError(Code.FORBIDDEN),
            httpError(Code.TOO_MANY_REQUESTS),
            httpError(Code.INTERNAL_SERVER_ERROR),
        )
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class IsCompleted {

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN status response WHEN isCompleted THEN returns completed flag`(completed: Boolean) = runTest {
            // Arrange
            coEvery {
                tangemTechApi.getSurveyCompletionStatus(walletId = userWalletId.stringValue, surveyId = "demand")
            } returns ApiResponse.Success(SurveyCompletionStatusResponse(isCompleted = completed))

            // Act
            val actual = repository.isCompleted(userWalletId)

            // Assert
            assertThat(actual).isEqualTo(Either.Right(completed))
        }

        @Test
        fun `GIVEN api error WHEN isCompleted THEN returns Left with cause`() = runTest {
            // Arrange
            val cause = ApiResponseError.NetworkException()
            coEvery { tangemTechApi.getSurveyCompletionStatus(any(), any()) } returns errorResponse(cause)

            // Act
            val actual = repository.isCompleted(userWalletId)

            // Assert
            assertThat(actual).isEqualTo(Either.Left(cause))
        }

        @Suppress("UnusedPrivateMember")
        private fun provideTestModels() = listOf(true, false)
    }

    private fun httpError(code: Code) = ApiResponseError.HttpException(code = code, message = null, errorBody = null)

    @Suppress("UNCHECKED_CAST")
    private fun <T : Any> errorResponse(cause: ApiResponseError): ApiResponse<T> =
        ApiResponse.Error(cause = cause) as ApiResponse<T>
}