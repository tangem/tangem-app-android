package com.tangem.data.polymarket.error

import com.tangem.core.remote.response.ApiResponseError
import com.tangem.core.remote.response.ApiResponseError.HttpException.Code
import com.tangem.domain.polymarket.model.PolymarketSessionKeyError
import javax.inject.Inject

internal class PolymarketSessionKeyErrorResolver @Inject constructor(
    private val problemDetailParser: ProblemDetailParser,
) {

    fun resolve(error: ApiResponseError): PolymarketSessionKeyError = when (error) {
        is ApiResponseError.HttpException -> resolveHttp(error)
        is ApiResponseError.NetworkException,
        is ApiResponseError.TimeoutException,
        -> PolymarketSessionKeyError.Network
        is ApiResponseError.UnknownException -> PolymarketSessionKeyError.Unknown(
            httpCode = null,
            reason = null,
            detail = error.cause.message ?: error.message,
        )
    }

    private fun resolveHttp(error: ApiResponseError.HttpException): PolymarketSessionKeyError {
        val problem = problemDetailParser.parse(error.errorBody)
        val reason = problem?.reason
        val detail = problem?.detail
        return resolveReason(reason = reason, detail = detail) ?: when (error.code) {
            Code.UNAUTHORIZED -> PolymarketSessionKeyError.Unauthorized
            Code.BAD_GATEWAY -> PolymarketSessionKeyError.UpstreamUnavailable
            else -> PolymarketSessionKeyError.Unknown(
                httpCode = error.code.numericCode,
                reason = reason,
                detail = detail,
            )
        }
    }

    @Suppress("CyclomaticComplexMethod")
    private fun resolveReason(reason: String?, detail: String?): PolymarketSessionKeyError? = when (reason) {
        "INVALID_WALLET_ID" -> PolymarketSessionKeyError.InvalidWalletId
        "MALFORMED_REQUEST" -> PolymarketSessionKeyError.MalformedRequest
        "INVALID_DEADLINE" -> PolymarketSessionKeyError.InvalidDeadline
        "INVALID_VALID_UNTIL" -> PolymarketSessionKeyError.InvalidValidUntil
        "INVALID_CALL" -> PolymarketSessionKeyError.InvalidCall
        "INVALID_SIGNATURE" -> PolymarketSessionKeyError.InvalidSignature
        "WALLET_NOT_OWNED" -> PolymarketSessionKeyError.WalletNotOwned
        "SESSION_KEY_NOT_FOUND" -> PolymarketSessionKeyError.SessionKeyNotFound
        "WALLET_NOT_READY" -> PolymarketSessionKeyError.WalletNotReady
        "SESSION_KEY_OWNED_BY_ANOTHER_WALLET" -> PolymarketSessionKeyError.SessionKeyOwnedByAnotherWallet
        "SESSION_KEY_BUSY" -> PolymarketSessionKeyError.SessionKeyBusy
        "DEVICE_HAS_SESSION_KEY" -> PolymarketSessionKeyError.DeviceHasSessionKey
        "INVALID_REPLACEMENT" -> PolymarketSessionKeyError.InvalidReplacement
        "SESSION_KEY_LIMIT_REACHED" -> PolymarketSessionKeyError.SessionKeyLimitReached
        "SESSION_KEY_NOT_TOGGLEABLE" -> PolymarketSessionKeyError.SessionKeyNotToggleable
        "RELAYER_REJECTED" -> PolymarketSessionKeyError.RelayerRejected(detail = detail)
        "NONCE_ALREADY_USED" -> PolymarketSessionKeyError.NonceAlreadyUsed
        "UPSTREAM_UNAVAILABLE" -> PolymarketSessionKeyError.UpstreamUnavailable
        else -> null
    }
}