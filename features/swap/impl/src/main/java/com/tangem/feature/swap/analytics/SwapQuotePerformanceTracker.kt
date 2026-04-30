package com.tangem.feature.swap.analytics

import com.google.firebase.perf.FirebasePerformance
import com.google.firebase.perf.metrics.Trace

internal class SwapQuotePerformanceTracker {

    private var trace: Trace? = null

    fun onLoadingStarted(providersCount: Int) {
        trace?.stop()
        trace = FirebasePerformance.getInstance().newTrace(SWAP_QUOTES_LOADED_TRACE_NAME).apply {
            putAttribute(PROVIDERS_COUNT, providersCount.toString())
            start()
        }
    }

    fun onLoadingFinished(hasError: Boolean) {
        trace?.apply {
            putAttribute(HAS_ERROR, if (hasError) "Yes" else "No")
            stop()
        }
        trace = null
    }

    fun onDestroy() {
        trace?.stop()
        trace = null
    }

    private companion object {
        const val SWAP_QUOTES_LOADED_TRACE_NAME = "Swap_quotes_loaded"
        const val PROVIDERS_COUNT = "providers_count"
        const val HAS_ERROR = "has_error"
    }
}