package com.tangem.data.walletconnect.respond

import arrow.core.Either
import com.tangem.domain.walletconnect.model.WcRequestError
import com.tangem.domain.walletconnect.model.sdkcopy.WcSdkSessionRequest

interface WcRespondService {
    suspend fun respond(request: WcSdkSessionRequest, response: String): Either<WcRequestError, String>
    fun rejectRequestNonBlock(request: WcSdkSessionRequest, message: String = "")

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