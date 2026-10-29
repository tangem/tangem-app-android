package com.tangem.features.mcp.impl.pair.model

import com.google.common.truth.Truth.assertThat
import com.tangem.core.remote.response.ApiResponseError
import com.tangem.test.core.ProvideTestModels
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import java.io.IOException

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class McpAuthorizationErrorCodeTest {

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN error WHEN mapped THEN error code matches the spec`(model: ErrorCodeModel) {
        // Act
        val actual = McpAuthorizationErrorCode.of(model.error)

        // Assert
        assertThat(actual).isEqualTo(model.expected)
    }

    private fun provideTestModels() = listOf(
        ErrorCodeModel(error = httpException(ApiResponseError.HttpException.Code.BAD_REQUEST), expected = "400"),
        ErrorCodeModel(error = httpException(ApiResponseError.HttpException.Code.NOT_FOUND), expected = "404"),
        ErrorCodeModel(
            error = httpException(ApiResponseError.HttpException.Code.INTERNAL_SERVER_ERROR),
            expected = "500",
        ),
        ErrorCodeModel(error = ApiResponseError.NetworkException(), expected = "network"),
        ErrorCodeModel(error = ApiResponseError.TimeoutException(), expected = "timeout"),
        ErrorCodeModel(error = ApiResponseError.UnknownException(IOException()), expected = "unknown"),
        ErrorCodeModel(error = IOException(), expected = "unknown"),
    )

    private fun httpException(code: ApiResponseError.HttpException.Code) =
        ApiResponseError.HttpException(code = code, message = null, errorBody = null)

    data class ErrorCodeModel(val error: Throwable, val expected: String)
}