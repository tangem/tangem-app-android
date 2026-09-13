package com.tangem.features.feed.crypto.model.analytics

import com.tangem.core.analytics.models.AnalyticsEvent
import com.tangem.core.analytics.models.AnalyticsParam.Key.ERROR_CODE
import com.tangem.core.analytics.models.AnalyticsParam.Key.ERROR_MESSAGE
import com.tangem.core.analytics.models.AnalyticsParam.Key.TOKEN_PARAM
import com.tangem.core.analytics.models.IS_NOT_HTTP_ERROR

/**
 * Events of the Crypto search tab.
 *
 * The strings duplicate `SearchAnalyticsEvent` in `:features:feed:impl` on purpose: they are a wire
 * protocol shared with the legacy search screen, which still ships behind
 * `TWI_1608_NEW_SHTORKA_ENABLED` and cannot be depended on from here. The legacy copy dies with the
 * toggle.
 *
 * `Search Screen Opened` and `Search Started` belong to the search host, which owns the screen and its
 * search bar. Everything here needs data only this tab has — a clicked row, the result counts, the
 * markets request that failed.
 */
internal sealed class CryptoSearchAnalyticsEvent(
    event: String,
    params: Map<String, String> = emptyMap(),
) : AnalyticsEvent(category = "Search", event = event, params = params) {

    data class PortfolioItemClicked(
        private val tokenSymbol: String,
    ) : CryptoSearchAnalyticsEvent(
        event = "Portfolio Item Clicked",
        params = mapOf(TOKEN_PARAM to tokenSymbol),
    )

    data class MarketItemClicked(
        private val tokenSymbol: String,
    ) : CryptoSearchAnalyticsEvent(
        event = "Market Item Clicked",
        params = mapOf(TOKEN_PARAM to tokenSymbol),
    )

    data class ResultsShown(
        private val totalResultsCount: Int,
        private val marketsResultsCount: Int,
        private val userTokensResultsCount: Int,
    ) : CryptoSearchAnalyticsEvent(
        event = "Results Shown",
        params = mapOf(
            "Total Results" to totalResultsCount.toString(),
            "User Tokens Count" to userTokensResultsCount.toString(),
            "Market Tokens Count" to marketsResultsCount.toString(),
        ),
    )

    data class ErrorMarketsData(
        private val code: Int?,
        private val message: String,
    ) : CryptoSearchAnalyticsEvent(
        event = "Error - Markets Data",
        params = mapOf(
            ERROR_CODE to (code ?: IS_NOT_HTTP_ERROR).toString(),
            ERROR_MESSAGE to message,
        ),
    )
}