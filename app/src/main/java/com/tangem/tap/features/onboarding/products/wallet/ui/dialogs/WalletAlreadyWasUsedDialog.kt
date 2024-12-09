package com.tangem.tap.features.onboarding.products.wallet.ui.dialogs

import android.content.Context
import androidx.appcompat.app.AlertDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.tangem.tap.common.redux.global.GlobalAction
import com.tangem.tap.store
import com.tangem.wallet.R

object WalletAlreadyWasUsedDialog {

    fun create(context: Context, onOk: () -> Unit, onCancel: () -> Unit, onSupport: () -> Unit): AlertDialog {
        return MaterialAlertDialogBuilder(context, R.style.CustomMaterialDialog).apply {
            setTitle(R.string.security_alert_title)
            setMessage(R.string.wallet_been_activated_message)
            setPositiveButton(R.string.this_is_my_wallet_title) { dialog, _ ->
                onOk()
                dialog.dismiss()
            }
            setNeutralButton(R.string.common_cancel) { dialog, _ ->
                onCancel()
                dialog.dismiss()
            }
            setNegativeButton(R.string.alert_button_request_support) { dialog, _ ->
                onSupport()
                dialog.dismiss()
            }
            setOnDismissListener {
                store.dispatch(GlobalAction.HideDialog)
            }
        }.create()
    }
}