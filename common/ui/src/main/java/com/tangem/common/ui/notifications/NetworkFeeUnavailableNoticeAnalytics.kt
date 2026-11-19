package com.tangem.common.ui.notifications

import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.analytics.api.ResettableOneTimeEventSender
import com.tangem.core.analytics.models.event.NetworkFeeUnavailableAnalyticsEvent

/**
 * Sends [NetworkFeeUnavailableAnalyticsEvent] once per appearance of the fee-unreachable banner: notification lists
 * are rebuilt on every input change, so the event fires again only after the banner has gone and come back.
 */
class NetworkFeeUnavailableNoticeAnalytics(
    analyticsEventHandler: AnalyticsEventHandler,
    private val source: NetworkFeeUnavailableAnalyticsEvent.Source,
) {

    private val sender = ResettableOneTimeEventSender(analyticsEventHandler)

    fun onNotificationsUpdated(notifications: List<NotificationUM>) {
        onBannerVisibilityChanged(isShown = notifications.any { it is NotificationUM.Warning.NetworkFeeUnreachable })
    }

    fun onBannerVisibilityChanged(isShown: Boolean) {
        if (isShown) {
            sender.sendEventOnce(key = EVENT_KEY, event = NetworkFeeUnavailableAnalyticsEvent(source))
        } else {
            sender.reset(EVENT_KEY)
        }
    }

    private companion object {
        const val EVENT_KEY = "network_fee_unavailable"
    }
}