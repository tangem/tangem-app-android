package com.tangem.tap.features.onboarding.products.wallet.ui.dialogs

import android.content.Context
import androidx.appcompat.app.AlertDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.tangem.core.analytics.Analytics
import com.tangem.domain.models.scan.ScanResponse
import com.tangem.features.onboarding.v2.multiwallet.impl.analytics.OnboardingEvent
import com.tangem.tap.common.redux.global.GlobalAction
import com.tangem.tap.features.onboarding.products.wallet.redux.BackupAction
import com.tangem.tap.features.onboarding.products.wallet.redux.BackupDialog
import com.tangem.tap.store
import com.tangem.wallet.R

object UnfinishedBackupFoundDialog {
    fun create(context: Context, scanResponse: ScanResponse? = null): AlertDialog {
        return MaterialAlertDialogBuilder(context, R.style.CustomMaterialDialog).apply {
            setTitle(R.string.common_warning)
            setMessage(R.string.welcome_interrupted_backup_alert_message)
            setPositiveButton(R.string.welcome_interrupted_backup_alert_resume) { _, _ ->
                Analytics.send(OnboardingEvent.Backup.ResumeInterruptedBackup)
                store.dispatch(GlobalAction.HideDialog)
                store.dispatch(BackupAction.ResumeFoundUnfinishedBackup(scanResponse))
            }
            setNegativeButton(R.string.welcome_interrupted_backup_alert_discard) { _, _ ->
                Analytics.send(OnboardingEvent.Backup.CancelInterruptedBackup)
                store.dispatch(GlobalAction.HideDialog)
                store.dispatch(GlobalAction.ShowDialog(BackupDialog.ConfirmDiscardingBackup(scanResponse)))
            }
            setCancelable(false)
        }.create()
    }
}