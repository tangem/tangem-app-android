package com.tangem.data.transaction

import com.tangem.core.remote.response.ApiResponse
import com.tangem.core.remote.response.ApiResponseError
import com.tangem.domain.transaction.error.GaslessServiceException

/**
 * Unwraps a gasless-service response, converting a failure into [GaslessServiceException].
 *
 * Plain `getOrThrow` rethrows [ApiResponseError], whose type and HTTP status the domain layer cannot read,
 * so the send flow ends up showing an error with no code at all.
 */
internal fun <T : Any> ApiResponse<T>.getOrThrowGaslessError(): T = when (this) {
    is ApiResponse.Success -> data
    is ApiResponse.Error -> throw cause.toGaslessServiceException()
}

internal fun ApiResponseError.toGaslessServiceException(): GaslessServiceException = when (this) {
    is ApiResponseError.HttpException -> GaslessServiceException(
        httpCode = code.numericCode,
        // The reason phrase is absent over HTTP/2; the error body may be an arbitrarily long page from a proxy
        message = message?.takeIf(String::isNotBlank) ?: code.readableName(),
        cause = this,
    )
    is ApiResponseError.NetworkException -> GaslessServiceException(
        httpCode = null,
        message = "Gasless service is unreachable",
        cause = this,
    )
    is ApiResponseError.TimeoutException -> GaslessServiceException(
        httpCode = null,
        message = "Gasless service request timed out",
        cause = this,
    )
    is ApiResponseError.UnknownException -> GaslessServiceException(
        httpCode = null,
        message = cause.message?.takeIf(String::isNotBlank) ?: "Gasless service request failed",
        cause = this,
    )
}

private fun ApiResponseError.HttpException.Code.readableName(): String {
    return name.split('_').joinToString(separator = " ") { word ->
        word.lowercase().replaceFirstChar(Char::uppercaseChar)
    }
}