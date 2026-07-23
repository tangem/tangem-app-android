package com.tangem.data.polymarket.error

import com.squareup.moshi.Moshi
import com.tangem.core.remote.response.ApiResponseError
import com.tangem.datasource.api.polymarket.models.ProblemDetailResponse
import com.tangem.datasource.di.NetworkMoshi
import com.tangem.domain.polymarket.model.PolymarketWalletError
import com.tangem.domain.polymarket.model.PolymarketWalletError.RelayerRejected
import javax.inject.Inject

/**
 * Maps a BFF [ApiResponseError] into a typed [PolymarketWalletError].
 *
 * HTTP status → variant is stable; the `422` sub-variants are matched on the RFC-7807 ProblemDetail
 * `detail` message (the only machine hint the BFF exposes today), with a defensive [RelayerRejected.Other]
 * fallback for unrecognised messages.
 */
internal class PolymarketWalletErrorResolver @Inject constructor(
    @NetworkMoshi moshi: Moshi,
) {

    private val problemDetailAdapter = moshi.adapter(ProblemDetailResponse::class.java)

    fun resolve(error: ApiResponseError): PolymarketWalletError = when (error) {
        is ApiResponseError.HttpException -> resolveHttp(error)
        is ApiResponseError.NetworkException,
        is ApiResponseError.TimeoutException,
        -> PolymarketWalletError.Network
        is ApiResponseError.UnknownException ->
            PolymarketWalletError.Unexpected(httpCode = null, detail = error.cause.message ?: error.message)
    }

    private fun resolveHttp(error: ApiResponseError.HttpException): PolymarketWalletError {
        val detail = parseDetail(error.errorBody)
        return when (error.code.numericCode) {
            HTTP_BAD_REQUEST -> PolymarketWalletError.InvalidRequest
            HTTP_UNAUTHORIZED -> PolymarketWalletError.Unauthorized
            HTTP_CONFLICT -> PolymarketWalletError.WalletNotDeployed
            HTTP_UNPROCESSABLE_ENTITY -> resolveRelayerRejection(detail)
            HTTP_BAD_GATEWAY -> PolymarketWalletError.RelayerUnavailable
            else -> PolymarketWalletError.Unexpected(httpCode = error.code.numericCode, detail = detail)
        }
    }

    private fun resolveRelayerRejection(detail: String?): RelayerRejected {
        val message = detail.orEmpty()
        return when {
            message.contains(NOT_REGISTERED, ignoreCase = true) -> RelayerRejected.WalletNotRegistered
            message.contains(DEADLINE_TOO_SOON, ignoreCase = true) -> RelayerRejected.DeadlineTooSoon
            message.contains(INVALID_SIGNATURE, ignoreCase = true) -> RelayerRejected.InvalidSignature
            message.contains(NONCE_REUSED, ignoreCase = true) -> RelayerRejected.NonceReused
            else -> RelayerRejected.Other(detail = detail)
        }
    }

    private fun parseDetail(errorBody: String?): String? =
        errorBody?.let { body -> runCatching { problemDetailAdapter.fromJson(body)?.detail }.getOrNull() }

    private companion object {
        const val HTTP_BAD_REQUEST = 400
        const val HTTP_UNAUTHORIZED = 401
        const val HTTP_CONFLICT = 409
        const val HTTP_UNPROCESSABLE_ENTITY = 422
        const val HTTP_BAD_GATEWAY = 502

        // Relayer rejection messages from the BFF retry table (matched case-insensitively, as substrings).
        // Kept specific enough to avoid misclassifying unrelated 422s; anything else → RelayerRejected.Other.
        const val NOT_REGISTERED = "not registered"
        const val DEADLINE_TOO_SOON = "deadline is too soon"
        const val INVALID_SIGNATURE = "invalid signature"
        const val NONCE_REUSED = "nonce reused"
    }
}