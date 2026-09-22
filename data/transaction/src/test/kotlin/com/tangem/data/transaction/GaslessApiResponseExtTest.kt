package com.tangem.data.transaction

import com.google.common.truth.Truth.assertThat
import com.tangem.core.remote.response.ApiResponseError
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class GaslessApiResponseExtTest {

    @ParameterizedTest
    @MethodSource("provideTestModels")
    fun toGaslessServiceException(model: TestModel) {
        // Act
        val actual = model.error.toGaslessServiceException()

        // Assert
        assertThat(actual.httpCode).isEqualTo(model.expectedHttpCode)
        assertThat(actual.message).isEqualTo(model.expectedMessage)
    }

    @Test
    fun `GIVEN transport error WHEN converted THEN original error is kept as cause`() {
        // Arrange
        val error = ApiResponseError.NetworkException()

        // Act
        val actual = error.toGaslessServiceException()

        // Assert
        assertThat(actual.cause).isSameInstanceAs(error)
    }

    internal data class TestModel(
        val error: ApiResponseError,
        val expectedHttpCode: Int?,
        val expectedMessage: String,
    )

    private fun provideTestModels() = listOf(
        TestModel(
            error = httpError(message = "Service Unavailable"),
            expectedHttpCode = 503,
            expectedMessage = "Service Unavailable",
        ),
        // HTTP/2 carries no reason phrase, so the status name stands in for it
        TestModel(
            error = httpError(message = null),
            expectedHttpCode = 503,
            expectedMessage = "Service Unavailable",
        ),
        TestModel(
            error = httpError(message = ""),
            expectedHttpCode = 503,
            expectedMessage = "Service Unavailable",
        ),
        TestModel(
            error = ApiResponseError.NetworkException(),
            expectedHttpCode = null,
            expectedMessage = "Gasless service is unreachable",
        ),
        TestModel(
            error = ApiResponseError.TimeoutException(),
            expectedHttpCode = null,
            expectedMessage = "Gasless service request timed out",
        ),
        TestModel(
            error = ApiResponseError.UnknownException(cause = IllegalStateException("boom")),
            expectedHttpCode = null,
            expectedMessage = "boom",
        ),
        TestModel(
            error = ApiResponseError.UnknownException(cause = IllegalStateException()),
            expectedHttpCode = null,
            expectedMessage = "Gasless service request failed",
        ),
    )

    private fun httpError(message: String?) = ApiResponseError.HttpException(
        code = ApiResponseError.HttpException.Code.SERVICE_UNAVAILABLE,
        message = message,
        errorBody = null,
    )
}