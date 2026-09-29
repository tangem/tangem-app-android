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
import com.tangem.domain.walletconnect.config.WalletConnectConfig
import com.tangem.utils.coroutines.AppCoroutineScope
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.coroutines.runSuspendCatching
import com.tangem.utils.annotations.RemoveWithToggle
import com.tangem.utils.logging.TangemLogger
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
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
 * own storage, which is why with on-demand init it no longer runs during `Application.onCreate`.
 */
internal class WcSdk(
    private val application: Application,
    private val config: WalletConnectConfig,
    private val observers: () -> Set<WcSdkObserver>,
    private val scope: AppCoroutineScope,
    private val dispatchers: CoroutineDispatcherProvider,
) {

    private val lock = Any()

    // Held on a scope of its own so that a cancelled caller leaves the in-flight start-up running: the SDK
    // callback is already registered, and starting a second one would register the observers twice.
    private var initialization: Deferred<Result<Unit>>? = null

    @Volatile
    var isInitialized: Boolean = false
        private set

    /**
     * Starts the SDK right away on the calling thread, which must be the main one. Called from
     * `Application.onCreate`, before the first activity exists, so the SDK sees every activity from the start.
     */
    @RemoveWithToggle(
        toggleName = "AND_18021_WALLETCONNECT_ON_DEMAND_INIT_ENABLED",
        description = "Only the toggle-off path starts the SDK synchronously. Fold start() back into initialize().",
    )
    fun initializeNow() {
        val started = CompletableDeferred<Result<Unit>>()
        synchronized(lock) {
            if (initialization != null) return
            initialization = started
        }
        runCatching { start(onResult = { started.complete(it) }) }
            .onFailure { error ->
                TangemLogger.withTag(WC_TAG).e("Failed to start WalletConnect", error)
                started.complete(Result.failure(error))
            }
    }

    suspend fun ensureInitialized() {
        if (isInitialized) return

        val inFlight = synchronized(lock) {
            // The Result keeps a failure inside the Deferred: an exception escaping this async would
            // cancel the app-wide scope it runs on.
            initialization
                ?: scope.async { runSuspendCatching { initialize().getOrThrow() } }.also { initialization = it }
        }

        inFlight.await()
            .onFailure { error ->
                // Forget the failed attempt so the next caller starts a new one.
                synchronized(lock) { if (initialization === inFlight) initialization = null }
                throw error
            }
    }

    suspend fun pair(uri: String): Either<Throwable, Unit> = withStartedSdk {
        suspendCancellableCoroutine { continuation ->
            WalletKit.pair(
                params = Wallet.Params.Pair(uri),
                onSuccess = { if (continuation.isActive) continuation.resume(Unit.right()) },
                onError = { if (continuation.isActive) continuation.resume(it.throwable.left()) },
            )
        }
    }

    suspend fun approveSession(params: Wallet.Params.SessionApprove): Either<Throwable, Unit> = withStartedSdk {
        suspendCancellableCoroutine { continuation ->
            WalletKit.approveSession(
                params = params,
                onSuccess = { if (continuation.isActive) continuation.resume(Unit.right()) },
                onError = { if (continuation.isActive) continuation.resume(it.throwable.left()) },
            )
        }
    }

    /** Rejecting answers a proposal the SDK has just delivered, so it is already running. */
    fun rejectSessionNonBlocking(
        params: Wallet.Params.SessionReject,
        onSuccess: (Wallet.Params.SessionReject) -> Unit,
        onError: (Wallet.Model.Error) -> Unit,
    ) {
        WalletKit.rejectSession(params = params, onSuccess = onSuccess, onError = onError)
    }

    suspend fun getActiveSessions(): List<Wallet.Model.Session> {
        ensureInitialized()
        return WalletKit.getListOfActiveSessions()
    }

    suspend fun disconnectSession(topic: String): Either<Throwable, Unit> = withStartedSdk {
        suspendCancellableCoroutine { continuation ->
            WalletKit.disconnectSession(
                params = Wallet.Params.SessionDisconnect(topic),
                onSuccess = { if (continuation.isActive) continuation.resume(Unit.right()) },
                onError = { if (continuation.isActive) continuation.resume(it.throwable.left()) },
            )
        }
    }

    suspend fun extendSession(topic: String): Either<Throwable, Unit> = withStartedSdk {
        suspendCancellableCoroutine { continuation ->
            WalletKit.extendSession(
                params = Wallet.Params.SessionExtend(topic),
                onSuccess = { if (continuation.isActive) continuation.resume(Unit.right()) },
                onError = { if (continuation.isActive) continuation.resume(it.throwable.left()) },
            )
        }
    }

    suspend fun subscribe(topic: String): Either<Throwable, Unit> = withStartedSdk {
        suspendCancellableCoroutine { continuation ->
            CoreClient.Relay.subscribe(topic) { result ->
                val either = result.fold(onSuccess = { Unit.right() }, onFailure = { it.left() })
                if (continuation.isActive) continuation.resume(either)
            }
        }
    }

    suspend fun updateSession(params: Wallet.Params.SessionUpdate) = withStartedSdk {
        suspendCancellableCoroutine { continuation ->
            WalletKit.updateSession(
                params = params,
                onSuccess = { if (continuation.isActive) continuation.resume(it.right()) },
                onError = { if (continuation.isActive) continuation.resume(it.throwable.left()) },
            )
        }
    }

    suspend fun respondSessionRequest(params: Wallet.Params.SessionRequestResponse) = withStartedSdk {
        suspendCancellableCoroutine { continuation ->
            WalletKit.respondSessionRequest(
                params = params,
                onSuccess = { if (continuation.isActive) continuation.resume(it.right()) },
                onError = { if (continuation.isActive) continuation.resume(it.throwable.left()) },
            )
        }
    }

    fun respondSessionRequestNonBlocking(
        params: Wallet.Params.SessionRequestResponse,
        onSuccess: (Wallet.Params.SessionRequestResponse) -> Unit,
        onError: (Wallet.Model.Error) -> Unit,
    ) {
        WalletKit.respondSessionRequest(params = params, onSuccess = onSuccess, onError = onError)
    }

    /** Read failures propagate on purpose — see [com.tangem.data.walletconnect.respond.DefaultWcRespondService]. */
    fun getActiveSessionByTopic(topic: String): Wallet.Model.Session? = WalletKit.getActiveSessionByTopic(topic)

    fun getPendingSessionRequests(topic: String): List<Wallet.Model.SessionRequest> =
        WalletKit.getPendingListOfSessionRequests(topic)

    // Keeps these calls total: a failed start-up comes back as Left, the same channel the SDK's own
    // failures use, instead of cancelling the caller's coroutine.
    private suspend fun <T> withStartedSdk(block: suspend () -> Either<Throwable, T>): Either<Throwable, T> {
        return runSuspendCatching { ensureInitialized() }
            .fold(
                onSuccess = { block() },
                onFailure = { it.left() },
            )
    }

    // Reown builds its Koin graph and Android components here, so it stays on the main thread the way the
    // SDK expects; what changed is when this runs, not where.
    private suspend fun initialize(): Result<Unit> = withContext(dispatchers.main) {
        suspendCancellableCoroutine { continuation ->
            start(onResult = { result -> if (continuation.isActive) continuation.resume(result) })
        }
    }

    private fun start(onResult: (Result<Unit>) -> Unit) {
        CoreClient.initialize(
            relayServerUrl = "wss://$RELAY_URL?projectId=${config.projectId}",
            connectionType = ConnectionType.AUTOMATIC,
            application = application,
            metaData = appMetaData(),
        ) { error ->
            TangemLogger.withTag(WC_TAG).e("Error while initializing client: $error")
        }

        WalletKit.initialize(
            Wallet.Params.Init(core = CoreClient),
            onSuccess = {
                WalletKit.setWalletDelegate(walletDelegate())
                observers().forEach { it.onWcSdkInit() }
                // Recorded here, not after the await: the caller may be cancelled before it resumes,
                // and a second initialization would register the observers twice.
                isInitialized = true
                TangemLogger.withTag(WC_TAG).i("onWcSdkInit")
                onResult(Result.success(Unit))
            },
            onError = { error ->
                TangemLogger.withTag(WC_TAG).e("Error while initializing Web3Wallet: $error")
                onResult(Result.failure(error.throwable))
            },
        )
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
            observers().forEach { it.onConnectionStateChange(state) }
        }

        override fun onError(error: Wallet.Model.Error) {
            TangemLogger.withTag(WC_TAG).e("sdk callback onError", error.throwable)
            observers().forEach { it.onError(error) }
        }

        override fun onProposalExpired(proposal: Wallet.Model.ExpiredProposal) {
            TangemLogger.withTag(WC_TAG).i("sdk callback onProposalExpired $proposal")
            observers().forEach { it.onProposalExpired(proposal) }
        }

        override fun onRequestExpired(request: Wallet.Model.ExpiredRequest) {
            TangemLogger.withTag(WC_TAG).i("sdk callback onRequestExpired $request")
            observers().forEach { it.onRequestExpired(request) }
        }

        override fun onSessionDelete(sessionDelete: Wallet.Model.SessionDelete) {
            TangemLogger.withTag(WC_TAG).i("sdk callback onSessionDelete $sessionDelete")
            observers().forEach { it.onSessionDelete(sessionDelete) }
        }

        override fun onSessionExtend(session: Wallet.Model.Session) {
            TangemLogger.withTag(WC_TAG).i("sdk callback onSessionExtend $session")
            observers().forEach { it.onSessionExtend(session) }
        }

        override fun onSessionProposal(
            sessionProposal: Wallet.Model.SessionProposal,
            verifyContext: Wallet.Model.VerifyContext,
        ) {
            TangemLogger.withTag(WC_TAG).i("sdk callback onSessionProposal $sessionProposal")
            observers().forEach { it.onSessionProposal(sessionProposal, verifyContext) }
        }

        override fun onSessionRequest(
            sessionRequest: Wallet.Model.SessionRequest,
            verifyContext: Wallet.Model.VerifyContext,
        ) {
            TangemLogger.withTag(WC_TAG).i("sdk callback onSessionRequest $sessionRequest")
            observers().forEach { it.onSessionRequest(sessionRequest, verifyContext) }
        }

        override fun onSessionSettleResponse(settleSessionResponse: Wallet.Model.SettledSessionResponse) {
            TangemLogger.withTag(WC_TAG).i("sdk callback onSessionSettleResponse $settleSessionResponse")
            observers().forEach { it.onSessionSettleResponse(settleSessionResponse) }
        }

        override fun onSessionUpdateResponse(sessionUpdateResponse: Wallet.Model.SessionUpdateResponse) {
            TangemLogger.withTag(WC_TAG).i("sdk callback onSessionUpdateResponse $sessionUpdateResponse")
            observers().forEach { it.onSessionUpdateResponse(sessionUpdateResponse) }
        }
    }

    private companion object {
        const val RELAY_URL = "relay.walletconnect.com"
    }
}