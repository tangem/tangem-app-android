package com.tangem.features.swap.v2.impl.amount.analytics

import com.tangem.core.analytics.models.AnalyticsEvent
import com.tangem.core.analytics.models.AnalyticsParam.Key.PROVIDER

internal sealed class SwapAmountAnalyticEvents(
    category: String,
    event: String,
    params: Map<String, String> = mapOf(),
) : AnalyticsEvent(category = category, event = event, params = params) {

    data class ProviderSelectorClicked(
        val categoryName: String,
    ) : SwapAmountAnalyticEvents(
        category = categoryName,
        event = "Provider Clicked",
    )

    data class ProviderChosen(
        val categoryName: String,
        val providerName: String,
    ) : SwapAmountAnalyticEvents(
        category = categoryName,
        event = "Provider Chosen",
        params = mapOf(PROVIDER to providerName),
    )
}