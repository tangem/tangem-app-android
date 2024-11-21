package com.tangem.core.analytics.api

import com.tangem.core.analytics.models.AnalyticsEvent
import com.tangem.core.analytics.models.EventValue

/**
[REDACTED_AUTHOR]
 */
interface AnalyticsEventHandler {
    fun send(event: AnalyticsEvent)
}

interface AnalyticsHandler : AnalyticsEventHandler {
    fun id(): String

    fun send(eventId: String, params: Map<String, EventValue> = emptyMap())

    override fun send(event: AnalyticsEvent) {
        send(event.id, event.params)
    }
}

interface ErrorEventHandler {
    fun send(error: Throwable, params: Map<String, EventValue> = emptyMap())
}

interface AnalyticsHandlerHolder {
    fun addHandler(name: String, handler: AnalyticsHandler)
    fun removeHandler(name: String): AnalyticsHandler?
}