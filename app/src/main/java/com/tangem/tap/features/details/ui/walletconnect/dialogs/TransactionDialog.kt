package com.tangem.tap.features.details.ui.walletconnect.dialogs

import android.content.Context
import androidx.appcompat.app.AlertDialog
import com.tangem.tap.common.redux.global.GlobalAction
import com.tangem.tap.features.details.redux.walletconnect.WalletConnectAction
import com.tangem.tap.features.details.redux.walletconnect.WcTransactionType
import com.tangem.tap.store
import com.tangem.wallet.R
import com.trustwallet.walletconnect.models.session.WCSession

class TransactionDialog {
    companion object {
        fun create(
            data: TransactionRequestDialogData,
            context: Context,
        ): AlertDialog {
            val message = context.getString(
                R.string.wallet_connect_create_tx_message,
                data.dAppName,
                data.dAppUrl,
                data.amount,
                data.gasAmount,
                data.totalAmount,
                data.balance,
            )

            val positiveButtonTitle = when (data.type) {
                WcTransactionType.EthSignTransaction -> context.getText(R.string.common_sign)
                WcTransactionType.EthSendTransaction -> context.getText(R.string.common_sign_and_send)
            }
            return AlertDialog.Builder(context).apply {
                setTitle(context.getString(R.string.wallet_connect_title))
                setMessage(message)
                setPositiveButton(positiveButtonTitle) { _, _ ->
                    if (data.isEnoughFundsToSend) {
                        store.dispatch(WalletConnectAction.SendTransaction(data.session))
                    } else {
                        store.dispatch(WalletConnectAction.RejectRequest(data.session, data.id))
                        store.dispatch(WalletConnectAction.NotEnoughFunds)
                    }
                }
                setNegativeButton(context.getText(R.string.common_reject)) { _, _ ->
                    store.dispatch(WalletConnectAction.RejectRequest(data.session, data.id))
                }
                setOnDismissListener {
                    store.dispatch(GlobalAction.HideDialog)
                }
            }.create()
        }
    }
}

data class TransactionRequestDialogData(
    val dAppName: String,
    val dAppUrl: String,
    val amount: String,
    val gasAmount: String,
    val totalAmount: String,
    val balance: String,
    val isEnoughFundsToSend: Boolean,
    val session: WCSession,
    val id: Long,
    val type: WcTransactionType,
)
