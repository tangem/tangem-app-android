package com.tangem.features.send.v2.subcomponents.notifications.analytics

import com.tangem.core.analytics.models.AnalyticsEvent
import com.tangem.core.analytics.models.AnalyticsParam.Key.BLOCKCHAIN
import com.tangem.core.analytics.models.AnalyticsParam.Key.TOKEN_PARAM

internal sealed class NotificationsAnalyticEvents(
    category: String,
    event: String,
    params: Map<String, String> = mapOf(),
) : AnalyticsEvent(category = category, event = event, params = params) {

    abstract val categoryName: String

    /** If not enough fee notification is present */
    data class NoticeNotEnoughFee(
        override val categoryName: String,
        val token: String,
        val blockchain: String,
    ) : NotificationsAnalyticEvents(
        category = categoryName,
        event = "Notice - Not Enough Fee",
        params = mapOf(TOKEN_PARAM to token, BLOCKCHAIN to blockchain),
    )

    data class NoticeFeeCoverage(
        override val categoryName: String,
    ) : NotificationsAnalyticEvents(
        category = categoryName,
        event = "Notice - Network Fee Coverage",
    )
}