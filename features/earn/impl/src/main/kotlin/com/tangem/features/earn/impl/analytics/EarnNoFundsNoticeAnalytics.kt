package com.tangem.features.earn.impl.analytics

import com.tangem.core.analytics.api.AnalyticsEventHandler

internal class EarnNoFundsNoticeAnalytics(private val analyticsEventHandler: AnalyticsEventHandler) {

    private var reported: Notice? = null

    fun onCta(notice: Notice?) {
        if (notice == reported) return
        reported = notice
        notice?.let {
            analyticsEventHandler.send(EarnPickerAnalyticsEvent.NoFundsNotice(token = it.token, product = it.product))
        }
    }

    data class Notice(val token: String, val product: String)
}