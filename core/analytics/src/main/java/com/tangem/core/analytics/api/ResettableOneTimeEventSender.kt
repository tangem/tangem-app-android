package com.tangem.core.analytics.api

import com.tangem.core.analytics.models.AnalyticsEvent
import java.util.concurrent.ConcurrentHashMap

class ResettableOneTimeEventSender(val analyticsEventHandler: AnalyticsEventHandler) {

    private val sentEvents = ConcurrentHashMap<String, AnalyticsEvent>()

    fun sendEventOnce(key: String, event: AnalyticsEvent) {
        if (sentEvents[key] != null) return
        analyticsEventHandler.send(event)
        sentEvents[key] = event
    }

    fun reset(key: String) {
        sentEvents.remove(key)
    }
}