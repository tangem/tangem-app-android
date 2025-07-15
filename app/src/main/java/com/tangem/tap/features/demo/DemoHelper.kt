package com.tangem.tap.features.demo

import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.message.DialogMessage
import com.tangem.domain.demo.DemoConfig
import com.tangem.domain.models.scan.ScanResponse
import com.tangem.tap.common.extensions.inject
import com.tangem.tap.common.redux.AppState
import com.tangem.tap.features.details.redux.walletconnect.WalletConnectAction
import com.tangem.tap.proxy.redux.DaggerGraphState
import com.tangem.tap.store
import com.tangem.wallet.R
import org.rekotlin.Action

object DemoHelper {
    val config = DemoConfig()

    private val disabledActionFeatures = listOf(
        WalletConnectAction.StartWalletConnect::class.java,
    )

    fun isDemoCard(scanResponse: ScanResponse): Boolean = isDemoCardId(scanResponse.card.cardId)

    fun isTestDemoCard(scanResponse: ScanResponse): Boolean = config.isTestDemoCardId(scanResponse.card.cardId)

    fun isDemoCardId(cardId: String): Boolean = config.isDemoCardId(cardId)

    fun tryHandle(appState: () -> AppState?, action: Action): Boolean {
        val scanResponse = getScanResponse(appState) ?: return false
        if (!scanResponse.isDemoCard()) return false

        disabledActionFeatures.firstOrNull { it == action::class.java }?.let {
            val uiMessageSender = store.inject(DaggerGraphState::uiMessageSender)
            uiMessageSender.send(
                DialogMessage(
                    message = resourceReference(R.string.alert_demo_feature_disabled),
                ),
            )
            return true
        }

        return false
    }

    private fun getScanResponse(appState: () -> AppState?): ScanResponse? {
        return appState()?.globalState?.scanResponse
    }
}