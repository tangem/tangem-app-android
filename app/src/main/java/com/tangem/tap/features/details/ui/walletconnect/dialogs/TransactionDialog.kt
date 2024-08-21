package com.tangem.tap.features.details.ui.walletconnect.dialogs

import android.content.Context
import androidx.appcompat.app.AlertDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.tangem.tap.common.redux.global.GlobalAction
import com.tangem.tap.domain.walletconnect2.domain.WcPreparedRequest
import com.tangem.tap.features.details.redux.walletconnect.WalletConnectAction
import com.tangem.tap.features.details.redux.walletconnect.WcEthTransactionType
import com.tangem.tap.store
import com.tangem.wallet.R

object TransactionDialog {
    fun create(preparedData: WcPreparedRequest.EthTransaction, context: Context): AlertDialog {
        val data = preparedData.preparedRequestData.dialogData
        val message = context.getString(
            R.string.wallet_connect_create_tx_message,
            data.dAppName,
            data.dAppUrl,
            data.amount,
            data.feeAmount,
            data.totalAmount,
            data.balance,
        )

        val positiveButtonTitle = when (data.type) {
            WcEthTransactionType.EthSignTransaction -> context.getText(R.string.common_sign)
            WcEthTransactionType.EthSendTransaction -> context.getText(R.string.common_sign_and_send)
        }
        return MaterialAlertDialogBuilder(context, R.style.CustomMaterialDialog).apply {
            setTitle(context.getString(R.string.wallet_connect_title))
            setMessage(message)
            setPositiveButton(positiveButtonTitle) { _, _ ->
                if (data.isEnoughFundsToSend) {
                    store.dispatch(WalletConnectAction.PerformRequestedAction(preparedData))
                } else {
                    store.dispatch(WalletConnectAction.RejectRequest(data.topic, data.id))
                }
            }
            setNegativeButton(context.getText(R.string.common_reject)) { _, _ ->
                store.dispatch(WalletConnectAction.RejectRequest(data.topic, data.id))
            }
            setOnDismissListener {
                store.dispatch(GlobalAction.HideDialog)
            }
        }.create()
    }
}

data class TransactionRequestDialogData(
    val dAppName: String,
    val dAppUrl: String,
    val amount: String,
    val feeAmount: String,
    val totalAmount: String,
    val balance: String,
    val isEnoughFundsToSend: Boolean,
    val topic: String,
    val id: Long,
    val type: WcEthTransactionType,
)