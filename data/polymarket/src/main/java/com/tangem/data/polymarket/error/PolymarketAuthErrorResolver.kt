package com.tangem.data.polymarket.error

import com.tangem.core.remote.response.ApiResponseError
import com.tangem.core.remote.response.ApiResponseError.HttpException.Code
import com.tangem.domain.polymarket.model.PolymarketAuthError
import javax.inject.Inject

/**
 * Maps CLOB auth failures onto [PolymarketAuthError].
 *
 * The onboarding spec says an address with no key yet answers `404`; the live CLOB answers `400` with
 * `Could not derive api key!` instead, so that body is what marks the case where creating a key is the right
 * next step. Matching on the message keeps every other `400` — a malformed request, a rejected signature —
 * out of the create path.
 */
internal class PolymarketAuthErrorResolver @Inject constructor() {

    fun resolve(error: ApiResponseError): PolymarketAuthError = when (error) {
        is ApiResponseError.HttpException -> resolveHttp(error)
        is ApiResponseError.NetworkException,
        is ApiResponseError.TimeoutException,
        -> PolymarketAuthError.Network
        is ApiResponseError.UnknownException ->
            PolymarketAuthError.Unknown(httpCode = null, detail = error.cause.message ?: error.message)
    }

    private fun resolveHttp(error: ApiResponseError.HttpException): PolymarketAuthError = when {
        error.code == Code.UNAUTHORIZED -> PolymarketAuthError.InvalidSignature
        error.code == Code.NOT_FOUND -> PolymarketAuthError.KeyNotFound
        error.code == Code.TOO_MANY_REQUESTS -> PolymarketAuthError.RateLimited
        error.code == Code.BAD_REQUEST && error.errorBody?.contains(NO_KEY_YET_MARKER) == true ->
            PolymarketAuthError.KeyNotFound
        else -> PolymarketAuthError.Unknown(httpCode = error.code.numericCode, detail = error.errorBody)
    }

    private companion object {
        const val NO_KEY_YET_MARKER = "Could not derive api key"
    }
}