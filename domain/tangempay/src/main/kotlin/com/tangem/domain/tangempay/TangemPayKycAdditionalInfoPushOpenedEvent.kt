package com.tangem.domain.tangempay

import com.tangem.core.analytics.models.AnalyticsEvent

class TangemPayKycAdditionalInfoPushOpenedEvent : AnalyticsEvent(
    category = "Push",
    event = "Push Notification Opened",
    params = mapOf("verification_context" to "additional_info"),
)