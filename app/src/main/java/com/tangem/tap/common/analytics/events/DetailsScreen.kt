package com.tangem.tap.common.analytics.events

import com.tangem.core.analytics.models.AnalyticsEvent

/**
 * Created by Anton Zhilenkov on 28.09.2022.
 */
sealed class DetailsScreen(
    event: String,
    params: Map<String, String> = mapOf(),
) : AnalyticsEvent("Details Screen", event, params) {

    class ScreenOpened : DetailsScreen("Details Screen Opened")
}
