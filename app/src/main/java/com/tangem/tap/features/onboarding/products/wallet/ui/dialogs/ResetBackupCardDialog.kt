package com.tangem.tap.features.onboarding.products.wallet.ui.dialogs

import android.content.Context
import androidx.appcompat.app.AlertDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.tangem.core.analytics.Analytics
import com.tangem.tap.common.analytics.events.Onboarding
import com.tangem.tap.common.redux.global.GlobalAction
import com.tangem.tap.features.onboarding.products.wallet.redux.BackupAction
import com.tangem.tap.store
import com.tangem.wallet.R

object ResetBackupCardDialog {
    fun create(context: Context, cardId: String): AlertDialog {
        return MaterialAlertDialogBuilder(context, R.style.CustomMaterialDialog).apply {
            setTitle(R.string.common_attention)
            setMessage(R.string.onboarding_linking_error_card_with_wallets)
            setPositiveButton(R.string.common_cancel) { _, _ ->
                Analytics.send(Onboarding.Backup.ResetCancelEvent)
            }
            setNegativeButton(R.string.card_settings_action_sheet_reset) { _, _ ->
                Analytics.send(Onboarding.Backup.ResetPerformEvent)
                store.dispatch(BackupAction.ResetBackupCard(cardId))
            }
            setOnDismissListener {
                store.dispatch(GlobalAction.HideDialog)
            }
        }.create()
    }
}