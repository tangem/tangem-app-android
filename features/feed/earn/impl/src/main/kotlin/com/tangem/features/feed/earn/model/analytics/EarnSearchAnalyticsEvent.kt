package com.tangem.features.feed.earn.model.analytics

import com.tangem.core.analytics.models.AnalyticsEvent
import com.tangem.core.analytics.models.AnalyticsParam.Key.ERROR_CODE
import com.tangem.core.analytics.models.AnalyticsParam.Key.ERROR_MESSAGE
import com.tangem.core.analytics.models.AnalyticsParam.Key.TOKEN_PARAM
import com.tangem.core.analytics.models.IS_NOT_HTTP_ERROR

/**
 * Events of the Earn search tab. Category `Search`, not `Earn`: these describe the search screen, and
 * they sit next to `CryptoSearchAnalyticsEvent` rather than next to [EarnAnalyticsEvent].
 *
 * `Search Screen Opened` and `Search Started` belong to the search host. Everything here needs data
 * only this tab has — a clicked row, the result counts, the catalogue request that failed.
 */
internal sealed class EarnSearchAnalyticsEvent(
    event: String,
    params: Map<String, String> = emptyMap(),
) : AnalyticsEvent(category = "Search", event = event, params = params) {

    data class PortfolioItemClicked(
        private val tokenSymbol: String,
    ) : EarnSearchAnalyticsEvent(
        event = "Portfolio Item Clicked",
        params = mapOf(TOKEN_PARAM to tokenSymbol),
    )

    data class EarnItemClicked(
        private val tokenSymbol: String,
    ) : EarnSearchAnalyticsEvent(
        event = "Earn Item Clicked",
        params = mapOf(TOKEN_PARAM to tokenSymbol),
    )

    data class ResultsShown(
        private val totalResultsCount: Int,
        private val earnResultsCount: Int,
        private val userTokensResultsCount: Int,
    ) : EarnSearchAnalyticsEvent(
        event = "Results Shown",
        params = mapOf(
            "Total Results" to totalResultsCount.toString(),
            "User Tokens Count" to userTokensResultsCount.toString(),
            "Earn Tokens Count" to earnResultsCount.toString(),
        ),
    )

    data class ErrorEarnData(
        private val code: Int?,
        private val message: String,
    ) : EarnSearchAnalyticsEvent(
        event = "Error - Earn Data",
        params = mapOf(
            ERROR_CODE to (code ?: IS_NOT_HTTP_ERROR).toString(),
            ERROR_MESSAGE to message,
        ),
    )
}