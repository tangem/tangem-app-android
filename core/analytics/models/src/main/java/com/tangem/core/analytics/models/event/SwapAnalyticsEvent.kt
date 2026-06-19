package com.tangem.core.analytics.models.event

import com.tangem.core.analytics.models.AnalyticsEvent
import com.tangem.core.analytics.models.AnalyticsParam.Key.SEARCHED
import com.tangem.core.analytics.models.AnalyticsParam.Key.SOURCE
import com.tangem.core.analytics.models.AnalyticsParam.Key.TOKEN_PARAM
import com.tangem.core.analytics.models.AnalyticsParam.Key.TYPE
import com.tangem.core.analytics.models.AnalyticsParam.ScreensSources

/**
[REDACTED_AUTHOR]
 */
sealed class SwapAnalyticsEvent(
    event: String,
    params: Map<String, String> = emptyMap(),
) : AnalyticsEvent("Swap", event, params) {

    class FilterProvider(filterType: String) : SwapAnalyticsEvent(
        event = "Filter Provider",
        params = mapOf(TYPE to filterType),
    )
}