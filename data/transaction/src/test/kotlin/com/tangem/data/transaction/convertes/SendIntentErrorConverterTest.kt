package com.tangem.data.transaction.convertes

import com.google.common.truth.Truth.assertThat
import com.tangem.core.remote.response.ApiResponseError
import com.tangem.core.remote.response.ApiResponseError.HttpException.Code
import com.tangem.domain.transaction.error.SendIntentError
import com.tangem.test.core.ProvideTestModels
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

internal class SendIntentErrorConverterTest {

    private val converter = SendIntentErrorConverter()

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class KnownReason {

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN error body with a known code WHEN convert THEN the reason is reported`(model: KnownReasonModel) {
            // Act
            val actual = converter.convert(input(httpError(model.code, problem(model.errorCode))))

            // Assert
            assertThat(actual).isEqualTo(model.expected)
        }

        private fun provideTestModels() = listOf(
            KnownReasonModel(Code.NOT_FOUND, "INTENT_NOT_FOUND", SendIntentError.NotFound(INTENT_ID)),
            KnownReasonModel(Code.CONFLICT, "INTENT_SESSION_REVOKED", SendIntentError.SessionRevoked(INTENT_ID)),
            KnownReasonModel(Code.GONE, "INTENT_SESSION_REVOKED", SendIntentError.SessionRevoked(INTENT_ID)),
            KnownReasonModel(Code.CONFLICT, "INTENT_ALREADY_COMPLETED", SendIntentError.AlreadyCompleted(INTENT_ID)),
        )
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class RawFailure {

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN failure without a known code WHEN convert THEN it is kept as it is`(cause: ApiResponseError) {
            // Act
            val actual = converter.convert(input(cause))

            // Assert
            assertThat(actual).isSameInstanceAs(cause)
        }

        private fun provideTestModels(): List<ApiResponseError> = listOf(
            httpError(Code.UNAUTHORIZED, problem("UNAUTHORIZED")),
            httpError(Code.INTERNAL_SERVER_ERROR, problem("INTERNAL_ERROR")),
            httpError(Code.NOT_FOUND, "<html><body>404 Not Found</body></html>"),
            httpError(Code.NOT_FOUND, errorBody = null),
            httpError(Code.BAD_GATEWAY, errorBody = ""),
            ApiResponseError.NetworkException(),
            ApiResponseError.TimeoutException(),
        )
    }

    data class KnownReasonModel(val code: Code, val errorCode: String, val expected: SendIntentError)

    private fun input(cause: ApiResponseError) = SendIntentErrorConverter.Input(intentId = INTENT_ID, cause = cause)

    private fun httpError(code: Code, errorBody: String?) = ApiResponseError.HttpException(
        code = code,
        message = null,
        errorBody = errorBody,
    )

    /** The shape the Transaction Service answers with, as seen on the dev gateway. */
    private fun problem(code: String) = """
        {
          "status": 404,
          "title": "Not Found",
          "detail": "intent not found",
          "code": "$code",
          "instance": "/api/v1/intents/$INTENT_ID",
          "timestamp": "2026-09-17T13:05:12.305016Z",
          "traceId": "7b63ddfabc2bb1fe13472043ed3ad3ae"
        }
    """.trimIndent()

    private companion object {
        const val INTENT_ID = "3f7c2a1e-9b4d-4c6a-8e21-5d0f7a9b1c33"
    }
}