package com.tangem.tap.common.ui

import android.content.Context
import androidx.appcompat.app.AlertDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.tangem.core.analytics.Analytics
import com.tangem.core.analytics.models.Basic
import com.tangem.tap.common.extensions.dispatchDialogHide
import com.tangem.tap.common.feedback.ScanFailsEmail
import com.tangem.tap.common.redux.global.GlobalAction
import com.tangem.tap.store
import com.tangem.wallet.R

/**
* [REDACTED_AUTHOR]
 */
object ScanFailsDialog {
    fun create(context: Context): AlertDialog {
        return MaterialAlertDialogBuilder(context, R.style.CustomMaterialDialog).apply {
            setTitle(context.getString(R.string.common_warning))
            setMessage(R.string.alert_troubleshooting_scan_card_title)
            setPositiveButton(R.string.alert_button_request_support) { _, _ ->
                Analytics.send(Basic.ButtonSupport())
                store.dispatch(GlobalAction.SendEmail(ScanFailsEmail()))
            }
            setNeutralButton(R.string.common_cancel) { _, _ -> }
            setOnDismissListener { store.dispatchDialogHide() }
        }.create()
    }
}
