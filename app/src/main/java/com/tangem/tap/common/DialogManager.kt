package com.tangem.tap.common

import android.app.Dialog
import android.content.Context
import com.tangem.tap.common.redux.AppDialog
import com.tangem.tap.common.redux.global.GlobalState
import com.tangem.tap.features.details.redux.walletconnect.WalletConnectDialog
import com.tangem.tap.features.details.ui.walletconnect.dialogs.*
import com.tangem.tap.features.onboarding.AddressInfoBottomSheetDialog
import com.tangem.tap.features.onboarding.products.twins.redux.TwinCardsAction
import com.tangem.tap.features.onboarding.products.twins.ui.dialog.CreateWalletInterruptDialog
import com.tangem.tap.features.onboarding.products.wallet.redux.BackupDialog
import com.tangem.tap.features.onboarding.products.wallet.ui.dialogs.AddMoreBackupCardsDialog
import com.tangem.tap.features.onboarding.products.wallet.ui.dialogs.BackupInProgressDialog
import com.tangem.tap.features.onboarding.products.wallet.ui.dialogs.ConfirmDiscardingBackupDialog
import com.tangem.tap.features.onboarding.products.wallet.ui.dialogs.UnfinishedBackupFoundDialog
import com.tangem.tap.features.wallet.ui.dialogs.ScanFailsDialog
import com.tangem.tap.store
import com.tangem.wallet.R
import org.rekotlin.StoreSubscriber

class DialogManager : StoreSubscriber<GlobalState> {
    var context: Context? = null
    private var dialog: Dialog? = null

    fun onStart(context: Context) {
        this.context = context
        store.subscribe(this) { state ->
            state.skipRepeats { oldState, newState ->
                oldState.globalState == newState.globalState
            }.select { it.globalState }
        }
    }

    fun onStop() {
        this.context = null
        store.unsubscribe(this)
    }


    override fun newState(state: GlobalState) {
        if (state.dialog == null) {
            dialog?.dismiss()
            dialog = null
            return
        }
        val context = context ?: return
        if (dialog != null) return

        dialog = when (state.dialog) {
            is AppDialog.ScanFailsDialog -> ScanFailsDialog.create(context)
            is AppDialog.AddressInfoDialog -> AddressInfoBottomSheetDialog(state.dialog, context)
            is TwinCardsAction.Wallet.ShowInterruptDialog -> CreateWalletInterruptDialog.create(state.dialog, context)
            is WalletConnectDialog.UnsupportedCard ->
                SimpleAlertDialog.create(
                    titleRes = R.string.wallet_connect,
                    messageRes = R.string.wallet_connect_scanner_error_no_ethereum_wallet,
                    context = context
                )
            is WalletConnectDialog.OpeningSessionRejected -> {
                SimpleAlertDialog.create(
                    titleRes = R.string.wallet_connect,
                    messageRes = R.string.wallet_connect_same_wcuri,
                    context = context
                )
            }
            is WalletConnectDialog.SessionTimeout -> {
                SimpleAlertDialog.create(
                    titleRes = R.string.wallet_connect,
                    messageRes = R.string.wallet_connect_error_timeout,
                    context = context
                )
            }
            is WalletConnectDialog.ApproveWcSession ->
                ApproveWcSessionDialog.create(state.dialog.session, context)
            is WalletConnectDialog.ClipboardOrScanQr ->
                ClipboardOrScanQrDialog.create(state.dialog.clipboardUri, context)
            is WalletConnectDialog.RequestTransaction ->
                TransactionDialog.create(state.dialog.dialogData, context)
            is WalletConnectDialog.PersonalSign ->
                PersonalSignDialog.create(state.dialog.data, context)
            is BackupDialog.AddMoreBackupCards -> AddMoreBackupCardsDialog.create(context)
            is BackupDialog.BackupInProgress -> BackupInProgressDialog.create(context)
            is BackupDialog.UnfinishedBackupFound -> UnfinishedBackupFoundDialog.create(context)
            is BackupDialog.ConfirmDiscardingBackup -> ConfirmDiscardingBackupDialog.create(context)
            else -> null
        }
        dialog?.show()
    }
}