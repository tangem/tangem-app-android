package com.tangem.data.walletconnect.sdk

import android.app.Application
import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.reown.android.Core
import com.reown.android.CoreClient
import com.reown.android.relay.ConnectionType
import com.reown.walletkit.client.Wallet
import com.reown.walletkit.client.WalletKit
import com.tangem.data.walletconnect.utils.WcSdkObserver
import com.tangem.domain.walletconnect.WC_TAG
import com.tangem.domain.walletconnect.WalletConnectProjectIdProvider
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.logging.TangemLogger
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import javax.inject.Provider
import kotlin.coroutines.resume

/**
 * The only place that talks to the WalletConnect SDK.
 *
 * Every call routes through here so that starting the SDK stays one decision instead of a check scattered
 * over the call sites. Operations that can be the first thing a user does — pairing, listing sessions,
 * disconnecting — are suspending and bring the SDK up themselves; operations that only ever run inside an
 * already-live session are plain functions that assume it is up.
 *
 * Initialization costs ~400 ms, most of it Keystore IPC and the SQLCipher key derivation behind the SDK's
 * own storage, which is why it no longer runs during `Application.onCreate`.
 */
internal class WcSdk(
    private val application: Application,
    private val projectIdProvider: WalletConnectProjectIdProvider,
    private val observers: Provider<Set<WcSdkObserver>>,
    private val dispatchers: CoroutineDispatcherProvider,
) {

    private val mutex = Mutex()

    @Volatile
    var isInitialized: Boolean = false
        private set

    suspend fun ensureInitialized() {
        if (isInitialized) return

        mutex.withLock {
            if (isInitialized) return

            initialize()
            isInitialized = true
        }
    }

    suspend fun pair(uri: String): Either<Throwable, Unit> {
        ensureInitialized()
        return suspendCancellableCoroutine { continuation ->
            WalletKit.pair(
                params = Wallet.Params.Pair(uri),
                onSuccess = { if (continuation.isActive) continuation.resume(Unit.right()) },
                onError = { if (continuation.isActive) continuation.resume(it.throwable.left()) },
            )
        }
    }

    suspend fun approveSession(params: Wallet.Params.SessionApprove): Either<Throwable, Unit> {
        ensureInitialized()
        return suspendCancellableCoroutine { continuation ->
            WalletKit.approveSession(
                params = params,
                onSuccess = { if (continuation.isActive) continuation.resume(Unit.right()) },
                onError = { if (continuation.isActive) continuation.resume(it.throwable.left()) },
            )
        }
    }

    suspend fun activeSessions(): List<Wallet.Model.Session> {
        ensureInitialized()
        return WalletKit.getListOfActiveSessions()
    }

    suspend fun disconnectSession(topic: String): Either<Throwable, Unit> {
        ensureInitialized()
        return suspendCancellableCoroutine { continuation ->
            WalletKit.disconnectSession(
                params = Wallet.Params.SessionDisconnect(topic),
                onSuccess = { if (continuation.isActive) continuation.resume(Unit.right()) },
                onError = { if (continuation.isActive) continuation.resume(it.throwable.left()) },
            )
        }
    }

    suspend fun extendSession(topic: String): Either<Throwable, Unit> {
        ensureInitialized()
        return suspendCancellableCoroutine { continuation ->
            WalletKit.extendSession(
                params = Wallet.Params.SessionExtend(topic),
                onSuccess = { if (continuation.isActive) continuation.resume(Unit.right()) },
                onError = { if (continuation.isActive) continuation.resume(it.throwable.left()) },
            )
        }
    }

    suspend fun updateSession(params: Wallet.Params.SessionUpdate): Either<Throwable, Wallet.Params.SessionUpdate> {
        ensureInitialized()
        return suspendCancellableCoroutine { continuation ->
            WalletKit.updateSession(
                params = params,
                onSuccess = { if (continuation.isActive) continuation.resume(it.right()) },
                onError = { if (continuation.isActive) continuation.resume(it.throwable.left()) },
            )
        }
    }

    suspend fun respondSessionRequest(
        params: Wallet.Params.SessionRequestResponse,
    ): Either<Throwable, Wallet.Params.SessionRequestResponse> {
        ensureInitialized()
        return suspendCancellableCoroutine { continuation ->
            WalletKit.respondSessionRequest(
                params = params,
                onSuccess = { if (continuation.isActive) continuation.resume(it.right()) },
                onError = { if (continuation.isActive) continuation.resume(it.throwable.left()) },
            )
        }
    }

    /** Rejecting answers a proposal the SDK has just delivered, so it is already running. */
    fun rejectSession(proposerPublicKey: String) {
        WalletKit.rejectSession(
            params = Wallet.Params.SessionReject(proposerPublicKey = proposerPublicKey, reason = ""),
            onSuccess = {},
            onError = {},
        )
    }

    /** Read failures propagate on purpose — see [com.tangem.data.walletconnect.respond.DefaultWcRespondService]. */
    fun activeSessionByTopic(topic: String): Wallet.Model.Session? = WalletKit.getActiveSessionByTopic(topic)

    fun pendingSessionRequests(topic: String): List<Wallet.Model.SessionRequest> =
        WalletKit.getPendingListOfSessionRequests(topic)

    fun respondSessionRequestNonBlocking(params: Wallet.Params.SessionRequestResponse) {
        WalletKit.respondSessionRequest(params = params, onSuccess = {}, onError = {})
    }

    // Reown builds its Koin graph and Android components here, so it stays on the main thread the way the
    // SDK expects; what changed is when this runs, not where.
    private suspend fun initialize() = withContext(dispatchers.main) {
        CoreClient.initialize(
            relayServerUrl = "wss://$RELAY_URL?projectId=${projectIdProvider.provideProjectId()}",
            connectionType = ConnectionType.AUTOMATIC,
            application = application,
            metaData = appMetaData(),
        ) { error ->
            TangemLogger.withTag(WC_TAG).e("Error while initializing client: $error")
        }

        suspendCancellableCoroutine { continuation ->
            WalletKit.initialize(
                Wallet.Params.Init(core = CoreClient),
                onSuccess = {
                    WalletKit.setWalletDelegate(walletDelegate())
                    observers.get().forEach { it.onWcSdkInit() }
                    TangemLogger.withTag(WC_TAG).i("onWcSdkInit")
                    if (continuation.isActive) continuation.resume(Unit)
                },
                onError = { error ->
                    TangemLogger.withTag(WC_TAG).e("Error while initializing Web3Wallet: $error")
                    if (continuation.isActive) continuation.cancel(error.throwable)
                },
            )
        }
    }

    private fun appMetaData() = Core.Model.AppMetaData(
        name = "Tangem",
        description = "Tangem Wallet",
        url = "tangem.com",
        icons = listOf(
            "https://user-images.githubusercontent.com/24321494/124071202-72a00900-da58-11eb-935a-dcdab21de52b.png",
        ),
        redirect = "kotlin-wallet-wc:/request", // Custom Redirect URI
    )

    @Suppress("TooManyFunctions")
    private fun walletDelegate() = object : WalletKit.WalletDelegate {
        override val onSessionAuthenticate: ((Wallet.Model.SessionAuthenticate, Wallet.Model.VerifyContext) -> Unit)?
            get() = super.onSessionAuthenticate

        override fun onConnectionStateChange(state: Wallet.Model.ConnectionState) {
            TangemLogger.withTag(WC_TAG).i("sdk callback onConnectionStateChange isAvailable=${state.isAvailable}")
            observers.get().forEach { it.onConnectionStateChange(state) }
        }

        override fun onError(error: Wallet.Model.Error) {
            TangemLogger.withTag(WC_TAG).e("sdk callback onError", error.throwable)
            observers.get().forEach { it.onError(error) }
        }

        override fun onProposalExpired(proposal: Wallet.Model.ExpiredProposal) {
            TangemLogger.withTag(WC_TAG).i("sdk callback onProposalExpired $proposal")
            observers.get().forEach { it.onProposalExpired(proposal) }
        }

        override fun onRequestExpired(request: Wallet.Model.ExpiredRequest) {
            TangemLogger.withTag(WC_TAG).i("sdk callback onRequestExpired $request")
            observers.get().forEach { it.onRequestExpired(request) }
        }

        override fun onSessionDelete(sessionDelete: Wallet.Model.SessionDelete) {
            TangemLogger.withTag(WC_TAG).i("sdk callback onSessionDelete $sessionDelete")
            observers.get().forEach { it.onSessionDelete(sessionDelete) }
        }

        override fun onSessionExtend(session: Wallet.Model.Session) {
            TangemLogger.withTag(WC_TAG).i("sdk callback onSessionExtend $session")
            observers.get().forEach { it.onSessionExtend(session) }
        }

        override fun onSessionProposal(
            sessionProposal: Wallet.Model.SessionProposal,
            verifyContext: Wallet.Model.VerifyContext,
        ) {
            TangemLogger.withTag(WC_TAG).i("sdk callback onSessionProposal $sessionProposal")
            observers.get().forEach { it.onSessionProposal(sessionProposal, verifyContext) }
        }

        override fun onSessionRequest(
            sessionRequest: Wallet.Model.SessionRequest,
            verifyContext: Wallet.Model.VerifyContext,
        ) {
            TangemLogger.withTag(WC_TAG).i("sdk callback onSessionRequest $sessionRequest")
            observers.get().forEach { it.onSessionRequest(sessionRequest, verifyContext) }
        }

        override fun onSessionSettleResponse(settleSessionResponse: Wallet.Model.SettledSessionResponse) {
            TangemLogger.withTag(WC_TAG).i("sdk callback onSessionSettleResponse $settleSessionResponse")
            observers.get().forEach { it.onSessionSettleResponse(settleSessionResponse) }
        }

        override fun onSessionUpdateResponse(sessionUpdateResponse: Wallet.Model.SessionUpdateResponse) {
            TangemLogger.withTag(WC_TAG).i("sdk callback onSessionUpdateResponse $sessionUpdateResponse")
            observers.get().forEach { it.onSessionUpdateResponse(sessionUpdateResponse) }
        }
    }

    private companion object {
        const val RELAY_URL = "relay.walletconnect.com"
    }
}