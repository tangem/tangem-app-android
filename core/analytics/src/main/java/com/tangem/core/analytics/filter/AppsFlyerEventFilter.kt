package com.tangem.core.analytics.filter

import com.tangem.core.analytics.api.AnalyticsEventFilter
import com.tangem.core.analytics.api.AnalyticsHandler
import com.tangem.core.analytics.models.AnalyticsEvent
import com.tangem.core.analytics.models.AppsFlyerEvent

class AppsFlyerEventFilter : AnalyticsEventFilter {

    override fun canBeAppliedTo(event: AnalyticsEvent): Boolean = event is AppsFlyerEvent

    override suspend fun canBeSent(event: AnalyticsEvent): Boolean = true

    override fun canBeConsumedByHandler(
        handler: AnalyticsHandler,
        event: AnalyticsEvent,
    ): Boolean = handler.id() == "AppsFlyer"
}