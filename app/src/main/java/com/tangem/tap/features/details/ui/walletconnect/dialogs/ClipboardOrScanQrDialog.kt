package com.tangem.tap.features.details.ui.walletconnect.dialogs

import android.content.Context
import androidx.appcompat.app.AlertDialog
import androidx.core.os.bundleOf
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.tangem.core.navigation.AppScreen
import com.tangem.core.navigation.NavigationAction
import com.tangem.feature.qrscanning.QrScanningRouter
import com.tangem.domain.qrscanning.models.SourceType
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
                store.dispatch(WalletConnectAction.OpenSession(wcUri))
            }
            setNegativeButton(context.getText(R.string.wallet_connect_scan_new_code)) { _, _ ->
                store.dispatch(
                    NavigationAction.NavigateTo(
                        screen = AppScreen.QrScanning,
                        bundle = bundleOf(
                            QrScanningRouter.SOURCE_KEY to SourceType.WALLET_CONNECT,
                        ),
                    ),
                )
            }
            setOnDismissListener {
                store.dispatch(GlobalAction.HideDialog)
            }
        }.create()
    }
}
