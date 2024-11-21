package com.tangem.features.pushnotifications.api.analytics

import com.tangem.core.analytics.models.AnalyticsEvent
import com.tangem.core.analytics.models.AnalyticsParam
import com.tangem.core.analytics.models.EventValue

sealed class PushNotificationAnalyticEvents(
    event: String,
    params: Map<String, EventValue> = mapOf(),
) : AnalyticsEvent(category = "Push", event = event, params = params) {

    data class ButtonAllow(
        val source: AnalyticsParam.ScreensSources,
    ) : PushNotificationAnalyticEvents(
        event = "Button - Allow",
        params = mapOf(
            AnalyticsParam.SOURCE to source.value.asStringValue(),
        ),
    )

    data class ButtonCancel(
        val source: AnalyticsParam.ScreensSources,
    ) : PushNotificationAnalyticEvents(
        event = "Button - Cancel",
        params = mapOf(
            AnalyticsParam.SOURCE to source.value.asStringValue(),
        ),
    )

    data class ButtonLater(
        val source: AnalyticsParam.ScreensSources,
    ) : PushNotificationAnalyticEvents(
        event = "Button - Later",
        params = mapOf(
            AnalyticsParam.SOURCE to source.value.asStringValue(),
        ),
    )

    data class PermissionStatus(
        val isAllowed: Boolean,
    ) : PushNotificationAnalyticEvents(
        event = "Permission Status",
        params = mapOf(
            AnalyticsParam.STATE to if (isAllowed) "Allow".asStringValue() else "Cancel".asStringValue(),
        ),
    )
}