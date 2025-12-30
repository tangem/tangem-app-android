package com.tangem.core.analytics.filter

import com.tangem.core.analytics.api.AnalyticsEventFilter
import com.tangem.core.analytics.api.AnalyticsHandler
import com.tangem.core.analytics.models.AnalyticsEvent
import com.tangem.core.analytics.models.AppsFlyerIncludedEvent
import com.tangem.core.analytics.models.AppsFlyerOnlyEvent

class AppsFlyerEventFilter : AnalyticsEventFilter {

    override fun canBeAppliedTo(event: AnalyticsEvent): Boolean =
        event is AppsFlyerOnlyEvent || event is AppsFlyerIncludedEvent

    override suspend fun canBeSent(event: AnalyticsEvent): Boolean = true

    override fun canBeConsumedByHandler(handler: AnalyticsHandler, event: AnalyticsEvent): Boolean {
        return when (event) {
            is AppsFlyerOnlyEvent -> handler.id() == "AppsFlyer"
            is AppsFlyerIncludedEvent -> true
            else -> false
        }
    }
}