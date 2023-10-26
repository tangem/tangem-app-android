package com.tangem.tap.features.onboarding.products.twins.ui.dialog

import android.content.Context
import androidx.appcompat.app.AlertDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.tangem.tap.common.extensions.dispatchDialogHide
import com.tangem.tap.store
import com.tangem.wallet.R

/**
[REDACTED_AUTHOR]
 */
object TwinningProcessNotCompletedDialog {
    fun create(context: Context): AlertDialog {
        return MaterialAlertDialogBuilder(context, R.style.CustomMaterialDialog)
            .setMessage(R.string.onboarding_twin_exit_warning)
            .setPositiveButton(R.string.warning_button_ok) { _, _ -> }
            .setOnDismissListener { store.dispatchDialogHide() }
            .create()
    }
}