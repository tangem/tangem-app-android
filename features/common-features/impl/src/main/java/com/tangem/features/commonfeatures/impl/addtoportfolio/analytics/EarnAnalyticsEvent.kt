package com.tangem.features.commonfeatures.impl.addtoportfolio.analytics

import com.tangem.core.analytics.models.AnalyticsEvent
import com.tangem.core.analytics.models.AnalyticsParam

// todo swap unify with PortfolioAnalyticsEvent, AddToPortfolioFlow
internal sealed class EarnAnalyticsEvent(
    event: String,
    params: Map<String, String> = emptyMap(),
) : AnalyticsEvent(category = "Earn", event = event, params = params) {

    data class AddTokenScreenOpened(
        private val tokenSymbol: String,
        private val blockchain: String,
        private val source: String,
    ) : EarnAnalyticsEvent(
        event = "Add Token Screen Opened",
        params = mapOf(
            AnalyticsParam.TOKEN_PARAM to tokenSymbol,
            AnalyticsParam.BLOCKCHAIN to blockchain,
            AnalyticsParam.SOURCE to source,
        ),
    )

    data class TokenAdded(
        private val tokenSymbol: String,
        private val blockchain: String,
    ) : EarnAnalyticsEvent(
        event = "Token Added",
        params = mapOf(
            AnalyticsParam.TOKEN_PARAM to tokenSymbol,
            AnalyticsParam.BLOCKCHAIN to blockchain,
        ),
    )
}