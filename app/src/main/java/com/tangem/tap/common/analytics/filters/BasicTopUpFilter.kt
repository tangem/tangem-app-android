package com.tangem.tap.common.analytics.filters

import com.tangem.common.extensions.guard
import com.tangem.core.analytics.api.AnalyticsEventFilter
import com.tangem.core.analytics.api.AnalyticsHandler
import com.tangem.core.analytics.AnalyticsEvent
import com.tangem.tap.common.analytics.events.AnalyticsParam
import com.tangem.tap.common.analytics.events.Basic
import com.tangem.tap.persistence.ToppedUpWalletStorage

/**
* [REDACTED_AUTHOR]
 */
class BasicTopUpFilter(
    private val topupWalletStorage: ToppedUpWalletStorage,
) : AnalyticsEventFilter {

    override fun canBeAppliedTo(event: AnalyticsEvent): Boolean = event is Basic.ToppedUp

    override fun canBeSent(event: AnalyticsEvent): Boolean {
        val data = event.filterData as? Data ?: return false

        val walletInfo = topupWalletStorage.restore(data.walletId).guard {
            val newWalletInfo = Data(
                walletId = data.walletId,
                cardBalanceState = data.cardBalanceState,
            )
            topupWalletStorage.save(newWalletInfo)
            return false
        }

        if (walletInfo.isToppedUp) return false

        return if (!walletInfo.isToppedUp && data.isToppedUp) {
            topupWalletStorage.save(walletInfo.copy(cardBalanceState = AnalyticsParam.CardBalanceState.Full))
            true
        } else {
            false
        }
    }

    override fun canBeConsumedByHandler(handler: AnalyticsHandler, event: AnalyticsEvent): Boolean = true

    data class Data(
        val walletId: String,
        val cardBalanceState: AnalyticsParam.CardBalanceState,
    ) {
        val isToppedUp: Boolean = cardBalanceState == AnalyticsParam.CardBalanceState.Full
    }
}
