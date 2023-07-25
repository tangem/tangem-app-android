package com.tangem.tap.features.demo

import com.tangem.domain.demo.DemoConfig
import com.tangem.domain.models.scan.ScanResponse
import com.tangem.tap.common.extensions.dispatchNotification
import com.tangem.tap.common.redux.AppState
import com.tangem.tap.features.details.redux.DetailsAction
import com.tangem.tap.features.details.redux.walletconnect.WalletConnectAction
import com.tangem.tap.features.onboarding.products.wallet.redux.BackupAction
import com.tangem.tap.features.wallet.redux.WalletAction
import com.tangem.tap.store
import com.tangem.wallet.R
import org.rekotlin.Action

object DemoHelper {
    val config = DemoConfig()

    private val demoMiddlewares = listOf(
        DemoOnboardingNoteMiddleware(),
    )

    private val disabledActionFeatures = listOf(
        WalletConnectAction.StartWalletConnect::class.java,
        WalletAction.TradeCryptoAction.Buy::class.java,
        WalletAction.TradeCryptoAction.Sell::class.java,
        BackupAction.StartBackup::class.java,
        WalletAction.ExploreAddress::class.java,
        DetailsAction.ResetToFactory.Start::class.java,
    )

    fun isDemoCard(scanResponse: ScanResponse): Boolean = isDemoCardId(scanResponse.card.cardId)

    fun isTestDemoCard(scanResponse: ScanResponse): Boolean = config.isTestDemoCardId(scanResponse.card.cardId)

    fun isDemoCardId(cardId: String): Boolean = config.isDemoCardId(cardId)

    fun tryHandle(appState: () -> AppState?, action: Action): Boolean {
        val scanResponse = getScanResponse(appState) ?: return false
        if (!scanResponse.isDemoCard()) return false

        demoMiddlewares.forEach {
            if (it.tryHandle(config, scanResponse, action)) return true
        }

        disabledActionFeatures.firstOrNull { it == action::class.java }?.let {
            store.dispatchNotification(R.string.alert_demo_feature_disabled)
            return true
        }

        return false
    }

    private fun getScanResponse(appState: () -> AppState?): ScanResponse? {
        val state = appState() ?: return null

        return state.globalState.onboardingState.onboardingManager?.scanResponse
            ?: state.globalState.scanResponse
    }
}
