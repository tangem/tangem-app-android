package com.tangem.features.earn.impl.analytics

import com.tangem.core.analytics.models.AnalyticsEvent

internal const val EARN_ANALYTICS_CATEGORY = "Earn"

internal sealed class EarnAnalyticsEvent(
    category: String,
    event: String,
    params: Map<String, String> = emptyMap(),
) : AnalyticsEvent(category = category, event = event, params = params)

internal object EarnAnalyticsParam {
    const val PRODUCT = "Product"
    const val POSITION = "Position"
    const val PRODUCTS = "Products"
    const val CURRENT_APY = "Current APY"
    const val BEST_APY = "Best APY"
    const val TAB = "Tab"
    const val HORIZON = "Horizon"
    const val INPUT_TYPE = "Input Type"
    const val ROW = "Row"
    const val ROW_STATE = "Row State"
    const val ACTIVE_COUNT = "Active Count"
    const val INACTIVE_COUNT = "Inactive Count"
    const val POSITION_BUCKET = "Position Bucket"
    const val EARNED_BUCKET = "Earned Bucket"
    const val PERIOD = "Period"
    const val ACTION = "Action"
}