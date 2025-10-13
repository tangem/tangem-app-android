package com.tangem.domain.card.analytics

import com.tangem.core.analytics.models.AnalyticsEvent

sealed class Shop(
    event: String,
    params: Map<String, String> = emptyMap(),
) : AnalyticsEvent("Shop", event, params) {

    object ScreenOpened : Shop("Shop Screen Opened")
}