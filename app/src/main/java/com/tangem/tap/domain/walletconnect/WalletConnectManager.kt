package com.tangem.tap.domain.walletconnect

import com.tangem.blockchain.common.Blockchain
import com.tangem.common.card.EllipticCurve
import com.tangem.common.extensions.guard
import com.tangem.domain.common.ScanResponse
import com.tangem.core.analytics.Analytics
import com.tangem.tap.common.analytics.events.WalletConnect
import com.tangem.tap.common.extensions.dispatchOnMain
import com.tangem.tap.common.redux.global.GlobalAction
import com.tangem.tap.domain.TapError
import com.tangem.tap.domain.walletconnect.extensions.isDappSupported
import com.tangem.tap.features.details.redux.walletconnect.WalletConnectAction
import com.tangem.tap.features.details.redux.walletconnect.WalletConnectDialog
import com.tangem.tap.features.details.redux.walletconnect.WalletConnectSession
import com.tangem.tap.features.details.redux.walletconnect.WalletForSession
import com.tangem.tap.features.details.redux.walletconnect.WcPersonalSignData
import com.tangem.tap.features.details.redux.walletconnect.WcTransactionData
import com.tangem.tap.features.details.redux.walletconnect.WcTransactionType
import com.tangem.tap.scope
import com.tangem.tap.store
import com.tangem.tap.walletConnectRepository
import com.trustwallet.walletconnect.WCClient
import com.trustwallet.walletconnect.models.WCPeerMeta
import com.trustwallet.walletconnect.models.binance.WCBinanceCancelOrder
import com.trustwallet.walletconnect.models.binance.WCBinanceTradeOrder
import com.trustwallet.walletconnect.models.binance.WCBinanceTransferOrder
import com.trustwallet.walletconnect.models.binance.WCBinanceTxConfirmParam
import com.trustwallet.walletconnect.models.ethereum.WCEthereumSignMessage
import com.trustwallet.walletconnect.models.ethereum.WCEthereumTransaction
import com.trustwallet.walletconnect.models.session.WCSession
import com.trustwallet.walletconnect.models.session.WCSessionUpdate
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import timber.log.Timber
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.collections.set

class WalletConnectManager {

    private var cardId: String? = null

