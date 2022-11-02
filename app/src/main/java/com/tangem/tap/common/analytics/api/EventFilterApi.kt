package com.tangem.tap.common.analytics.api

import com.tangem.tap.common.analytics.events.AnalyticsEvent

/**
[REDACTED_AUTHOR]
 */
interface AnalyticsEventFilter {
    fun canBeAppliedTo(event: AnalyticsEvent): Boolean
    fun canBeSent(event: AnalyticsEvent): Boolean
    fun canBeConsumedBy(handler: AnalyticsEventHandler, event: AnalyticsEvent): Boolean
}