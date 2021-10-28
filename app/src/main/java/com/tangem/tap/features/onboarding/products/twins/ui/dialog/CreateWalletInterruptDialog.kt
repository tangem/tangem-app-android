package com.tangem.tap.features.onboarding.products.twins.ui.dialog

import android.content.Context
import androidx.appcompat.app.AlertDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.tangem.tap.common.extensions.dispatchDialogHide
import com.tangem.tap.features.onboarding.products.twins.redux.TwinCardsAction
import com.tangem.tap.store
import com.tangem.wallet.R

/**
[REDACTED_AUTHOR]
 */
class CreateWalletInterruptDialog {
    companion object {
        fun create(state: TwinCardsAction.Wallet.ShowInterruptDialog, context: Context): AlertDialog {
            return MaterialAlertDialogBuilder(context)
                    .setMessage(R.string.twins_recreate_alert)
                    .setPositiveButton(R.string.common_ok) { _, _ -> state.onOk() }
                    .setNegativeButton(R.string.common_cancel) { _, _ -> }
                    .setOnDismissListener { store.dispatchDialogHide() }
                    .create()
        }
    }
}