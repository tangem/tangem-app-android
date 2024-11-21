package com.tangem.core.analytics.api

import com.tangem.core.analytics.models.EventValue

/**
[REDACTED_AUTHOR]
 */
interface EventLogger {
    fun logEvent(event: String, params: Map<String, EventValue> = emptyMap())
}

interface ErrorEventLogger {
    fun logErrorEvent(error: Throwable, params: Map<String, EventValue> = emptyMap())
}