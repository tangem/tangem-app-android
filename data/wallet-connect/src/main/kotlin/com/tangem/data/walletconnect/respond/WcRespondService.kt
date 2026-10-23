package com.tangem.data.walletconnect.respond

import arrow.core.Either
import com.tangem.domain.walletconnect.model.WcJsonRpcError
import com.tangem.domain.walletconnect.model.WcRequestError
import com.tangem.domain.walletconnect.model.sdkcopy.WcSdkSessionRequest

interface WcRespondService {

    /**
     * Answers [request] with a successful result. [response] is `null` for the methods whose specification
     * defines no result — `wallet_addEthereumChain` and `wallet_switchEthereumChain` under EIP-3085 and
     * EIP-3326 — where a dApp may treat an empty string as a malformed answer.
     */
    suspend fun respond(request: WcSdkSessionRequest, response: String?): Either<WcRequestError, String>

    fun rejectRequestNonBlock(request: WcSdkSessionRequest, error: WcJsonRpcError = WcJsonRpcError.UserRejected)

    /**
     * Returns `true` only if [request] is still actionable right now: its session is still active
     * and the request itself is still pending (not expired and not already responded). A `false`
     * result means the SDK state was read successfully and the request is genuinely no longer valid.
     *
     * Must be checked immediately before any sensitive action (sign/send), because the confirmation
     * UI keeps an actionable callback alive after the request may have expired or its session was
     * removed.
     *
     * Failures to read the SDK state are **not** swallowed — they propagate to the caller, so a
     * transient SDK error surfaces as a retriable error rather than being mistaken for a genuinely
     * expired request (which would wrongly reject a still-valid request).
     */
    fun isRequestActual(request: WcSdkSessionRequest): Boolean
}