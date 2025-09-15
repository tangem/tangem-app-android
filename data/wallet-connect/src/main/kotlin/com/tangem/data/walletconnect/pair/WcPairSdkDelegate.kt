package com.tangem.data.walletconnect.pair

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.reown.walletkit.client.Wallet
import com.reown.walletkit.client.WalletKit
import com.tangem.data.walletconnect.utils.WC_TAG
import com.tangem.data.walletconnect.utils.WcScope
import com.tangem.data.walletconnect.utils.WcSdkObserver
import com.tangem.datasource.local.walletconnect.WalletConnectStore
import com.tangem.data.walletconnect.utils.getDappOriginUrl
import com.tangem.domain.walletconnect.model.WcPairError
import com.tangem.domain.walletconnect.model.WcPairError.ApprovalFailed
import com.tangem.domain.walletconnect.model.WcPendingApprovalSessionDTO
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*
import timber.log.Timber
import kotlin.coroutines.resume
import kotlin.time.Duration.Companion.seconds

internal class WcPairSdkDelegate(
    private val scope: WcScope,
    private val store: WalletConnectStore,
) : WcSdkObserver {

    private val onSessionProposal = Channel<Pair<Wallet.Model.SessionProposal, Wallet.Model.VerifyContext>>()
    private val onSdkErrorCallback = Channel<Wallet.Model.Error>()
    private val onSessionSettleCallback = Channel<Wallet.Model.SettledSessionResponse>()

    init {
        onSessionSettleCallback.receiveAsFlow()
            .filterIsInstance<Wallet.Model.SettledSessionResponse.Result>()
            .buffer()
            .onEach { settledResponse ->
                val savedPending = store.pendingApproval.first()
                val settledSession = settledResponse.session
                val savedPendingSession = savedPending
                    .find { it.pairingTopic == settledSession.pairingTopic }
                    ?: return@onEach
                store.saveSession(savedPendingSession.session.copy(topic = settledSession.topic))
                store.removePendingApproval(setOf(savedPendingSession))
            }
            .launchIn(scope)
    }

    suspend fun pair(
        url: String,
    ): Either<WcPairError, Pair<Wallet.Model.SessionProposal, Wallet.Model.VerifyContext>> = coroutineScope {
        val proposalCallback = async { withTimeout(CALLBACK_TIMEOUT.seconds) { proposalCallback() } }
        val pairCall = async { sdkPair(url) }
        pairCall.await().onLeft {
            proposalCallback.cancel()
            return@coroutineScope it.left()
        }
        proposalCallback.await()
    }

    private suspend fun proposalCallback() = callbackFlow {
        // wait first onSessionProposal callback
        launch {
            val sessionProposal = onSessionProposal.receiveAsFlow().first()
            trySend(sessionProposal.right())
            channel.close()
        }
        // OR
        // wait first onError callback
        launch {
            val error = onSdkErrorCallback.receiveAsFlow().first()
            trySend(error.throwable.toPairError().left())
            channel.close()
        }
        awaitClose()
    }.first()

    suspend fun approve(
        pendingSessionForSave: WcPendingApprovalSessionDTO,
        sessionApprove: Wallet.Params.SessionApprove,
    ): Either<WcPairError, Unit> = coroutineScope {
        val forSave = setOf(pendingSessionForSave)
        store.savePendingApproval(forSave)
        sdkApprove(sessionApprove).fold(
            ifRight = { Unit.right() },
            ifLeft = {
                store.removePendingApproval(forSave)
                it.left()
            },
        )
    }

    fun rejectSession(proposerPublicKey: String) {
        Timber.tag(WC_TAG).i("reject session proposerPublicKey = $proposerPublicKey")
        WalletKit.rejectSession(
            params = Wallet.Params.SessionReject(
                proposerPublicKey = proposerPublicKey,
                reason = "",
            ),
            onSuccess = {},
            onError = {},
        )
    }

    override fun onError(error: Wallet.Model.Error) {
        onSdkErrorCallback.trySend(error)
    }

    override fun onSessionProposal(
        sessionProposal: Wallet.Model.SessionProposal,
        verifyContext: Wallet.Model.VerifyContext,
    ) {
        val sessionProposalWithRealUrl = sessionProposal.copy(url = verifyContext.getDappOriginUrl())
        // Triggered when wallet receives the session proposal sent by a Dapp
        onSessionProposal.trySend(sessionProposalWithRealUrl to verifyContext)
    }

    override fun onSessionSettleResponse(settleSessionResponse: Wallet.Model.SettledSessionResponse) {
        // Triggered when wallet receives the session settlement response from Dapp
        onSessionSettleCallback.trySend(settleSessionResponse)
    }

    private suspend fun sdkApprove(sessionApprove: Wallet.Params.SessionApprove): Either<WcPairError, Unit> {
        return suspendCancellableCoroutine { continuation ->
            WalletKit.approveSession(
                params = sessionApprove,
                onSuccess = { if (continuation.isActive) continuation.resume(Unit.right()) },
                onError = { if (continuation.isActive) continuation.resume(it.throwable.toApproveError()) },
            )
        }
    }

    private suspend fun sdkPair(uri: String): Either<WcPairError, Unit> {
        return suspendCancellableCoroutine { continuation ->
            WalletKit.pair(
                params = Wallet.Params.Pair(uri),
                onSuccess = { if (continuation.isActive) continuation.resume(Unit.right()) },
                onError = { if (continuation.isActive) continuation.resume(it.throwable.toPairError().left()) },
            )
        }
    }

    private fun Throwable.toPairError() = when {
        pairingExpiredMessages.any { message.orEmpty().contains(it) } -> WcPairError.UriAlreadyUsed(message.orEmpty())
        else -> WcPairError.PairingFailed(this.localizedMessage.orEmpty())
    }

    private fun Throwable.toApproveError() = ApprovalFailed(this.localizedMessage.orEmpty()).left()

    companion object {
        private const val CALLBACK_TIMEOUT = 15
        // com.reown.android.pairing.engine.domain.PairingEngine.pair
        private val pairingExpiredMessages = listOf(
            "Pairing URI expired",
            "Pairing expired",
            "No proposal or pending session authenticate request for pairing topic",
        )
    }
}