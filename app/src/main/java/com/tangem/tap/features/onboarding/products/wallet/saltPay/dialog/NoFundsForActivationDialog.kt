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
[REDACTED_AUTHOR]
 */
class NoFundsForActivationDialog {
    companion object {
        fun create(context: Context): Dialog {
            return AlertDialog.Builder(context).apply {
                setTitle(R.string.saltpay_error_no_gas_title)
                setMessage(R.string.saltpay_error_no_gas_message)
                setPositiveButton(R.string.chat_button_title) { _, _ ->
                    val sprinklrConfig = store.state.globalState.configManager?.config
                        ?.saltPayConfig
                        ?.sprinklr
                        .guard {
                            store.dispatchDebugErrorNotification("SaltPayConfig not initialized")
                            return@setPositiveButton
                        }
                    store.dispatch(GlobalAction.OpenChat(SupportInfo(), sprinklrConfig))
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
}