package com.tangem.tap.features.wallet.redux

import android.content.Context
import com.tangem.blockchain.common.Wallet
import com.tangem.tap.common.redux.ErrorAction
import com.tangem.tap.common.redux.NotificationAction
import com.tangem.tap.domain.TapError
import com.tangem.wallet.R
import org.rekotlin.Action
import java.math.BigDecimal

sealed class WalletAction : Action {
    object LoadWallet : WalletAction() {
        data class Success(val wallet: Wallet): WalletAction()
        object Failure: WalletAction()
    }
    object LoadFiatRate : WalletAction() {
        data class Success(val fiatRates: Pair<String, BigDecimal>) : WalletAction()
        object Failure: WalletAction()
    }
    object LoadPayId : WalletAction() {
        data class Success(val payId: String): WalletAction()
        object NotCreated: WalletAction()
        object Failure: WalletAction()
    }
    object Scan : WalletAction()
    object Send : WalletAction()
    object CreatePayId : WalletAction() {
        data class CompleteCreatingPayId(val payId: String): WalletAction()
        data class Success(val payId: String): WalletAction()
        object EmptyField: WalletAction(), ErrorAction {
            override val error = TapError.PayIdEmptyField
        }
        class Failure(override val error: TapError) : WalletAction(), ErrorAction
        object Cancel: WalletAction()
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