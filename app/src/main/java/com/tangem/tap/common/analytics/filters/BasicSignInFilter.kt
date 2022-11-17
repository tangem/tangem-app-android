package com.tangem.tap.common.analytics.filters

import com.tangem.tap.common.analytics.api.AnalyticsEventFilter
import com.tangem.tap.common.analytics.api.AnalyticsHandler
import com.tangem.tap.common.analytics.events.AnalyticsEvent
import com.tangem.tap.common.analytics.events.Basic

/**
 * Created by Anton Zhilenkov on 02.11.2022.
 */
class BasicSignInFilter : AnalyticsEventFilter {

    private val alreadySignedIn = mutableSetOf<String>()

    override fun canBeAppliedTo(event: AnalyticsEvent): Boolean = event is Basic.SignedIn

    override fun canBeSent(event: AnalyticsEvent): Boolean {
        val userWalletId = event.filterData as? String ?: return false

        val canBeSent = !alreadySignedIn.contains(userWalletId)
        alreadySignedIn.add(userWalletId)

        return canBeSent
    }

    override fun canBeConsumedByHandler(handler: AnalyticsHandler, event: AnalyticsEvent): Boolean = true
}
