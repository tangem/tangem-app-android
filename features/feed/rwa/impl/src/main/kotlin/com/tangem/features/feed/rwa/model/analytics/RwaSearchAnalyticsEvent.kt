package com.tangem.features.feed.rwa.model.analytics

import com.tangem.core.analytics.models.AnalyticsEvent
import com.tangem.core.analytics.models.AnalyticsParam.Key.ERROR_CODE
import com.tangem.core.analytics.models.AnalyticsParam.Key.ERROR_MESSAGE
import com.tangem.core.analytics.models.AnalyticsParam.Key.TOKEN_PARAM
import com.tangem.core.analytics.models.IS_NOT_HTTP_ERROR

/**
 * Events of the Real assets search tab. Category `Search`, not `Rwa`: these describe the search screen,
 * and they belong next to `CryptoSearchAnalyticsEvent` rather than to the feed tab.
 *
 * `Search Screen Opened` and `Search Started` belong to the search host, which owns the screen and its
 * search bar. Everything here needs data only this tab has — a clicked row, the result counts, the
 * request that failed.
 */
internal sealed class RwaSearchAnalyticsEvent(
    event: String,
    params: Map<String, String> = emptyMap(),
) : AnalyticsEvent(category = "Search", event = event, params = params) {

    data class PortfolioItemClicked(
        private val tokenSymbol: String,
    ) : RwaSearchAnalyticsEvent(
        event = "Portfolio Item Clicked",
        params = mapOf(TOKEN_PARAM to tokenSymbol),
    )

    data class RealAssetItemClicked(
        private val tokenSymbol: String,
    ) : RwaSearchAnalyticsEvent(
        event = "Real Assets Item Clicked",
        params = mapOf(TOKEN_PARAM to tokenSymbol),
    )

    data class ResultsShown(
        private val totalResultsCount: Int,
        private val realAssetsResultsCount: Int,
        private val userTokensResultsCount: Int,
    ) : RwaSearchAnalyticsEvent(
        event = "Results Shown",
        params = mapOf(
            "Total Results" to totalResultsCount.toString(),
            "User Tokens Count" to userTokensResultsCount.toString(),
            "Real Assets Count" to realAssetsResultsCount.toString(),
        ),
    )

    data class ErrorRealAssetsData(
        private val code: Int?,
        private val message: String,
    ) : RwaSearchAnalyticsEvent(
        event = "Error - Real Assets Data",
        params = mapOf(
            ERROR_CODE to (code ?: IS_NOT_HTTP_ERROR).toString(),
            ERROR_MESSAGE to message,
        ),
    )
}