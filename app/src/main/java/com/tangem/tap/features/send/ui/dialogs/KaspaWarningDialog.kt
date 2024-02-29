package com.tangem.tap.features.send.ui.dialogs

import android.content.Context
import androidx.appcompat.app.AlertDialog
import com.tangem.blockchain.common.Blockchain
import com.tangem.tap.features.send.redux.SendAction
import com.tangem.tap.store
import com.tangem.wallet.R

object KaspaWarningDialog {

    fun create(context: Context, dialog: SendAction.Dialog.KaspaWarningDialog): AlertDialog {
        return AlertDialog.Builder(context).apply {
            setTitle(R.string.common_warning)
            setMessage(
                context.getString(
                    R.string.common_utxo_validate_withdrawal_message_warning,
                    Blockchain.Kaspa.fullName,
                    dialog.maxOutputs,
                    dialog.maxAmount.toPlainString(),
                ),
            )
            setPositiveButton(R.string.common_ok) { _, _ ->
                dialog.onOk()
            }
            setOnDismissListener {
                store.dispatch(SendAction.Dialog.Hide)
            }
        }.create()
    }
}
