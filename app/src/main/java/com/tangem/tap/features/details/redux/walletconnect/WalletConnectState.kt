package com.tangem.tap.features.details.redux.walletconnect

import com.tangem.blockchain.common.TransactionData
import com.tangem.blockchain.common.WalletManager
import com.tangem.tap.common.redux.StateDialog
import com.tangem.tap.features.details.ui.walletconnect.dialogs.PersonalSignDialogData
import com.tangem.tap.features.details.ui.walletconnect.dialogs.TransactionRequestDialogData
import com.trustwallet.walletconnect.models.WCPeerMeta
import com.trustwallet.walletconnect.models.session.WCSession

data class WalletConnectState(
    val loading: Boolean = false,
    val sessions: List<WalletConnectSession> = listOf(),
)

data class WalletConnectSession(
    val peerId: String,
    val remotePeerId: String?,
    val wallet: WalletForSession,
    val session: WCSession,
    val peerMeta: WCPeerMeta,
)

data class WalletForSession(
    val cardId: String,
    val walletPublicKey: String,
    val isTestNet: Boolean = false,
) {
    val chainId
        get() = if (isTestNet) 4 else 1
}


sealed class WalletConnectDialog : StateDialog {
    data class ClipboardOrScanQr(val clipboardUri: String) : WalletConnectDialog()
    object UnsupportedCard : WalletConnectDialog()
    object OpeningSessionRejected : WalletConnectDialog()
    object SessionTimeout : WalletConnectDialog()
    data class ApproveWcSession(val session: WalletConnectSession) : WalletConnectDialog()
    data class RequestTransaction(val dialogData: TransactionRequestDialogData) :
        WalletConnectDialog()

    data class PersonalSign(val data: PersonalSignDialogData) : WalletConnectDialog()
}

data class WcTransactionData(
    val type: WcTransactionType,
    val transaction: TransactionData,
    val session: WalletConnectSession,
    val id: Long,
    val walletManager: WalletManager,
    val dialogData: TransactionRequestDialogData,
)

enum class WcTransactionType {
    EthSignTransaction,
    EthSendTransaction,
}

data class WcPersonalSignData(
    val hash: ByteArray,
    val session: WalletConnectSession,
    val id: Long,
    val dialogData: PersonalSignDialogData,

    )