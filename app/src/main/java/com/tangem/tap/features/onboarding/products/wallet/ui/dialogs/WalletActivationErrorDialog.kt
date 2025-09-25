package com.tangem.tap.features.onboarding.products.wallet.ui.dialogs

import android.app.Dialog
import android.content.Context
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.tangem.core.analytics.Analytics
import com.tangem.core.analytics.models.AnalyticsParam
import com.tangem.core.analytics.models.Basic
import com.tangem.domain.feedback.models.FeedbackEmailType
import com.tangem.tap.common.extensions.dispatchDialogHide
import com.tangem.tap.common.extensions.inject
import com.tangem.tap.features.onboarding.OnboardingDialog
import com.tangem.tap.proxy.redux.DaggerGraphState
import com.tangem.tap.scope
import com.tangem.tap.store
import com.tangem.wallet.R
import kotlinx.coroutines.launch

object WalletActivationErrorDialog {

    fun create(context: Context, dialog: OnboardingDialog.WalletActivationError): Dialog {
        return MaterialAlertDialogBuilder(context, R.style.CustomMaterialDialog).apply {
            setTitle(context.getString(R.string.onboarding_activation_error_title))
            setMessage(context.getString(R.string.onboarding_activation_error_message))
            setPositiveButton(R.string.common_ok) { _, _ -> dialog.onConfirm() }
            setNegativeButton(R.string.common_support) { _, _ ->
                // changed on email support [REDACTED_TASK_KEY]
                Analytics.send(Basic.ButtonSupport(AnalyticsParam.ScreensSources.Intro))

                val scanResponse = store.state.globalState.scanResponse
                    ?: error("ScanResponse must be not null")

                val cardInfo = store.inject(DaggerGraphState::getWalletMetaInfoUseCase).invoke(scanResponse).getOrNull()
                    ?: error("CardInfo must be not null")

                scope.launch {
                    store.inject(DaggerGraphState::sendFeedbackEmailUseCase)
                        .invoke(type = FeedbackEmailType.DirectUserRequest(cardInfo))
                }
            }
            setOnDismissListener { store.dispatchDialogHide() }
            setCancelable(false)
        }.create()
    }
}