package com.tangem.tap.common.analytics.api

import com.tangem.tap.common.analytics.events.AnalyticsEvent

/**
 * Created by Anton Zhilenkov on 02.11.2022.
 * The filter is used to separate events to send and determine which handler it can be processed with.
 */
interface AnalyticsEventFilter {
    /**
     * Recognizes the event and checks if the given filter can be applied to the event.
     */
    fun canBeAppliedTo(event: AnalyticsEvent): Boolean

    /**
     * An internal filter check that, on external or internal conditions, recognizes the possibility of
     * sending an event.
     */
    fun canBeSent(event: AnalyticsEvent): Boolean

    /**
     * Performs a check to see if the event can be dispatched by a specific handler
     */
    fun canBeConsumedByHandler(handler: AnalyticsHandler, event: AnalyticsEvent): Boolean
}

interface AnalyticsFilterHolder {
    fun addFilter(filter: AnalyticsEventFilter)
    fun removeFilter(filter: AnalyticsEventFilter): Boolean
}
