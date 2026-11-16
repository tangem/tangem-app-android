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

interface AnalyticsUserPropertiesHandler {
    fun setUserProperties(properties: Map<String, String>)
}

interface AnalyticsHandler : AnalyticsEventHandler {

    fun id(): String

    override fun send(event: AnalyticsEvent)
}