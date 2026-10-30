package com.tangem.core.analytics.api

/** Measures how long an operation takes and reports it to performance monitoring. */
interface PerformanceTracer {

    /** Starts a trace named [name]; the measurement is reported once the returned trace is stopped. */
    fun start(name: String): PerformanceTrace
}

interface PerformanceTrace {

    fun putAttribute(name: String, value: String)

    fun stop()
}