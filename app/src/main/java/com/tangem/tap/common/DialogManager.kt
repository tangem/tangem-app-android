package com.tangem.tap.common

import android.app.Dialog
import android.content.Context
import com.tangem.tap.common.redux.AppDialog
import com.tangem.tap.common.redux.global.GlobalState
import com.tangem.tap.common.ui.SimpleAlertDialog
import com.tangem.tap.common.ui.SimpleCancelableAlertDialog
import com.tangem.tap.features.details.redux.walletconnect.WalletConnectDialog
import com.tangem.tap.features.details.ui.walletconnect.dialogs.ApproveWcSessionDialog
import com.tangem.tap.features.details.ui.walletconnect.dialogs.BnbTransactionDialog
import com.tangem.tap.features.details.ui.walletconnect.dialogs.ChooseNetworkDialog
import com.tangem.tap.features.details.ui.walletconnect.dialogs.ClipboardOrScanQrDialog
import com.tangem.tap.features.details.ui.walletconnect.dialogs.PersonalSignDialog
import com.tangem.tap.features.details.ui.walletconnect.dialogs.TransactionDialog
import com.tangem.tap.features.onboarding.AddressInfoBottomSheetDialog
import com.tangem.tap.features.onboarding.products.twins.redux.TwinCardsAction
import com.tangem.tap.features.onboarding.products.twins.ui.dialog.CreateWalletInterruptDialog
import com.tangem.tap.features.onboarding.products.wallet.redux.BackupDialog
import com.tangem.tap.features.onboarding.products.wallet.ui.dialogs.AddMoreBackupCardsDialog
import com.tangem.tap.features.onboarding.products.wallet.ui.dialogs.BackupInProgressDialog
import com.tangem.tap.features.onboarding.products.wallet.ui.dialogs.ConfirmDiscardingBackupDialog
import com.tangem.tap.features.onboarding.products.wallet.ui.dialogs.UnfinishedBackupFoundDialog
import com.tangem.tap.features.wallet.redux.models.WalletDialog
import com.tangem.tap.features.wallet.ui.dialogs.AmountToSendBottomSheetDialog
import com.tangem.tap.features.wallet.ui.dialogs.ChooseTradeActionBottomSheetDialog
import com.tangem.tap.features.wallet.ui.dialogs.RussianCardholdersWarningBottomSheetDialog
import com.tangem.tap.features.wallet.ui.dialogs.ScanFailsDialog
import com.tangem.tap.features.wallet.ui.dialogs.SignedHashesWarningDialog
import com.tangem.tap.features.wallet.ui.dialogs.SimpleOkDialog
import com.tangem.tap.features.wallet.ui.wallet.CurrencySelectionDialog
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
        if (dialog != null && dialog == state.dialog) return

        dialog = when (state.dialog) {
            is AppDialog.SimpleOkDialog -> SimpleOkDialog.create(state.dialog, context)
            is AppDialog.SimpleOkDialogRes -> SimpleOkDialog.create(state.dialog, context)
            is AppDialog.ScanFailsDialog -> ScanFailsDialog.create(context)
            is AppDialog.AddressInfoDialog -> AddressInfoBottomSheetDialog(state.dialog, context)
            is AppDialog.TestActionsDialog -> TestActionsBottomSheetDialog(state.dialog, context)
            is TwinCardsAction.Wallet.ShowInterruptDialog ->
                CreateWalletInterruptDialog.create(state.dialog, context)
            is WalletConnectDialog.UnsupportedCard ->
                SimpleAlertDialog.create(
                    titleRes = R.string.wallet_connect,
                    messageRes = R.string.wallet_connect_scanner_error_no_ethereum_wallet,
                    context = context
                )
            is WalletConnectDialog.AddNetwork ->
                SimpleAlertDialog.create(
                    titleRes = R.string.wallet_connect,
                    message = context.getString(
                        R.string.wallet_connect_network_not_found_format,
                        state.dialog.network
                    ),
                    context = context
                )
            is WalletConnectDialog.OpeningSessionRejected -> {
                SimpleAlertDialog.create(
                    titleRes = R.string.wallet_connect,
                    messageRes = R.string.wallet_connect_same_wcuri,
                    context = context,
                )
            }
            is WalletConnectDialog.SessionTimeout -> {
                SimpleAlertDialog.create(
                    titleRes = R.string.wallet_connect,
                    messageRes = R.string.wallet_connect_error_timeout,
                    context = context,
                )
            }
            is WalletConnectDialog.ApproveWcSession ->
                ApproveWcSessionDialog.create(state.dialog.session, state.dialog.networks, context)
            is WalletConnectDialog.ChooseNetwork ->
                ChooseNetworkDialog.create(state.dialog.session, state.dialog.networks, context)
            is WalletConnectDialog.ClipboardOrScanQr ->
                ClipboardOrScanQrDialog.create(state.dialog.clipboardUri, context)
            is WalletConnectDialog.RequestTransaction ->
                TransactionDialog.create(state.dialog.dialogData, context)
            is WalletConnectDialog.PersonalSign ->
                PersonalSignDialog.create(state.dialog.data, context)
            is WalletConnectDialog.BnbTransactionDialog ->
                BnbTransactionDialog.create(
                    data = state.dialog.data,
                    session = state.dialog.session,
                    sessionId = state.dialog.sessionId,
                    cardId = state.dialog.cardId,
                    dAppName = state.dialog.dAppName,
                    context = context,
                )
            is WalletConnectDialog.UnsupportedNetwork ->
                SimpleAlertDialog.create(
                    titleRes = R.string.wallet_connect,
                    messageRes = R.string.wallet_connect_scanner_error_unsupported_network,
                    context = context
                )
            is BackupDialog.AddMoreBackupCards -> AddMoreBackupCardsDialog.create(context)
            is BackupDialog.BackupInProgress -> BackupInProgressDialog.create(context)
            is BackupDialog.UnfinishedBackupFound -> UnfinishedBackupFoundDialog.create(context)
            is BackupDialog.ConfirmDiscardingBackup -> ConfirmDiscardingBackupDialog.create(context)
            is WalletDialog.CurrencySelectionDialog ->
                CurrencySelectionDialog.create(state.dialog, context)
            is WalletDialog.ChooseTradeActionDialog ->
                ChooseTradeActionBottomSheetDialog(context)
            is WalletDialog.SelectAmountToSendDialog ->
                AmountToSendBottomSheetDialog(context, state.dialog)
            is WalletDialog.SignedHashesMultiWalletDialog ->
                SignedHashesWarningDialog.create(context)
            is WalletDialog.TokensAreLinkedDialog -> SimpleAlertDialog.create(
                title = context.getString(state.dialog.titleRes, state.dialog.currencySymbol),
                message = context.getString(
                    state.dialog.messageRes, state.dialog.currencySymbol, state.dialog.currencyTitle
                ),
                context = context,
            )
            is WalletDialog.RemoveWalletDialog -> SimpleCancelableAlertDialog.create(
                title = context.getString(state.dialog.titleRes, state.dialog.currencyTitle),
                messageRes = state.dialog.messageRes,
                context = context,
                primaryButtonRes = state.dialog.primaryButtonRes,
                primaryButtonAction = state.dialog.onOk
            )
            is WalletDialog.RussianCardholdersWarningDialog ->
                RussianCardholdersWarningBottomSheetDialog(context)
            else -> null
        }
        dialog?.show()
    }
}
