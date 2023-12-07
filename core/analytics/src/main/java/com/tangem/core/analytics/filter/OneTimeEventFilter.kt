package com.tangem.core.analytics.filter

import com.tangem.core.analytics.api.AnalyticsEventFilter
import com.tangem.core.analytics.api.AnalyticsHandler
import com.tangem.core.analytics.models.AnalyticsEvent
import com.tangem.core.analytics.models.OneTimeAnalyticsEvent
import com.tangem.domain.analytics.repository.AnalyticsRepository

class OneTimeEventFilter(
    private val analyticsRepository: AnalyticsRepository,
) : AnalyticsEventFilter {

    override fun canBeAppliedTo(event: AnalyticsEvent): Boolean = event is OneTimeAnalyticsEvent

    override suspend fun canBeSent(event: AnalyticsEvent): Boolean {
        if (event !is OneTimeAnalyticsEvent) return true

        val isSent = analyticsRepository.checkIsEventSent(event.oneTimeEventId)

        if (!isSent) {
            analyticsRepository.setIsEventSent(event.oneTimeEventId)
        }

        return !isSent
    }

    override fun canBeConsumedByHandler(handler: AnalyticsHandler, event: AnalyticsEvent): Boolean {
        return canBeAppliedTo(event)
    }
}