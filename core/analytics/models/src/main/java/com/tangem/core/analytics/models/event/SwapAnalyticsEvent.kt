package com.tangem.core.analytics.models.event

import com.tangem.core.analytics.models.AnalyticsEvent
import com.tangem.core.analytics.models.AnalyticsParam.Key.SEARCHED
import com.tangem.core.analytics.models.AnalyticsParam.Key.SOURCE
import com.tangem.core.analytics.models.AnalyticsParam.Key.TOKEN_PARAM
import com.tangem.core.analytics.models.AnalyticsParam.ScreensSources

/**
[REDACTED_AUTHOR]
 */
sealed class SwapAnalyticsEvent(
    event: String,
    params: Map<String, String> = emptyMap(),
) : AnalyticsEvent("Swap", event, params) {

    data class TokenSelected(
        val token: String,
        val source: ScreensSources,
        val isSearched: Boolean,
    ) : SwapAnalyticsEvent(
        event = "Token Selected",
        params = mapOf(
            TOKEN_PARAM to token,
            SOURCE to source.value,
            SEARCHED to if (isSearched) "True" else "False",
        ),
    )
}