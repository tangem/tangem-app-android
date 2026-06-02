package com.tangem.lib.auth.session.internal

import com.google.common.truth.Truth.assertThat
import com.tangem.datasource.api.common.response.ApiResponseError
import com.tangem.datasource.api.common.response.ApiResponseError.HttpException.Code
import com.tangem.lib.auth.session.AuthError
import com.tangem.lib.auth.session.AuthErrorResponse
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AuthErrorConverterTest {

    private val converter = AuthErrorConverter()

    private val sampleBody = """
        {
          "type": "https://problems.tangem.com/auth/invalid-signature",
          "title": "Bad Request",
          "status": 400,
          "detail": "Nonce or signature validation failed.",
          "instance": "/api/v1/auth/refresh",
          "code": "invalid_signature",
          "retryAfterSeconds": null
        }
    """.trimIndent()

    private val sampleProblem = AuthErrorResponse(
        type = "https://problems.tangem.com/auth/invalid-signature",
        title = "Bad Request",
        status = 400,
        detail = "Nonce or signature validation failed.",
        instance = "/api/v1/auth/refresh",
        code = "invalid_signature",
        retryAfterSeconds = null,
    )

    @Test
    fun `400 with problem body is converted to BadRequest with parsed problem`() {
        val result = converter.convert(httpError(Code.BAD_REQUEST, sampleBody))

        assertThat(result).isEqualTo(AuthError.BadRequest(sampleProblem))
    }

    @Test
    fun `400 without body is converted to BadRequest with null problem`() {
        val result = converter.convert(httpError(Code.BAD_REQUEST, errorBody = null))

        assertThat(result).isEqualTo(AuthError.BadRequest(problem = null))
    }

    @Test
    fun `401 is converted to Unauthorized`() {
        val result = converter.convert(httpError(Code.UNAUTHORIZED, sampleBody))

        assertThat(result).isInstanceOf(AuthError.Unauthorized::class.java)
        assertThat((result as AuthError.Unauthorized).problem).isEqualTo(sampleProblem)
    }

    @Test
    fun `403 is converted to Forbidden`() {
        val result = converter.convert(httpError(Code.FORBIDDEN, sampleBody))

        assertThat(result).isInstanceOf(AuthError.Forbidden::class.java)
    }

    @Test
    fun `404 is converted to NotFound`() {
        val result = converter.convert(httpError(Code.NOT_FOUND, sampleBody))

        assertThat(result).isInstanceOf(AuthError.NotFound::class.java)
    }

    @Test
    fun `429 surfaces retryAfterSeconds from problem`() {
        val rateLimitBody = """
            {
              "type": "https://problems.tangem.com/auth/rate-limit",
              "title": "Too Many Requests",
              "status": 429,
              "detail": "Try again later.",
              "instance": "/api/v1/auth/refresh",
              "code": "rate_limited",
              "retryAfterSeconds": 45
            }
        """.trimIndent()

        val result = converter.convert(httpError(Code.TOO_MANY_REQUESTS, rateLimitBody))

        assertThat(result).isInstanceOf(AuthError.RateLimited::class.java)
        val rateLimited = result as AuthError.RateLimited
        assertThat(rateLimited.retryAfterSeconds).isEqualTo(45)
        assertThat(rateLimited.problem?.code).isEqualTo("rate_limited")
    }

    @Test
    fun `429 without body has null retryAfterSeconds`() {
        val result = converter.convert(httpError(Code.TOO_MANY_REQUESTS, errorBody = null))

        assertThat(result).isEqualTo(AuthError.RateLimited(retryAfterSeconds = null, problem = null))
    }

    @Test
    fun `500 is converted to ServerUnavailable`() {
        val result = converter.convert(httpError(Code.INTERNAL_SERVER_ERROR, errorBody = null))

        assertThat(result).isInstanceOf(AuthError.ServerUnavailable::class.java)
    }

    @Test
    fun `502 is converted to ServerUnavailable`() {
        val result = converter.convert(httpError(Code.BAD_GATEWAY, errorBody = null))

        assertThat(result).isInstanceOf(AuthError.ServerUnavailable::class.java)
    }

    @Test
    fun `503 is converted to ServerUnavailable`() {
        val result = converter.convert(httpError(Code.SERVICE_UNAVAILABLE, errorBody = null))

        assertThat(result).isInstanceOf(AuthError.ServerUnavailable::class.java)
    }

    @Test
    fun `non-server 4xx not explicitly mapped becomes Unknown`() {
        // 418 I_M_A_TEAPOT is a 4xx that isServerError() reports as non-server.
        val httpException = httpError(Code.IM_A_TEAPOT, errorBody = null)

        val result = converter.convert(httpException)

        assertThat(result).isInstanceOf(AuthError.Unknown::class.java)
        assertThat((result as AuthError.Unknown).cause).isSameInstanceAs(httpException)
    }

    @Test
    fun `NetworkException becomes NetworkError`() {
        val result = converter.convert(ApiResponseError.NetworkException())

        assertThat(result).isSameInstanceAs(AuthError.NetworkError)
    }

    @Test
    fun `TimeoutException becomes NetworkError`() {
        val result = converter.convert(ApiResponseError.TimeoutException())

        assertThat(result).isSameInstanceAs(AuthError.NetworkError)
    }

    @Test
    fun `UnknownException is unwrapped to its underlying cause`() {
        val cause = IllegalStateException("boom")
        val result = converter.convert(ApiResponseError.UnknownException(cause))

        // Consumers reading AuthError.Unknown.cause should see the original failure, not the wrapper.
        assertThat(result).isEqualTo(AuthError.Unknown(cause))
    }

    @Test
    fun `non-ApiResponseError Throwable is wrapped as Unknown`() {
        val cause = RuntimeException("misc")
        val result = converter.convert(cause)

        assertThat(result).isEqualTo(AuthError.Unknown(cause))
    }

    @Test
    fun `malformed JSON in errorBody yields null problem but preserves AuthError shape`() {
        val result = converter.convert(httpError(Code.BAD_REQUEST, errorBody = "{not json"))

        assertThat(result).isEqualTo(AuthError.BadRequest(problem = null))
    }

    @Test
    fun `unknown JSON fields in errorBody are ignored (ignoreUnknownKeys)`() {
        val bodyWithExtra = """
            {
              "type": "https://problems.tangem.com/auth/x",
              "title": "Bad Request",
              "status": 400,
              "detail": null,
              "instance": null,
              "code": null,
              "retryAfterSeconds": null,
              "futureField": "ignored"
            }
        """.trimIndent()

        val result = converter.convert(httpError(Code.BAD_REQUEST, bodyWithExtra))

        assertThat(result).isInstanceOf(AuthError.BadRequest::class.java)
        assertThat((result as AuthError.BadRequest).problem?.title).isEqualTo("Bad Request")
    }

    private fun httpError(code: Code, errorBody: String?): ApiResponseError.HttpException =
        ApiResponseError.HttpException(code = code, message = null, errorBody = errorBody)
}