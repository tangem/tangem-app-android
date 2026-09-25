package com.tangem.data.transaction

import com.squareup.moshi.JsonAdapter
import com.tangem.core.remote.response.ApiResponse
import com.tangem.core.remote.response.ApiResponseError
import com.tangem.domain.transaction.error.GaslessServiceException
import com.tangem.domain.transaction.error.TronActivationRefusal
import com.tangem.grow.datasource.gasless.models.GaslessErrorDto
import com.tangem.grow.datasource.gasless.models.GaslessErrorResponse

/**
 * Unwraps a gasless-service response, converting a failure into [GaslessServiceException].
 *
 * Plain `getOrThrow` rethrows [ApiResponseError], whose type and HTTP status the domain layer cannot read,
 * so the send flow ends up showing an error with no code at all.
 *
 * With [errorAdapter] the error body is read as well: the service's own message replaces the HTTP reason phrase,
 * and a refused Tron account activation is carried as [GaslessServiceException.tronActivationRefusal].
 */
internal fun <T : Any> ApiResponse<T>.getOrThrowGaslessError(
    errorAdapter: JsonAdapter<GaslessErrorResponse>? = null,
): T = when (this) {
    is ApiResponse.Success -> data
    is ApiResponse.Error -> throw cause.toGaslessServiceException(errorAdapter)
}

internal fun ApiResponseError.toGaslessServiceException(
    errorAdapter: JsonAdapter<GaslessErrorResponse>? = null,
): GaslessServiceException = when (this) {
    is ApiResponseError.HttpException -> {
        val error = errorAdapter?.let { parseError(it) }
        GaslessServiceException(
            httpCode = code.numericCode,
            // The reason phrase is absent over HTTP/2; the error body may be an arbitrarily long page from a proxy
            message = error?.message?.takeIf(String::isNotBlank)
                ?: message?.takeIf(String::isNotBlank)
                ?: code.readableName(),
            cause = this,
            tronActivationRefusal = error?.toTronActivationRefusal(),
        )
    }
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

private fun ApiResponseError.HttpException.parseError(adapter: JsonAdapter<GaslessErrorResponse>): GaslessErrorDto? {
    val body = errorBody?.takeIf(String::isNotBlank) ?: return null
    // A proxy may answer with an HTML page instead of the service's JSON — that is simply "no error code".
    return runCatching { adapter.fromJson(body)?.error }.getOrNull()
}

private fun GaslessErrorDto.toTronActivationRefusal(): TronActivationRefusal? = when (code) {
    "TRON_ACTIVATION_BALANCE_TOO_LOW" -> details?.minimum?.toBigIntegerOrNull()?.let(
        TronActivationRefusal::BalanceTooLow,
    )
    "TRON_ACTIVATION_RATE_LIMITED" -> TronActivationRefusal.RateLimited(isPerWallet = details?.scope == "wallet")
    else -> null
}

private fun ApiResponseError.HttpException.Code.readableName(): String {
    return name.split('_').joinToString(separator = " ") { word ->
        word.lowercase().replaceFirstChar(Char::uppercaseChar)
    }
}