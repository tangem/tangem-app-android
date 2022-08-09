package com.tangem.tap.features.send.ui.dialogs

import android.content.Context
import androidx.appcompat.app.AlertDialog
import com.tangem.tangem_sdk_new.extensions.localizedDescription
import com.tangem.tap.common.feedback.SendTransactionFailedEmail
import com.tangem.tap.common.redux.global.GlobalAction
import com.tangem.tap.features.send.redux.SendAction
import com.tangem.tap.store
import com.tangem.wallet.R

/**
[REDACTED_AUTHOR]
 */
class SendTransactionFailsDialog {

    companion object {
        fun create(context: Context, dialog: SendAction.Dialog.SendTransactionFails.CardSdkError): AlertDialog {
            return create(context, dialog.error.localizedDescription(context))
        }

        fun create(context: Context, dialog: SendAction.Dialog.SendTransactionFails.BlockchainSdkError): AlertDialog {
            return create(context, dialog.error.customMessage)
        }

        private fun create(context: Context, errorMessage: String): AlertDialog {
            return AlertDialog.Builder(context).apply {
                setTitle(R.string.alert_failed_to_send_transaction_title)
                setMessage(context.getString(R.string.alert_failed_to_send_transaction_message, errorMessage))
                setNeutralButton(R.string.alert_button_send_feedback) { _, _ ->
                    store.dispatch(GlobalAction.SendEmail(SendTransactionFailedEmail(errorMessage)))
                }
                setPositiveButton(R.string.common_cancel) { _, _ -> }
                setOnDismissListener { store.dispatch(SendAction.Dialog.Hide) }
            }.create()
        }
    }
}