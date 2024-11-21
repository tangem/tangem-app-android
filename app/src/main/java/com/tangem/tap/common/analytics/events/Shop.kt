package com.tangem.tap.common.analytics.events

import com.tangem.core.analytics.models.AnalyticsEvent
import com.tangem.core.analytics.models.EventValue
import com.tangem.tap.common.extensions.filterNotNull

/**
[REDACTED_AUTHOR]
 */
sealed class Shop(
    event: String,
    params: Map<String, EventValue> = mapOf(),
) : AnalyticsEvent("Shop", event, params) {

    class ScreenOpened : Shop("Shop Screen Opened")

    class Purchased(sku: String, count: String, amount: String, couponCode: String?) : Shop(
        event = "Purchased",
        params = mapOf(
            "SKU" to sku.asStringValue(),
            "Count" to count.asStringValue(),
            "Amount" to amount.asStringValue(),
            "Coupon Code" to couponCode?.asStringValue(),
        ).filterNotNull(),
    )

    class Redirected(partnerName: String?) : Shop(
        event = "Redirected",
        params = partnerName?.let { mapOf("Partner" to partnerName.asStringValue()) } ?: mapOf(),
    )
}