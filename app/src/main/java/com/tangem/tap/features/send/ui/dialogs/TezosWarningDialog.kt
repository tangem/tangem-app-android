package com.tangem.tap.features.send.ui.dialogs

import android.content.Context
import androidx.appcompat.app.AlertDialog
import com.tangem.tap.features.send.redux.SendAction
import com.tangem.tap.store
import com.tangem.wallet.R

class TezosWarningDialog(context: Context) : AlertDialog(context) {

    companion object {
        fun create(context: Context, showDialogData: SendAction.Dialog.ShowTezosWarningDialog): AlertDialog {
            val reduceAmount = showDialogData.reduceAmount.toPlainString()
            return Builder(context).apply {
                setTitle(context.getString(R.string.common_warning))
                setMessage(context.getString(R.string.xtz_withdrawal_message_warning, reduceAmount))
                setNegativeButton(R.string.xtz_withdrawal_message_ignore) { _, _ ->
                    showDialogData.sendAllCallback()
                }
                setPositiveButton(context.getString(R.string.xtz_withdrawal_message_reduce, reduceAmount)) { _, _ ->
                    showDialogData.reduceCallback()
                }
                setOnDismissListener {
                    store.dispatch(SendAction.Dialog.Hide)
                }
            }.create()
        }
    }
}