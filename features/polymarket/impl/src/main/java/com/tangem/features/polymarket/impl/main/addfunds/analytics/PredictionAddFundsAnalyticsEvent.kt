package com.tangem.features.polymarket.impl.main.addfunds.analytics

import com.tangem.core.analytics.models.AnalyticsEvent
import com.tangem.core.analytics.models.AnalyticsParam

internal sealed class PredictionAddFundsAnalyticsEvent(
    event: String,
    params: Map<String, String> = emptyMap(),
) : AnalyticsEvent(category = CATEGORY, event = event, params = params) {

    class MethodScreenOpened : PredictionAddFundsAnalyticsEvent(
        event = "Method Screen Opened",
        params = mapOf(AnalyticsParam.SOURCE to AnalyticsParam.ScreensSources.Predictions.value),
    )

    class ButtonSwap : PredictionAddFundsAnalyticsEvent(event = "Button - Swap")

    class ButtonReceive : PredictionAddFundsAnalyticsEvent(event = "Button - Receive")

    private companion object {
        const val CATEGORY = "Add Funds"
    }
}