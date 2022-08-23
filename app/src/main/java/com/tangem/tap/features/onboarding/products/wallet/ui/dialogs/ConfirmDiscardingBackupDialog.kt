package com.tangem.tap.features.onboarding.products.wallet.ui.dialogs

import android.content.Context
import androidx.appcompat.app.AlertDialog
import com.tangem.tap.common.redux.global.GlobalAction
import com.tangem.tap.features.onboarding.products.wallet.redux.BackupAction
import com.tangem.tap.store
import com.tangem.wallet.R

class ConfirmDiscardingBackupDialog {
    companion object {
        fun create(context: Context): AlertDialog {
            return AlertDialog.Builder(context).apply {
                setTitle(R.string.welcome_interrupted_backup_discard_title)
                setMessage(R.string.welcome_interrupted_backup_discard_message)
                setPositiveButton(R.string.welcome_interrupted_backup_discard_resume) { _, _ ->
                    store.dispatch(BackupAction.ResumeBackup)
                }
                setNegativeButton(R.string.welcome_interrupted_backup_discard_discard) { _, _ ->
                    store.dispatch(BackupAction.DiscardSavedBackup)
                }
                setOnDismissListener {
                    store.dispatch(GlobalAction.HideDialog)
                }
                setCancelable(false)
            }.create()
        }
    }
}