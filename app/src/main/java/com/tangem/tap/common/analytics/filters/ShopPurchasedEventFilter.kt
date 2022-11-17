package com.tangem.tap.common.analytics.filters

import com.tangem.tap.common.analytics.api.AnalyticsEventFilter
import com.tangem.tap.common.analytics.api.AnalyticsHandler
import com.tangem.tap.common.analytics.events.AnalyticsEvent
import com.tangem.tap.common.analytics.events.Shop
import com.tangem.tap.common.analytics.handlers.appsFlyer.AppsFlyerAnalyticsHandler
import com.tangem.tap.common.analytics.handlers.firebase.FirebaseAnalyticsHandler

/**
 * Created by Anton Zhilenkov on 02.11.2022.
 */
class ShopPurchasedEventFilter : AnalyticsEventFilter {

    override fun canBeAppliedTo(event: AnalyticsEvent): Boolean = event is Shop.Purchased

    override fun canBeSent(event: AnalyticsEvent): Boolean = true

    override fun canBeConsumedByHandler(handler: AnalyticsHandler, event: AnalyticsEvent): Boolean {
        return when (handler) {
            is AppsFlyerAnalyticsHandler, is FirebaseAnalyticsHandler -> true
            else -> false
        }
    }
}
