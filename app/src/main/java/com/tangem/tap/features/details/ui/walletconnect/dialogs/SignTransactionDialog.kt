package com.tangem.tap.features.details.ui.walletconnect.dialogs

import android.content.Context
import androidx.appcompat.app.AlertDialog
import com.tangem.tap.common.redux.global.GlobalAction
import com.tangem.tap.domain.walletconnect.WalletConnectSdkHelper.Companion.MAX_HASH_TO_SIGN_SIZE
import com.tangem.tap.domain.walletconnect2.domain.WcPreparedRequest
import com.tangem.tap.features.details.redux.walletconnect.WalletConnectAction
import com.tangem.tap.store
import com.tangem.wallet.R

internal object SignTransactionDialog {

    fun create(preparedData: WcPreparedRequest.SolanaSignTransaction, context: Context): AlertDialog {
        val hashSize = preparedData.preparedRequestData.hashToSign.size
        val signMessage = if (hashSize > MAX_HASH_TO_SIGN_SIZE) {
            context.getString(R.string.wallet_connect_alert_sign_message_large_hash)
        } else {
            context.getString(R.string.wallet_connect_alert_sign_message, "")
        }
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