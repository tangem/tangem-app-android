package com.tangem.features.feed.components.market.details.analytics

import com.tangem.core.analytics.models.AnalyticsEvent
import com.tangem.core.analytics.models.AnalyticsParam
import com.tangem.core.analytics.models.AnalyticsParam.Key.ERROR_CODE
import com.tangem.core.analytics.models.AnalyticsParam.Key.ERROR_MESSAGE
import com.tangem.core.analytics.models.IS_NOT_HTTP_ERROR
import com.tangem.core.analytics.models.OneTimePerSessionEvent

internal sealed class MarketTokenAnalyticsEvent(
    event: String,
    params: Map<String, String> = emptyMap(),
) : AnalyticsEvent(category = "CoinPage", event = event, params = params) {

    data class TokenNewsViewed(
        private val tokenSymbol: String,
    ) : MarketTokenAnalyticsEvent(
        event = "Token News Viewed",
        params = mapOf(
            AnalyticsParam.TOKEN_PARAM to tokenSymbol,
        ),
    ), OneTimePerSessionEvent {
        override val oneTimeEventId: String = event + tokenSymbol
    }

    data class TokenNewsCarouselScrolled(
        private val tokenSymbol: String,
    ) : MarketTokenAnalyticsEvent(
        event = "Token News Carousel Scrolled",
        params = mapOf(
            AnalyticsParam.TOKEN_PARAM to tokenSymbol,
        ),
    ), OneTimePerSessionEvent {
        override val oneTimeEventId: String = event + tokenSymbol
    }

    data class TokenNewsLoadError(
        private val tokenSymbol: String,
        private val code: Int?,
        private val message: String,
    ) : MarketTokenAnalyticsEvent(
        event = "Token News Load Error",
        params = mapOf(
            AnalyticsParam.TOKEN_PARAM to tokenSymbol,
            ERROR_CODE to (code ?: IS_NOT_HTTP_ERROR).toString(),
            ERROR_MESSAGE to message,
        ),
    )
}