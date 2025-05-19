package com.tangem.core.analytics

import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.analytics.models.AnalyticsEvent

class DummyAnalyticsEventHandler : AnalyticsEventHandler {

    override fun send(event: AnalyticsEvent) {
        /* no-op */
    }
}