package com.tangem.feature.wallet.presentation.wallet.analytics.utils

import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.analytics.models.AnalyticsEvent
import com.tangem.domain.wallets.models.UserWallet
import com.tangem.domain.wallets.models.isLocked
import com.tangem.feature.wallet.presentation.wallet.analytics.WalletScreenAnalyticsEvent
import com.tangem.feature.wallet.presentation.wallet.utils.ScreenLifecycleProvider
import javax.inject.Inject

internal class SelectedWalletAnalyticsSender @Inject constructor(
    private val analyticsEventHandler: AnalyticsEventHandler,
    private val screenLifecycleProvider: ScreenLifecycleProvider,
) {

    fun send(userWallet: UserWallet) {
        if (screenLifecycleProvider.isBackgroundState.value) return

        val event = getEvent(userWallet)

        if (event != null) {
            analyticsEventHandler.send(event)
        }
    }

    /**
     * This is here because the state of a locked wallet is immediately created with a notification
     * that cannot be processed in [WalletWarningsAnalyticsSender].
     * */
    private fun getEvent(userWallet: UserWallet): AnalyticsEvent? = when {
        userWallet.isLocked -> WalletScreenAnalyticsEvent.MainScreen.WalletUnlock
        else -> null
    }
}
