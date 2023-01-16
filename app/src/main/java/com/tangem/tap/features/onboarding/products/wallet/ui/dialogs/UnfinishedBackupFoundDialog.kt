package com.tangem.tap.features.onboarding.products.wallet.ui.dialogs

import android.content.Context
import androidx.appcompat.app.AlertDialog
import com.tangem.tap.common.redux.global.GlobalAction
import com.tangem.tap.features.onboarding.products.wallet.redux.BackupAction
import com.tangem.tap.features.onboarding.products.wallet.redux.BackupDialog
import com.tangem.tap.store
import com.tangem.wallet.R

object UnfinishedBackupFoundDialog {
    fun create(context: Context): AlertDialog {
        return AlertDialog.Builder(context).apply {
            setTitle(R.string.common_warning)
            setMessage(R.string.welcome_interrupted_backup_alert_message)
            setPositiveButton(R.string.welcome_interrupted_backup_alert_resume) { _, _ ->
                store.dispatch(GlobalAction.HideDialog)
                store.dispatch(BackupAction.ResumeFoundUnfinishedBackup)
            }
            setNegativeButton(R.string.welcome_interrupted_backup_alert_discard) { _, _ ->
                store.dispatch(GlobalAction.HideDialog)
                store.dispatch(GlobalAction.ShowDialog(BackupDialog.ConfirmDiscardingBackup))
            }
            setCancelable(false)
        }.create()
    }
}
