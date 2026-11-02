package com.tangem.core.analytics.handlers.firebase

import com.google.firebase.perf.FirebasePerformance
import com.google.firebase.perf.metrics.Trace
import com.tangem.core.analytics.api.PerformanceTrace
import com.tangem.core.analytics.api.PerformanceTracer

internal class FirebasePerformanceTracer : PerformanceTracer {

    override fun start(name: String): PerformanceTrace {
        return FirebasePerformanceTrace(trace = FirebasePerformance.getInstance().newTrace(name).apply { start() })
    }

    private class FirebasePerformanceTrace(private val trace: Trace) : PerformanceTrace {

        override fun putAttribute(name: String, value: String) {
            trace.putAttribute(name, value)
        }

        override fun stop() {
            trace.stop()
        }
    }
}