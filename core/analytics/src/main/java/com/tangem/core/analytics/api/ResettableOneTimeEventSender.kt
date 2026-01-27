package com.tangem.core.analytics.api

import com.tangem.core.analytics.models.AnalyticsEvent
import java.util.concurrent.ConcurrentHashMap

class ResettableOneTimeEventSender(private val analyticsEventHandler: AnalyticsEventHandler) {

    private val sentEvents = ConcurrentHashMap<String, AnalyticsEvent>()

    fun sendEventOnce(key: String, event: AnalyticsEvent) {
        if (sentEvents.putIfAbsent(key, event) == null) {
            analyticsEventHandler.send(event)
        }
    }

    fun reset(key: String) {
        sentEvents.remove(key)
    }
}