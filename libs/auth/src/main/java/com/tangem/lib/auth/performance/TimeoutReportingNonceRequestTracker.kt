package com.tangem.lib.auth.performance

import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.remote.response.ApiResponse
import com.tangem.lib.auth.api.models.response.NonceApiResponse
import com.tangem.utils.logging.TangemLogger
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration

/**
 * Reports a nonce request that got no response within [timeout]. The request itself is left running —
 * this only observes it, the HTTP client's own timeouts still decide when it fails.
 */
internal class TimeoutReportingNonceRequestTracker(
    private val delegate: NonceRequestTracker,
    private val analyticsEventHandler: AnalyticsEventHandler,
    private val timeout: Duration,
) : NonceRequestTracker {

    override suspend fun track(
        type: NonceRequestType,
        request: suspend () -> ApiResponse<NonceApiResponse>,
    ): ApiResponse<NonceApiResponse> = coroutineScope {
        val watchdog = launch {
            delay(timeout)
            TangemLogger.e("${type.analyticsValue} nonce request got no response within $timeout")
            analyticsEventHandler.send(NonceRequestTimeoutAnalyticsEvent(type))
        }
        try {
            delegate.track(type, request)
        } finally {
            watchdog.cancel()
        }
    }
}