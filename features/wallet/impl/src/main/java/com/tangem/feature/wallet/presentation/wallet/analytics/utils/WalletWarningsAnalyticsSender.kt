package com.tangem.feature.wallet.presentation.wallet.analytics.utils

import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.analytics.models.AnalyticsEvent
import com.tangem.feature.wallet.presentation.wallet.analytics.WalletScreenAnalyticsEvent.MainScreen
import com.tangem.feature.wallet.presentation.wallet.state.components.WalletNotification
import com.tangem.feature.wallet.presentation.wallet.state2.model.WalletState
import dagger.hilt.android.scopes.ViewModelScoped
import javax.inject.Inject

@ViewModelScoped
internal class WalletWarningsAnalyticsSender @Inject constructor(
    private val analyticsEventHandler: AnalyticsEventHandler,
) {

    fun send(displayedUiState: WalletState?, newWarnings: List<WalletNotification>) {
        if (newWarnings.isEmpty()) return
        if (displayedUiState == null || displayedUiState.pullToRefreshConfig.isRefreshing) return

        val eventsFromNewWarnings = getEvents(newWarnings)
        val eventsFromDisplayedWarnings = getEvents(displayedUiState.warnings)
        val eventsToSend = eventsFromNewWarnings.filter { it !in eventsFromDisplayedWarnings }

        eventsToSend.forEach { event ->
            analyticsEventHandler.send(event)
        }
    }

    private fun getEvents(warnings: List<WalletNotification>): Set<AnalyticsEvent> {
        return warnings.mapNotNullTo(mutableSetOf(), ::getEvent)
    }

    private fun getEvent(warning: WalletNotification): AnalyticsEvent? {
        return when (warning) {
            is WalletNotification.Critical.DevCard -> MainScreen.DevelopmentCard
            is WalletNotification.Critical.FailedCardValidation -> MainScreen.ProductSampleCard
            is WalletNotification.Warning.MissingBackup -> MainScreen.BackupYourWallet
            is WalletNotification.Warning.NumberOfSignedHashesIncorrect -> MainScreen.CardSignedTransactions
            is WalletNotification.Warning.TestNetCard -> MainScreen.TestnetCard
            is WalletNotification.Informational.DemoCard -> MainScreen.DemoCard
            is WalletNotification.Informational.MissingAddresses -> MainScreen.MissingAddresses
            is WalletNotification.RateApp -> MainScreen.HowDoYouLikeTangem
            is WalletNotification.UnlockWallets -> MainScreen.WalletUnlock
            is WalletNotification.Informational.NoAccount,
            is WalletNotification.Warning.LowSignatures,
            is WalletNotification.Warning.SomeNetworksUnreachable,
            is WalletNotification.Warning.NetworksUnreachable,
            is WalletNotification.SwapPromo,
            -> null
        }
    }
}