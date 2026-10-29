package com.tangem.core.analytics.filter

import com.tangem.core.analytics.api.AnalyticsEventFilter
import com.tangem.core.analytics.api.OneTimeEventStore
import com.tangem.core.analytics.api.AnalyticsHandler
import com.tangem.core.analytics.models.AnalyticsEvent
import com.tangem.core.analytics.models.OneTimeAnalyticsEvent
import javax.inject.Inject

internal class OneTimeEventFilter @Inject constructor(
    private val oneTimeEventStore: OneTimeEventStore,
) : AnalyticsEventFilter {

    override fun canBeAppliedTo(event: AnalyticsEvent): Boolean = event is OneTimeAnalyticsEvent

    override suspend fun canBeSent(event: AnalyticsEvent): Boolean {
        if (event !is OneTimeAnalyticsEvent) return true

        return !oneTimeEventStore.markSent(eventId = event.oneTimeEventId)
    }

    override fun canBeConsumedByHandler(handler: AnalyticsHandler, event: AnalyticsEvent): Boolean {
        return canBeAppliedTo(event)
    }
}