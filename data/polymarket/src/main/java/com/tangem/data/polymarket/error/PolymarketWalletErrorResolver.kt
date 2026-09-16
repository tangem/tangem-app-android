package com.tangem.data.polymarket.error

import com.tangem.core.remote.response.ApiResponseError
import com.tangem.core.remote.response.ApiResponseError.HttpException.Code
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
    private val problemDetailParser: ProblemDetailParser,
) {

    fun resolve(error: ApiResponseError): PolymarketWalletError = when (error) {
        is ApiResponseError.HttpException -> resolveHttp(error)
        is ApiResponseError.NetworkException,
        is ApiResponseError.TimeoutException,
        -> PolymarketWalletError.Network
        is ApiResponseError.UnknownException ->
            PolymarketWalletError.Unexpected(httpCode = null, detail = error.cause.message ?: error.message)
    }

    private fun resolveHttp(error: ApiResponseError.HttpException): PolymarketWalletError {
        val detail = problemDetailParser.parseDetail(error.errorBody)
        return when (error.code) {
            Code.BAD_REQUEST -> PolymarketWalletError.InvalidRequest
            Code.UNAUTHORIZED -> PolymarketWalletError.Unauthorized
            Code.CONFLICT -> PolymarketWalletError.WalletNotDeployed
            Code.UNPROCESSABLE_ENTITY -> resolveRelayerRejection(detail)
            Code.BAD_GATEWAY -> PolymarketWalletError.RelayerUnavailable
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

    private companion object {
        // Relayer rejection messages from the BFF retry table (matched case-insensitively, as substrings).
        // Kept specific enough to avoid misclassifying unrelated 422s; anything else → RelayerRejected.Other.
        const val NOT_REGISTERED = "not registered"
        const val DEADLINE_TOO_SOON = "deadline is too soon"
        const val INVALID_SIGNATURE = "invalid signature"
        const val NONCE_REUSED = "nonce reused"
    }
}