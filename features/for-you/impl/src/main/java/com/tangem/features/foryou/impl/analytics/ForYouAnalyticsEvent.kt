package com.tangem.features.foryou.impl.analytics

import com.tangem.core.analytics.models.AnalyticsEvent
import com.tangem.core.analytics.models.AnalyticsParam.Key.BLOCKCHAIN
import com.tangem.core.analytics.models.AnalyticsParam.Key.INFO
import com.tangem.core.analytics.models.AnalyticsParam.Key.PERIOD
import com.tangem.core.analytics.models.AnalyticsParam.Key.TOKEN_PARAM
import com.tangem.core.analytics.models.AnalyticsParam.Key.TYPE

/**
 * For You analytics events.
 *
 * Every event takes values that are already analytics-ready — callers resolve them via
 * [toAnalyticsTokenAndNetwork] and the `analyticsValue` of the type at hand — so an event never reaches
 * into a domain or UI model itself.
 *
 * The token-summary events fire on every summary open regardless of the entry point, so they are also
 * reported when the summary is reached from market details — the screen itself is a For You surface.
 *
 * A `null` [BLOCKCHAIN] value omits the param: not every token has a single network to report.
 *
 * @param event  event name
 * @param params params
 */
internal sealed class ForYouAnalyticsEvent(
    event: String,
    params: Map<String, String> = emptyMap(),
) : AnalyticsEvent(category = "For You", event = event, params = params) {

    data object ScreenOpened : ForYouAnalyticsEvent(event = "For You Screen Opened")

    data object AccountFilterOpened : ForYouAnalyticsEvent(event = "Account Filter Opened")

    data object ApplySelected : ForYouAnalyticsEvent(event = "Apply Selected")

    data object ExploreAllTokens : ForYouAnalyticsEvent(event = "Explore All Tokens")

    data object DiagramTap : ForYouAnalyticsEvent(event = "Diagram Tap")

    data class FilterInterval(private val period: String) : ForYouAnalyticsEvent(
        event = "Filter Interval",
        params = mapOf(PERIOD to period),
    )

    data class TokenSummary(
        private val token: String,
        private val blockchain: String?,
    ) : ForYouAnalyticsEvent(
        event = "Token Summary",
        params = tokenParams(token, blockchain),
    )

    data class TokenSummaryInterval(
        private val token: String,
        private val blockchain: String?,
        private val period: String,
    ) : ForYouAnalyticsEvent(
        event = "Token Summary Interval",
        params = buildMap {
            put(TOKEN_PARAM, token)
            blockchain?.let { put(BLOCKCHAIN, it) }
            put(PERIOD, period)
        },
    )

    data class GoToSwap(
        private val token: String,
        private val blockchain: String?,
    ) : ForYouAnalyticsEvent(
        event = "Go To Swap",
        params = tokenParams(token, blockchain),
    )

    data class AddFunds(
        private val token: String,
        private val blockchain: String?,
    ) : ForYouAnalyticsEvent(
        event = "Add Funds",
        params = tokenParams(token, blockchain),
    )

    data class EarnTokenOpened(
        private val token: String,
        private val blockchain: String?,
        private val type: String,
    ) : ForYouAnalyticsEvent(
        event = "Earn Token Opened",
        params = buildMap {
            put(TOKEN_PARAM, token)
            blockchain?.let { put(BLOCKCHAIN, it) }
            put(TYPE, type)
        },
    )

    data class IndicatorInfo(private val info: String) : ForYouAnalyticsEvent(
        event = "Indicator Info",
        params = mapOf(INFO to info),
    )
}

private fun tokenParams(token: String, blockchain: String?): Map<String, String> = buildMap {
    put(TOKEN_PARAM, token)
    blockchain?.let { put(BLOCKCHAIN, it) }
}