package com.tangem.data.walletconnect.pair

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.reown.walletkit.client.Wallet
import com.reown.walletkit.client.WalletKit
import com.tangem.data.walletconnect.utils.WC_TAG
import com.tangem.data.walletconnect.utils.WcSdkObserver
import com.tangem.domain.walletconnect.model.WcPairError
import com.tangem.domain.walletconnect.model.WcPairError.ApprovalFailed
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import timber.log.Timber
import kotlin.coroutines.resume
import kotlin.time.Duration.Companion.seconds

internal class WcPairSdkDelegate : WcSdkObserver {

    private val onSessionProposal = Channel<Pair<Wallet.Model.SessionProposal, Wallet.Model.VerifyContext>>()
    private val onSdkErrorCallback = Channel<Wallet.Model.Error>()
    private val onSessionSettleResponse = Channel<Wallet.Model.SettledSessionResponse>()

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
        sessionApprove: Wallet.Params.SessionApprove,
    ): Either<WcPairError, Wallet.Model.SettledSessionResponse.Result> = coroutineScope {
        val approveCallback = async { withTimeout(CALLBACK_TIMEOUT.seconds) { approveCallback() } }
        val approveCall = async { sdkApprove(sessionApprove) }
        approveCall.await()
            .onLeft {
                approveCallback.cancel()
                return@coroutineScope it.left()
            }
        return@coroutineScope approveCallback.await().fold(
            ifLeft = { it.left() },
            ifRight = { result ->
                when (result) {
                    is Wallet.Model.SettledSessionResponse.Result -> result.right()
                    is Wallet.Model.SettledSessionResponse.Error -> ApprovalFailed(result.errorMessage).left()
                }
            },
        )
    }

    private suspend fun approveCallback() = callbackFlow<Either<WcPairError, Wallet.Model.SettledSessionResponse>> {
        // wait first onSessionSettleResponse callback
        launch {
            val settledSessionResponse = onSessionSettleResponse.receiveAsFlow().first()
            trySend(settledSessionResponse.right())
            channel.close()
        }
        // OR
        // wait first onError callback
        launch {
            val error = onSdkErrorCallback.receiveAsFlow().first()
            trySend(error.throwable.toApproveError())
            channel.close()
        }
        awaitClose()
    }.first()

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
        val sessionProposalWithRealUrl = sessionProposal.copy(url = verifyContext.origin)
        // Triggered when wallet receives the session proposal sent by a Dapp
        onSessionProposal.trySend(sessionProposalWithRealUrl to verifyContext)
    }

    override fun onSessionSettleResponse(settleSessionResponse: Wallet.Model.SettledSessionResponse) {
        // Triggered when wallet receives the session settlement response from Dapp
        onSessionSettleResponse.trySend(settleSessionResponse)
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