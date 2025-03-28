package com.tangem.tap.features.details.ui.walletconnect.dialogs

import android.content.Context
import androidx.appcompat.app.AlertDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.tangem.common.routing.AppRoute
import com.tangem.domain.qrscanning.models.SourceType
import com.tangem.tap.common.extensions.dispatchNavigationAction
import com.tangem.tap.common.redux.global.GlobalAction
import com.tangem.tap.features.details.redux.walletconnect.WalletConnectAction
import com.tangem.tap.store
import com.tangem.wallet.R

object ClipboardOrScanQrDialog {
    fun create(wcUri: String, context: Context): AlertDialog {
        return MaterialAlertDialogBuilder(context, R.style.CustomMaterialDialog).apply {
            setTitle(context.getString(R.string.common_select_action))
            setMessage(context.getText(R.string.wallet_connect_clipboard_alert))
            setPositiveButton(context.getText(R.string.wallet_connect_paste_from_clipboard)) { _, _ ->
                store.dispatch(WalletConnectAction.OpenSession(wcUri, WalletConnectAction.OpenSession.SourceType.ETC))
            }
            setNegativeButton(context.getText(R.string.wallet_connect_scan_new_code)) { _, _ ->
                store.dispatchNavigationAction {
                    push(AppRoute.QrScanning(source = AppRoute.QrScanning.Source.WALLET_CONNECT))
                }
            }
            setOnDismissListener {
                store.dispatch(GlobalAction.HideDialog)
            }
        }.create()
    }
}