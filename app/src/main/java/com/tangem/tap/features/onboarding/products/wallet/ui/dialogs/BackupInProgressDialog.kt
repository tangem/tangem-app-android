package com.tangem.tap.features.onboarding.products.wallet.ui.dialogs

import android.content.Context
import androidx.appcompat.app.AlertDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.tangem.tap.common.redux.global.GlobalAction
import com.tangem.tap.store
import com.tangem.wallet.R

object BackupInProgressDialog {
    fun create(context: Context): AlertDialog {
        return MaterialAlertDialogBuilder(context, R.style.CustomMaterialDialog).apply {
            setTitle(R.string.common_warning)
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
