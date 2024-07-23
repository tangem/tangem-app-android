package com.tangem.features.pushnotifications.api.analytics

import com.tangem.core.analytics.models.AnalyticsEvent
import com.tangem.core.analytics.models.AnalyticsParam

sealed class PushNotificationAnalyticEvents(
    event: String,
    params: Map<String, String> = mapOf(),
) : AnalyticsEvent(category = "Push", event = event, params = params) {

    data class ButtonAllow(
        val source: AnalyticsParam.ScreensSources,
    ) : PushNotificationAnalyticEvents(
        event = "Button - Allow",
        params = mapOf(
            AnalyticsParam.SOURCE to source.value,
        ),
    )

    data class ButtonLater(
        val source: AnalyticsParam.ScreensSources,
    ) : PushNotificationAnalyticEvents(
        event = "Button - Later",
        params = mapOf(
            AnalyticsParam.SOURCE to source.value,
        ),
    )

    data object ButtonCancel : PushNotificationAnalyticEvents(
        event = "Button - Cancel",
    )

    data class PermissionStatus(
        val isAllowed: Boolean,
    ) : PushNotificationAnalyticEvents(
        event = "Permission Status",
        params = mapOf(
            AnalyticsParam.STATE to if (isAllowed) "Allow" else "Cancel",
        ),
    )
}
