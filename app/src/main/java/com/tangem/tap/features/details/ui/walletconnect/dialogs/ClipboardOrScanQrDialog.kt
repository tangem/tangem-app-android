package com.tangem.tap.features.details.ui.walletconnect.dialogs

import android.content.Context
import androidx.appcompat.app.AlertDialog
import com.tangem.tap.common.redux.global.GlobalAction
import com.tangem.tap.common.redux.navigation.AppScreen
import com.tangem.tap.common.redux.navigation.NavigationAction
import com.tangem.tap.features.details.redux.walletconnect.WalletConnectAction
import com.tangem.tap.store
import com.tangem.wallet.R

class ClipboardOrScanQrDialog {
    companion object {
        fun create(wcUri: String, context: Context): AlertDialog {
            return AlertDialog.Builder(context).apply {
                setTitle(context.getString(R.string.wallet_connect_title))
                setMessage(context.getText(R.string.wallet_connect_clipboard_alert))
                setPositiveButton(context.getText(R.string.wallet_connect_paste_from_clipboard)) { _, _ ->
                    store.dispatch(WalletConnectAction.OpenSession(wcUri))
                }
                setNegativeButton(context.getText(R.string.wallet_connect_scan_new_code)) { _, _ ->
                    store.dispatch(NavigationAction.NavigateTo(AppScreen.QrScan))
                }
                setOnDismissListener {
                    store.dispatch(GlobalAction.HideDialog)
                }
            }.create()
        }
    }
}
