package com.tangem.tap.features.wallet.ui.dialogs

import android.content.Context
import androidx.appcompat.app.AlertDialog
import com.tangem.tap.common.redux.global.GlobalAction
import com.tangem.tap.domain.configurable.warningMessage.WarningMessagesManager
import com.tangem.tap.features.wallet.redux.WalletAction
import com.tangem.tap.store
import com.tangem.wallet.R

class SignedHashesWarningDialog {
    companion object {
        fun create(context: Context): AlertDialog {
            return AlertDialog.Builder(context).apply {
                setTitle(context.getString(R.string.warning_important_security_info))
                setMessage(R.string.alert_signed_hashes_message)
                setPositiveButton(R.string.common_understand) { _, _ ->
                    store.dispatch(WalletAction.Warnings.CheckHashesCount.SaveCardId)
                    store.dispatch(
                        GlobalAction.HideWarningMessage(
                            WarningMessagesManager.signedHashesMultiWalletWarning()
                        )
                    )
                }
                setNegativeButton(R.string.common_cancel) { _, _ -> }
                setOnDismissListener {
                    store.dispatch(WalletAction.DialogAction.Hide)
                }
            }.create()
        }
    }

}
