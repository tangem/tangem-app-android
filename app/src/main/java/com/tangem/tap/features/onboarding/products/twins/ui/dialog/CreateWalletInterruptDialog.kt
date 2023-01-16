package com.tangem.tap.features.onboarding.products.twins.ui.dialog

import android.content.Context
import androidx.appcompat.app.AlertDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.tangem.tap.common.extensions.dispatchDialogHide
import com.tangem.tap.store
import com.tangem.wallet.R

/**
 * Created by Anton Zhilenkov on 22/10/2021.
 */
object CreateWalletInterruptDialog {
    fun create(context: Context): AlertDialog {
        return MaterialAlertDialogBuilder(context)
            .setMessage(R.string.onboarding_twin_exit_warning)
            .setPositiveButton(R.string.warning_button_ok) { _, _ -> }
            .setOnDismissListener { store.dispatchDialogHide() }
            .create()
    }
}
