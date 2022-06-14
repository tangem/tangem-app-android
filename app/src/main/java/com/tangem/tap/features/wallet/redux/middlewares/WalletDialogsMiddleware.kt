package com.tangem.tap.features.wallet.redux.middlewares

import com.tangem.tap.common.extensions.dispatchDialogHide
import com.tangem.tap.common.extensions.dispatchDialogShow
import com.tangem.tap.common.redux.AppDialog
import com.tangem.tap.features.wallet.redux.WalletAction
import com.tangem.tap.features.wallet.redux.models.WalletDialog
import com.tangem.tap.store

class WalletDialogsMiddleware {
    fun handle(action: WalletAction.DialogAction) {
        when (action) {
            is WalletAction.DialogAction.SignedHashesMultiWalletDialog -> {
                store.dispatchDialogShow(WalletDialog.SignedHashesMultiWalletDialog)
            }
            is WalletAction.DialogAction.ChooseTradeActionDialog -> {
                store.dispatchDialogShow(WalletDialog.ChooseTradeActionDialog)
            }
            is WalletAction.DialogAction.QrCode -> {
                store.dispatchDialogShow(
                    AppDialog.AddressInfoDialog(
                        currency = action.currency,
                        addressData = action.selectedAddress,
                    )
                )
            }
            is WalletAction.DialogAction.ChooseCurrency -> {
                store.dispatchDialogShow(
                    WalletDialog.SelectAmountToSendDialog(
                        amounts = action.amounts
                    )
                )
            }
            is WalletAction.DialogAction.Hide -> {
                store.dispatchDialogHide()
            }
        }
    }
}
