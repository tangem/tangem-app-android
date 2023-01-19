package com.tangem.tap.features.onboarding.products.wallet.saltPay.dialog

import android.app.Dialog
import android.content.Context
import androidx.appcompat.app.AlertDialog
import com.tangem.common.extensions.guard
import com.tangem.tap.common.extensions.dispatchDebugErrorNotification
import com.tangem.tap.common.feedback.SupportInfo
import com.tangem.tap.common.redux.global.GlobalAction
import com.tangem.tap.store
import com.tangem.wallet.R

/**
 * Created by Anton Zhilenkov on 12.10.2022.
 */
object NoFundsForActivationDialog {
    fun create(context: Context): Dialog {
        return AlertDialog.Builder(context).apply {
            setTitle(R.string.saltpay_error_no_gas_title)
            setMessage(R.string.saltpay_error_no_gas_message)
            // TODO: SaltPay: change onboarding_supplement_button_kyc_waiting -> to appropriate string
            setPositiveButton(R.string.onboarding_supplement_button_kyc_waiting) { _, _ ->
                val config = store.state.globalState.configManager?.config?.saltPayConfig?.zendesk.guard {
                    store.dispatchDebugErrorNotification("SaltPayConfig not initialized")
                    return@setPositiveButton
                }
                store.dispatch(GlobalAction.OpenChat(SupportInfo(), config))
            }
            setNegativeButton(R.string.common_cancel) { _, _ ->
            }
            setOnDismissListener {
                store.dispatch(GlobalAction.HideDialog)
            }
            setCancelable(false)
        }.create()
    }
}
