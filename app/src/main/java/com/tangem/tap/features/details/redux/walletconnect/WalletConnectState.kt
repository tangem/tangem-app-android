package com.tangem.tap.features.details.redux.walletconnect

import com.squareup.moshi.JsonClass
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.TransactionData
import com.tangem.blockchain.common.WalletManager
import com.tangem.blockchain.common.derivation.DerivationStyle
import com.tangem.crypto.hdWallet.DerivationPath
import com.tangem.domain.models.scan.ScanResponse
import com.tangem.tap.common.redux.StateDialog
import com.tangem.tap.domain.walletconnect2.domain.WcEthereumSignMessage
import com.tangem.tap.domain.walletconnect2.domain.WcPreparedRequest
import com.tangem.tap.domain.walletconnect2.domain.models.WalletConnectEvents
import com.tangem.tap.features.details.ui.walletconnect.WcSessionForScreen
import com.tangem.tap.features.details.ui.walletconnect.dialogs.PersonalSignDialogData
import com.tangem.tap.features.details.ui.walletconnect.dialogs.TransactionRequestDialogData
import com.trustwallet.walletconnect.models.WCPeerMeta
import com.trustwallet.walletconnect.models.session.WCSession

data class WalletConnectState(
    val loading: Boolean = false,
    val sessions: List<WalletConnectSession> = listOf(),
    val wc2Sessions: List<WcSessionForScreen> = listOf(),
    val newSessionData: NewWcSessionData? = null,
)

data class NewWcSessionData(
    val session: WalletConnectSession,
    val scanResponse: ScanResponse,
    val blockchain: Blockchain?,
)

data class WalletConnectSession(
    val peerId: String,
    val remotePeerId: String?,
    val wallet: WalletForSession,
    val session: WCSession,
    val peerMeta: WCPeerMeta,
) {
    fun getAddress(): String? {
        val key = wallet.derivedPublicKey ?: wallet.walletPublicKey ?: return null
        return wallet.blockchain?.makeAddresses(key)?.first()?.value
    }
}

@JsonClass(generateAdapter = true)
data class WalletForSession(
    val walletPublicKey: ByteArray? = null,
    val derivedPublicKey: ByteArray? = null,
    val derivationPath: DerivationPath? = null,
    val derivationStyle: DerivationStyle? = null,
    val isTestNet: Boolean = false,
    val blockchain: Blockchain? = if (isTestNet) Blockchain.EthereumTestnet else Blockchain.Ethereum,
) {

    fun getBlockchainForSession(): Blockchain {
        return blockchain ?: if (isTestNet) Blockchain.EthereumTestnet else Blockchain.Ethereum
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as WalletForSession

        if (walletPublicKey != null) {
            if (other.walletPublicKey == null) return false
            if (!walletPublicKey.contentEquals(other.walletPublicKey)) return false
        } else if (other.walletPublicKey != null) return false
        if (derivedPublicKey != null) {
            if (other.derivedPublicKey == null) return false
            if (!derivedPublicKey.contentEquals(other.derivedPublicKey)) return false
        } else if (other.derivedPublicKey != null) return false
        if (derivationPath != other.derivationPath) return false
        if (isTestNet != other.isTestNet) return false
        if (blockchain != other.blockchain) return false

        return true
    }

    override fun hashCode(): Int {
        var result = walletPublicKey?.contentHashCode() ?: 0
        result = 31 * result + (derivedPublicKey?.contentHashCode() ?: 0)
        result = 31 * result + (derivationPath?.hashCode() ?: 0)
        result = 31 * result + isTestNet.hashCode()
        result = 31 * result + (blockchain?.hashCode() ?: 0)
        return result
    }
}

sealed class WalletConnectDialog : StateDialog {
    data class ClipboardOrScanQr(val clipboardUri: String) : WalletConnectDialog()
    object UnsupportedCard : WalletConnectDialog()
    data class UnsupportedNetwork(val networks: List<String>? = null) : WalletConnectDialog()
    data class AddNetwork(val network: String) : WalletConnectDialog()
    object OpeningSessionRejected : WalletConnectDialog()
    object SessionTimeout : WalletConnectDialog()
    data class ApproveWcSession(
        val session: WalletConnectSession,
        val networks: List<Blockchain>,
    ) : WalletConnectDialog()

    data class SessionProposalDialog(
        val sessionProposal: WalletConnectEvents.SessionProposal,
        val networks: String,
        val onApprove: () -> Unit,
        val onReject: () -> Unit,
    ) : WalletConnectDialog()

    data class ChooseNetwork(
        val session: WalletConnectSession,
        val networks: List<Blockchain>,
    ) : WalletConnectDialog()

    data class RequestTransaction(val data: WcPreparedRequest.EthTransaction) :
        WalletConnectDialog()

    data class PersonalSign(val data: WcPreparedRequest.EthSign) : WalletConnectDialog()
    data class BnbTransactionDialog(
        val data: WcPreparedRequest.BnbTransaction,
    ) : WalletConnectDialog()
}

data class WcTransactionData(
    val type: WcEthTransactionType,
    val transaction: TransactionData,
    val topic: String,
    val id: Long,
    val walletManager: WalletManager,
    val dialogData: TransactionRequestDialogData,
)

enum class WcEthTransactionType {
    EthSignTransaction,
    EthSendTransaction,
}

data class WcPersonalSignData(
    val hash: ByteArray,
    val topic: String,
    val id: Long,
    val dialogData: PersonalSignDialogData,
    val type: WcEthereumSignMessage.WCSignType,
)

sealed class BinanceMessageData(
    val address: String,
    val data: ByteArray,
) {
    class Trade(
        val tradeData: List<TradeData>,
        address: String,
        data: ByteArray,
    ) : BinanceMessageData(address, data)

    class Transfer(
        val outputAddress: String,
        val amount: String,
        address: String,
        data: ByteArray,
    ) : BinanceMessageData(address, data)
}

data class TradeData(
    val price: String,
    val quantity: String,
    val amount: String,
    val symbol: String,
)
