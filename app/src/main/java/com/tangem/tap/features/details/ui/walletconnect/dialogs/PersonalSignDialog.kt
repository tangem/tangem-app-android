package com.tangem.tap.features.details.ui.walletconnect.dialogs

import android.content.Context
import androidx.appcompat.app.AlertDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.tangem.tap.common.redux.global.GlobalAction
import com.tangem.tap.domain.walletconnect2.domain.WcPreparedRequest
import com.tangem.tap.features.details.redux.walletconnect.WalletConnectAction
import com.tangem.tap.store
import com.tangem.wallet.R

object PersonalSignDialog {
    fun create(preparedData: WcPreparedRequest.EthSign, context: Context): AlertDialog {
        val data = preparedData.preparedRequestData.dialogData
        val message = context.getString(R.string.wallet_connect_alert_sign_message, data.message)
        return MaterialAlertDialogBuilder(context, R.style.CustomMaterialDialog).apply {
            setTitle(context.getString(R.string.wallet_connect_title))
            setMessage(message)
            setPositiveButton(context.getText(R.string.common_sign)) { _, _ ->
                store.dispatch(WalletConnectAction.PerformRequestedAction(preparedData))
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

data class PersonalSignDialogData(
    val dAppName: String,
    val message: String,
    val topic: String,
    val id: Long,
)