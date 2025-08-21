package com.tangem.features.managetokens.component.analytics

import com.tangem.core.analytics.models.AnalyticsEvent
import com.tangem.core.analytics.models.AnalyticsParam.Key.BLOCKCHAIN
import com.tangem.core.analytics.models.AnalyticsParam.Key.CHOSEN_TOKEN
import com.tangem.core.analytics.models.AnalyticsParam.Key.TOKEN_PARAM

sealed class CommonManageTokensAnalyticEvents(
    category: String,
    event: String,
    params: Map<String, String> = mapOf(),
) : AnalyticsEvent(category = category, event = event, params = params) {

    data class TokenSearchClicked(
        val categoryName: String,
    ) : CommonManageTokensAnalyticEvents(category = categoryName, event = "Token Search Clicked")

    /** Searched token chosen event */
    data class TokenSearched(
        val categoryName: String,
        val token: String?,
        val blockchain: String?,
        val isTokenChosen: Boolean,
    ) : CommonManageTokensAnalyticEvents(
        category = categoryName,
        event = "Token Searched",
        params = buildMap {
            put(CHOSEN_TOKEN, if (isTokenChosen) "Yes" else "No")
            token?.let { put(TOKEN_PARAM, token) }
            blockchain?.let { put(BLOCKCHAIN, blockchain) }
        },
    )
}