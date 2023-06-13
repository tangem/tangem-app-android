package com.tangem.tap.features.onboarding.products.wallet.saltPay.dialog

import android.app.Dialog
import android.content.Context
import androidx.appcompat.app.AlertDialog
import com.tangem.tap.common.redux.global.GlobalAction
import com.tangem.tap.domain.moduleMessage.ConvertedDialogMessage
import com.tangem.tap.domain.moduleMessage.saltPay.SaltPayErrorConverter
import com.tangem.tap.store
import com.tangem.wallet.R

/**
 * Created by Anton Zhilenkov on 12.10.2022.
 */
object RegistrationErrorDialog {
    fun create(context: Context, dialog: SaltPayDialog.Activation.OnError): Dialog {
        val convertedMessage = SaltPayErrorConverter(context).convert(dialog.error) as ConvertedDialogMessage

        return AlertDialog.Builder(context).apply {
            setTitle(convertedMessage.title)
            setMessage(convertedMessage.message)
            setPositiveButton(R.string.common_ok) { _, _ ->
                store.dispatch(GlobalAction.HideDialog)
            }
            setOnDismissListener {
                store.dispatch(GlobalAction.HideDialog)
            }
            setCancelable(false)
        }.create()
    }
}
