package com.tangem.tap.features.onboarding.products.wallet.ui.dialogs

import android.content.Context
import androidx.appcompat.app.AlertDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.tangem.tap.common.redux.global.GlobalAction
import com.tangem.tap.features.onboarding.products.wallet.redux.BackupAction
import com.tangem.tap.store
import com.tangem.wallet.R

object AddMoreBackupCardsDialog {
    fun create(context: Context): AlertDialog {
        return MaterialAlertDialogBuilder(context, R.style.CustomMaterialDialog).apply {
            setTitle(R.string.common_warning)
            setMessage(R.string.onboarding_alert_message_not_max_backup_cards_added)
            setPositiveButton(R.string.common_continue) { _, _ ->
                store.dispatch(BackupAction.ShowAccessCodeInfoScreen)
            }
            setNegativeButton(R.string.common_cancel) { _, _ ->
            }
            setOnDismissListener {
                store.dispatch(GlobalAction.HideDialog)
            }
        }.create()
    }
}
