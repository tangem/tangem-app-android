package com.tangem.tap.features.onboarding.products.wallet.ui.dialogs

import android.app.Dialog
import android.content.Context
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.tangem.core.analytics.Analytics
import com.tangem.core.analytics.models.AnalyticsParam
import com.tangem.core.analytics.models.Basic
import com.tangem.tap.common.extensions.dispatchDialogHide
import com.tangem.tap.common.feedback.SupportInfo
import com.tangem.tap.common.redux.global.GlobalAction
import com.tangem.tap.features.onboarding.OnboardingDialog
import com.tangem.tap.store
import com.tangem.wallet.R

object WalletActivationErrorDialog {

    fun create(context: Context, dialog: OnboardingDialog.WalletActivationError): Dialog {
        return MaterialAlertDialogBuilder(context, R.style.CustomMaterialDialog).apply {
            setTitle(context.getString(R.string.onboarding_activation_error_title))
            setMessage(context.getString(R.string.onboarding_activation_error_message))
            setPositiveButton(R.string.common_ok) { _, _ -> dialog.onConfirm() }
            setNegativeButton(R.string.common_support) { _, _ ->
                // changed on email support [REDACTED_TASK_KEY]
                Analytics.send(Basic.ButtonSupport(AnalyticsParam.ScreensSources.Intro))
                store.dispatch(
                    GlobalAction.SendEmail(
                        feedbackData = SupportInfo(),
                        scanResponse = store.state.globalState.onboardingState.onboardingManager?.scanResponse
                            ?: error("ScanResponse must be not null"),
                    ),
                )
            }
            setOnDismissListener { store.dispatchDialogHide() }
            setCancelable(false)
        }.create()
    }
}