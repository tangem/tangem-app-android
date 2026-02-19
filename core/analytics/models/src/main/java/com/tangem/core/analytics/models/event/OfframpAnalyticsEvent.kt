package com.tangem.core.analytics.models.event

import com.tangem.core.analytics.models.AnalyticsEvent

/**
 * Offramp (withdraw/sell) analytics events
 */
sealed class OfframpAnalyticsEvent(
    event: String,
    params: Map<String, String> = emptyMap(),
) : AnalyticsEvent(category = "Token / Withdraw", event = event, params = params) {

    /**
     * Withdraw screen opened event
     */
    data object ScreenOpened : OfframpAnalyticsEvent("Withdraw Screen Opened")
}