package com.tangem.tap.features.wallet.ui.dialogs

import android.content.Context
import androidx.appcompat.app.AlertDialog
import com.tangem.tap.features.wallet.redux.WalletAction
import com.tangem.tap.features.wallet.redux.WarningType
import com.tangem.tap.store
import com.tangem.wallet.R

class WarningDialog(context: Context) : AlertDialog(context) {

    private val dialog: AlertDialog = Builder(context)
            .setTitle(context.getString(R.string.common_warning))
            .setPositiveButton(context.getString(R.string.common_ok)) { _, _ ->
                dismiss()
            }.setOnDismissListener {
                store.dispatch(WalletAction.SaveCardId)
                store.dispatch(WalletAction.HideDialog)
            }
            .create()

    fun show(warningType: WarningType) {
        val messageRes = when (warningType) {
            WarningType.CardSignedHashesBefore -> R.string.alert_card_signed_transactions
            WarningType.DevCard -> R.string.alert_developer_card
        }
        dialog.setMessage(dialog.context.getString(messageRes))
        dialog.show()
    }
}