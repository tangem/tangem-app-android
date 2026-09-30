package com.tangem.lib.auth.performance

import com.tangem.core.remote.response.ApiResponse
import com.tangem.lib.auth.api.models.response.NonceApiResponse

/**
 * Observes a nonce network request (performance trace, timeout reporting). Abstracted so the auth flows
 * stay free of the Firebase SDK and analytics (and unit-testable with a pass-through double).
 */
fun interface NonceRequestTracker {

    suspend fun track(
        type: NonceRequestType,
        request: suspend () -> ApiResponse<NonceApiResponse>,
    ): ApiResponse<NonceApiResponse>
}