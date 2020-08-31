package com.tangem.tap.features.wallet.redux

import android.content.Context
import com.tangem.blockchain.common.Wallet
import com.tangem.blockchain.common.WalletManager
import com.tangem.tap.common.redux.NotificationAction
import com.tangem.wallet.R
import org.rekotlin.Action

sealed class WalletAction : Action {
    data class LoadWallet(val walletManager: WalletManager) : WalletAction() {
        data class Success(val wallet: Wallet): WalletAction()
        object Failure: WalletAction()
    }
    data class LoadPayId(val address: String) : WalletAction() {
        object Success: WalletAction()
        object Failure: WalletAction()
    }
    object Scan : WalletAction()
    object Send : WalletAction()
    object CreatePayId : WalletAction() {
        object Success: WalletAction()
        object Failure: WalletAction()
    }
    data class CopyAddress(val context: Context) : WalletAction() {
        object Success : WalletAction(), NotificationAction {
            override val messageResource = R.string.notification_address_copied
        }
    }
    object ShowQrCode : WalletAction()
    object HideQrCode : WalletAction()
    data class ExploreAddress(val context: Context) : WalletAction()
    object CreateWallet : WalletAction()
}