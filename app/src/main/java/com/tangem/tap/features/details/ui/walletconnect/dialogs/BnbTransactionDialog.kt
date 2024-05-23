package com.tangem.tap.features.details.ui.walletconnect.dialogs

import android.content.Context
import androidx.appcompat.app.AlertDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.tangem.tap.common.redux.global.GlobalAction
import com.tangem.tap.domain.walletconnect2.domain.WcPreparedRequest
import com.tangem.tap.features.details.redux.walletconnect.BinanceMessageData
import com.tangem.tap.features.details.redux.walletconnect.WalletConnectAction
import com.tangem.tap.store
import com.tangem.wallet.R

object BnbTransactionDialog {
    fun create(preparedData: WcPreparedRequest.BnbTransaction, context: Context): AlertDialog {
        val data = preparedData.preparedRequestData.data
        val message = when (data) {
            is BinanceMessageData.Trade -> data.tradeData.map {
                context.getString(
                    R.string.wallet_connect_bnb_trade_order_message,
                    it.symbol,
                    it.price,
                    it.quantity,
                    it.amount,
                )
            }.joinToString(separator = "\n\n")
            is BinanceMessageData.Transfer -> context.getString(
                R.string.wallet_connect_bnb_transaction_message,
                data.address,
                data.outputAddress,
                data.amount,
            )
        }

        val fullMessage = context.getString(
            R.string.wallet_connect_bnb_sign_message,
            preparedData.preparedRequestData.dAppName,
            message,
        )
        val positiveButtonTitle = context.getText(R.string.common_sign)

        return MaterialAlertDialogBuilder(context, R.style.CustomMaterialDialog).apply {
            setTitle(context.getString(R.string.wallet_connect_title))
            setMessage(fullMessage)
            setPositiveButton(positiveButtonTitle) { _, _ ->
                store.dispatch(WalletConnectAction.PerformRequestedAction(preparedData))
            }
            setNegativeButton(context.getText(R.string.common_reject)) { _, _ ->
                store.dispatch(WalletConnectAction.RejectRequest(preparedData.topic, preparedData.requestId))
            }
            setOnDismissListener {
                store.dispatch(GlobalAction.HideDialog)
            }
        }.create()
    }
}