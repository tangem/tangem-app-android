package com.tangem.datasource.remote.response

import com.google.common.truth.Truth.assertThat
import com.tangem.core.analytics.api.AnalyticsErrorHandler
import com.tangem.core.remote.response.ApiResponse
import com.tangem.core.remote.response.ApiResponseError
import io.mockk.mockk
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.jupiter.api.Test
import retrofit2.Response

internal class ResponseExtTest {

    private val analyticsErrorHandler: AnalyticsErrorHandler = mockk(relaxed = true)

    @Test
    fun `GIVEN a 204 without a body WHEN the call is declared as Unit THEN it is a success`() {
        // Arrange
        val response = Response.success<Unit>(NO_CONTENT, null)

        // Act
        val actual = response.toSafeApiResponse(analyticsErrorHandler, isBodyOptional = true)

        // Assert
        assertThat(actual).isEqualTo(
            ApiResponse.Success(
                data = Unit,
                code = ApiResponseError.HttpException.Code.NO_CONTENT,
                headers = emptyMap(),
            ),
        )
    }

    @Test
    fun `GIVEN a 200 with a body WHEN the call expects a body THEN the body is returned`() {
        // Arrange
        val response = Response.success("payload")

        // Act
        val actual = response.toSafeApiResponse(analyticsErrorHandler)

        // Assert
        assertThat((actual as ApiResponse.Success).data).isEqualTo("payload")
    }

    @Test
    fun `GIVEN a 204 without a body WHEN the call expects a body THEN it stays an error`() {
        // Arrange
        val response = Response.success<String>(NO_CONTENT, null)

        // Act
        val actual = response.toSafeApiResponse(analyticsErrorHandler)

        // Assert
        assertThat(actual).isInstanceOf(ApiResponse.Error::class.java)
    }

    @Test
    fun `GIVEN a 409 WHEN the call is declared as Unit THEN the http error is kept`() {
        // Arrange
        val response = Response.error<Unit>(CONFLICT, "".toResponseBody())

        // Act
        val actual = response.toSafeApiResponse(analyticsErrorHandler, isBodyOptional = true)

        // Assert
        val cause = (actual as ApiResponse.Error).cause as ApiResponseError.HttpException
        assertThat(cause.code).isEqualTo(ApiResponseError.HttpException.Code.CONFLICT)
    }

    private companion object {
        const val NO_CONTENT = 204
        const val CONFLICT = 409
    }
}