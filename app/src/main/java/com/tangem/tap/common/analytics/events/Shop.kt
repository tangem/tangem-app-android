package com.tangem.tap.common.analytics.events

import com.tangem.core.analytics.AnalyticsEvent
import com.tangem.tap.common.extensions.filterNotNull

/**
[REDACTED_AUTHOR]
 */
sealed class Shop(
    event: String,
    params: Map<String, String> = mapOf(),
) : AnalyticsEvent("Shop", event, params) {

    class ScreenOpened : IntroductionProcess("Shop Screen Opened")

    class Purchased(sku: String, count: String, amount: String, couponCode: String?) : Shop(
        event = "Purchased",
        params = mapOf(
            "SKU" to sku,
            "Count" to count,
            "Amount" to amount,
            "Coupon Code" to couponCode,
        ).filterNotNull(),
    )

    class Redirected(partnerName: String) : Shop(
        event = "Redirected",
        params = mapOf("Partner" to partnerName),
    )
}