package com.tangem.tap.features.details.ui.walletconnect.dialogs

import android.content.Context
import androidx.appcompat.app.AlertDialog
import com.tangem.tap.common.redux.global.GlobalAction
import com.tangem.tap.store
import com.tangem.wallet.R

class SimpleAlertDialog {
    companion object {
        fun create(
            titleRes: Int,
            messageRes: Int,
            buttonRes: Int = R.string.common_ok,
            context: Context,
        ): AlertDialog {

            return AlertDialog.Builder(context).apply {
                setTitle(context.getString(titleRes))
                setMessage(context.getText(messageRes))
                setPositiveButton(context.getText(buttonRes)) { _, _ -> }
                setOnDismissListener {
                    store.dispatch(GlobalAction.HideDialog)
                }
            }.create()
        }
    }
}