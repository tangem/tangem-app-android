package com.tangem.features.home.impl.analytics

import com.tangem.core.analytics.models.AnalyticsEvent

internal sealed class Shop(
    event: String,
    params: Map<String, String> = mapOf(),
) : AnalyticsEvent("Shop", event, params) {

    object ScreenOpened : Shop("Shop Screen Opened")
}