package com.tangem.tap.data

import com.tangem.core.analytics.models.AnalyticsEvent

internal class TangemSdkWarningEvent(message: String) : AnalyticsEvent(
    category = "Tangem SDK",
    event = "Warning",
    error = IllegalStateException(message),
)