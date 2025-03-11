package com.tangem.feature.wallet.presentation.wallet.analytics.utils

import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.analytics.models.AnalyticsEvent
import com.tangem.domain.wallets.models.UserWallet
import com.tangem.feature.wallet.presentation.wallet.analytics.WalletScreenAnalyticsEvent
import javax.inject.Inject

internal class SelectedWalletAnalyticsSender @Inject constructor(
    private val analyticsEventHandler: AnalyticsEventHandler,
) {

    fun send(userWallet: UserWallet) {
        val event = getEvent(userWallet)

        if (event != null) {
            analyticsEventHandler.send(event)
        }
    }

    /**

     * that cannot be processed in [WalletWarningsAnalyticsSender].
     * */
    private fun getEvent(userWallet: UserWallet): AnalyticsEvent? = when {
        userWallet.isLocked -> WalletScreenAnalyticsEvent.MainScreen.WalletUnlock
        else -> null
    }
}