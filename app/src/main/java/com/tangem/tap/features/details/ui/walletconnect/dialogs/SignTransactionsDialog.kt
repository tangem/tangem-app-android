package com.tangem.tap.features.details.ui.walletconnect.dialogs

import android.content.Context
import androidx.appcompat.app.AlertDialog
import com.tangem.tap.common.redux.global.GlobalAction
import com.tangem.tap.domain.walletconnect2.domain.WcPreparedRequest
import com.tangem.tap.features.details.redux.walletconnect.WalletConnectAction
import com.tangem.tap.store
import com.tangem.wallet.R

internal object SignTransactionsDialog {

    fun create(preparedData: WcPreparedRequest.SolanaSignMultipleTransactions, context: Context): AlertDialog {
        val signMessage = context.getString(R.string.wallet_connect_alert_sign_message, "")
        val message = "${preparedData.preparedRequestData.dAppName}\n$signMessage"
        return AlertDialog.Builder(context).apply {
            setTitle(context.getString(R.string.wallet_connect_title))
            setMessage(message)
            setPositiveButton(context.getText(R.string.common_sign)) { _, _ ->
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