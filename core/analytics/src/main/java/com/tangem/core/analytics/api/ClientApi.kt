package com.tangem.core.analytics.api

/**
[REDACTED_AUTHOR]
 */
interface EventLogger {
    fun logEvent(event: String, params: Map<String, String> = emptyMap())
}

interface ErrorEventLogger {
    fun logErrorEvent(event: String, params: Map<String, String> = emptyMap())
}

interface ExceptionLogger {
    fun logException(error: Throwable, params: Map<String, String> = emptyMap())
}