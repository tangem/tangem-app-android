package com.tangem.tap.features.onboarding.products.wallet.ui.dialogs

import android.content.Context
import androidx.appcompat.app.AlertDialog
import com.tangem.tap.common.redux.global.GlobalAction
import com.tangem.tap.store
import com.tangem.wallet.R

class BackupInProgressDialog {
    companion object {
        fun create(context: Context): AlertDialog {
            return AlertDialog.Builder(context).apply {
                setTitle(R.string.alert_title)
                setMessage(R.string.onboarding_backup_exit_warning)
                setPositiveButton(R.string.warning_button_ok) { _, _ ->
                    store.dispatch(GlobalAction.HideDialog)
                }
                setOnDismissListener {
                    store.dispatch(GlobalAction.HideDialog)
                }
            }.create()
        }
    }
}