package com.tangem.data.walletconnect.pair

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.reown.walletkit.client.Wallet
import com.reown.walletkit.client.WalletKit
import com.tangem.data.walletconnect.utils.WcSdkObserver
import com.tangem.domain.walletconnect.model.WcPairError
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class WcPairSdkDelegate : WcSdkObserver {

    private val onSessionProposal = Channel<Wallet.Model.SessionProposal>()
    private val onSessionSettleResponse = Channel<Wallet.Model.SettledSessionResponse>()

    suspend fun pair(url: String): Either<WcPairError, Wallet.Model.SessionProposal> = coroutineScope {
        suspend fun proposalCallback() = onSessionProposal
            .receiveAsFlow()
            .filter { proposal -> proposal.url == url }
            .first()

        val pairCall = async { sdkPair(url) }
        val proposal = async { proposalCallback() }
        pairCall.await().map { proposal.await() }
    }

    suspend fun approve(
        sessionApprove: Wallet.Params.SessionApprove,
    ): Either<WcPairError, Wallet.Model.SettledSessionResponse.Result> = coroutineScope {
        suspend fun approveCallback() = onSessionSettleResponse
            .receiveAsFlow()
            .first()

        val approveCall = async { sdkApprove(sessionApprove) }
        val approveCallback = async { approveCallback() }
        approveCall.await()
            .onLeft { return@coroutineScope it.left() }
        when (val result = approveCallback.await()) {
            is Wallet.Model.SettledSessionResponse.Result -> result.right()
            is Wallet.Model.SettledSessionResponse.Error ->
                WcPairError.ExternalApprovalError(result.errorMessage).left()
        }
    }

    fun rejectSession(proposerPublicKey: String) {
        WalletKit.rejectSession(
            params = Wallet.Params.SessionReject(
                proposerPublicKey = proposerPublicKey,
                reason = "",
            ),
            onSuccess = {},
            onError = {},
        )
    }

    override fun onSessionProposal(
        sessionProposal: Wallet.Model.SessionProposal,
        verifyContext: Wallet.Model.VerifyContext,
    ) {
        // Triggered when wallet receives the session proposal sent by a Dapp
        onSessionProposal.trySend(sessionProposal)
    }

    override fun onSessionSettleResponse(settleSessionResponse: Wallet.Model.SettledSessionResponse) {
        // Triggered when wallet receives the session settlement response from Dapp
        onSessionSettleResponse.trySend(settleSessionResponse)
    }

    private suspend fun sdkApprove(sessionApprove: Wallet.Params.SessionApprove): Either<WcPairError, Unit> {
        return suspendCancellableCoroutine { continuation ->
            WalletKit.approveSession(
                params = sessionApprove,
                onSuccess = { continuation.resume(Unit.right()) },
                onError = { continuation.resume(it.throwable.toPairError()) },
            )
        }
    }

    private suspend fun sdkPair(uri: String): Either<WcPairError, Unit> {
        return suspendCancellableCoroutine { continuation ->
            WalletKit.pair(
                params = Wallet.Params.Pair(uri),
                onSuccess = { continuation.resume(Unit.right()) },
                onError = { continuation.resume(it.throwable.toPairError()) },
            )
        }
    }

    private fun Throwable.toPairError() = WcPairError.ExternalApprovalError(this.localizedMessage.orEmpty()).left()
}