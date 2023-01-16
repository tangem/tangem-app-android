package com.tangem.tap.features.details.ui.walletconnect.dialogs

import android.content.Context
import androidx.appcompat.app.AlertDialog
import com.tangem.tap.common.redux.global.GlobalAction
import com.tangem.tap.features.details.redux.walletconnect.WalletConnectAction
import com.tangem.tap.store
import com.tangem.wallet.R
import com.trustwallet.walletconnect.models.session.WCSession

object PersonalSignDialog {
    fun create(data: PersonalSignDialogData, context: Context): AlertDialog {
        val message = context.getString(R.string.wallet_connect_alert_sign_message, data.message)
        return AlertDialog.Builder(context).apply {
            setTitle(context.getString(R.string.wallet_connect_title))
            setMessage(message)
            setPositiveButton(context.getText(R.string.common_sign)) { _, _ ->
                store.dispatch(WalletConnectAction.SignMessage(data.session))
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

data class PersonalSignDialogData(
    val dAppName: String,
    val message: String,
    val session: WCSession,
    val id: Long,
)
