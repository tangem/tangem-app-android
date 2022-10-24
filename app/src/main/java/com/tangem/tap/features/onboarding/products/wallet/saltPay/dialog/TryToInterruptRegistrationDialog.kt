package com.tangem.tap.features.onboarding.products.wallet.saltPay.dialog

import android.app.Dialog
import android.content.Context
import androidx.appcompat.app.AlertDialog
import com.tangem.tap.common.extensions.dispatchDialogHide
import com.tangem.tap.store
import com.tangem.wallet.R

/**
* [REDACTED_AUTHOR]
 */
class TryToInterruptRegistrationDialog {
    companion object {
        fun create(context: Context, dialog: SaltPayDialog.Activation.TryToInterrupt): Dialog {
            return AlertDialog.Builder(context).apply {
                setTitle(context.getString(R.string.onboarding_exit_alert_title))
                setMessage(context.getString(R.string.onboarding_exit_alert_message))
                setPositiveButton(R.string.common_ok) { _, _ -> dialog.onOk() }
                setNegativeButton(R.string.common_cancel) { _, _ -> dialog.onCancel() }
                setOnDismissListener { store.dispatchDialogHide() }
                setCancelable(false)
            }.create()
        }
    }
}
