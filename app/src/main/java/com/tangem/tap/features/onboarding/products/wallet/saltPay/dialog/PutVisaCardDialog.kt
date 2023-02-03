package com.tangem.tap.features.onboarding.products.wallet.saltPay.dialog

import android.app.Dialog
import android.content.Context
import androidx.appcompat.app.AlertDialog
import com.tangem.tap.common.redux.global.GlobalAction
import com.tangem.tap.store
import com.tangem.wallet.R

/**
 * Created by Anton Zhilenkov on 12.10.2022.
 */
object PutVisaCardDialog {
    fun create(context: Context): Dialog {
        return AlertDialog.Builder(context).apply {
            setTitle(R.string.saltpay_error_empty_backup_title)
            setMessage(R.string.saltpay_error_empty_backup_message)
            setPositiveButton(R.string.common_ok) { _, _ -> }
            setOnDismissListener {
                store.dispatch(GlobalAction.HideDialog)
            }
            setCancelable(false)
        }.create()
    }
}
