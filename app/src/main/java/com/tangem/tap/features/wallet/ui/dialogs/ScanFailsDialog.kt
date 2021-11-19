package com.tangem.tap.features.wallet.ui.dialogs

import android.content.Context
import androidx.appcompat.app.AlertDialog
import com.tangem.tap.common.extensions.dispatchDialogHide
import com.tangem.tap.common.redux.global.GlobalAction
import com.tangem.tap.features.feedback.ScanFailsEmail
import com.tangem.tap.store
import com.tangem.wallet.R

/**
[REDACTED_AUTHOR]
 */
class ScanFailsDialog {

    companion object {
        fun create(context: Context): AlertDialog {
            return AlertDialog.Builder(context).apply {
                setTitle(context.getString(R.string.common_warning))
                setMessage(R.string.alert_troubleshooting_scan_card_title)
                setPositiveButton(R.string.alert_button_request_support) { _, _ ->
                    store.dispatch(GlobalAction.SendFeedback(ScanFailsEmail()))
                }
                setNeutralButton(R.string.alert_troubleshooting_scan_card_ok) { _, _ -> }
                setOnDismissListener { store.dispatchDialogHide() }
            }.create()
        }
    }

}