    private val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .writeTimeout(20, TimeUnit.SECONDS)
            .addInterceptor(interceptor)
            .addInterceptor(RetryInterceptor())
            .build()
    }
    private val interceptor by lazy {
        HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BODY }
    }

    private var sessions: MutableMap<WCSession, WalletConnectActiveData> = mutableMapOf()

    fun connect(wcUri: String) {
        val session = WCSession.from(wcUri).guard {
            store.dispatchOnMain(
                WalletConnectAction.FailureEstablishingSession(
                    session = null,
                    error = TapError.WalletConnect.UnsupportedLink,
                ),
            )
            return
        }
        if (sessions[session] != null) {
            store.dispatchOnMain(WalletConnectAction.RefuseOpeningSession)
            return
        }
        val client = WCClient(httpClient = okHttpClient)
        setListeners(client)
        val peerId = UUID.randomUUID().toString()

        try {
            client.connect(session, tangemPeerMeta, peerId)
        } catch (exception: IllegalArgumentException) {
            store.dispatchOnMain(
                WalletConnectAction.FailureEstablishingSession(
                    session = null,
                    error = TapError.WalletConnect.UnsupportedLink,
                ),
            )
            return
        }

        sessions[session] = WalletConnectActiveData(
            peerId = peerId,
            remotePeerId = null,
            session = session,
            client = client,
            wallet = WalletForSession(),
        )
        setupConnectionTimeoutCheck(session)
    }

    fun updateSession(session: WalletConnectSession) {
        val updatedSession = sessions[session.session]?.copy(
            wallet = session.wallet
        )
        if (updatedSession != null) {
            sessions[session.session] = updatedSession
        }
    }

    fun updateBlockchain(session: WalletConnectSession) {
        sessions[session.session]?.client?.updateSession(
            accounts = listOfNotNull(session.getAddress()),
            chainId = session.wallet.blockchain?.getChainId(),
            approved = true
        )


        val updatedSession = sessions[session.session]?.copy(
            wallet = session.wallet
        )
        if (updatedSession != null) {
            sessions[session.session] = updatedSession
        }
    }

    private fun setupConnectionTimeoutCheck(session: WCSession) {
        scope.launch {
            delay(20_000)
            val data = sessions[session]
            if (data != null && data.peerMeta == null) {
                disconnect(session)
                store.dispatchOnMain(WalletConnectAction.OpeningSessionTimeout(session))
            }
        }
    }

    fun restoreSessions(scanResponse: ScanResponse) {
        val walletPublicKey = scanResponse.card.wallets.firstOrNull { it.curve == EllipticCurve.Secp256k1 }?.publicKey
            ?: return
        if (scanResponse.card.backupStatus?.isActive != true) cardId = scanResponse.card.cardId
        val sessions = walletConnectRepository.loadSavedSessions()
            // filter sessions for this particular card
            .filter { it.wallet.walletPublicKey.contentEquals(walletPublicKey) }
        this.sessions = sessions
            .map { session ->
                WalletConnectActiveData(
                    peerId = session.peerId,
                    remotePeerId = session.remotePeerId,
                    client = WCClient(httpClient = okHttpClient),
                    session = session.session,
                    peerMeta = session.peerMeta,
                    wallet = session.wallet,
                )
                    .also {
                        setListeners(it.client)
                        it.client.connect(it.session, tangemPeerMeta, it.peerId, it.remotePeerId)
                    }
            }.associateBy { it.session }.toMutableMap()

        store.dispatchOnMain(WalletConnectAction.SetSessionsRestored(sessions))
    }

    fun approve(session: WCSession) {
        val activeData = sessions[session] ?: return
        removeSimilarSessions(activeData)

        val key = activeData.wallet.derivedPublicKey ?: activeData.wallet.walletPublicKey ?: return
        val blockchain = activeData.wallet.getBlockchainForSession()
        val accounts = listOf(blockchain.makeAddresses(key).first().value)
        val approved = activeData.client.approveSession(
            accounts = accounts,
            chainId = blockchain.getChainId() ?: Blockchain.Ethereum.getChainId()!!
        )
        if (approved) {
            val walletConnectSession = WalletConnectSession(
                peerId = activeData.peerId,
                remotePeerId = activeData.remotePeerId,
                wallet = activeData.wallet,
                session = session,
                peerMeta = activeData.peerMeta!!
            )
            walletConnectRepository.saveSession(walletConnectSession)
            store.dispatchOnMain(
                WalletConnectAction.ApproveSession.Success(
                    walletConnectSession
                )
            )

        }
    }

    fun removeSimilarSessions(activeData: WalletConnectActiveData) {
        val sessionsToRemove = sessions.filter {
            it.value.wallet.walletPublicKey?.equals(activeData.wallet.walletPublicKey) == true
                    && it.value.peerMeta?.url == activeData.peerMeta?.url
                    && it.value.session != activeData.session
        }
        Timber.d("RemoveSimilarSessions: ${sessionsToRemove.values.map { it.client.session }}")
        sessionsToRemove.forEach { disconnect(it.value.session) }
    }

    fun rejectRequest(session: WCSession, id: Long) {
        val activeData = sessions[session] ?: return
        activeData.client.rejectRequest(id)
    }

    fun acceptRequest(session: WCSession, id: Long, data: String) {
        val activeData = sessions[session] ?: return
        activeData.client.approveRequest(id, data)
    }

    fun disconnect(session: WCSession) {
        val activeData = sessions[session] ?: return
        val disconnected = if (activeData.client.isConnected) {
            activeData.client.killSession()
        } else {
            true
        }

        if (disconnected) {
            onSessionClosed(session)
        }
    }

    private fun onSessionClosed(session: WCSession) {
        Analytics.send(WalletConnect.SessionDisconnected())
        sessions.remove(session)
        walletConnectRepository.removeSession(session)
        store.dispatchOnMain(WalletConnectAction.RemoveSession(session))
    }

    fun handleTransactionRequest(
        transaction: WCEthereumTransaction,
        session: WalletConnectSession,
        id: Long,
        type: WcTransactionType,
    ) {
        val activeData = sessions[session.session] ?: return
        scope.launch {
            val data = WalletConnectSdkHelper().prepareTransactionData(
                transaction = transaction,
                session = session,
                id = id,
                type = type
            ).guard {
                sessions[session.session] = activeData.copy(transactionData = null)
                store.dispatchOnMain(
                    WalletConnectAction.RejectRequest(
                        session.session,
                        id
                    )
                )
                return@launch
            }
            sessions[session.session] = activeData.copy(transactionData = data)

            store.dispatchOnMain(
                GlobalAction.ShowDialog(
                    WalletConnectDialog.RequestTransaction(
                        data.dialogData
                    )
                )
            )
        }
    }

    fun completeTransaction(session: WCSession) {
        val activeData = sessions[session]
        val data = activeData?.transactionData ?: return
        scope.launch {
            val hash = WalletConnectSdkHelper().completeTransaction(data, cardId).guard {
                sessions[data.session.session] = activeData.copy(transactionData = null)
                store.dispatchOnMain(
                    WalletConnectAction.RejectRequest(
                        data.session.session,
                        data.id,
                    ),
                )
                return@launch
            }
            acceptRequest(session, data.id, hash)
            sessions[session] = activeData.copy(transactionData = null)
        }
    }

    fun signBnb(
        id: Long, data: ByteArray, sessionData: WCSession,
    ) {
        val activeData = sessions[sessionData] ?: return
        scope.launch {
            val hash = WalletConnectSdkHelper().signBnbTransaction(data, activeData, cardId).guard {
                store.dispatchOnMain(
                    WalletConnectAction.RejectRequest(
                        sessionData,
                        id,
                    ),
                )
                return@launch
            }
            acceptRequest(sessionData, id, hash)
        }
    }

    fun handlePersonalSignRequest(
        message: WCEthereumSignMessage,
        session: WalletConnectSession,
        id: Long,
    ) {
        val activeData = sessions[session.session] ?: return
        scope.launch {
            val data = WalletConnectSdkHelper().prepareDataForPersonalSign(
                message = message, session = session, id = id
            )
            sessions[session.session] = activeData.copy(personalSignData = data)
            store.dispatchOnMain(
                GlobalAction.ShowDialog(
                    WalletConnectDialog.PersonalSign(
                        data.dialogData
                    )
                )
            )
        }
    }

    fun sendSignedMessage(session: WCSession) {
        val activeData = sessions[session]
        val data = activeData?.personalSignData ?: return
        scope.launch {
            val hash = WalletConnectSdkHelper().signPersonalMessage(data.hash, activeData.wallet, cardId)
                .guard {
                    sessions[data.session.session] = activeData.copy(transactionData = null)
                    store.dispatchOnMain(
                        WalletConnectAction.RejectRequest(
                            data.session.session,
                            data.id,
                        ),
                    )
                    return@launch
                }
            sessions[session] = activeData.copy(personalSignData = data)
            acceptRequest(data.session.session, data.id, hash)
            sessions[data.session.session] = activeData.copy(transactionData = null)
        }
    }


    fun setListeners(client: WCClient) {
        client.onSessionRequest = { id: Long, peer: WCPeerMeta ->
            Timber.d("OnSessionRequest: $peer")
            val session = client.session
            val data = sessions[session]?.copy(peerMeta = peer, remotePeerId = client.remotePeerId)
            if (data != null && session != null) {
                if (!peer.isDappSupported()) {
                    store.dispatchOnMain(
                        WalletConnectAction.FailureEstablishingSession(
                            session = session,
                            error = TapError.WalletConnect.UnsupportedDapp,
                        ),
                    )
                } else {
                    sessions[session] = data
                    val sessionData = data.toWalletConnectSession()
                    sessionData?.let {
                        store.dispatchOnMain(
                            WalletConnectAction.ScanCard(
                                session = sessionData,
                                chainId = client.chainId?.toIntOrNull(),
                            ),
                        )
                    }
                    Analytics.send(WalletConnect.NewSessionEstablished())
                }
            }
        }
        client.onSessionUpdate = { id: Long, update: WCSessionUpdate ->
            Timber.d("onSessionUpdate: $update")
            val session = client.session
            if (session != null && !update.approved) onSessionClosed(session)
        }
        client.onEthSendTransaction = { id: Long, transaction: WCEthereumTransaction ->
            Timber.d("onEthSendTransaction: $transaction")
            // Analytics.logWcEvent(
            //     AnalyticsAnOld.WcAnalyticsEvent.Action(
            //         AnalyticsAnOld.WcAction.SendTransaction
            //     )
            // )
            sessions[client.session]?.toWalletConnectSession()?.let { sessionData ->
                store.dispatchOnMain(
                    WalletConnectAction.HandleTransactionRequest(
                        transaction = transaction,
                        session = sessionData,
                        id = id,
                        type = WcTransactionType.EthSendTransaction
                    )
                )
            }
        }
        client.onEthSignTransaction = { id: Long, transaction: WCEthereumTransaction ->
            Timber.d("onEthSignTransaction: $transaction")
            // Analytics.logWcEvent(
            //     AnalyticsAnOld.WcAnalyticsEvent.Action(
            //         AnalyticsAnOld.WcAction.SignTransaction
            //     )
            // )
            sessions[client.session]?.toWalletConnectSession()?.let { sessionData ->
                store.dispatchOnMain(
                    WalletConnectAction.HandleTransactionRequest(
                        transaction = transaction,
                        session = sessionData,
                        id = id,
                        type = WcTransactionType.EthSignTransaction
                    )
                )
            }
        }
        client.onEthSign = { id: Long, message: WCEthereumSignMessage ->
            Timber.d("onEthSign: $message")
            // Analytics.logWcEvent(
            //     AnalyticsAnOld.WcAnalyticsEvent.Action(
            //         AnalyticsAnOld.WcAction.PersonalSign
            //     )
            // )
            sessions[client.session]?.toWalletConnectSession()?.let { sessionData ->
                store.dispatchOnMain(
                    WalletConnectAction.HandlePersonalSignRequest(
                        message,
                        sessionData,
                        id
                    )
                )
            }
        }


        client.onBnbCancel = { id: Long, order: WCBinanceCancelOrder ->

        }
        client.onBnbTrade = { id: Long, order: WCBinanceTradeOrder ->
            sessions[client.session]?.toWalletConnectSession()?.let { sessionData ->
                store.dispatchOnMain(
                    WalletConnectAction.BinanceTransaction.Trade(
                        id = id, order = order, sessionData = sessionData
                    )
                )
            }
        }
        client.onBnbTransfer = { id: Long, order: WCBinanceTransferOrder ->
            sessions[client.session]?.toWalletConnectSession()?.let { sessionData ->
                store.dispatchOnMain(
                    WalletConnectAction.BinanceTransaction.Transfer(
                        id = id, order = order, sessionData = sessionData
                    )
                )
            }
        }
        client.onBnbTxConfirm = { id: Long, order: WCBinanceTxConfirmParam ->
            // send empty approve request if status is OK
            if (order.ok) client.approveRequest(id, "")
        }
        client.onDisconnect = { code: Int, reason: String ->
            val session = client.session
            if (session != null) {
                onSessionClosed(session)
            }
        }
        client.onWalletChangeNetwork = { id: Long, chainId: Int ->
            Timber.d("On WC Switch Chain")
            val blockchain = Blockchain.fromChainId(chainId)
            Timber.d("WC switch chainID\nNew Blockchain: $blockchain")
            val session = sessions[client.session]?.toWalletConnectSession()
            if (session != null) {
                store.dispatchOnMain(WalletConnectAction.SwitchBlockchain(blockchain, session))
            }
        }
    }

    companion object {
        private val tangemPeerMeta =
            WCPeerMeta(name = "Tangem Wallet", url = "https://tangem.com")

        fun isCorrectWcUri(string: String): Boolean = WCSession.from(string) != null

        const val WC_SCHEME = "wc"
    }
}

data class WalletConnectActiveData(
    val peerId: String,
    val remotePeerId: String?,
    val client: WCClient,
    val session: WCSession,
    val peerMeta: WCPeerMeta? = null,
    val wallet: WalletForSession,
    val transactionData: WcTransactionData? = null,
    val personalSignData: WcPersonalSignData? = null,
) {
    fun toWalletConnectSession(): WalletConnectSession? {
        if (peerMeta == null) return null
        return WalletConnectSession(
            peerId = peerId,
            remotePeerId = remotePeerId,
            wallet = wallet,
            session = session,
            peerMeta = peerMeta
        )
    }
}

class RetryInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request: Request = chain.request()
        val response = chain.proceed(request)
        when (response.code) {
            502 -> {
                return chain.proceed(request)
            }
        }
        return response
    }
}
