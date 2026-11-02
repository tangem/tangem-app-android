package com.tangem.data.marketing

import app.cash.turbine.test
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
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.junit.jupiter.params.provider.ValueSource

internal class DefaultDemandSurveyRepositoryTest {

    private val tangemTechApi: TangemTechApi = mockk()
    private lateinit var repository: DefaultDemandSurveyRepository

    private val userWalletId = UserWalletId("0000000000000000000000000000000000000000000000000000000000000001")
    private val otherUserWalletId = UserWalletId("0000000000000000000000000000000000000000000000000000000000000002")
    private val answers = listOf(
        SurveyAnswer(questionKey = "ticket_size", answerKey = "100k_500k"),
        SurveyAnswer(questionKey = "frequency", answerKey = "1_5_per_month"),
    )

    @BeforeEach
    fun reset() {
        clearMocks(tangemTechApi)
        repository = DefaultDemandSurveyRepository(
            tangemTechApi = tangemTechApi,
            dispatchers = TestingCoroutineDispatcherProvider(),
        )
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class Submit {

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

    @Nested
    inner class ObserveCompleted {

        @Test
        fun `GIVEN status not requested WHEN observeCompleted THEN requests status once and emits it`() = runTest {
            // Arrange
            mockStatus(completed = false)

            // Act
            val actual = repository.observeCompleted(userWalletId).first()

            // Assert
            assertThat(actual).isFalse()
            coVerify(exactly = 1) {
                tangemTechApi.getSurveyCompletionStatus(walletId = userWalletId.stringValue, surveyId = "demand")
            }
        }

        @ParameterizedTest
        @ValueSource(booleans = [true, false])
        fun `GIVEN previous session WHEN observeCompleted in a new session THEN requests status again`(
            completed: Boolean,
        ) = runTest {
            // Arrange
            mockStatus(completed = completed)
            repository.observeCompleted(userWalletId).first()

            // Act
            val actual = repository.observeCompleted(userWalletId).first()

            // Assert
            assertThat(actual).isEqualTo(completed)
            coVerify(exactly = 2) { tangemTechApi.getSurveyCompletionStatus(any(), any()) }
        }

        @Test
        fun `GIVEN status request fails WHEN observeCompleted THEN emits false`() = runTest {
            // Arrange
            coEvery { tangemTechApi.getSurveyCompletionStatus(any(), any()) } returns
                errorResponse(ApiResponseError.NetworkException())

            // Act
            val actual = repository.observeCompleted(userWalletId).first()

            // Assert
            assertThat(actual).isFalse()
        }

        @ParameterizedTest
        @ValueSource(strings = ["created", "already_exists"])
        fun `GIVEN observing not completed wallet WHEN submit succeeds THEN emits true`(status: String) = runTest {
            // Arrange
            mockStatus(completed = false)
            mockSubmit(status = status)

            repository.observeCompleted(userWalletId).test {
                assertThat(awaitItem()).isFalse()

                // Act
                repository.submit(userWalletId, answers)

                // Assert
                assertThat(awaitItem()).isTrue()
            }
        }

        @Test
        fun `GIVEN status request in flight WHEN submit succeeds THEN emits true before the request finishes`() =
            runTest {
                // Arrange
                val status = CompletableDeferred<ApiResponse<SurveyCompletionStatusResponse>>()
                coEvery { tangemTechApi.getSurveyCompletionStatus(any(), any()) } coAnswers { status.await() }
                mockSubmit(status = "created")

                repository.observeCompleted(userWalletId).test {
                    // Act
                    repository.submit(userWalletId, answers)

                    // Assert
                    assertThat(awaitItem()).isTrue()
                    status.complete(ApiResponse.Success(SurveyCompletionStatusResponse(isCompleted = false)))
                    expectNoEvents()
                }
            }

        @Test
        fun `GIVEN wallet submitted WHEN later session request fails THEN emits true`() =
            runTest {
                // Arrange
                mockSubmit(status = "created")
                repository.submit(userWalletId, answers)
                coEvery { tangemTechApi.getSurveyCompletionStatus(any(), any()) } returns
                    errorResponse(ApiResponseError.NetworkException())

                // Act
                val actual = repository.observeCompleted(userWalletId).first()

                // Assert
                assertThat(actual).isTrue()
                coVerify(exactly = 1) { tangemTechApi.getSurveyCompletionStatus(any(), any()) }
            }

        @Test
        fun `GIVEN observing not completed wallet WHEN submit fails THEN emits nothing new`() = runTest {
            // Arrange
            mockStatus(completed = false)
            coEvery { tangemTechApi.submitSurveyCompletion(any(), any()) } returns
                errorResponse(httpError(Code.TOO_MANY_REQUESTS))

            repository.observeCompleted(userWalletId).test {
                assertThat(awaitItem()).isFalse()

                // Act
                repository.submit(userWalletId, answers)

                // Assert
                expectNoEvents()
            }
        }

        @Test
        fun `GIVEN another wallet submitted WHEN observeCompleted THEN requests status for this wallet`() = runTest {
            // Arrange
            mockSubmit(status = "created")
            mockStatus(completed = false)
            repository.submit(otherUserWalletId, answers)

            // Act
            val actual = repository.observeCompleted(userWalletId).first()

            // Assert
            assertThat(actual).isFalse()
            coVerify(exactly = 1) {
                tangemTechApi.getSurveyCompletionStatus(walletId = userWalletId.stringValue, surveyId = "demand")
            }
        }

        private fun mockStatus(completed: Boolean) {
            coEvery { tangemTechApi.getSurveyCompletionStatus(any(), any()) } returns
                ApiResponse.Success(SurveyCompletionStatusResponse(isCompleted = completed))
        }

        private fun mockSubmit(status: String) {
            coEvery { tangemTechApi.submitSurveyCompletion(any(), any()) } returns
                ApiResponse.Success(SurveyCompletionResponse(status = status))
        }
    }

    private fun httpError(code: Code) = ApiResponseError.HttpException(code = code, message = null, errorBody = null)

    @Suppress("UNCHECKED_CAST")
    private fun <T : Any> errorResponse(cause: ApiResponseError): ApiResponse<T> =
        ApiResponse.Error(cause = cause) as ApiResponse<T>
}