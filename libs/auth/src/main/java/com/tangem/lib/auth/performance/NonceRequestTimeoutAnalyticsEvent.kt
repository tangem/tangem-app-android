package com.tangem.lib.auth.performance

import com.tangem.core.analytics.models.AnalyticsEvent

internal class NonceRequestTimeoutAnalyticsEvent(type: NonceRequestType) : AnalyticsEvent(
    category = "Backend Auth",
    event = "Nonce Request Timeout",
    params = mapOf("Type" to type.analyticsValue),
)