package com.tangem.features.feed.model.news.list.analytics

import com.tangem.core.analytics.models.AnalyticsEvent
import com.tangem.core.analytics.models.AnalyticsParam.Key.ERROR_CODE
import com.tangem.core.analytics.models.AnalyticsParam.Key.ERROR_MESSAGE

internal sealed class NewsListAnalyticsEvent(
    event: String,
    params: Map<String, String> = emptyMap(),
) : AnalyticsEvent(category = "Markets", event = event, params = params) {

    data class NewsListLoadError(
        private val code: Int?,
        private val message: String,
    ) : NewsListAnalyticsEvent(
        event = "News List Load Error",
        params = mapOf(
            ERROR_CODE to (code ?: IS_NOT_HTTP_ERROR).toString(),
            ERROR_MESSAGE to message,
        ),
    )

    data class NewsCategoriesClick(
        private val categoryId: Int,
    ) : NewsListAnalyticsEvent(
        event = "News Categories Selected",
        params = mapOf(
            "Selected Categories" to categoryId.toString(),
        ),
    )

    private companion object {
        const val IS_NOT_HTTP_ERROR = "Is not http error"
    }
}