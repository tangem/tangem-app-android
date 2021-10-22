package com.tangem.tap.features.twins.ui.dialog

import android.content.Context
import androidx.appcompat.app.AlertDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.tangem.tap.common.extensions.dispatchDialogHide
import com.tangem.tap.features.twins.redux.TwinCardsAction
import com.tangem.tap.store
import com.tangem.wallet.R

/**
[REDACTED_AUTHOR]
 */
class CreateWalletInterruptDialog {
    companion object {
        fun create(state: TwinCardsAction.Wallet.InterruptDialog, context: Context): AlertDialog {
            return MaterialAlertDialogBuilder(context)
                    .setMessage(R.string.details_twins_recreate_alert)
                    .setPositiveButton(R.string.common_ok) { _, _ -> state.interrupt() }
                    .setNegativeButton(R.string.common_cancel) { _, _ -> }
                    .setOnDismissListener { store.dispatchDialogHide() }
                    .create()
        }
    }
}