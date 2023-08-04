package com.tangem.core.analytics.api

import com.tangem.core.analytics.models.AnalyticsEvent

/**
* [REDACTED_AUTHOR]
 */
interface AnalyticsEventHandler {
    fun send(event: AnalyticsEvent)
}

interface AnalyticsHandler : AnalyticsEventHandler {
    fun id(): String

    fun send(event: String, params: Map<String, String> = emptyMap())

    override fun send(event: AnalyticsEvent) {
        send(prepareEventString(event), event.params)
    }

    fun prepareEventString(event: AnalyticsEvent): String = "[${event.category}] ${event.event}"
}

interface ErrorEventHandler {
    fun send(error: Throwable, params: Map<String, String> = emptyMap())
}

interface AnalyticsHandlerHolder {
    fun addHandler(name: String, handler: AnalyticsHandler)
    fun removeHandler(name: String): AnalyticsHandler?
}
