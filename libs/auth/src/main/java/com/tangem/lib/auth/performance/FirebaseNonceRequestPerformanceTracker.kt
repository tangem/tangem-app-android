package com.tangem.lib.auth.performance

import com.google.firebase.perf.FirebasePerformance
import com.tangem.core.remote.response.ApiResponse
import com.tangem.lib.auth.api.models.response.NonceApiResponse

internal class FirebaseNonceRequestPerformanceTracker : NonceRequestTracker {

    override suspend fun track(
        type: NonceRequestType,
        request: suspend () -> ApiResponse<NonceApiResponse>,
    ): ApiResponse<NonceApiResponse> {
        // Telemetry is best-effort: an unavailable/uninitialized Firebase Performance must never fail
        // the nonce request itself, so trace creation and every trace call are guarded.
        val trace = runCatching { FirebasePerformance.getInstance().newTrace(type.traceName).apply { start() } }
            .getOrNull()
        return try {
            request().also { response ->
                runCatching {
                    trace?.putAttribute(RESULT_ATTRIBUTE, if (response is ApiResponse.Success) SUCCESS else ERROR)
                }
            }
        } finally {
            runCatching { trace?.stop() }
        }
    }

    private companion object {
        const val RESULT_ATTRIBUTE = "result"
        const val SUCCESS = "success"
        const val ERROR = "error"
    }
}