package com.tangem.core.analytics.api

import com.tangem.core.analytics.models.AnalyticsEvent
import com.tangem.core.analytics.models.ExceptionAnalyticsEvent

/**
 * Sends usual analytics events
 */
interface AnalyticsEventHandler {
    fun send(event: AnalyticsEvent)
}

interface AnalyticsErrorHandler {
    fun sendErrorEvent(event: AnalyticsEvent)
}

interface AnalyticsExceptionHandler {
    fun sendException(event: ExceptionAnalyticsEvent)
}

interface AnalyticsUserIdHandler {
    fun setUserId(userId: String)
    fun clearUserId()
}

interface AnalyticsHandler : AnalyticsEventHandler {

    fun id(): String

    fun send(eventId: String, params: Map<String, String> = emptyMap())

    override fun send(event: AnalyticsEvent) {
        send(event.id, event.params)
    }
}

interface AnalyticsHandlerHolder {
    fun addHandler(name: String, handler: AnalyticsHandler)
    fun removeHandler(name: String): AnalyticsHandler?
}