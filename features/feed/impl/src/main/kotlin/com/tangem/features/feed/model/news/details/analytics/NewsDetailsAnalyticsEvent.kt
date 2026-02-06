package com.tangem.features.feed.model.news.details.analytics

import com.tangem.core.analytics.models.AnalyticsEvent
import com.tangem.core.analytics.models.AnalyticsParam
import com.tangem.core.analytics.models.AnalyticsParam.Key.ERROR_CODE
import com.tangem.core.analytics.models.AnalyticsParam.Key.ERROR_MESSAGE
import com.tangem.core.analytics.models.IS_NOT_HTTP_ERROR
import com.tangem.core.analytics.models.OneTimePerSessionEvent

internal sealed class NewsDetailsAnalyticsEvent(
    event: String,
    params: Map<String, String> = emptyMap(),
) : AnalyticsEvent(category = "Markets", event = event, params = params) {

    data class NewsArticleOpened(
        private val source: String,
        private val newsId: Int,
    ) : NewsDetailsAnalyticsEvent(
        event = "News Article Opened",
        params = mapOf(
            AnalyticsParam.SOURCE to source,
            "News Id" to newsId.toString(),
        ),
    )

    data class RelatedNewsClicked(
        private val newsId: Int,
        private val relatedNewsId: Int,
    ) : NewsDetailsAnalyticsEvent(
        event = "Related News Clicked",
        params = mapOf(
            "News Id" to newsId.toString(),
            "Related News Id" to relatedNewsId.toString(),
        ),
    )

    data class NewsLikeClicked(
        private val newsId: Int,
    ) : NewsDetailsAnalyticsEvent(
        event = "News Like Clicked",
        params = mapOf(
            "News Id" to newsId.toString(),
        ),
    ), OneTimePerSessionEvent {
        override val oneTimeEventId: String = event + newsId
    }

    data class NewsArticleLoadError(
        private val newsId: Int,
        private val code: Int?,
        private val message: String,
    ) : NewsDetailsAnalyticsEvent(
        event = "News Article Load Error",
        params = mapOf(
            "News Id" to newsId.toString(),
            ERROR_CODE to (code ?: IS_NOT_HTTP_ERROR).toString(),
            ERROR_MESSAGE to message,
        ),
    )

    data class NewsLinkMismatch(
        private val newsId: Int,
        private val code: Int?,
        private val message: String,
    ) : NewsDetailsAnalyticsEvent(
        event = "News Link Mismatch",
        params = mapOf(
            "News Id" to newsId.toString(),
            ERROR_CODE to (code ?: IS_NOT_HTTP_ERROR).toString(),
            ERROR_MESSAGE to message,
        ),
    )

    data class NewsShareButtonClick(
        private val newsId: Int,
    ) : NewsDetailsAnalyticsEvent(
        event = "News Share Button Clicked",
        params = mapOf(
            "News Id" to newsId.toString(),
        ),
    ), OneTimePerSessionEvent {
        override val oneTimeEventId: String = event
        override val throttleSeconds: Long = THROTTLE_SECONDS
    }

    private companion object {
        const val THROTTLE_SECONDS = 10L
    }
}