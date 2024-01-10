package com.tangem.feature.wallet.presentation.wallet.analytics.utils

import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.feature.wallet.presentation.wallet.analytics.WalletScreenAnalyticsEvent.MainScreen
import com.tangem.feature.wallet.presentation.wallet.state.components.WalletNotification
import dagger.hilt.android.scopes.ViewModelScoped
import kotlinx.collections.immutable.ImmutableList
import javax.inject.Inject

@ViewModelScoped
internal class WalletWarningsAnalyticsSender @Inject constructor(
    private val analyticsEventHandler: AnalyticsEventHandler,
) {

    fun send(warnings: ImmutableList<WalletNotification>) {
        val analyticsEvents = warnings.mapNotNull { warning ->
            when (warning) {
                is WalletNotification.Critical.DevCard -> MainScreen.DevelopmentCard
                is WalletNotification.Informational.DemoCard -> MainScreen.DemoCard
                is WalletNotification.RateApp -> MainScreen.HowDoYouLikeTangem
                is WalletNotification.Warning.NumberOfSignedHashesIncorrect -> MainScreen.CardSignedTransactions
                is WalletNotification.Warning.TestNetCard -> MainScreen.TestnetCard
                is WalletNotification.UnlockWallets -> MainScreen.WalletUnlock
                is WalletNotification.Critical.FailedCardValidation -> MainScreen.ProductSampleCard
                is WalletNotification.Warning.MissingBackup -> MainScreen.BackupYourWallet
                is WalletNotification.Informational.MissingAddresses,
                is WalletNotification.Informational.NoAccount,
                is WalletNotification.Warning.LowSignatures,
                is WalletNotification.Warning.NetworksUnreachable,
                is WalletNotification.Warning.SomeNetworksUnreachable,
                is WalletNotification.SwapPromo,
                -> null
            }
        }

        analyticsEvents.forEach { event ->
            analyticsEventHandler.send(event)
        }
    }
}