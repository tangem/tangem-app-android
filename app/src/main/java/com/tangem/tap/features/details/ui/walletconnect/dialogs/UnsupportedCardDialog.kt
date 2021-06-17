package com.tangem.tap.features.details.ui.walletconnect.dialogs

import android.content.Context
import androidx.appcompat.app.AlertDialog
import com.tangem.tap.common.redux.global.GlobalAction
import com.tangem.tap.store
import com.tangem.wallet.R

class UnsupportedCardDialog {
    companion object {
        fun create(context: Context): AlertDialog {

            return AlertDialog.Builder(context).apply {
                setTitle(context.getString(R.string.wallet_connect))
                setMessage(context.getText(R.string.wallet_connect_scanner_error_no_ethereum_wallet))
                setPositiveButton(context.getText(R.string.common_ok)) { _, _ -> }
//                setNegativeButton(context.getText(R.string.common_reject)) { _, _ -> }
                setOnDismissListener {
                    store.dispatch(GlobalAction.HideDialog)
                }
            }.create()
        }
    }
}