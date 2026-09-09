package com.tangem.lib.auth.performance

import com.tangem.core.remote.response.ApiResponse
import com.tangem.lib.auth.api.models.response.NonceApiResponse

/**
 * Traces the wallet-nonce network request for performance monitoring. Abstracted so the registrar
 * stays free of the Firebase SDK (and unit-testable with a pass-through double).
 */
fun interface NonceRequestPerformanceTracker {

    suspend fun track(request: suspend () -> ApiResponse<NonceApiResponse>): ApiResponse<NonceApiResponse>
}