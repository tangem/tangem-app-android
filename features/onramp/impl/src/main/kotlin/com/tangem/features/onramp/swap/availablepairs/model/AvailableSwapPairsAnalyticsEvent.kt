package com.tangem.features.onramp.swap.availablepairs.model

import com.tangem.core.analytics.models.AnalyticsEvent
import com.tangem.core.analytics.models.AnalyticsParam.Key.BLOCKCHAIN
import com.tangem.core.analytics.models.AnalyticsParam.Key.TOKEN_PARAM

private const val SWAP_CATEGORY = "Swap"

internal sealed class AvailableSwapPairsAnalyticsEvent(
    event: String,
    params: Map<String, String> = emptyMap(),
) : AnalyticsEvent(SWAP_CATEGORY, event, params) {

    class TokenSelected(
        val token: String,
        val source: String,
        val isSearched: Boolean,
    ) : AvailableSwapPairsAnalyticsEvent(
        event = "Token Selected",
        params = mapOf(
            TOKEN_PARAM to token,
            SOURCE to source,
            SEARCHED to if (isSearched) "True" else "False",
        ),
    ) {
        companion object {
            const val SOURCE = "Source"
            const val SEARCHED = "Searched"
            const val SOURCE_PORTFOLIO = "Portfolio"
            const val SOURCE_MARKETS = "Markets"
        }
    }

    class TokenAdded(
        val token: String,
        val blockchain: String,
    ) : AvailableSwapPairsAnalyticsEvent(
        event = "Token Added",
        params = mapOf(
            TOKEN_PARAM to token,
            BLOCKCHAIN to blockchain,
        ),
    )
}