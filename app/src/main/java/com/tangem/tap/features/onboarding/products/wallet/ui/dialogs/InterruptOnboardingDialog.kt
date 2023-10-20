package com.tangem.tap.features.onboarding.products.wallet.ui.dialogs

import android.app.Dialog
import android.content.Context
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.tangem.tap.common.extensions.dispatchDialogHide
import com.tangem.tap.features.onboarding.OnboardingDialog
import com.tangem.tap.store
import com.tangem.wallet.R

/**
[REDACTED_AUTHOR]
 */
object InterruptOnboardingDialog {
    fun create(context: Context, dialog: OnboardingDialog.InterruptOnboarding): Dialog {
        return MaterialAlertDialogBuilder(context, R.style.CustomMaterialDialog).apply {
            setTitle(context.getString(R.string.onboarding_exit_alert_title))
            setMessage(context.getString(R.string.onboarding_exit_alert_message))
            setPositiveButton(R.string.common_ok) { _, _ -> dialog.onOk() }
            setNegativeButton(R.string.common_cancel) { _, _ -> }
            setOnDismissListener { store.dispatchDialogHide() }
            setCancelable(false)
        }.create()
    }
}