package com.tangem.tap.common.analytics.filters

import com.tangem.common.extensions.guard
import com.tangem.core.analytics.AnalyticsEvent
import com.tangem.core.analytics.api.AnalyticsEventFilter
import com.tangem.core.analytics.api.AnalyticsHandler
import com.tangem.tap.common.analytics.events.AnalyticsParam
import com.tangem.tap.common.analytics.events.Basic
import com.tangem.tap.common.analytics.topup.log
import com.tangem.tap.common.extensions.dispatchDebugErrorNotification
import com.tangem.tap.persistence.ToppedUpWalletStorage
import com.tangem.tap.store

/**
[REDACTED_AUTHOR]
 */
class TopUpFilter(
    private val topupWalletStorage: ToppedUpWalletStorage,
) : AnalyticsEventFilter {

    override fun canBeAppliedTo(event: AnalyticsEvent): Boolean = event is Basic.ToppedUp

    override fun canBeSent(event: AnalyticsEvent): Boolean {
        val data = event.filterData as? Data ?: return false

        if (data.isToppedUpInPast) {
            log("BasicTopUpFilter: [false]: sending is blocked")
            val newWalletInfo = ToppedUpWalletStorage.Companion.TopupInfo(
                walletId = data.walletId,
                cardBalanceState = AnalyticsParam.CardBalanceState.Full,
            )
            topupWalletStorage.save(newWalletInfo)
            return false
        }

        val topupInfo = topupWalletStorage.restore(data.walletId).guard {
            log("BasicTopUpFilter: [false]: sending is blocked")
            val topupInfo = ToppedUpWalletStorage.Companion.TopupInfo(
                walletId = data.walletId,
                cardBalanceState = data.cardBalanceState,
            )
            topupWalletStorage.save(topupInfo)
            return false
        }

        if (topupInfo.isToppedUp) {
            log("BasicTopUpFilter: [false]: sending is blocked")
            return false
        }

        return if (!topupInfo.isToppedUp && data.isToppedUp) {
            log("BasicTopUpFilter: [TRUE]: !topupInfo.isToppedUp && data.isToppedUp")
            log("BasicTopUpFilter: [TRUE]: SEND")
            store.dispatchDebugErrorNotification("Topped UP sent")
            topupWalletStorage.save(topupInfo.copy(cardBalanceState = AnalyticsParam.CardBalanceState.Full))
            true
        } else {
            log("BasicTopUpFilter: [false]: sending is blocked")
            false
        }
    }

    override fun canBeConsumedByHandler(handler: AnalyticsHandler, event: AnalyticsEvent): Boolean = true

    data class Data(
        val walletId: String,
        val cardBalanceState: AnalyticsParam.CardBalanceState,
        val isToppedUpInPast: Boolean,
    ) {
        val isToppedUp: Boolean = cardBalanceState == AnalyticsParam.CardBalanceState.Full
    }
}