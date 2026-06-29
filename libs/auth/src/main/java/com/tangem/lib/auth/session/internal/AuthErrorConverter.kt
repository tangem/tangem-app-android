package com.tangem.lib.auth.session.internal

import com.tangem.datasource.api.common.response.ApiResponseError
import com.tangem.datasource.api.common.response.ApiResponseError.HttpException.Code
import com.tangem.lib.auth.session.AuthError
import com.tangem.lib.auth.session.AuthErrorResponse
import com.tangem.utils.converter.Converter
import kotlinx.serialization.json.Json
import javax.inject.Inject

/**
 * Converts the raw `ApiResponseError` produced by Retrofit into a typed [AuthError], parsing
 * `application/problem+json` payloads into [AuthErrorResponse] when present. Follows the
 * project pattern set by `TangemPayErrorConverter`, `ExpressErrorConverter`, etc.
 */
internal class AuthErrorConverter @Inject constructor() : Converter<Throwable, AuthError> {

    private val json = Json { ignoreUnknownKeys = true }

    override fun convert(error: Throwable): AuthError = when (error) {
        is ApiResponseError.HttpException -> convertHttp(error)
        is ApiResponseError.NetworkException,
        is ApiResponseError.TimeoutException,
        -> AuthError.NetworkError
        // Unwrap the wrapper so consumers inspect the original failure instead of
        // `ApiResponseError.UnknownException` itself.
        is ApiResponseError.UnknownException -> AuthError.Unknown(error.cause)
        else -> AuthError.Unknown(error)
    }

    private fun convertHttp(error: ApiResponseError.HttpException): AuthError {
        val problem = error.errorBody?.let(::parseErrorResponse)
        return when (error.code) {
            Code.BAD_REQUEST -> AuthError.BadRequest(problem)
            Code.UNAUTHORIZED -> AuthError.Unauthorized(problem)
            Code.FORBIDDEN -> AuthError.Forbidden(problem)
            Code.NOT_FOUND -> AuthError.NotFound(problem)
            Code.CONFLICT -> AuthError.Conflict(problem)
            Code.TOO_MANY_REQUESTS -> AuthError.RateLimited(problem?.retryAfterSeconds, problem)
            else -> if (error.isServerError()) AuthError.ServerUnavailable(problem) else AuthError.Unknown(error)
        }
    }

    private fun parseErrorResponse(response: String): AuthErrorResponse? {
        return runCatching { json.decodeFromString<AuthErrorResponse>(response) }.getOrNull()
    }
}