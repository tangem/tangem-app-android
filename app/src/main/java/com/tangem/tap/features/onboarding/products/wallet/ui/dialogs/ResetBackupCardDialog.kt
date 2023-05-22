package com.tangem.tap.features.onboarding.products.wallet.ui.dialogs

import android.content.Context
import androidx.appcompat.app.AlertDialog
import com.tangem.tap.common.redux.global.GlobalAction
import com.tangem.tap.features.onboarding.products.wallet.redux.BackupAction
import com.tangem.tap.store
import com.tangem.wallet.R

object ResetBackupCardDialog {
    fun create(context: Context): AlertDialog {
        return AlertDialog.Builder(context).apply {
            setTitle(R.string.common_warning)
            setMessage(R.string.onboarding_linking_error_card_with_wallets)
            setPositiveButton(R.string.common_continue) { _, _ ->
                store.dispatch(BackupAction.ResetBackupCard)
            }
            setNegativeButton(R.string.common_cancel) { _, _ ->
            }
            setOnDismissListener {
                store.dispatch(GlobalAction.HideDialog)
            }
        }.create()
    }
}