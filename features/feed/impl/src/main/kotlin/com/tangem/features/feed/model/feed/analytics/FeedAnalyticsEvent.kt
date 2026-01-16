package com.tangem.features.feed.model.feed.analytics

import com.tangem.core.analytics.models.AnalyticsEvent
import com.tangem.core.analytics.models.AnalyticsParam
import com.tangem.core.analytics.models.AnalyticsParam.Key.ERROR_CODE
import com.tangem.core.analytics.models.AnalyticsParam.Key.ERROR_MESSAGE
import com.tangem.core.analytics.models.AnalyticsParam.Key.SOURCE
import com.tangem.core.analytics.models.OneTimePerSessionEvent

internal sealed class FeedAnalyticsEvent(
    event: String,
    params: Map<String, String> = emptyMap(),
) : AnalyticsEvent(category = "Markets", event = event, params = params) {

    data class TokenListOpened(
        private val screensSources: AnalyticsParam.ScreensSources,
    ) : FeedAnalyticsEvent(
        event = "Token List Opened",
        params = mapOf(
            SOURCE to screensSources.value,
        ),
    )

    class NewsListOpened : FeedAnalyticsEvent(
        event = "News List Opened",
        params = mapOf(
            SOURCE to AnalyticsParam.ScreensSources.Markets.value,
        ),
    )

    data class NewsCarouselTrendingClicked(private val newsId: Int) : FeedAnalyticsEvent(
        event = "News Carousel Trending Clicked",
        params = mapOf(
            "News id" to newsId.toString(),
        ),
    ), OneTimePerSessionEvent {
        override val oneTimeEventId: String = event
    }

    class NewsCarouselAllNewsButton : FeedAnalyticsEvent(
        event = "News Carousel All News button",
    ), OneTimePerSessionEvent {
        override val oneTimeEventId: String = event
    }

    class NewsCarouselEndReached : FeedAnalyticsEvent(
        event = "News Carousel End Reached",
    ), OneTimePerSessionEvent {
        override val oneTimeEventId: String = event
    }

    class NewsCarouselScrolled : FeedAnalyticsEvent(
        event = "News Carousel Scrolled",
    ), OneTimePerSessionEvent {
        override val oneTimeEventId: String = event
    }

    data class MarketsLoadError(
        private val code: Int?,
        private val message: String,
    ) : FeedAnalyticsEvent(
        event = "Markets Load Error",
        params = mapOf(
            ERROR_CODE to (code ?: IS_NOT_HTTP_ERROR).toString(),
            ERROR_MESSAGE to message,
        ),
    )

    data class NewsLoadError(
        private val code: Int?,
        private val message: String,
    ) : FeedAnalyticsEvent(
        event = "News Load Error",
        params = mapOf(
            ERROR_CODE to (code ?: IS_NOT_HTTP_ERROR).toString(),
            ERROR_MESSAGE to message,
        ),
    )

    data class AllWidgetsLoadError(
        private val code: Int?,
        private val message: String,
    ) : FeedAnalyticsEvent(
        event = "All Widgets Load Error",
        params = mapOf(
            ERROR_CODE to (code ?: IS_NOT_HTTP_ERROR).toString(),
            ERROR_MESSAGE to message,
        ),
    )

    class TokenSearchedClicked : FeedAnalyticsEvent(event = "Token Searched Clicked")

    private companion object {
        const val IS_NOT_HTTP_ERROR = "Is not http error"
    }
}