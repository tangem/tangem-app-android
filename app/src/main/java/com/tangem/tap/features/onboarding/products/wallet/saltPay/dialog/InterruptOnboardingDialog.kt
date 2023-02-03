package com.tangem.tap.features.onboarding.products.wallet.saltPay.dialog

import android.app.Dialog
import android.content.Context
import androidx.appcompat.app.AlertDialog
import com.tangem.tap.common.extensions.dispatchDialogHide
import com.tangem.tap.features.onboarding.products.wallet.redux.OnboardingDialog
import com.tangem.tap.store
import com.tangem.wallet.R

/**
 * Created by Anton Zhilenkov on 24.10.2022.
 */
object InterruptOnboardingDialog {
    fun create(context: Context, dialog: OnboardingDialog.InterruptOnboarding): Dialog {
        return AlertDialog.Builder(context).apply {
            setTitle(context.getString(R.string.onboarding_exit_alert_title))
            setMessage(context.getString(R.string.onboarding_exit_alert_message))
            setPositiveButton(R.string.common_ok) { _, _ -> dialog.onOk() }
            setNegativeButton(R.string.common_cancel) { _, _ -> }
            setOnDismissListener { store.dispatchDialogHide() }
            setCancelable(false)
        }.create()
    }
}
