package com.tangem.tap.features.details.redux.walletconnect

import android.app.Activity
import com.tangem.tap.common.redux.NotificationAction
import com.tangem.wallet.R
import com.trustwallet.walletconnect.models.ethereum.WCEthereumSignMessage
import com.trustwallet.walletconnect.models.ethereum.WCEthereumTransaction
import com.trustwallet.walletconnect.models.session.WCSession
import org.rekotlin.Action

sealed class WalletConnectAction : Action {

    data class HandleDeepLink(val wcUri: String?) : WalletConnectAction()

    object RestoreSessions : WalletConnectAction()

    data class StartWalletConnect(
        val activity: Activity,
    ) : WalletConnectAction()

    data class ShowClipboardOrScanQrDialog(val wcUri: String) : WalletConnectAction()

    data class ScanCard(val wcUri: String) : WalletConnectAction()

    object UnsupportedCard : WalletConnectAction()

    data class OpenSession(
        val wcUri: String,
        val wallet: WalletForSession,
    ) : WalletConnectAction()

    object RefuseOpeningSession : WalletConnectAction()

    data class OpeningSessionTimeout(val session: WCSession) : WalletConnectAction()

    data class AcceptOpeningSession(val session: WalletConnectSession) : WalletConnectAction()

    data class ApproveSession(
        val session: WCSession,
    ) : WalletConnectAction() {
        data class Success(val session: WalletConnectSession) : WalletConnectAction()
    }

    data class FailureEstablishingSession(val session: WCSession?) : WalletConnectAction()

    data class SetSessionsRestored(val sessions: List<WalletConnectSession>) :
        WalletConnectAction()

    data class DisconnectSession(val session: WCSession) : WalletConnectAction()

    data class RemoveSession(val session: WCSession) : WalletConnectAction()

    data class HandleTransactionRequest(
        val transaction: WCEthereumTransaction,
        val session: WalletConnectSession,
        val id: Long,
        val type: WcTransactionType,
    ) :
        WalletConnectAction()

    data class SetDataToSend(val transactionData: WcTransactionData) : WalletConnectAction()

    data class HandlePersonalSignRequest(
        val message: WCEthereumSignMessage,
        val session: WalletConnectSession,
        val id: Long,
    ) : WalletConnectAction()

    data class SendTransaction(val session: WCSession) : WalletConnectAction()

    data class SignMessage(val session: WCSession) : WalletConnectAction()

    data class RejectRequest(val session: WCSession, val id: Long) : WalletConnectAction()

    object NotEnoughFunds : WalletConnectAction(), NotificationAction {
        override val messageResource = R.string.wallet_connect_create_tx_not_enough_funds
    }

    object NotifyCameraPermissionIsRequired : WalletConnectAction(), NotificationAction {
        override val messageResource = R.string.common_camera_denied_alert_message
    }
}