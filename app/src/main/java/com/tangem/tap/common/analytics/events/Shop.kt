package com.tangem.tap.common.analytics.events

import com.tangem.core.analytics.AnalyticsEvent
import com.tangem.tap.common.extensions.filterNotNull

/**
 * Created by Anton Zhilenkov on 28.09.2022.
 */
sealed class Shop(
    event: String,
    params: Map<String, String> = mapOf(),
) : AnalyticsEvent("Shop", event, params) {

    class ScreenOpened : Shop("Shop Screen Opened")

    class Purchased(sku: String, count: String, amount: String, couponCode: String?) : Shop(
        event = "Purchased",
        params = mapOf(
            "SKU" to sku,
            "Count" to count,
            "Amount" to amount,
            "Coupon Code" to couponCode,
        ).filterNotNull(),
    )

    class Redirected(partnerName: String?) : Shop(
        event = "Redirected",
        params = partnerName?.let { mapOf("Partner" to partnerName) } ?: mapOf(),
    )
}
