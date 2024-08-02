package com.tangem.tap.common

import android.app.Dialog
import android.content.Context
import com.tangem.domain.redux.StateDialog
import com.tangem.tap.common.redux.AppDialog
import com.tangem.tap.common.redux.global.GlobalState
import com.tangem.tap.common.ui.*
import com.tangem.tap.features.details.redux.walletconnect.WalletConnectDialog
import com.tangem.tap.features.details.ui.walletconnect.dialogs.*
import com.tangem.tap.features.onboarding.OnboardingDialog
import com.tangem.tap.features.onboarding.products.twins.ui.dialog.TwinningProcessNotCompletedDialog
import com.tangem.tap.features.onboarding.products.wallet.redux.BackupDialog
import com.tangem.tap.features.onboarding.products.wallet.ui.dialogs.*
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

    @Suppress("LongMethod", "ComplexMethod")
    override fun newState(state: GlobalState) {
        if (state.dialog == null) {
            dialog?.dismiss()
            dialog = null
            return
        }
        val context = context ?: return
        if (dialog != null) return

        dialog = when (state.dialog) {
            is AppDialog.SimpleOkDialogRes -> SimpleOkDialog.create(state.dialog, context)
            is StateDialog.ScanFailsDialog -> ScanFailsDialog.create(
                context = context,
                source = state.dialog.source,
                onTryAgain = state.dialog.onTryAgain,
            )
            is AppDialog.AddressInfoDialog -> AddressInfoBottomSheetDialog(state.dialog, context)
            is AppDialog.TestActionsDialog -> TestActionsBottomSheetDialog(state.dialog, context)
            is AppDialog.RussianCardholdersWarningDialog -> RussianCardholdersWarningBottomSheetDialog(
                context,
                state.dialog.data,
            )
            is OnboardingDialog.TwinningProcessNotCompleted -> TwinningProcessNotCompletedDialog.create(context)
            is OnboardingDialog.InterruptOnboarding -> InterruptOnboardingDialog.create(context, state.dialog)
            is OnboardingDialog.WalletActivationError -> WalletActivationErrorDialog.create(context, state.dialog)
            is WalletConnectDialog.UnsupportedCard ->
                SimpleAlertDialog.create(
                    titleRes = R.string.wallet_connect_title,
                    messageRes = R.string.wallet_connect_scanner_error_not_valid_card,
                    context = context,
                )
            is WalletConnectDialog.AddNetwork -> {
                val message = context.getString(
                    R.string.wallet_connect_error_missing_blockchains,
                ) + state.dialog.networks.joinToString()
                SimpleAlertDialog.create(
                    titleRes = R.string.wallet_connect_title,
                    message = message,
                    context = context,
                )
            }
            is WalletConnectDialog.OpeningSessionRejected -> {
                SimpleAlertDialog.create(
                    titleRes = R.string.wallet_connect_title,
                    messageRes = R.string.wallet_connect_same_wcuri,
                    context = context,
                )
            }
            is WalletConnectDialog.SessionTimeout -> {
                SimpleAlertDialog.create(
                    titleRes = R.string.wallet_connect_title,
                    messageRes = R.string.wallet_connect_error_timeout,
                    context = context,
                )
            }
            is WalletConnectDialog.ClipboardOrScanQr ->
                ClipboardOrScanQrDialog.create(state.dialog.clipboardUri, context)
            is WalletConnectDialog.RequestTransaction -> TransactionDialog.create(state.dialog.data, context)
            is WalletConnectDialog.PersonalSign -> PersonalSignDialog.create(state.dialog.data, context)
            is WalletConnectDialog.BnbTransactionDialog ->
                BnbTransactionDialog.create(
                    preparedData = state.dialog.data,
                    context = context,
                )
            is WalletConnectDialog.UnsupportedNetwork -> {
                val warning = if (state.dialog.networks.isNullOrEmpty()) {
                    context.getString(R.string.wallet_connect_scanner_error_unsupported_network)
                } else {
                    context.getString(R.string.wallet_connect_error_unsupported_blockchains) +
                        state.dialog.networks.joinToString()
                }
                SimpleAlertDialog.create(
                    titleRes = R.string.wallet_connect_title,
                    message = warning,
                    context = context,
                )
            }
            is WalletConnectDialog.UnsupportedDapp -> SimpleAlertDialog.create(
                titleRes = R.string.wallet_connect_title,
                messageRes = R.string.wallet_connect_error_unsupported_dapp,
                context = context,
            )
            is WalletConnectDialog.SessionProposalDialog -> {
                SessionProposalDialog.create(
                    sessionProposal = state.dialog.sessionProposal,
                    networks = state.dialog.networks,
                    context = context,
                    onApprove = state.dialog.onApprove,
                    onReject = state.dialog.onReject,
                )
            }
            is WalletConnectDialog.SignTransactionDialog -> SignTransactionDialog.create(
                preparedData = state.dialog.data,
                context = context,
            )
            is WalletConnectDialog.PairConnectErrorDialog -> SimpleAlertDialog.create(
                titleRes = R.string.wallet_connect_title,
                message = state.dialog.error.message,
                context = context,
            )
            is BackupDialog.AttestationFailed -> AttestationFailedDialog.create(context)
            is BackupDialog.AddMoreBackupCards -> AddMoreBackupCardsDialog.create(context)
            is BackupDialog.BackupInProgress -> BackupInProgressDialog.create(context)
            is BackupDialog.UnfinishedBackupFound -> UnfinishedBackupFoundDialog.create(context)
            is BackupDialog.ConfirmDiscardingBackup -> ConfirmDiscardingBackupDialog.create(context)
            is BackupDialog.ResetBackupCard -> ResetBackupCardDialog.create(
                context = context,
                cardId = state.dialog.cardId,
            )
            is AppDialog.TokensAreLinkedDialog -> SimpleAlertDialog.create(
                title = context.getString(state.dialog.titleRes, state.dialog.currencySymbol),
                message = context.getString(
                    state.dialog.messageRes,
                    state.dialog.currencyTitle,
                    state.dialog.currencySymbol,
                    state.dialog.networkName,
                ),
                context = context,
            )
            is AppDialog.RemoveWalletDialog -> SimpleCancelableAlertDialog.create(
                title = context.getString(state.dialog.titleRes, state.dialog.currencyTitle),
                messageRes = state.dialog.messageRes,
                context = context,
                primaryButtonRes = state.dialog.primaryButtonRes,
                primaryButtonAction = state.dialog.onOk,
            )
            else -> null
        }
        dialog?.show()
    }
}