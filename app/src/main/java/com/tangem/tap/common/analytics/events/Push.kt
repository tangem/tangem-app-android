package com.tangem.tap.common.analytics.events

import com.tangem.core.analytics.models.AnalyticsEvent

internal sealed class Push(event: String) : AnalyticsEvent(
    category = "Push",
    event = event,
    params = emptyMap(),
    error = null,
) {

    data object PushNotificationOpened : Push(event = "Push Notification Opened")
}