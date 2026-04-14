package com.tangem.tap.common.analytics.events

import com.tangem.core.analytics.models.AnalyticsEvent
import com.tangem.core.analytics.models.CriticalEvent

internal sealed class Push(event: String) : AnalyticsEvent(
    category = "Push",
    event = event,
    params = emptyMap(),
) {

    class PushNotificationOpened : Push(event = "Push Notification Opened"), CriticalEvent
}